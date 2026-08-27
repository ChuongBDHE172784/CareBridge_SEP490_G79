package com.carebridge.backend.content.dto.response;

import com.carebridge.backend.content.entity.ReportTargetType;
import java.time.Instant;
import java.util.UUID;

// CB-MOD-IMP-009: response of POST /actions/{actionId}/undo — resultingStatus is always "PENDING"
// (ADR-001, Undo never tries to reconstruct a prior status).
public record UndoModerationActionResponse(
        UUID undoActionId,
        UUID originalActionId,
        UUID targetId,
        ReportTargetType targetType,
        UUID moderatorUserId,
        Instant actionAt,
        String resultingStatus) {
}
