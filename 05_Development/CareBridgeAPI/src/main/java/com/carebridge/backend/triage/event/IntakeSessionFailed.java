package com.carebridge.backend.triage.event;

import java.time.Instant;
import java.util.UUID;

public record IntakeSessionFailed(
        UUID eventId,
        UUID sessionId,
        UUID userId,
        String reason,
        Instant failedAt
) {}
