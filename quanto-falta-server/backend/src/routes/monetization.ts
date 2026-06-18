import { Hono } from 'hono';
import { z } from 'zod';
import { zValidator } from '@hono/zod-validator';
import type { Env } from '../types';
import { getGoogleAccessToken, verifyGooglePlayPurchase } from '../utils/googlePlay';

export const monetizationRoutes = new Hono<{ Bindings: Env }>();

const LIFETIME_PRODUCT_ID = 'premium_lifetime';
const LEGACY_SUBSCRIPTION_PRODUCT_IDS = new Set(['premium_monthly', 'premium_annual']);

async function sha256(input: string): Promise<string> {
  const encoder = new TextEncoder();
  const data = encoder.encode(input);
  const hashBuffer = await crypto.subtle.digest('SHA-256', data);
  const hashArray = Array.from(new Uint8Array(hashBuffer));
  return hashArray.map((b) => b.toString(16).padStart(2, '0')).join('');
}

monetizationRoutes.get('/monetization/entitlements', async (c) => {
  const userId = c.req.query('userId');
  const installationId = c.req.query('installationId');

  if (!userId && !installationId) {
    return c.json({ error: 'Missing userId or installationId' }, 400);
  }

  const db = c.env.DB;
  const now = Math.floor(Date.now() / 1000);

  const { results } = await db.prepare(`
    SELECT * FROM monetization_entitlements
    WHERE (user_id = ? OR user_id = ?)
      AND status = 'ACTIVE'
      AND (expires_at IS NULL OR expires_at > ?)
  `).bind(userId || '', installationId || '', now).all();

  return c.json({ entitlements: results });
});

const redeemSchema = z.object({
  code: z.string().min(4).max(64).trim(),
  userId: z.string().optional(),
  installationId: z.string().min(1),
  platform: z.string(),
  appVersion: z.string(),
});

monetizationRoutes.post('/monetization/redeem', zValidator('json', redeemSchema), async (c) => {
  const data = c.req.valid('json');
  const db = c.env.DB;
  const now = Math.floor(Date.now() / 1000);

  const normalizedCode = data.code.toUpperCase().replace(/\s/g, '').trim();
  const codeHash = await sha256(normalizedCode);

  const codeRow = await db.prepare(`
    SELECT * FROM premium_codes
    WHERE code_hash = ?
      AND status = 'ACTIVE'
      AND valid_from <= ?
      AND (valid_until IS NULL OR valid_until > ?)
  `).bind(codeHash, now, now).first<any>();

  if (!codeRow) {
    return c.json({ error: 'Este código não é válido ou já expirou.' }, 400);
  }

  if (codeRow.redemption_count >= codeRow.max_redemptions) {
    return c.json({ error: 'Este código atingiu o limite de ativações.' }, 400);
  }

  const userId = data.userId || data.installationId;
  const existingRedemption = await db.prepare(`
    SELECT id FROM premium_code_redemptions
    WHERE code_id = ? AND (user_id = ? OR installation_id = ?)
  `).bind(codeRow.id, userId, data.installationId).first();

  if (existingRedemption) {
    return c.json({ error: 'Você já resgatou este código.' }, 409);
  }

  let expiresAt: number | null = null;
  if (codeRow.duration_type === 'DAYS' && codeRow.duration_value) {
    expiresAt = now + codeRow.duration_value * 86400;
  } else if (codeRow.duration_type === 'MONTHS' && codeRow.duration_value) {
    const d = new Date(now * 1000);
    d.setMonth(d.getMonth() + codeRow.duration_value);
    expiresAt = Math.floor(d.getTime() / 1000);
  } else if (codeRow.duration_type === 'LIFETIME') {
    expiresAt = null;
  }

  const entitlementId = crypto.randomUUID();
  const redemptionId = crypto.randomUUID();

  try {
    const batchResult = await db.batch([
      db.prepare(`UPDATE premium_codes SET redemption_count = redemption_count + 1, updated_at = ? WHERE id = ?`)
        .bind(now, codeRow.id),
      db.prepare(`
        INSERT INTO monetization_entitlements
          (id, user_id, type, source, code_id, status, features, starts_at, expires_at)
        VALUES (?, ?, 'PREMIUM_CODE', 'CODE', ?, 'ACTIVE', ?, ?, ?)
      `).bind(entitlementId, userId, codeRow.id, codeRow.features, now, expiresAt),
      db.prepare(`
        INSERT INTO premium_code_redemptions
          (id, code_id, user_id, installation_id, entitlement_id, app_version, platform)
        VALUES (?, ?, ?, ?, ?, ?, ?)
      `).bind(redemptionId, codeRow.id, userId, data.installationId, entitlementId, data.appVersion, data.platform),
    ]);

    if (batchResult.some((r) => !r.success)) {
      console.error('Batch failed during redeem:', JSON.stringify(batchResult));
      return c.json({ error: 'Erro ao processar o resgate. Tente novamente.' }, 500);
    }
  } catch (err) {
    console.error('Exception during redeem batch:', err);
    return c.json({ error: 'Erro interno ao resgatar o código.' }, 500);
  }

  return c.json({
    success: true,
    message: 'Premium ativado!',
    entitlement: {
      id: entitlementId,
      expiresAt,
      features: codeRow.features,
      planType: codeRow.benefit_type,
      durationDays: (codeRow.duration_type === 'DAYS' ? codeRow.duration_value : null) as number | null,
    },
  });
});

