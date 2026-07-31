package com.carebridge.backend.checklist.distribution;

import com.carebridge.backend.checklist.model.ChecklistCareContextType;
import com.carebridge.backend.content.entity.ContentStage;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

public record ChecklistDistributionCommand(
        UUID templateLineageId,
        UUID templateVersionId,
        UUID careGroupId,
        UUID careGroupOwnerUserId,
        ChecklistCareContextType contextType,
        UUID contextId,
        UUID contextOwnerUserId,
        ContentStage stage,
        ChecklistLifecycleEligibility substage,
        ChecklistLifecycleDates lifecycleDates,
        LocalDate effectiveDate,
        ZoneId timezone,
        List<ChecklistDistributionRecipient> recipients,
        List<ChecklistDistributionItem> items,
        UUID correlationId) {
}
