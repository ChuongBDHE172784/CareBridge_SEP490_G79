package com.carebridge.backend.safety.event;

import java.time.Instant;
import java.util.UUID;

public record FallDetectionDisabled(
        UUID eventId,
        UUID userId,
        UUID sessionId,
        Instant timestamp
) {}
