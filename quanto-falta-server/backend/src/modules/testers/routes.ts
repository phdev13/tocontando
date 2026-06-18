import { Hono } from 'hono';
import type { Env } from '../../types';
import { ok } from '../../contracts/apiResponse';

export const testersPublicRoutes = new Hono<{ Bindings: Env }>();

testersPublicRoutes.get('/testers', async (c) => {
  const { results } = await c.env.DB.prepare(`
    SELECT
      id,
      display_name as displayName,
      nickname,
      avatar_key as avatarUrl,
      badge_key as badgeKey,
      message,
      participation_version as participationVersion,
      participation_period as participationPeriod,
      display_order as displayOrder,
      is_featured as isFeatured,
      published_at as publishedAt
    FROM testers
    WHERE is_active = 1 AND consent_confirmed = 1 AND published_at IS NOT NULL
    ORDER BY is_featured DESC, display_order ASC, display_name ASC
  `).all();

  c.header('Cache-Control', 'public, max-age=600, stale-while-revalidate=3600');

  return ok(c, {
    version: 1,
    testers: results,
  });
});
