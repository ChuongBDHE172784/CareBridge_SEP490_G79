package com.carebridge.backend.health.device.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ConnectDeviceRequest(
        @NotBlank @Size(max = 80) String providerName,
        @Size(max = 150) String deviceName,
        List<String> scopes,
        String tokenReference,
        @NotNull Boolean consentAccepted) {
}
