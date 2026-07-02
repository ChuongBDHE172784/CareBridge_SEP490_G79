package com.carebridge.backend.triage.dto.response;

import com.carebridge.backend.triage.entity.RedFlagAction;
import com.carebridge.backend.triage.entity.RedFlagSeverity;
import java.time.LocalDateTime;
import java.util.UUID;

public record RedFlagRuleResponse(
        UUID id,
        String keyword,
        RedFlagSeverity severity,
        RedFlagAction action,
        boolean isActive,
        boolean isSystemDefault,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
