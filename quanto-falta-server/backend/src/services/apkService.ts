/**
 * Generates a short-lived download token URL for GitHub proxy.
 */
export async function generateSignedApkUrl(
  githubReleaseTag: string,
  ttlSeconds: number
): Promise<string> {
  const expiresAt = Math.floor(Date.now() / 1000) + ttlSeconds;
  const encodedTag = encodeURIComponent(githubReleaseTag);
  return `/api/v1/apk/download?tag=${encodedTag}&expires=${expiresAt}`;
}

export function isApkTokenValid(expiresAt: number): boolean {
  return Math.floor(Date.now() / 1000) < expiresAt;
}
