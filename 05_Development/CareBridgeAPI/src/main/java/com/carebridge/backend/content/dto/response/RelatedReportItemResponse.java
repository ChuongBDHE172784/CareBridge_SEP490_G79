package com.carebridge.backend.content.dto.response;

import com.carebridge.backend.content.entity.ReportSource;
import com.carebridge.backend.content.entity.ReportStatus;
import java.time.Instant;
import java.util.UUID;

/** Privacy-safe report projection for moderator review of reports sharing one target. */
public record RelatedReportItemResponse(
        UUID id,
        String category,
        String reason,
        ReportStatus status,
        ReportSource reportSource,
        Instant reportedAt
) {}
