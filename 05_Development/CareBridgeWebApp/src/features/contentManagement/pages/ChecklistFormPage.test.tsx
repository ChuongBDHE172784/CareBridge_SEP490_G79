// @vitest-environment jsdom

import { cleanup, render, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import type { AdminChecklistTemplateDetail } from '../models/content';

let routeId: string | undefined;

const harness = vi.hoisted(() => ({
  fetchChecklistTemplateDetail: vi.fn(),
  createChecklistTemplate: vi.fn(),
  updateChecklistTemplate: vi.fn(),
  navigate: vi.fn(),
}));

vi.mock('../services/contentApi', () => ({
  fetchChecklistTemplateDetail: harness.fetchChecklistTemplateDetail,
  createChecklistTemplate: harness.createChecklistTemplate,
  updateChecklistTemplate: harness.updateChecklistTemplate,
}));

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');
  return {
    ...actual,
    useParams: () => ({ id: routeId }),
    useNavigate: () => harness.navigate,
  };
});

import ChecklistFormPage from './ChecklistFormPage';

function checklistDetail(): AdminChecklistTemplateDetail {
  return {
    id: 'checklist-123',
    name: 'Checklist cần sửa',
    stage: 'PREGNANCY',
    status: 'DRAFT',
    description: 'Mô tả',
    versionNo: 4,
    items: [],
  };
}

describe('ChecklistFormPage version', () => {
  beforeEach(() => {
    routeId = undefined;
    harness.fetchChecklistTemplateDetail.mockReset();
    harness.createChecklistTemplate.mockReset();
    harness.updateChecklistTemplate.mockReset();
    harness.navigate.mockReset();
    vi.stubGlobal('crypto', { randomUUID: vi.fn(() => 'row-id') });
  });

  afterEach(() => {
    cleanup();
    vi.unstubAllGlobals();
  });

  it('shows the fetched current version while editing', async () => {
    routeId = 'checklist-123';
    harness.fetchChecklistTemplateDetail.mockResolvedValue(checklistDetail());

    render(<ChecklistFormPage />);

    expect(await screen.findByText('Chỉnh sửa Checklist')).toBeTruthy();
    expect(screen.getByText('Phiên bản hiện tại: v4')).toBeTruthy();
  });

  it('does not show a current version while creating', () => {
    render(<ChecklistFormPage />);

    expect(screen.getByText('Tạo Checklist mới')).toBeTruthy();
    expect(screen.queryByText(/Phiên bản hiện tại:/)).toBeNull();
    expect(harness.fetchChecklistTemplateDetail).not.toHaveBeenCalled();
  });
});
