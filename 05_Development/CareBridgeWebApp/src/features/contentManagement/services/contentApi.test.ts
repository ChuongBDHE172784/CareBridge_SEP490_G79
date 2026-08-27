import { beforeEach, describe, expect, it, vi } from 'vitest';
import apiClient from '../../../shared/api/apiClient';
import type { AdminChecklistTemplateDetail, CreateChecklistTemplatePayload, PaginatedResponse } from '../models/content';
import { fetchAdminChecklistTemplates, importChecklistTemplatesBatch } from './contentApi';

vi.mock('../../../shared/api/apiClient', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
}));

function canonicalTemplate(): AdminChecklistTemplateDetail {
  return {
    id: 'template-1',
    templateType: 'MANDATORY',
    lineageId: 'lineage-1',
    versionId: 'version-1',
    name: 'Canonical checklist',
    description: 'Canonical metadata',
    recipientRoles: ['MOTHER', 'FAMILY'],
    stage: 'POSTPARTUM',
    substage: {
      code: 'BABY_CARE_MONTH_0_3',
      anchor: 'BIRTH_DATE',
      startInclusive: 0,
      endInclusive: 3,
      unit: 'MONTH',
    },
    status: 'REJECTED',
    versionNo: 2,
    migrationReviewRequired: false,
    distributionEnabled: false,
    approvedAt: null,
    approvedBy: null,
    items: [
      { id: 'item-1', itemText: 'Theo dõi bé', order: 1, isRequired: true, targetSubject: 'BABY' },
    ],
  };
}

describe('fetchAdminChecklistTemplates', () => {
  beforeEach(() => {
    vi.mocked(apiClient.get).mockReset();
  });

  it('uses the canonical admin endpoint and preserves distribution metadata', async () => {
    const response: PaginatedResponse<AdminChecklistTemplateDetail> = {
      content: [canonicalTemplate()],
      number: 2,
      size: 7,
      totalElements: 15,
      totalPages: 3,
    };
    vi.mocked(apiClient.get).mockResolvedValue({ data: { data: response } } as never);

    const result = await fetchAdminChecklistTemplates({
      status: 'REJECTED',
      stage: 'POSTPARTUM',
      keyword: 'Theo dõi',
      page: 2,
      size: 7,
    });

    expect(apiClient.get).toHaveBeenCalledWith('/api/v1/admin/checklist-templates', {
      params: { status: 'REJECTED', stage: 'POSTPARTUM', keyword: 'Theo dõi', page: 2, size: 7 },
    });
    expect(result).toEqual(response);
    expect(result.content[0]).toEqual(expect.objectContaining({
      recipientRoles: ['MOTHER', 'FAMILY'],
      substage: expect.objectContaining({ code: 'BABY_CARE_MONTH_0_3' }),
      items: [expect.objectContaining({ targetSubject: 'BABY' })],
    }));
  });
});

describe('importChecklistTemplatesBatch', () => {
  beforeEach(() => {
    vi.mocked(apiClient.post).mockReset();
  });

  it('posts the grouped checklist templates to the dedicated batch endpoint', async () => {
    const template: CreateChecklistTemplatePayload = {
      name: 'Checklist import',
      templateType: 'MANDATORY' as const,
      checklistContractVersion: 2,
      recipientRoles: ['MOTHER'],
      stage: 'PREGNANCY' as const,
      substage: { code: 'PREGNANCY_LMP_WEEK_0_3', anchor: 'LMP' as const, startInclusive: 0, endInclusive: 3, unit: 'WEEK' as const },
      items: [{ itemText: 'Mục một', order: 1, isRequired: true, targetSubject: null }],
    };
    const response = { totalRows: 1, successCount: 1, failedCount: 0, errors: [], createdIds: ['created-1'] };
    vi.mocked(apiClient.post).mockResolvedValue({ data: { data: response } } as never);

    await expect(importChecklistTemplatesBatch({
      templates: [{ rowIndex: 2, checklistCode: 'PREG-01', template }],
    })).resolves.toEqual(response);

    expect(apiClient.post).toHaveBeenCalledWith('/api/v1/admin/checklist-templates/import-batch', {
      templates: [{ rowIndex: 2, checklistCode: 'PREG-01', template }],
    });
  });
});
