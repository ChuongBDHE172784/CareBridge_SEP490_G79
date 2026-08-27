package com.carebridge.backend.aimoderation.dto.response;

import com.carebridge.backend.aimoderation.entity.AiPolicySeverity;
import com.carebridge.backend.aimoderation.entity.AiViolationCategory;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** policyId/policyVersion are the immutable policy snapshot taken at scan time (CB-MOD-IMP-017). */
public record AiAssessmentMatchResponse(
        UUID policyId,
        String policyCode,
        Integer policyVersion,
        AiViolationCategory category,
        AiPolicySeverity severity,
        BigDecimal confidence,
        List<String> evidence,
        String explanation
) {
}
