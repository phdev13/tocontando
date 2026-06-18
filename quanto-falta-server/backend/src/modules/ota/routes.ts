import { Hono } from 'hono';
import { z } from 'zod';
import type { Env } from '../../types';
import { fail, ok } from '../../contracts/apiResponse';
import { computeRolloutEligibility } from '../../security/rollout';
import { generateSignedApkUrl } from '../../services/apkService';
import { getSetting } from '../../services/configService';

export const otaAppRoutes = new Hono<{ Bindings: Env }>();

const CheckUpdateSchema = z.object({
  versionCode: z.coerce.number().int().positive(),
  versionName: z.string().min(1).max(20),
  packageName: z.string().min(1).max(100),
  releaseChannel: z.enum(['stable', 'beta', 'internal']).default('stable'),
  androidVersion: z.string().max(10).optional(),
  architecture: z.string().max(20).optional(),
  installationId: z.string().uuid(),
});

otaAppRoutes.get('/ota/check', async (c) => {
  const parsed = CheckUpdateSchema.safeParse(c.req.query());
  if (!parsed.success) return fail(c, 'INVALID_REQUEST', 'Parametros de OTA invalidos.', 400);

  const { versionCode, packageName, releaseChannel, installationId } = parsed.data;

  if (packageName !== c.env.APP_PACKAGE_NAME) {
    return ok(c, { updateAvailable: false });
  }

  const latestVersion = await c.env.DB.prepare(
    `SELECT * FROM app_versions
     WHERE release_channel = ?
       AND status = 'active'
       AND rollout_percentage > 0
     ORDER BY version_code DESC
     LIMIT 1`
  ).bind(releaseChannel).first<AppVersion>();

  if (!latestVersion || latestVersion.version_code <= versionCode) {
    return ok(c, { updateAvailable: false });
  }

  const isEligible = computeRolloutEligibility(
    installationId,
    latestVersion.version_code,
    latestVersion.rollout_percentage,
  );

  if (!isEligible) return ok(c, { updateAvailable: false });

  const minSupportedStr = await getSetting(c.env.DB, 'min_supported_version_code');
  const minSupported = parseInt(minSupportedStr ?? '1', 10);
  const mandatory = latestVersion.mandatory === 1 || versionCode < minSupported;

  await c.env.DB.prepare(
    `INSERT INTO ota_attempts(installation_id, version_code, event_type)
     VALUES(?, ?, 'check')`
  ).bind(installationId, latestVersion.version_code).run().catch(() => undefined);

  let apkUrl = latestVersion.github_release_tag
    ? await generateSignedApkUrl(latestVersion.github_release_tag, 3600)
    : null;

  if (apkUrl && apkUrl.startsWith('/')) {
    apkUrl = new URL(c.req.url).origin + apkUrl;
  }

  c.header('Cache-Control', 'private, max-age=300');

  return ok(c, {
    updateAvailable: true,
    mandatory,
    versionCode: latestVersion.version_code,
    versionName: latestVersion.version_name,
    minimumSupportedVersionCode: minSupported,
    title: latestVersion.title,
    summary: latestVersion.summary,
    changelog: JSON.parse(latestVersion.changelog ?? '[]'),
    apkUrl,
    apkSize: null,
    sha256: latestVersion.sha256,
    signatureFingerprint: latestVersion.signature_fingerprint,
    publishedAt: latestVersion.published_at ? new Date(latestVersion.published_at).toISOString() : null,
    rolloutPercentage: latestVersion.rollout_percentage,
    releaseChannel: latestVersion.release_channel,
  });
});

interface AppVersion {
  version_code: number;
  version_name: string;
  release_channel: string;
  mandatory: number;
  title: string;
  summary: string;
  changelog: string;
  github_release_tag: string | null;
  sha256: string | null;
  signature_fingerprint: string | null;
  rollout_percentage: number;
  published_at: number | null;
}
