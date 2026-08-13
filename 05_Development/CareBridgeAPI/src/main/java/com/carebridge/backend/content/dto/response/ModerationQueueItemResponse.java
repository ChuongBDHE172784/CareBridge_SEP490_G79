package com.carebridge.backend.content.dto.response;

import com.carebridge.backend.content.entity.CasePriority;
import com.carebridge.backend.content.entity.ReportStatus;
import com.carebridge.backend.content.entity.ReportTargetType;
import com.carebridge.backend.content.entity.ReportSource;
import java.time.Instant;
import java.util.UUID;

// status determines whether the frontend shows actionable or read-only report details.
// revertedAt/revertedBy remain response-only legacy audit metadata for historical records.
public record ModerationQueueItemResponse(
        UUID id,
        UUID targetId,
        ReportTargetType targetType,
        UUID reporterUserId,
        String contentPreview,
        long reportCount,
        Instant reportedAt,
        String reportReason,
        ReportStatus status,
        ReportSource reportSource,
        CasePriority priority,
        Instant claimedAt,
        Instant resolvedAt,
        UUID assignedModeratorId,
        Instant revertedAt,
        UUID revertedBy,
        UUID authorId,
        String authorName,
        String targetTitle
) {
    public ModerationQueueItemResponse(
            UUID id,
            UUID targetId,
            ReportTargetType targetType,
            UUID reporterUserId,
            String contentPreview,
            long reportCount,
            Instant reportedAt,
            String reportReason,
            ReportStatus status,
            ReportSource reportSource,
            CasePriority priority,
            Instant claimedAt,
            Instant resolvedAt,
            UUID assignedModeratorId,
            Instant revertedAt,
            UUID revertedBy) {
        this(id, targetId, targetType, reporterUserId, contentPreview, reportCount, reportedAt, reportReason, status,
                reportSource, priority, claimedAt, resolvedAt, assignedModeratorId, revertedAt, revertedBy,
                null, null, null);
    }

    public ModerationQueueItemResponse(
            UUID id,
            ReportTargetType targetType,
            String contentPreview,
            long reportCount,
            Instant reportedAt,
            String reportReason,
            ReportStatus status) {
        this(id, null, targetType, null, contentPreview, reportCount, reportedAt, reportReason, status,
                ReportSource.USER, CasePriority.NORMAL, null, null, null, null, null,
                null, null, null);
    }
}
