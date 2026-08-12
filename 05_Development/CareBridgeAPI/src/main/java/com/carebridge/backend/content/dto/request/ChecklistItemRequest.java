package com.carebridge.backend.content.dto.request;

import com.carebridge.backend.checklist.model.ChecklistTargetSubject;
import com.carebridge.backend.checklist.model.ChecklistSupportFunction;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record ChecklistItemRequest(
        UUID id,
        @NotBlank @Size(max = 500) String itemText,
        @NotNull Integer order,
        /** V1 requires this flag; V2 recommendation leaves intentionally omit it. */
        Boolean isRequired,
        ChecklistTargetSubject targetSubject,
        @Size(max = 4000) String description,
        ChecklistSupportFunction supportFunction
) {
    public ChecklistItemRequest(String itemText, Integer order, Boolean isRequired) {
        this(null, itemText, order, isRequired, ChecklistTargetSubject.MOTHER, null, null);
    }

    public ChecklistItemRequest(UUID id, String itemText, Integer order, Boolean isRequired) {
        this(id, itemText, order, isRequired, ChecklistTargetSubject.MOTHER, null, null);
    }

    /** Compatibility constructor for callers that supplied the target subject. */
    public ChecklistItemRequest(
            UUID id,
            String itemText,
            Integer order,
            Boolean isRequired,
            ChecklistTargetSubject targetSubject) {
        this(id, itemText, order, isRequired, targetSubject, null, null);
    }
}
