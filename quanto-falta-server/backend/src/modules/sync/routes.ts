import { Hono } from 'hono';
import { z } from 'zod';
import type { Env } from '../../types';
import { fail, ok } from '../../contracts/apiResponse';

export const syncAppRoutes = new Hono<{ Bindings: Env }>();

const BackupSchema = z.object({
  installation_id: z.string().min(1),
  data: z.record(z.unknown()),
});

syncAppRoutes.post('/sync/backup', async (c) => {
  const body = await c.req.json().catch(() => null);
  const parsed = BackupSchema.safeParse(body);
  if (!parsed.success) return fail(c, 'INVALID_REQUEST', 'Payload de backup invalido.', 400);

  await c.env.DB.prepare(
    `INSERT INTO user_backups (installation_id, data)
     VALUES (?, ?)
     ON CONFLICT(installation_id) DO UPDATE SET data = excluded.data, updated_at = unixepoch() * 1000`
  ).bind(parsed.data.installation_id, JSON.stringify(parsed.data.data)).run();

  return ok(c, { backedUp: true });
});

syncAppRoutes.get('/sync/restore', async (c) => {
  const installationId = c.req.query('installation_id');
  if (!installationId) return fail(c, 'INVALID_REQUEST', 'installation_id obrigatorio.', 400);

  const row = await c.env.DB.prepare(
    `SELECT data, updated_at FROM user_backups WHERE installation_id = ?`
  ).bind(installationId).first<{ data: string; updated_at: number }>();

  if (!row) return fail(c, 'NOT_FOUND', 'Backup nao encontrado.', 404);

  return ok(c, {
    data: JSON.parse(row.data),
    updatedAt: row.updated_at,
  });
});
