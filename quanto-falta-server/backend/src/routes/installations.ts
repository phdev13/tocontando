import { Hono } from 'hono';
import { z } from 'zod';
import type { Env } from '../types';

export const installationsRoutes = new Hono<{ Bindings: Env }>();

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

/**
 * POST /api/v1/installations/register
 * Registers or updates an installation.
 * Uses UPSERT to handle repeated calls idempotently.
 */
installationsRoutes.post('/installations/register', async (c) => {
  const body = await c.req.json().catch(() => null);
  if (!body) return c.json({ error: 'Invalid body' }, 400);

  const parsed = RegisterSchema.safeParse(body);
  if (!parsed.success) return c.json({ error: 'Invalid payload' }, 400);

  const d = parsed.data;

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
    Date.now(), Date.now()
  ).run();

  return c.json({ registered: true }, 200);
});

/**
 * PATCH /api/v1/installations/version
 * Called after a successful app update to record adoption.
 */
installationsRoutes.patch('/installations/version', async (c) => {
  const body = await c.req.json().catch(() => null);
  const parsed = z.object({
    installationId: z.string().uuid(),
    fromVersionCode: z.number().int().positive(),
    toVersionCode: z.number().int().positive(),
  }).safeParse(body);

  if (!parsed.success) return c.json({ error: 'Invalid payload' }, 400);

  const { installationId, fromVersionCode, toVersionCode } = parsed.data;

  await c.env.DB.prepare(
    `UPDATE installations SET version_code = ?, last_seen_at = ? WHERE installation_id = ?`
  ).bind(toVersionCode, Date.now(), installationId).run();

  // Record adoption event in OTA attempts
  await c.env.DB.prepare(
    `INSERT INTO ota_attempts(installation_id, version_code, event_type)
     VALUES(?, ?, 'adopted')`
  ).bind(installationId, toVersionCode).run();

  return c.json({ updated: true }, 200);
});
