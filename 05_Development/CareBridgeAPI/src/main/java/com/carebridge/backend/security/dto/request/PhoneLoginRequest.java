package com.carebridge.backend.security.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PhoneLoginRequest(
        @NotBlank @Size(max = 8192) String idToken,
        @Size(max = 500) String deviceInfo) {
}
