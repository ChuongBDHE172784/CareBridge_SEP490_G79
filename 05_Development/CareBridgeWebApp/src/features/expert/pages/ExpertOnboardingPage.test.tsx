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
  searchTrackAsiaHospitals: vi.fn(),
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
  searchTrackAsiaHospitals: harness.searchTrackAsiaHospitals,
}));

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>(
    'react-router-dom',
  );
  return { ...actual, useNavigate: () => harness.navigate };
});

import ExpertOnboardingPage from './ExpertOnboardingPage';

const jpeg = (name: string) => new File([new Uint8Array([1, 2, 3])], name, { type: 'image/jpeg' });

function attachIdentityImages(container: HTMLElement) {
  const inputs = Array.from(container.querySelectorAll('input[type="file"]'));
  expect(inputs).toHaveLength(3);
  const names = ['selfie.jpg', 'cccd-front.jpg', 'cccd-back.jpg'];
  inputs.forEach((input, index) => {
    fireEvent.change(input, { target: { files: [jpeg(names[index])] } });
  });
}

async function fillProfessionalSection() {
  expect(await screen.findByRole('option', { name: 'Maternal medicine' })).toBeTruthy();
  expect(await screen.findByRole('option', { name: 'Province 3' })).toBeTruthy();
  fireEvent.change(screen.getByLabelText('Chuyên khoa *'), { target: { value: 'specialty-1' } });
  fireEvent.change(screen.getByLabelText('Chức danh *'), { target: { value: 'Doctor' } });
  fireEvent.change(screen.getByLabelText('Tỉnh/Thành phố *'), { target: { value: 'province-3' } });

  expect(await screen.findByRole('option', { name: 'District 7' })).toBeTruthy();
  fireEvent.change(screen.getByLabelText('Quận/Huyện'), { target: { value: 'district-7' } });

  expect(await screen.findByRole('option', { name: 'Care Facility 99' })).toBeTruthy();
  fireEvent.change(screen.getByLabelText('Cơ sở y tế *'), { target: { value: 'facility-99' } });
  fireEvent.change(
    screen.getByPlaceholderText('Mô tả các bệnh lý và chuyên môn tư vấn chính...'),
    { target: { value: 'Maternal health' } },
  );
}

