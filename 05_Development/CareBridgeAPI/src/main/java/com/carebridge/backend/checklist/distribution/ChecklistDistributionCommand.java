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
        UUID correlationId,
        Long gestationalDatingRevision,
        ChecklistCadenceMetadata cadence) {

    public ChecklistDistributionCommand withCadence(ChecklistCadenceMetadata replacement) {
        return new ChecklistDistributionCommand(templateLineageId, templateVersionId, careGroupId,
                careGroupOwnerUserId, contextType, contextId, contextOwnerUserId, stage, substage,
                lifecycleDates, effectiveDate, timezone, recipients, items, correlationId,
                gestationalDatingRevision, replacement);
    }

    /** Compatibility constructor for non-pregnancy and legacy callers. */
    public ChecklistDistributionCommand(
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
        this(templateLineageId, templateVersionId, careGroupId, careGroupOwnerUserId,
                contextType, contextId, contextOwnerUserId, stage, substage, lifecycleDates,
                effectiveDate, timezone, recipients, items, correlationId, null);
    }

    /** Compatibility constructor for existing non-cadence callers. */
    public ChecklistDistributionCommand(
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
            UUID correlationId,
            Long gestationalDatingRevision) {
        this(templateLineageId, templateVersionId, careGroupId, careGroupOwnerUserId,
                contextType, contextId, contextOwnerUserId, stage, substage, lifecycleDates,
                effectiveDate, timezone, recipients, items, correlationId,
                gestationalDatingRevision, null);
    }
}
