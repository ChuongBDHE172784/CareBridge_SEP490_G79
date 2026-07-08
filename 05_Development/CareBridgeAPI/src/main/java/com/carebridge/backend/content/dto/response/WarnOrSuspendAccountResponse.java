package com.carebridge.backend.content.dto.response;

import com.carebridge.backend.content.entity.ModerationActionType;
import java.time.Instant;
import java.util.UUID;

public record WarnOrSuspendAccountResponse(
        UUID actionId,
        UUID targetUserId,
        ModerationActionType actionType,
        UUID moderatorUserId,
        String reason,
        Instant actionAt,
        Instant expiresAt,
        boolean accountSuspended
) {}
