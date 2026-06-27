package com.carebridge.backend.privacy.dto;

import java.time.Instant;
import java.util.UUID;

public record PrivacySettingsResponse(
    UUID id,
    UUID userId,
    String profileVisibility,
    boolean locationSharingEnabled,
    boolean analyticsConsent,
    boolean dataExportOptOut,
    Instant updatedAt
) {}
