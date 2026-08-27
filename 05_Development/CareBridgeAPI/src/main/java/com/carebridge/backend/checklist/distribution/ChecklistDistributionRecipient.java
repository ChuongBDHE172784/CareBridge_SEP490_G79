package com.carebridge.backend.checklist.distribution;

import com.carebridge.backend.checklist.model.ChecklistRecipientRole;
import java.util.UUID;

public record ChecklistDistributionRecipient(
        UUID userId,
        ChecklistRecipientRole role,
        boolean acceptedMembership,
        boolean checklistView,
        boolean checklistComplete,
        UUID careGroupMemberId,
        Long checklistAccessEpoch) {

    /** Compatibility constructor for callers that do not materialize Family metadata. */
    public ChecklistDistributionRecipient(
            UUID userId,
            ChecklistRecipientRole role,
            boolean acceptedMembership,
            boolean checklistView,
            boolean checklistComplete) {
        this(userId, role, acceptedMembership, checklistView, checklistComplete, null, null);
    }
}
