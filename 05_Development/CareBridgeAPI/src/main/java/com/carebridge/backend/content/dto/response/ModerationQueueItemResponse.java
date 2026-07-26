package com.carebridge.backend.content.dto.response;

import com.carebridge.backend.content.entity.CasePriority;
import com.carebridge.backend.content.entity.ReportStatus;
import com.carebridge.backend.content.entity.ReportTargetType;
import com.carebridge.backend.content.entity.ReportSource;
import java.time.Instant;
import java.util.UUID;

// resolvedAt/assignedModeratorId/revertedAt/revertedBy are null for a PENDING report; the
// frontend detail pages use their presence to switch a report between an actionable view and a
// read-only "already processed" view with a revert option (CB-MOD-IMP-015 follow-up).
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
        UUID revertedBy
) {
    public ModerationQueueItemResponse(
            UUID id,
            ReportTargetType targetType,
            String contentPreview,
            long reportCount,
            Instant reportedAt,
            String reportReason,
            ReportStatus status) {
        this(id, null, targetType, null, contentPreview, reportCount, reportedAt, reportReason, status,
                ReportSource.USER, CasePriority.NORMAL, null, null, null, null, null);
    }
}
