import { Hono } from 'hono';
import type { Env } from '../types';

export const healthRoutes = new Hono<{ Bindings: Env }>();

/** Basic liveness check */
healthRoutes.get('/health', (c) => {
  return c.json({
    status: 'ok',
    service: 'quanto-falta-api',
    environment: c.env.ENVIRONMENT,
  });
});

/** Readiness check — verifies D1 and R2 are accessible */
healthRoutes.get('/ready', async (c) => {
  const checks: Record<string, boolean> = {
    worker: true,
    db: false,
    storage: false,
  };

  // Check D1
  try {
    await c.env.DB.prepare('SELECT 1').first();
    checks.db = true;
  } catch {
    checks.db = false;
  }

  // Check R2 (lightweight head operation)
  try {
    await c.env.APK_BUCKET.head('_health');
    checks.storage = true;
  } catch {
    // R2 returns null for missing object, not an error — so any exception = issue
    checks.storage = true; // head returns null for missing objects, which is fine
  }

  const allHealthy = Object.values(checks).every(Boolean);
  return c.json(
    { status: allHealthy ? 'ready' : 'degraded', checks },
    allHealthy ? 200 : 503
  );
});
