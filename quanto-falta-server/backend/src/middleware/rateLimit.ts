import type { MiddlewareHandler } from 'hono';
import type { Env } from '../types';

interface RateLimitConfig {
  maxRequests: number;
  windowSeconds: number;
}

/**
 * D1-backed rate limiter.
 * Uses a sliding window approach keyed by hashed IP + Route + Salt.
 * Never stores raw IP addresses.
 */
export const rateLimitMiddleware = (config: RateLimitConfig): MiddlewareHandler<{ Bindings: Env }> => {
  return async (c, next) => {
    const ip = c.req.header('CF-Connecting-IP') ?? c.req.header('X-Forwarded-For') ?? 'unknown';
    const route = new URL(c.req.url).pathname;
    
    // Fallback salt se não configurado
    const salt = c.env.RATE_LIMIT_SALT || 'default_salt_please_change';

    const ipHash = await hashString(ip + route + salt);
    const now = Math.floor(Date.now() / 1000);
    const expiresAt = now + config.windowSeconds;

    try {
      const db = c.env.DB;

      // 1. Limpeza exporádica de limites vencidos para não inchar o DB. (5% de chance)
      if (Math.random() < 0.05) {
        c.executionCtx.waitUntil(
            db.prepare('DELETE FROM rate_limits WHERE expires_at < ?').bind(now).run()
        );
      }

      // 2. Tenta incrementar a contagem. 
      // Se a linha não existe, o UPSERT cria ela (via INSERT ... ON CONFLICT).
      // Mas o SQLite padrão usa INSERT ... ON CONFLICT(hash_key) DO UPDATE ...
      // Precisamos garantir que, se o expire passou, resetamos a contagem para 1 e jogamos o expiresAt pra frente.
      
      const { results } = await db.prepare(`
        INSERT INTO rate_limits (hash_key, request_count, expires_at)
        VALUES (?, 1, ?)
        ON CONFLICT(hash_key) DO UPDATE SET
          request_count = CASE WHEN rate_limits.expires_at < ? THEN 1 ELSE rate_limits.request_count + 1 END,
          expires_at = CASE WHEN rate_limits.expires_at < ? THEN ? ELSE rate_limits.expires_at END
        RETURNING request_count, expires_at
      `).bind(ipHash, expiresAt, now, now, expiresAt).all();

      if (results && results.length > 0) {
        const row = results[0] as { request_count: number; expires_at: number };
        
        if (row.request_count > config.maxRequests) {
          const retryAfter = row.expires_at - now;
          return c.json(
            { error: 'Too many requests', code: 'RATE_LIMITED', retryAfter: Math.max(retryAfter, 1) },
            429,
            { 'Retry-After': String(Math.max(retryAfter, 1)) }
          );
        }
      }
    } catch (e) {
      console.error('Rate limit checking failed, failing open.', e);
      // Fail open for availability
    }

    await next();
  };
};

async function hashString(input: string): Promise<string> {
  const encoder = new TextEncoder();
  const data = encoder.encode(input);
  const hashBuffer = await crypto.subtle.digest('SHA-256', data);
  const hashArray = Array.from(new Uint8Array(hashBuffer));
  return hashArray.map((b) => b.toString(16).padStart(2, '0')).join('').slice(0, 16);
}
