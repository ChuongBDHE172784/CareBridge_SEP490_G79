import { describe, expect, it } from 'vitest';
import { STAGE_LABELS, STAGE_OPTIONS } from './content';

describe('content stage metadata', () => {
  it('exposes exactly the three canonical stages in lifecycle order', () => {
    expect(STAGE_OPTIONS).toEqual([
      { value: 'PRE_PREGNANCY', label: 'Chuẩn bị mang thai' },
      { value: 'PREGNANCY', label: 'Thai kỳ' },
      { value: 'POSTPARTUM', label: 'Hậu sản & Chăm bé' },
    ]);
  });

  it('keeps stage labels synchronized with the ordered options', () => {
    expect(Object.keys(STAGE_LABELS)).toEqual(
      STAGE_OPTIONS.map(({ value }) => value),
    );
    expect(Object.values(STAGE_LABELS)).not.toContain('Chăm bé');
    expect(Object.values(STAGE_LABELS)).not.toContain('Sau sinh');
  });
});
