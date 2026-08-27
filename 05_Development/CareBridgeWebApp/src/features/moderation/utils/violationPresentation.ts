import type { AccountViolationHistoryItem, ModerationActionType } from '../models/moderation';

export type ViolationStatus = 'ACTIVE' | 'EXPIRED' | 'INDEFINITE' | 'ESCALATED';

export const ACCOUNT_ACTION_META: Record<
  Extract<ModerationActionType, 'WARN' | 'SUSPEND' | 'RESTRICT' | 'ESCALATE'>,
  { icon: string; badgeClass: string; railClass: string }
> = {
  WARN: { icon: 'warning', badgeClass: 'bg-[#FFF4CE] text-[#775A00]', railClass: 'border-l-[#C99A00]' },
  SUSPEND: { icon: 'person_off', badgeClass: 'bg-error-container text-error', railClass: 'border-l-error' },
  RESTRICT: { icon: 'speaker_notes_off', badgeClass: 'bg-[#FCE8E6] text-[#A8332B]', railClass: 'border-l-[#C65D54]' },
  ESCALATE: { icon: 'upgrade', badgeClass: 'bg-secondary-container text-on-secondary-container', railClass: 'border-l-secondary' },
};

export function formatViolationDateTime(value: string): string {
  return new Date(value).toLocaleString('vi-VN', { dateStyle: 'short', timeStyle: 'short' });
}

export function getViolationStatus(item: AccountViolationHistoryItem): {
  label: string;
  value: ViolationStatus;
  muted: boolean;
} {
  if (item.actionType === 'ESCALATE') return { label: 'Đã chuyển cấp', value: 'ESCALATED', muted: false };
  if (!item.expiresAt) return { label: 'Không thời hạn', value: 'INDEFINITE', muted: false };
  return new Date(item.expiresAt).getTime() > Date.now()
    ? { label: `Hiệu lực đến ${formatViolationDateTime(item.expiresAt)}`, value: 'ACTIVE', muted: false }
    : { label: 'Đã hết hiệu lực', value: 'EXPIRED', muted: true };
}
