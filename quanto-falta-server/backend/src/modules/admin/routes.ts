import { Hono } from 'hono';
import { z } from 'zod';
import type { Env } from '../../types';
import { adminAuthMiddleware } from '../../middleware/auth';
import { fail, ok } from '../../contracts/apiResponse';
import { adminSettingsRoutes } from '../../routes/admin/settings';
import { adminTestersRoutes } from '../../routes/admin/testers';
import { adminMonetizationRoutes } from '../../routes/admin/monetization';
import { adminVersionsRoutes } from '../../routes/admin/versions';
import { adminMetricsRoutes } from '../../routes/admin/metrics';
import { adminFeedbackRoutes } from '../../routes/admin/feedback';

export const adminV1Routes = new Hono<{ Bindings: Env }>();

adminV1Routes.use('*', adminAuthMiddleware);

adminV1Routes.route('/settings', adminSettingsRoutes);
adminV1Routes.route('/testers', adminTestersRoutes);
adminV1Routes.route('/premium', adminMonetizationRoutes);
adminV1Routes.route('/ota/releases', adminVersionsRoutes);
adminV1Routes.route('/metrics', adminMetricsRoutes);
adminV1Routes.route('/feedback', adminFeedbackRoutes);

adminV1Routes.get('/dashboard', async (c) => {
  const days = Math.min(parseInt(c.req.query('days') ?? '30', 10), 90);
  const since = Date.now() - days * 24 * 60 * 60 * 1000;
  const previousSince = since - days * 24 * 60 * 60 * 1000;

  const [
    installations,
    previousInstallations,
    activeInstallations,
    feedback,
    previousFeedback,
    feedbackRating,
    eventsCreated,
    installsByDay,
    eventsByDay,
    versionDistribution,
    retention,
    otaStats,
    performance,
    recentFeedback,
    recentVersions,
    recentAudit,
  ] = await Promise.all([
    c.env.DB.prepare('SELECT COUNT(*) as count FROM installations').first<{ count: number }>(),
    c.env.DB.prepare('SELECT COUNT(*) as count FROM installations WHERE first_seen_at >= ? AND first_seen_at < ?').bind(previousSince, since).first<{ count: number }>(),
    c.env.DB.prepare('SELECT COUNT(*) as count FROM installations WHERE last_seen_at >= ?').bind(since).first<{ count: number }>(),
    c.env.DB.prepare('SELECT COUNT(*) as count FROM feedback WHERE created_at >= ?').bind(since).first<{ count: number }>(),
    c.env.DB.prepare('SELECT COUNT(*) as count FROM feedback WHERE created_at >= ? AND created_at < ?').bind(previousSince, since).first<{ count: number }>(),
    c.env.DB.prepare('SELECT AVG(rating) as average FROM feedback WHERE rating IS NOT NULL AND created_at >= ?').bind(since).first<{ average: number | null }>(),
    c.env.DB.prepare("SELECT COUNT(*) as count FROM analytics_events WHERE event_name = 'event_created' AND created_at >= ?").bind(since).first<{ count: number }>(),
    c.env.DB.prepare(`
      SELECT date(first_seen_at / 1000, 'unixepoch') as date, COUNT(*) as value
      FROM installations
      WHERE first_seen_at >= ?
      GROUP BY date
      ORDER BY date ASC
    `).bind(since).all(),
    c.env.DB.prepare(`
      SELECT date(created_at / 1000, 'unixepoch') as date, event_name as eventName, COUNT(*) as value
      FROM analytics_events
      WHERE created_at >= ?
      GROUP BY date, event_name
      ORDER BY date ASC
    `).bind(since).all(),
    c.env.DB.prepare(`
      SELECT version_code as versionCode, version_name as versionName, COUNT(*) as count
      FROM installations
      WHERE last_seen_at >= ?
      GROUP BY version_code, version_name
      ORDER BY count DESC
      LIMIT 8
    `).bind(since).all(),
    c.env.DB.prepare(`
      SELECT
        COUNT(*) as base,
        COUNT(CASE WHEN last_seen_at >= first_seen_at + 86400000 THEN 1 END) as d1,
        COUNT(CASE WHEN last_seen_at >= first_seen_at + 604800000 THEN 1 END) as d7,
        COUNT(CASE WHEN last_seen_at >= first_seen_at + 2592000000 THEN 1 END) as d30
      FROM installations
      WHERE first_seen_at >= ?
    `).bind(since).first<{ base: number; d1: number; d7: number; d30: number }>(),
    c.env.DB.prepare(`
      SELECT
        COUNT(CASE WHEN event_type = 'check' THEN 1 END) as checks,
        COUNT(CASE WHEN event_type = 'download_started' THEN 1 END) as downloadsStarted,
        COUNT(CASE WHEN event_type = 'download_completed' THEN 1 END) as downloadsCompleted,
        COUNT(CASE WHEN event_type = 'adopted' THEN 1 END) as adopted,
        COUNT(CASE WHEN event_type = 'failed' THEN 1 END) as failed
      FROM ota_attempts
      WHERE created_at >= ?
    `).bind(since).first<any>(),
    c.env.DB.prepare(`
      SELECT
        AVG(CASE WHEN metric_type = 'cold_start' THEN value_ms END) as avgColdStartMs,
        AVG(CASE WHEN metric_type = 'warm_start' THEN value_ms END) as avgWarmStartMs,
        COUNT(CASE WHEN metric_type = 'slow_frame' THEN 1 END) as slowFrames
      FROM performance_metrics
      WHERE created_at >= ?
    `).bind(since).first<any>(),
    c.env.DB.prepare(`
      SELECT id, rating, category, message, status, version_code as versionCode, created_at as createdAt
      FROM feedback
      ORDER BY created_at DESC
      LIMIT 5
    `).all(),
    c.env.DB.prepare(`
      SELECT version_code as versionCode, version_name as versionName, status, release_channel as releaseChannel, published_at as publishedAt
      FROM app_versions
      ORDER BY COALESCE(published_at, created_at) DESC
      LIMIT 5
    `).all(),
    c.env.DB.prepare(`
      SELECT id, action, target_type as targetType, target_id as targetId, details, created_at as createdAt
      FROM audit_logs
      ORDER BY created_at DESC
      LIMIT 5
    `).all(),
  ]);

  const totalInstalls = installations?.count ?? 0;
  const previousInstalls = previousInstallations?.count ?? 0;
  const feedbackCount = feedback?.count ?? 0;
  const previousFeedbackCount = previousFeedback?.count ?? 0;
  const versionRows = versionDistribution.results as any[];
  const versionTotal = versionRows.reduce((sum, item) => sum + Number(item.count ?? 0), 0);
  const otaFailed = Number(otaStats?.failed ?? 0);
  const otaCompleted = Number(otaStats?.downloadsCompleted ?? 0);
  const appHealth = otaFailed === 0 ? 100 : Math.max(0, Math.round((otaCompleted / Math.max(1, otaCompleted + otaFailed)) * 100));
  const retentionBase = Math.max(1, retention?.base ?? 0);

  return ok(c, {
    period: { days, since: new Date(since).toISOString() },
    installations: {
      total: totalInstalls,
      active: activeInstallations?.count ?? 0,
      previousNew: previousInstalls,
      changePercent: percentChange(totalInstalls, previousInstalls),
    },
    feedback: {
      recent: feedbackCount,
      previousRecent: previousFeedbackCount,
      changePercent: percentChange(feedbackCount, previousFeedbackCount),
      averageRating: feedbackRating?.average ? round(feedbackRating.average, 1) : null,
    },
    eventsCreated: eventsCreated?.count ?? 0,
    retention: {
      d1: round(((retention?.d1 ?? 0) / retentionBase) * 100, 1),
      d7: round(((retention?.d7 ?? 0) / retentionBase) * 100, 1),
      d30: round(((retention?.d30 ?? 0) / retentionBase) * 100, 1),
      base: retention?.base ?? 0,
    },
    charts: {
      installations: fillDailySeries(since, days, installsByDay.results as any[], 'value'),
      events: fillEventSeries(since, days, eventsByDay.results as any[]),
    },
    versionDistribution: versionRows.map((item) => ({
      ...item,
      percent: versionTotal > 0 ? round((Number(item.count) / versionTotal) * 100, 1) : 0,
    })),
    appHealth: {
      score: appHealth,
      status: appHealth >= 98 ? 'healthy' : appHealth >= 90 ? 'attention' : 'critical',
      ota: otaStats,
      performance,
    },
    recentActivity: [
      ...(recentVersions.results as any[]).map((item) => ({
        type: 'OTA_RELEASE',
        title: `Versao ${item.versionName} ${item.status}`,
        description: `${item.releaseChannel} build ${item.versionCode}`,
        createdAt: item.publishedAt,
      })),
      ...(recentFeedback.results as any[]).map((item) => ({
        type: 'FEEDBACK',
        title: `Feedback ${item.status}`,
        description: `${item.rating ?? '-'} estrelas - ${item.category}`,
        createdAt: item.createdAt,
      })),
      ...(recentAudit.results as any[]).map((item) => ({
        type: item.action,
        title: item.action,
        description: `${item.targetType ?? 'sistema'} ${item.targetId ?? ''}`.trim(),
        createdAt: item.createdAt,
      })),
    ].sort((a, b) => Number(b.createdAt ?? 0) - Number(a.createdAt ?? 0)).slice(0, 8),
    alerts: buildAlerts({
      appHealth,
      feedbackCount,
      avgColdStartMs: performance?.avgColdStartMs,
      d7: round(((retention?.d7 ?? 0) / retentionBase) * 100, 1),
    }),
    generatedAt: new Date().toISOString(),
  });
});

