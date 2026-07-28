import apiClient from '../../../shared/api/apiClient';
import type {
  AiAssessment,
  AiFeedbackResult,
  AiFeedbackVerdict,
  CasePriority,
  ClaimReportResult,
  ModerationQueuePage,
  RelatedReportPage,
  AccountViolationHistoryItem,
  AccountViolationHistoryPage,
  AccountViolationSummaryItem,
  AccountViolationSummaryPage,
  ModerateContentResult,
  ModerationActionType,
  ModerationContentDetail,
  ModerationHistoryPage,
  PendingContentQueuePage,
  ReportTargetType,
  ReportSource,
  ReportStatus,
  ResolutionOutcome,
  ResolveReportResult,
  UndoModerationActionResult,
} from '../models/moderation';

function isAccountViolationSummaryItem(item: AccountViolationSummaryItem | AccountViolationHistoryItem): item is AccountViolationSummaryItem {
  return 'latestAction' in item;
}

function toAccountViolationSummaryPage(
  response: AccountViolationSummaryPage | AccountViolationHistoryPage,
): AccountViolationSummaryPage {
  if (response.content.every(isAccountViolationSummaryItem)) {
    return response as AccountViolationSummaryPage;
  }

  // During a rolling local restart, an older API can briefly return the previous flat action feed.
  // Group the loaded page so the moderator UI remains usable until the updated API is available.
  const grouped = new Map<string, AccountViolationSummaryItem>();
  for (const action of response.content as AccountViolationHistoryItem[]) {
    const existing = grouped.get(action.targetUserId);
    if (!existing) {
      grouped.set(action.targetUserId, {
        targetUserId: action.targetUserId,
        targetUserName: action.targetUserName,
        violationCount: 1,
        latestAction: action,
      });
    } else {
      existing.violationCount += 1;
    }
  }

  return {
    content: [...grouped.values()],
    totalElements: grouped.size,
    page: response.page,
    size: response.size,
  };
}

export async function fetchAccountViolationHistory(params: {
  page?: number;
  size?: number;
} = {}): Promise<AccountViolationSummaryPage> {
  const res = await apiClient.get<AccountViolationSummaryPage | AccountViolationHistoryPage>('/api/v1/admin/moderation/account-history', {
    params: { page: params.page ?? 0, size: params.size ?? 20 },
  });
  return toAccountViolationSummaryPage(res.data);
}

export async function fetchAccountViolationDetail(
  targetUserId: string,
  params: { page?: number; size?: number } = {},
): Promise<AccountViolationHistoryPage> {
  try {
    const res = await apiClient.get<AccountViolationHistoryPage>(
      `/api/v1/admin/moderation/account-history/${targetUserId}`,
      { params: { page: params.page ?? 0, size: params.size ?? 20 } },
    );
    return res.data;
  } catch (error) {
    const status = (error as { response?: { status?: number } })?.response?.status;
    if (status !== 404) throw error;

    // Keep local development usable while an older backend process is still serving the flat feed.
    const legacy = await apiClient.get<AccountViolationHistoryPage | AccountViolationSummaryPage>(
      '/api/v1/admin/moderation/account-history',
      { params: { page: 0, size: 50 } },
    );
    if (legacy.data.content.every(isAccountViolationSummaryItem)) throw error;

    const firstPage = legacy.data as AccountViolationHistoryPage;
    const pageCount = Math.ceil(firstPage.totalElements / 50);
    const remainingPages = pageCount > 1
      ? await Promise.all(Array.from({ length: pageCount - 1 }, (_, index) => apiClient.get<AccountViolationHistoryPage>(
          '/api/v1/admin/moderation/account-history',
          { params: { page: index + 1, size: 50 } },
        )))
      : [];
    const actions = [
      ...firstPage.content,
      ...remainingPages.flatMap((response) => response.data.content),
    ].filter((action) => action.targetUserId === targetUserId);

    return { content: actions, totalElements: actions.length, page: 0, size: actions.length || 20 };
  }
}

export async function fetchRelatedReports(reportId: string, params: {
  page?: number;
  size?: number;
} = {}): Promise<RelatedReportPage> {
  const res = await apiClient.get<RelatedReportPage>(`/api/v1/admin/moderation/reports/${reportId}/related`, {
    params: { page: params.page ?? 0, size: params.size ?? 20 },
  });
  return res.data;
}

// ModerationController returns raw DTOs (no ApiResponse envelope) — unlike content endpoints.
export async function fetchModerationQueue(params: {
  targetType?: ReportTargetType;
  status?: ReportStatus;
  source?: ReportSource;
  priority?: CasePriority;
  page?: number;
  size?: number;
}): Promise<ModerationQueuePage> {
  const res = await apiClient.get<ModerationQueuePage>('/api/v1/admin/moderation/queue', {
    params: {
      targetType: params.targetType,
      status: params.status,
      source: params.source,
      priority: params.priority,
      page: params.page ?? 0,
      size: params.size ?? 50,
    },
  });
  return res.data;
}

// ============ CB-MOD-IMP-016: claim workflow + AI assessment ============

// Atomic claim (PENDING -> IN_REVIEW). 409 MOD-036 when another moderator won the race.
export async function claimReport(reportId: string): Promise<ClaimReportResult> {
  const res = await apiClient.post<ClaimReportResult>(
    `/api/v1/admin/moderation/reports/${reportId}/claim`,
  );
  return res.data;
}

// Release (IN_REVIEW -> PENDING) — only the claiming moderator may release (409 MOD-037).
export async function releaseReport(reportId: string): Promise<ClaimReportResult> {
  const res = await apiClient.post<ClaimReportResult>(
    `/api/v1/admin/moderation/reports/${reportId}/release`,
  );
  return res.data;
}

// Latest AI assessment for a report (linked case first, then latest for the target).
// Returns null when no assessment exists (404 AIM-007) — a purely user-reported case.
export async function fetchAiAssessment(reportId: string): Promise<AiAssessment | null> {
  try {
    const res = await apiClient.get<AiAssessment>(
      `/api/v1/admin/moderation/reports/${reportId}/assessment`,
    );
    return res.data;
  } catch (err) {
    if ((err as { response?: { status?: number } })?.response?.status === 404) {
      return null;
    }
    throw err;
  }
}

// Agree/disagree with the AI assessment — stored for audit/precision only, never auto-applies.
export async function submitAiFeedback(
  assessmentId: string,
  verdict: AiFeedbackVerdict,
  note?: string,
): Promise<AiFeedbackResult> {
  const res = await apiClient.post<AiFeedbackResult>(
    `/api/v1/admin/moderation/assessments/${assessmentId}/feedback`,
    { verdict, note },
  );
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
