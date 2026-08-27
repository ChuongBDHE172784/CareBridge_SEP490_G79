package com.carebridge.backend.emergency.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** A newly confirmed incident needs a throttled update on an active emergency. */
public record EmergencySessionRealertRequested(
        UUID eventId,
        UUID sessionId,
        UUID userId,
        String triggerSource,
        BigDecimal latitude,
        BigDecimal longitude,
        Instant occurredAt) {
}
