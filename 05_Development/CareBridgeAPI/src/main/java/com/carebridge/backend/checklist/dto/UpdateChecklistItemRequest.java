package com.carebridge.backend.checklist.dto;

import com.carebridge.backend.checklist.entity.ChecklistCategory;
import jakarta.validation.constraints.Size;

public record UpdateChecklistItemRequest(

        @Size(max = 500, message = "CHECKLIST-002: itemText exceeds 500 characters")
        String itemText,

        ChecklistCategory category,

        Integer itemOrder
) {}
