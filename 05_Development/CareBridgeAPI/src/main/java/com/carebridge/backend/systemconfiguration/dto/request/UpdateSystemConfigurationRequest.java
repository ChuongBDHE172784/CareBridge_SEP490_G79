package com.carebridge.backend.systemconfiguration.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateSystemConfigurationRequest(
        @NotNull Boolean aiModerationEnabled,
        @NotNull Boolean maintenanceModeEnabled,
        @NotNull @Min(0) Long rowVersion) {}
