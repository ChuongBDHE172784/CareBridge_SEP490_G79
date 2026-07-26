package com.carebridge.backend.content.mapper;

import com.carebridge.backend.content.dto.response.ModerationQueueItemResponse;
import com.carebridge.backend.content.dto.response.ModerationQueueResponse;
import com.carebridge.backend.content.dto.response.PendingContentItemResponse;
import com.carebridge.backend.content.entity.ContentReport;
import com.carebridge.backend.content.entity.ReportTargetType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
public class ModerationMapper {

    private static final int MAX_PREVIEW_LENGTH = 200;
    private static final String ELLIPSIS = "...";

    public ModerationQueueItemResponse toQueueItemResponse(
            ContentReport report, String rawPreview, long reportCount) {
        return new ModerationQueueItemResponse(
                report.getId(),
                report.getTargetId(),
                report.getTargetType(),
                report.getReporterUserId(),
                truncate(rawPreview),
                reportCount,
                report.getCreatedAt(),
                buildReason(report),
                report.getStatus(),
                report.getReportSource(),
                report.getResolvedAt(),
                report.getAssignedModeratorId(),
                report.getRevertedAt(),
                report.getRevertedBy()
        );
    }

    public ModerationQueueResponse toQueueResponse(
            Page<ContentReport> page, List<ModerationQueueItemResponse> items) {
        return new ModerationQueueResponse(
                items,
                page.getTotalElements(),
                page.getNumber(),
                page.getSize()
        );
    }

    // CB-MOD-IMP-004 (Pending Content Queue): preview is already truncated by
    // ContentPreviewService (§9); truncate() is applied again defensively, matching
    // the existing double-truncation convention in toQueueItemResponse() above.
    public PendingContentItemResponse toPendingContentItemResponse(
            UUID targetId, ReportTargetType targetType, String rawPreview, Instant createdAt) {
        return new PendingContentItemResponse(targetId, targetType, truncate(rawPreview), createdAt);
    }

    private String truncate(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        if (text.length() <= MAX_PREVIEW_LENGTH) {
            return text;
        }
        return text.substring(0, MAX_PREVIEW_LENGTH - ELLIPSIS.length()) + ELLIPSIS;
    }

    private String buildReason(ContentReport report) {
        if (report.getCategory() != null && !report.getCategory().isBlank()) {
            return report.getCategory();
        }
        return report.getDescription() != null ? report.getDescription() : "";
    }
}
