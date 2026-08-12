package com.carebridge.backend.emergency.dto.response;

import java.time.Instant;
import java.util.UUID;

public record LocationShareResponse(
        UUID shareId,
        int recipientCount,
        int pushDeliveredCount,
        Instant sharedAt) {
}
