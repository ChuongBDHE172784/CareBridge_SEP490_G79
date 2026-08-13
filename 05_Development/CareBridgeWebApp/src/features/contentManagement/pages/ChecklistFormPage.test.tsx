// @vitest-environment jsdom

import '@testing-library/jest-dom/vitest';
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

  it('loads item description and support function into the editable row', async () => {
    routeId = 'checklist-123';
    harness.fetchChecklistTemplateDetail.mockResolvedValue({
      ...checklistDetail(),
      items: [{
        id: 'item-1',
        itemText: 'Ghi lại chỉ số sức khỏe',
        order: 1,
        isRequired: true,
        targetSubject: 'MOTHER',
        description: 'Theo dõi và cập nhật chỉ số mỗi ngày.',
        supportFunction: 'HEALTH_RECORDS',
      }],
    });

    render(<ChecklistFormPage />);

    expect(await screen.findByDisplayValue('Ghi lại chỉ số sức khỏe')).toBeTruthy();
    expect(screen.getByDisplayValue('Theo dõi và cập nhật chỉ số mỗi ngày.')).toBeTruthy();
    expect(screen.getByLabelText('Chức năng hỗ trợ mục 1')).toHaveProperty('value', 'HEALTH_RECORDS');
  });

  it('does not show a current version while creating', () => {
    render(<ChecklistFormPage />);

    expect(screen.getByText('Tạo Checklist mới')).toBeTruthy();
    expect(screen.queryByText(/Phiên bản hiện tại:/)).toBeNull();
    expect(screen.queryByRole('button', { name: 'Xem toàn bộ lịch sử' })).toBeNull();
    expect(harness.fetchChecklistTemplateDetail).not.toHaveBeenCalled();
  });

  it('renders V2 authoring controls without explicit recipient selection card', () => {
    render(<ChecklistFormPage />);

    expect(screen.queryByRole('checkbox', { name: 'Recipient MOTHER' })).toBeNull();
    expect(screen.queryByRole('checkbox', { name: 'Recipient FAMILY' })).toBeNull();
    expect(screen.queryByRole('region', { name: 'Checklist contract' })).toBeNull();
    expect(screen.queryByRole('radio', { name: 'Recommendation-only V2 contract' })).toBeNull();
    expect(screen.queryByRole('radio', { name: 'Legacy V1 target-bearing contract' })).toBeNull();
    expect(screen.getByLabelText('List weekly recurrence')).toBeTruthy();
    expect(screen.getByLabelText('List daily recurrence')).toBeTruthy();
    expect(screen.queryByLabelText('Item 1 target')).toBeNull();
    expect(screen.getByRole('checkbox', { name: 'Bắt buộc' })).toBeTruthy();
  });

  it.each(['PREGNANCY', 'POSTPARTUM'] as const)('keeps the lifecycle anchor internal for %s instead of exposing it in the form', async (selectedStage) => {
    const user = userEvent.setup();
    render(<ChecklistFormPage />);

    await user.selectOptions(screen.getByLabelText('Lifecycle stage'), selectedStage);

    expect(screen.queryByLabelText('Lifecycle anchor')).toBeNull();
    expect(screen.queryByText('Mốc tính')).toBeNull();
  });

  it('keeps V2 item payload targetless while serializing requiredness', async () => {
    const user = userEvent.setup();
    harness.createChecklistTemplate.mockResolvedValue({
      id: 'v2-created', name: 'Daily recommendation', description: '', stage: 'PREGNANCY',
      status: 'DRAFT', versionNo: 1, items: [], recipientRoles: ['MOTHER'],
    });
    render(<ChecklistFormPage />);

    await user.type(screen.getByLabelText('Template name'), 'Daily recommendation');
    await user.selectOptions(screen.getByLabelText('Lifecycle stage'), 'PREGNANCY');
    await user.type(screen.getByLabelText('Item 1 text'), 'Uống đủ nước');
    expect(screen.queryByLabelText('Item 1 target')).toBeNull();
    expect(screen.getByRole('checkbox', { name: 'Bắt buộc' })).toHaveProperty('checked', true);
    await user.click(screen.getByRole('button', { name: 'Save draft' }));

    await waitFor(() => expect(harness.createChecklistTemplate).toHaveBeenCalledWith(expect.objectContaining({
      checklistContractVersion: 2,
      items: [expect.objectContaining({ itemText: 'Uống đủ nước' })],
    })));
    const payload = harness.createChecklistTemplate.mock.calls[0][0];
    expect(payload.items[0]).not.toHaveProperty('targetSubject');
    expect(payload.items[0]).toHaveProperty('isRequired', true);
  });

  it.each([
    ['PRE_PREGNANCY', 2],
    ['PREGNANCY', 2],
    ['POSTPARTUM', 1],
    ['BABY_CARE', 1],
  ] as const)('derives checklist contract V%i for %s stage', async (selectedStage, expectedVersion) => {
    const user = userEvent.setup();
    harness.createChecklistTemplate.mockResolvedValue({
      id: `created-${selectedStage}`, name: 'Stage defaults', description: '', stage: selectedStage,
      status: 'DRAFT', versionNo: 1, items: [], recipientRoles: ['MOTHER'],
    });
    render(<ChecklistFormPage />);

    await user.type(screen.getByLabelText('Template name'), 'Stage defaults');
    await user.selectOptions(screen.getByLabelText('Lifecycle stage'), selectedStage);
    await user.click(screen.getByRole('button', { name: 'Save draft' }));

    await waitFor(() => expect(harness.createChecklistTemplate).toHaveBeenCalledWith(expect.objectContaining({
      checklistContractVersion: expectedVersion,
      stage: selectedStage,
    })));
  });

  it('uses the birth-date anchor and baby target for Chăm bé', async () => {
    const user = userEvent.setup();
    harness.createChecklistTemplate.mockResolvedValue({
      id: 'created-baby-care', name: 'Baby care', description: '', stage: 'BABY_CARE',
      status: 'DRAFT', versionNo: 1, items: [], recipientRoles: ['MOTHER'],
    });
    render(<ChecklistFormPage />);
    await user.type(screen.getByLabelText('Template name'), 'Baby care');
    await user.selectOptions(screen.getByLabelText('Lifecycle stage'), 'BABY_CARE');
    await user.type(screen.getByLabelText('Item 1 text'), 'Theo dõi giấc ngủ của bé');
    expect(screen.getByLabelText('Lifecycle window start')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Save draft' }));
    await waitFor(() => expect(harness.createChecklistTemplate).toHaveBeenCalledWith(
      expect.objectContaining({
        stage: 'BABY_CARE', checklistContractVersion: 1, scheduleContextType: 'BABY',
      }),
    ));
    const payload = harness.createChecklistTemplate.mock.calls.at(-1)?.[0];
    expect(payload.substage).toEqual(expect.objectContaining({ anchor: 'BIRTH_DATE' }));
    expect(payload.items[0]?.targetSubject).toBe('BABY');
  });

  it('serializes a source-facing single week as a zero-based runtime offset', async () => {
    const user = userEvent.setup();
    harness.createChecklistTemplate.mockResolvedValue({
      id: 'single-week', name: 'Week 21', description: '', stage: 'PREGNANCY',
      status: 'DRAFT', versionNo: 1, items: [], recipientRoles: ['MOTHER'],
    });
    render(<ChecklistFormPage />);

    await user.type(screen.getByLabelText('Template name'), 'Week 21');
    await user.selectOptions(screen.getByLabelText('Lifecycle stage'), 'PREGNANCY');
    await user.selectOptions(screen.getByLabelText('Lifecycle window mode'), 'SINGLE');
    await user.selectOptions(screen.getByLabelText('Lifecycle window start'), '21');
    await user.click(screen.getByRole('button', { name: 'Save draft' }));

    await waitFor(() => expect(harness.createChecklistTemplate).toHaveBeenCalledWith(expect.objectContaining({
      substage: {
        code: 'PREGNANCY_LMP_WEEK_20_20',
        anchor: 'LMP',
        startInclusive: 20,
        endInclusive: 20,
        unit: 'WEEK',
      },
    })));
  });

  it('serializes a source-facing week range 21-25 as offsets 20-24', async () => {
    const user = userEvent.setup();
    harness.createChecklistTemplate.mockResolvedValue({
      id: 'range-week', name: 'Plan 2', description: '', stage: 'PREGNANCY',
      status: 'DRAFT', versionNo: 1, items: [], recipientRoles: ['MOTHER'],
    });
    render(<ChecklistFormPage />);

    await user.type(screen.getByLabelText('Template name'), 'Plan 2');
    await user.selectOptions(screen.getByLabelText('Lifecycle stage'), 'PREGNANCY');
    await user.selectOptions(screen.getByLabelText('Lifecycle window start'), '21');
    await user.selectOptions(screen.getByLabelText('Lifecycle window end'), '25');
    await user.click(screen.getByRole('button', { name: 'Save draft' }));

    await waitFor(() => expect(harness.createChecklistTemplate).toHaveBeenCalledWith(expect.objectContaining({
      substage: {
        code: 'PREGNANCY_LMP_WEEK_20_24',
        anchor: 'LMP',
        startInclusive: 20,
        endInclusive: 24,
        unit: 'WEEK',
      },
    })));
  });

  it('maps the list-level weekly checkbox to root weekly cadence', async () => {
    const user = userEvent.setup();
    harness.createChecklistTemplate.mockResolvedValue({
      id: 'weekly-item', name: 'Weekly item', description: '', stage: 'PREGNANCY',
      status: 'DRAFT', versionNo: 1, items: [], recipientRoles: ['MOTHER'],
    });
    render(<ChecklistFormPage />);

    await user.type(screen.getByLabelText('Template name'), 'Weekly item');
    await user.selectOptions(screen.getByLabelText('Lifecycle stage'), 'PREGNANCY');
    await user.type(screen.getByLabelText('Item 1 text'), 'Theo dõi huyết áp');
    await user.click(screen.getByLabelText('List weekly recurrence'));
    await user.click(screen.getByRole('button', { name: 'Save draft' }));

    await waitFor(() => expect(harness.createChecklistTemplate).toHaveBeenCalledWith(expect.objectContaining({
      scheduleType: 'WEEKLY',
      materializationPolicy: 'EACH_WEEK',
      items: [expect.objectContaining({ repeatWeekly: true, repeatDaily: false })],
    })));
  });

  it('applies list-level recurrence to all items in the checklist', async () => {
    const user = userEvent.setup();
    harness.createChecklistTemplate.mockResolvedValue({
      id: 'list-recurrence', name: 'Shared cadence', description: '', stage: 'PREGNANCY',
      status: 'DRAFT', versionNo: 1, items: [], recipientRoles: ['MOTHER'],
    });
    render(<ChecklistFormPage />);

    await user.type(screen.getByLabelText('Template name'), 'Shared cadence');
    await user.selectOptions(screen.getByLabelText('Lifecycle stage'), 'PREGNANCY');
    await user.type(screen.getByLabelText('Item 1 text'), 'Theo dõi huyết áp');
    await user.click(screen.getByLabelText('List weekly recurrence'));
    await user.click(screen.getByRole('button', { name: 'Thêm mục' }));
    await user.type(screen.getByLabelText('Item 2 text'), 'Khám thai lần đầu');
    await user.click(screen.getByRole('button', { name: 'Save draft' }));

    await waitFor(() => expect(harness.createChecklistTemplate).toHaveBeenCalledWith(expect.objectContaining({
      scheduleType: 'WEEKLY',
      materializationPolicy: 'EACH_WEEK',
      items: [
        expect.objectContaining({ itemText: 'Theo dõi huyết áp', repeatWeekly: true }),
        expect.objectContaining({ itemText: 'Khám thai lần đầu', repeatWeekly: true }),
      ],
    })));
  });

  it('loads recurrence checkbox state when editing an existing item', async () => {
    routeId = 'recurrence-edit';
    harness.fetchChecklistTemplateDetail.mockResolvedValue({
      ...checklistDetail(),
      checklistContractVersion: 2,
      items: [{
        id: 'item-weekly', itemText: 'Theo dõi huyết áp', order: 1,
        isRequired: null, targetSubject: null, repeatWeekly: true, repeatDaily: false,
      }],
    });
    render(<ChecklistFormPage />);

    expect(await screen.findByDisplayValue('Theo dõi huyết áp')).toBeTruthy();
    expect((screen.getByLabelText('List weekly recurrence') as HTMLInputElement).checked).toBe(true);
    expect((screen.getByLabelText('List daily recurrence') as HTMLInputElement).checked).toBe(false);
  });

  it('does not invent a lifecycle substage for a family-neutral draft', async () => {
    routeId = 'neutral-edit';
    harness.fetchChecklistTemplateDetail.mockResolvedValue({
      ...checklistDetail(),
      stage: null,
      recipientRoles: ['MOTHER'],
      substage: null,
    });
    render(<ChecklistFormPage />);

    expect(await screen.findByText('Chỉnh sửa Checklist')).toBeTruthy();
    expect(screen.getByLabelText('Lifecycle stage')).toHaveProperty('value', '');
    expect(screen.queryByLabelText('Lifecycle window start')).toBeNull();
  });

  it('preserves an open-ended pregnancy window while editing', async () => {
    routeId = 'open-ended-edit';
    harness.fetchChecklistTemplateDetail.mockResolvedValue({
      ...checklistDetail(),
      substage: {
        code: 'PREGNANCY_LMP_WEEK_39_2147483647',
        anchor: 'LMP',
        startInclusive: 39,
        endInclusive: 2147483647,
        unit: 'WEEK',
      },
    });
    render(<ChecklistFormPage />);

    expect(await screen.findByText('Chỉnh sửa Checklist')).toBeTruthy();
    expect(screen.getByLabelText('Lifecycle window start')).toHaveProperty('value', '40');
    expect(screen.getByLabelText('Lifecycle window end')).toHaveProperty('value', 'STAGE_EXIT');
  });

  it('renders lifecycle targeting for default MOTHER recipient', async () => {
    const user = userEvent.setup();
    render(<ChecklistFormPage />);

    expect(screen.getByRole('region', { name: 'Lifecycle targeting' })).toBeTruthy();
    await user.selectOptions(screen.getByLabelText('Lifecycle stage'), 'PRE_PREGNANCY');
    expect(screen.getByRole('region', { name: 'Lifecycle targeting' })).toBeTruthy();
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
      substage: expect.objectContaining({ code: 'PREGNANCY_LMP_WEEK_0_19', anchor: 'LMP', unit: 'WEEK' }),
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
    expect(screen.queryByLabelText('Lifecycle window start')).toBeNull();
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

  it('serializes weekly cadence and postpartum stage window', async () => {
    const user = userEvent.setup();
    harness.createChecklistTemplate.mockResolvedValue({
      id: 'created-2', name: 'Mixed checks', description: '', stage: 'PREGNANCY',
      status: 'DRAFT', versionNo: 1, items: [], recipientRoles: ['MOTHER'],
    });
    render(<ChecklistFormPage />);

    await user.type(screen.getByLabelText('Template name'), 'Mixed checks');
    await user.selectOptions(screen.getByLabelText('Lifecycle stage'), 'POSTPARTUM');
    await user.selectOptions(screen.getByLabelText('Lifecycle window start'), '1');
    await user.selectOptions(screen.getByLabelText('Lifecycle window end'), '6');
    expect(screen.getByText('Cửa sổ vòng đời')).toBeTruthy();
    await user.click(screen.getByLabelText('List weekly recurrence'));
    await user.type(screen.getByLabelText('Item 1 text'), 'Prepare documents');
    await user.type(screen.getByLabelText('Nội dung chi tiết mục 1'), 'Chuẩn bị giấy tờ cần thiết.');
    await user.selectOptions(screen.getByLabelText('Chức năng hỗ trợ mục 1'), 'CONTENT_LIBRARY');
    await user.click(screen.getByRole('button', { name: 'Save draft' }));

    await waitFor(() => expect(harness.createChecklistTemplate).toHaveBeenCalledWith(expect.objectContaining({
      recipientRoles: ['MOTHER'],
      checklistContractVersion: 1,
      stage: 'POSTPARTUM',
      scheduleContextType: 'JOURNEY',
      substage: expect.objectContaining({
        code: 'POSTPARTUM_DELIVERY_DATE_WEEK_0_5', anchor: 'DELIVERY_DATE', unit: 'WEEK',
      }),
      items: [expect.objectContaining({
        description: 'Chuẩn bị giấy tờ cần thiết.',
        supportFunction: 'CONTENT_LIBRARY',
      })],
    })));
  });

  it('serializes maternal health metrics as a support destination', async () => {
    const user = userEvent.setup();
    harness.createChecklistTemplate.mockResolvedValue({
      id: 'created-maternal-health-metrics', name: 'Theo dõi chỉ số', description: '', stage: 'PREGNANCY',
      status: 'DRAFT', versionNo: 1, items: [], recipientRoles: ['MOTHER'],
    });
    render(<ChecklistFormPage />);

    await user.type(screen.getByLabelText('Template name'), 'Theo dõi chỉ số');
    await user.selectOptions(screen.getByLabelText('Lifecycle stage'), 'PREGNANCY');
    await user.type(screen.getByLabelText('Item 1 text'), 'Ghi nhận chỉ số sức khỏe');
    await user.selectOptions(screen.getByRole('combobox', { name: /hỗ trợ mục 1/i }), 'MATERNAL_HEALTH_METRICS');
    await user.click(screen.getByRole('button', { name: 'Save draft' }));

    await waitFor(() => expect(harness.createChecklistTemplate).toHaveBeenCalledWith(expect.objectContaining({
      items: [expect.objectContaining({ supportFunction: 'MATERNAL_HEALTH_METRICS' })],
    })));
  });

  it('serializes maternal exercises as a support destination', async () => {
    const user = userEvent.setup();
    harness.createChecklistTemplate.mockResolvedValue({
      id: 'created-maternal-exercises', name: 'Bài tập cho mẹ', description: '', stage: 'PREGNANCY',
      status: 'DRAFT', versionNo: 1, items: [], recipientRoles: ['MOTHER'],
    });
    render(<ChecklistFormPage />);

    await user.type(screen.getByLabelText('Template name'), 'Bài tập cho mẹ');
    await user.selectOptions(screen.getByLabelText('Lifecycle stage'), 'PREGNANCY');
    await user.type(screen.getByLabelText('Item 1 text'), 'Thực hiện bài tập phù hợp');
    await user.selectOptions(screen.getByRole('combobox', { name: /hỗ trợ mục 1/i }), 'MATERNAL_EXERCISES');
    await user.click(screen.getByRole('button', { name: 'Save draft' }));

    await waitFor(() => expect(harness.createChecklistTemplate).toHaveBeenCalledWith(expect.objectContaining({
      items: [expect.objectContaining({ supportFunction: 'MATERNAL_EXERCISES' })],
    })));
  });

  it('uses AA contrast tokens for readable secondary copy', () => {
    render(<ChecklistFormPage />);

    const guidance = screen.getByText((value) => value.startsWith('Checklist V2'));
    expect(guidance.className).toContain('text-on-surface-variant');
    expect(guidance.className).not.toContain('text-[#9C857C]');
  });

  it('disables weekly recurrence checkbox when lifecycle window is 1 week', async () => {
    const user = userEvent.setup();
    render(<ChecklistFormPage />);

    await user.selectOptions(screen.getByLabelText('Lifecycle stage'), 'PREGNANCY');
    expect((screen.getByLabelText('List weekly recurrence') as HTMLInputElement).disabled).toBe(false);

    await user.selectOptions(screen.getByLabelText('Lifecycle window mode'), 'SINGLE');
    expect((screen.getByLabelText('List weekly recurrence') as HTMLInputElement).disabled).toBe(true);
  });
});
