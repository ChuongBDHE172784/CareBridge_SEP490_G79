package com.carebridge.backend.checklist.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record ImportFromTemplateRequest(

        UUID journeyId,

        UUID babyId,

        @NotNull(message = "CHECKLIST-001: templateItemIds is required")
        List<UUID> templateItemIds
) {}
