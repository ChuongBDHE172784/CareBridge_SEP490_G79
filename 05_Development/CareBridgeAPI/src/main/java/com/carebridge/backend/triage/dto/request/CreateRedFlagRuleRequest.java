package com.carebridge.backend.triage.dto.request;

import com.carebridge.backend.triage.entity.RedFlagAction;
import com.carebridge.backend.triage.entity.RedFlagSeverity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateRedFlagRuleRequest(
        @NotBlank @Size(max = 255) String keyword,
        @NotNull RedFlagSeverity severity,
        @NotNull RedFlagAction action) {
}
