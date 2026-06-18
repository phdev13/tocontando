import { Hono } from 'hono';
import type { Env } from '../types';

export const testersRoutes = new Hono<{ Bindings: Env }>();

testersRoutes.get('/testers', async (c) => {
  try {
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

    // Cache Control Strategy
    // Calculate a simple ETag based on the data length and latest updated_at
    // But since we only get published testers, let's hash the stringified results for a robust ETag
    // Since this runs in a worker, we can just use a fast hash or string length + last timestamp.
    // For simplicity and high performance, we'll use SHA-1 of the payload.
    const payloadString = JSON.stringify({ version: 1, testers: results });
    const encoder = new TextEncoder();
    const dataBuffer = encoder.encode(payloadString);
    const hashBuffer = await crypto.subtle.digest('SHA-1', dataBuffer);
    const hashArray = Array.from(new Uint8Array(hashBuffer));
    const etag = '"' + hashArray.map(b => b.toString(16).padStart(2, '0')).join('') + '"';

    const ifNoneMatch = c.req.header('If-None-Match');
    if (ifNoneMatch === etag) {
      return new Response(null, { status: 304 });
    }

    c.header('ETag', etag);
    c.header('Cache-Control', 'public, max-age=600, stale-while-revalidate=3600'); // 10 minutes cache

    return c.json({
      version: 1,
      testers: results
    });
  } catch (error) {
    console.error('Error fetching testers:', error);
    return c.json({ error: 'Failed to fetch testers' }, 500);
  }
});
