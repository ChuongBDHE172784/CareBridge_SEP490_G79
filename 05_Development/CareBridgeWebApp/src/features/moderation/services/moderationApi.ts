import apiClient from '../../../shared/api/apiClient';
import type {
  ModerationQueuePage,
  ModerateContentResult,
  ModerationActionType,
  ModerationContentDetail,
  ModerationHistoryPage,
  PendingContentQueuePage,
  ReportTargetType,
  ReportStatus,
  ResolutionOutcome,
  ResolveReportResult,
  UndoModerationActionResult,
} from '../models/moderation';

// ModerationController returns raw DTOs (no ApiResponse envelope) — unlike content endpoints.
export async function fetchModerationQueue(params: {
  targetType?: ReportTargetType;
  status?: ReportStatus;
  page?: number;
  size?: number;
}): Promise<ModerationQueuePage> {
  const res = await apiClient.get<ModerationQueuePage>('/api/v1/admin/moderation/queue', {
    params: {
      targetType: params.targetType,
      status: params.status,
      page: params.page ?? 0,
      size: params.size ?? 50,
    },
  });
  return res.data;
}

// CB-MOD-IMP-004: content never reported, queried directly by status=PENDING (ADR-005/ADR-006)
export async function fetchPendingContentQueue(params: {
  targetType: ReportTargetType;
  page?: number;
  size?: number;
}): Promise<PendingContentQueuePage> {
  const res = await apiClient.get<PendingContentQueuePage>('/api/v1/admin/moderation/pending-content', {
    params: {
      targetType: params.targetType,
      page: params.page ?? 0,
      size: params.size ?? 50,
    },
  });
  return res.data;
}

// UC-100's direct action endpoint — approves/hides/locks content independent of any ContentReport.
// Previously only resolveReport() (report-scoped) was wired on the frontend; this closes that gap.
export async function moderateContentDirect(
  targetId: string,
  targetType: ReportTargetType,
  actionType: ModerationActionType,
  reason?: string,
): Promise<ModerateContentResult> {
  const res = await apiClient.post<ModerateContentResult>('/api/v1/admin/moderation/actions', {
    targetId,
    targetType,
    actionType,
    reason,
  });
  return res.data;
}

// CB-MOD-IMP-004 §16: past APPROVE/HIDE/LOCK actions on QUESTION/ANSWER, read from moderation_actions
export async function fetchModerationHistory(params: {
  targetType?: ReportTargetType;
  page?: number;
  size?: number;
} = {}): Promise<ModerationHistoryPage> {
  const res = await apiClient.get<ModerationHistoryPage>('/api/v1/admin/moderation/history', {
    params: {
      targetType: params.targetType,
      page: params.page ?? 0,
      size: params.size ?? 50,
    },
  });
  return res.data;
}

export async function resolveReport(
  reportId: string,
  outcome: ResolutionOutcome,
  reason?: string,
  expiresAt?: string,
): Promise<ResolveReportResult> {
  const res = await apiClient.post<ResolveReportResult>(
    `/api/v1/admin/moderation/reports/${reportId}/resolve`,
    { outcome, reason, expiresAt },
  );
  return res.data;
}

// CB-MOD-IMP-008: full (non-truncated) body — distinct from the *.contentPreview fields elsewhere
// in this file, which are always capped at 200 chars server-side.
export async function fetchContentDetail(
  targetType: ReportTargetType,
  targetId: string,
): Promise<ModerationContentDetail> {
  const res = await apiClient.get<ModerationContentDetail>(
    `/api/v1/admin/moderation/content/${targetType}/${targetId}`,
  );
  return res.data;
}

// CB-MOD-IMP-009: reverts a direct APPROVE/HIDE/LOCK action back to PENDING. Backend enforces the
// "most recent action" + "status still matches" guards (409 MOD-029/MOD-030 otherwise).
export async function undoModerationAction(actionId: string): Promise<UndoModerationActionResult> {
  const res = await apiClient.post<UndoModerationActionResult>(
    `/api/v1/admin/moderation/actions/${actionId}/undo`,
  );
  return res.data;
}

export async function moderateAccount(
  targetUserId: string,
  actionType: Extract<ModerationActionType, 'WARN' | 'SUSPEND' | 'RESTRICT'>,
  reason: string,
  expiresAt?: string,
) {
  const res = await apiClient.post('/api/v1/admin/moderation/account-actions', {
    targetUserId,
    actionType,
    reason,
    expiresAt,
  });
  return res.data;
}
