import { Hono } from 'hono';
import type { Env } from '../types';
import { isApkTokenValid } from '../services/apkService';

export const apkRoutes = new Hono<{ Bindings: Env }>();

/**
 * GET /api/v1/apk/download
 * Proxies APK from GitHub Releases securely via temporary 302 redirects.
 */
apkRoutes.get('/apk/download', async (c) => {
  const tag = c.req.query('tag');
  const expiresStr = c.req.query('expires');

  if (!tag || !expiresStr) {
    return c.json({ error: 'Invalid request', code: 'MISSING_PARAMS' }, 400);
  }

  const expiresAt = parseInt(expiresStr, 10);
  if (!isApkTokenValid(expiresAt)) {
    return c.json({ error: 'Download link expired', code: 'LINK_EXPIRED' }, 410);
  }

  if (!c.env.GITHUB_PAT) {
    return c.json({ error: 'GitHub PAT not configured', code: 'SERVER_ERROR' }, 500);
  }

  try {
    // 1. Fetch release info from GitHub API
    const repoPath = c.env.GITHUB_REPO ?? 'phdev13/ToContando';
    const githubRes = await fetch(`https://api.github.com/repos/${repoPath}/releases/tags/${tag}`, {
      headers: {
        'Authorization': `token ${c.env.GITHUB_PAT}`,
        'User-Agent': 'ToContando-Worker',
        'Accept': 'application/vnd.github.v3+json'
      }
    });

    if (!githubRes.ok) {
      return c.json({ error: 'Release not found', code: 'NOT_FOUND' }, 404);
    }

    const releaseData = await githubRes.json() as any;
    const asset = releaseData.assets?.find((a: any) => a.name.endsWith('.apk'));
    
    if (!asset) {
      return c.json({ error: 'APK asset not found in release', code: 'NOT_FOUND' }, 404);
    }

    // 2. Fetch the asset using octet-stream to get the S3 redirect URL
    const assetRes = await fetch(asset.url, {
      headers: {
        'Authorization': `token ${c.env.GITHUB_PAT}`,
        'User-Agent': 'ToContando-Worker',
        'Accept': 'application/octet-stream'
      },
      redirect: 'manual'
    });

    if (assetRes.status === 302 || assetRes.status === 301) {
      const s3Url = assetRes.headers.get('location');
      if (s3Url) return c.redirect(s3Url, 302);
    }

    // Fallback stream if GitHub doesn't redirect
    return new Response(assetRes.body, { headers: assetRes.headers });
  } catch (err) {
    return c.json({ error: 'Proxy error', code: 'PROXY_ERROR' }, 503);
  }
});
