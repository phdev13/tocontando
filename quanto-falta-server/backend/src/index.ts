import { Hono } from 'hono';
import { cors } from 'hono/cors';
import { secureHeaders } from 'hono/secure-headers';
import type { Env } from './types';
import { requestIdMiddleware } from './middleware/requestId';
import { rateLimitMiddleware } from './middleware/rateLimit';
import { loggerMiddleware } from './middleware/logger';
import { healthRoutes } from './routes/health';
import { updatesRoutes } from './routes/updates';
import { analyticsRoutes } from './routes/analytics';
import { feedbackRoutes } from './routes/feedback';
import { installationsRoutes } from './routes/installations';
import { adminRoutes } from './routes/admin';
import { configRoutes } from './routes/config';
import { apkRoutes } from './routes/apk';
import { testersRoutes } from './routes/testers';
import { monetizationRoutes } from './routes/monetization';
import { syncRoutes } from './routes/sync';
import { authRoutes } from './routes/auth';
import { uploadRoutes } from './routes/upload';
import { apiV1Routes } from './modules/apiV1';

import { performanceIngestRoutes, performanceAdminRoutes } from './routes/performance';

const app = new Hono<{ Bindings: Env }>();

// ── Security headers ────────────────────────────────────────
app.use('*', secureHeaders({
  strictTransportSecurity: 'max-age=63072000; includeSubDomains; preload',
  xFrameOptions: 'DENY',
  xXssProtection: '1; mode=block',
  contentSecurityPolicy: {
    defaultSrc: ["'none'"],
    baseUri: ["'none'"],
    fontSrc: ["'none'"],
    formAction: ["'none'"],
    frameAncestors: ["'none'"],
    imgSrc: ["'none'"],
    objectSrc: ["'none'"],
    scriptSrc: ["'none'"], 
    styleSrc: ["'none'"],
    upgradeInsecureRequests: [],
  },
  crossOriginEmbedderPolicy: 'require-corp',
  crossOriginOpenerPolicy: 'same-origin',
  crossOriginResourcePolicy: 'same-site',
}));

// Remove server fingerprinting headers
app.use('*', async (c, next) => {
  await next();
  c.res.headers.delete('X-Powered-By');
});

// ── CORS: public API endpoints allow any origin (read-only data)
//    Admin routes are restricted to the dashboard domain
function resolveAdminOrigin(origin: string | undefined) {
  if (!origin) return null;

  const allowedExactOrigins = new Set([
    'https://admin.tocontando.com.br',
    'https://tocontando.com.br',
    'https://share.tocontando.com.br',
    'https://www.tocontando.com.br',
    'https://admin.quantofalta.shop',
    'https://quantofalta.shop',
    'https://share.quantofalta.shop',
    'https://www.quantofalta.shop',
    'https://quanto-falta-dashboard.pages.dev',
    'https://quanto-falta-web.pages.dev',
    'http://localhost:5173',
  ]);

  try {
    const { hostname, protocol } = new URL(origin);
    const isHttpsPagesPreview = protocol === 'https:' && (
      hostname.endsWith('.quanto-falta-dashboard.pages.dev') ||
      hostname.endsWith('.quanto-falta-web.pages.dev')
    );

    if (allowedExactOrigins.has(origin) || isHttpsPagesPreview) return origin;
  } catch {
    return null;
  }

  return null;
}

app.use('/api/v1/admin/*', cors({
  origin: resolveAdminOrigin,
  allowMethods: ['GET', 'POST', 'PUT', 'PATCH', 'DELETE', 'OPTIONS'],
  allowHeaders: ['Content-Type', 'X-CSRF-Token', 'Authorization', 'Cf-Access-Jwt-Assertion'],
  credentials: true,
  maxAge: 3600,
}));

app.use('/api/v1/public/*', cors({
  origin: '*',
  allowMethods: ['GET', 'POST', 'PATCH', 'OPTIONS'],
  allowHeaders: ['Content-Type', 'X-Request-ID', 'If-None-Match'],
  maxAge: 86400,
}));

app.use('/api/v1/app/*', cors({
  origin: '*',
  allowMethods: ['GET', 'POST', 'PATCH', 'OPTIONS'],
  allowHeaders: ['Content-Type', 'X-Request-ID', 'If-None-Match'],
  maxAge: 86400,
}));

app.use('/api/v1/stats/*', cors({
  origin: '*',
  allowMethods: ['GET', 'OPTIONS'],
  allowHeaders: ['Content-Type', 'X-Request-ID', 'If-None-Match'],
  maxAge: 86400,
}));

app.use('/api/v1/performance/*', cors({
  origin: '*',
  allowMethods: ['GET', 'POST', 'PATCH', 'OPTIONS'],
  allowHeaders: ['Content-Type', 'Authorization', 'X-Request-ID'],
  maxAge: 86400,
}));

