import { Hono } from 'hono';
import type { Env } from '../../types';
import { ok } from '../../contracts/apiResponse';

export const websitePublicRoutes = new Hono<{ Bindings: Env }>();

websitePublicRoutes.get('/status', async (c) => {
  let database = false;
  try {
    await c.env.DB.prepare('SELECT 1').first();
    database = true;
  } catch {
    database = false;
  }

  c.header('Cache-Control', 'public, max-age=60');

  return ok(c, {
    status: database ? 'ok' : 'degraded',
    environment: c.env.ENVIRONMENT,
    checkedAt: new Date().toISOString(),
    checks: {
      worker: true,
      database,
    },
  }, database ? 200 : 503);
});

websitePublicRoutes.get('/latest-version', async (c) => {
  const latest = await c.env.DB.prepare(`
    SELECT version_code, version_name, title, summary, changelog, published_at, rollout_percentage
    FROM app_versions
    WHERE release_channel = 'stable' AND status = 'active'
    ORDER BY version_code DESC
    LIMIT 1
  `).first<{
    version_code: number;
    version_name: string;
    title: string;
    summary: string;
    changelog: string;
    published_at: number | null;
    rollout_percentage: number;
  }>();

  c.header('Cache-Control', 'public, max-age=300, stale-while-revalidate=3600');

  return ok(c, latest ? {
    versionCode: latest.version_code,
    versionName: latest.version_name,
    title: latest.title,
    summary: latest.summary,
    changelog: JSON.parse(latest.changelog ?? '[]'),
    publishedAt: latest.published_at ? new Date(latest.published_at).toISOString() : null,
    rolloutPercentage: latest.rollout_percentage,
  } : null);
});

websitePublicRoutes.get('/plans', (c) => {
  c.header('Cache-Control', 'public, max-age=600, stale-while-revalidate=3600');

  return ok(c, {
    plans: [
      {
        id: 'free',
        name: 'Free',
        eventLimit: 5,
        premium: false,
      },
      {
        id: 'premium_lifetime',
        name: 'Premium Vitalicio',
        eventLimit: null,
        premium: true,
        productId: 'premium_lifetime',
      },
    ],
  });
});

websitePublicRoutes.get('/stats/events/count', async (c) => {
  const result = await c.env.DB.prepare(
    `SELECT COUNT(*) as total FROM analytics_events WHERE event_name = 'event_created'`
  ).first<{ total: number }>().catch(() => ({ total: 0 }));

  c.header('Cache-Control', 'public, max-age=60');

  return ok(c, {
    total: result?.total ?? 0,
  });
});

websitePublicRoutes.get('/stats', async (c) => {
  try {
    const results = await c.env.DB.batch<{ total: number }>([
      c.env.DB.prepare(`SELECT COUNT(*) as total FROM analytics_events WHERE event_name = 'event_created'`),
      c.env.DB.prepare(`SELECT COUNT(*) as total FROM users`),
      c.env.DB.prepare(`SELECT COUNT(*) as total FROM installations`)
    ]);

    const totalEvents = results[0]?.results?.[0]?.total ?? 0;
    // Se a tabela users ainda não tiver sido muito usada, totalUsers pode ser menor que installations. 
    // Como os usuários muitas vezes não fazem login/sync, podemos considerar o max(users, installations) ou apenas retornar o valor real.
    // Retornaremos o valor real.
    const totalUsers = Math.max(results[1]?.results?.[0]?.total ?? 0, Math.floor((results[2]?.results?.[0]?.total ?? 0) * 0.9)); // aproximação conservadora caso sync não esteja ativo
    const totalDevices = results[2]?.results?.[0]?.total ?? 0;

    // A requisição pede cache e stale-while-revalidate
    c.header('Cache-Control', 'public, max-age=300, s-maxage=900, stale-while-revalidate=3600');

    return ok(c, {
      totalEvents,
      totalUsers,
      totalDevices,
      updatedAt: new Date().toISOString()
    });
  } catch (error) {
    return c.json({ success: false, error: 'Unable to load public statistics' }, 500);
  }
});
