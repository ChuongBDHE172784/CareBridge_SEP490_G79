package com.carebridge.backend.journey.event;

import com.carebridge.backend.journey.entity.JourneyStatus;
import com.carebridge.backend.journey.entity.JourneyTransitionType;
import com.carebridge.backend.journey.entity.JourneyType;

import java.time.Instant;
import java.util.UUID;

public record MotherJourneyTransitioned(
        UUID eventId,
        UUID journeyId,
        UUID ownerUserId,
        JourneyTransitionType eventType,
        JourneyType journeyType,
        JourneyStatus status,
        long journeyVersion,
        Instant occurredAt,
        UUID correlationId) {
}
