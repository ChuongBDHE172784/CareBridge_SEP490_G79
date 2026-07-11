package com.carebridge.backend.content.dto.response;

import com.carebridge.backend.content.entity.ReportTargetType;
import java.time.Instant;
import java.util.UUID;

// CB-MOD-IMP-008: full (non-truncated) body of a CommunityQuestion/CommunityAnswer, regardless of
// its current status — distinct from contentPreview (ContentPreviewService, max 200 chars) used by
// the queue/pending-content/history list DTOs.
public record ModerationContentDetailResponse(
        UUID targetId,
        ReportTargetType targetType,
        UUID authorId,
        String authorName,
        String title,
        String body,
        String status,
        boolean anonymous,
        UUID questionId,
        String questionTitle,
        Instant createdAt,
        Instant updatedAt) {
}
