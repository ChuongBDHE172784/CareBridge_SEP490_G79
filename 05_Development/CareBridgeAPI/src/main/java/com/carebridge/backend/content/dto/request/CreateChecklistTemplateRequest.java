package com.carebridge.backend.content.dto.request;

import com.carebridge.backend.checklist.model.ChecklistRecipientRole;
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
        Set<ChecklistRecipientRole> recipientRoles,
        ContentStage stage,
        @Valid ChecklistSubstageRequest substage,
        // §11.2: empty/null both valid — an empty draft shell is allowed (matches existing seed data)
        @Valid List<ChecklistItemRequest> items
) {

    public CreateChecklistTemplateRequest(
            String name,
            String description,
            Set<ChecklistRecipientRole> recipientRoles,
            ContentStage stage,
            ChecklistSubstageRequest substage,
            List<ChecklistItemRequest> items) {
        this(name, description, ChecklistTemplateType.MANDATORY, recipientRoles, stage, substage, items);
    }

    /** Legacy constructor retained for existing callers while V2 metadata is adopted. */
    public CreateChecklistTemplateRequest(
            String name,
            String description,
            ContentStage stage,
            List<ChecklistItemRequest> items) {
        this(name, description, ChecklistTemplateType.MANDATORY,
                Set.of(ChecklistRecipientRole.MOTHER), stage, null, items);
    }
}
