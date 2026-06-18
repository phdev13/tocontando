import { Hono } from 'hono';
import { z } from 'zod';
import type { Env } from '../../types';
import { fail, ok } from '../../contracts/apiResponse';

export const shareAppRoutes = new Hono<{ Bindings: Env }>();
export const sharePublicRoutes = new Hono<{ Bindings: Env }>();

const ShareEventSchema = z.object({
  title: z.string().min(1).max(120),
  date: z.string().min(1).max(40),
  color: z.string().regex(/^#[0-9A-Fa-f]{6}$/),
  icon: z.string().min(1).max(64),
  cover_url: z.string().url().optional().nullable(),
});

shareAppRoutes.post('/share/events', async (c) => {
  const body = await c.req.json().catch(() => null);
  const parsed = ShareEventSchema.safeParse(body);
  if (!parsed.success) return fail(c, 'INVALID_REQUEST', 'Payload de compartilhamento invalido.', 400);

  const slug = await uniqueSlug(c.env.DB);
  await c.env.DB.prepare(
    `INSERT INTO events(slug, title, date, color, icon, cover_url) VALUES(?, ?, ?, ?, ?, ?)`
  ).bind(slug, parsed.data.title, parsed.data.date, parsed.data.color, parsed.data.icon, parsed.data.cover_url || null).run();

  return ok(c, {
    slug,
    url: `https://share.tocontando.com.br/s/${slug}`,
  }, 201);
});

sharePublicRoutes.get('/share/events/:slug', async (c) => {
  const event = await c.env.DB.prepare(
    `SELECT slug, title, date, color, icon, cover_url, created_at as createdAt FROM events WHERE slug = ? LIMIT 1`
  ).bind(c.req.param('slug')).first();

  if (!event) return fail(c, 'NOT_FOUND', 'Evento compartilhado nao encontrado.', 404);
  return ok(c, { event });
});

async function uniqueSlug(db: D1Database): Promise<string> {
  for (let attempt = 0; attempt < 5; attempt += 1) {
    const slug = generateSlug();
    const existing = await db.prepare('SELECT slug FROM events WHERE slug = ?').bind(slug).first();
    if (!existing) return slug;
  }
  return crypto.randomUUID().replace(/-/g, '').slice(0, 12);
}

function generateSlug(length = 8): string {
  const chars = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
  const bytes = new Uint8Array(length);
  crypto.getRandomValues(bytes);
  return Array.from(bytes).map((byte) => chars[byte % chars.length]).join('');
}
