import { Hono } from 'hono';
import { verify, sign } from 'hono/jwt';
import type { Env } from '../types';

export const uploadRoutes = new Hono<{ Bindings: Env }>();

uploadRoutes.post('/upload/url', async (c) => {
  try {
    const authHeader = c.req.header('Authorization');
    if (!authHeader?.startsWith('Bearer ')) {
      return c.json({ error: 'Unauthorized' }, 401);
    }
    const token = authHeader.split(' ')[1];
    const secret = c.env.JWT_SECRET || 'dev_secret_key_please_change';
    
    let decoded: any;
    try {
      decoded = await verify(token, secret, 'HS256');
    } catch (e) {
      return c.json({ error: 'Token inválido' }, 401);
    }
    
    const userId = decoded.sub as string;
    const { eventId, extension } = await c.req.json();
    if (!eventId || !extension) {
      return c.json({ error: 'eventId e extension são obrigatórios' }, 400);
    }

    const key = `covers/${userId}/${eventId}.${extension}`;
    
    // Generate a short-lived token for upload
    const uploadTokenPayload = {
      sub: userId,
      key: key,
      exp: Math.floor(Date.now() / 1000) + 15 * 60, // 15 minutes
    };
    const uploadToken = await sign(uploadTokenPayload, secret);
    
    // In production, use the actual domain. For dev, use the current request origin.
    const url = new URL(c.req.url);
    const uploadUrl = `${url.origin}/api/v1/upload/direct?key=${encodeURIComponent(key)}&token=${uploadToken}`;
    const downloadUrl = `${url.origin}/api/v1/download/direct?key=${encodeURIComponent(key)}`;

    return c.json({ uploadUrl, downloadUrl });
  } catch (err: any) {
    return c.json({ error: err.message }, 500);
  }
});

uploadRoutes.put('/upload/direct', async (c) => {
  try {
    const token = c.req.query('token');
    const key = c.req.query('key');
    if (!token || !key) {
      return c.json({ error: 'Missing token or key' }, 400);
    }

    const secret = c.env.JWT_SECRET || 'dev_secret_key_please_change';
    let decoded: any;
    try {
      decoded = await verify(token, secret, 'HS256');
    } catch (e) {
      return c.json({ error: 'Token inválido ou expirado' }, 401);
    }

    if (decoded.key !== key) {
      return c.json({ error: 'Key mismatch' }, 403);
    }

    const bucket = c.env.APK_BUCKET; // reusing APK_BUCKET for covers
    if (!bucket) {
      return c.json({ error: 'R2 não configurado' }, 500);
    }

    const body = c.req.raw.body;
    if (!body) {
      return c.json({ error: 'Empty body' }, 400);
    }

    const contentType = c.req.header('Content-Type') || 'application/octet-stream';
    
    await bucket.put(key, body, {
      httpMetadata: { contentType }
    });

    return c.json({ success: true });
  } catch (err: any) {
    return c.json({ error: err.message }, 500);
  }
});

uploadRoutes.get('/download/direct', async (c) => {
  try {
    const key = c.req.query('key');
    if (!key) return c.json({ error: 'Missing key' }, 400);

    const bucket = c.env.APK_BUCKET;
    if (!bucket) return c.json({ error: 'R2 não configurado' }, 500);

    const object = await bucket.get(key);
    if (!object) return c.text('Not found', 404);

    const headers = new Headers();
    object.writeHttpMetadata(headers);
    headers.set('etag', object.httpEtag);
    // Cache heavily since covers don't change often
    headers.set('Cache-Control', 'public, max-age=31536000');

    return new Response(object.body, { headers });
  } catch (err: any) {
    return c.json({ error: err.message }, 500);
  }
});