adminV1Routes.get('/telemetry/events', async (c) => {
  const days = Math.min(parseInt(c.req.query('days') ?? '30', 10), 90);
  const since = Date.now() - days * 24 * 60 * 60 * 1000;
  const [summary, byName, byDay] = await Promise.all([
    c.env.DB.prepare(`
      SELECT COUNT(*) as total, COUNT(DISTINCT installation_id) as installations
      FROM analytics_events
      WHERE created_at >= ?
    `).bind(since).first(),
    c.env.DB.prepare(`
      SELECT event_name as eventName, COUNT(*) as count
      FROM analytics_events
      WHERE created_at >= ?
      GROUP BY event_name
      ORDER BY count DESC
      LIMIT 20
    `).bind(since).all(),
    c.env.DB.prepare(`
      SELECT date(created_at / 1000, 'unixepoch') as date, event_name as eventName, COUNT(*) as count
      FROM analytics_events
      WHERE created_at >= ?
      GROUP BY date, event_name
      ORDER BY date ASC
    `).bind(since).all(),
  ]);

  return ok(c, {
    period: { days, since: new Date(since).toISOString() },
    summary,
    byName: byName.results,
    byDay: fillEventSeries(since, days, byDay.results as any[]),
  });
});

adminV1Routes.get('/logs', async (c) => {
  const limit = Math.min(parseInt(c.req.query('limit') ?? '100', 10), 200);
  const { results } = await c.env.DB.prepare(`
    SELECT id, admin_user_id as adminUserId, action, target_type as targetType, target_id as targetId, details, created_at as createdAt
    FROM audit_logs
    ORDER BY created_at DESC
    LIMIT ?
  `).bind(limit).all();

  return ok(c, { logs: results, pagination: { limit } });
});

