package com.carebridge.backend.content.service;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.community.entity.AnswerStatus;
import com.carebridge.backend.community.entity.CommunityAnswer;
import com.carebridge.backend.community.entity.CommunityQuestion;
import com.carebridge.backend.community.entity.QuestionStatus;
import com.carebridge.backend.community.repository.CommunityAnswerRepository;
import com.carebridge.backend.community.repository.CommunityQuestionRepository;
import com.carebridge.backend.content.dto.request.ModerateContentRequest;
import com.carebridge.backend.content.dto.request.ModerationQueueFilter;
import com.carebridge.backend.content.dto.request.ResolutionOutcome;
import com.carebridge.backend.content.dto.request.ResolveReportRequest;
import com.carebridge.backend.content.dto.request.WarnOrSuspendAccountRequest;
import com.carebridge.backend.content.dto.response.ModerateContentResponse;
import com.carebridge.backend.content.dto.response.ModerationQueueItemResponse;
import com.carebridge.backend.content.dto.response.ModerationQueueResponse;
import com.carebridge.backend.content.dto.response.ResolveReportResponse;
import com.carebridge.backend.content.dto.response.WarnOrSuspendAccountResponse;
import com.carebridge.backend.content.entity.ContentReport;
import com.carebridge.backend.content.entity.ModerationAction;
import com.carebridge.backend.content.entity.ModerationActionType;
import com.carebridge.backend.content.entity.ReportStatus;
import com.carebridge.backend.content.entity.ReportTargetType;
import com.carebridge.backend.content.exception.ModerationException;
import com.carebridge.backend.content.mapper.ModerationMapper;
import com.carebridge.backend.content.repository.ContentReportRepository;
import com.carebridge.backend.content.repository.ModerationActionRepository;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.repository.UserRepository;
import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ModerationServiceImpl implements ModerationService {

    private final ContentReportRepository contentReportRepository;
    private final ContentPreviewService contentPreviewService;
    private final ModerationMapper moderationMapper;
    private final AuditService auditService;
    private final CommunityQuestionRepository communityQuestionRepository;
    private final CommunityAnswerRepository communityAnswerRepository;
    private final ModerationActionRepository moderationActionRepository;
    private final UserRepository userRepository;

    @Override
    public ModerationQueueResponse getModerationQueue(ModerationQueueFilter filter, Principal principal) {
        // C5: Always sort by createdAt DESC (BR-MOD-003)
        PageRequest pageable = PageRequest.of(
                filter.page(),
                filter.size(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        // ADR-001: query ContentReport first, then fetch previews
        Page<ContentReport> page;
        if (filter.targetType() != null) {
            page = contentReportRepository.findByStatusAndTargetType(
                    filter.status(), filter.targetType(), pageable);
        } else {
            page = contentReportRepository.findByStatus(filter.status(), pageable);
        }

        List<ModerationQueueItemResponse> items = page.getContent().stream()
                .map(report -> {
                    // C4: preview truncated inside ContentPreviewService
                    String preview = contentPreviewService.fetchPreview(
                            report.getTargetId(), report.getTargetType());
                    long count = contentReportRepository.countByTargetIdAndStatus(
                            report.getTargetId(), report.getStatus());
                    return moderationMapper.toQueueItemResponse(report, preview, count);
                })
                .toList();

        // C2: AuditService.log() after every successful queue view (ADR-003)
        String userId = principal != null ? principal.getName() : null;
        auditService.log(AuditAction.MODERATION_QUEUE_VIEWED, userId, null,
                "filter=" + filter.targetType() + "/" + filter.status() + " count=" + page.getTotalElements());

        return moderationMapper.toQueueResponse(page, items);
    }

    // WARN/SUSPEND belong to UC-102 (account moderation), not this content-status endpoint (ADR-004)
    private static final Set<ModerationActionType> OUT_OF_SCOPE_ACTION_TYPES =
            Set.of(ModerationActionType.WARN, ModerationActionType.SUSPEND);

    // C6: reason required for HIDE/LOCK, optional for APPROVE (ADR-006)
    private static final Set<ModerationActionType> REASON_REQUIRED_ACTION_TYPES =
            Set.of(ModerationActionType.HIDE, ModerationActionType.LOCK);

    @Override
    @Transactional
    public ModerateContentResponse moderateContent(ModerateContentRequest request, Principal principal) {
        UUID moderatorUserId = SecurityUtils.requireCurrentUserId(principal);

        // C4: WARN/SUSPEND and targetType=CONTENT are out of scope for this endpoint (ADR-004)
        if (OUT_OF_SCOPE_ACTION_TYPES.contains(request.actionType())
                || request.targetType() == ReportTargetType.CONTENT) {
            throw ModerationException.unsupportedActionType(request.actionType());
        }

        // ADR-001 (UC-101 CB-MOD-IMP-003): reportId=null distinguishes proactive UC-100 actions
        ModerateContentResponse response = applyContentAction(request.targetId(), request.targetType(),
                request.actionType(), request.reason(), moderatorUserId, null);

        // C7: audit log after successful transaction (ADR-003) — caller-owned, not inside the
        // shared primitive (UC-101 RES-TC-117 — avoids double-audit-logging when reused by resolveReport())
        auditService.log(AuditAction.MODERATION_ACTION, moderatorUserId, request.targetType().name(),
                request.targetId().toString(),
                "actionType=" + request.actionType() + " reason=" + request.reason());

        return response;
    }

    /**
     * Shared primitive (UC-101 ADR-001): validates reason requirement + action-targetType
     * compatibility (TDS §6.4), mutates the target entity's status, and records an append-only
     * ModerationAction with the given reportId (null for UC-100 proactive actions, set for UC-101
     * report resolution). Does NOT call AuditService — that responsibility belongs to the caller
     * (moderateContent() for UC-100, resolveReport() for UC-101) to avoid double-audit-logging.
     */
    private ModerateContentResponse applyContentAction(UUID targetId, ReportTargetType targetType,
            ModerationActionType actionType, String reason, UUID moderatorUserId, UUID reportId) {
        // C6: reason required for HIDE/LOCK (ADR-006)
        if (REASON_REQUIRED_ACTION_TYPES.contains(actionType) && (reason == null || reason.isBlank())) {
            throw ModerationException.reasonRequired(actionType);
        }

        String resultingStatus = switch (targetType) {
            case QUESTION -> moderateQuestion(targetId, actionType);
            case ANSWER -> moderateAnswer(targetId, actionType);
            case CONTENT -> throw ModerationException.unsupportedActionType(actionType);
            // ACCOUNT targets (UC-102) never reach this content-only primitive — moderateAccount()
            // has its own dedicated write path.
            case ACCOUNT -> throw ModerationException.unsupportedActionType(actionType);
        };

        // C2/C3: append-only ModerationAction, reportId propagated by caller (BR-MOD-004/BR-MOD-011)
        ModerationAction action = ModerationAction.builder()
                .reportId(reportId)
                .targetId(targetId)
                .targetType(targetType)
                .actionType(actionType)
                .moderatorUserId(moderatorUserId)
                .reason(reason)
                .actionAt(Instant.now())
                .build();
        ModerationAction savedAction = moderationActionRepository.save(action);

        return new ModerateContentResponse(
                savedAction.getId(),
                targetId,
                targetType,
                actionType,
                moderatorUserId,
                reason,
                savedAction.getActionAt(),
                resultingStatus);
    }

    // C5: action-targetType compatibility per TDS §6.4 — QUESTION supports APPROVE/HIDE/LOCK
    private String moderateQuestion(UUID targetId, ModerationActionType actionType) {
        CommunityQuestion question = communityQuestionRepository.findById(targetId)
                .orElseThrow(() -> ModerationException.targetNotFound(targetId, ReportTargetType.QUESTION));

        QuestionStatus newStatus = switch (actionType) {
            case APPROVE -> QuestionStatus.APPROVED;
            case HIDE -> QuestionStatus.HIDDEN;
            case LOCK -> QuestionStatus.LOCKED;
            default -> throw ModerationException.actionNotSupportedForTargetType(
                    actionType, ReportTargetType.QUESTION);
        };

        question.setStatus(newStatus);
        communityQuestionRepository.save(question);
        return newStatus.name();
    }

    // C5: action-targetType compatibility per TDS §6.4 — ANSWER supports APPROVE/HIDE only (no LOCKED)
    private String moderateAnswer(UUID targetId, ModerationActionType actionType) {
        if (actionType == ModerationActionType.LOCK) {
            throw ModerationException.actionNotSupportedForTargetType(actionType, ReportTargetType.ANSWER);
        }

        CommunityAnswer answer = communityAnswerRepository.findById(targetId)
                .orElseThrow(() -> ModerationException.targetNotFound(targetId, ReportTargetType.ANSWER));

        AnswerStatus newStatus = switch (actionType) {
            case APPROVE -> AnswerStatus.APPROVED;
            case HIDE -> AnswerStatus.HIDDEN;
            default -> throw ModerationException.actionNotSupportedForTargetType(actionType, ReportTargetType.ANSWER);
        };

        answer.setStatus(newStatus);
        communityAnswerRepository.save(answer);
        return newStatus.name();
    }

    // WARN/SUSPEND -> account-target actions rejected v1 (ADR-005 of UC-101, forward dependency on UC-102)
    private static final Set<ResolutionOutcome> ACCOUNT_ACTION_OUTCOMES =
            Set.of(ResolutionOutcome.WARN, ResolutionOutcome.SUSPEND);

    @Override
    @Transactional
    public ResolveReportResponse resolveReport(UUID reportId, ResolveReportRequest request, Principal principal) {
        UUID moderatorUserId = SecurityUtils.requireCurrentUserId(principal);

        ContentReport report = contentReportRepository.findById(reportId)
                .orElseThrow(() -> ModerationException.reportNotFound(reportId));

        // ADR-006: PENDING-only transition guard — re-resolution rejected
        if (report.getStatus() != ReportStatus.PENDING) {
            throw ModerationException.reportAlreadyResolved(reportId);
        }

        // ADR-005: WARN/SUSPEND are account-target actions — forward dependency on UC-102, report untouched
        if (ACCOUNT_ACTION_OUTCOMES.contains(request.outcome())) {
            throw ModerationException.accountActionNotAvailable(mapToActionType(request.outcome()));
        }

        UUID actionId = null;
        ModerationActionType actionType = null;
        String resultingStatus = null;

        if (request.outcome() == ResolutionOutcome.DISMISS) {
            // BR-MOD-010: DISMISS creates no ModerationAction
            report.setStatus(ReportStatus.DISMISSED);
        } else {
            // ADR-004: report.targetType == CONTENT only accepts DISMISS via this endpoint
            if (report.getTargetType() == ReportTargetType.CONTENT) {
                throw ModerationException.contentActionNotSupportedForReport();
            }

            actionType = mapToActionType(request.outcome());
            ModerateContentResponse actionResponse = applyContentAction(report.getTargetId(), report.getTargetType(),
                    actionType, request.reason(), moderatorUserId, report.getId());
            actionId = actionResponse.actionId();
            resultingStatus = actionResponse.resultingStatus();
            report.setStatus(ReportStatus.RESOLVED);
        }

        // BR-MOD-009: resolvedAt/assignedModeratorId set for both branches
        report.setResolvedAt(Instant.now());
        report.setAssignedModeratorId(moderatorUserId);
        ContentReport savedReport = contentReportRepository.save(report);

        // ADR-003: audit log for EVERY successful outcome, including DISMISS — caller-owned (not
        // inside applyContentAction()) to avoid double-logging (RES-TC-117)
        auditService.log(AuditAction.MODERATION_ACTION, moderatorUserId, report.getTargetType().name(),
                report.getTargetId().toString(),
                "reportId=" + reportId + " outcome=" + request.outcome() + " reason=" + request.reason());

        return new ResolveReportResponse(
                savedReport.getId(),
                savedReport.getStatus(),
                moderatorUserId,
                savedReport.getResolvedAt(),
                actionId,
                actionType,
                resultingStatus);
    }

    // UC-102: only WARN/SUSPEND are valid at this endpoint (APPROVE/HIDE/LOCK belong to UC-100)
    private static final Set<ModerationActionType> ACCOUNT_ACTION_TYPES =
            Set.of(ModerationActionType.WARN, ModerationActionType.SUSPEND);

    @Override
    @Transactional
    public WarnOrSuspendAccountResponse moderateAccount(WarnOrSuspendAccountRequest request, Principal principal) {
        UUID moderatorUserId = SecurityUtils.requireCurrentUserId(principal);

        // ADR-007: self-action guard, checked before any other validation/lookup
        if (request.targetUserId().equals(moderatorUserId)) {
            throw ModerationException.selfActionForbidden();
        }

        // MOD-016: this endpoint only accepts WARN/SUSPEND
        if (!ACCOUNT_ACTION_TYPES.contains(request.actionType())) {
            throw ModerationException.accountActionTypeNotSupported(request.actionType());
        }

        // ADR-005: reason required (non-blank) for both WARN and SUSPEND
        if (request.reason() == null || request.reason().isBlank()) {
            throw ModerationException.accountReasonRequired(request.actionType());
        }

        // ADR-008: SUSPEND requires a strictly-future expiresAt; WARN must not carry one
        if (request.actionType() == ModerationActionType.SUSPEND) {
            if (request.expiresAt() == null || !request.expiresAt().isAfter(Instant.now())) {
                throw ModerationException.suspendExpiresAtInvalid();
            }
        } else if (request.expiresAt() != null) {
            throw ModerationException.warnExpiresAtNotAllowed();
        }

        User user = userRepository.findById(request.targetUserId())
                .orElseThrow(() -> ModerationException.targetUserNotFound(request.targetUserId()));

        boolean accountSuspended = false;
        if (request.actionType() == ModerationActionType.SUSPEND) {
            // ADR-001/C4: dedicated suspendedUntil column only — never reuse locked/enabled
            user.setSuspendedUntil(request.expiresAt());
            userRepository.save(user);
            accountSuspended = true;
        }
        // ADR-004: WARN never mutates User — no userRepository.save() call on this branch

        ModerationAction action = ModerationAction.builder()
                .reportId(null)
                .targetId(request.targetUserId())
                .targetType(ReportTargetType.ACCOUNT)
                .actionType(request.actionType())
                .moderatorUserId(moderatorUserId)
                .reason(request.reason())
                .actionAt(Instant.now())
                .expiresAt(request.actionType() == ModerationActionType.SUSPEND ? request.expiresAt() : null)
                .build();
        ModerationAction savedAction = moderationActionRepository.save(action);

        auditService.log(AuditAction.MODERATION_ACTION, moderatorUserId, "ACCOUNT",
                request.targetUserId().toString(),
                "actionType=" + request.actionType() + " reason=" + request.reason());

        return new WarnOrSuspendAccountResponse(
                savedAction.getId(),
                request.targetUserId(),
                request.actionType(),
                moderatorUserId,
                request.reason(),
                savedAction.getActionAt(),
                savedAction.getExpiresAt(),
                accountSuspended);
    }

    private static ModerationActionType mapToActionType(ResolutionOutcome outcome) {
        return switch (outcome) {
            case APPROVE -> ModerationActionType.APPROVE;
            case HIDE -> ModerationActionType.HIDE;
            case LOCK -> ModerationActionType.LOCK;
            case WARN -> ModerationActionType.WARN;
            case SUSPEND -> ModerationActionType.SUSPEND;
            case DISMISS -> throw new IllegalArgumentException("DISMISS has no ModerationActionType mapping");
        };
    }
}
