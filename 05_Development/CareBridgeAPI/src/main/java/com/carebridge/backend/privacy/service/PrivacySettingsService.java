package com.carebridge.backend.privacy.service;

import com.carebridge.backend.privacy.dto.PrivacySettingsResponse;
import com.carebridge.backend.privacy.dto.UpdatePrivacySettingsRequest;
import java.security.Principal;
import java.util.UUID;

public interface PrivacySettingsService {

    PrivacySettingsResponse getSettings(UUID userId, Principal principal);

    PrivacySettingsResponse updateSettings(UUID userId, UpdatePrivacySettingsRequest request, Principal principal);
}
