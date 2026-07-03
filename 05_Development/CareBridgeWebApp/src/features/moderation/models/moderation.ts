export type ReportTargetType = 'QUESTION' | 'ANSWER' | 'CONTENT' | 'ACCOUNT';
export type ReportStatus = 'PENDING' | 'RESOLVED' | 'DISMISSED';
export type ResolutionOutcome = 'DISMISS' | 'APPROVE' | 'HIDE' | 'LOCK' | 'WARN' | 'SUSPEND';
export type ModerationActionType = 'APPROVE' | 'HIDE' | 'LOCK' | 'WARN' | 'SUSPEND';

export interface ModerationQueueItem {
  id: string;
  targetType: ReportTargetType;
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

export interface ResolveReportResult {
  reportId: string;
  reportStatus: ReportStatus;
  resolvedByModeratorId: string;
  resolvedAt: string;
  actionId: string | null;
  actionType: ModerationActionType | null;
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
};

export const REPORT_STATUS_LABELS: Record<ReportStatus, string> = {
  PENDING: 'Đang chờ xử lý',
  RESOLVED: 'Đã xử lý',
  DISMISSED: 'Đã bỏ qua',
};

// Only QUESTION/ANSWER targets accept HIDE via resolveReport — CONTENT/ACCOUNT reports
// throw contentActionNotSupportedForReport / accountActionNotAvailable on the backend.
export function canHideTarget(targetType: ReportTargetType): boolean {
  return targetType === 'QUESTION' || targetType === 'ANSWER';
}
