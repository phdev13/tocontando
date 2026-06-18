import { Hono } from 'hono';
import { z } from 'zod';
import { zValidator } from '@hono/zod-validator';
import type { Env } from '../../types';
import { fail, ok } from '../../contracts/apiResponse';
import { getGoogleAccessToken, verifyGooglePlayPurchase } from '../../utils/googlePlay';

export const premiumAppRoutes = new Hono<{ Bindings: Env }>();

const LIFETIME_PRODUCT_ID = 'premium_lifetime';
const LEGACY_SUBSCRIPTION_PRODUCT_IDS = new Set(['premium_monthly', 'premium_annual']);

premiumAppRoutes.get('/premium/status', async (c) => {
  const userId = c.req.query('userId');
  const installationId = c.req.query('installationId');

  if (!userId && !installationId) {
    return fail(c, 'INVALID_REQUEST', 'Informe userId ou installationId.', 400);
  }

  const now = Math.floor(Date.now() / 1000);
  const entitlement = await c.env.DB.prepare(`
    SELECT id, type, source, product_id, features, starts_at, expires_at
    FROM monetization_entitlements
    WHERE (user_id = ? OR user_id = ?)
      AND status = 'ACTIVE'
      AND (expires_at IS NULL OR expires_at > ?)
    ORDER BY created_at DESC
    LIMIT 1
  `).bind(userId || '', installationId || '', now).first<any>();

  return ok(c, {
    premium: Boolean(entitlement),
    source: entitlement?.source ?? null,
    startsAt: entitlement?.starts_at ?? null,
    expiresAt: entitlement?.expires_at ?? null,
    features: parseJson(entitlement?.features),
    limits: {
      eventLimit: entitlement ? null : 5,
    },
  });
});

const redeemSchema = z.object({
  code: z.string().min(4).max(64).trim(),
  userId: z.string().optional(),
  installationId: z.string().min(1),
  platform: z.string(),
  appVersion: z.string(),
});

premiumAppRoutes.post('/premium/activate-token', zValidator('json', redeemSchema), async (c) => {
  const data = c.req.valid('json');
  const now = Math.floor(Date.now() / 1000);
  const normalizedCode = data.code.toUpperCase().replace(/\s/g, '').trim();
  const codeHash = await sha256(normalizedCode);

  const codeRow = await c.env.DB.prepare(`
    SELECT * FROM premium_codes
    WHERE code_hash = ?
      AND status = 'ACTIVE'
      AND valid_from <= ?
      AND (valid_until IS NULL OR valid_until > ?)
  `).bind(codeHash, now, now).first<any>();

  if (!codeRow) return fail(c, 'INVALID_TOKEN', 'Codigo invalido ou expirado.', 400);
  if (codeRow.redemption_count >= codeRow.max_redemptions) {
    return fail(c, 'TOKEN_ALREADY_USED', 'Este codigo atingiu o limite de ativacoes.', 409);
  }

  const userId = data.userId || data.installationId;
  const existingRedemption = await c.env.DB.prepare(`
    SELECT id FROM premium_code_redemptions
    WHERE code_id = ? AND (user_id = ? OR installation_id = ?)
  `).bind(codeRow.id, userId, data.installationId).first();

  if (existingRedemption) return fail(c, 'TOKEN_ALREADY_USED', 'Este codigo ja foi usado nesta instalacao.', 409);

  const expiresAt = expirationForCode(now, codeRow.duration_type, codeRow.duration_value);
  const entitlementId = crypto.randomUUID();
  const redemptionId = crypto.randomUUID();

  const batchResult = await c.env.DB.batch([
    c.env.DB.prepare(`UPDATE premium_codes SET redemption_count = redemption_count + 1, updated_at = ? WHERE id = ?`)
      .bind(now, codeRow.id),
    c.env.DB.prepare(`
      INSERT INTO monetization_entitlements
        (id, user_id, type, source, code_id, status, features, starts_at, expires_at)
      VALUES (?, ?, 'PREMIUM_CODE', 'CODE', ?, 'ACTIVE', ?, ?, ?)
    `).bind(entitlementId, userId, codeRow.id, codeRow.features, now, expiresAt),
    c.env.DB.prepare(`
      INSERT INTO premium_code_redemptions
        (id, code_id, user_id, installation_id, entitlement_id, app_version, platform)
      VALUES (?, ?, ?, ?, ?, ?, ?)
    `).bind(redemptionId, codeRow.id, userId, data.installationId, entitlementId, data.appVersion, data.platform),
  ]);

  if (batchResult.some((result) => !result.success)) {
    return fail(c, 'INTERNAL_ERROR', 'Nao foi possivel ativar o Premium.', 500);
  }

  return ok(c, {
    premium: true,
    entitlement: {
      id: entitlementId,
      expiresAt,
      features: parseJson(codeRow.features),
      planType: codeRow.benefit_type,
    },
  }, 201);
});

