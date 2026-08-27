package com.carebridge.backend.systemconfiguration.dto.response;

import java.time.Instant;
import java.util.UUID;

public record SystemConfigurationResponse(
        UUID id,
        boolean aiModerationEnabled,
        boolean maintenanceModeEnabled,
        long rowVersion,
        UUID updatedBy,
        Instant updatedAt) {}
