package com.carebridge.backend.notification.dto;

import com.carebridge.backend.notification.entity.DevicePlatform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterDeviceTokenRequest(
    @NotBlank @Size(max = 512) String token,
    @NotNull DevicePlatform platform
) {}