adminV1Routes.get('/metrics', async (c) => {
  const days = Math.min(parseInt(c.req.query('days') ?? '30', 10), 90);
  const since = Date.now() - days * 24 * 60 * 60 * 1000;
  const [dailyMetrics, versionDistribution, feedbackStats] = await Promise.all([
    c.env.DB.prepare(`
      SELECT date, metric_name as metricName, metric_value as metricValue, dimension_key as dimensionKey, dimension_value as dimensionValue
      FROM daily_metrics
      WHERE date >= date(?, 'unixepoch')
      ORDER BY date DESC, metric_name ASC
      LIMIT 500
    `).bind(Math.floor(since / 1000)).all(),
    c.env.DB.prepare(`
      SELECT version_code as versionCode, version_name as versionName, COUNT(*) as count
      FROM installations
      WHERE last_seen_at >= ?
      GROUP BY version_code, version_name
      ORDER BY version_code DESC
      LIMIT 20
    `).bind(since).all(),
    c.env.DB.prepare(`
      SELECT COUNT(*) as count, AVG(CASE WHEN rating IS NOT NULL THEN rating ELSE NULL END) as averageRating
      FROM feedback
      WHERE created_at >= ?
    `).bind(since).first<{ count: number; averageRating: number | null }>(),
  ]);

  return ok(c, {
    period: { days, since: new Date(since).toISOString() },
    daily: dailyMetrics.results,
    versionDistribution: versionDistribution.results,
    feedback: {
      count: feedbackStats?.count ?? 0,
      averageRating: feedbackStats?.averageRating ?? null,
    },
  });
});

