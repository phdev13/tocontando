import type { MiddlewareHandler } from 'hono';
import * as jose from 'jose';
import type { Env } from '../types';

let JWKS: ReturnType<typeof jose.createRemoteJWKSet> | null = null;

/**
 * Cloudflare Access JWT validation middleware.
 * Expects Cf-Access-Jwt-Assertion header or cookie.
 * Validates cryptographic signature, expiration, issuer, audience and allowed email.
 */
export const adminAuthMiddleware: MiddlewareHandler<{ Bindings: Env }> = async (c, next) => {
  // Cloudflare Access passes the JWT in the Cf-Access-Jwt-Assertion header.
  // It may also be present as a cookie named CF_Authorization.
  const authHeader = c.req.header('Cf-Access-Jwt-Assertion');
  const authCookie = getCookie(c.req.header('Cookie') ?? '', 'CF_Authorization');
  const bearerHeader = c.req.header('Authorization')?.replace('Bearer ', '');

  const token = authHeader || authCookie || bearerHeader;

  if (!token) {
    return c.json({ error: 'Unauthorized: Missing Cloudflare Access Token', code: 'AUTH_REQUIRED' }, 401);
  }

  const teamDomain = c.env.CF_ACCESS_TEAM_DOMAIN;
  const audience = c.env.CF_ACCESS_AUDIENCE;

  if (!teamDomain || !audience) {
    console.error('Missing CF_ACCESS_TEAM_DOMAIN or CF_ACCESS_AUDIENCE environment variables');
    return c.json({ error: 'Server configuration error', code: 'SERVER_ERROR' }, 500);
  }

  const certsUrl = new URL('/cdn-cgi/access/certs', teamDomain).toString();

  // Initialize JWKS with caching
  if (!JWKS) {
    JWKS = jose.createRemoteJWKSet(new URL(certsUrl));
  }

  try {
    const { payload } = await jose.jwtVerify(token, JWKS, {
      issuer: teamDomain,
      audience: audience,
    });

    const email = payload.email as string;

    if (email !== 'philippeboechat1@gmail.com') {
      return c.json({ error: 'Forbidden: Unauthorized user', code: 'FORBIDDEN' }, 403);
    }

    c.set('adminUserId' as never, email); // use email as ID since we removed the DB admin users
    c.set('adminUsername' as never, email.split('@')[0]);

    await next();
  } catch (err) {
    console.error('JWT validation failed:', err);
    return c.json({ error: 'Unauthorized: Invalid token', code: 'AUTH_FAILED' }, 401);
  }
};

function getCookie(cookieHeader: string, name: string): string | null {
  const match = cookieHeader.match(new RegExp(`(?:^|;\\s*)${name}=([^;]*)`));
  return match ? decodeURIComponent(match[1]) : null;
}

/**
 * Middleware for validating Performance Ingestion endpoints.
 * Expects Authorization: Bearer <PERFORMANCE_INGEST_TOKEN>.
 */
export const performanceIngestAuthMiddleware: MiddlewareHandler<{ Bindings: Env }> = async (c, next) => {
  const bearerHeader = c.req.header('Authorization')?.replace('Bearer ', '');
  const ingestToken = c.env.PERFORMANCE_INGEST_TOKEN;

  if (!ingestToken) {
    console.error('Missing PERFORMANCE_INGEST_TOKEN environment variable');
    return c.json({ error: 'Server configuration error', code: 'SERVER_ERROR' }, 500);
  }

  if (!bearerHeader || bearerHeader !== ingestToken) {
    return c.json({ error: 'Unauthorized: Invalid or missing ingest token', code: 'AUTH_FAILED' }, 401);
  }

  await next();
};
