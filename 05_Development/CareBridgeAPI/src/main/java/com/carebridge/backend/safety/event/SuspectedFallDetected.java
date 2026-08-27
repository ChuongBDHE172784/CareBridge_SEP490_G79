package com.carebridge.backend.safety.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SuspectedFallDetected(
        UUID eventId,
        UUID userId,
        UUID safetyEventId,
        String eventType,
        double magnitude,
        BigDecimal latitude,
        BigDecimal longitude,
        Instant detectedAt
) {}
