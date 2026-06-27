package com.carebridge.backend.audit.dto.response;

import java.time.Instant;
import java.util.UUID;

public record SecurityEventResponse(
    Long id,
    String eventType,
    UUID userId,
    String ipAddress,
    String severity,
    String status,
    String details,
    UUID correlationId,
    UUID reviewedBy,
    Instant reviewedAt,
    Instant occurredAt
) {}
