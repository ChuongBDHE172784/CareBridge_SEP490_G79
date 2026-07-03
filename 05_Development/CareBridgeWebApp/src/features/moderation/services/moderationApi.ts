import apiClient from '../../../shared/api/apiClient';
import type {
  ModerationQueuePage,
  ModerateContentResult,
  ModerationActionType,
  PendingContentQueuePage,
  ReportTargetType,
  ReportStatus,
  ResolutionOutcome,
  ResolveReportResult,
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

export async function resolveReport(
  reportId: string,
  outcome: ResolutionOutcome,
  reason?: string,
): Promise<ResolveReportResult> {
  const res = await apiClient.post<ResolveReportResult>(
    `/api/v1/admin/moderation/reports/${reportId}/resolve`,
    { outcome, reason },
  );
  return res.data;
}
