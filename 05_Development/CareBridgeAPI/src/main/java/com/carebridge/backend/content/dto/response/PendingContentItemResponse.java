package com.carebridge.backend.content.dto.response;

import com.carebridge.backend.content.entity.ReportTargetType;
import java.time.Instant;
import java.util.UUID;

public record PendingContentItemResponse(
        UUID targetId,
        ReportTargetType targetType,
        String contentPreview,
        Instant createdAt
) {}
