package com.carebridge.backend.content.dto.response;

import com.carebridge.backend.content.entity.ContentStatus;
import java.time.Instant;
import java.util.UUID;

public record UnpublishResponse(UUID id, ContentStatus previousStatus, ContentStatus newStatus,
        Instant publishedAt, UUID unpublishedByAdminId, String reason, Instant unpublishedAt) {}
