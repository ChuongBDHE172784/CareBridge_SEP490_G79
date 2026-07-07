package com.carebridge.backend.checklist.dto;

import com.carebridge.backend.checklist.entity.ChecklistCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record AddChecklistItemRequest(

        UUID journeyId,

        UUID babyId,

        @NotBlank(message = "CHECKLIST-001: itemText is required")
        @Size(max = 500, message = "CHECKLIST-002: itemText exceeds 500 characters")
        String itemText,

        ChecklistCategory category,

        Integer itemOrder
) {}
