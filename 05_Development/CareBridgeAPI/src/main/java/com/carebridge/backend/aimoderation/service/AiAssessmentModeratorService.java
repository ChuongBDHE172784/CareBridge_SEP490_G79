package com.carebridge.backend.aimoderation.service;

import com.carebridge.backend.aimoderation.dto.request.AiFeedbackRequest;
import com.carebridge.backend.aimoderation.dto.response.AiAssessmentResponse;
import com.carebridge.backend.aimoderation.dto.response.AiFeedbackResponse;
import com.carebridge.backend.aimoderation.entity.AiContentAssessment;
import com.carebridge.backend.aimoderation.entity.AiFeedbackVerdict;
import com.carebridge.backend.aimoderation.exception.AiModerationException;
import com.carebridge.backend.aimoderation.mapper.AiModerationMapper;
import com.carebridge.backend.aimoderation.repository.AiContentAssessmentRepository;
import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.content.entity.ContentReport;
import com.carebridge.backend.content.entity.ModerationAction;
import com.carebridge.backend.content.entity.ModerationActionType;
import com.carebridge.backend.content.entity.ReportStatus;
import com.carebridge.backend.content.exception.ModerationException;
import com.carebridge.backend.content.repository.ContentReportRepository;
import com.carebridge.backend.content.repository.ModerationActionRepository;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Moderator-facing assessment view + agree/disagree feedback. CB-MOD-IMP-017: the CURRENT
 * feedback lives directly on moderation_cases (no join table); every submission also appends
 * an immutable MODERATION_AI_FEEDBACK_SUBMITTED event to the canonical audit_events table in
 * the same transaction, so the full history survives repeated updates. Feedback never mutates
 * policies and never "trains" anything.
 */
@Service
@RequiredArgsConstructor
public class AiAssessmentModeratorService {

    private static final int MAX_REASON_LENGTH = 500;

    private final AiContentAssessmentRepository assessmentRepository;
    private final ContentReportRepository contentReportRepository;
    private final ModerationActionRepository moderationActionRepository;
    private final AiModerationMapper mapper;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public AiAssessmentResponse getAssessmentForReport(UUID reportId, UUID moderatorUserId) {
        Optional<AiContentAssessment> byCase = assessmentRepository
                .findFirstByModerationCaseIdOrderByCreatedAtDesc(reportId);
        ContentReport moderationCase = contentReportRepository.findById(reportId).orElse(null);
        AiContentAssessment assessment = byCase.orElseGet(() -> Optional.ofNullable(moderationCase)
                .flatMap(this::latestForTarget)
                .orElseThrow(() -> AiModerationException.assessmentNotFound("report " + reportId)));
        return mapper.toAssessmentResponse(assessment, moderationCase);
    }

    private Optional<AiContentAssessment> latestForTarget(ContentReport report) {
        if (report.getTargetType() == null || report.getTargetId() == null) {
            return Optional.empty();
        }
        return assessmentRepository.findFirstByTargetTypeAndTargetIdOrderByCreatedAtDesc(
                report.getTargetType(), report.getTargetId());
    }

    /**
     * Latest-wins on moderation_cases + append-only history in moderation_events, atomically.
     * Feedback is only accepted for an assessment that is attached to a moderation case, and
     * never while another moderator holds the case IN_REVIEW.
     */
    @Transactional
    public AiFeedbackResponse submitFeedback(UUID assessmentId, AiFeedbackRequest request, UUID moderatorUserId) {
        AiContentAssessment assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> AiModerationException.assessmentNotFound(assessmentId.toString()));
        if (assessment.getModerationCaseId() == null) {
            throw AiModerationException.feedbackRequiresAttachedCase(assessmentId.toString());
        }
        ContentReport moderationCase = contentReportRepository.findById(assessment.getModerationCaseId())
                .orElseThrow(() -> AiModerationException.feedbackRequiresAttachedCase(assessmentId.toString()));

        // Case-access guard: a case another moderator is actively reviewing is theirs alone.
        if (moderationCase.getStatus() == ReportStatus.IN_REVIEW
                && !moderatorUserId.equals(moderationCase.getAssignedModeratorId())) {
            throw ModerationException.reportClaimedByAnotherModerator(moderationCase.getId());
        }

        AiFeedbackVerdict previousDecision = moderationCase.getAiFeedbackDecision();
        String reason = sanitizeReason(request.note());
        Instant now = Instant.now();

        moderationCase.setAiFeedbackDecision(request.verdict());
        moderationCase.setAiFeedbackReason(reason);
        moderationCase.setAiFeedbackBy(moderatorUserId);
        moderationCase.setAiFeedbackAt(now);
        moderationCase.setAiFeedbackAssessmentId(assessmentId);
        contentReportRepository.save(moderationCase);

        ModerationAction event = moderationActionRepository.save(ModerationAction.builder()
                .reportId(moderationCase.getId())
                .targetId(moderationCase.getTargetId())
                .targetType(moderationCase.getTargetType())
                .actionType(ModerationActionType.AI_FEEDBACK_SUBMITTED)
                .moderatorUserId(moderatorUserId)
                .actionAt(now)
                .reason(reason)
                .payload(buildFeedbackPayload(request.verdict(), assessmentId,
                        previousDecision, now))
                .build());

        auditService.log(AuditAction.AI_FEEDBACK_SUBMITTED, moderatorUserId, "AiContentAssessment",
                assessmentId.toString(), "verdict=" + request.verdict()
                        + " caseId=" + moderationCase.getId()
                        + (previousDecision != null ? " previous=" + previousDecision : ""));

        // feedbackId in the response is the immutable history event's id (there is no feedback row).
        return new AiFeedbackResponse(event.getId(), assessmentId, request.verdict(), reason, now);
    }

    private String sanitizeReason(String note) {
        if (note == null || note.isBlank()) {
            return null;
        }
        String trimmed = note.strip();
        return trimmed.length() <= MAX_REASON_LENGTH ? trimmed : trimmed.substring(0, MAX_REASON_LENGTH);
    }

    /**
     * Sanitized structured payload only — never raw content, never model output. Stored in the
     * canonical audit_events.payload jsonb column; the sanitized reason rides on the entity's
     * transient reason field and is folded into the same payload by the entity lifecycle hook.
     */
    private Map<String, Object> buildFeedbackPayload(AiFeedbackVerdict decision, UUID assessmentId,
            AiFeedbackVerdict previousDecision, Instant submittedAt) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("decision", decision.name());
        payload.put("assessmentId", assessmentId.toString());
        if (previousDecision != null) {
            payload.put("previousDecision", previousDecision.name());
        }
        payload.put("submittedAt", submittedAt.toString());
        return payload;
    }
}
