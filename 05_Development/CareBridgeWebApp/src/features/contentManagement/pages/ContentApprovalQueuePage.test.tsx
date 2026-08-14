// @vitest-environment jsdom

import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

const harness = vi.hoisted(() => ({
  fetchAdminChecklists: vi.fn(),
  fetchStaffContentList: vi.fn(),
  decideChecklistTemplate: vi.fn(),
  decideContent: vi.fn(),
  navigate: vi.fn(),
}));

vi.mock('../services/contentApi', () => ({
  fetchAdminChecklists: harness.fetchAdminChecklists,
  fetchStaffContentList: harness.fetchStaffContentList,
  decideChecklistTemplate: harness.decideChecklistTemplate,
  decideContent: harness.decideContent,
}));

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');
  return { ...actual, useNavigate: () => harness.navigate };
});

import ContentApprovalQueuePage from './ContentApprovalQueuePage';

describe('ContentApprovalQueuePage sequence context', () => {
  beforeEach(() => {
    harness.fetchAdminChecklists.mockReset();
    harness.fetchStaffContentList.mockReset();
    harness.decideChecklistTemplate.mockReset();
    harness.decideContent.mockReset();
    harness.navigate.mockReset();
    harness.fetchStaffContentList.mockResolvedValue({ content: [], number: 0, size: 50, totalElements: 0, totalPages: 0 });
  });

  afterEach(cleanup);

  it('shows legacy and positive sequence labels before approval', async () => {
    harness.fetchAdminChecklists.mockResolvedValue({
      content: [
        {
          id: 'legacy', name: 'Legacy preconception', stage: 'PRE_PREGNANCY', status: 'PENDING_REVIEW',
          description: '', templateType: 'MANDATORY', versionNo: 1, updatedAt: null, itemCount: 1,
          displayOrder: 0, recipientRoles: ['MOTHER'],
        },
        {
          id: 'sequence-2', name: 'Sequence two', stage: 'PRE_PREGNANCY', status: 'PENDING_REVIEW',
          description: '', templateType: 'MANDATORY', versionNo: 1, updatedAt: null, itemCount: 1,
          displayOrder: 2, recipientRoles: ['MOTHER'],
        },
        {
          id: 'pregnancy', name: 'Pregnancy checklist', stage: 'PREGNANCY', status: 'PENDING_REVIEW',
          description: '', templateType: 'MANDATORY', versionNo: 1, updatedAt: null, itemCount: 1,
          displayOrder: 0, recipientRoles: ['MOTHER'],
        },
      ],
      number: 0, size: 50, totalElements: 3, totalPages: 1,
    });

    render(<ContentApprovalQueuePage />);

    expect(await screen.findByText('Legacy preconception')).toBeTruthy();
    expect(screen.getByText(/Legacy.*ngoài/)).toBeTruthy();
    expect(screen.getByText(/B.*chu.*i 2/)).toBeTruthy();
    expect(screen.getByText(/Không áp dụng chuỗi PRE_PREGNANCY/)).toBeTruthy();
  });

  it('opens batch publish dropdown with 4 options and handles batch approval on confirmation', async () => {
    harness.fetchStaffContentList.mockResolvedValue({
      content: [
        {
          id: 'article-1', title: 'Bài viết 1', type: 'ARTICLE', stage: 'PREGNANCY',
          version: 1, createdAt: '2026-01-01T00:00:00Z', updatedAt: null,
        },
        {
          id: 'faq-1', title: 'FAQ 1', type: 'FAQ', stage: 'POSTPARTUM',
          version: 1, createdAt: '2026-01-01T00:00:00Z', updatedAt: null,
        },
      ],
      number: 0, size: 50, totalElements: 2, totalPages: 1,
    });

    harness.fetchAdminChecklists.mockResolvedValue({
      content: [
        {
          id: 'checklist-1', name: 'Checklist 1', stage: 'PREGNANCY', status: 'PENDING_REVIEW',
          description: '', templateType: 'MANDATORY', versionNo: 1, updatedAt: null, itemCount: 2,
          displayOrder: 1, recipientRoles: ['MOTHER'],
        },
      ],
      number: 0, size: 50, totalElements: 1, totalPages: 1,
    });

    harness.decideContent.mockResolvedValue({ id: 'art-1', previousStatus: 'PENDING_REVIEW', newStatus: 'APPROVED' });
    harness.decideChecklistTemplate.mockResolvedValue({ previousStatus: 'PENDING_REVIEW', newStatus: 'APPROVED' });

    render(<ContentApprovalQueuePage />);

    expect(await screen.findByText('Bài viết 1')).toBeTruthy();

    const publishAllButtons = screen.getAllByRole('button', { name: /Xuất bản tất cả/i });
    fireEvent.click(publishAllButtons[0]);

    expect(screen.getByText('Xuất bản tất cả bài viết')).toBeTruthy();
    expect(screen.getByText('Xuất bản tất cả FAQ')).toBeTruthy();
    expect(screen.getByText('Xuất bản tất cả Checklist')).toBeTruthy();

    fireEvent.click(screen.getByText('Xuất bản tất cả bài viết'));

    expect(await screen.findByText('Xuất bản tất cả bài viết?')).toBeTruthy();
    expect(screen.getByText(/Bạn có chắc chắn muốn xuất bản 1\/1 bài viết/)).toBeTruthy();

    const confirmBtn = screen.getByRole('button', { name: /Xuất bản \(1 mục\)/i });
    fireEvent.click(confirmBtn);

    await waitFor(() => {
      expect(harness.decideContent).toHaveBeenCalledWith('article-1', 'APPROVE');
    });
  });

  it('allows expanding collapsible list and unchecking items in batch publish modal', async () => {
    harness.fetchStaffContentList.mockResolvedValue({
      content: [
        {
          id: 'article-1', title: 'Bài viết 1', type: 'ARTICLE', stage: 'PREGNANCY',
          version: 1, createdAt: '2026-01-01T00:00:00Z', updatedAt: null,
        },
        {
          id: 'article-2', title: 'Bài viết 2', type: 'ARTICLE', stage: 'POSTPARTUM',
          version: 1, createdAt: '2026-01-01T00:00:00Z', updatedAt: null,
        },
      ],
      number: 0, size: 50, totalElements: 2, totalPages: 1,
    });
    harness.fetchAdminChecklists.mockResolvedValue({ content: [], number: 0, size: 50, totalElements: 0, totalPages: 0 });
    harness.decideContent.mockResolvedValue({ id: 'article-1', previousStatus: 'PENDING_REVIEW', newStatus: 'APPROVED' });

    render(<ContentApprovalQueuePage />);

    expect(await screen.findByText('Bài viết 1')).toBeTruthy();

    const publishAllButtons = screen.getAllByRole('button', { name: /Xuất bản tất cả/i });
    fireEvent.click(publishAllButtons[0]);
    fireEvent.click(screen.getByText('Xuất bản tất cả bài viết'));

    expect(await screen.findByText('Xuất bản tất cả bài viết?')).toBeTruthy();

    const expandBtn = screen.getByRole('button', { name: /Danh sách mục chờ xuất bản/i });
    fireEvent.click(expandBtn);

    expect(screen.getByText('Đã chọn 2 / 2')).toBeTruthy();

    const checkboxes = screen.getAllByRole('checkbox');
    expect(checkboxes.length).toBe(2);

    // Uncheck item 2
    fireEvent.click(checkboxes[1]);

    const confirmBtn = screen.getByRole('button', { name: /Xuất bản \(1 mục\)/i });
    fireEvent.click(confirmBtn);

    await waitFor(() => {
      expect(harness.decideContent).toHaveBeenCalledTimes(1);
      expect(harness.decideContent).toHaveBeenCalledWith('article-1', 'APPROVE');
    });
  });

  it('allows System Admin to publish Pregnancy V2 without a clinical/content sign-off gate', async () => {
    harness.fetchAdminChecklists.mockResolvedValue({
      content: [
        {
          id: 'pregnancy-v2', name: 'WHO Plan 1', stage: 'PREGNANCY', status: 'PENDING_REVIEW',
          description: '', templateType: 'MANDATORY', checklistContractVersion: 2,
          provenanceStatus: 'PENDING_CLINICAL_COPY_SIGN_OFF', versionNo: 1,
          updatedAt: null, itemCount: 5, displayOrder: null, recipientRoles: ['MOTHER'],
        },
      ],
      number: 0, size: 50, totalElements: 1, totalPages: 1,
    });

    render(<ContentApprovalQueuePage />);

    const title = await screen.findByText('WHO Plan 1');
    const row = title.closest('tr');
    expect(row).not.toBeNull();
    const publishButton = within(row!).getByRole('button', { name: /Xuất bản/i });
    expect(publishButton).toHaveProperty('disabled', false);
    fireEvent.click(publishButton);
    const confirmButtons = await screen.findAllByRole('button', { name: /Xuất bản/i });
    fireEvent.click(confirmButtons[confirmButtons.length - 1]);
    await waitFor(() => expect(harness.decideChecklistTemplate).toHaveBeenCalledWith('pregnancy-v2', 'APPROVE', undefined));
  });

  it('includes Pregnancy V2 checklists in batch publish without a clinical/content sign-off gate', async () => {
    harness.fetchStaffContentList.mockResolvedValue({
      content: [
        {
          id: 'article-eligible', title: 'Bài viết hợp lệ', type: 'ARTICLE', stage: 'PREGNANCY',
          version: 1, createdAt: '2026-01-01T00:00:00Z', updatedAt: null,
        },
      ],
      number: 0, size: 50, totalElements: 1, totalPages: 1,
    });
    harness.fetchAdminChecklists.mockResolvedValue({
      content: [
        {
          id: 'pregnancy-v2', name: 'WHO Plan chưa sign-off', stage: 'PREGNANCY', status: 'PENDING_REVIEW',
          description: '', templateType: 'MANDATORY', checklistContractVersion: 2,
          provenanceStatus: 'PENDING_CLINICAL_COPY_SIGN_OFF', versionNo: 1,
          updatedAt: null, itemCount: 5, displayOrder: null, recipientRoles: ['MOTHER'],
        },
      ],
      number: 0, size: 50, totalElements: 1, totalPages: 1,
    });
    harness.decideContent.mockResolvedValue({ id: 'article-eligible', previousStatus: 'PENDING_REVIEW', newStatus: 'APPROVED' });

    render(<ContentApprovalQueuePage />);

    expect(await screen.findByText('Bài viết hợp lệ')).toBeTruthy();
    fireEvent.click(screen.getAllByRole('button', { name: /Xuất bản tất cả/i })[0]);
    fireEvent.click(screen.getAllByRole('button', { name: /Xuất bản tất cả/i })[1]);
    fireEvent.click(screen.getByRole('button', { name: /Xuất bản \(2 mục\)/i }));

    await waitFor(() => expect(harness.decideContent).toHaveBeenCalledWith('article-eligible', 'APPROVE'));
    await waitFor(() => expect(harness.decideChecklistTemplate).toHaveBeenCalledWith('pregnancy-v2', 'APPROVE'));
  });
});

