package com.carebridge.backend.content.dto.response;

import com.carebridge.backend.content.entity.ModerationActionType;
import com.carebridge.backend.content.entity.ReportStatus;
import java.time.Instant;
import java.util.UUID;

public record ResolveReportResponse(
        UUID reportId,
        ReportStatus reportStatus,
        UUID resolvedByModeratorId,
        Instant resolvedAt,
        UUID actionId,
        ModerationActionType actionType,
        String resultingStatus
) {}
