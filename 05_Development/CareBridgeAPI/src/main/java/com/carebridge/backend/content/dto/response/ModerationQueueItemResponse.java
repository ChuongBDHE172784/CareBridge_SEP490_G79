package com.carebridge.backend.content.dto.response;

import com.carebridge.backend.content.entity.ReportStatus;
import com.carebridge.backend.content.entity.ReportTargetType;
import java.time.Instant;
import java.util.UUID;

public record ModerationQueueItemResponse(
        UUID id,
        ReportTargetType targetType,
        String contentPreview,
        long reportCount,
        Instant reportedAt,
        String reportReason,
        ReportStatus status
) {}
