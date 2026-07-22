export type ReportTargetType = 'QUESTION' | 'ANSWER' | 'CONTENT' | 'ACCOUNT' | 'EXPERT' | 'USER';
export type ReportStatus = 'PENDING' | 'RESOLVED' | 'DISMISSED';
export type ReportCategory = 'INACCURATE_INFORMATION' | 'DISGUISED_ADVERTISING' | 'HARASSMENT' | 'UNSAFE_ADVICE' | 'SPAM' | 'OTHER';
export type ResolutionOutcome = 'DISMISS' | 'APPROVE' | 'HIDE' | 'LOCK' | 'REQUEST_REVISION' | 'WARN' | 'SUSPEND' | 'RESTRICT';
export type ModerationActionType = 'APPROVE' | 'HIDE' | 'LOCK' | 'REQUEST_REVISION' | 'WARN' | 'SUSPEND' | 'RESTRICT' | 'ESCALATE' | 'UNDO';

// actionType values a moderator can undo from the "Đã xử lý" tab — REQUEST_REVISION/WARN/SUSPEND/
// RESTRICT/UNDO are excluded server-side too (CB-MOD-IMP-009 ADR-001/ADR-004, MOD-028/MOD-026).
export const UNDOABLE_ACTION_TYPES: ReadonlySet<ModerationActionType> = new Set(['APPROVE', 'HIDE', 'LOCK']);

// resolvedAt/assignedModeratorId/revertedAt/revertedBy are null for a PENDING report — their
// presence is what the detail pages (ContentReportDetailPage/AccountReportDetailPage) use to
// switch between the actionable view and the read-only "already processed" view (CB-MOD-IMP-015).
export interface ModerationQueueItem {
  id: string;
  targetId: string | null;
  targetType: ReportTargetType;
  reporterUserId: string | null;
  contentPreview: string;
  reportCount: number;
  reportedAt: string;
  reportReason: string;
  status: ReportStatus;
  resolvedAt: string | null;
  assignedModeratorId: string | null;
  revertedAt: string | null;
  revertedBy: string | null;
}

export interface ModerationQueuePage {
  content: ModerationQueueItem[];
  totalElements: number;
  page: number;
  size: number;
}

export interface RelatedReportItem {
  id: string;
  category: string | null;
  reason: string | null;
  status: ReportStatus;
  reportSource: 'USER' | 'AUTOMATED';
  reportedAt: string;
}

export interface RelatedReportPage {
  content: RelatedReportItem[];
  totalElements: number;
  page: number;
  size: number;
}

// CB-MOD-IMP-004: content that has never been reported, queried directly by status=PENDING —
// distinct from ModerationQueueItem, which is report-driven and carries reportCount/reportReason
// that do not apply here.
export interface PendingContentItem {
  targetId: string;
  targetType: ReportTargetType;
  contentPreview: string;
  createdAt: string;
}

export interface PendingContentQueuePage {
  content: PendingContentItem[];
  totalElements: number;
  page: number;
  size: number;
}

// CB-MOD-IMP-004 §16: past APPROVE/HIDE/LOCK/REQUEST_REVISION actions on QUESTION/ANSWER, read from moderation_actions
export interface ModerationHistoryItem {
  actionId: string;
  targetId: string;
  targetType: ReportTargetType;
  actionType: ModerationActionType;
  contentPreview: string | null;
  moderatorName: string | null;
  reason: string | null;
  actionAt: string;
}

export interface ModerationHistoryPage {
  content: ModerationHistoryItem[];
  totalElements: number;
  page: number;
  size: number;
}

export const ACTION_TYPE_LABELS: Record<ModerationActionType, string> = {
  APPROVE: 'Duyệt',
  HIDE: 'Ẩn',
  LOCK: 'Khoá',
  REQUEST_REVISION: 'Yêu cầu sửa',
  WARN: 'Cảnh cáo',
  SUSPEND: 'Đình chỉ',
  RESTRICT: 'Hạn chế đăng',
  ESCALATE: 'Chuyển cấp xử lý',
  UNDO: 'Hoàn tác',
};

