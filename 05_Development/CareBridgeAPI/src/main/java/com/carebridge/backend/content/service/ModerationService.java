package com.carebridge.backend.content.service;

import com.carebridge.backend.content.dto.request.ModerateContentRequest;
import com.carebridge.backend.content.dto.request.ModerationHistoryFilter;
import com.carebridge.backend.content.dto.request.ModerationQueueFilter;
import com.carebridge.backend.content.dto.request.PendingContentQueueFilter;
import com.carebridge.backend.content.dto.request.ResolveReportRequest;
import com.carebridge.backend.content.dto.request.WarnOrSuspendAccountRequest;
import com.carebridge.backend.content.dto.response.ModerateContentResponse;
import com.carebridge.backend.content.dto.response.AccountViolationHistoryResponse;
import com.carebridge.backend.content.dto.response.AccountViolationSummaryResponse;
import com.carebridge.backend.content.dto.response.CommunityContentMonitorResponse;
import com.carebridge.backend.content.dto.response.ModerationContentDetailResponse;
import com.carebridge.backend.content.dto.response.ModerationHistoryResponse;
import com.carebridge.backend.content.dto.response.ModerationQueueResponse;
import com.carebridge.backend.content.dto.response.PendingContentQueueResponse;
import com.carebridge.backend.content.dto.response.RelatedReportPageResponse;
import com.carebridge.backend.content.dto.response.ResolveReportResponse;
import com.carebridge.backend.content.dto.response.UndoModerationActionResponse;
import com.carebridge.backend.content.dto.response.WarnOrSuspendAccountResponse;
import com.carebridge.backend.content.entity.ReportTargetType;
import java.security.Principal;
import java.util.UUID;

public interface ModerationService {

    ModerationQueueResponse getModerationQueue(ModerationQueueFilter filter, Principal principal);

    /**
     * Lists CommunityQuestion or CommunityAnswer rows with status = PENDING, queried directly
     * (not via ContentReport — see CB-MOD-IMP-004 ADR-005). Complements getModerationQueue()
     * (UC-99, report-driven) by surfacing content that has never been reported.
     *
     * @throws com.carebridge.backend.content.exception.ModerationException (MOD-023) if
     *         filter.targetType() is not QUESTION or ANSWER
     */
    PendingContentQueueResponse getPendingContentQueue(PendingContentQueueFilter filter, Principal principal);

    /**
     * Lists the exact QUESTION or ANSWER records currently visible to community members
     * (moderation status APPROVED). This is independent from the pending queue and audit history.
     */
    CommunityContentMonitorResponse getVisibleCommunityContent(PendingContentQueueFilter filter, Principal principal);

    /**
     * Lists past APPROVE/HIDE/LOCK actions on QUESTION/ANSWER targets from audit_events
     * (CB-MOD-IMP-004 §16 ADR-007), read directly — no ACCOUNT actions (separate history view).
     */
    ModerationHistoryResponse getModerationHistory(ModerationHistoryFilter filter, Principal principal);

    /**
     * Lists append-only account enforcement actions only. This is deliberately separate from the
     * content-only moderation history so account identity projection remains moderator scoped.
     */
    AccountViolationSummaryResponse getAccountViolationHistory(int page, int size, Principal principal);

    AccountViolationHistoryResponse getAccountViolationHistory(UUID targetUserId, int page, int size, Principal principal);

    /** Lists privacy-safe reports that apply to the exact target of the selected report. */
    RelatedReportPageResponse getRelatedReports(UUID reportId, int page, int size, Principal principal);

    /**
     * Applies an APPROVE/HIDE/LOCK action directly to a community question or answer,
     * independent of any ContentReport (proactive moderation). Updates the target entity's
     * status synchronously and records an append-only ModerationAction with reportId = null (ADR-001).
     *
     * @throws com.carebridge.backend.content.exception.ModerationException (MOD-007) if targetId/targetType
     *         does not resolve to an existing row
     * @throws com.carebridge.backend.content.exception.ModerationException (MOD-008) if actionType is not
     *         supported for targetType (ADR-004 matrix)
     * @throws com.carebridge.backend.content.exception.ModerationException (MOD-009) if actionType is
     *         WARN/SUSPEND (belongs to UC-102) or targetType is CONTENT (ADR-004)
     * @throws com.carebridge.backend.content.exception.ModerationException (MOD-010) if reason is blank
     *         for HIDE/LOCK (ADR-006)
     */
    ModerateContentResponse moderateContent(ModerateContentRequest request, Principal principal);

    /**
     * Resolves a single PENDING ContentReport by choosing an outcome: DISMISS (no action,
     * report.status -> DISMISSED) or a content action APPROVE/HIDE/LOCK (delegates to the same
     * validation/mutation primitive as UC-100's moderateContent(), with reportId populated;
     * report.status -> RESOLVED). WARN/SUSPEND are accepted at the DTO contract level per FS but
     * rejected at v1 (ADR-005 — forward dependency on UC-102, not yet built).
     *
     * @throws com.carebridge.backend.content.exception.ModerationException (MOD-003) if reportId does
     *         not resolve to an existing ContentReport
     * @throws com.carebridge.backend.content.exception.ModerationException (MOD-011) if report.status
     *         is not PENDING (already resolved/dismissed, ADR-006)
     * @throws com.carebridge.backend.content.exception.ModerationException (MOD-012) if
     *         report.targetType == CONTENT and outcome != DISMISS (ADR-004)
     * @throws com.carebridge.backend.content.exception.ModerationException (MOD-013) if outcome is
     *         WARN or SUSPEND (ADR-005)
     * @throws com.carebridge.backend.content.exception.ModerationException (MOD-008) if outcome is a
     *         content action not supported for report.targetType (reused UC-100 §6.4 matrix)
     * @throws com.carebridge.backend.content.exception.ModerationException (MOD-007) if
     *         report.targetId does not resolve to an existing CommunityQuestion/CommunityAnswer row
     * @throws com.carebridge.backend.content.exception.ModerationException (MOD-010) if reason is
     *         blank for HIDE/LOCK
     */
    ResolveReportResponse resolveReport(UUID reportId, ResolveReportRequest request, Principal principal);

