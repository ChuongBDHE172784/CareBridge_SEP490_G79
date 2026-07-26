// @vitest-environment jsdom

import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

const harness = vi.hoisted(() => ({
  createMyProfile: vi.fn(),
  getExpertOnboarding: vi.fn(),
  submitCredential: vi.fn(),
  submitIdentityEvidence: vi.fn(),
  verifyFace: vi.fn(),
  getProvinces: vi.fn(),
  getDistricts: vi.fn(),
  getWards: vi.fn(),
  getSpecialties: vi.fn(),
  getHospitals: vi.fn(),
  navigate: vi.fn(),
}));

vi.mock('../services/expertApi', () => ({
  createMyProfile: harness.createMyProfile,
  getExpertOnboarding: harness.getExpertOnboarding,
  submitCredential: harness.submitCredential,
  submitIdentityEvidence: harness.submitIdentityEvidence,
  verifyFace: harness.verifyFace,
  getProvinces: harness.getProvinces,
  getDistricts: harness.getDistricts,
  getWards: harness.getWards,
  getSpecialties: harness.getSpecialties,
  getHospitals: harness.getHospitals,
}));

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>(
    'react-router-dom',
  );
  return { ...actual, useNavigate: () => harness.navigate };
});

import ExpertOnboardingPage from './ExpertOnboardingPage';

