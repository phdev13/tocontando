import { Hono } from 'hono';
import { z } from 'zod';
import { zValidator } from '@hono/zod-validator';
import type { Env } from '../../types';

export const adminMonetizationRoutes = new Hono<{ Bindings: Env }>();

function generateSecureSuffix(length = 6): string {
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
  const bytes = new Uint8Array(length);
  crypto.getRandomValues(bytes);
  return Array.from(bytes).map((b) => chars[b % chars.length]).join('');
}

async function sha256(input: string): Promise<string> {
  const encoder = new TextEncoder();
  const data = encoder.encode(input);
  const hashBuffer = await crypto.subtle.digest('SHA-256', data);
  const hashArray = Array.from(new Uint8Array(hashBuffer));
  return hashArray.map((b) => b.toString(16).padStart(2, '0')).join('');
}

adminMonetizationRoutes.get('/metrics', async (c) => {
  const db = c.env.DB;
  const now = Math.floor(Date.now() / 1000);

  const purchasesResult = await db.prepare(`SELECT count(id) as total FROM monetization_purchases`).first();
  const entitlementsResult = await db.prepare(`
    SELECT count(id) as total FROM monetization_entitlements
    WHERE status = 'ACTIVE' AND (expires_at IS NULL OR expires_at > ?)
  `).bind(now).first();
  const lifetimeResult = await db.prepare(`
    SELECT count(id) as total FROM monetization_entitlements
    WHERE status = 'ACTIVE' AND expires_at IS NULL
  `).first();
  const codesResult = await db.prepare(`SELECT count(id) as total FROM premium_codes WHERE status = 'ACTIVE'`).first();
  const redemptionsResult = await db.prepare(`SELECT count(id) as total FROM premium_code_redemptions`).first();

  return c.json({
    purchases: purchasesResult?.total || 0,
    activeEntitlements: entitlementsResult?.total || 0,
    lifetimeUsers: lifetimeResult?.total || 0,
    totalCodes: codesResult?.total || 0,
    totalRedemptions: redemptionsResult?.total || 0,
    validationFailures: 0,
    restoredPurchases: 0,
  });
});

adminMonetizationRoutes.get('/codes', async (c) => {
  const db = c.env.DB;
  const page = Math.max(1, Number(c.req.query('page') ?? 1));
  const limit = Math.min(100, Math.max(1, Number(c.req.query('limit') ?? 50)));
  const offset = (page - 1) * limit;

  const { results } = await db.prepare(
    `SELECT * FROM premium_codes ORDER BY created_at DESC LIMIT ? OFFSET ?`
  ).bind(limit, offset).all();
  const countRow = await db.prepare(`SELECT COUNT(*) as total FROM premium_codes`).first<{ total: number }>();

  return c.json({ codes: results, total: countRow?.total ?? 0, page, limit });
});

adminMonetizationRoutes.get('/codes/:id', async (c) => {
  const db = c.env.DB;
  const id = c.req.param('id');
  const code = await db.prepare(`SELECT * FROM premium_codes WHERE id = ?`).bind(id).first();
  if (!code) return c.json({ error: 'Code not found' }, 404);
  return c.json({ code });
});

const createCodeSchema = z.object({
  planType: z.enum(['VITALICIO', 'PERSONALIZADO']),
  customDays: z.number().int().min(1).max(3650).optional(),
  internalName: z.string().optional(),
  description: z.string().optional(),
  features: z.string().optional(),
  maxRedemptions: z.number().int().min(1).default(1),
  validFrom: z.number().optional(),
  validUntil: z.number().optional(),
  codePrefix: z.string().default('QF'),
  customCode: z.string().max(64).optional(),
}).refine(
  (data) => data.planType !== 'PERSONALIZADO' || (data.customDays !== undefined && data.customDays > 0),
  { message: 'customDays é obrigatório quando planType é PERSONALIZADO', path: ['customDays'] }
);

adminMonetizationRoutes.post('/codes', zValidator('json', createCodeSchema), async (c) => {
  const data = c.req.valid('json');
  const db = c.env.DB;
  const now = Math.floor(Date.now() / 1000);

  const planPrefix = { VITALICIO: 'VIT', PERSONALIZADO: 'TMP' }[data.planType];
  const generatedCode = `${data.codePrefix}-${planPrefix}-${generateSecureSuffix(6)}`;
  const rawCode = (data.customCode && data.customCode.trim().length > 0)
    ? data.customCode.trim().toUpperCase().replace(/\s/g, '')
    : generatedCode;

  const codeHash = await sha256(rawCode);
  const id = crypto.randomUUID();

  const durationType = data.planType === 'VITALICIO' ? 'LIFETIME' : 'DAYS';
  const durationValue = data.planType === 'PERSONALIZADO' ? data.customDays! : null;
  const planLabel = data.planType === 'PERSONALIZADO' ? `${data.customDays} dias` : 'Vitalício';
  const name = data.internalName || `Código Premium ${planLabel}`;

  try {
    const batchResult = await db.batch([
      db.prepare(`
        INSERT INTO premium_codes (
          id, code_hash, code_prefix, internal_name, description, benefit_type,
          features, duration_type, duration_value, max_redemptions, valid_from, valid_until, created_at, updated_at
        )
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      `).bind(
        id, codeHash, rawCode, name, data.description || null, data.planType,
        data.features || null, durationType, durationValue, data.maxRedemptions,
        data.validFrom || now, data.validUntil || null, now, now
      )
    ]);

    if (!batchResult[0].success) {
      return c.json({ error: 'Falha ao salvar o código no banco de dados.' }, 500);
    }
  } catch (err) {
    console.error('Failed to insert premium code:', err);
    return c.json({ error: 'Erro interno ao criar código.' }, 500);
  }

  return c.json({
    success: true,
    code: rawCode,
    id,
    planType: data.planType,
    durationDays: durationValue,
  });
});

const revokeSchema = z.object({
  reason: z.string().min(3).max(200).optional(),
  revokedBy: z.string().optional(),
});

adminMonetizationRoutes.delete('/codes/:id', zValidator('json', revokeSchema), async (c) => {
  const db = c.env.DB;
  const id = c.req.param('id');
  const data = c.req.valid('json');
  const now = Math.floor(Date.now() / 1000);

  const existing = await db.prepare(`SELECT id, status FROM premium_codes WHERE id = ?`).bind(id).first<{ id: string; status: string }>();
  if (!existing) return c.json({ error: 'Code not found' }, 404);
  if (existing.status === 'REVOKED') return c.json({ error: 'Code already revoked' }, 409);

  await db.prepare(
    `UPDATE premium_codes SET status = 'REVOKED', revoked_at = ?, revoked_by = ?, revocation_reason = ?, updated_at = ? WHERE id = ?`
  ).bind(now, data.revokedBy || 'admin', data.reason || null, now, id).run();

  return c.json({ success: true, message: 'Código revogado com sucesso.' });
});

adminMonetizationRoutes.get('/purchases', async (c) => {
  const db = c.env.DB;
  const { results } = await db.prepare(`SELECT * FROM monetization_purchases ORDER BY purchased_at DESC LIMIT 100`).all();
  return c.json({ purchases: results });
});

adminMonetizationRoutes.get('/users', async (c) => {
  const db = c.env.DB;
  const { results } = await db.prepare(`SELECT * FROM monetization_entitlements ORDER BY created_at DESC LIMIT 100`).all();
  return c.json({ users: results });
});
