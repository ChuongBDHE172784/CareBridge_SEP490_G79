package com.carebridge.backend.aimoderation.dto;

import com.carebridge.backend.aimoderation.entity.AiClassification;
import com.carebridge.backend.aimoderation.entity.AiPolicySeverity;
import com.carebridge.backend.aimoderation.entity.AiRecommendedAction;
import java.math.BigDecimal;
import java.util.List;

/** Fully validated structured classification result. */
public record AiVerdict(
        AiClassification classification,
        AiPolicySeverity overallSeverity,
        BigDecimal confidence,
        List<AiVerdictMatch> matchedPolicies,
        AiRecommendedAction recommendedAction,
        String explanation
) {
}