describe('expert onboarding canonical workplace selection', () => {
  beforeEach(() => {
    Object.values(harness).forEach(mock => mock.mockReset());
    harness.getExpertOnboarding.mockResolvedValue({
      profileExists: false,
      identityStatus: null,
      credentialStatus: null,
      verificationStatus: null,
      nextStep: 'PROFILE',
      latestIdentityAttempt: null,
    });
    harness.getProvinces.mockResolvedValue([{
      provinceId: 'province-3',
      name: 'Province 3',
      nameEn: 'Province 3',
      region: 'SOUTH',
    }]);
    harness.getDistricts.mockResolvedValue([{
      districtId: 'district-7',
      provinceId: 'province-3',
      name: 'District 7',
      nameEn: 'District 7',
    }]);
    harness.getWards.mockResolvedValue([{
      wardId: 'ward-42',
      districtId: 'district-7',
      provinceId: 'province-3',
      name: 'Ward 42',
      nameEn: 'Ward 42',
    }]);
    harness.getSpecialties.mockResolvedValue([{
      specialtyId: 'specialty-1',
      name: 'Maternal medicine',
      description: 'Maternal health',
      category: 'MEDICAL',
    }]);
    harness.getHospitals.mockResolvedValue([{
      hospitalId: 'facility-99',
      name: 'Care Facility 99',
      provinceId: 'province-3',
      districtId: 'district-7',
      address: '99 Canonical Street',
      level: 'DISTRICT',
      type: 'HOSPITAL',
      phone: '0900000099',
    }]);
    harness.createMyProfile.mockResolvedValue({});
  });

  afterEach(() => {
    cleanup();
  });

  it('uses ward as location context but submits a real care-facility hospitalId', async () => {
    render(<ExpertOnboardingPage />);

    expect(await screen.findByRole('heading', { name: 'Thông tin chuyên môn' })).toBeTruthy();
    expect(await screen.findByRole('option', { name: 'Maternal medicine' })).toBeTruthy();
    expect(await screen.findByRole('option', { name: 'Province 3' })).toBeTruthy();
    fireEvent.change(screen.getByLabelText('Chuyên khoa *'), {
      target: { value: 'specialty-1' },
    });
    fireEvent.change(screen.getByLabelText('Chức danh *'), {
      target: { value: 'Doctor' },
    });
    fireEvent.change(screen.getByLabelText('Tỉnh/Thành phố *'), {
      target: { value: 'province-3' },
    });

    expect(await screen.findByRole('option', { name: 'District 7' })).toBeTruthy();
    fireEvent.change(screen.getByLabelText('Quận/Huyện'), {
      target: { value: 'district-7' },
    });

    expect(await screen.findByRole('option', { name: 'Ward 42' })).toBeTruthy();
    expect(await screen.findByRole('option', { name: 'Care Facility 99' })).toBeTruthy();
    fireEvent.change(screen.getByLabelText('Phường/Xã'), {
      target: { value: 'ward-42' },
    });
    fireEvent.change(screen.getByLabelText('Bệnh viện/Cơ sở y tế *'), {
      target: { value: 'facility-99' },
    });
    fireEvent.change(screen.getByLabelText('Phạm vi tư vấn *'), {
      target: { value: 'Maternal health' },
    });
    expect((screen.getByLabelText('Chuyên khoa *') as HTMLSelectElement).value)
      .toBe('specialty-1');
    expect((screen.getByLabelText('Tỉnh/Thành phố *') as HTMLSelectElement).value)
      .toBe('province-3');
    expect((screen.getByLabelText('Quận/Huyện') as HTMLSelectElement).value)
      .toBe('district-7');
    expect((screen.getByLabelText('Phường/Xã') as HTMLSelectElement).value)
      .toBe('ward-42');
    expect((screen.getByLabelText('Bệnh viện/Cơ sở y tế *') as HTMLSelectElement).value)
      .toBe('facility-99');

    const submitButton = screen.getByRole('button', { name: 'Lưu và tiếp tục' });
    const form = submitButton.closest('form');
    expect(form).not.toBeNull();
    expect(form?.checkValidity()).toBe(true);
    fireEvent.click(submitButton);

    await waitFor(() => expect(harness.createMyProfile).toHaveBeenCalledTimes(1));
    expect(harness.getWards).toHaveBeenCalledWith('district-7');
    expect(harness.getHospitals).toHaveBeenLastCalledWith({
      provinceId: 'province-3',
      districtId: 'district-7',
    });
    const submitted = harness.createMyProfile.mock.calls[0][0];
    expect(submitted).toEqual({
      specialtyId: 'specialty-1',
      professionalTitle: 'Doctor',
      hospitalId: 'facility-99',
      consultationScope: 'Maternal health',
      experienceYears: undefined,
    });
    expect(submitted).not.toHaveProperty('wardId');
    expect(submitted).not.toHaveProperty('hospitalName');
  });

  it('keeps required hospitals usable when the optional ward request fails', async () => {
    harness.getWards.mockRejectedValue(new Error('ward service unavailable'));
    render(<ExpertOnboardingPage />);

    expect(await screen.findByRole('option', { name: 'Maternal medicine' })).toBeTruthy();
    expect(await screen.findByRole('option', { name: 'Province 3' })).toBeTruthy();
    fireEvent.change(screen.getByLabelText('Chuyên khoa *'), {
      target: { value: 'specialty-1' },
    });
    fireEvent.change(screen.getByLabelText('Chức danh *'), {
      target: { value: 'Doctor' },
    });
    fireEvent.change(screen.getByLabelText('Tỉnh/Thành phố *'), {
      target: { value: 'province-3' },
    });
    expect(await screen.findByRole('option', { name: 'District 7' })).toBeTruthy();
    fireEvent.change(screen.getByLabelText('Quận/Huyện'), {
      target: { value: 'district-7' },
    });

    await waitFor(() => expect(harness.getHospitals).toHaveBeenLastCalledWith({
      provinceId: 'province-3',
      districtId: 'district-7',
    }));
    expect(await screen.findByRole('option', { name: 'Care Facility 99' })).toBeTruthy();
    fireEvent.change(screen.getByLabelText('Bệnh viện/Cơ sở y tế *'), {
      target: { value: 'facility-99' },
    });
    fireEvent.change(screen.getByLabelText('Phạm vi tư vấn *'), {
      target: { value: 'Maternal health' },
    });
    fireEvent.click(screen.getByRole('button', { name: 'Lưu và tiếp tục' }));

    await waitFor(() => expect(harness.createMyProfile).toHaveBeenCalledTimes(1));
    expect(harness.createMyProfile.mock.calls[0][0].hospitalId).toBe('facility-99');
  });
});
