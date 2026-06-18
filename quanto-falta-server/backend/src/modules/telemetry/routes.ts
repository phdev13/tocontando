import { Hono } from 'hono';
import { z } from 'zod';
import type { Env } from '../../types';
import { fail, ok } from '../../contracts/apiResponse';

export const telemetryAppRoutes = new Hono<{ Bindings: Env }>();

const APPROVED_SOURCES = ['android', 'website', 'admin', 'backend'] as const;
const APPROVED_EVENTS = new Set([
  'app_opened', 'session_started', 'session_ended', 'screen_viewed',
  'onboarding_started', 'onboarding_completed', 'onboarding_skipped',
  'event_creation_started', 'event_created', 'event_creation_abandoned',
  'event_edited', 'event_archived', 'event_restored', 'event_deleted',
  'highlight_opened', 'notification_permission_result', 'notification_opened',
  'feedback_submitted', 'ota_check_completed', 'ota_update_available',
  'ota_download_started', 'ota_download_completed', 'ota_modal_shown',
  'ota_update_deferred', 'ota_installation_started', 'app_version_changed',
  'premium_status_checked', 'premium_token_activated',
]);
const APPROVED_PROPERTY_KEYS = new Set([
  'screen', 'session_id', 'duration_ms', 'version_code', 'version_name',
  'android_version', 'theme', 'event_type', 'is_premium', 'ota_version_code',
  'ota_channel', 'ota_mandatory', 'feedback_category', 'error_type',
]);

const TelemetrySchema = z.object({
  installationId: z.string().uuid(),
  versionCode: z.number().int().positive().optional(),
  source: z.enum(APPROVED_SOURCES).default('android'),
  events: z.array(z.object({
    event: z.string().max(64).optional(),
    name: z.string().max(64).optional(),
    timestamp: z.string().datetime().optional(),
    occurredAt: z.number().int().optional(),
    sessionId: z.string().max(64).optional(),
    properties: z.record(z.union([z.string().max(256), z.number(), z.boolean()])).optional(),
  })).max(100),
});

telemetryAppRoutes.post('/telemetry', async (c) => {
  let body: any = null;
  try {
    if (c.req.header('content-encoding') === 'gzip') {
      const decompressed = c.req.raw.body!.pipeThrough(new DecompressionStream('gzip'));
      body = await new Response(decompressed).json();
    } else {
      body = await c.req.json();
    }
  } catch (e) {
    return fail(c, 'INVALID_REQUEST', 'Payload de telemetria invalido ou malformado.', 400);
  }

  const parsed = TelemetrySchema.safeParse(body);
  if (!parsed.success) return fail(c, 'INVALID_REQUEST', 'Schema de telemetria invalido.', 400);

  const validEvents = parsed.data.events
    .map((event) => ({ ...event, eventName: event.event ?? event.name ?? '' }))
    .filter((event) => APPROVED_EVENTS.has(event.eventName))
    .map((event) => ({
      event: event.eventName,
      timestamp: event.timestamp ? Date.parse(event.timestamp) : event.occurredAt ?? Date.now(),
      sessionId: event.sessionId,
      properties: sanitizeProperties(event.properties ?? {}),
    }));

  if (validEvents.length > 0) {
    const statements = validEvents.map((event) =>
      c.env.DB.prepare(
        `INSERT INTO analytics_events(installation_id, event_name, properties, version_code, session_id, created_at)
         VALUES(?, ?, ?, ?, ?, ?)`
      ).bind(
        parsed.data.installationId,
        event.event,
        JSON.stringify({ source: parsed.data.source, ...event.properties }),
        parsed.data.versionCode ?? (typeof event.properties.version_code === 'number' ? event.properties.version_code : null),
        event.sessionId ?? (typeof event.properties.session_id === 'string' ? event.properties.session_id : null),
        event.timestamp,
      )
    );

    c.executionCtx.waitUntil(c.env.DB.batch(statements).catch(() => undefined));
  }

  return ok(c, { accepted: validEvents.length });
});

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

telemetryAppRoutes.post('/telemetry/performance', async (c) => {
  let body: any = null;
  try {
    if (c.req.header('content-encoding') === 'gzip') {
      const decompressed = c.req.raw.body!.pipeThrough(new DecompressionStream('gzip'));
      body = await new Response(decompressed).json();
    } else {
      body = await c.req.json();
    }
  } catch (e) {
    return fail(c, 'INVALID_REQUEST', 'Payload de performance invalido ou malformado.', 400);
  }

  const parsed = PerfSchema.safeParse(body);
  if (!parsed.success) return fail(c, 'INVALID_REQUEST', 'Schema de performance invalido.', 400);

  const { installationId, versionCode, androidVersion, metrics } = parsed.data;
  const statements = metrics.map((metric) =>
    c.env.DB.prepare(
      `INSERT INTO performance_metrics(installation_id, metric_type, value_ms, screen, version_code, android_version)
       VALUES(?, ?, ?, ?, ?, ?)`
    ).bind(installationId, metric.type, metric.valueMs, metric.screen ?? null, versionCode, androidVersion ?? null)
  );

  c.executionCtx.waitUntil(c.env.DB.batch(statements).catch(() => undefined));

  return ok(c, { accepted: metrics.length });
});

function sanitizeProperties(props: Record<string, unknown>): Record<string, unknown> {
  return Object.fromEntries(
    Object.entries(props)
      .filter(([key]) => APPROVED_PROPERTY_KEYS.has(key))
      .slice(0, 20),
  );
}
