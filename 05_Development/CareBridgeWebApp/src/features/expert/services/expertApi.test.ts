import { beforeEach, describe, expect, it, vi } from 'vitest';
import apiClient from '../../../shared/api/apiClient';
import { getExpertOnboarding, submitIdentityEvidence } from './expertApi';

vi.mock('../../../shared/api/apiClient', () => ({
  default: { get: vi.fn(), post: vi.fn(), patch: vi.fn(), put: vi.fn(), delete: vi.fn() },
}));

describe('expert onboarding API', () => {
  beforeEach(() => vi.clearAllMocks());

  it('loads the server-owned resume state', async () => {
    const aggregate = { profileExists: true, identityStatus: 'PENDING_REVIEW', credentialStatus: null, verificationStatus: 'PENDING', nextStep: 'CREDENTIAL', latestIdentityAttempt: null };
    vi.mocked(apiClient.get).mockResolvedValue({ data: { data: aggregate } });
    await expect(getExpertOnboarding()).resolves.toEqual(aggregate);
    expect(apiClient.get).toHaveBeenCalledWith('/api/v1/expert/onboarding');
  });

  it('submits the three identity images atomically with stable field names', async () => {
    vi.mocked(apiClient.post).mockResolvedValue({ data: { data: { status: 'MANUAL_REVIEW_REQUIRED' } } });
    const selfie = new File(['selfie'], 'selfie.jpg', { type: 'image/jpeg' });
    const identityFront = new File(['front'], 'front.png', { type: 'image/png' });
    const identityBack = new File(['back'], 'back.jpg', { type: 'image/jpeg' });
    await submitIdentityEvidence({ selfie, identityFront, identityBack });
    const form = vi.mocked(apiClient.post).mock.calls[0][1] as FormData;
    expect(form.get('selfie')).toBe(selfie);
    expect(form.get('identityFront')).toBe(identityFront);
    expect(form.get('identityBack')).toBe(identityBack);
  });
});
