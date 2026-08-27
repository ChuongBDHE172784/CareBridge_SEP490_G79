package com.carebridge.backend.health.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Published after a mother's EPDS screening is committed (CB-EPDS-IMP-001).
 *
 * <p>{@code question10Score} is the self-harm item and is carried here <strong>only</strong> as a
 * routing predicate for the escalation decision. It must never reach a family-facing message —
 * see {@code EpdsSeverityPolicy} and TDS ADR-003 / INV-2.
 */
public record EpdsScreeningCompleted(
        UUID observationId,
        UUID journeyId,
        UUID motherUserId,
        int totalScore,
        int question10Score,
        Instant occurredAt
) {
}
