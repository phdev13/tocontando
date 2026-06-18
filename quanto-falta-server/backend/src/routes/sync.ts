import { Hono } from 'hono';
import type { Env } from '../types';

export const syncRoutes = new Hono<{ Bindings: Env }>();

syncRoutes.post('/sync/backup', async (c) => {
  try {
    const { installation_id, data } = await c.req.json();
    if (!installation_id || !data) {
      return c.json({ error: 'Missing parameters' }, 400);
    }

    const db = c.env.DB;
    await db.prepare(
      `INSERT INTO user_backups (installation_id, data)
       VALUES (?, ?)
       ON CONFLICT(installation_id) DO UPDATE SET data = excluded.data, updated_at = unixepoch() * 1000`
    ).bind(installation_id, JSON.stringify(data)).run();

    return c.json({ success: true });
  } catch (err: any) {
    return c.json({ error: err.message }, 500);
  }
});

syncRoutes.get('/sync/restore', async (c) => {
  try {
    const installation_id = c.req.query('installation_id');
    if (!installation_id) {
      return c.json({ error: 'Missing installation_id' }, 400);
    }

    const db = c.env.DB;
    const row = await db.prepare(
      `SELECT data, updated_at FROM user_backups WHERE installation_id = ?`
    ).bind(installation_id).first<{ data: string; updated_at: number }>();

    if (!row) {
      return c.json({ error: 'No backup found' }, 404);
    }

    return c.json({ success: true, data: JSON.parse(row.data), updated_at: row.updated_at });
  } catch (err: any) {
    return c.json({ error: err.message }, 500);
  }
});

import { verify } from 'hono/jwt';

// Helper to decompress body
async function readBody(req: Request): Promise<any> {
  const encoding = req.headers.get('Content-Encoding');
  if (encoding === 'gzip' && req.body) {
    const ds = new DecompressionStream('gzip');
    const decompressed = req.body.pipeThrough(ds);
    const text = await new Response(decompressed).text();
    return JSON.parse(text);
  }
  return req.json();
}

syncRoutes.post('/sync', async (c) => {
  try {
    const authHeader = c.req.header('Authorization');
    if (!authHeader?.startsWith('Bearer ')) {
      return c.json({ error: 'Unauthorized' }, 401);
    }
    const token = authHeader.split(' ')[1];
    const secret = c.env.JWT_SECRET || 'dev_secret_key_please_change';
    
    let decoded: any;
    try {
      decoded = await verify(token, secret, 'HS256');
    } catch (e) {
      return c.json({ error: 'Token inválido' }, 401);
    }
    
    const userId = decoded.sub as string;
    
    // Check if session is revoked
    const db = c.env.DB;
    const session = await db.prepare(`SELECT revoked_at FROM sessions WHERE id = ?`).bind(decoded.session_id).first<{ revoked_at: number | null }>();
    if (!session || session.revoked_at) {
      return c.json({ error: 'Sessão revogada' }, 401);
    }

    const body = await readBody(c.req.raw);
    const { c: cursorStr, d: deviceId, o: operations } = body;
    
    const conflicts: any[] = [];
    const okIds: string[] = [];
    const now = Date.now();
    
    if (Array.isArray(operations) && operations.length > 0) {
      if (operations.length > 50) {
        return c.json({ error: 'Batch too large (max 50)' }, 413);
      }
      
      // Process each operation
      // Since D1 batch transactions are just an array of statements, and we need logic (conflict detection),
      // we'll fetch existing revisions first.
      const opIds = operations.map((op: any) => op.i);
      const placeholders = opIds.map(() => '?').join(',');
      const existingRows = await db.prepare(`SELECT id, revision, payload FROM sync_events WHERE user_id = ? AND id IN (${placeholders})`)
        .bind(userId, ...opIds).all<{ id: string; revision: number; payload: string }>();
        
      const existingMap = new Map(existingRows.results.map(r => [r.id, r]));
      
      const statements: any[] = [];
      
      for (const op of operations) {
        const { i: eventId, r: baseRevision, t: type, p: payload } = op;
        const existing = existingMap.get(eventId);
        
        if (existing && existing.revision !== baseRevision) {
          // Conflict: server wins
          conflicts.push({
            i: existing.id,
            r: existing.revision,
            p: JSON.parse(existing.payload || '{}') // Return full payload on conflict
          });
          continue;
        }
        
        // No conflict, proceed
        okIds.push(eventId);
        const nextRev = (existing?.revision || 0) + 1;
        const deletedAt = type === 'd' ? now : null;
        const payloadStr = payload ? JSON.stringify(payload) : '{}';
        
        if (existing) {
          statements.push(
            db.prepare(`UPDATE sync_events SET payload = ?, revision = ?, updated_at = ?, deleted_at = ? WHERE id = ? AND user_id = ?`)
              .bind(payloadStr, nextRev, now, deletedAt, eventId, userId)
          );
        } else {
          statements.push(
            db.prepare(`INSERT INTO sync_events (id, user_id, payload, revision, updated_at, deleted_at) VALUES (?, ?, ?, ?, ?, ?)`)
              .bind(eventId, userId, payloadStr, nextRev, now, deletedAt)
          );
        }
      }
      
      if (statements.length > 0) {
        await db.batch(statements);
      }
    }
    
    // Parse cursor (format: "timestamp_id" or empty)
    let cursorTs = 0;
    let cursorId = '';
    if (cursorStr) {
      const parts = cursorStr.split('_');
      if (parts.length >= 2) {
        cursorTs = parseInt(parts[0], 10);
        cursorId = parts.slice(1).join('_');
      }
    }
    
    // Fetch remote changes
    const ninetyDaysAgo = now - 90 * 24 * 60 * 60 * 1000;
    const remoteChanges = await db.prepare(`
      SELECT id, payload, revision, updated_at, deleted_at
      FROM sync_events
      WHERE user_id = ?
        AND (updated_at > ? OR (updated_at = ? AND id > ?))
        AND (deleted_at IS NULL OR deleted_at > ?)
      ORDER BY updated_at ASC, id ASC
      LIMIT 51
    `).bind(userId, cursorTs, cursorTs, cursorId, ninetyDaysAgo).all<{ id: string, payload: string, revision: number, updated_at: number, deleted_at: number | null }>();
    
    let hasMore = false;
    let results = remoteChanges.results;
    if (results.length === 51) {
      hasMore = true;
      results = results.slice(0, 50);
    }
    
    let nextCursor = cursorStr || '';
    if (results.length > 0) {
      const last = results[results.length - 1];
      nextCursor = `${last.updated_at}_${last.id}`;
    }
    
    // Exclude changes that were just ACKed by this same request
    const filteredRemote = results
      .filter(r => !okIds.includes(r.id))
      .map(r => ({
        i: r.id,
        r: r.revision,
        t: r.deleted_at ? 'd' : 'u',
        p: JSON.parse(r.payload || '{}')
      }));

    const responseObj: any = { c: nextCursor };
    if (okIds.length > 0) responseObj.ok = okIds;
    if (conflicts.length > 0) responseObj.conflicts = conflicts;
    if (filteredRemote.length > 0) responseObj.remote = filteredRemote;
    
    return c.json(responseObj);

  } catch (err: any) {
    return c.json({ error: err.message }, 500);
  }
});
