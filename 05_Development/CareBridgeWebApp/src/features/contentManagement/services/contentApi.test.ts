import { beforeEach, describe, expect, it, vi } from 'vitest';
import apiClient from '../../../shared/api/apiClient';
import type { AdminChecklistTemplateDetail, PaginatedResponse } from '../models/content';
import { fetchAdminChecklistTemplates } from './contentApi';

vi.mock('../../../shared/api/apiClient', () => ({
  default: {
    get: vi.fn(),
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
      page: 2,
      size: 7,
    });

    expect(apiClient.get).toHaveBeenCalledWith('/api/v1/admin/checklist-templates', {
      params: { status: 'REJECTED', stage: 'POSTPARTUM', page: 2, size: 7 },
    });
    expect(result).toEqual(response);
    expect(result.content[0]).toEqual(expect.objectContaining({
      recipientRoles: ['MOTHER', 'FAMILY'],
      substage: expect.objectContaining({ code: 'BABY_CARE_MONTH_0_3' }),
      items: [expect.objectContaining({ targetSubject: 'BABY' })],
    }));
  });
});
