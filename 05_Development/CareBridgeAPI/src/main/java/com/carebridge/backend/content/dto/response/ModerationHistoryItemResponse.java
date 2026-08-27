package com.carebridge.backend.content.dto.response;

import com.carebridge.backend.content.entity.ModerationActionType;
import com.carebridge.backend.content.entity.ReportTargetType;
import java.time.Instant;
import java.util.UUID;

public record ModerationHistoryItemResponse(
        UUID actionId,
        UUID targetId,
        ReportTargetType targetType,
        ModerationActionType actionType,
        String contentPreview,
        String moderatorName,
        String reason,
        Instant actionAt
) {}
