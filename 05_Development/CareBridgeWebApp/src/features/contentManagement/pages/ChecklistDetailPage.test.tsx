// @vitest-environment jsdom

import { cleanup, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import type { AdminChecklistTemplateDetail } from '../models/content';

const harness = vi.hoisted(() => ({
  fetchChecklistTemplateDetail: vi.fn(),
  updateChecklistTemplate: vi.fn(),
  archiveChecklistTemplate: vi.fn(),
  cloneChecklistVersion: vi.fn(),
  reviewMigratedChecklistVersion: vi.fn(),
  activateChecklistVersion: vi.fn(),
  navigate: vi.fn(),
  hasRole: vi.fn(),
}));

vi.mock('../services/contentApi', () => ({
  fetchChecklistTemplateDetail: harness.fetchChecklistTemplateDetail,
  updateChecklistTemplate: harness.updateChecklistTemplate,
  archiveChecklistTemplate: harness.archiveChecklistTemplate,
  cloneChecklistVersion: harness.cloneChecklistVersion,
  reviewMigratedChecklistVersion: harness.reviewMigratedChecklistVersion,
  activateChecklistVersion: harness.activateChecklistVersion,
}));

vi.mock('../../../shared/auth/useAuth', () => ({
  useAuth: () => ({ hasRole: harness.hasRole }),
}));

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');
  return {
    ...actual,
    useParams: () => ({ id: 'checklist-123' }),
    useNavigate: () => harness.navigate,
  };
});

import ChecklistDetailPage from './ChecklistDetailPage';

function checklistDetail(): AdminChecklistTemplateDetail {
  return {
    id: 'checklist-123',
    templateType: 'MANDATORY',
    name: 'Checklist phiên bản ba',
    stage: 'PREGNANCY',
    status: 'DRAFT',
    description: 'Mô tả',
    versionNo: 3,
    lineageId: 'lineage-1',
    versionId: 'version-3',
    recipientRoles: ['MOTHER'],
    substage: null,
    migrationReviewRequired: false,
    distributionEnabled: false,
    approvedAt: null,
    approvedBy: null,
    items: [],
  };
}

