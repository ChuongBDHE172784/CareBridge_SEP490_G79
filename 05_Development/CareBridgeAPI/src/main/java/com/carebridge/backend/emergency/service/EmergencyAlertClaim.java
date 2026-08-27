package com.carebridge.backend.emergency.service;

import java.time.Instant;
import java.util.UUID;

/** Fence returned by an atomic emergency-alert claim. */
public record EmergencyAlertClaim(
        UUID emergencySessionId,
        long generation,
        UUID fenceToken,
        Instant leaseExpiresAt
) {
}
