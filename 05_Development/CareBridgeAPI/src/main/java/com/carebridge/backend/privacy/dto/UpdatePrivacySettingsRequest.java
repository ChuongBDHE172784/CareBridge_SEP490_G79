package com.carebridge.backend.privacy.dto;

import com.carebridge.backend.privacy.entity.ProfileVisibility;

public record UpdatePrivacySettingsRequest(
    ProfileVisibility profileVisibility,
    Boolean locationSharingEnabled,
    Boolean analyticsConsent,
    Boolean dataExportOptOut
) {}
