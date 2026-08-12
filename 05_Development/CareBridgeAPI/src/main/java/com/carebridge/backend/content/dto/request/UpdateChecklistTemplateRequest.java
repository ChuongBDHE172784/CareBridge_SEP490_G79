package com.carebridge.backend.content.dto.request;

import com.carebridge.backend.checklist.model.ChecklistRecipientRole;
import com.carebridge.backend.content.entity.ChecklistTemplateStatus;
import com.carebridge.backend.content.entity.ChecklistTemplateType;
import com.carebridge.backend.content.entity.ContentStage;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Set;

public record UpdateChecklistTemplateRequest(
        @JsonAlias("title") @NotBlank @Size(max = 200) String name,
        @Size(max = 2000) String description,
        ChecklistTemplateType templateType,
        /** 1 = legacy target-bearing authoring; 2 = recommendation-only targetless authoring. */
        Short checklistContractVersion,
        Set<ChecklistRecipientRole> recipientRoles,
        ContentStage stage,
        @Valid ChecklistSubstageRequest substage,
        @NotNull ChecklistTemplateStatus status,
        // null = keep existing items unchanged; [] = clear all items; non-empty = full replace
        @Valid List<ChecklistItemRequest> items,
        @JsonAlias("sequencePosition") Integer displayOrder
) {

    public UpdateChecklistTemplateRequest(
            String name,
            String description,
            Set<ChecklistRecipientRole> recipientRoles,
            ContentStage stage,
            ChecklistSubstageRequest substage,
            ChecklistTemplateStatus status,
            List<ChecklistItemRequest> items) {
        this(name, description, ChecklistTemplateType.MANDATORY, null,
                recipientRoles, stage, substage, status, items, 0);
    }

    /** Legacy constructor retained for existing callers while V2 metadata is adopted. */
    public UpdateChecklistTemplateRequest(
            String name,
            String description,
            ContentStage stage,
            ChecklistTemplateStatus status,
            List<ChecklistItemRequest> items) {
        this(name, description, ChecklistTemplateType.MANDATORY, null,
                Set.of(ChecklistRecipientRole.MOTHER), stage, null, status, items, 0);
    }

    public UpdateChecklistTemplateRequest(
            String name,
            String description,
            ChecklistTemplateType templateType,
            Short checklistContractVersion,
            Set<ChecklistRecipientRole> recipientRoles,
            ContentStage stage,
            ChecklistSubstageRequest substage,
            ChecklistTemplateStatus status,
            List<ChecklistItemRequest> items) {
        this(name, description, templateType, checklistContractVersion,
                recipientRoles, stage, substage, status, items, 0);
    }

    /** Compatibility constructor retaining the pre-contract-version argument order. */
    public UpdateChecklistTemplateRequest(
            String name,
            String description,
            ChecklistTemplateType templateType,
            Set<ChecklistRecipientRole> recipientRoles,
            ContentStage stage,
            ChecklistSubstageRequest substage,
            ChecklistTemplateStatus status,
            List<ChecklistItemRequest> items) {
        this(name, description, templateType, null,
                recipientRoles, stage, substage, status, items, 0);
    }

    /** Compatibility constructor retaining the pre-contract-version argument order. */
    public UpdateChecklistTemplateRequest(
            String name,
            String description,
            ChecklistTemplateType templateType,
            Set<ChecklistRecipientRole> recipientRoles,
            ContentStage stage,
            ChecklistSubstageRequest substage,
            ChecklistTemplateStatus status,
            List<ChecklistItemRequest> items,
            Integer displayOrder) {
        this(name, description, templateType, null, recipientRoles, stage, substage, status, items, displayOrder);
    }
}
