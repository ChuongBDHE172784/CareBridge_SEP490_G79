package com.carebridge.backend.aimoderation.dto.request;

import com.carebridge.backend.content.entity.ReportTargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Sandbox classification of admin-supplied sample text. Never persisted, never audited verbatim. */
public record AiPolicyTestRequest(
        @NotNull ReportTargetType targetType,
        @NotBlank @Size(max = 5000) String sampleText
) {
}
