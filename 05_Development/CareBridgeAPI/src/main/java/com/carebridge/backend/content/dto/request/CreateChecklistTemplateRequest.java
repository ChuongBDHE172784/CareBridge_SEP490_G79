package com.carebridge.backend.content.dto.request;

import com.carebridge.backend.checklist.model.ChecklistRecipientRole;
import com.carebridge.backend.checklist.model.ChecklistCareContextType;
import com.carebridge.backend.checklist.model.ChecklistMaterializationPolicy;
import com.carebridge.backend.checklist.model.ChecklistScheduleEndMode;
import com.carebridge.backend.checklist.model.ChecklistScheduleType;
import com.carebridge.backend.checklist.model.ChecklistWeekBoundaryRule;
import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.entity.ChecklistTemplateType;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Set;

public record CreateChecklistTemplateRequest(
        @JsonAlias("title") @NotBlank @Size(max = 200) String name,
        @Size(max = 2000) String description,
        ChecklistTemplateType templateType,
        /** 1 = legacy target-bearing authoring; 2 = recommendation-only targetless authoring. */
        Short checklistContractVersion,
        Set<ChecklistRecipientRole> recipientRoles,
        ContentStage stage,
        @Valid ChecklistSubstageRequest substage,
        // §11.2: empty/null both valid — an empty draft shell is allowed (matches existing seed data)
        List<@Valid ChecklistItemRequest> items,
        @JsonAlias("sequencePosition") Integer displayOrder,
        ChecklistScheduleType scheduleType,
        ChecklistMaterializationPolicy materializationPolicy,
        String scheduleGroupKey,
        ChecklistCareContextType scheduleContextType,
        ChecklistScheduleEndMode scheduleEndMode,
        ChecklistWeekBoundaryRule weekBoundaryRule
) {

    public CreateChecklistTemplateRequest(
            String name,
            String description,
            Set<ChecklistRecipientRole> recipientRoles,
            ContentStage stage,
            ChecklistSubstageRequest substage,
            List<ChecklistItemRequest> items) {
        this(name, description, ChecklistTemplateType.MANDATORY, null, recipientRoles, stage, substage, items, 0,
                null, null, null, null, null, null);
    }

    /** Legacy constructor retained for existing callers while V2 metadata is adopted. */
    public CreateChecklistTemplateRequest(
            String name,
            String description,
            ContentStage stage,
            List<ChecklistItemRequest> items) {
        this(name, description, ChecklistTemplateType.MANDATORY, null,
                Set.of(ChecklistRecipientRole.MOTHER), stage, null, items, 0,
                null, null, null, null, null, null);
    }

    public CreateChecklistTemplateRequest(
            String name,
            String description,
            ChecklistTemplateType templateType,
            Short checklistContractVersion,
            Set<ChecklistRecipientRole> recipientRoles,
            ContentStage stage,
            ChecklistSubstageRequest substage,
            List<ChecklistItemRequest> items) {
        this(name, description, templateType, checklistContractVersion,
                recipientRoles, stage, substage, items, 0,
                null, null, null, null, null, null);
    }

    /** Compatibility constructor retaining the pre-cadence canonical shape. */
    public CreateChecklistTemplateRequest(
            String name,
            String description,
            ChecklistTemplateType templateType,
            Short checklistContractVersion,
            Set<ChecklistRecipientRole> recipientRoles,
            ContentStage stage,
            ChecklistSubstageRequest substage,
            List<ChecklistItemRequest> items,
            Integer displayOrder) {
        this(name, description, templateType, checklistContractVersion, recipientRoles, stage, substage, items,
                displayOrder, null, null, null, null, null, null);
    }

    /** Compatibility constructor retaining the pre-contract-version argument order. */
    public CreateChecklistTemplateRequest(
            String name,
            String description,
            ChecklistTemplateType templateType,
            Set<ChecklistRecipientRole> recipientRoles,
            ContentStage stage,
            ChecklistSubstageRequest substage,
            List<ChecklistItemRequest> items) {
        this(name, description, templateType, null, recipientRoles, stage, substage, items, 0,
                null, null, null, null, null, null);
    }

    /** Compatibility constructor retaining the pre-contract-version argument order. */
    public CreateChecklistTemplateRequest(
            String name,
            String description,
            ChecklistTemplateType templateType,
            Set<ChecklistRecipientRole> recipientRoles,
            ContentStage stage,
            ChecklistSubstageRequest substage,
            List<ChecklistItemRequest> items,
            Integer displayOrder) {
        this(name, description, templateType, null, recipientRoles, stage, substage, items, displayOrder,
                null, null, null, null, null, null);
    }
}
