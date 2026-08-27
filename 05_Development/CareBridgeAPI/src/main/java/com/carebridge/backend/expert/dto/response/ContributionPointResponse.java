package com.carebridge.backend.expert.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record ContributionPointResponse(
        UUID pointRecordId,
        Integer points,
        String reason,
        String sourceType,
        LocalDateTime recordedAt) {
}
