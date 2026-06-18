import { describe, it, expect } from 'vitest';
import { computeRolloutEligibility } from '../src/security/rollout';

describe('Rollout Eligibility', () => {
  it('returns false when rollout is 0%', () => {
    expect(computeRolloutEligibility('test-uuid', 24, 0)).toBe(false);
  });

  it('returns true when rollout is 100%', () => {
    expect(computeRolloutEligibility('test-uuid', 24, 100)).toBe(true);
  });

  it('is deterministic — same input always returns same result', () => {
    const id = 'a1b2c3d4-e5f6-7890-abcd-ef1234567890';
    const code = 42;
    const r1 = computeRolloutEligibility(id, code, 50);
    const r2 = computeRolloutEligibility(id, code, 50);
    expect(r1).toBe(r2);
  });

  it('distributes roughly evenly across many installations', () => {
    let eligible = 0;
    const total = 10000;
    for (let i = 0; i < total; i++) {
      const fakeId = `${i.toString().padStart(8, '0')}-0000-0000-0000-000000000000`;
      if (computeRolloutEligibility(fakeId, 24, 50)) eligible++;
    }
    const pct = eligible / total;
    // Should be within ±5% of the target 50%
    expect(pct).toBeGreaterThan(0.45);
    expect(pct).toBeLessThan(0.55);
  });

  it('different versionCodes give different eligibility for same installationId', () => {
    const id = 'a1b2c3d4-e5f6-7890-abcd-ef1234567890';
    // With 50% rollout, at least some version codes should differ
    const results = [24, 25, 26, 27, 28].map(code => computeRolloutEligibility(id, code, 50));
    // Not all should be the same
    const allSame = results.every(r => r === results[0]);
    expect(allSame).toBe(false);
  });
});
