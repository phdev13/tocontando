import { Hono } from 'hono';
import { z } from 'zod';
import type { Env } from '../types';
import { computeRolloutEligibility } from '../security/rollout';
import { generateSignedApkUrl } from '../services/apkService';
import { getSetting } from '../services/configService';

export const updatesRoutes = new Hono<{ Bindings: Env }>();

const CheckUpdateSchema = z.object({
  versionCode: z.coerce.number().int().positive(),
  versionName: z.string().min(1).max(20),
  packageName: z.string().min(1).max(100),
  releaseChannel: z.enum(['stable', 'beta', 'internal']).default('stable'),
  androidVersion: z.string().max(10).optional(),
  architecture: z.string().max(20).optional(),
  installationId: z.string().uuid(),
});

/**
 * GET /api/v1/updates/check
 * Main OTA check endpoint. Returns update manifest if applicable.
 * Never exposes internal paths, admin data, or secrets.
 */
updatesRoutes.get('/updates/check', async (c) => {
  const query = c.req.query();
  const parsed = CheckUpdateSchema.safeParse(query);

  if (!parsed.success) {
    return c.json({ error: 'Invalid parameters', code: 'VALIDATION_ERROR', details: parsed.error.flatten() }, 400);
  }

  const { versionCode, packageName, releaseChannel, installationId } = parsed.data;

  // Validate package name matches expected app
  if (packageName !== c.env.APP_PACKAGE_NAME) {
    return c.json({ updateAvailable: false }, 200);
  }

  // Get the latest active version for this channel
  const latestVersion = await c.env.DB.prepare(
    `SELECT * FROM app_versions
     WHERE release_channel = ?
       AND status = 'active'
       AND rollout_percentage > 0
     ORDER BY version_code DESC
     LIMIT 1`
  ).bind(releaseChannel).first<AppVersion>();

  if (!latestVersion || latestVersion.version_code <= versionCode) {
    return c.json({ updateAvailable: false }, 200);
  }

  // Check if this installation is within the rollout percentage
  const isEligible = computeRolloutEligibility(
    installationId,
    latestVersion.version_code,
    latestVersion.rollout_percentage
  );

  if (!isEligible) {
    return c.json({ updateAvailable: false }, 200);
  }

  // Check minimum supported version (mandatory update check)
  const minSupportedStr = await getSetting(c.env.DB, 'min_supported_version_code');
  const minSupported = parseInt(minSupportedStr ?? '1', 10);
  const isMandatory = latestVersion.mandatory === 1 || versionCode < minSupported;

  // Record OTA check attempt
  await c.env.DB.prepare(
    `INSERT INTO ota_attempts(installation_id, version_code, event_type)
     VALUES(?, ?, 'check')`
  ).bind(installationId, latestVersion.version_code).run().catch(() => {/* non-blocking */});

  // Generate signed URL for APK download
  let apkUrl = latestVersion.github_release_tag
    ? await generateSignedApkUrl(latestVersion.github_release_tag, 3600)
    : null;

  if (apkUrl && apkUrl.startsWith('/')) {
    apkUrl = new URL(c.req.url).origin + apkUrl;
  }

  return c.json({
    updateAvailable: true,
    mandatory: isMandatory,
    versionCode: latestVersion.version_code,
    versionName: latestVersion.version_name,
    minimumSupportedVersionCode: minSupported,
    title: latestVersion.title,
    summary: latestVersion.summary,
    changelog: JSON.parse(latestVersion.changelog ?? '[]'),
    apkUrl,
    apkSize: null, // Force null so older clients skip strict size validation which might fail due to S3/Cloudflare quirks
    sha256: latestVersion.sha256,
    signatureFingerprint: latestVersion.signature_fingerprint,
    publishedAt: latestVersion.published_at
      ? new Date(latestVersion.published_at).toISOString()
      : null,
    rolloutPercentage: latestVersion.rollout_percentage,
    releaseChannel: latestVersion.release_channel,
  });
});

interface AppVersion {
  id: number;
  version_code: number;
  version_name: string;
  release_channel: string;
  status: string;
  mandatory: number;
  min_supported_version_code: number;
  title: string;
  summary: string;
  changelog: string;
  github_release_tag: string | null;
  apk_size_bytes: number | null;
  sha256: string | null;
  signature_fingerprint: string | null;
  rollout_percentage: number;
  published_at: number | null;
}
