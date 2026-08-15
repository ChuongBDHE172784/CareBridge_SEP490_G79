package com.carebridge.backend.content.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ChecklistTemplateBatchImportRowRequest(
        @NotNull @Positive Integer rowIndex,
        @NotBlank @Size(max = 100) String checklistCode,
        @NotNull @Valid CreateChecklistTemplateRequest template
) {
}
