package com.carebridge.backend.content.dto.request;

import com.carebridge.backend.checklist.model.ChecklistTargetSubject;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record ChecklistItemRequest(
        UUID id,
        @NotBlank @Size(max = 500) String itemText,
        @NotNull Integer order,
        @NotNull Boolean isRequired,
        ChecklistTargetSubject targetSubject
) {
    public ChecklistItemRequest(String itemText, Integer order, Boolean isRequired) {
        this(null, itemText, order, isRequired, ChecklistTargetSubject.MOTHER);
    }

    public ChecklistItemRequest(UUID id, String itemText, Integer order, Boolean isRequired) {
        this(id, itemText, order, isRequired, ChecklistTargetSubject.MOTHER);
    }
}
