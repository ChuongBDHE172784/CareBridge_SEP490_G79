package com.carebridge.backend.content.dto.request;

import com.carebridge.backend.content.entity.ChecklistTemplateStatus;
import com.carebridge.backend.content.entity.ContentStage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UpdateChecklistTemplateRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 2000) String description,
        @NotNull ContentStage stage,
        @NotNull ChecklistTemplateStatus status,
        // null = keep existing items unchanged; [] = clear all items; non-empty = full replace
        @Valid List<ChecklistItemRequest> items
) {}
