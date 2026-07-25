package com.carebridge.backend.consultation.context.dto;

import java.time.Instant;
import java.util.UUID;

public record SharedCitationResponse(
        UUID evidenceSourceId,
        String organization,
        String baseUrl,
        Instant reviewedAt) {
}
