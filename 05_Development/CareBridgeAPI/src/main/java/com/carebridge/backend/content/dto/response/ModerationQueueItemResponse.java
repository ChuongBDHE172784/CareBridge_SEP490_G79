package com.carebridge.backend.content.dto.response;

import com.carebridge.backend.content.entity.ReportStatus;
import com.carebridge.backend.content.entity.ReportTargetType;
import java.time.Instant;
import java.util.UUID;

public record ModerationQueueItemResponse(
        UUID id,
        UUID targetId,
        ReportTargetType targetType,
        UUID reporterUserId,
        String contentPreview,
        long reportCount,
        Instant reportedAt,
        String reportReason,
        ReportStatus status
) {
    public ModerationQueueItemResponse(
            UUID id,
            ReportTargetType targetType,
            String contentPreview,
            long reportCount,
            Instant reportedAt,
            String reportReason,
            ReportStatus status) {
        this(id, null, targetType, null, contentPreview, reportCount, reportedAt, reportReason, status);
    }
}
