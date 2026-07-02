package com.carebridge.backend.triage.dto.request;

import com.carebridge.backend.triage.entity.RedFlagAction;
import com.carebridge.backend.triage.entity.RedFlagSeverity;
import jakarta.validation.constraints.Size;

// Partial update — null field = no change
public record UpdateRedFlagRuleRequest(
        @Size(max = 255) String keyword,
        RedFlagSeverity severity,
        RedFlagAction action,
        Boolean isActive) {
}
