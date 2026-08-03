// @vitest-environment jsdom

import { cleanup, render, screen } from '@testing-library/react';
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
});
