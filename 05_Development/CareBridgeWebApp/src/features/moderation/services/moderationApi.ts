import apiClient from '../../../shared/api/apiClient';
import type {
  ModerationQueuePage,
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
