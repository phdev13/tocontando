/** Simple cached settings fetcher — avoids repeated D1 queries for hot paths */
const settingsCache = new Map<string, { value: string; expiresAt: number }>();
const CACHE_TTL_MS = 60 * 1000; // 1 minute cache per Worker instance

export async function getSetting(db: D1Database, key: string): Promise<string | null> {
  const cached = settingsCache.get(key);
  if (cached && cached.expiresAt > Date.now()) return cached.value;

  const row = await db.prepare(
    'SELECT value FROM system_settings WHERE key = ? LIMIT 1'
  ).bind(key).first<{ value: string }>();

  if (row) {
    settingsCache.set(key, { value: row.value, expiresAt: Date.now() + CACHE_TTL_MS });
    return row.value;
  }
  return null;
}
