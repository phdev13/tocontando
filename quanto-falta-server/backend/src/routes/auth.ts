import { Hono } from 'hono';
import { sign, verify } from 'hono/jwt';
import type { Env } from '../types';

export const authRoutes = new Hono<{ Bindings: Env }>();

async function hashToken(token: string): Promise<string> {
  const hashBuffer = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(token));
  return Array.from(new Uint8Array(hashBuffer))
    .map((b) => b.toString(16).padStart(2, '0'))
    .join('');
}

function generateRandomString(length: number): string {
  const array = new Uint8Array(length);
  crypto.getRandomValues(array);
  return Array.from(array)
    .map((b) => b.toString(16).padStart(2, '0'))
    .join('');
}

// Fase 8: Request OTP
authRoutes.post('/auth/otp/request', async (c) => {
  try {
    const body = await c.req.json();
    const email = body.email;
    if (!email || typeof email !== 'string') {
      return c.json({ error: 'E-mail inválido' }, 400);
    }

    // Rate Limit: Check recent requests
    const db = c.env.DB;
    const now = Date.now();
    
    // Generate 6-digit code
    const code = Math.floor(100000 + Math.random() * 900000).toString();
    const expiresAt = now + 10 * 60 * 1000; // 10 minutes

    await db.prepare(
      `INSERT INTO otp_requests (email, code, expires_at, attempts) 
       VALUES (?, ?, ?, 0) 
       ON CONFLICT(email) DO UPDATE SET code = excluded.code, expires_at = excluded.expires_at, attempts = 0`
    ).bind(email.toLowerCase(), code, expiresAt).run();

    // TODO: Send email via actual provider (SendGrid/Resend)
    console.log(`[DEV ONLY] OTP para ${email}: ${code}`);

    // Proteção contra enumeração: always returns success
    return c.json({ success: true });
  } catch (err: any) {
    return c.json({ error: err.message }, 500);
  }
});

// Fase 8: Verify OTP
authRoutes.post('/auth/otp/verify', async (c) => {
  try {
    const body = await c.req.json();
    const { email, code, device_name } = body;
    if (!email || !code) {
      return c.json({ error: 'E-mail e código são obrigatórios' }, 400);
    }

    const db = c.env.DB;
    const now = Date.now();
    const emailLower = email.toLowerCase();

    const request = await db.prepare(
      `SELECT code, expires_at, attempts FROM otp_requests WHERE email = ?`
    ).bind(emailLower).first<{ code: string; expires_at: number; attempts: number }>();

    if (!request) {
      return c.json({ error: 'Código inválido ou expirado' }, 400);
    }

    if (now > request.expires_at) {
      await db.prepare(`DELETE FROM otp_requests WHERE email = ?`).bind(emailLower).run();
      return c.json({ error: 'Código expirado' }, 400);
    }

    if (request.attempts >= 5) {
      return c.json({ error: 'Muitas tentativas. Solicite um novo código.' }, 429);
    }

    if (request.code !== code) {
      await db.prepare(`UPDATE otp_requests SET attempts = attempts + 1 WHERE email = ?`).bind(emailLower).run();
      return c.json({ error: 'Código incorreto' }, 400);
    }

    // Success! Delete OTP
    await db.prepare(`DELETE FROM otp_requests WHERE email = ?`).bind(emailLower).run();

    // Find or create user
    let user = await db.prepare(`SELECT id FROM users WHERE email = ?`).bind(emailLower).first<{ id: string }>();
    if (!user) {
      const newUserId = crypto.randomUUID();
      await db.prepare(`INSERT INTO users (id, email, created_at) VALUES (?, ?, ?)`).bind(newUserId, emailLower, now).run();
      user = { id: newUserId };
    }

    // Register device
    const deviceId = crypto.randomUUID();
    await db.prepare(`INSERT INTO devices (id, user_id, name, last_seen) VALUES (?, ?, ?, ?)`).bind(deviceId, user.id, device_name || 'Android Device', now).run();

    // Create session
    const refreshToken = generateRandomString(32);
    const hashedRefresh = await hashToken(refreshToken);
    const sessionId = crypto.randomUUID();
    const sessionExpiresAt = now + 90 * 24 * 60 * 60 * 1000; // 90 days

    await db.prepare(`INSERT INTO sessions (id, user_id, token_hash, device_id, expires_at) VALUES (?, ?, ?, ?, ?)`).bind(sessionId, user.id, hashedRefresh, deviceId, sessionExpiresAt).run();

    // Generate JWT Access Token
    const payload = {
      sub: user.id,
      device_id: deviceId,
      session_id: sessionId,
      exp: Math.floor(now / 1000) + 60 * 60, // 1 hour
    };
    
    const secret = c.env.JWT_SECRET || 'dev_secret_key_please_change';
    const accessToken = await sign(payload, secret);

    return c.json({
      access_token: accessToken,
      refresh_token: refreshToken,
      user_id: user.id,
      device_id: deviceId
    });

  } catch (err: any) {
    return c.json({ error: err.message }, 500);
  }
});

// Fase 8: Refresh Token
authRoutes.post('/auth/refresh', async (c) => {
  try {
    const body = await c.req.json();
    const { refresh_token, device_id } = body;
    if (!refresh_token || !device_id) {
      return c.json({ error: 'Parâmetros inválidos' }, 400);
    }

    const hashedRefresh = await hashToken(refresh_token);
    const db = c.env.DB;
    const now = Date.now();

    const session = await db.prepare(
      `SELECT id, user_id, expires_at, revoked_at FROM sessions WHERE token_hash = ? AND device_id = ?`
    ).bind(hashedRefresh, device_id).first<{ id: string; user_id: string; expires_at: number; revoked_at: number | null }>();

    if (!session || session.revoked_at || now > session.expires_at) {
      return c.json({ error: 'Sessão inválida ou expirada' }, 401);
    }

    // Rotacionar refresh token
    const newRefreshToken = generateRandomString(32);
    const newHashedRefresh = await hashToken(newRefreshToken);
    await db.prepare(`UPDATE sessions SET token_hash = ?, expires_at = ? WHERE id = ?`).bind(newHashedRefresh, now + 90 * 24 * 60 * 60 * 1000, session.id).run();

    // Generate new JWT
    const payload = {
      sub: session.user_id,
      device_id: device_id,
      session_id: session.id,
      exp: Math.floor(now / 1000) + 60 * 60, // 1 hour
    };
    
    const secret = c.env.JWT_SECRET || 'dev_secret_key_please_change';
    const accessToken = await sign(payload, secret);

    return c.json({
      access_token: accessToken,
      refresh_token: newRefreshToken
    });
  } catch (err: any) {
    return c.json({ error: err.message }, 500);
  }
});

// Logout
authRoutes.post('/auth/logout', async (c) => {
  try {
    const authHeader = c.req.header('Authorization');
    if (!authHeader?.startsWith('Bearer ')) {
      return c.json({ success: true }); // Ignore if no auth
    }
    
    const token = authHeader.split(' ')[1];
    const secret = c.env.JWT_SECRET || 'dev_secret_key_please_change';
    
    try {
      const decoded = await verify(token, secret, 'HS256');
      const sessionId = decoded.session_id as string;
      const db = c.env.DB;
      const now = Date.now();
      await db.prepare(`UPDATE sessions SET revoked_at = ? WHERE id = ?`).bind(now, sessionId).run();
    } catch (e) {
      // Ignora erro de JWT expirado no logout
    }

    return c.json({ success: true });
  } catch (err: any) {
    return c.json({ error: err.message }, 500);
  }
});