app.use('/admin/*', cors({
  origin: (origin) => {
    if (!origin) return null;

    const allowedExactOrigins = new Set([
      'https://admin.tocontando.com.br',
      'https://tocontando.com.br',
      'https://share.tocontando.com.br',
      'https://www.tocontando.com.br',
      'https://admin.quantofalta.shop',
      'https://quantofalta.shop',
      'https://share.quantofalta.shop',
      'https://www.quantofalta.shop',
      'https://quanto-falta-dashboard.pages.dev',
      'https://quanto-falta-web.pages.dev',
      'http://localhost:5173',
    ]);

    try {
      const { hostname, protocol } = new URL(origin);
      const isHttpsPagesPreview = protocol === 'https:' && (
        hostname.endsWith('.quanto-falta-dashboard.pages.dev') ||
        hostname.endsWith('.quanto-falta-web.pages.dev')
      );

      if (allowedExactOrigins.has(origin) || isHttpsPagesPreview) return origin;
    } catch {
      return null;
    }

    return null;
  },
  allowMethods: ['GET', 'POST', 'PUT', 'PATCH', 'DELETE', 'OPTIONS'],
  allowHeaders: ['Content-Type', 'X-CSRF-Token', 'Authorization', 'Cf-Access-Jwt-Assertion'],
  credentials: true,
  maxAge: 3600,
}));

// Disable cache for all admin routes
app.use('/admin/*', async (c, next) => {
  await next();
  c.res.headers.set('Cache-Control', 'no-store, no-cache, must-revalidate, proxy-revalidate');
  c.res.headers.set('Pragma', 'no-cache');
  c.res.headers.set('Expires', '0');
  c.res.headers.set('Surrogate-Control', 'no-store');
});

app.use('/api/v1/admin/*', async (c, next) => {
  await next();
  c.res.headers.set('Cache-Control', 'no-store, no-cache, must-revalidate, proxy-revalidate');
  c.res.headers.set('Pragma', 'no-cache');
  c.res.headers.set('Expires', '0');
  c.res.headers.set('Surrogate-Control', 'no-store');
});

// ── Global middleware ───────────────────────────────────────
app.use('*', requestIdMiddleware);
app.use('*', loggerMiddleware);

// Rate Limits Rigorosos
const redeemRateLimit = rateLimitMiddleware({ maxRequests: 5, windowSeconds: 600 }); // 5 por 10 min
const downloadRateLimit = rateLimitMiddleware({ maxRequests: 10, windowSeconds: 60 }); // 10 por 1 min
const batchRateLimit = rateLimitMiddleware({ maxRequests: 30, windowSeconds: 60 }); // 30 por 1 min

// ── Routes ──────────────────────────────────────────────────
app.route('/', healthRoutes);

app.route('/api/v1', apiV1Routes);

app.use('/api/v1/updates/*', downloadRateLimit);
app.route('/api/v1', updatesRoutes);

app.use('/api/v1/analytics/batch', batchRateLimit);
app.use('/api/v1/app/telemetry*', batchRateLimit);
app.route('/api/v1', analyticsRoutes);

app.route('/api/v1', feedbackRoutes);
app.route('/api/v1', installationsRoutes);
app.route('/api/v1', configRoutes);
app.route('/api/v1', authRoutes);
app.route('/api/v1', syncRoutes);
app.route('/api/v1', uploadRoutes);

app.use('/api/v1/apk/download', downloadRateLimit);
app.route('/api/v1', apkRoutes);

app.route('/api/v1', testersRoutes);

app.use('/api/v1/monetization/redeem', redeemRateLimit);
app.route('/api/v1', monetizationRoutes);
app.route('/admin', adminRoutes);

app.route('/api/v1/performance', performanceIngestRoutes);
app.route('/api/v1/admin/performance', performanceAdminRoutes);

// ── 404 handler ─────────────────────────────────────────────
app.notFound((c) => {
  return c.json({ error: 'Not found', code: 'NOT_FOUND' }, 404);
});

// ── Error handler ───────────────────────────────────────────
app.onError((err, c) => {
  const requestId = c.get('requestId' as never) as string | undefined;
  console.error(JSON.stringify({
    level: 'error',
    requestId,
    message: err.message,
    // Never log stack traces containing personal data
  }));
  return c.json({ error: 'Internal server error', code: 'INTERNAL_ERROR' }, 500);
});

export default {
  fetch: app.fetch,
  async scheduled(event: any, env: Env, ctx: ExecutionContext) {
    const thirtyDaysAgo = Date.now() - 30 * 24 * 60 * 60 * 1000;
    
    try {
      // Deletar traces antigos do R2
      const oldArtifacts = await env.DB.prepare(
        `SELECT r2_key FROM performance_artifacts WHERE created_at < ?`
      ).bind(thirtyDaysAgo).all();
      
      for (const row of oldArtifacts.results) {
        if (row.r2_key) await env.PERFORMANCE_TRACES.delete(row.r2_key as string);
      }
      
      // Deletar do DB
      await env.DB.prepare(`DELETE FROM performance_artifacts WHERE created_at < ?`).bind(thirtyDaysAgo).run();
      await env.DB.prepare(`DELETE FROM performance_runs WHERE created_at < ?`).bind(thirtyDaysAgo).run();
      await env.DB.prepare(`DELETE FROM performance_metrics WHERE created_at < ?`).bind(thirtyDaysAgo).run();
    } catch (e) {
      console.error("Cleanup job failed", e);
    }
  }
};
