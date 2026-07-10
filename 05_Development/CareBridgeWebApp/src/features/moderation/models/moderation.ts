export type ReportTargetType = 'QUESTION' | 'ANSWER' | 'CONTENT' | 'ACCOUNT' | 'EXPERT' | 'USER';
export type ReportStatus = 'PENDING' | 'RESOLVED' | 'DISMISSED';
export type ResolutionOutcome = 'DISMISS' | 'APPROVE' | 'HIDE' | 'LOCK' | 'REQUEST_REVISION' | 'WARN' | 'SUSPEND' | 'RESTRICT';
export type ModerationActionType = 'APPROVE' | 'HIDE' | 'LOCK' | 'REQUEST_REVISION' | 'WARN' | 'SUSPEND' | 'RESTRICT';

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
}

export interface ModerationQueuePage {
  content: ModerationQueueItem[];
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
};

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

export function canHideTarget(targetType: ReportTargetType): boolean {
  return targetType === 'QUESTION' || targetType === 'ANSWER';
}

export function canEnforceAccount(targetType: ReportTargetType): boolean {
  return targetType === 'QUESTION' || targetType === 'ANSWER' || targetType === 'USER' || targetType === 'EXPERT' || targetType === 'ACCOUNT';
}
