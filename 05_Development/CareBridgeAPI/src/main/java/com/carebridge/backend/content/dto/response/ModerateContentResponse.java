package com.carebridge.backend.content.dto.response;

import com.carebridge.backend.content.entity.ModerationActionType;
import com.carebridge.backend.content.entity.ReportTargetType;
import java.time.Instant;
import java.util.UUID;

public record ModerateContentResponse(
        UUID actionId,
        UUID targetId,
        ReportTargetType targetType,
        ModerationActionType actionType,
        UUID moderatorUserId,
        String reason,
        Instant actionAt,
        String resultingStatus
) {}