    /**
     * Applies a WARN or SUSPEND action to a user account.
     * WARN is audit-only and never mutates the User entity (ADR-004).
     * SUSPEND requires a future expiresAt and sets User.suspendedUntil to that value (ADR-001, ADR-008),
     * which JwtAuthenticationFilter / AuthenticationPolicy enforce on every subsequent login/request (ADR-003).
     * Records an append-only ModerationAction with reportId = null, targetType = ACCOUNT (ADR-006).
     *
     * @throws com.carebridge.backend.content.exception.ModerationException (MOD-015) if targetUserId does not
     *         resolve to an existing users row
     * @throws com.carebridge.backend.content.exception.ModerationException (MOD-016) if actionType is not
     *         WARN or SUSPEND (APPROVE/HIDE/LOCK belong to UC-100)
     * @throws com.carebridge.backend.content.exception.ModerationException (MOD-017) if reason is blank for
     *         WARN or SUSPEND (ADR-005)
     * @throws com.carebridge.backend.content.exception.ModerationException (MOD-018) if actionType=SUSPEND
     *         and expiresAt is missing or not strictly in the future (ADR-008)
     * @throws com.carebridge.backend.content.exception.ModerationException (MOD-019) if actionType=WARN and
     *         expiresAt is non-null (ambiguous request)
     * @throws com.carebridge.backend.content.exception.ModerationException (MOD-020) if targetUserId equals
     *         the acting moderator's own user id (ADR-007, Accepted)
     */
    WarnOrSuspendAccountResponse moderateAccount(WarnOrSuspendAccountRequest request, Principal principal);

    /**
     * CB-MOD-IMP-008: returns the full (non-truncated) body of a CommunityQuestion/CommunityAnswer,
     * regardless of its current status — reads directly via repository.findById(), does NOT go through
     * CommunityQuestionService.getQuestionDetail() (which filters by status, ADR-001). authorId/authorName
     * are returned even when the question was posted anonymously (ADR-003 — moderator accountability view).
     *
     * @throws com.carebridge.backend.content.exception.ModerationException (MOD-023) if targetType is not
     *         QUESTION or ANSWER
     * @throws com.carebridge.backend.content.exception.ModerationException (MOD-007) if targetId does not
     *         resolve to an existing row
     */
    ModerationContentDetailResponse getContentDetail(ReportTargetType targetType, UUID targetId, Principal principal);

    /**
     * CB-MOD-IMP-009: undoes a single direct (reportId == null) APPROVE/HIDE/LOCK action on a
     * QUESTION/ANSWER, setting the target's status back to PENDING (ADR-001 — never attempts to
     * reconstruct a prior status). Only allowed when actionId is the most recent action for its
     * target (ADR-002 guard 1) AND the target's current status still matches what that action
     * produced (ADR-002 guard 2). Records a new append-only ModerationAction (actionType=UNDO) —
     * the original action row is never mutated (ADR-005).
     *
     * @throws com.carebridge.backend.content.exception.ModerationException (MOD-025) if actionId does not exist
     * @throws com.carebridge.backend.content.exception.ModerationException (MOD-026) if the action's targetType
     *         is not QUESTION or ANSWER
     * @throws com.carebridge.backend.content.exception.ModerationException (MOD-027) if the action's reportId
     *         is not null (originated from resolveReport(), out of scope — ADR-004)
     * @throws com.carebridge.backend.content.exception.ModerationException (MOD-028) if the action's actionType
     *         is not one of APPROVE/HIDE/LOCK
     * @throws com.carebridge.backend.content.exception.ModerationException (MOD-029) if actionId is not the
     *         most recent action for its target
     * @throws com.carebridge.backend.content.exception.ModerationException (MOD-030) if the target's current
     *         status no longer matches what this action produced
     */
    UndoModerationActionResponse undoModerationAction(UUID actionId, Principal principal);

    /**
     * CB-MOD-IMP-016: atomically claims a PENDING report (status -> IN_REVIEW,
     * assigned_moderator_id + claimed_at set). Exactly one moderator can win the claim —
     * concurrency is resolved by a status-guarded UPDATE, not application locking.
     *
     * @throws com.carebridge.backend.content.exception.ModerationException (MOD-003) if reportId does not exist
     * @throws com.carebridge.backend.content.exception.ModerationException (MOD-036) if the report is not
     *         PENDING anymore (already claimed, resolved or dismissed)
     */
    com.carebridge.backend.content.dto.response.ClaimReportResponse claimReport(UUID reportId, Principal principal);

    /**
     * CB-MOD-IMP-016: releases an IN_REVIEW report back to PENDING. Only the claiming
     * moderator may release.
     *
     * @throws com.carebridge.backend.content.exception.ModerationException (MOD-003) if reportId does not exist
     * @throws com.carebridge.backend.content.exception.ModerationException (MOD-037) if the report is not
     *         IN_REVIEW or the caller is not its claimer
     */
    com.carebridge.backend.content.dto.response.ClaimReportResponse releaseReport(UUID reportId, Principal principal);
}
