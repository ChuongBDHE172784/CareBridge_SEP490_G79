package com.carebridge.backend.aimoderation.mapper;

import com.carebridge.backend.aimoderation.dto.AiVerdictMatch;
import com.carebridge.backend.aimoderation.dto.response.AiAssessmentMatchResponse;
import com.carebridge.backend.aimoderation.dto.response.AiAssessmentResponse;
import com.carebridge.backend.aimoderation.dto.response.AiPolicyPageResponse;
import com.carebridge.backend.aimoderation.dto.response.AiPolicyResponse;
import com.carebridge.backend.aimoderation.entity.AiContentAssessment;
import com.carebridge.backend.aimoderation.entity.AiModerationPolicy;
import com.carebridge.backend.content.entity.ContentReport;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AiModerationMapper {

    private final ObjectMapper objectMapper;

    public AiPolicyResponse toPolicyResponse(AiModerationPolicy policy) {
        return new AiPolicyResponse(
                policy.getId(),
                policy.getPolicyCode(),
                policy.getName(),
                policy.getDetectionGuidance(),
                policy.getViolationCategory(),
                policy.getReportCategory(),
                policy.getSeverity(),
                policy.targetTypes(),
                policy.getConfidenceThreshold(),
                policy.isActive(),
                policy.isSystemDefault(),
                policy.getVersion(),
                policy.getCreatedAt(),
                policy.getUpdatedAt());
    }

    public AiPolicyPageResponse toPolicyPageResponse(Page<AiModerationPolicy> page) {
        return new AiPolicyPageResponse(
                page.getContent().stream().map(this::toPolicyResponse).toList(),
                page.getTotalElements(),
                page.getNumber(),
                page.getSize());
    }

    /** CB-MOD-IMP-017: serializes validated matches into the matches_jsonb storage shape. */
    public String serializeMatches(List<AiVerdictMatch> matches) {
        try {
            return objectMapper.writeValueAsString(matches == null ? List.of() : matches);
        } catch (JsonProcessingException ex) {
            // Our own records always serialize; failing loudly keeps the scan retryable
            // instead of silently persisting an empty snapshot.
            throw new IllegalStateException("Failed to serialize assessment matches", ex);
        }
    }

    /** Deserializes matches_jsonb back into the typed list (API stays typed, never raw JSON). */
    public List<AiVerdictMatch> parseMatches(String matchesJson) {
        if (matchesJson == null || matchesJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(matchesJson, new TypeReference<List<AiVerdictMatch>>() {
            });
        } catch (JsonProcessingException ex) {
            // Corrupt stored JSON must not break the moderator view — log and degrade to empty.
            log.warn("Unparseable matches_jsonb on an assessment: {}", ex.getClass().getSimpleName());
            return List.of();
        }
    }

    public AiAssessmentMatchResponse toMatchResponse(AiVerdictMatch match) {
        return new AiAssessmentMatchResponse(
                match.policyId(),
                match.policyCode(),
                match.policyVersion(),
                match.category(),
                match.severity(),
                match.confidence(),
                match.evidence() == null ? List.of() : match.evidence(),
                match.explanation());
    }

    /**
     * CB-MOD-IMP-017: the feedback object is built from moderation_cases columns (the dropped
     * ai_assessment_feedback table is gone) — shown only when the case's current feedback
     * refers to THIS assessment. API field names are unchanged for frontend compatibility.
     */
    public AiAssessmentResponse toAssessmentResponse(AiContentAssessment assessment, ContentReport moderationCase) {
        boolean feedbackForThisAssessment = moderationCase != null
                && assessment.getId() != null
                && assessment.getId().equals(moderationCase.getAiFeedbackAssessmentId());
        return new AiAssessmentResponse(
                assessment.getId(),
                assessment.getTargetType(),
                assessment.getTargetId(),
                assessment.getStatus(),
                assessment.getClassification(),
                assessment.getOverallSeverity(),
                assessment.getConfidence(),
                assessment.getRecommendedAction(),
                assessment.getExplanation(),
                assessment.getProvider(),
                assessment.getModel(),
                assessment.getPolicySetHash(),
                assessment.getErrorCode(),
                assessment.getModerationCaseId(),
                assessment.getCreatedAt(),
                assessment.getCompletedAt(),
                parseMatches(assessment.getMatchesJson()).stream().map(this::toMatchResponse).toList(),
                feedbackForThisAssessment ? moderationCase.getAiFeedbackDecision() : null,
                feedbackForThisAssessment ? moderationCase.getAiFeedbackReason() : null);
    }
}
