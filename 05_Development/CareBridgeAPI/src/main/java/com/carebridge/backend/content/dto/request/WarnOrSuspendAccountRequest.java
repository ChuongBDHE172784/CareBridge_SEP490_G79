package com.carebridge.backend.content.dto.request;

import com.carebridge.backend.content.entity.ModerationActionType;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record WarnOrSuspendAccountRequest(
        @NotNull UUID targetUserId,
        @NotNull ModerationActionType actionType,
        String reason,
        Instant expiresAt
) {}
