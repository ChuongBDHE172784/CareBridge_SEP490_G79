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
import com.carebridge.backend.content.dto.request.ModerationHistoryFilter;
import com.carebridge.backend.content.dto.request.ModerationQueueFilter;
import com.carebridge.backend.content.dto.request.PendingContentQueueFilter;
import com.carebridge.backend.content.dto.request.ResolutionOutcome;
import com.carebridge.backend.content.dto.request.ResolveReportRequest;
import com.carebridge.backend.content.dto.request.WarnOrSuspendAccountRequest;
import com.carebridge.backend.content.dto.response.ModerateContentResponse;
import com.carebridge.backend.content.dto.response.AccountViolationHistoryItemResponse;
import com.carebridge.backend.content.dto.response.AccountViolationHistoryResponse;
import com.carebridge.backend.content.dto.response.ModerationContentDetailResponse;
import com.carebridge.backend.content.dto.response.ModerationHistoryItemResponse;
import com.carebridge.backend.content.dto.response.ModerationHistoryResponse;
import com.carebridge.backend.content.dto.response.ModerationQueueItemResponse;
import com.carebridge.backend.content.dto.response.ModerationQueueResponse;
import com.carebridge.backend.content.dto.response.PendingContentItemResponse;
import com.carebridge.backend.content.dto.response.PendingContentQueueResponse;
import com.carebridge.backend.content.dto.response.ResolveReportResponse;
import com.carebridge.backend.content.dto.response.UndoModerationActionResponse;
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
import java.util.Map;
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

    @Override
    public PendingContentQueueResponse getPendingContentQueue(PendingContentQueueFilter filter, Principal principal) {
        // ADR-006: targetType is mandatory, only QUESTION/ANSWER are backed by a real query
        if (filter.targetType() != ReportTargetType.QUESTION && filter.targetType() != ReportTargetType.ANSWER) {
            throw ModerationException.pendingContentTargetTypeUnsupported(filter.targetType());
        }

        // C5: Always sort by createdAt DESC (BR-MOD-003, reused for consistency with getModerationQueue)
        PageRequest pageable = PageRequest.of(
                filter.page(),
                filter.size(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        List<PendingContentItemResponse> items;
        long totalElements;
        int pageNumber;
        int pageSize;

        if (filter.targetType() == ReportTargetType.QUESTION) {
            Page<CommunityQuestion> page = communityQuestionRepository.findByStatus(QuestionStatus.PENDING, pageable);
            List<UUID> targetIds = page.getContent().stream().map(CommunityQuestion::getId).toList();
            Map<UUID, String> previews = contentPreviewService.batchFetchPreviews(targetIds, ReportTargetType.QUESTION);
            items = page.getContent().stream()
                    .map(q -> moderationMapper.toPendingContentItemResponse(
                            q.getId(), ReportTargetType.QUESTION, previews.get(q.getId()), q.getCreatedAt()))
                    .toList();
            totalElements = page.getTotalElements();
            pageNumber = page.getNumber();
            pageSize = page.getSize();
        } else {
            Page<CommunityAnswer> page = communityAnswerRepository.findByStatus(AnswerStatus.PENDING, pageable);
            List<UUID> targetIds = page.getContent().stream().map(CommunityAnswer::getId).toList();
            Map<UUID, String> previews = contentPreviewService.batchFetchPreviews(targetIds, ReportTargetType.ANSWER);
            items = page.getContent().stream()
                    .map(a -> moderationMapper.toPendingContentItemResponse(
                            a.getId(), ReportTargetType.ANSWER, previews.get(a.getId()), a.getCreatedAt()))
                    .toList();
            totalElements = page.getTotalElements();
            pageNumber = page.getNumber();
            pageSize = page.getSize();
        }

        // C2: AuditService.log() after every successful queue view — reuses MODERATION_QUEUE_VIEWED
        // (ADR-003 of UC-99; no new AuditAction needed, avoids an audit_logs_action_check migration)
        String userId = principal != null ? principal.getName() : null;
        auditService.log(AuditAction.MODERATION_QUEUE_VIEWED, userId, null,
                "pending-content targetType=" + filter.targetType() + " count=" + totalElements);

        return new PendingContentQueueResponse(items, totalElements, pageNumber, pageSize);
    }

    // Excludes ACCOUNT actions — belongs to a separate account-violation history view (TDS §16 ADR-007)
    private static final List<ReportTargetType> HISTORY_TARGET_TYPES =
            List.of(ReportTargetType.QUESTION, ReportTargetType.ANSWER);

    @Override
    public ModerationHistoryResponse getModerationHistory(ModerationHistoryFilter filter, Principal principal) {
        PageRequest pageable = PageRequest.of(filter.page(), filter.size(),
                Sort.by(Sort.Direction.DESC, "actionAt"));

        List<ReportTargetType> targetTypes = filter.targetType() != null
                ? List.of(filter.targetType())
                : HISTORY_TARGET_TYPES;

        Page<ModerationAction> page =
                moderationActionRepository.findByTargetTypeInOrderByActionAtDesc(targetTypes, pageable);

        // Batch-resolve moderator display names (avoid N+1)
        List<UUID> moderatorIds = page.getContent().stream()
                .map(ModerationAction::getModeratorUserId)
                .distinct()
                .toList();
        Map<UUID, String> moderatorNames = userRepository.findAllById(moderatorIds).stream()
                .collect(java.util.stream.Collectors.toMap(User::getId, User::getName));

        // Batch-resolve content previews per targetType (ContentPreviewService takes one type at a time)
        Map<UUID, String> questionPreviews = batchPreviewsFor(page.getContent(), ReportTargetType.QUESTION);
        Map<UUID, String> answerPreviews = batchPreviewsFor(page.getContent(), ReportTargetType.ANSWER);

        List<ModerationHistoryItemResponse> items = page.getContent().stream()
                .map(action -> {
                    String preview = action.getTargetType() == ReportTargetType.QUESTION
                            ? questionPreviews.get(action.getTargetId())
                            : answerPreviews.get(action.getTargetId());
                    return new ModerationHistoryItemResponse(
                            action.getId(),
                            action.getTargetId(),
                            action.getTargetType(),
                            action.getActionType(),
                            preview,
                            moderatorNames.get(action.getModeratorUserId()),
                            action.getReason(),
                            action.getActionAt());
                })
                .toList();

        return new ModerationHistoryResponse(items, page.getTotalElements(), page.getNumber(), page.getSize());
    }

    @Override
    public AccountViolationHistoryResponse getAccountViolationHistory(int page, int size, Principal principal) {
        Page<ModerationAction> actionPage = moderationActionRepository.findByTargetTypeAndActionTypeInOrderByActionAtDesc(
                ReportTargetType.ACCOUNT,
                ACCOUNT_ACTION_TYPES,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "actionAt")
                        .and(Sort.by(Sort.Direction.DESC, "id"))));

        List<UUID> userIds = actionPage.getContent().stream()
                .flatMap(action -> java.util.stream.Stream.of(action.getTargetId(), action.getModeratorUserId()))
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        Map<UUID, String> names = userRepository.findAllById(userIds).stream()
                .collect(java.util.stream.Collectors.toMap(User::getId, User::getName));

        List<AccountViolationHistoryItemResponse> items = actionPage.getContent().stream()
                .map(action -> new AccountViolationHistoryItemResponse(
                        action.getId(),
                        action.getTargetId(),
                        names.getOrDefault(action.getTargetId(), "Tài khoản không còn tồn tại"),
                        action.getModeratorUserId(),
                        names.getOrDefault(action.getModeratorUserId(), "Người kiểm duyệt không còn tồn tại"),
                        action.getActionType(),
                        action.getReason(),
                        action.getExpiresAt(),
                        action.getReportId(),
                        action.getActionAt()))
                .toList();
        return new AccountViolationHistoryResponse(
                items, actionPage.getTotalElements(), actionPage.getNumber(), actionPage.getSize());
    }

    private Map<UUID, String> batchPreviewsFor(List<ModerationAction> actions, ReportTargetType targetType) {
        List<UUID> targetIds = actions.stream()
                .filter(a -> a.getTargetType() == targetType)
                .map(ModerationAction::getTargetId)
                .toList();
        if (targetIds.isEmpty()) {
            return Map.of();
        }
        return contentPreviewService.batchFetchPreviews(targetIds, targetType);
    }

    // Account-level enforcement belongs to moderateAccount/resolveReport, not this content-status
    // endpoint. UNDO (CB-MOD-IMP-009) is also excluded — it may only be created via the dedicated
    // undoModerationAction()/{actionId}/undo endpoint, which enforces the ADR-002 guards.
    private static final Set<ModerationActionType> OUT_OF_SCOPE_ACTION_TYPES =
            Set.of(ModerationActionType.WARN, ModerationActionType.SUSPEND, ModerationActionType.RESTRICT,
                    ModerationActionType.UNDO);

    // C6: reason required for HIDE/LOCK/REQUEST_REVISION, optional for APPROVE (ADR-006)
    private static final Set<ModerationActionType> REASON_REQUIRED_ACTION_TYPES =
            Set.of(ModerationActionType.HIDE, ModerationActionType.LOCK,
                    ModerationActionType.REQUEST_REVISION, ModerationActionType.LABEL);

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
            // ACCOUNT/EXPERT/USER targets (UC-102/UC-14) never reach this content-only primitive —
            // moderateAccount() has its own dedicated write path.
            case ACCOUNT, EXPERT, USER -> throw ModerationException.unsupportedActionType(actionType);
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

    // C5: action-targetType compatibility per TDS §6.4 — QUESTION supports APPROVE/HIDE/LOCK/REQUEST_REVISION
    private String moderateQuestion(UUID targetId, ModerationActionType actionType) {
        if (actionType == ModerationActionType.LOCK) {
            if (communityQuestionRepository.lockIfApproved(targetId) == 0) {
                throw ModerationException.questionMustBeApprovedToLock(targetId);
            }
            return QuestionStatus.LOCKED.name();
        }

        CommunityQuestion question = communityQuestionRepository.findById(targetId)
                .orElseThrow(() -> ModerationException.targetNotFound(targetId, ReportTargetType.QUESTION));

        QuestionStatus newStatus = switch (actionType) {
            case APPROVE -> QuestionStatus.APPROVED;
            case HIDE -> QuestionStatus.HIDDEN;
            case REQUEST_REVISION -> QuestionStatus.PENDING;
            // A label records a safety warning without changing visibility or discussion state.
            case LABEL -> question.getStatus();
            default -> throw ModerationException.actionNotSupportedForTargetType(
                    actionType, ReportTargetType.QUESTION);
        };

        question.setStatus(newStatus);
        communityQuestionRepository.save(question);
        return newStatus.name();
    }

    // C5: action-targetType compatibility per TDS §6.4 — ANSWER supports APPROVE/HIDE/REQUEST_REVISION only (no LOCKED)
    private String moderateAnswer(UUID targetId, ModerationActionType actionType) {
        if (actionType == ModerationActionType.LOCK) {
            throw ModerationException.actionNotSupportedForTargetType(actionType, ReportTargetType.ANSWER);
        }

        CommunityAnswer answer = communityAnswerRepository.findById(targetId)
                .orElseThrow(() -> ModerationException.targetNotFound(targetId, ReportTargetType.ANSWER));

        AnswerStatus oldStatus = answer.getStatus();
        AnswerStatus newStatus = switch (actionType) {
            case APPROVE -> AnswerStatus.APPROVED;
            case HIDE -> AnswerStatus.HIDDEN;
            case REQUEST_REVISION -> AnswerStatus.PENDING;
            case LABEL -> answer.getStatus();
            default -> throw ModerationException.actionNotSupportedForTargetType(actionType, ReportTargetType.ANSWER);
        };

        answer.setStatus(newStatus);
        communityAnswerRepository.save(answer);

        // Keep community_questions.answer_count in sync with the visible (APPROVED) answer set
        if (oldStatus != AnswerStatus.APPROVED && newStatus == AnswerStatus.APPROVED) {
            communityQuestionRepository.incrementAnswerCount(answer.getQuestionId());
        } else if (oldStatus == AnswerStatus.APPROVED && newStatus != AnswerStatus.APPROVED) {
            communityQuestionRepository.decrementAnswerCount(answer.getQuestionId());
        }

        return newStatus.name();
    }

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

        UUID actionId = null;
        ModerationActionType actionType = null;
        String resultingStatus = null;

        if (request.outcome() == ResolutionOutcome.DISMISS) {
            // BR-MOD-010: DISMISS creates no ModerationAction
            report.setStatus(ReportStatus.DISMISSED);
        } else {
            actionType = mapToActionType(request.outcome());
            if (ACCOUNT_ACTION_TYPES.contains(actionType)) {
                UUID targetUserId = resolveAccountTargetUserId(report);
                WarnOrSuspendAccountResponse accountResponse = applyAccountAction(targetUserId, actionType,
                        request.reason(), request.expiresAt(), moderatorUserId, report.getId());
                actionId = accountResponse.actionId();
                resultingStatus = accountResultingStatus(accountResponse);
            } else {
                if (report.getTargetType() == ReportTargetType.CONTENT
                        || report.getTargetType() == ReportTargetType.USER
                        || report.getTargetType() == ReportTargetType.EXPERT
                        || report.getTargetType() == ReportTargetType.ACCOUNT) {
                    throw ModerationException.contentActionNotSupportedForReport();
                }
                ModerateContentResponse actionResponse = applyContentAction(report.getTargetId(), report.getTargetType(),
                        actionType, request.reason(), moderatorUserId, report.getId());
                actionId = actionResponse.actionId();
                resultingStatus = actionResponse.resultingStatus();
            }
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

    // UC-102/UC-58: only account-level actions are valid at this endpoint.
    private static final Set<ModerationActionType> ACCOUNT_ACTION_TYPES =
            Set.of(ModerationActionType.WARN, ModerationActionType.SUSPEND, ModerationActionType.RESTRICT,
                    ModerationActionType.ESCALATE);

    @Override
    @Transactional
    public WarnOrSuspendAccountResponse moderateAccount(WarnOrSuspendAccountRequest request, Principal principal) {
        UUID moderatorUserId = SecurityUtils.requireCurrentUserId(principal);

        WarnOrSuspendAccountResponse response = applyAccountAction(request.targetUserId(), request.actionType(),
                request.reason(), request.expiresAt(), moderatorUserId, null);

        auditService.log(AuditAction.MODERATION_ACTION, moderatorUserId, "ACCOUNT",
                request.targetUserId().toString(),
                "actionType=" + request.actionType() + " reason=" + request.reason());

        return response;
    }

    private WarnOrSuspendAccountResponse applyAccountAction(UUID targetUserId, ModerationActionType actionType,
            String reason, Instant expiresAt, UUID moderatorUserId, UUID reportId) {
        if (targetUserId.equals(moderatorUserId)) {
            throw ModerationException.selfActionForbidden();
        }

        if (!ACCOUNT_ACTION_TYPES.contains(actionType)) {
            throw ModerationException.accountActionTypeNotSupported(actionType);
        }

        if (reason == null || reason.isBlank()) {
            throw ModerationException.accountReasonRequired(actionType);
        }

        if (actionType == ModerationActionType.ESCALATE) {
            if (expiresAt != null) throw ModerationException.warnExpiresAtNotAllowed();
        } else if (actionType == ModerationActionType.SUSPEND) {
            if (expiresAt == null || !expiresAt.isAfter(Instant.now())) {
                throw ModerationException.suspendExpiresAtInvalid();
            }
        } else if (actionType == ModerationActionType.RESTRICT) {
            if (expiresAt == null || !expiresAt.isAfter(Instant.now())) {
                throw ModerationException.restrictExpiresAtInvalid();
            }
        } else if (expiresAt != null) {
            throw ModerationException.warnExpiresAtNotAllowed();
        }

        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> ModerationException.targetUserNotFound(targetUserId));

        boolean accountSuspended = false;
        if (actionType == ModerationActionType.SUSPEND) {
            user.setSuspendedUntil(expiresAt);
            userRepository.save(user);
            accountSuspended = true;
        } else if (actionType == ModerationActionType.RESTRICT) {
            user.setCommunityPostingRestrictedUntil(expiresAt);
            userRepository.save(user);
        }

        ModerationAction action = ModerationAction.builder()
                .reportId(reportId)
                .targetId(targetUserId)
                .targetType(ReportTargetType.ACCOUNT)
                .actionType(actionType)
                .moderatorUserId(moderatorUserId)
                .reason(reason)
                .actionAt(Instant.now())
                .expiresAt(actionType == ModerationActionType.SUSPEND
                        || actionType == ModerationActionType.RESTRICT ? expiresAt : null)
                .build();
        ModerationAction savedAction = moderationActionRepository.save(action);

        return new WarnOrSuspendAccountResponse(
                savedAction.getId(),
                targetUserId,
                actionType,
                moderatorUserId,
                reason,
                savedAction.getActionAt(),
                savedAction.getExpiresAt(),
                accountSuspended);
    }

    private UUID resolveAccountTargetUserId(ContentReport report) {
        return switch (report.getTargetType()) {
            case USER, EXPERT, ACCOUNT -> report.getTargetId();
            case QUESTION -> communityQuestionRepository.findById(report.getTargetId())
                    .map(CommunityQuestion::getAuthorId)
                    .orElseThrow(() -> ModerationException.targetNotFound(
                            report.getTargetId(), ReportTargetType.QUESTION));
            case ANSWER -> communityAnswerRepository.findById(report.getTargetId())
                    .map(CommunityAnswer::getAuthorId)
                    .orElseThrow(() -> ModerationException.targetNotFound(
                            report.getTargetId(), ReportTargetType.ANSWER));
            case CONTENT -> throw ModerationException.accountActionNotAvailable(ModerationActionType.WARN);
        };
    }

    private String accountResultingStatus(WarnOrSuspendAccountResponse response) {
        return switch (response.actionType()) {
            case WARN -> "WARNED";
            case SUSPEND -> "SUSPENDED_UNTIL_" + response.expiresAt();
            case RESTRICT -> "COMMUNITY_RESTRICTED_UNTIL_" + response.expiresAt();
            case ESCALATE -> "ESCALATED_TO_SYSTEM_ADMIN";
            default -> throw ModerationException.accountActionTypeNotSupported(response.actionType());
        };
    }

    // CB-MOD-IMP-008 ADR-002: only QUESTION/ANSWER carry full body text — same restriction as the
    // Pending Content Queue (reuses pendingContentTargetTypeUnsupported, MOD-023, no new error code)
    private static final Set<ReportTargetType> CONTENT_DETAIL_SUPPORTED_TARGET_TYPES =
            Set.of(ReportTargetType.QUESTION, ReportTargetType.ANSWER);

    // CB-MOD-IMP-008 ADR-001: reads directly via repository.findById() — deliberately does NOT
    // reuse CommunityQuestionService.getQuestionDetail(), which filters out non-APPROVED content
    // not owned by the caller and would 404 for a moderator reviewing a PENDING/HIDDEN/LOCKED item.
    @Override
    public ModerationContentDetailResponse getContentDetail(
            ReportTargetType targetType, UUID targetId, Principal principal) {
        if (!CONTENT_DETAIL_SUPPORTED_TARGET_TYPES.contains(targetType)) {
            throw ModerationException.pendingContentTargetTypeUnsupported(targetType);
        }

        ModerationContentDetailResponse response = targetType == ReportTargetType.QUESTION
                ? buildQuestionDetail(targetId)
                : buildAnswerDetail(targetId);

        String userId = principal != null ? principal.getName() : null;
        auditService.log(AuditAction.MODERATION_QUEUE_VIEWED, userId, null,
                "content-detail-viewed targetType=" + targetType + " targetId=" + targetId);

        return response;
    }

    private ModerationContentDetailResponse buildQuestionDetail(UUID targetId) {
        CommunityQuestion question = communityQuestionRepository.findById(targetId)
                .orElseThrow(() -> ModerationException.targetNotFound(targetId, ReportTargetType.QUESTION));

        // ADR-003: authorId/authorName returned even when the question was posted anonymously —
        // `anonymous` only gates the public-facing feed/detail views, not this moderator-only endpoint.
        return new ModerationContentDetailResponse(
                question.getId(),
                ReportTargetType.QUESTION,
                question.getAuthorId(),
                resolveAuthorName(question.getAuthorId()),
                question.getTitle(),
                question.getBody(),
                question.getStatus().name(),
                question.isAnonymous(),
                null,
                null,
                question.getCreatedAt(),
                question.getUpdatedAt());
    }

    private ModerationContentDetailResponse buildAnswerDetail(UUID targetId) {
        CommunityAnswer answer = communityAnswerRepository.findById(targetId)
                .orElseThrow(() -> ModerationException.targetNotFound(targetId, ReportTargetType.ANSWER));

        // Parent question title gives the moderator context for what the answer is responding to.
        String questionTitle = communityQuestionRepository.findById(answer.getQuestionId())
                .map(CommunityQuestion::getTitle)
                .orElse(null);

        return new ModerationContentDetailResponse(
                answer.getId(),
                ReportTargetType.ANSWER,
                answer.getAuthorId(),
                resolveAuthorName(answer.getAuthorId()),
                null,
                answer.getBody(),
                answer.getStatus().name(),
                false,
                answer.getQuestionId(),
                questionTitle,
                answer.getCreatedAt(),
                answer.getUpdatedAt());
    }

    private String resolveAuthorName(UUID authorId) {
        return userRepository.findById(authorId).map(User::getName).orElse(null);
    }

    // CB-MOD-IMP-009 ADR-001: only APPROVE/HIDE/LOCK produce a status that undo can meaningfully
    // revert (always to PENDING). REQUEST_REVISION already results in PENDING (no-op); WARN/SUSPEND/
    // RESTRICT are account-level (ADR-004, out of scope); UNDO cannot undo itself (no UNDO-of-UNDO).
    private static final Set<ModerationActionType> UNDOABLE_ACTION_TYPES =
            Set.of(ModerationActionType.APPROVE, ModerationActionType.HIDE, ModerationActionType.LOCK);

    // CB-MOD-IMP-009: guard order is fixed and load-bearing (Test-Spec §UNDO-TC-010 pins targetType
    // before actionType) — not-found -> targetType -> reportId -> actionType-undoable -> most-recent
    // -> status-match -> mutate. See ADR-001 (always PENDING), ADR-002 (2 guards), ADR-003
    // (answer_count mirror), ADR-004 (direct actions only), ADR-005 (append-only, new UNDO row).
    @Override
    @Transactional
    public UndoModerationActionResponse undoModerationAction(UUID actionId, Principal principal) {
        ModerationAction original = moderationActionRepository.findById(actionId)
                .orElseThrow(() -> ModerationException.moderationActionNotFound(actionId));

        if (original.getTargetType() != ReportTargetType.QUESTION && original.getTargetType() != ReportTargetType.ANSWER) {
            throw ModerationException.undoTargetTypeUnsupported(original.getTargetType());
        }
        if (original.getReportId() != null) {
            throw ModerationException.undoNotSupportedForReportResolution(actionId);
        }
        if (!UNDOABLE_ACTION_TYPES.contains(original.getActionType())) {
            throw ModerationException.undoActionTypeNotSupported(original.getActionType());
        }

        ModerationAction mostRecent = moderationActionRepository
                .findTopByTargetIdAndTargetTypeOrderByActionAtDesc(original.getTargetId(), original.getTargetType())
                .orElseThrow(() -> ModerationException.moderationActionNotFound(actionId));
        if (!mostRecent.getId().equals(actionId)) {
            throw ModerationException.undoNotMostRecentAction(actionId);
        }

        String resultingStatus = original.getTargetType() == ReportTargetType.QUESTION
                ? undoQuestionAction(original)
                : undoAnswerAction(original);

        UUID moderatorUserId = SecurityUtils.requireCurrentUserId(principal);
        ModerationAction undoAction = ModerationAction.builder()
                .reportId(null)
                .targetId(original.getTargetId())
                .targetType(original.getTargetType())
                .actionType(ModerationActionType.UNDO)
                .moderatorUserId(moderatorUserId)
                .reason("Hoàn tác hành động " + original.getActionType())
                .actionAt(Instant.now())
                .build();
        ModerationAction savedUndoAction = moderationActionRepository.save(undoAction);

        auditService.log(AuditAction.MODERATION_ACTION, moderatorUserId, original.getTargetType().name(),
                original.getTargetId().toString(), "undo actionId=" + actionId + " originalActionType=" + original.getActionType());

        return new UndoModerationActionResponse(
                savedUndoAction.getId(),
                actionId,
                original.getTargetId(),
                original.getTargetType(),
                moderatorUserId,
                savedUndoAction.getActionAt(),
                resultingStatus);
    }

    // ADR-002 guard 2 (status-match) + mutation to PENDING for a QUESTION target.
    private String undoQuestionAction(ModerationAction original) {
        CommunityQuestion question = communityQuestionRepository.findById(original.getTargetId())
                .orElseThrow(() -> ModerationException.targetNotFound(original.getTargetId(), ReportTargetType.QUESTION));

        QuestionStatus expectedCurrentStatus = switch (original.getActionType()) {
            case APPROVE -> QuestionStatus.APPROVED;
            case HIDE -> QuestionStatus.HIDDEN;
            case LOCK -> QuestionStatus.LOCKED;
            default -> throw ModerationException.undoActionTypeNotSupported(original.getActionType());
        };
        if (question.getStatus() != expectedCurrentStatus) {
            throw ModerationException.undoStatusSuperseded(original.getId());
        }

        question.setStatus(QuestionStatus.PENDING);
        communityQuestionRepository.save(question);
        return QuestionStatus.PENDING.name();
    }

    // ADR-002 guard 2 (status-match) + mutation to PENDING + ADR-003 answer_count mirror for an
    // ANSWER target — decrements only when the action being undone was the APPROVE that counted it.
    private String undoAnswerAction(ModerationAction original) {
        CommunityAnswer answer = communityAnswerRepository.findById(original.getTargetId())
                .orElseThrow(() -> ModerationException.targetNotFound(original.getTargetId(), ReportTargetType.ANSWER));

        AnswerStatus expectedCurrentStatus = switch (original.getActionType()) {
            case APPROVE -> AnswerStatus.APPROVED;
            case HIDE -> AnswerStatus.HIDDEN;
            default -> throw ModerationException.undoActionTypeNotSupported(original.getActionType());
        };
        if (answer.getStatus() != expectedCurrentStatus) {
            throw ModerationException.undoStatusSuperseded(original.getId());
        }

        answer.setStatus(AnswerStatus.PENDING);
        communityAnswerRepository.save(answer);

        // Mirrors moderateAnswer()'s exact condition (line ~332): only APPROVED<->non-APPROVED
        // transitions touch the counter.
        if (expectedCurrentStatus == AnswerStatus.APPROVED) {
            communityQuestionRepository.decrementAnswerCount(answer.getQuestionId());
        }
        return AnswerStatus.PENDING.name();
    }

    private static ModerationActionType mapToActionType(ResolutionOutcome outcome) {
        return switch (outcome) {
            case APPROVE -> ModerationActionType.APPROVE;
            case HIDE -> ModerationActionType.HIDE;
            case LOCK -> ModerationActionType.LOCK;
            case REQUEST_REVISION -> ModerationActionType.REQUEST_REVISION;
            case WARN -> ModerationActionType.WARN;
            case SUSPEND -> ModerationActionType.SUSPEND;
            case RESTRICT -> ModerationActionType.RESTRICT;
            case ESCALATE -> ModerationActionType.ESCALATE;
            case DISMISS -> throw new IllegalArgumentException("DISMISS has no ModerationActionType mapping");
        };
    }
}
