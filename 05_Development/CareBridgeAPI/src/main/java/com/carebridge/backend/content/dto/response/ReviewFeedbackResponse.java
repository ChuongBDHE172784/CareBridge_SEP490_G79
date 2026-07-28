package com.carebridge.backend.content.dto.response;

import java.time.Instant;
import java.util.UUID;

public record ReviewFeedbackResponse(
        String reason,
        Instant requestedAt,
        UUID requestedBy,
        Integer versionNo) {
}