describe('expert onboarding identity step', () => {
  beforeEach(() => {
    Object.values(harness).forEach(mock => mock.mockReset());
    globalThis.URL.createObjectURL = vi.fn(() => 'blob:preview');
    globalThis.URL.revokeObjectURL = vi.fn();

    harness.getExpertOnboarding.mockResolvedValue({
      profileExists: false,
      identityStatus: null,
      credentialStatus: null,
      verificationStatus: null,
      nextStep: 'IDENTITY',
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
    harness.searchTrackAsiaHospitals.mockResolvedValue([]);
    harness.createMyProfile.mockResolvedValue({});
    harness.submitIdentityEvidence.mockResolvedValue({});
    harness.verifyFace.mockResolvedValue({ similar: true, similarity: 0.93, reason: 'match' });
  });

  afterEach(() => {
    cleanup();
  });

  it('renders the professional section and scopes master data by the selected province', async () => {
    render(<ExpertOnboardingPage />);

    expect(await screen.findByRole('heading', { name: '1. Thông tin chuyên môn' })).toBeTruthy();
    await fillProfessionalSection();

    expect(harness.getDistricts).toHaveBeenCalledWith('province-3');
    expect(harness.getWards).toHaveBeenLastCalledWith({ provinceId: 'province-3' });
    expect(harness.getHospitals).toHaveBeenLastCalledWith({ provinceId: 'province-3' });
    expect((screen.getByLabelText('Cơ sở y tế *') as HTMLSelectElement).value).toBe('facility-99');
  });

  it('submits the canonical care-facility hospitalId without any ward identifier', async () => {
    const { container } = render(<ExpertOnboardingPage />);

    await fillProfessionalSection();
    attachIdentityImages(container);

    fireEvent.click(screen.getByRole('button', { name: 'Hoàn tất & Gửi hồ sơ' }));

    await waitFor(() => expect(harness.createMyProfile).toHaveBeenCalledTimes(1));
    const submitted = harness.createMyProfile.mock.calls[0][0];
    expect(submitted.specialtyId).toBe('specialty-1');
    expect(submitted.professionalTitle).toBe('Doctor');
    expect(submitted.hospitalId).toBe('facility-99');
    expect(submitted.consultationScope).toBe('Maternal health');
    expect(submitted.experienceYears).toBeUndefined();
    expect(submitted).not.toHaveProperty('wardId');
    expect(submitted).not.toHaveProperty('hospitalName');

    await waitFor(() => expect(harness.submitIdentityEvidence).toHaveBeenCalledTimes(1));
  });

  it('keeps the required care facilities usable when the optional ward request fails', async () => {
    harness.getWards.mockRejectedValue(new Error('ward service unavailable'));
    const { container } = render(<ExpertOnboardingPage />);

    await fillProfessionalSection();
    await waitFor(() =>
      expect(harness.getHospitals).toHaveBeenLastCalledWith({ provinceId: 'province-3' }),
    );
    attachIdentityImages(container);

    fireEvent.click(screen.getByRole('button', { name: 'Hoàn tất & Gửi hồ sơ' }));

    await waitFor(() => expect(harness.createMyProfile).toHaveBeenCalledTimes(1));
    expect(harness.createMyProfile.mock.calls[0][0].hospitalId).toBe('facility-99');
  });

  it('changing the province clears the previously picked district and facility', async () => {
    render(<ExpertOnboardingPage />);
    await fillProfessionalSection();

    fireEvent.change(screen.getByLabelText('Tỉnh/Thành phố *'), { target: { value: '' } });

    expect((screen.getByLabelText('Quận/Huyện') as HTMLSelectElement).value).toBe('');
    expect((screen.getByLabelText('Cơ sở y tế *') as HTMLSelectElement).value).toBe('');
  });

  it('blocks submission and reports missing professional fields', async () => {
    const { container } = render(<ExpertOnboardingPage />);
    expect(await screen.findByRole('heading', { name: '1. Thông tin chuyên môn' })).toBeTruthy();
    attachIdentityImages(container);

    fireEvent.click(screen.getByRole('button', { name: 'Hoàn tất & Gửi hồ sơ' }));

    expect(
      await screen.findByText('Vui lòng điền đầy đủ thông tin chuyên môn bắt buộc.'),
    ).toBeTruthy();
    expect(harness.createMyProfile).not.toHaveBeenCalled();
    expect(harness.submitIdentityEvidence).not.toHaveBeenCalled();
  });

  it('blocks submission when the three identity photos are incomplete', async () => {
    render(<ExpertOnboardingPage />);
    await fillProfessionalSection();

    fireEvent.click(screen.getByRole('button', { name: 'Hoàn tất & Gửi hồ sơ' }));

    expect(await screen.findByText('Vui lòng chụp đầy đủ 3 ảnh.')).toBeTruthy();
    expect(harness.createMyProfile).not.toHaveBeenCalled();
  });

  it('rejects an identity photo whose MIME type is not JPEG or PNG', async () => {
    const { container } = render(<ExpertOnboardingPage />);
    expect(await screen.findByRole('heading', { name: '1. Thông tin chuyên môn' })).toBeTruthy();

    const firstInput = container.querySelector('input[type="file"]')!;
    fireEvent.change(firstInput, {
      target: { files: [new File(['x'], 'scan.gif', { type: 'image/gif' })] },
    });

    expect(await screen.findByText('Chỉ chấp nhận ảnh JPEG hoặc PNG.')).toBeTruthy();
  });

  it('surfaces a banner when the care-facility catalogue cannot be loaded', async () => {
    harness.getHospitals.mockRejectedValue(new Error('facility service unavailable'));
    render(<ExpertOnboardingPage />);

    expect(await screen.findByRole('option', { name: 'Province 3' })).toBeTruthy();
    fireEvent.change(screen.getByLabelText('Tỉnh/Thành phố *'), { target: { value: 'province-3' } });

    expect(await screen.findByText('Không thể tải danh sách cơ sở y tế.')).toBeTruthy();
  });
});
