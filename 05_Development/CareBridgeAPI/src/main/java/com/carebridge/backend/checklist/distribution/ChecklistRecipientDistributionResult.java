package com.carebridge.backend.checklist.distribution;

import java.time.LocalDate;
import java.util.UUID;

/** Result attributable to one recipient, or to the command when recipientUserId is null. */
public record ChecklistRecipientDistributionResult(
        UUID recipientUserId,
        LocalDate windowStart,
        LocalDate windowEnd,
        ChecklistDistributionResult result,
        String dispositionCode) {
}
