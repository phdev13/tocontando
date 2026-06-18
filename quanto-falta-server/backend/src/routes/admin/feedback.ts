import { Hono } from 'hono';
import { z } from 'zod';
import type { Env } from '../../types';

export const adminFeedbackRoutes = new Hono<{ Bindings: Env }>();

/** GET /admin/feedback — list with filters */
adminFeedbackRoutes.get('/', async (c) => {
  const q = c.req.query();
  const limit = Math.min(parseInt(q.limit ?? '50', 10), 200);
  const offset = parseInt(q.offset ?? '0', 10);
  const status = q.status;
  const category = q.category;
  const minRating = q.minRating ? parseInt(q.minRating, 10) : undefined;
  const search = q.search;

  let query = 'SELECT * FROM feedback WHERE 1=1';
  const params: (string | number)[] = [];

  if (status) { query += ' AND status = ?'; params.push(status); }
  if (category) { query += ' AND category = ?'; params.push(category); }
  if (minRating) { query += ' AND rating >= ?'; params.push(minRating); }
  if (search) {
    // Search in message only (not personal data)
    query += ' AND message LIKE ?';
    params.push(`%${search.slice(0, 100)}%`);
  }

  query += ' ORDER BY created_at DESC LIMIT ? OFFSET ?';
  params.push(limit, offset);

  const { results } = await c.env.DB.prepare(query).bind(...params).all();

  // Get total count
  let countQuery = 'SELECT COUNT(*) as total FROM feedback WHERE 1=1';
  const countParams: (string | number)[] = [];
  if (status) { countQuery += ' AND status = ?'; countParams.push(status); }
  if (category) { countQuery += ' AND category = ?'; countParams.push(category); }

  const countResult = await c.env.DB.prepare(countQuery).bind(...countParams).first<{ total: number }>();

  return c.json({ feedback: results, total: countResult?.total ?? 0, limit, offset });
});

/** PATCH /admin/feedback/:id — update status, priority, admin notes */
adminFeedbackRoutes.patch('/:id', async (c) => {
  const id = c.req.param('id');
  const body = await c.req.json().catch(() => null);

  const UpdateSchema = z.object({
    status: z.enum(['new', 'reviewing', 'planned', 'resolved', 'ignored']).optional(),
    priority: z.enum(['low', 'normal', 'high', 'critical']).optional(),
    adminNotes: z.string().max(2000).optional(),
    tags: z.array(z.string().max(50)).max(10).optional(),
  });

  const parsed = UpdateSchema.safeParse(body);
  if (!parsed.success) return c.json({ error: 'Invalid payload' }, 400);

  const d = parsed.data;
  const updates: string[] = ['updated_at = ?'];
  const params: (string | number | null)[] = [Date.now()];

  if (d.status) { updates.push('status = ?'); params.push(d.status); }
  if (d.priority) { updates.push('priority = ?'); params.push(d.priority); }
  if (d.adminNotes !== undefined) { updates.push('admin_notes = ?'); params.push(d.adminNotes); }
  if (d.tags) { updates.push('tags = ?'); params.push(JSON.stringify(d.tags)); }

  params.push(id);
  await c.env.DB.prepare(`UPDATE feedback SET ${updates.join(', ')} WHERE id = ?`).bind(...params).run();

  return c.json({ updated: true });
});

/** GET /admin/feedback/stats — aggregate stats */
adminFeedbackRoutes.get('/stats', async (c) => {
  const [total, byStatus, byCategory, avgRating] = await Promise.all([
    c.env.DB.prepare('SELECT COUNT(*) as count FROM feedback').first<{ count: number }>(),
    c.env.DB.prepare(`SELECT status, COUNT(*) as count FROM feedback GROUP BY status`).all(),
    c.env.DB.prepare(`SELECT category, COUNT(*) as count FROM feedback GROUP BY category`).all(),
    c.env.DB.prepare(`SELECT AVG(rating) as avg FROM feedback WHERE rating IS NOT NULL`).first<{ avg: number }>(),
  ]);

  return c.json({
    total: total?.count ?? 0,
    byStatus: byStatus.results,
    byCategory: byCategory.results,
    averageRating: avgRating?.avg ? Math.round(avgRating.avg * 10) / 10 : null,
  });
});
