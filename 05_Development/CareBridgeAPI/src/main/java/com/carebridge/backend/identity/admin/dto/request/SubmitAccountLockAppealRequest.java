package com.carebridge.backend.identity.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SubmitAccountLockAppealRequest(
        @NotBlank String appealToken,
        @NotBlank @Size(max = 1000) String reason) {
}

