package com.carebridge.backend.triage.dto.request;

import com.carebridge.backend.triage.entity.RedFlagSeverity;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.lang.Nullable;

public record RedFlagRuleFilter(
        @Nullable RedFlagSeverity severity,
        @Nullable Boolean isActive,
        @Min(0) int page,
        @Min(1) @Max(50) int size) {

    public RedFlagRuleFilter {
        if (size == 0) {
            size = 20;
        }
    }
}
