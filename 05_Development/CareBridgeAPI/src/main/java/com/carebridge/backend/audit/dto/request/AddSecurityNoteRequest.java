package com.carebridge.backend.audit.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddSecurityNoteRequest(
    @NotBlank @Size(min = 1, max = 5000) String noteText
) {}
