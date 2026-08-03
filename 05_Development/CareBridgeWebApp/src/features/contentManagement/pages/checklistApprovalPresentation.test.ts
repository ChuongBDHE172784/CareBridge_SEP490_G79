import { describe, expect, it } from 'vitest';
import {
  checklistApprovalErrorMessage,
  checklistCoexistenceGuidance,
  checklistRecipientLabel,
  checklistSequenceLabel,
} from './checklistApprovalPresentation';

describe('checklist approval presentation', () => {
  it('labels legacy and positive sequence positions distinctly', () => {
    expect(checklistSequenceLabel(0)).toContain('Legacy');
    expect(checklistSequenceLabel(null)).toContain('Legacy');
    expect(checklistSequenceLabel(3)).toContain('3');
    expect(checklistSequenceLabel(0, 'PREGNANCY')).toContain('Không áp dụng');
    expect(checklistSequenceLabel(0, null)).toContain('Không áp dụng');
  });

  it('describes recipients and cohort coexistence', () => {
    expect(checklistRecipientLabel(['MOTHER', 'FAMILY'])).toContain('·');
    expect(checklistCoexistenceGuidance(0)).toContain('legacy');
    expect(checklistCoexistenceGuidance(2)).toContain('PRE_PREGNANCY');
    expect(checklistCoexistenceGuidance(0, 'POSTPARTUM')).toContain('không áp dụng');
  });

  it('maps allowlisted backend reasons without exposing arbitrary server text', () => {
    const known = checklistApprovalErrorMessage({
      response: { data: { metadata: { reasonCode: 'CHECKLIST_ACTIVE_LEGACY_CONFLICT' } } },
    });
    expect(known).toContain('legacy');

    const unknown = checklistApprovalErrorMessage({
      response: { data: { metadata: { reasonCode: 'UNKNOWN', message: 'internal SQL details' } } },
    });
    expect(unknown).not.toContain('internal SQL details');
    expect(unknown).toContain('Vui');
  });
});
