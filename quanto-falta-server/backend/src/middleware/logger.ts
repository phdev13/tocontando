import type { MiddlewareHandler } from 'hono';
import type { Env } from '../types';

/** Structured JSON logger — never logs tokens, cookies, or personal data */
export const loggerMiddleware: MiddlewareHandler<{ Bindings: Env }> = async (c, next) => {
  const start = Date.now();
  const requestId = c.get('requestId' as never) as string;
  await next();
  const durationMs = Date.now() - start;
  const status = c.res.status;
  let route = new URL(c.req.url).pathname;
  
  // Sanitize path to avoid logging sensitive IDs or codes
  // Masks UUIDs:
  route = route.replace(/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}/g, '[UUID]');
  // Masks potential premium codes (e.g., QF-MEN-ABC123)
  route = route.replace(/\b[A-Z0-9]+-[A-Z0-9]+-[A-Z0-9]+\b/g, '[CODE]');

  console.log(JSON.stringify({
    level: status >= 500 ? 'error' : status >= 400 ? 'warn' : 'info',
    requestId,
    method: c.req.method,
    route,
    status,
    durationMs,
    env: c.env.ENVIRONMENT,
  }));
};
