// @vitest-environment jsdom

import { cleanup, render, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import type { AdminChecklistTemplateDetail } from '../models/content';

const harness = vi.hoisted(() => ({
  fetchChecklistTemplateDetail: vi.fn(),
  updateChecklistTemplate: vi.fn(),
  archiveChecklistTemplate: vi.fn(),
  navigate: vi.fn(),
  hasRole: vi.fn(),
}));

vi.mock('../services/contentApi', () => ({
  fetchChecklistTemplateDetail: harness.fetchChecklistTemplateDetail,
  updateChecklistTemplate: harness.updateChecklistTemplate,
  archiveChecklistTemplate: harness.archiveChecklistTemplate,
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
    name: 'Checklist phiên bản ba',
    stage: 'PREGNANCY',
    status: 'DRAFT',
    description: 'Mô tả',
    versionNo: 3,
    items: [],
  };
}

describe('ChecklistDetailPage version', () => {
  beforeEach(() => {
    harness.fetchChecklistTemplateDetail.mockReset();
    harness.updateChecklistTemplate.mockReset();
    harness.archiveChecklistTemplate.mockReset();
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
  });
});
