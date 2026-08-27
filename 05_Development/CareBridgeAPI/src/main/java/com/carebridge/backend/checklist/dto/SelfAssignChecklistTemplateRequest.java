package com.carebridge.backend.checklist.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record SelfAssignChecklistTemplateRequest(
        @NotNull UUID templateId,
        UUID journeyId,
        UUID babyId) {
}
