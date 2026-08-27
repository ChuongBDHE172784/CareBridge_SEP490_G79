package com.carebridge.backend.checklist.distribution;

import java.util.List;

/** Aggregate distribution counts plus the truthful outcome of each recipient attempt. */
public record ChecklistDistributionExecutionResult(
        ChecklistDistributionResult total,
        List<ChecklistRecipientDistributionResult> recipients) {

    public ChecklistDistributionExecutionResult {
        recipients = recipients == null ? List.of() : List.copyOf(recipients);
    }
}
