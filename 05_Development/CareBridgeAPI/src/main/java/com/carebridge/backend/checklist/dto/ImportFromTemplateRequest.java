package com.carebridge.backend.checklist.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record ImportFromTemplateRequest(

        UUID journeyId,

        UUID babyId,

        @NotNull(message = "CHECKLIST-001: templateItemIds is required")
        @Size(min = 1, max = 50, message = "CHECKLIST-001: templateItemIds must contain 1 to 50 entries")
        List<@NotNull(message = "CHECKLIST-001: templateItemIds cannot contain null") UUID> templateItemIds
) {}
