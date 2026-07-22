package com.carebridge.backend.content.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChecklistItemRequest(
        @NotBlank @Size(max = 500) String itemText,
        @NotNull Integer order,
        @NotNull Boolean isRequired
) {}
