// @vitest-environment jsdom

import { cleanup, render, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import type { AdminChecklistTemplateDetail } from '../models/content';

const harness = vi.hoisted(() => ({
  fetchChecklistTemplateDetail: vi.fn(),
  navigate: vi.fn(),
}));

vi.mock('../services/contentApi', () => ({
  fetchChecklistTemplateDetail: harness.fetchChecklistTemplateDetail,
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
    harness.navigate.mockReset();
  });

  afterEach(cleanup);

  it('shows the current version and explains the counter-only history model', async () => {
    harness.fetchChecklistTemplateDetail.mockResolvedValue(checklistDetail());

    render(<ChecklistVersionHistoryPage />);

    expect(await screen.findByText('Phiên bản hiện tại: v4')).toBeTruthy();
    expect(screen.getByText('Checklist thai kỳ · 2 mục')).toBeTruthy();
    expect(screen.getByText(/chưa lưu snapshot lịch sử/)).toBeTruthy();
  });

  it('shows a safe Vietnamese error without fake version data', async () => {
    harness.fetchChecklistTemplateDetail.mockRejectedValue(new Error('synthetic failure'));

    render(<ChecklistVersionHistoryPage />);

    expect((await screen.findByRole('alert')).textContent).toBe(
      'Không thể tải thông tin phiên bản checklist.',
    );
    expect(screen.queryByText(/Phiên bản hiện tại:/)).toBeNull();
  });
});
