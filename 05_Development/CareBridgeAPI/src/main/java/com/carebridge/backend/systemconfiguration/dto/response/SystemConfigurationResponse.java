package com.carebridge.backend.systemconfiguration.dto.response;

import java.time.Instant;
import java.util.UUID;

public record SystemConfigurationResponse(
        UUID id,
        int apiRateLimit,
        int connectionTimeoutMs,
        int maxUploadSizeMb,
        String administratorEmail,
        boolean emailAlerts,
        boolean smsAlerts,
        boolean webhookAlerts,
        boolean aiModerationEnabled,
        boolean maintenanceModeEnabled,
        UUID updatedBy,
        Instant updatedAt) {}
