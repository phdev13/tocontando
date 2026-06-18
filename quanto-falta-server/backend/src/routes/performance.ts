import { Hono } from 'hono';
import type { Env } from '../types';
import { performanceIngestAuthMiddleware, adminAuthMiddleware } from '../middleware/auth';

export const performanceIngestRoutes = new Hono<{ Bindings: Env }>();
export const performanceAdminRoutes = new Hono<{ Bindings: Env }>();

// ── Ingest Routes (Autenticadas com PERFORMANCE_INGEST_TOKEN) ───────────────

performanceIngestRoutes.use('*', performanceIngestAuthMiddleware);

performanceIngestRoutes.post('/runs', async (c) => {
  const db = c.env.DB;
  let payload: any = null;
  try {
    if (c.req.header('content-encoding') === 'gzip') {
      const decompressed = c.req.raw.body!.pipeThrough(new DecompressionStream('gzip'));
      payload = await new Response(decompressed).json();
    } else {
      payload = await c.req.json();
    }
  } catch (e) {
    return c.json({ error: 'Invalid payload' }, 400);
  }
  
  const {
    id, source, app_version, version_code, commit_sha, branch, build_type,
    benchmark_name, compilation_mode, iterations, device_model, device_manufacturer,
    android_version, api_level, refresh_rate, memory_available_mb, battery_level,
    thermal_status, status, payload_hash
  } = payload;

  try {
    await db.prepare(`
      INSERT INTO performance_runs (
        id, source, app_version, version_code, commit_sha, branch, build_type,
        benchmark_name, compilation_mode, iterations, device_model, device_manufacturer,
        android_version, api_level, refresh_rate, memory_available_mb, battery_level,
        thermal_status, status, payload_hash
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    `).bind(
      id, source, app_version, version_code, commit_sha ?? null, branch ?? null, build_type,
      benchmark_name ?? null, compilation_mode ?? null, iterations ?? null, device_model, device_manufacturer,
      android_version, api_level, refresh_rate ?? null, memory_available_mb ?? null, battery_level ?? null,
      thermal_status ?? null, status, payload_hash
    ).run();

    return c.json({ success: true, id });
  } catch (error: any) {
    if (error.message.includes('UNIQUE constraint failed')) {
      return c.json({ error: 'Duplicate payload', code: 'DUPLICATE' }, 409);
    }
    throw error;
  }
});

performanceIngestRoutes.post('/runs/:runId/metrics', async (c) => {
  const db = c.env.DB;
  const runId = c.req.param('runId');
  let payload: any = null;
  try {
    if (c.req.header('content-encoding') === 'gzip') {
      const decompressed = c.req.raw.body!.pipeThrough(new DecompressionStream('gzip'));
      payload = await new Response(decompressed).json();
    } else {
      payload = await c.req.json();
    }
  } catch (e) {
    return c.json({ error: 'Invalid payload' }, 400);
  }
  const metrics = payload.metrics as any[];

  if (!metrics || metrics.length === 0) return c.json({ success: true });

  const stmts = metrics.map(m => db.prepare(`
    INSERT INTO performance_run_metrics (id, run_id, metric_name, percentile, value, unit)
    VALUES (?, ?, ?, ?, ?, ?)
  `).bind(m.id, runId, m.metric_name, m.percentile ?? null, m.value, m.unit));

  await db.batch(stmts);
  return c.json({ success: true, count: metrics.length });
});

performanceIngestRoutes.post('/runs/:runId/artifacts', async (c) => {
  const db = c.env.DB;
  const r2 = c.env.PERFORMANCE_TRACES;
  const runId = c.req.param('runId');
  
  const formData = await c.req.parseBody();
  const file = formData['file'] as File;
  const artifactType = formData['artifact_type'] as string;
  const id = formData['id'] as string;
  const sha256 = formData['sha256'] as string;

  if (!file || !artifactType || !id) {
    return c.json({ error: 'Missing parameters' }, 400);
  }

  const r2Key = `runs/${runId}/${file.name}`;
  
  await r2.put(r2Key, await file.arrayBuffer(), {
    httpMetadata: { contentType: file.type || 'application/octet-stream' }
  });

  await db.prepare(`
    INSERT INTO performance_artifacts (id, run_id, artifact_type, r2_key, file_name, size_bytes, sha256)
    VALUES (?, ?, ?, ?, ?, ?, ?)
  `).bind(id, runId, artifactType, r2Key, file.name, file.size, sha256 || '').run();

  return c.json({ success: true, r2_key: r2Key });
});

performanceIngestRoutes.post('/runs/:runId/complete', async (c) => {
  const db = c.env.DB;
  const runId = c.req.param('runId');
  const payload = await c.req.json();

  await db.prepare(`UPDATE performance_runs SET status = ? WHERE id = ?`)
    .bind(payload.status || 'COMPLETED', runId)
    .run();

  return c.json({ success: true });
});

// ── Admin Routes (Autenticadas com Cloudflare Access) ───────────────────────

performanceAdminRoutes.use('*', adminAuthMiddleware);