const verifySchema = z.object({
  purchaseToken: z.string().min(1),
  productId: z.string().min(1),
  userId: z.string().optional(),
  installationId: z.string().min(1),
});

monetizationRoutes.post('/monetization/verify-purchase', zValidator('json', verifySchema), async (c) => {
  const data = c.req.valid('json');
  const db = c.env.DB;
  const now = Math.floor(Date.now() / 1000);
  const userId = data.userId || data.installationId;
  const isLifetimePurchase = data.productId === LIFETIME_PRODUCT_ID;
  const isLegacySubscription = LEGACY_SUBSCRIPTION_PRODUCT_IDS.has(data.productId);

  if (!isLifetimePurchase && !isLegacySubscription) {
    return c.json({ error: 'Produto Premium não reconhecido.' }, 400);
  }

  const tokenHash = await sha256(data.purchaseToken);
  const existing = await db.prepare(
    `SELECT id FROM monetization_purchases WHERE purchase_token_hash = ?`
  ).bind(tokenHash).first<{ id: string }>();

  if (existing) {
    const entitlement = await db.prepare(`
      SELECT id, product_id, features, expires_at FROM monetization_entitlements
      WHERE user_id = ?
        AND source = 'GOOGLE_PLAY'
        AND product_id = ?
        AND status = 'ACTIVE'
        AND (expires_at IS NULL OR expires_at > ?)
      ORDER BY created_at DESC
      LIMIT 1
    `).bind(userId, data.productId, now).first<any>();

    return c.json({
      success: true,
      alreadyRegistered: true,
      message: 'Compra já registrada.',
      entitlement: entitlement ? {
        id: entitlement.id,
        expiresAt: entitlement.expires_at ?? null,
        features: entitlement.features ?? null,
        planType: planTypeForProduct(data.productId),
      } : null,
    });
  }

  let expiresAt: number | null = null;
  let autoRenewing = false;

  if (c.env.GOOGLE_PLAY_CREDENTIALS) {
    const accessToken = await getGoogleAccessToken(c.env.GOOGLE_PLAY_CREDENTIALS);
    if (!accessToken) {
      return c.json({ error: 'Erro de autenticação interna com Google Play.' }, 500);
    }

    const googleResult = await verifyGooglePlayPurchase(
      c.env.APP_PACKAGE_NAME || 'com.phdev.quantofalta',
      data.productId,
      data.purchaseToken,
      isLegacySubscription,
      accessToken
    );

    if (!googleResult.isValid) {
      return c.json({ error: 'Token de compra inválido ou expirado.' }, 400);
    }

    if (isLegacySubscription && googleResult.expiryTimeMillis) {
      expiresAt = Math.floor(googleResult.expiryTimeMillis / 1000);
      autoRenewing = googleResult.isAutoRenewing ?? false;
    }
  } else {
    console.warn('GOOGLE_PLAY_CREDENTIALS not set, rejecting purchase verification.');
    return c.json({ error: 'Configuração do servidor ausente.' }, 500);
  }

  const entitlementId = crypto.randomUUID();
  const purchaseId = crypto.randomUUID();
  const entitlementType = isLifetimePurchase ? 'PREMIUM_LIFETIME_PURCHASE' : 'PREMIUM_LEGACY_SUBSCRIPTION';
  const planType = planTypeForProduct(data.productId);

  try {
    const batchResult = await db.batch([
      db.prepare(`
        INSERT INTO monetization_purchases
          (id, user_id, installation_id, platform, product_id, purchase_token_hash, purchase_state, purchased_at, expires_at, auto_renewing, last_verified_at)
        VALUES (?, ?, ?, 'ANDROID', ?, ?, 'PURCHASED', ?, ?, ?, ?)
      `).bind(purchaseId, userId, data.installationId, data.productId, tokenHash, now, expiresAt, autoRenewing ? 1 : 0, now),
      db.prepare(`
        INSERT INTO monetization_entitlements
          (id, user_id, type, source, product_id, status, starts_at, expires_at, last_synced_at)
        VALUES (?, ?, ?, 'GOOGLE_PLAY', ?, 'ACTIVE', ?, ?, ?)
      `).bind(entitlementId, userId, entitlementType, data.productId, now, expiresAt, now),
    ]);

    if (batchResult.some((r) => !r.success)) {
      return c.json({ error: 'Erro ao registrar a compra.' }, 500);
    }
  } catch (err) {
    console.error('Exception during verify-purchase batch:', err);
    return c.json({ error: 'Erro interno ao verificar a compra.' }, 500);
  }

  return c.json({
    success: true,
    message: 'Compra validada com sucesso.',
    entitlement: {
      id: entitlementId,
      expiresAt,
      features: null,
      planType,
    },
  });
});

function planTypeForProduct(productId: string): 'VITALICIO' | 'ANUAL' | 'MENSAL' {
  if (productId === 'premium_annual') return 'ANUAL';
  if (productId === 'premium_monthly') return 'MENSAL';
  return 'VITALICIO';
}
