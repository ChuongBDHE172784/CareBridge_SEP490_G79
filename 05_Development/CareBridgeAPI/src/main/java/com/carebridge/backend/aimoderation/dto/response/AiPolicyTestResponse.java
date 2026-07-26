package com.carebridge.backend.aimoderation.dto.response;

import com.carebridge.backend.aimoderation.entity.AiClassification;
import com.carebridge.backend.aimoderation.entity.AiPolicySeverity;
import com.carebridge.backend.aimoderation.entity.AiRecommendedAction;
import com.carebridge.backend.content.entity.CasePriority;
import java.math.BigDecimal;
import java.util.List;

public record AiPolicyTestResponse(
        AiClassification classification,
        AiPolicySeverity overallSeverity,
        BigDecimal confidence,
        AiRecommendedAction recommendedAction,
        String explanation,
        List<AiAssessmentMatchResponse> matches,
        boolean wouldCreateCase,
        CasePriority wouldCreatePriority,
        String model,
        long latencyMs
) {
}