describe('ChecklistDetailPage version', () => {
  beforeEach(() => {
    harness.fetchChecklistTemplateDetail.mockReset();
    harness.updateChecklistTemplate.mockReset();
    harness.archiveChecklistTemplate.mockReset();
    harness.cloneChecklistVersion.mockReset();
    harness.reviewMigratedChecklistVersion.mockReset();
    harness.activateChecklistVersion.mockReset();
    harness.navigate.mockReset();
    harness.hasRole.mockReset();
    harness.hasRole.mockReturnValue(true);
  });

  afterEach(cleanup);

  it('shows the persisted checklist version in detail metadata', async () => {
    harness.fetchChecklistTemplateDetail.mockResolvedValue(checklistDetail());

    render(<ChecklistDetailPage />);

    expect(await screen.findByText('Checklist phiên bản ba')).toBeTruthy();
    expect(screen.getByText('PHIÊN BẢN')).toBeTruthy();
    expect(screen.getByText('v3')).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Checklist' })).toBeTruthy();
    expect(screen.getByText(/Không áp dụng chuỗi PRE_PREGNANCY/)).toBeTruthy();
    expect(screen.getByTestId('checklist-detail-layout').className).toContain('grid-cols-1');
    expect(screen.getByTestId('checklist-detail-layout').className).toContain('lg:grid-cols-[minmax(0,1fr)_340px]');
    expect(screen.getByTestId('checklist-detail-page').className).toContain('bg-background');
    expect(screen.getByTestId('checklist-detail-page').className).toContain('font-sans');
    expect(screen.getByRole('heading', { name: 'Checklist phiên bản ba' }).className).toContain('text-on-surface');
    const editButton = screen.getByRole('button', { name: 'Edit checklist' });
    expect(editButton.className).toContain('min-h-12');
    expect(editButton.className).toContain('rounded-full');
    expect(editButton.querySelector('[aria-hidden="true"]')).toBeTruthy();
  });

  it('shows a positive sequence position and guidance for preconception review', async () => {
    harness.fetchChecklistTemplateDetail.mockResolvedValue({
      ...checklistDetail(),
      stage: 'PRE_PREGNANCY',
      displayOrder: 2,
    });

    render(<ChecklistDetailPage />);

    expect(await screen.findByText(/B.*chu.*i 2/)).toBeTruthy();
    expect(screen.getByRole('note')).toBeTruthy();
  });

  it('wraps a maximum-length unbroken item without widening the viewport', async () => {
    const unbrokenTitle = 'x'.repeat(500);
    harness.fetchChecklistTemplateDetail.mockResolvedValue({
      ...checklistDetail(),
      items: [{ id: 'item-1', itemText: unbrokenTitle, order: 1, isRequired: true, targetSubject: 'MOTHER' }],
    });

    render(<ChecklistDetailPage />);

    const wrapper = (await screen.findByText(unbrokenTitle)).parentElement;
    expect(wrapper?.className).toContain('min-w-0');
    expect(wrapper?.className).toContain('break-words');
  });

  it('visibly labels recipient roles, substage, and every item target', async () => {
    harness.fetchChecklistTemplateDetail.mockResolvedValue({
      ...checklistDetail(),
      recipientRoles: ['MOTHER', 'FAMILY'],
      substage: {
        code: 'PREGNANCY_LMP_WEEK_0_12',
        anchor: 'LMP',
        startInclusive: 0,
        endInclusive: 12,
        unit: 'WEEK',
      },
      items: [
        {
          id: 'mother-item',
          itemText: 'Uống vitamin',
          order: 1,
          isRequired: true,
          targetSubject: 'MOTHER',
          description: 'Uống sau bữa sáng theo hướng dẫn.',
          supportFunction: 'REMINDERS',
        },
        { id: 'baby-item', itemText: 'Chuẩn bị đồ cho bé', order: 2, isRequired: false, targetSubject: 'BABY' },
      ],
    });

    render(<ChecklistDetailPage />);

    expect(await screen.findByLabelText('Người nhận: Mẹ')).toBeTruthy();
    expect(screen.getByLabelText('Người nhận: Gia đình')).toBeTruthy();
    expect(screen.getByText('PREGNANCY_LMP_WEEK_0_12')).toBeTruthy();
    expect(screen.getByLabelText('Đối tượng mục 1: Mẹ')).toBeTruthy();
    expect(screen.getByLabelText('Đối tượng mục 2: Em bé')).toBeTruthy();
    expect(screen.getByText('NỘI DUNG CHI TIẾT')).toBeTruthy();
    expect(screen.getByText('Uống sau bữa sáng theo hướng dẫn.')).toBeTruthy();
    expect(screen.getByLabelText('Chức năng hỗ trợ mục 1: Nhắc nhở')).toBeTruthy();
    expect(screen.getByLabelText('Chức năng hỗ trợ mục 2: Không liên kết')).toBeTruthy();
  });

  it('shows requiredness for V2 targetless items', async () => {
    harness.fetchChecklistTemplateDetail.mockResolvedValue({
      ...checklistDetail(),
      checklistContractVersion: 2,
      items: [
        { id: 'required-v2', itemText: 'Theo dõi chỉ số', order: 1, isRequired: true, targetSubject: null },
        { id: 'optional-v2', itemText: 'Đọc thêm', order: 2, isRequired: false, targetSubject: null },
      ],
    });

    render(<ChecklistDetailPage />);

    expect(await screen.findByText('Thứ tự: 1 · Bắt buộc')).toBeTruthy();
    expect(screen.getByText('Thứ tự: 2 · Không bắt buộc')).toBeTruthy();
  });

  it('offers clone instead of direct edit for an approved immutable version', async () => {
    const user = userEvent.setup();
    const approved = { ...checklistDetail(), status: 'APPROVED' as const, distributionEnabled: true };
    harness.fetchChecklistTemplateDetail.mockResolvedValue(approved);
    harness.cloneChecklistVersion.mockResolvedValue({ ...checklistDetail(), id: 'draft-clone', versionNo: 4 });

    render(<ChecklistDetailPage />);

    await screen.findByText('Checklist phiên bản ba');
    expect(screen.queryByRole('button', { name: 'Edit checklist' })).toBeNull();
    await user.click(screen.getByRole('button', { name: 'Clone approved version' }));
    await waitFor(() => expect(harness.cloneChecklistVersion).toHaveBeenCalledWith('lineage-1', 'version-3'));
    expect(harness.navigate).toHaveBeenCalledWith('/content/checklists/draft-clone/edit');
  });

  it('shows explicit review action for an imported version', async () => {
    const user = userEvent.setup();
    harness.fetchChecklistTemplateDetail.mockResolvedValue({
      ...checklistDetail(), migrationReviewRequired: true, distributionEnabled: false,
    });
    harness.reviewMigratedChecklistVersion.mockResolvedValue({ previousStatus: 'PENDING_REVIEW', newStatus: 'PENDING_REVIEW' });

    render(<ChecklistDetailPage />);
    await screen.findByText('Checklist phiên bản ba');
    await user.click(screen.getByRole('button', { name: 'Review migrated version' }));

    await waitFor(() => expect(harness.reviewMigratedChecklistVersion).toHaveBeenCalledWith('lineage-1', 'version-3'));
  });

  it('activates only a reviewed distribution-disabled version', async () => {
    const user = userEvent.setup();
    harness.fetchChecklistTemplateDetail.mockResolvedValue({
      ...checklistDetail(), status: 'PENDING_REVIEW', migrationReviewRequired: false,
      migrationReviewedAt: '2026-07-29T00:00:00Z', distributionEnabled: false,
      provenance: { provenanceStatus: 'SIGNED_OFF' },
    });
    harness.activateChecklistVersion.mockResolvedValue({ previousStatus: 'PENDING_REVIEW', newStatus: 'APPROVED' });

    render(<ChecklistDetailPage />);
    await screen.findByText('Checklist phiên bản ba');
    await user.click(screen.getByRole('button', { name: 'Activate reviewed version' }));

    await waitFor(() => expect(harness.activateChecklistVersion).toHaveBeenCalledWith('lineage-1', 'version-3'));
  });

  it('offers activation after technical review without a clinical/content sign-off gate', async () => {
    const user = userEvent.setup();
    harness.fetchChecklistTemplateDetail.mockResolvedValue({
      ...checklistDetail(), status: 'PENDING_REVIEW', migrationReviewRequired: false,
      migrationReviewedAt: '2026-07-29T00:00:00Z', distributionEnabled: false,
      provenance: { provenanceStatus: 'PENDING_CLINICAL_COPY_SIGN_OFF' },
    });
    harness.activateChecklistVersion.mockResolvedValue({ previousStatus: 'PENDING_REVIEW', newStatus: 'APPROVED' });

    render(<ChecklistDetailPage />);
    await screen.findByRole('heading', { name: /Checklist phiên bản ba/ });

    await user.click(screen.getByRole('button', { name: 'Activate reviewed version' }));
    await waitFor(() => expect(harness.activateChecklistVersion).toHaveBeenCalledWith('lineage-1', 'version-3'));
  });
});
