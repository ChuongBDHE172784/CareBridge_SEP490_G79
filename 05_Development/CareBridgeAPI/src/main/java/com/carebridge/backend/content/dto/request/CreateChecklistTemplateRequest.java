package com.carebridge.backend.content.dto.request;

import com.carebridge.backend.content.entity.ContentStage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateChecklistTemplateRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 2000) String description,
        @NotNull ContentStage stage,
        // §11.2: empty/null both valid — an empty draft shell is allowed (matches existing seed data)
        @Valid List<ChecklistItemRequest> items
) {}
