package com.carebridge.backend.aimoderation.dto.request;

import com.carebridge.backend.aimoderation.entity.AiPolicySeverity;
import com.carebridge.backend.aimoderation.entity.AiViolationCategory;
import com.carebridge.backend.content.entity.ReportCategory;
import com.carebridge.backend.content.entity.ReportTargetType;
import com.carebridge.backend.aimoderation.dto.PolicyReferenceFile;
import com.carebridge.backend.aimoderation.dto.PolicyReferenceLink;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record CreateAiPolicyRequest(
        @NotBlank @Pattern(regexp = "[A-Z0-9_]{3,60}") String policyCode,
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Size(max = 3000) String detectionGuidance,
        @NotNull AiViolationCategory violationCategory,
        @NotNull ReportCategory reportCategory,
        @NotNull AiPolicySeverity severity,
        @NotEmpty List<ReportTargetType> applicableTargetTypes,
        @NotNull BigDecimal confidenceThreshold,
        Boolean active,
        List<PolicyReferenceLink> referenceLinks,
        List<PolicyReferenceFile> referenceFiles
) {
}
