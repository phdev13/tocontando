import type { MiddlewareHandler } from 'hono';
import type { Env } from '../types';

/** Attaches a unique request ID to every request for structured logging */
export const requestIdMiddleware: MiddlewareHandler<{ Bindings: Env }> = async (c, next) => {
  const requestId = crypto.randomUUID();
  c.set('requestId' as never, requestId);
  c.res.headers.set('X-Request-ID', requestId);
  await next();
};
