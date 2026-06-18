import { Hono } from 'hono';
import type { Env } from '../../types';

export const adminMetricsRoutes = new Hono<{ Bindings: Env }>();

/** GET /admin/metrics/overview — main dashboard numbers */
adminMetricsRoutes.get('/overview', async (c) => {
  const daysParam = c.req.query('days');
  let days = 30;
  if (daysParam !== undefined) {
    const parsed = parseInt(daysParam, 10);
    if (isNaN(parsed)) return c.json({ error: 'Parâmetro days inválido. Deve ser um número inteiro.' }, 400);
    days = Math.max(1, Math.min(parsed, 365));
  }
  const since = Date.now() - days * 24 * 60 * 60 * 1000;

  const [
    totalInstallations,
    activeInstallations,
    newInstallations,
    versionDistribution,
    avgColdStart,
    otaStats,
    feedbackStats,
    eventsStats,
    retentionD1,
    retentionD7,
    retentionD30,
    syncUsers,
    syncDevices,
    syncEvents,
  ] = await Promise.all([
    c.env.DB.prepare('SELECT COUNT(*) as count FROM installations').first<{ count: number }>(),
    c.env.DB.prepare(`SELECT COUNT(*) as count FROM installations WHERE last_seen_at >= ?`).bind(since).first<{ count: number }>(),
    c.env.DB.prepare(`SELECT COUNT(*) as count FROM installations WHERE first_seen_at >= ?`).bind(since).first<{ count: number }>(),
    c.env.DB.prepare(`SELECT version_code, version_name, COUNT(*) as count FROM installations WHERE last_seen_at >= ? GROUP BY version_code, version_name ORDER BY version_code DESC LIMIT 10`).bind(since).all(),
    c.env.DB.prepare(`SELECT AVG(value_ms) as avg FROM performance_metrics WHERE metric_type = 'cold_start' AND created_at >= ?`).bind(since).first<{ avg: number }>(),
    c.env.DB.prepare(`SELECT
      COUNT(CASE WHEN event_type = 'download_started' THEN 1 END) as downloads_started,
      COUNT(CASE WHEN event_type = 'download_completed' THEN 1 END) as downloads_completed,
      COUNT(CASE WHEN event_type = 'adopted' THEN 1 END) as adopted,
      COUNT(CASE WHEN event_type = 'failed' THEN 1 END) as failed
    FROM ota_attempts WHERE created_at >= ?`).bind(since).first(),
    c.env.DB.prepare(`SELECT COUNT(*) as count, AVG(CASE WHEN rating IS NOT NULL THEN rating ELSE NULL END) as avg_rating FROM feedback WHERE created_at >= ?`).bind(since).first<{ count: number; avg_rating: number }>(),
    c.env.DB.prepare(`SELECT COUNT(*) as count FROM analytics_events WHERE event_name = 'event_created' AND created_at >= ?`).bind(since).first<{ count: number }>(),
    // Retention queries
    c.env.DB.prepare(`SELECT COUNT(*) as count FROM installations WHERE first_seen_at >= ? AND last_seen_at >= first_seen_at + 86400000`).bind(since).first<{ count: number }>(),
    c.env.DB.prepare(`SELECT COUNT(*) as count FROM installations WHERE first_seen_at >= ? AND last_seen_at >= first_seen_at + 604800000`).bind(since).first<{ count: number }>(),
    c.env.DB.prepare(`SELECT COUNT(*) as count FROM installations WHERE first_seen_at >= ? AND last_seen_at >= first_seen_at + 2592000000`).bind(since).first<{ count: number }>(),
    // Sync queries
    c.env.DB.prepare('SELECT COUNT(*) as count FROM users').first<{ count: number }>(),
    c.env.DB.prepare('SELECT COUNT(*) as count FROM devices').first<{ count: number }>(),
    c.env.DB.prepare('SELECT COUNT(*) as count FROM sync_events WHERE deleted_at IS NULL').first<{ count: number }>(),
  ]);

  return c.json({
    period: { days, since: new Date(since).toISOString() },
    installations: {
      total: totalInstallations?.count ?? 0,
      active: activeInstallations?.count ?? 0,
      new: newInstallations?.count ?? 0,
    },
    sync: {
      users: syncUsers?.count ?? 0,
      devices: syncDevices?.count ?? 0,
      cloudEvents: syncEvents?.count ?? 0,
    },
    eventsCreated: eventsStats?.count ?? 0,
    versionDistribution: versionDistribution.results,
    performance: {
      avgColdStartMs: avgColdStart?.avg ? Math.round(avgColdStart.avg) : null,
    },
    ota: otaStats,
    feedback: {
      count: feedbackStats?.count ?? 0,
      averageRating: feedbackStats?.avg_rating ? Math.round(feedbackStats.avg_rating * 10) / 10 : null,
    },
    retention: {
      d1: retentionD1?.count ?? 0,
      d7: retentionD7?.count ?? 0,
      d30: retentionD30?.count ?? 0,
      baseInstallations: newInstallations?.count ?? 0,
    },
    generatedAt: new Date().toISOString(),
  });
});

/** GET /admin/metrics/events — event counts over time */
adminMetricsRoutes.get('/events', async (c) => {
  const daysParam = c.req.query('days');
  let days = 7;
  if (daysParam !== undefined) {
    const parsed = parseInt(daysParam, 10);
    if (isNaN(parsed)) return c.json({ error: 'Parâmetro days inválido. Deve ser um número inteiro.' }, 400);
    days = Math.max(1, Math.min(parsed, 365));
  }
  const since = Date.now() - days * 24 * 60 * 60 * 1000;
  const eventName = c.req.query('event');

  let query = `SELECT
    date(created_at / 1000, 'unixepoch') as date,
    event_name,
    COUNT(*) as count
  FROM analytics_events
  WHERE created_at > ?`;
  const params: (string | number)[] = [since];
  if (eventName) { query += ' AND event_name = ?'; params.push(eventName); }
  query += ' GROUP BY date, event_name ORDER BY date DESC';

  const { results } = await c.env.DB.prepare(query).bind(...params).all();
  return c.json({ events: results });
});

/** GET /admin/metrics/performance — performance trends */
adminMetricsRoutes.get('/performance', async (c) => {
  const daysParam = c.req.query('days');
  let days = 14;
  if (daysParam !== undefined) {
    const parsed = parseInt(daysParam, 10);
    if (isNaN(parsed)) return c.json({ error: 'Parâmetro days inválido. Deve ser um número inteiro.' }, 400);
    days = Math.max(1, Math.min(parsed, 365));
  }
  const since = Date.now() - days * 24 * 60 * 60 * 1000;

  const { results } = await c.env.DB.prepare(`
    SELECT
      date(created_at / 1000, 'unixepoch') as date,
      metric_type,
      AVG(value_ms) as avg_ms,
      MIN(value_ms) as min_ms,
      MAX(value_ms) as max_ms,
      COUNT(*) as samples
    FROM performance_metrics
    WHERE created_at > ?
    GROUP BY date, metric_type
    ORDER BY date DESC
  `).bind(since).all();

  return c.json({ performance: results });
});

