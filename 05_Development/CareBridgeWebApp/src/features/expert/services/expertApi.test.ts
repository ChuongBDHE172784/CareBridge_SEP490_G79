import { beforeEach, describe, expect, it, vi } from 'vitest';
import apiClient from '../../../shared/api/apiClient';
import {
  createMyProfile,
  getExpertOnboarding,
  getHospitals,
  getWards,
  submitIdentityEvidence,
} from './expertApi';

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

  it('loads wards and care facilities through separate canonical master-data contracts', async () => {
    const wards = [{
      wardId: 'ward-42',
      districtId: 'district-7',
      provinceId: 'province-3',
      name: 'Ward 42',
      nameEn: 'Ward 42',
    }];
    const hospitals = [{
      hospitalId: 'facility-99',
      name: 'Care Facility 99',
      provinceId: 'province-3',
      districtId: 'district-7',
      address: '99 Canonical Street',
      level: 'DISTRICT',
      type: 'HOSPITAL',
      phone: '0900000099',
    }];
    vi.mocked(apiClient.get)
      .mockResolvedValueOnce({ data: { data: wards } })
      .mockResolvedValueOnce({ data: { data: hospitals } });

    await expect(getWards({ districtId: 'district-7' })).resolves.toEqual(wards);
    await expect(getHospitals({
      provinceId: 'province-3',
      districtId: 'district-7',
    })).resolves.toEqual(hospitals);

    expect(apiClient.get).toHaveBeenNthCalledWith(
      1,
      '/api/v1/master-data/wards',
      { params: { districtId: 'district-7' } },
    );
    expect(apiClient.get).toHaveBeenNthCalledWith(
      2,
      '/api/v1/master-data/hospitals',
      { params: { provinceId: 'province-3', districtId: 'district-7' } },
    );
  });

  it('posts the selected care-facility id without repurposing a location id', async () => {
    const request = {
      specialtyId: 'specialty-1',
      professionalTitle: 'Doctor',
      experienceYears: 8,
      hospitalId: 'facility-99',
      consultationScope: 'Maternal health',
      consultationFeeVnd: 350000,
    };
    vi.mocked(apiClient.post).mockResolvedValue({ data: { data: request } });

    await createMyProfile(request);

    expect(apiClient.post).toHaveBeenCalledWith('/api/v1/expert/profiles', request);
    expect(request.hospitalId).toBe('facility-99');
  });
});
