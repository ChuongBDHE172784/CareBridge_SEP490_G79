package com.carebridge.backend.checklist.distribution;

import com.carebridge.backend.checklist.model.ChecklistRecipientRole;
import java.util.UUID;

public record ChecklistDistributionRecipient(
        UUID userId,
        ChecklistRecipientRole role,
        boolean acceptedMembership,
        boolean checklistView,
        boolean checklistComplete) {
}
