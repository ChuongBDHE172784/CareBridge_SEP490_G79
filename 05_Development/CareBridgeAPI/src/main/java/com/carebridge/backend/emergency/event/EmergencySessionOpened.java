package com.carebridge.backend.emergency.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record EmergencySessionOpened(
        UUID eventId,
        UUID sessionId,
        UUID userId,
        String triggerSource,
        BigDecimal latitude,
        BigDecimal longitude,
        Instant openedAt
) {}
