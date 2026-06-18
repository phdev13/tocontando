import { Hono } from 'hono';
import { z } from 'zod';
import type { Env } from '../types';

export const analyticsRoutes = new Hono<{ Bindings: Env }>();

// Approved event names — contract-locked, no free-form strings
const APPROVED_EVENTS = new Set([
  'app_opened', 'session_started', 'session_ended', 'screen_viewed',
  'onboarding_started', 'onboarding_completed', 'onboarding_skipped',
  'event_creation_started', 'event_created', 'event_creation_abandoned',
  'event_edited', 'event_archived', 'event_restored', 'event_deleted',
  'highlight_opened', 'notification_permission_result', 'notification_opened',
  'feedback_opened', 'feedback_submitted',
  'ota_check_completed', 'ota_update_available', 'ota_download_started',
  'ota_download_completed', 'ota_modal_shown', 'ota_update_deferred',
  'ota_installation_started', 'app_version_changed',
]);

// Approved property keys — prevents accidental personal data leakage
const APPROVED_PROPERTY_KEYS = new Set([
  'screen', 'previous_screen', 'session_id', 'duration_ms',
  'version_code', 'version_name', 'android_version', 'theme',
  'ota_version_code', 'ota_channel', 'ota_mandatory',
  'notification_granted', 'feedback_category', 'error_type',
  'cold_start_ms', 'warm_start_ms',
]);

const EventSchema = z.object({
  name: z.string().max(64),
  properties: z.record(z.string().max(32), z.union([z.string().max(256), z.number(), z.boolean()])).optional(),
  sessionId: z.string().max(64).optional(),
  occurredAt: z.number().int().optional(),
});

const BatchSchema = z.object({
  installationId: z.string().uuid(),
  versionCode: z.number().int().positive(),
  events: z.array(EventSchema).max(100),
});

/**
 * POST /api/v1/analytics/events
 * Receives a batch of analytics events. Non-blocking insert.
 * Rejects unknown event names and property keys silently.
 */
analyticsRoutes.post('/analytics/events', async (c) => {
  const body = await c.req.json().catch(() => null);
  if (!body) return c.json({ error: 'Invalid body', code: 'PARSE_ERROR' }, 400);

  const parsed = BatchSchema.safeParse(body);
  if (!parsed.success) {
    return c.json({ error: 'Invalid payload', code: 'VALIDATION_ERROR' }, 400);
  }

  const { installationId, versionCode, events } = parsed.data;

  // Filter to only approved events and sanitize properties
  const validEvents = events
    .filter(e => APPROVED_EVENTS.has(e.name))
    .map(e => ({
      name: e.name,
      properties: sanitizeProperties(e.properties ?? {}),
      sessionId: e.sessionId,
      occurredAt: e.occurredAt ?? Date.now(),
    }));

  if (validEvents.length === 0) {
    return c.json({ accepted: 0 }, 200);
  }

  // Batch insert using D1 batch API
  const statements = validEvents.map(e =>
    c.env.DB.prepare(
      `INSERT INTO analytics_events(installation_id, event_name, properties, version_code, session_id, created_at)
       VALUES(?, ?, ?, ?, ?, ?)`
    ).bind(
      installationId,
      e.name,
      JSON.stringify(e.properties),
      versionCode,
      e.sessionId ?? null,
      e.occurredAt
    )
  );

  // Fire and forget — analytics never blocks the response
  c.executionCtx.waitUntil(
    c.env.DB.batch(statements).catch((err) => {
      console.error(JSON.stringify({ level: 'error', message: 'analytics_batch_failed', error: String(err) }));
    })
  );

  return c.json({ accepted: validEvents.length }, 200);
});

/**
 * GET /api/v1/analytics/events/count
 * Public endpoint — returns total count of events created across all users.
 * Used by the marketing site to show a live counter.
 * No auth required (read-only aggregate, no personal data).
 */
analyticsRoutes.get('/stats/events/count', async (c) => {
  try {
    const result = await c.env.DB.prepare(
      `SELECT COUNT(*) as total FROM analytics_events WHERE event_name = 'event_created'`
    ).first<{ total: number }>();

    const total = result?.total ?? 0;

    return c.json({ total }, 200, {
      'Cache-Control': 'public, max-age=60', // cache por 60s para não sobrecarregar o DB
    });
  } catch (error) {
    return c.json({ total: 0 }, 200);
  }
});


