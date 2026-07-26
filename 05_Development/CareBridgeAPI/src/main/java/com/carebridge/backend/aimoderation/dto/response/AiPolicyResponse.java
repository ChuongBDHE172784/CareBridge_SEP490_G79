package com.carebridge.backend.aimoderation.dto.response;

import com.carebridge.backend.aimoderation.entity.AiPolicySeverity;
import com.carebridge.backend.aimoderation.entity.AiViolationCategory;
import com.carebridge.backend.content.entity.ReportCategory;
import com.carebridge.backend.content.entity.ReportTargetType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AiPolicyResponse(
        UUID id,
        String policyCode,
        String name,
        String detectionGuidance,
        AiViolationCategory violationCategory,
        ReportCategory reportCategory,
        AiPolicySeverity severity,
        List<ReportTargetType> applicableTargetTypes,
        BigDecimal confidenceThreshold,
        boolean active,
        boolean systemDefault,
        int version,
        Instant createdAt,
        Instant updatedAt
) {
}
