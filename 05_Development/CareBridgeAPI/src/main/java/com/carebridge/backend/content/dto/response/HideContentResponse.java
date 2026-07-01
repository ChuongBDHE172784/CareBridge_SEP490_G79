package com.carebridge.backend.content.dto.response;

import com.carebridge.backend.content.entity.ContentStatus;
import java.time.Instant;
import java.util.UUID;

public record HideContentResponse(
        UUID id,
        ContentStatus previousStatus,
        ContentStatus newStatus,
        String reason,
        UUID hiddenByAdminId,
        Instant hiddenAt
) {}
