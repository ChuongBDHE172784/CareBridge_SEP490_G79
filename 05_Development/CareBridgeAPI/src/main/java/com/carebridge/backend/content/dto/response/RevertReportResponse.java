package com.carebridge.backend.content.dto.response;

import com.carebridge.backend.content.entity.ReportStatus;
import com.carebridge.backend.content.entity.ReportTargetType;
import java.time.Instant;
import java.util.UUID;

// CB-MOD-IMP-015: response of POST /reports/{reportId}/revert. reportStatus is always "PENDING"
// on success. undoActionId/resultingStatus are null when the reverted report was DISMISSED (no
// ModerationAction was ever created for that outcome — BR-MOD-010).
public record RevertReportResponse(
        UUID reportId,
        ReportStatus reportStatus,
        UUID revertedByModeratorId,
        Instant revertedAt,
        UUID undoActionId,
        ReportTargetType targetType,
        UUID targetId,
        String resultingStatus) {
}
