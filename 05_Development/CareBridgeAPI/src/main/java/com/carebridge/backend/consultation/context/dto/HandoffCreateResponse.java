package com.carebridge.backend.consultation.context.dto;

import java.time.Instant;
import java.util.UUID;

public record HandoffCreateResponse(
        UUID consultationRequestId,
        String requestStatus,
        boolean replayed,
        Instant sharedAt,
        HandoffContextResponse context) {
}
