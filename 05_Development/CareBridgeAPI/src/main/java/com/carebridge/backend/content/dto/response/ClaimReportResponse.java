package com.carebridge.backend.content.dto.response;

import com.carebridge.backend.content.entity.ReportStatus;
import java.time.Instant;
import java.util.UUID;

/** Result of claim (status=IN_REVIEW) or release (status=PENDING, moderator/claimedAt null). */
public record ClaimReportResponse(
        UUID reportId,
        ReportStatus status,
        UUID assignedModeratorId,
        Instant claimedAt
) {
}
