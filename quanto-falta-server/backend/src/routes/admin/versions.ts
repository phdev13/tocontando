import { Hono } from 'hono';
import { z } from 'zod';
import type { Env } from '../../types';

export const adminVersionsRoutes = new Hono<{ Bindings: Env }>();

const VersionSchema = z.object({
  versionCode: z.number().int().positive(),
  versionName: z.string().min(1).max(20),
  releaseChannel: z.enum(['stable', 'beta', 'internal']).default('stable'),
  mandatory: z.boolean().default(false),
  minSupportedVersionCode: z.number().int().positive().default(1),
  title: z.string().max(100).default('Nova atualização disponível'),
  summary: z.string().max(300).default(''),
  changelog: z.array(z.string().max(200)).max(20).default([]),
  githubReleaseTag: z.string().max(256).optional(),
  apkSizeBytes: z.number().int().optional(),
  sha256: z.string().max(64).optional(),
  signatureFingerprint: z.string().max(128).optional(),
  rolloutPercentage: z.number().int().min(0).max(100).default(0),
});

/** GET /admin/versions — list all versions with stats */
adminVersionsRoutes.get('/', async (c) => {
  const channel = c.req.query('channel');
  const status = c.req.query('status');
  const limit = Math.min(parseInt(c.req.query('limit') ?? '50', 10), 100);
  const offset = parseInt(c.req.query('offset') ?? '0', 10);

  let query = `SELECT v.*,
    (SELECT COUNT(*) FROM installations WHERE version_code = v.version_code) as installations_count,
    (SELECT COUNT(*) FROM ota_attempts WHERE version_code = v.version_code AND event_type = 'adopted') as adoption_count,
    (SELECT COUNT(*) FROM ota_attempts WHERE version_code = v.version_code AND event_type = 'download_started') as downloads_started,
    (SELECT COUNT(*) FROM ota_attempts WHERE version_code = v.version_code AND event_type = 'failed') as failures_count
  FROM app_versions v WHERE 1=1`;

  const params: (string | number)[] = [];
  if (channel) { query += ' AND v.release_channel = ?'; params.push(channel); }
  if (status) { query += ' AND v.status = ?'; params.push(status); }
  query += ' ORDER BY v.version_code DESC LIMIT ? OFFSET ?';
  params.push(limit, offset);

  const { results } = await c.env.DB.prepare(query).bind(...params).all();
  return c.json({ versions: results, limit, offset });
});

/** POST /admin/versions — create a new version (starts in draft) */
adminVersionsRoutes.post('/', async (c) => {
  const body = await c.req.json().catch(() => null);
  if (!body) return c.json({ error: 'Invalid body' }, 400);

  const parsed = VersionSchema.safeParse(body);
  if (!parsed.success) return c.json({ error: 'Invalid payload', details: parsed.error.flatten() }, 400);

  const d = parsed.data;

  // Prevent duplicate versionCode
  const existing = await c.env.DB.prepare(
    `SELECT id FROM app_versions WHERE version_code = ? LIMIT 1`
  ).bind(d.versionCode).first();
  if (existing) return c.json({ error: 'Version code already exists', code: 'DUPLICATE_VERSION' }, 409);

  const result = await c.env.DB.prepare(
    `INSERT INTO app_versions(version_code, version_name, release_channel, status, mandatory,
       min_supported_version_code, title, summary, changelog, github_release_tag, apk_size_bytes,
       sha256, signature_fingerprint, rollout_percentage)
     VALUES(?, ?, ?, 'draft', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`
  ).bind(
    d.versionCode, d.versionName, d.releaseChannel, d.mandatory ? 1 : 0,
    d.minSupportedVersionCode, d.title, d.summary, JSON.stringify(d.changelog),
    d.githubReleaseTag ?? null, d.apkSizeBytes ?? null, d.sha256 ?? null,
    d.signatureFingerprint ?? null, d.rolloutPercentage,
  ).run();

  await auditLog(c, 'version_created', 'app_version', String(d.versionCode));
  return c.json({ id: result.meta.last_row_id, versionCode: d.versionCode, status: 'draft' }, 201);
});

/** PATCH /admin/versions/:versionCode — update rollout, status, etc. */
adminVersionsRoutes.patch('/:versionCode', async (c) => {
  const versionCode = parseInt(c.req.param('versionCode'), 10);
  const body = await c.req.json().catch(() => null);

  const UpdateSchema = z.object({
    status: z.enum(['draft', 'active', 'paused', 'retired']).optional(),
    mandatory: z.boolean().optional(),
    rolloutPercentage: z.number().int().min(0).max(100).optional(),
    releaseChannel: z.enum(['stable', 'beta', 'internal']).optional(),
    minSupportedVersionCode: z.number().int().positive().optional(),
    githubReleaseTag: z.string().max(256).optional(),
    apkSizeBytes: z.number().int().optional(),
    sha256: z.string().max(64).optional(),
  });

  const parsed = UpdateSchema.safeParse(body);
  if (!parsed.success) return c.json({ error: 'Invalid payload' }, 400);

  const updates: string[] = ['updated_at = ?'];
  const params: (string | number | null)[] = [Date.now()];

  const d = parsed.data;
  if (d.status !== undefined) { updates.push('status = ?'); params.push(d.status); }
  if (d.mandatory !== undefined) { updates.push('mandatory = ?'); params.push(d.mandatory ? 1 : 0); }
  if (d.rolloutPercentage !== undefined) { updates.push('rollout_percentage = ?'); params.push(d.rolloutPercentage); }
  if (d.releaseChannel !== undefined) { updates.push('release_channel = ?'); params.push(d.releaseChannel); }
  if (d.minSupportedVersionCode !== undefined) { updates.push('min_supported_version_code = ?'); params.push(d.minSupportedVersionCode); }
  if (d.githubReleaseTag !== undefined) { updates.push('github_release_tag = ?'); params.push(d.githubReleaseTag); }
  if (d.apkSizeBytes !== undefined) { updates.push('apk_size_bytes = ?'); params.push(d.apkSizeBytes); }
  if (d.sha256 !== undefined) { updates.push('sha256 = ?'); params.push(d.sha256); }

  // Activate: set published_at
  if (d.status === 'active') { updates.push('published_at = ?'); params.push(Date.now()); }

  params.push(versionCode);
  await c.env.DB.prepare(
    `UPDATE app_versions SET ${updates.join(', ')} WHERE version_code = ?`
  ).bind(...params).run();

  await auditLog(c, 'version_updated', 'app_version', String(versionCode), d);
  return c.json({ updated: true });
});