/**
 * POST /api/v1/analytics/performance
 * Receives performance metrics from the app.
 */
const PerfSchema = z.object({
  installationId: z.string().uuid(),
  versionCode: z.number().int().positive(),
  androidVersion: z.string().max(10).optional(),
  metrics: z.array(z.object({
    type: z.enum(['cold_start', 'warm_start', 'query_duration', 'render_time', 'slow_frame']),
    valueMs: z.number().min(0).max(60000),
    screen: z.string().max(64).optional(),
  })).max(50),
});

analyticsRoutes.post('/analytics/performance', async (c) => {
  const body = await c.req.json().catch(() => null);
  if (!body) return c.json({ error: 'Invalid body' }, 400);

  const parsed = PerfSchema.safeParse(body);
  if (!parsed.success) return c.json({ error: 'Invalid payload' }, 400);

  const { installationId, versionCode, androidVersion, metrics } = parsed.data;

  const statements = metrics.map(m =>
    c.env.DB.prepare(
      `INSERT INTO performance_metrics(installation_id, metric_type, value_ms, screen, version_code, android_version)
       VALUES(?, ?, ?, ?, ?, ?)`
    ).bind(installationId, m.type, m.valueMs, m.screen ?? null, versionCode, androidVersion ?? null)
  );

  c.executionCtx.waitUntil(
    c.env.DB.batch(statements).catch(() => {/* silent */})
  );

  return c.json({ accepted: metrics.length }, 200);
});

function sanitizeProperties(props: Record<string, unknown>): Record<string, unknown> {
  return Object.fromEntries(
    Object.entries(props)
      .filter(([key]) => APPROVED_PROPERTY_KEYS.has(key))
      .slice(0, 20) // max 20 properties
  );
}

/**
 * POST /api/v1/telemetry/notifications
 * Receives notification diagnostics.
 */
const NotifDiagSchema = z.object({
  installationId: z.string().uuid(),
  notificationsAllowed: z.boolean(),
  exactAlarmsAllowed: z.boolean(),
  activeSchedules: z.number().int().min(0),
  nextTriggerAt: z.number().int().nullable().optional(),
  lastReconciliationAt: z.number().int().optional(),
});

analyticsRoutes.post('/telemetry/notifications', async (c) => {
  const body = await c.req.json().catch(() => null);
  if (!body) return c.json({ error: 'Invalid body' }, 400);

  const parsed = NotifDiagSchema.safeParse(body);
  if (!parsed.success) return c.json({ error: 'Invalid payload' }, 400);

  const { installationId, notificationsAllowed, exactAlarmsAllowed, activeSchedules, nextTriggerAt, lastReconciliationAt } = parsed.data;

  // Upsert
  const query = `
    INSERT INTO notification_diagnostics(installation_id, notifications_allowed, exact_alarms_allowed, active_schedules, next_trigger_at, last_reconciliation_at, updated_at)
    VALUES(?, ?, ?, ?, ?, ?, ?)
    ON CONFLICT(installation_id) DO UPDATE SET
      notifications_allowed = excluded.notifications_allowed,
      exact_alarms_allowed = excluded.exact_alarms_allowed,
      active_schedules = excluded.active_schedules,
      next_trigger_at = excluded.next_trigger_at,
      last_reconciliation_at = excluded.last_reconciliation_at,
      updated_at = excluded.updated_at
  `;

  c.executionCtx.waitUntil(
    c.env.DB.prepare(query)
      .bind(
        installationId,
        notificationsAllowed ? 1 : 0,
        exactAlarmsAllowed ? 1 : 0,
        activeSchedules,
        nextTriggerAt ?? null,
        lastReconciliationAt ?? null,
        Date.now()
      )
      .run()
      .catch((e) => console.error("Failed to save notif diags", e))
  );

  return c.json({ success: true, data: { accepted: true }, error: null, meta: { requestId: crypto.randomUUID() } }, 200);
});

