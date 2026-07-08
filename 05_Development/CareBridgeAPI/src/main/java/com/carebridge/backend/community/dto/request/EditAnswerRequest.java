package com.carebridge.backend.community.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EditAnswerRequest {

    @NotBlank(message = "body is required")
    @Size(min = 1, max = 2000, message = "body must be between 1 and 2000 characters")
    private String body;

    @NotNull(message = "isPersonalExperience is required")
    private Boolean isPersonalExperience;

    // isExpertLabeled is NOT accepted from request — set by Moderator/System only (ADR-COM-005)
}