async function auditLog(c: any, action: string, targetType: string, targetId: string, details?: unknown) {
  const adminUserId = c.get('adminUserId') as number;
  await c.env.DB.prepare(
    `INSERT INTO audit_logs(admin_user_id, action, target_type, target_id, details)
     VALUES(?, ?, ?, ?, ?)`
  ).bind(adminUserId, action, targetType, targetId, details ? JSON.stringify(details) : null).run().catch(() => {});
}

/** POST /admin/versions/github-release — create GitHub release and upload APK */
adminVersionsRoutes.post('/github-release', async (c) => {
  if (!c.env.GITHUB_PAT) {
    return c.json({ error: 'GitHub PAT is not configured on the server.' }, 500);
  }

  const formData = await c.req.parseBody().catch(() => null);
  if (!formData) return c.json({ error: 'Invalid form data' }, 400);

  const apkFile = formData['apk'];
  if (!(apkFile instanceof File)) return c.json({ error: 'Missing apk file' }, 400);

  const versionCodeStr = formData['versionCode'] as string;
  const versionName = formData['versionName'] as string;
  const title = formData['title'] as string;
  const notes = formData['notes'] as string;

  if (!versionCodeStr || !versionName || !title) {
    return c.json({ error: 'Missing required fields' }, 400);
  }

  const versionCode = parseInt(versionCodeStr, 10);
  if (isNaN(versionCode)) return c.json({ error: 'Invalid versionCode' }, 400);

  // Prevent duplicate versionCode
  const existing = await c.env.DB.prepare(
    `SELECT id FROM app_versions WHERE version_code = ? LIMIT 1`
  ).bind(versionCode).first();
  if (existing) return c.json({ error: 'Version code already exists', code: 'DUPLICATE_VERSION' }, 409);

  try {
    // 1. Create GitHub Release
    const releasePayload = {
      tag_name: versionName,
      name: title,
      body: notes,
      draft: false,
      prerelease: false,
    };

    const repoPath = c.env.GITHUB_REPO ?? 'phdev13/ToContando';
    const ghRes = await fetch(`https://api.github.com/repos/${repoPath}/releases`, {
      method: 'POST',
      headers: {
        'Authorization': `token ${c.env.GITHUB_PAT}`,
        'User-Agent': 'ToContando-Worker',
        'Accept': 'application/vnd.github.v3+json',
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(releasePayload),
    });

    if (!ghRes.ok) {
      const err = await ghRes.text();
      return c.json({ error: 'Failed to create GitHub Release', details: err }, 502);
    }

    const releaseData = await ghRes.json() as any;
    const uploadUrlTemplate = releaseData.upload_url as string; 
    const uploadUrl = uploadUrlTemplate.replace('{?name,label}', `?name=app-release-${versionName}.apk`);

    // 2. Upload APK Asset
    const assetRes = await fetch(uploadUrl, {
      method: 'POST',
      headers: {
        'Authorization': `token ${c.env.GITHUB_PAT}`,
        'User-Agent': 'ToContando-Worker',
        'Accept': 'application/vnd.github.v3+json',
        'Content-Type': 'application/vnd.android.package-archive',
        'Content-Length': apkFile.size.toString(),
      },
      body: await apkFile.arrayBuffer(),
    });

    if (!assetRes.ok) {
      const err = await assetRes.text();
      return c.json({ error: 'Failed to upload APK to GitHub', details: err }, 502);
    }

    // 3. Register in DB
    const result = await c.env.DB.prepare(
      `INSERT INTO app_versions(version_code, version_name, release_channel, status, mandatory,
         min_supported_version_code, title, summary, changelog, github_release_tag, apk_size_bytes, rollout_percentage, published_at)
       VALUES(?, ?, 'stable', 'active', 0, 1, ?, ?, ?, ?, ?, 100, ?)`
    ).bind(
      versionCode, versionName, title, notes, JSON.stringify([notes]), versionName, apkFile.size, Date.now()
    ).run();

    await auditLog(c, 'version_created_github', 'app_version', String(versionCode));
    return c.json({ id: result.meta.last_row_id, versionCode, status: 'active', github_release_id: releaseData.id }, 201);
  } catch (error: any) {
    return c.json({ error: 'Upload process failed', details: error.message }, 500);
  }
});
