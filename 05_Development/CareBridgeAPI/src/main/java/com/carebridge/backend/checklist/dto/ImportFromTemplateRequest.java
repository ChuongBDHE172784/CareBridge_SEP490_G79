package com.carebridge.backend.checklist.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record ImportFromTemplateRequest(

        @NotNull(message = "CHECKLIST-001: journeyId is required")
        UUID journeyId,

        UUID babyId,

        @NotEmpty(message = "CHECKLIST-001: templateItemIds is required")
        @Size(max = 100, message = "CHECKLIST-001: templateItemIds must not exceed 100 items")
        List<@NotNull(message = "CHECKLIST-001: templateItemId is required") UUID> templateItemIds
) {}
