// @vitest-environment jsdom

import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

const harness = vi.hoisted(() => ({
  fetchStaffContentList: vi.fn(),
  archiveContent: vi.fn(),
  updateContent: vi.fn(),
  navigate: vi.fn(),
}));

vi.mock('../services/contentApi', () => ({
  fetchStaffContentList: harness.fetchStaffContentList,
  archiveContent: harness.archiveContent,
  updateContent: harness.updateContent,
}));

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');
  return { ...actual, useNavigate: () => harness.navigate };
});

import ContentTypeListPage from './ContentTypeListPage';

describe('ContentTypeListPage submit all for approval', () => {
  beforeEach(() => {
    harness.fetchStaffContentList.mockReset();
    harness.archiveContent.mockReset();
    harness.updateContent.mockReset();
    harness.navigate.mockReset();
  });

  afterEach(cleanup);

  it('renders "Gửi phê duyệt tất cả" button and handles bulk submission upon confirmation', async () => {
    // Initial page load response
    harness.fetchStaffContentList.mockResolvedValueOnce({
      content: [
        {
          id: 'art-1', title: 'Bài viết Nháp 1', type: 'ARTICLE', stage: 'PREGNANCY',
          status: 'DRAFT', createdAt: '2026-01-01T00:00:00Z', tagIds: [], sources: [],
        },
      ],
      totalElements: 1,
      totalPages: 1,
      page: 0,
      size: 10,
    });

    render(
      <ContentTypeListPage
        type="ARTICLE"
        title="Quản lý Bài viết"
        subtitle="Quản lý các bài viết nội dung"
        createLabel="Tạo Bài viết Mới"
        emptyLabel="Không có bài viết nào."
      />
    );

    expect(await screen.findByText('Bài viết Nháp 1')).toBeTruthy();

    const submitAllBtn = screen.getByRole('button', { name: /Gửi phê duyệt tất cả/i });
    expect(submitAllBtn).toBeTruthy();

    // Mock response when fetching all DRAFT items for modal
    harness.fetchStaffContentList.mockResolvedValueOnce({
      content: [
        {
          id: 'art-1', title: 'Bài viết Nháp 1', type: 'ARTICLE', stage: 'PREGNANCY',
          status: 'DRAFT', createdAt: '2026-01-01T00:00:00Z', tagIds: [], sources: [],
        },
        {
          id: 'art-2', title: 'Bài viết Nháp 2', type: 'ARTICLE', stage: 'POSTPARTUM',
          status: 'DRAFT', createdAt: '2026-01-01T00:00:00Z', tagIds: [], sources: [],
        },
      ],
      totalElements: 2,
      totalPages: 1,
      page: 0,
      size: 50,
    });

    harness.updateContent.mockResolvedValue({ id: 'art-1', status: 'PENDING_REVIEW', versionNo: 1 });

    fireEvent.click(submitAllBtn);

    expect(await screen.findByText('Gửi phê duyệt tất cả bài viết?')).toBeTruthy();
    expect(screen.getByText(/Bạn có chắc chắn muốn gửi phê duyệt tất cả 2 bài viết bản nháp/)).toBeTruthy();

    const confirmBtn = screen.getByRole('button', { name: /Gửi phê duyệt \(2 mục\)/i });
    fireEvent.click(confirmBtn);

    await waitFor(() => {
      expect(harness.updateContent).toHaveBeenCalledTimes(2);
      expect(harness.updateContent).toHaveBeenNthCalledWith(1, 'art-1', expect.objectContaining({ status: 'PENDING_REVIEW' }));
      expect(harness.updateContent).toHaveBeenNthCalledWith(2, 'art-2', expect.objectContaining({ status: 'PENDING_REVIEW' }));
    });
  });
});
