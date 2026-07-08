package com.carebridge.backend.triage.dto;

import java.util.UUID;

// `details` payload passed to AuditService.log() for red-flag rule create/update/delete
public record RedFlagRuleAuditDetails(
        UUID ruleId,
        String keyword,
        String severity,
        String action,
        Boolean isActive,
        Boolean isSystemDefault,
        String changeType) {
}
