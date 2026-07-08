package com.carebridge.backend.content.dto.response;

import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.entity.ContentStatus;
import com.carebridge.backend.content.entity.ContentType;
import java.time.Instant;
import java.util.UUID;

public record UpdateContentResponse(
        UUID id,
        ContentType type,
        String title,
        String body,
        ContentStage stage,
        UUID topicId,
        ContentStatus status,
        Integer versionNo,
        Instant updatedAt
) {}
