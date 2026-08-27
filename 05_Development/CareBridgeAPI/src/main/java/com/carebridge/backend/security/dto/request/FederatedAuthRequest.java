package com.carebridge.backend.security.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FederatedAuthRequest(
        @NotBlank String idToken,
        @Size(max = 500) String deviceInfo) {
}