performanceAdminRoutes.get('/summary', async (c) => {
  const db = c.env.DB;
  
  const totalRuns = await db.prepare(`SELECT COUNT(*) as count FROM performance_runs`).first('count');
  const totalRegressions = await db.prepare(`SELECT COUNT(*) as count FROM performance_regressions WHERE status = 'OPEN'`).first('count');
  
  // Exemplo de agregação de cold startup
  const coldStartupP50 = await db.prepare(`
    SELECT AVG(value) as avg_val FROM performance_run_metrics 
    WHERE metric_name = 'timeToInitialDisplayMs' AND percentile = 'p50'
    ORDER BY created_at DESC LIMIT 50
  `).first('avg_val');

  return c.json({
    total_runs: totalRuns,
    open_regressions: totalRegressions,
    avg_cold_startup_p50: coldStartupP50 || 0
  });
});

performanceAdminRoutes.get('/runs', async (c) => {
  const db = c.env.DB;
  const page = Number(c.req.query('page') || 1);
  const limit = Number(c.req.query('limit') || 20);
  const offset = (page - 1) * limit;
  const source = c.req.query('source');

  let query = `SELECT * FROM performance_runs`;
  let params: any[] = [];
  
  if (source) {
    query += ` WHERE source = ?`;
    params.push(source);
  }
  
  query += ` ORDER BY created_at DESC LIMIT ? OFFSET ?`;
  params.push(limit, offset);

  const { results } = await db.prepare(query).bind(...params).all();
  return c.json({ runs: results });
});

performanceAdminRoutes.get('/runs/:runId', async (c) => {
  const db = c.env.DB;
  const runId = c.req.param('runId');

  const run = await db.prepare(`SELECT * FROM performance_runs WHERE id = ?`).bind(runId).first();
  if (!run) return c.json({ error: 'Run not found' }, 404);

  const { results: metrics } = await db.prepare(`SELECT * FROM performance_run_metrics WHERE run_id = ?`).bind(runId).all();
  const { results: artifacts } = await db.prepare(`SELECT * FROM performance_artifacts WHERE run_id = ?`).bind(runId).all();

  return c.json({ run, metrics, artifacts });
});

performanceAdminRoutes.get('/artifacts/:artifactId/download', async (c) => {
  const db = c.env.DB;
  const r2 = c.env.PERFORMANCE_TRACES;
  const artifactId = c.req.param('artifactId');

  const artifact = await db.prepare(`SELECT * FROM performance_artifacts WHERE id = ?`).bind(artifactId).first();
  if (!artifact) return c.json({ error: 'Artifact not found' }, 404);

  const r2Key = artifact.r2_key as string;
  const object = await r2.get(r2Key);
  
  if (!object) return c.json({ error: 'Object not found in R2' }, 404);

  const headers = new Headers();
  object.writeHttpMetadata(headers);
  headers.set('etag', object.httpEtag);
  headers.set('Content-Disposition', `attachment; filename="${artifact.file_name}"`);

  return new Response(object.body, { headers });
});

/** GET /screens — slowest screens / scroll issues */
performanceAdminRoutes.get('/screens', async (c) => {
  const days = Math.min(parseInt(c.req.query('days') ?? '14', 10), 90);
  const since = Date.now() - days * 24 * 60 * 60 * 1000;

  const { results } = await c.env.DB.prepare(`
    SELECT
      screen,
      AVG(CASE WHEN metric_type = 'cold_start' THEN value_ms ELSE NULL END) as avg_startup,
      SUM(CASE WHEN metric_type = 'slow_frame' THEN value_ms ELSE 0 END) as slow_frames_sum,
      COUNT(DISTINCT installation_id) as affected_sessions,
      MAX(version_code) as last_version
    FROM performance_metrics
    WHERE created_at > ? AND screen IS NOT NULL
    GROUP BY screen
    ORDER BY avg_startup DESC
    LIMIT 20
  `).bind(since).all();

  return c.json({ screens: results });
});

/** GET /errors — errors overview */
performanceAdminRoutes.get('/errors', async (c) => {
  const { results } = await c.env.DB.prepare(`
    SELECT
      error_type as code,
      error_message as module,
      COUNT(*) as count,
      MAX(version_code) as latest_version,
      MAX(created_at) as last_seen
    FROM crash_reports
    GROUP BY error_type, error_message
    ORDER BY count DESC
    LIMIT 20
  `).all();
  return c.json({ errors: results });
});

/** GET /devices — device tier breakdown */
performanceAdminRoutes.get('/devices', async (c) => {
  const { results } = await c.env.DB.prepare(`
    SELECT
      model,
      android_version,
      COUNT(DISTINCT installation_id) as sessions,
      AVG(value_ms) as avg_cold_start
    FROM performance_metrics
    WHERE metric_type = 'cold_start'
    GROUP BY model, android_version
    ORDER BY sessions DESC
    LIMIT 15
  `).all();
  return c.json({ devices: results });
});

/** GET /releases — comparison */
performanceAdminRoutes.get('/releases', async (c) => {
  const { results } = await c.env.DB.prepare(`
    SELECT
      version_code,
      COUNT(DISTINCT installation_id) as adoptions,
      AVG(CASE WHEN metric_type = 'cold_start' THEN value_ms ELSE NULL END) as avg_startup
    FROM performance_metrics
    GROUP BY version_code
    ORDER BY version_code DESC
    LIMIT 5
  `).all();
  return c.json({ releases: results });
});