adminV1Routes.get('/devices', async (c) => {
  const limit = Math.min(parseInt(c.req.query('limit') ?? '50', 10), 100);
  const { results } = await c.env.DB.prepare(`
    SELECT installation_id as installationId, version_code as versionCode, version_name as versionName,
      android_version as androidVersion, manufacturer, model, release_channel as releaseChannel,
      first_seen_at as firstSeenAt, last_seen_at as lastSeenAt, is_active as isActive
    FROM installations
    ORDER BY last_seen_at DESC
    LIMIT ?
  `).bind(limit).all();

  return ok(c, { devices: results, pagination: { limit } });
});

adminV1Routes.get('/devices/:id/diagnostics', async (c) => {
  const item = await c.env.DB.prepare(`
    SELECT
      notifications_allowed as notificationsAllowed,
      exact_alarms_allowed as exactAlarmsAllowed,
      active_schedules as activeSchedules,
      next_trigger_at as nextTriggerAt,
      last_reconciliation_at as lastReconciliationAt,
      updated_at as updatedAt
    FROM notification_diagnostics
    WHERE installation_id = ?
  `).bind(c.req.param('id')).first();

  if (!item) return fail(c, 'NOT_FOUND', 'Diagnostico nao encontrado para este dispositivo.', 404);
  return ok(c, { diagnostics: item });
});

adminV1Routes.get('/feedback', async (c) => {
  const status = c.req.query('status');
  const limit = Math.min(parseInt(c.req.query('limit') ?? '50', 10), 100);
  const params: (string | number)[] = [];
  let query = `
    SELECT id, installation_id as installationId, rating, category, message, status, priority,
      admin_notes as adminNotes, tags, created_at as createdAt, updated_at as updatedAt
    FROM feedback
  `;

  if (status) {
    query += ' WHERE status = ?';
    params.push(status);
  }

  query += ' ORDER BY created_at DESC LIMIT ?';
  params.push(limit);

  const { results } = await c.env.DB.prepare(query).bind(...params).all();
  return ok(c, { feedback: results, pagination: { limit } });
});

adminV1Routes.get('/feedback/:id', async (c) => {
  const item = await c.env.DB.prepare(`
    SELECT *
    FROM feedback
    WHERE id = ?
  `).bind(c.req.param('id')).first();

  if (!item) return fail(c, 'NOT_FOUND', 'Feedback nao encontrado.', 404);
  return ok(c, { feedback: item });
});

adminV1Routes.patch('/feedback/:id', async (c) => {
  const body = await c.req.json().catch(() => null);
  const parsed = z.object({
    status: z.enum(['new', 'reviewing', 'planned', 'resolved', 'ignored']).optional(),
    priority: z.enum(['low', 'normal', 'high', 'urgent']).optional(),
    adminNotes: z.string().max(2000).optional(),
    tags: z.array(z.string().max(32)).max(20).optional(),
  }).safeParse(body);

  if (!parsed.success) return fail(c, 'INVALID_REQUEST', 'Payload de feedback invalido.', 400);

  const existing = await c.env.DB.prepare('SELECT id FROM feedback WHERE id = ?').bind(c.req.param('id')).first();
  if (!existing) return fail(c, 'NOT_FOUND', 'Feedback nao encontrado.', 404);

  await c.env.DB.prepare(`
    UPDATE feedback
    SET status = COALESCE(?, status),
      priority = COALESCE(?, priority),
      admin_notes = COALESCE(?, admin_notes),
      tags = COALESCE(?, tags),
      updated_at = ?
    WHERE id = ?
  `).bind(
    parsed.data.status ?? null,
    parsed.data.priority ?? null,
    parsed.data.adminNotes ?? null,
    parsed.data.tags ? JSON.stringify(parsed.data.tags) : null,
    Date.now(),
    c.req.param('id'),
  ).run();

  await writeAudit(c, 'feedback_updated', 'feedback', c.req.param('id'), parsed.data);

  return ok(c, { updated: true });
});

adminV1Routes.get('/ota/releases', async (c) => {
  const { results } = await c.env.DB.prepare(`
    SELECT id, version_code as versionCode, version_name as versionName, release_channel as releaseChannel,
      status, mandatory, title, summary, rollout_percentage as rolloutPercentage, published_at as publishedAt,
      created_at as createdAt, updated_at as updatedAt
    FROM app_versions
    ORDER BY version_code DESC
    LIMIT 100
  `).all();

  return ok(c, { releases: results });
});

