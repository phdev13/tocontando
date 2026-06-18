import { Hono } from 'hono';
import { z } from 'zod';
import type { Env } from '../../types';

export const adminSettingsRoutes = new Hono<{ Bindings: Env }>();

const ALLOWED_SETTINGS = new Set([
  'ota_check_interval_hours', 'ota_modal_cooldown_hours',
  'feedback_contextual_enabled', 'feedback_contextual_cooldown_days',
  'maintenance_mode', 'maintenance_message',
  'telemetry_max_queue_size', 'min_supported_version_code',
  'active_icon_mode',
]);

/** GET /admin/settings — all settings */
adminSettingsRoutes.get('/', async (c) => {
  const { results } = await c.env.DB.prepare('SELECT key, value, description, updated_at FROM system_settings').all();
  return c.json({ settings: results });
});

/** PUT /admin/settings/:key — update a setting */
adminSettingsRoutes.put('/:key', async (c) => {
  const key = c.req.param('key');
  if (!ALLOWED_SETTINGS.has(key)) {
    return c.json({ error: 'Setting not allowed', code: 'FORBIDDEN_KEY' }, 403);
  }

  const body = await c.req.json().catch(() => null);
  const parsed = z.object({ value: z.string().max(500) }).safeParse(body);
  if (!parsed.success) return c.json({ error: 'Invalid payload' }, 400);

  await c.env.DB.prepare(
    `INSERT INTO system_settings(key, value, updated_at) VALUES(?, ?, ?)
     ON CONFLICT(key) DO UPDATE SET value = excluded.value, updated_at = excluded.updated_at`
  ).bind(key, parsed.data.value, Date.now()).run();

  const adminUserId = c.get('adminUserId' as never) as number;
  await c.env.DB.prepare(
    `INSERT INTO audit_logs(admin_user_id, action, target_type, target_id, details)
     VALUES(?, 'setting_updated', 'system_setting', ?, ?)`
  ).bind(adminUserId, key, JSON.stringify({ key, value: parsed.data.value })).run().catch(() => {});

  return c.json({ updated: true, key, value: parsed.data.value });
});
