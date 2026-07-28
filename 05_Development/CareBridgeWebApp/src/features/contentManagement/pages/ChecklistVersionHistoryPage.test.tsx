// @vitest-environment jsdom

import { cleanup, render, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import type { AdminChecklistTemplateDetail } from '../models/content';

const harness = vi.hoisted(() => ({
  fetchChecklistTemplateDetail: vi.fn(),
  fetchChecklistVersionHistory: vi.fn(),
  navigate: vi.fn(),
}));

vi.mock('../services/contentApi', () => ({
  fetchChecklistTemplateDetail: harness.fetchChecklistTemplateDetail,
  fetchChecklistVersionHistory: harness.fetchChecklistVersionHistory,
}));

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');
  return {
    ...actual,
    useParams: () => ({ id: 'checklist-123' }),
    useNavigate: () => harness.navigate,
  };
});

import ChecklistVersionHistoryPage from './ChecklistVersionHistoryPage';

function checklistDetail(): AdminChecklistTemplateDetail {
  return {
    id: 'checklist-123',
    name: 'Checklist thai kỳ',
    stage: 'PREGNANCY',
    status: 'DRAFT',
    description: 'Mô tả',
    versionNo: 4,
    items: [
      { id: 'item-1', itemText: 'Khám thai', order: 1, isRequired: true },
      { id: 'item-2', itemText: 'Uống vitamin', order: 2, isRequired: false },
    ],
  };
}

describe('ChecklistVersionHistoryPage', () => {
  beforeEach(() => {
    harness.fetchChecklistTemplateDetail.mockReset();
    harness.fetchChecklistVersionHistory.mockReset();
    harness.navigate.mockReset();
  });

  afterEach(cleanup);

  it('lists persisted snapshots with the current version marked', async () => {
    harness.fetchChecklistTemplateDetail.mockResolvedValue(checklistDetail());
    harness.fetchChecklistVersionHistory.mockResolvedValue([
      { versionNo: 4, name: 'Checklist thai kỳ', stage: 'PREGNANCY', status: 'DRAFT', itemCount: 2, changedBy: null, createdAt: '2026-07-27T10:00:00Z' },
      { versionNo: 3, name: 'Checklist thai kỳ cũ', stage: 'PREGNANCY', status: 'DRAFT', itemCount: 1, changedBy: null, createdAt: '2026-07-26T10:00:00Z' },
    ]);

    render(<ChecklistVersionHistoryPage />);

    expect(await screen.findByText('v4')).toBeTruthy();
    expect(screen.getByText('Hiện hành')).toBeTruthy();
    expect(screen.getByText('Checklist thai kỳ cũ')).toBeTruthy();
  });

  it('shows a safe Vietnamese error without fake version data', async () => {
    harness.fetchChecklistTemplateDetail.mockRejectedValue(new Error('synthetic failure'));
    harness.fetchChecklistVersionHistory.mockResolvedValue([]);

    render(<ChecklistVersionHistoryPage />);

    expect((await screen.findByRole('alert')).textContent).toBe(
      'Không thể tải lịch sử phiên bản checklist. Vui lòng thử lại.',
    );
    expect(screen.queryByText('Hiện hành')).toBeNull();
  });
});
