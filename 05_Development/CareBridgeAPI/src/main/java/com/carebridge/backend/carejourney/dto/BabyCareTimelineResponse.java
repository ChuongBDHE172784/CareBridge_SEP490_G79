package com.carebridge.backend.carejourney.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BabyCareTimelineResponse(UUID babyId, List<Event> events, String nextCursor) {
    public record Event(String sourceType, UUID sourceId, Instant occurredAt, String displayLabel) {}
}
