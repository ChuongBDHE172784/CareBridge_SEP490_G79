package com.carebridge.backend.systemconfiguration.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateSystemConfigurationRequest(
        @NotNull @Min(1) @Max(100000) Integer apiRateLimit,
        @NotNull @Min(1000) @Max(300000) Integer connectionTimeoutMs,
        @NotNull @Min(1) @Max(1024) Integer maxUploadSizeMb,
        @NotNull @Email String administratorEmail,
        @NotNull Boolean emailAlerts,
        @NotNull Boolean smsAlerts,
        @NotNull Boolean webhookAlerts,
        @NotNull Boolean aiModerationEnabled,
        @NotNull Boolean maintenanceModeEnabled) {}