export interface AccountViolationHistoryItem {
  actionId: string;
  targetUserId: string;
  targetUserName: string;
  moderatorUserId: string;
  moderatorName: string;
  actionType: Extract<ModerationActionType, 'WARN' | 'SUSPEND' | 'RESTRICT' | 'ESCALATE'>;
  reason: string;
  expiresAt: string | null;
  reportId: string | null;
  actionAt: string;
}

export interface AccountViolationHistoryPage {
  content: AccountViolationHistoryItem[];
  totalElements: number;
  page: number;
  size: number;
}

export interface ResolveReportResult {
  reportId: string;
  reportStatus: ReportStatus;
  resolvedByModeratorId: string;
  resolvedAt: string;
  actionId: string | null;
  actionType: ModerationActionType | null;
  resultingStatus: string | null;
}

// CB-MOD-IMP-008: GET /content/{targetType}/{targetId} response — full body, not truncated like
// contentPreview elsewhere in this file (ContentPreviewService caps that at 200 chars).
export interface ModerationContentDetail {
  targetId: string;
  targetType: ReportTargetType;
  authorId: string | null;
  authorName: string | null;
  title: string | null;
  body: string;
  status: string;
  anonymous: boolean;
  questionId: string | null;
  questionTitle: string | null;
  createdAt: string;
  updatedAt: string | null;
}

// CB-MOD-IMP-009: POST /actions/{actionId}/undo response — resultingStatus is always "PENDING".
export interface UndoModerationActionResult {
  undoActionId: string;
  originalActionId: string;
  targetId: string;
  targetType: ReportTargetType;
  moderatorUserId: string;
  actionAt: string;
  resultingStatus: string;
}

// CB-MOD-IMP-015: POST /reports/{reportId}/revert response — reportStatus is always "PENDING" on
// success. undoActionId/resultingStatus are null when the reverted report was DISMISS (no
// ModerationAction ever existed for that outcome).
export interface RevertReportResult {
  reportId: string;
  reportStatus: ReportStatus;
  revertedByModeratorId: string;
  revertedAt: string;
  undoActionId: string | null;
  targetType: ReportTargetType | null;
  targetId: string | null;
  resultingStatus: string | null;
}

// UC-100's POST /actions response — used directly by the Pending Content queue (no ContentReport)
export interface ModerateContentResult {
  actionId: string;
  targetId: string;
  targetType: ReportTargetType;
  actionType: ModerationActionType;
  moderatorUserId: string;
  reason: string | null;
  actionAt: string;
  resultingStatus: string;
}

export const TARGET_TYPE_LABELS: Record<ReportTargetType, string> = {
  QUESTION: 'Câu hỏi cộng đồng',
  ANSWER: 'Câu trả lời cộng đồng',
  CONTENT: 'Nội dung thư viện',
  ACCOUNT: 'Tài khoản',
  EXPERT: 'Chuyên gia',
  USER: 'Người dùng',
};

export const REPORT_STATUS_LABELS: Record<ReportStatus, string> = {
  PENDING: 'Đang chờ xử lý',
  RESOLVED: 'Đã xử lý',
  DISMISSED: 'Đã bỏ qua',
};

export const REPORT_CATEGORY_LABELS: Record<ReportCategory, string> = {
  INACCURATE_INFORMATION: 'Thông tin không chính xác',
  DISGUISED_ADVERTISING: 'Quảng cáo trá hình',
  HARASSMENT: 'Quấy rối hoặc bắt nạt',
  UNSAFE_ADVICE: 'Lời khuyên không an toàn',
  SPAM: 'Nội dung rác',
  OTHER: 'Lý do khác',
};

export function formatReportReason(reason: string): string {
  return REPORT_CATEGORY_LABELS[reason as ReportCategory] ?? reason;
}

export function canHideTarget(targetType: ReportTargetType): boolean {
  return targetType === 'QUESTION' || targetType === 'ANSWER';
}

export function canEnforceAccount(targetType: ReportTargetType): boolean {
  return targetType === 'QUESTION' || targetType === 'ANSWER' || targetType === 'USER' || targetType === 'EXPERT' || targetType === 'ACCOUNT';
}
