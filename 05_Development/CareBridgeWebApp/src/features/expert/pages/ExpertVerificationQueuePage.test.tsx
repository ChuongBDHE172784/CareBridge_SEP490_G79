// @vitest-environment jsdom

import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

const api = vi.hoisted(() => ({
  getExpertReviewCases: vi.fn(),
  getCredentialDocumentPreview: vi.fn(),
  getCredentialFileUrl: vi.fn(),
  getIdentityFileUrl: vi.fn(),
  approveExpert: vi.fn(),
  rejectExpert: vi.fn(),
  reviewCredential: vi.fn(),
  reviewIdentity: vi.fn(),
  setExpertTrust: vi.fn(),
}));

vi.mock('../services/expertApi', () => api);

import ExpertVerificationQueuePage from './ExpertVerificationQueuePage';

describe('centralized expert review', () => {
  beforeEach(() => {
    Object.values(api).forEach((mock) => mock.mockReset());
    api.getExpertReviewCases.mockResolvedValue([{
      profile: {
        expertProfileId: 'profile-1',
        userId: 'user-1',
        displayName: 'BS Nguyễn An',
        specialty: 'Sản khoa',
        professionalTitle: 'Bác sĩ CKII',
        experienceYears: 12,
        hospitalId: 'facility-1',
        consultationScope: 'Thai kỳ',
        verificationStatus: 'PENDING',
        trustStatus: 'ACTIVE',
        verifiedAt: null,
        ratingAvg: null,
        consultationFeeVnd: null,
        createdAt: '2026-07-28T00:00:00Z',
      },
      latestIdentity: null,
      credentials: [{
        credentialId: 'credential-1',
        expertProfileId: 'profile-1',
        credentialType: 'MEDICAL_LICENSE',
        credentialNumber: 'CCHN-123',
        issuer: 'Bộ Y tế',
        issuedDate: '2024-01-01',
        expiryDate: null,
        fileUrl: 'https://r2.example/signed-document',
        fileId: 'file-1',
        fileName: 'giay-phep.docx',
        mimeType: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
        fileSizeBytes: 2048,
        createdAt: '2026-07-28T00:00:00Z',
        reviewStatus: 'PENDING',
        reviewNote: null,
        reviewedBy: '',
        reviewedAt: null,
      }],
      identityStatus: 'MISSING',
      credentialStatus: 'PENDING',
      readyForFinalApproval: false,
    }]);
    api.getCredentialDocumentPreview.mockResolvedValue({
      credentialId: 'credential-1',
      fileName: 'giay-phep.docx',
      mimeType: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
      fileSizeBytes: 2048,
      content: 'Nội dung giấy phép hành nghề',
      truncated: false,
    });
    api.getCredentialFileUrl.mockResolvedValue('https://r2.example/fresh-signed-document');
  });

  afterEach(cleanup);

  it('keeps identity, credential, final approval and trust in one screen', async () => {
    render(<ExpertVerificationQueuePage />);

    expect((await screen.findAllByText('BS Nguyễn An')).length).toBeGreaterThan(0);
    expect(screen.getByText('1. Định danh chuyên gia')).toBeTruthy();
    expect(screen.getByText('2. Chứng chỉ chuyên môn')).toBeTruthy();
    expect(screen.getByText('3. Quyết định hồ sơ chuyên gia')).toBeTruthy();
    expect(screen.getByDisplayValue('Trust: Hoạt động')).toBeTruthy();
  });

  it('reads a private R2 DOCX through the protected preview API', async () => {
    render(<ExpertVerificationQueuePage />);

    fireEvent.click(await screen.findByRole('button', { name: 'Đọc tài liệu' }));

    expect(await screen.findByText('Nội dung giấy phép hành nghề')).toBeTruthy();
    expect(api.getCredentialDocumentPreview).toHaveBeenCalledWith('credential-1');
    expect(api.getCredentialFileUrl).toHaveBeenCalledWith('credential-1');
    expect(screen.getByRole('link', { name: 'Tải bản gốc' })).toBeTruthy();
  });
});
