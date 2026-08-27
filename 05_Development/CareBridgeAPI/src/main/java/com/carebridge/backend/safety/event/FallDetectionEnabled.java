package com.carebridge.backend.safety.event;

import java.time.Instant;
import java.util.UUID;

public record FallDetectionEnabled(
        UUID eventId,
        UUID userId,
        UUID sessionId,
        String sensitivityLevel,
        Instant timestamp
) {}
