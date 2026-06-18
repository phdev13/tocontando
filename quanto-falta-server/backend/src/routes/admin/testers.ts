import { Hono } from 'hono';
import type { Env } from '../../types';

export const adminTestersRoutes = new Hono<{ Bindings: Env }>();

async function auditLog(c: any, action: string, targetType: string, targetId: string, details?: unknown) {
  const adminUserId = c.get('adminUserId') as number;
  await c.env.DB.prepare(
    `INSERT INTO audit_logs (admin_user_id, action, target_type, target_id, details) VALUES (?, ?, ?, ?, ?)`
  ).bind(adminUserId, action, targetType, targetId, details ? JSON.stringify(details) : null).run();
}

// ── GET /admin/testers ───────────────────────────────────────
adminTestersRoutes.get('/', async (c) => {
  try {
    const { results } = await c.env.DB.prepare(`
      SELECT * FROM testers ORDER BY display_order ASC, display_name ASC
    `).all();
    return c.json({ testers: results });
  } catch (error) {
    console.error('Error fetching admin testers:', error);
    return c.json({ error: 'Failed to fetch testers' }, 500);
  }
});

// ── POST /admin/testers ──────────────────────────────────────
adminTestersRoutes.post('/', async (c) => {
  try {
    const body = await c.req.json();
    const id = body.id || crypto.randomUUID();
    const now = Date.now();
    
    // Only set published_at if consent_confirmed is true and it is active.
    const publishedAt = (body.is_active && body.consent_confirmed) ? now : null;

    await c.env.DB.prepare(`
      INSERT INTO testers (
        id, display_name, nickname, avatar_key, badge_key, message,
        participation_version, participation_period, display_order,
        is_active, is_featured, consent_confirmed, published_at, created_at, updated_at
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    `).bind(
      id,
      body.display_name,
      body.nickname || null,
      body.avatar_key || null,
      body.badge_key || null,
      body.message || null,
      body.participation_version || null,
      body.participation_period || null,
      body.display_order || 0,
      body.is_active ? 1 : 0,
      body.is_featured ? 1 : 0,
      body.consent_confirmed ? 1 : 0,
      publishedAt,
      now,
      now
    ).run();

    await auditLog(c, 'tester_created', 'tester', id, { display_name: body.display_name });
    return c.json({ success: true, id }, 201);
  } catch (error) {
    console.error('Error creating tester:', error);
    return c.json({ error: 'Failed to create tester' }, 500);
  }
});

// ── PUT /admin/testers/:id ───────────────────────────────────
adminTestersRoutes.put('/:id', async (c) => {
  try {
    const id = c.req.param('id');
    const body = await c.req.json();
    const now = Date.now();

    // Determine publishedAt logic.
    // If we transition to consent_confirmed && is_active, and published_at was null, we set it.
    // If not, we keep the previous, or set it to null if consent is revoked.
    const existing = await c.env.DB.prepare('SELECT published_at FROM testers WHERE id = ?').bind(id).first();
    if (!existing) {
      return c.json({ error: 'Tester not found' }, 404);
    }
    
    let publishedAt = existing.published_at;
    if (body.is_active && body.consent_confirmed) {
      if (!publishedAt) publishedAt = now;
    } else {
      publishedAt = null;
    }

    await c.env.DB.prepare(`
      UPDATE testers SET
        display_name = ?,
        nickname = ?,
        avatar_key = ?,
        badge_key = ?,
        message = ?,
        participation_version = ?,
        participation_period = ?,
        display_order = ?,
        is_active = ?,
        is_featured = ?,
        consent_confirmed = ?,
        published_at = ?,
        updated_at = ?
      WHERE id = ?
    `).bind(
      body.display_name,
      body.nickname || null,
      body.avatar_key || null,
      body.badge_key || null,
      body.message || null,
      body.participation_version || null,
      body.participation_period || null,
      body.display_order || 0,
      body.is_active ? 1 : 0,
      body.is_featured ? 1 : 0,
      body.consent_confirmed ? 1 : 0,
      publishedAt,
      now,
      id
    ).run();

    await auditLog(c, 'tester_updated', 'tester', id, { display_name: body.display_name });
    return c.json({ success: true });
  } catch (error) {
    console.error('Error updating tester:', error);
    return c.json({ error: 'Failed to update tester' }, 500);
  }
});

// ── DELETE /admin/testers/:id ────────────────────────────────
adminTestersRoutes.delete('/:id', async (c) => {
  try {
    const id = c.req.param('id');
    await c.env.DB.prepare('DELETE FROM testers WHERE id = ?').bind(id).run();
    await auditLog(c, 'tester_deleted', 'tester', id);
    return c.json({ success: true });
  } catch (error) {
    console.error('Error deleting tester:', error);
    return c.json({ error: 'Failed to delete tester' }, 500);
  }
});
