package com.carebridge.backend.audit.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ReviewSecurityEventRequest(
    @NotBlank
    @Pattern(regexp = "UNDER_REVIEW|RESOLVED|FALSE_POSITIVE",
             message = "Status must be UNDER_REVIEW, RESOLVED, or FALSE_POSITIVE")
    String status
) {}
