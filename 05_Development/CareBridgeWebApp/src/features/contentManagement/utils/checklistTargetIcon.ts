/**
 * Chọn icon thể hiện checklist này dành cho mẹ hay cho em bé.
 *
 * Trước đây hàm này nằm trong cả ChecklistListPage và ChecklistDetailPage với nội
 * dung giống hệt nhau, nên hai trang có thể lệch nhau khi chỉ một bên được sửa.
 * Bản trong ChecklistListPage còn được export ra ngoài, khiến file đó vừa xuất
 * component vừa xuất hàm thường và làm hỏng Fast Refresh của Vite.
 */
export function getChecklistTargetIcon(checklist: {
  name?: string;
  stage?: string | null;
  items?: Array<{ targetSubject?: 'MOTHER' | 'BABY' }>;
}): 'child_care' | 'pregnant_woman' {
  const hasBabyItem = checklist.items?.some((i) => i.targetSubject === 'BABY');
  const isBabyStage = checklist.stage === 'BABY_CARE';
  const nameLower = (checklist.name || '').toLowerCase();
  const isBabyName = nameLower.includes('bé') || nameLower.includes('trẻ') || nameLower.includes('sơ sinh');

  if (hasBabyItem || isBabyStage || isBabyName) {
    return 'child_care';
  }
  return 'pregnant_woman';
}
