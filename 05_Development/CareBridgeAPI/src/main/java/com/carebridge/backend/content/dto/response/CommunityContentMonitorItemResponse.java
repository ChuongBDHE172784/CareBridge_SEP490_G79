package com.carebridge.backend.content.dto.response;

import com.carebridge.backend.content.entity.ReportTargetType;
import java.time.Instant;
import java.util.UUID;

/** Read-only moderator projection of content that is currently visible in the community. */
public record CommunityContentMonitorItemResponse(
        UUID targetId,
        ReportTargetType targetType,
        String title,
        String contentPreview,
        UUID authorId,
        String authorName,
        Instant createdAt,
        int imageCount) {
}
