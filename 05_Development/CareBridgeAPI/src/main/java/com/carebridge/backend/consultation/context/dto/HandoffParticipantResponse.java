package com.carebridge.backend.consultation.context.dto;

import java.time.Instant;
import java.util.UUID;

public record HandoffParticipantResponse(
        UUID consultationRequestId,
        String requestStatus,
        Instant sharedAt,
        HandoffContextResponse context) {
}
