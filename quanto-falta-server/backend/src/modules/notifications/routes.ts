import { Hono } from 'hono';
import { z } from 'zod';
import type { Env } from '../../types';
import { fail, ok } from '../../contracts/apiResponse';

export const notificationsAppRoutes = new Hono<{ Bindings: Env }>();

const RegisterSchema = z.object({
  installationId: z.string().uuid(),
  platform: z.literal('android').default('android'),
  token: z.string().min(10).max(4096),
  appVersion: z.string().max(20).optional(),
  locale: z.string().max(10).optional(),
});

notificationsAppRoutes.post('/notifications/register', async (c) => {
  const body = await c.req.json().catch(() => null);
  const parsed = RegisterSchema.safeParse(body);
  if (!parsed.success) return fail(c, 'INVALID_REQUEST', 'Payload de notificacao invalido.', 400);

  const tableExists = await c.env.DB.prepare(`
    SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'push_tokens'
  `).first<{ name: string }>();

  if (!tableExists) {
    return ok(c, { registered: false, reason: 'notifications_storage_unavailable' }, 202);
  }

  await c.env.DB.prepare(`
    INSERT INTO push_tokens(token, user_id, last_used_at)
    VALUES(?, ?, CURRENT_TIMESTAMP)
    ON CONFLICT(token) DO UPDATE SET
      user_id = excluded.user_id,
      last_used_at = CURRENT_TIMESTAMP
  `).bind(
    parsed.data.token,
    parsed.data.installationId,
  ).run();

  return ok(c, { registered: true });
});
