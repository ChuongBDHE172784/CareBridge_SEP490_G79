package com.carebridge.backend.consultation.event;

import java.time.Instant;
import java.util.UUID;

public record ConsultationRequestDomainEvent(
        String eventType,
        UUID requestId,
        UUID actorUserId,
        String actorType,
        Instant occurredAt) {
}
