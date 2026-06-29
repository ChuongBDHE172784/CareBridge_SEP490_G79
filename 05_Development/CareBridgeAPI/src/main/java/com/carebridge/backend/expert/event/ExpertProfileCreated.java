package com.carebridge.backend.expert.event;

import java.time.Instant;
import java.util.UUID;

public record ExpertProfileCreated(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        String version,
        ExpertProfileCreatedPayload payload,
        ExpertProfileCreatedMetadata metadata
) {
    public static final String EVENT_TYPE = "ExpertProfileCreated";

    public ExpertProfileCreated(UUID expertProfileId, UUID userId) {
        this(
                UUID.randomUUID(),
                EVENT_TYPE,
                Instant.now(),
                "1.0",
                new ExpertProfileCreatedPayload(expertProfileId, userId),
                new ExpertProfileCreatedMetadata("expert", "ExpertProfileService")
        );
    }
}

record ExpertProfileCreatedPayload(UUID expertProfileId, UUID userId) {
}

record ExpertProfileCreatedMetadata(String domain, String publisher) {
}
