package com.carebridge.backend.checklist.today.dto;

import com.carebridge.backend.checklist.today.model.TaskAction;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.UUID;

public record TaskActionRequest(
        @NotNull TaskAction action,
        @NotNull UUID clientRequestId,
        @Pattern(regexp = "^[A-Z0-9_]{1,80}$") String reason) {
}
