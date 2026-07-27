// @vitest-environment jsdom

import { cleanup, render, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import type { ContentDetail } from '../models/content';

const harness = vi.hoisted(() => ({
  fetchStaffContentDetail: vi.fn(),
  updateContent: vi.fn(),
  archiveContent: vi.fn(),
  navigate: vi.fn(),
  hasRole: vi.fn(),
}));

vi.mock('../services/contentApi', () => ({
  fetchStaffContentDetail: harness.fetchStaffContentDetail,
  updateContent: harness.updateContent,
  archiveContent: harness.archiveContent,
}));

vi.mock('../../../shared/auth/useAuth', () => ({
  useAuth: () => ({ hasRole: harness.hasRole }),
}));

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>(
    'react-router-dom',
  );
  return {
    ...actual,
    useParams: () => ({ id: 'content-123' }),
    useNavigate: () => harness.navigate,
  };
});

import ContentDetailPage from './ContentDetailPage';

function contentDetail(overrides: Partial<ContentDetail> = {}): ContentDetail {
  return {
    id: 'content-123',
    type: 'FAQ',
    title: 'FAQ không có ảnh minh họa',
    body: '<p>Nội dung chỉ có chữ.</p>',
    stage: 'PREGNANCY',
    topicId: 'topic-1',
    version: 1,
    publishedAt: null,
    status: 'DRAFT',
    createdAt: '2026-07-27T00:00:00Z',
    updatedAt: null,
    sourceLabel: null,
    sources: [],
    ...overrides,
  };
}

describe('ContentDetailPage', () => {
  beforeEach(() => {
    harness.fetchStaffContentDetail.mockReset();
    harness.updateContent.mockReset();
    harness.archiveContent.mockReset();
    harness.navigate.mockReset();
    harness.hasRole.mockReset();
    harness.hasRole.mockReturnValue(true);
  });

  afterEach(() => {
    cleanup();
  });

  it('does not render a synthetic hero image placeholder when content has no image', async () => {
    harness.fetchStaffContentDetail.mockResolvedValue(contentDetail());

    render(<ContentDetailPage />);

    expect(await screen.findByText('FAQ không có ảnh minh họa')).toBeTruthy();
    expect(screen.getByText('Nội dung chỉ có chữ.')).toBeTruthy();
    expect(screen.queryByText('image')).toBeNull();
  });

  it('shows the full return reason to a Content Admin', async () => {
    harness.fetchStaffContentDetail.mockResolvedValue(contentDetail({
      latestReviewFeedback: {
        reason: 'Cần bổ sung nguồn y khoa đáng tin cậy',
        requestedAt: '2026-07-27T10:00:00Z',
        requestedBy: 'system-admin-id',
        versionNo: 1,
      },
    }));

    render(<ContentDetailPage />);

    expect(await screen.findByText('System Admin yêu cầu chỉnh sửa')).toBeTruthy();
    expect(screen.getByText('Cần bổ sung nguồn y khoa đáng tin cậy')).toBeTruthy();
    expect(screen.getByText('Cần chỉnh sửa')).toBeTruthy();
  });
});
