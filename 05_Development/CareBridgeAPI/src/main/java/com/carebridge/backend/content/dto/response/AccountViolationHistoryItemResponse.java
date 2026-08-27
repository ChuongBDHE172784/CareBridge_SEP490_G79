package com.carebridge.backend.content.dto.response;

import com.carebridge.backend.content.entity.ModerationActionType;
import java.time.Instant;
import java.util.UUID;

/** A read-only account enforcement record for the moderator violation history. */
public record AccountViolationHistoryItemResponse(
        UUID actionId,
        UUID targetUserId,
        String targetUserName,
        UUID moderatorUserId,
        String moderatorName,
        ModerationActionType actionType,
        String reason,
        Instant expiresAt,
        UUID reportId,
        Instant actionAt
) {}
