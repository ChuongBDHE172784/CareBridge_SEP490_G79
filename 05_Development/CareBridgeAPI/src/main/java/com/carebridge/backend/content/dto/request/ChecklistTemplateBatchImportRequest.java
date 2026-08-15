package com.carebridge.backend.content.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ChecklistTemplateBatchImportRequest(
        @NotEmpty
        @Size(max = 100, message = "templates must contain at most 100 entries")
        List<@NotNull @Valid ChecklistTemplateBatchImportRowRequest> templates
) {
}
