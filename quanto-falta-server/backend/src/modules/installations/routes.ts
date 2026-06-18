import { Hono } from 'hono';
import { z } from 'zod';
import type { Env } from '../../types';
import { fail, ok } from '../../contracts/apiResponse';

export const installationsAppRoutes = new Hono<{ Bindings: Env }>();

const RegisterSchema = z.object({
  installationId: z.string().uuid(),
  versionCode: z.number().int().positive(),
  versionName: z.string().max(20),
  androidVersion: z.string().max(10).optional(),
  architecture: z.string().max(20).optional(),
  language: z.string().max(10).optional(),
  manufacturer: z.string().max(50).optional(),
  model: z.string().max(50).optional(),
  theme: z.enum(['light', 'dark', 'system']).optional(),
  releaseChannel: z.enum(['stable', 'beta', 'internal']).default('stable'),
});

installationsAppRoutes.post('/installations/register', async (c) => {
  const body = await c.req.json().catch(() => null);
  const parsed = RegisterSchema.safeParse(body);
  if (!parsed.success) return fail(c, 'INVALID_REQUEST', 'Payload de instalacao invalido.', 400);

  const d = parsed.data;
  const now = Date.now();

  await c.env.DB.prepare(
    `INSERT INTO installations(
       installation_id, version_code, version_name, android_version,
       architecture, language, manufacturer, model, theme, release_channel,
       first_seen_at, last_seen_at
     ) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
     ON CONFLICT(installation_id) DO UPDATE SET
       version_code = excluded.version_code,
       version_name = excluded.version_name,
       last_seen_at = excluded.last_seen_at,
       theme = COALESCE(excluded.theme, theme),
       is_active = 1`
  ).bind(
    d.installationId, d.versionCode, d.versionName, d.androidVersion ?? null,
    d.architecture ?? null, d.language ?? null, d.manufacturer ?? null,
    d.model ?? null, d.theme ?? null, d.releaseChannel,
    now, now,
  ).run();

  return ok(c, { registered: true });
});
