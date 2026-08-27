package com.carebridge.backend.aimoderation.dto.response;

import com.carebridge.backend.aimoderation.entity.AiAssessmentStatus;
import com.carebridge.backend.aimoderation.entity.AiClassification;
import com.carebridge.backend.aimoderation.entity.AiFeedbackVerdict;
import com.carebridge.backend.aimoderation.entity.AiPolicySeverity;
import com.carebridge.backend.aimoderation.entity.AiRecommendedAction;
import com.carebridge.backend.content.entity.ReportTargetType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AiAssessmentResponse(
        UUID assessmentId,
        ReportTargetType targetType,
        UUID targetId,
        AiAssessmentStatus status,
        AiClassification classification,
        AiPolicySeverity overallSeverity,
        BigDecimal confidence,
        AiRecommendedAction recommendedAction,
        String explanation,
        String provider,
        String model,
        String policySetHash,
        String errorCode,
        UUID moderationCaseId,
        Instant createdAt,
        Instant completedAt,
        List<AiAssessmentMatchResponse> matches,
        AiFeedbackVerdict myFeedbackVerdict,
        String myFeedbackNote
) {
}