const verifySchema = z.object({
  purchaseToken: z.string().min(1),
  productId: z.string().min(1),
  userId: z.string().optional(),
  installationId: z.string().min(1),
});

premiumAppRoutes.post('/premium/verify-purchase', zValidator('json', verifySchema), async (c) => {
  const data = c.req.valid('json');
  const now = Math.floor(Date.now() / 1000);
  const userId = data.userId || data.installationId;
  const isLifetimePurchase = data.productId === LIFETIME_PRODUCT_ID;
  const isLegacySubscription = LEGACY_SUBSCRIPTION_PRODUCT_IDS.has(data.productId);

  if (!isLifetimePurchase && !isLegacySubscription) {
    return fail(c, 'PURCHASE_INVALID', 'Produto Premium nao reconhecido.', 400);
  }

  if (!c.env.GOOGLE_PLAY_CREDENTIALS) {
    return fail(c, 'INTERNAL_ERROR', 'Verificacao de compras indisponivel.', 500);
  }

  const tokenHash = await sha256(data.purchaseToken);
  const existing = await c.env.DB.prepare(
    `SELECT id FROM monetization_purchases WHERE purchase_token_hash = ?`
  ).bind(tokenHash).first<{ id: string }>();

  if (existing) {
    return ok(c, { verified: true, alreadyRegistered: true });
  }

  const accessToken = await getGoogleAccessToken(c.env.GOOGLE_PLAY_CREDENTIALS);
  if (!accessToken) return fail(c, 'INTERNAL_ERROR', 'Falha ao autenticar no Google Play.', 500);

  const googleResult = await verifyGooglePlayPurchase(
    c.env.APP_PACKAGE_NAME || 'com.phdev.quantofalta',
    data.productId,
    data.purchaseToken,
    isLegacySubscription,
    accessToken,
  );

  if (!googleResult.isValid) return fail(c, 'PURCHASE_INVALID', 'Compra invalida ou expirada.', 400);

  const expiresAt = isLegacySubscription && googleResult.expiryTimeMillis
    ? Math.floor(googleResult.expiryTimeMillis / 1000)
    : null;
  const entitlementId = crypto.randomUUID();
  const purchaseId = crypto.randomUUID();

  const batchResult = await c.env.DB.batch([
    c.env.DB.prepare(`
      INSERT INTO monetization_purchases
        (id, user_id, installation_id, platform, product_id, purchase_token_hash, purchase_state, purchased_at, expires_at, auto_renewing, last_verified_at)
      VALUES (?, ?, ?, 'ANDROID', ?, ?, 'PURCHASED', ?, ?, ?, ?)
    `).bind(purchaseId, userId, data.installationId, data.productId, tokenHash, now, expiresAt, googleResult.isAutoRenewing ? 1 : 0, now),
    c.env.DB.prepare(`
      INSERT INTO monetization_entitlements
        (id, user_id, type, source, product_id, status, starts_at, expires_at, last_synced_at)
      VALUES (?, ?, ?, 'GOOGLE_PLAY', ?, 'ACTIVE', ?, ?, ?)
    `).bind(entitlementId, userId, isLifetimePurchase ? 'PREMIUM_LIFETIME_PURCHASE' : 'PREMIUM_LEGACY_SUBSCRIPTION', data.productId, now, expiresAt, now),
  ]);

  if (batchResult.some((result) => !result.success)) {
    return fail(c, 'INTERNAL_ERROR', 'Nao foi possivel registrar a compra.', 500);
  }

  return ok(c, {
    verified: true,
    entitlement: {
      id: entitlementId,
      expiresAt,
      planType: planTypeForProduct(data.productId),
    },
  }, 201);
});

premiumAppRoutes.post('/premium/restore', async (c) => {
  return ok(c, {
    restored: false,
    message: 'Use verify-purchase para restaurar uma compra validada pelo Google Play.',
  });
});

async function sha256(input: string): Promise<string> {
  const encoder = new TextEncoder();
  const data = encoder.encode(input);
  const hashBuffer = await crypto.subtle.digest('SHA-256', data);
  const hashArray = Array.from(new Uint8Array(hashBuffer));
  return hashArray.map((b) => b.toString(16).padStart(2, '0')).join('');
}

function expirationForCode(now: number, durationType: string, durationValue: number | null): number | null {
  if (durationType === 'DAYS' && durationValue) return now + durationValue * 86400;
  if (durationType === 'MONTHS' && durationValue) {
    const date = new Date(now * 1000);
    date.setMonth(date.getMonth() + durationValue);
    return Math.floor(date.getTime() / 1000);
  }
  return null;
}

function parseJson(value: string | null | undefined) {
  if (!value) return null;
  try {
    return JSON.parse(value);
  } catch {
    return value;
  }
}

function planTypeForProduct(productId: string): 'VITALICIO' | 'ANUAL' | 'MENSAL' {
  if (productId === 'premium_annual') return 'ANUAL';
  if (productId === 'premium_monthly') return 'MENSAL';
  return 'VITALICIO';
}
