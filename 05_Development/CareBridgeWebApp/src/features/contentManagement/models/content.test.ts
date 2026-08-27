import { describe, expect, it } from 'vitest';
import { CHECKLIST_SUPPORT_FUNCTION_OPTIONS, STAGE_LABELS, STAGE_OPTIONS } from './content';

describe('content stage metadata', () => {
  it('exposes canonical stages in lifecycle order', () => {
    expect(STAGE_OPTIONS).toEqual([
      { value: 'PRE_PREGNANCY', label: 'Chuẩn bị mang thai' },
      { value: 'PREGNANCY', label: 'Thai kỳ' },
      { value: 'POSTPARTUM', label: 'Hậu sản' },
      { value: 'BABY_CARE', label: 'Chăm bé' },
    ]);
  });

  it('keeps stage labels synchronized with the ordered options', () => {
    expect(Object.keys(STAGE_LABELS)).toEqual(
      STAGE_OPTIONS.map(({ value }) => value),
    );
    expect(Object.values(STAGE_LABELS)).toContain('Chăm bé');
    expect(Object.values(STAGE_LABELS)).not.toContain('Sau sinh');
  });

  it('keeps the checklist support-function catalog stable and localized', () => {
    expect(CHECKLIST_SUPPORT_FUNCTION_OPTIONS).toEqual([
      { value: 'MATERNAL_HEALTH_METRICS', label: 'Đo chỉ số sức khỏe của mẹ' },
      { value: 'MATERNAL_EXERCISES', label: 'Bài tập cho mẹ' },
      { value: 'HEALTH_RECORDS', label: 'Hồ sơ sức khỏe' },
      { value: 'APPOINTMENTS', label: 'Lịch hẹn' },
      { value: 'REMINDERS', label: 'Nhắc nhở' },
      { value: 'JOURNEY', label: 'Hành trình' },
      { value: 'BABY_CARE', label: 'Chăm sóc em bé' },
      { value: 'EXPERT_CONSULTATION', label: 'Tư vấn chuyên gia' },
      { value: 'CONTENT_LIBRARY', label: 'Thư viện nội dung' },
      { value: 'AI_TRIAGE', label: 'Sàng lọc AI' },
    ]);
  });
});
