package com.carebridge.backend.aimoderation.dto.request;

import com.carebridge.backend.aimoderation.entity.AiPolicySeverity;
import com.carebridge.backend.aimoderation.entity.AiViolationCategory;
import com.carebridge.backend.content.entity.ReportCategory;
import com.carebridge.backend.content.entity.ReportTargetType;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/** Null-tolerant PATCH semantics (same convention as UpdateRedFlagRuleRequest). policyCode is immutable. */
public record UpdateAiPolicyRequest(
        @Size(max = 150) String name,
        @Size(max = 2000) String detectionGuidance,
        AiViolationCategory violationCategory,
        ReportCategory reportCategory,
        AiPolicySeverity severity,
        List<ReportTargetType> applicableTargetTypes,
        BigDecimal confidenceThreshold,
        Boolean active
) {
}
