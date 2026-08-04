// @vitest-environment jsdom

import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
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
    templateType: 'MANDATORY',
    name: 'Checklist cần sửa',
    stage: 'PREGNANCY',
    status: 'DRAFT',
    description: 'Mô tả',
    versionNo: 4,
    lineageId: 'lineage-1',
    versionId: 'version-4',
    recipientRoles: ['MOTHER'],
    substage: null,
    migrationReviewRequired: false,
    distributionEnabled: false,
    approvedAt: null,
    approvedBy: null,
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
    fireEvent.click(screen.getByRole('button', { name: 'Xem toàn bộ lịch sử' }));
    expect(harness.navigate).toHaveBeenCalledWith('/content/checklists/checklist-123/versions');
  });

  it('does not show a current version while creating', () => {
    render(<ChecklistFormPage />);

    expect(screen.getByText('Tạo Checklist mới')).toBeTruthy();
    expect(screen.queryByText(/Phiên bản hiện tại:/)).toBeNull();
    expect(screen.queryByRole('button', { name: 'Xem toàn bộ lịch sử' })).toBeNull();
    expect(harness.fetchChecklistTemplateDetail).not.toHaveBeenCalled();
  });

  it('renders recipient role controls for V2 authoring', () => {
    render(<ChecklistFormPage />);

    expect(screen.getByRole('checkbox', { name: 'Recipient MOTHER' })).toBeTruthy();
    expect(screen.getByRole('checkbox', { name: 'Recipient FAMILY' })).toBeTruthy();
  });

  it('renders lifecycle targeting for MOTHER and mixed recipients', async () => {
    const user = userEvent.setup();
    render(<ChecklistFormPage />);

    expect(screen.getByRole('region', { name: 'Lifecycle targeting' })).toBeTruthy();
    await user.selectOptions(screen.getByLabelText('Lifecycle stage'), 'PRE_PREGNANCY');
    await user.click(screen.getByRole('checkbox', { name: 'Recipient FAMILY' }));
    expect(screen.getByRole('region', { name: 'Lifecycle targeting' })).toBeTruthy();
    expect(screen.getByRole('note').textContent).toContain('legacy');
  });

  it('hides and normalizes lifecycle targeting for a FAMILY-only draft before submit', async () => {
    const user = userEvent.setup();
    harness.createChecklistTemplate.mockResolvedValue({
      id: 'created-1', name: 'Family checks', description: '', stage: null,
      status: 'DRAFT', versionNo: 1, items: [], recipientRoles: ['FAMILY'],
    });
    render(<ChecklistFormPage />);

    await user.type(screen.getByLabelText('Template name'), 'Family checks');
    await user.selectOptions(screen.getByLabelText('Lifecycle stage'), 'PREGNANCY');
    await user.click(screen.getByRole('checkbox', { name: 'Recipient FAMILY' }));
    await user.click(screen.getByRole('checkbox', { name: 'Recipient MOTHER' }));

    expect(screen.queryByRole('region', { name: 'Lifecycle targeting' })).toBeNull();
    expect(screen.queryByLabelText('Lifecycle stage')).toBeNull();
    await user.click(screen.getByRole('button', { name: 'Save draft' }));

    await waitFor(() => expect(harness.createChecklistTemplate).toHaveBeenCalledWith(expect.objectContaining({
      recipientRoles: ['FAMILY'], stage: null, substage: null,
    })));
  });

  it('hides and clears lifecycle state when no recipient is selected', async () => {
    const user = userEvent.setup();
    render(<ChecklistFormPage />);

    await user.type(screen.getByLabelText('Template name'), 'No recipient yet');
    await user.selectOptions(screen.getByLabelText('Lifecycle stage'), 'PREGNANCY');
    await user.click(screen.getByRole('checkbox', { name: 'Recipient MOTHER' }));

    expect(screen.queryByRole('region', { name: 'Lifecycle targeting' })).toBeNull();
    expect(screen.queryByLabelText('Lifecycle stage')).toBeNull();
    expect((screen.getByRole('button', { name: 'Save draft' }) as HTMLButtonElement).disabled).toBe(true);

    harness.createChecklistTemplate.mockResolvedValue({
      id: 'created-zero', name: 'No recipient yet', description: '', stage: null,
      status: 'DRAFT', versionNo: 1, items: [], recipientRoles: ['FAMILY'],
    });
    await user.click(screen.getByRole('checkbox', { name: 'Recipient FAMILY' }));
    await user.click(screen.getByRole('button', { name: 'Save draft' }));

    await waitFor(() => expect(harness.createChecklistTemplate).toHaveBeenCalledWith(expect.objectContaining({
      recipientRoles: ['FAMILY'], stage: null, substage: null,
    })));
  });

  it('normalizes an edited MOTHER-to-FAMILY transition before update', async () => {
    const user = userEvent.setup();
    routeId = 'family-transition';
    harness.fetchChecklistTemplateDetail.mockResolvedValue({
      ...checklistDetail(),
      recipientRoles: ['MOTHER', 'FAMILY'],
      substage: {
        code: 'PREGNANCY_LMP_WEEK_0_12', anchor: 'LMP', startInclusive: 0, endInclusive: 12, unit: 'WEEK',
      },
    });
    harness.updateChecklistTemplate.mockResolvedValue(checklistDetail());
    render(<ChecklistFormPage />);

    await screen.findByText('Chỉnh sửa Checklist');
    await user.click(screen.getByRole('checkbox', { name: 'Recipient FAMILY' }));
    await user.click(screen.getByRole('checkbox', { name: 'Recipient MOTHER' }));
    await user.click(screen.getByRole('checkbox', { name: 'Recipient FAMILY' }));
    expect(screen.queryByRole('region', { name: 'Lifecycle targeting' })).toBeNull();

    await user.click(screen.getByRole('button', { name: 'Save draft' }));
    await waitFor(() => expect(harness.updateChecklistTemplate).toHaveBeenCalledWith(
      'family-transition',
      expect.objectContaining({ recipientRoles: ['FAMILY'], stage: null, substage: null, status: 'DRAFT' }),
    ));
  });

  it('clears lifecycle state when an edited MOTHER template transitions to zero roles', async () => {
    const user = userEvent.setup();
    routeId = 'zero-transition';
    harness.fetchChecklistTemplateDetail.mockResolvedValue({
      ...checklistDetail(),
      substage: {
        code: 'PREGNANCY_LMP_WEEK_0_12', anchor: 'LMP', startInclusive: 0, endInclusive: 12, unit: 'WEEK',
      },
    });
    render(<ChecklistFormPage />);

    await screen.findByText('Chỉnh sửa Checklist');
    await user.click(screen.getByRole('checkbox', { name: 'Recipient MOTHER' }));

    expect(screen.queryByRole('region', { name: 'Lifecycle targeting' })).toBeNull();
    expect((screen.getByRole('button', { name: 'Save draft' }) as HTMLButtonElement).disabled).toBe(true);
    expect(harness.updateChecklistTemplate).not.toHaveBeenCalled();
  });

  it('serializes an explicit MOTHER-only payload with lifecycle metadata', async () => {
    const user = userEvent.setup();
    harness.createChecklistTemplate.mockResolvedValue({
      id: 'mother-only', name: 'Mother checks', description: '', stage: 'PREGNANCY',
      status: 'DRAFT', versionNo: 1, items: [], recipientRoles: ['MOTHER'],
    });
    render(<ChecklistFormPage />);

    await user.type(screen.getByLabelText('Template name'), 'Mother checks');
    await user.selectOptions(screen.getByLabelText('Lifecycle stage'), 'PREGNANCY');
    await user.click(screen.getByRole('button', { name: 'Save draft' }));

    await waitFor(() => expect(harness.createChecklistTemplate).toHaveBeenCalledWith(expect.objectContaining({
      recipientRoles: ['MOTHER'],
      stage: 'PREGNANCY',
      substage: expect.objectContaining({ code: 'PREGNANCY_LMP_WEEK_0_12', anchor: 'LMP', unit: 'WEEK' }),
    })));
  });

  it('supports Muốn mang thai and optional self-service checklist type', async () => {
    const user = userEvent.setup();
    harness.createChecklistTemplate.mockResolvedValue({
      id: 'pre-optional', name: 'Prepare for pregnancy', description: '', stage: 'PRE_PREGNANCY',
      templateType: 'OPTIONAL', status: 'DRAFT', versionNo: 1, items: [], recipientRoles: ['MOTHER'],
      substage: { code: 'PRE_PREGNANCY_ALL', anchor: 'NONE', startInclusive: 0, endInclusive: 0, unit: 'DAY' },
    });
    harness.updateChecklistTemplate.mockResolvedValue(undefined);
    render(<ChecklistFormPage />);

    await user.type(screen.getByLabelText('Template name'), 'Prepare for pregnancy');
    await user.click(screen.getByLabelText('Optional checklist'));
    await user.selectOptions(screen.getByLabelText('Lifecycle stage'), 'PRE_PREGNANCY');
    expect(screen.queryByLabelText('Lifecycle substage')).toBeNull();
    await user.click(screen.getByRole('button', { name: 'Submit for review' }));

    await waitFor(() => expect(harness.createChecklistTemplate).toHaveBeenCalledWith(expect.objectContaining({
      templateType: 'OPTIONAL',
      recipientRoles: ['MOTHER'],
      stage: 'PRE_PREGNANCY',
      substage: null,
    })));
    await waitFor(() => expect(harness.updateChecklistTemplate).toHaveBeenCalledWith(
      'pre-optional',
      expect.objectContaining({
        templateType: 'OPTIONAL',
        stage: 'PRE_PREGNANCY',
        substage: null,
        status: 'PENDING_REVIEW',
      }),
    ));
  });

  it('serializes mixed recipient roles and explicit item target', async () => {
    const user = userEvent.setup();
    harness.createChecklistTemplate.mockResolvedValue({
      id: 'created-2', name: 'Mixed checks', description: '', stage: 'PREGNANCY',
      status: 'DRAFT', versionNo: 1, items: [], recipientRoles: ['MOTHER', 'FAMILY'],
    });
    render(<ChecklistFormPage />);

    await user.type(screen.getByLabelText('Template name'), 'Mixed checks');
    await user.click(screen.getByRole('checkbox', { name: 'Recipient FAMILY' }));
    await user.selectOptions(screen.getByLabelText('Lifecycle stage'), 'PREGNANCY');
    expect(screen.getByText('Cửa sổ vòng đời')).toBeTruthy();
    await user.selectOptions(screen.getByLabelText('Lifecycle substage'), 'PREGNANCY_LMP_WEEK_0_12');
    await user.type(screen.getByLabelText('Item 1 text'), 'Prepare documents');
    await user.selectOptions(screen.getByLabelText('Item 1 target'), 'BABY');
    await user.click(screen.getByRole('button', { name: 'Save draft' }));

    await waitFor(() => expect(harness.createChecklistTemplate).toHaveBeenCalledWith(expect.objectContaining({
      recipientRoles: ['MOTHER', 'FAMILY'],
      stage: 'PREGNANCY',
      substage: expect.objectContaining({
        code: 'PREGNANCY_LMP_WEEK_0_12', anchor: 'LMP', unit: 'WEEK',
      }),
      items: [expect.objectContaining({ targetSubject: 'BABY' })],
    })));
  });

  it('uses AA contrast tokens for readable secondary copy', () => {
    render(<ChecklistFormPage />);

    const guidance = screen.getByText(/Thi.*t l.*p.*ng.*i nh.*n/);
    expect(guidance.className).toContain('text-on-surface-variant');
    expect(guidance.className).not.toContain('text-[#9C857C]');
  });
});
