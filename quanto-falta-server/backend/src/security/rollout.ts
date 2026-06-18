/**
 * Deterministic rollout eligibility based on stable hash of installationId + versionCode.
 * Same installation always gets the same result for the same version.
 * No random drift between checks.
 */
export function computeRolloutEligibility(
  installationId: string,
  versionCode: number,
  rolloutPercentage: number
): boolean {
  if (rolloutPercentage <= 0) return false;
  if (rolloutPercentage >= 100) return true;

  // Simple deterministic hash without crypto (sync)
  const input = `${installationId}:${versionCode}`;
  const hash = simpleHash(input);
  const bucket = hash % 100;
  return bucket < rolloutPercentage;
}

/** FNV-1a 32-bit hash — fast, deterministic, no async needed */
function simpleHash(input: string): number {
  let hash = 0x811c9dc5;
  for (let i = 0; i < input.length; i++) {
    hash ^= input.charCodeAt(i);
    hash = (hash * 0x01000193) >>> 0; // FNV prime, unsigned 32-bit
  }
  return hash;
}