adminV1Routes.patch('/config', async (c) => {
  const body = await c.req.json().catch(() => null);
  const parsed = z.record(z.string().max(500)).safeParse(body);
  if (!parsed.success) return fail(c, 'INVALID_REQUEST', 'Payload de configuracao invalido.', 400);

  const allowed = new Set([
    'ota_check_interval_hours',
    'ota_modal_cooldown_hours',
    'feedback_contextual_enabled',
    'feedback_contextual_cooldown_days',
    'maintenance_mode',
    'maintenance_message',
    'telemetry_max_queue_size',
    'min_supported_version_code',
    'active_icon_mode',
  ]);

  const entries = Object.entries(parsed.data).filter(([key]) => allowed.has(key));
  if (entries.length === 0) return fail(c, 'INVALID_REQUEST', 'Nenhuma configuracao permitida informada.', 400);

  await c.env.DB.batch(entries.map(([key, value]) =>
    c.env.DB.prepare(`
      INSERT INTO system_settings(key, value, updated_at)
      VALUES(?, ?, ?)
      ON CONFLICT(key) DO UPDATE SET value = excluded.value, updated_at = excluded.updated_at
    `).bind(key, value, Date.now())
  ));

  await writeAudit(c, 'config_updated', 'remote_config', 'system_settings', Object.fromEntries(entries));

  return ok(c, {
    updated: true,
    keys: entries.map(([key]) => key),
  });
});

async function writeAudit(c: any, action: string, targetType: string, targetId: string, details: unknown) {
  const adminUserId = c.get('adminUserId' as never) as string | number | undefined;
  await c.env.DB.prepare(`
    INSERT INTO audit_logs(admin_user_id, action, target_type, target_id, details)
    VALUES(?, ?, ?, ?, ?)
  `).bind(adminUserId ?? null, action, targetType, targetId, JSON.stringify(details)).run().catch(() => undefined);
}

function percentChange(current: number, previous: number): number | null {
  if (previous === 0) return current > 0 ? 100 : null;
  return round(((current - previous) / previous) * 100, 1);
}

function round(value: number, digits = 0): number {
  const factor = 10 ** digits;
  return Math.round(value * factor) / factor;
}

function fillDailySeries(since: number, days: number, rows: any[], valueKey: string) {
  const byDate = new Map(rows.map((row) => [row.date, Number(row[valueKey] ?? 0)]));
  return Array.from({ length: days }, (_, index) => {
    const d = new Date(since + index * 24 * 60 * 60 * 1000);
    const date = d.toISOString().slice(0, 10);
    return {
      date,
      label: d.toLocaleDateString('pt-BR', { day: '2-digit', month: 'short' }),
      value: byDate.get(date) ?? 0,
    };
  });
}

function fillEventSeries(since: number, days: number, rows: any[]) {
  const grouped = new Map<string, Record<string, number | string>>();
  for (let index = 0; index < days; index += 1) {
    const d = new Date(since + index * 24 * 60 * 60 * 1000);
    const date = d.toISOString().slice(0, 10);
    grouped.set(date, {
      date,
      label: d.toLocaleDateString('pt-BR', { day: '2-digit', month: 'short' }),
      total: 0,
    });
  }

  for (const row of rows) {
    const entry = grouped.get(row.date);
    if (!entry) continue;
    const eventName = row.eventName ?? row.event_name ?? 'unknown';
    const value = Number(row.value ?? row.count ?? 0);
    entry[eventName] = Number(entry[eventName] ?? 0) + value;
    entry.total = Number(entry.total ?? 0) + value;
  }

  return Array.from(grouped.values());
}

function buildAlerts(input: { appHealth: number; feedbackCount: number; avgColdStartMs?: number | null; d7: number }) {
  const alerts = [];
  if (input.appHealth < 98) {
    alerts.push({
      severity: input.appHealth < 90 ? 'critical' : 'warning',
      title: 'Saude do app abaixo do ideal',
      description: `Score operacional atual: ${input.appHealth}%.`,
    });
  }
  if ((input.avgColdStartMs ?? 0) > 2500) {
    alerts.push({
      severity: 'warning',
      title: 'Inicializacao lenta',
      description: `Cold start medio em ${Math.round(input.avgColdStartMs ?? 0)} ms.`,
    });
  }
  if (input.feedbackCount > 20) {
    alerts.push({
      severity: 'info',
      title: 'Volume alto de feedback',
      description: `${input.feedbackCount} feedbacks no periodo selecionado.`,
    });
  }
  if (input.d7 > 0 && input.d7 < 20) {
    alerts.push({
      severity: 'warning',
      title: 'Retencao D7 baixa',
      description: `Retencao D7 em ${input.d7}%.`,
    });
  }
  return alerts;
}
