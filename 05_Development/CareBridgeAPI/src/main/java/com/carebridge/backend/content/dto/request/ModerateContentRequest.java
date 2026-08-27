package com.carebridge.backend.content.dto.request;

import com.carebridge.backend.content.entity.ModerationActionType;
import com.carebridge.backend.content.entity.ReportTargetType;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ModerateContentRequest(
        @NotNull UUID targetId,
        @NotNull ReportTargetType targetType,
        @NotNull ModerationActionType actionType,
        String reason
) {}
