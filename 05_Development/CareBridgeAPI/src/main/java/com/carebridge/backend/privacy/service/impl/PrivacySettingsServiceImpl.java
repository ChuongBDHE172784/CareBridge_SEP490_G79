package com.carebridge.backend.privacy.service.impl;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.AuthorizationException;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.privacy.dto.PrivacySettingsResponse;
import com.carebridge.backend.privacy.dto.UpdatePrivacySettingsRequest;
import com.carebridge.backend.privacy.entity.PrivacySettings;
import com.carebridge.backend.privacy.repository.PrivacySettingsRepository;
import com.carebridge.backend.privacy.service.PrivacySettingsService;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PrivacySettingsServiceImpl implements PrivacySettingsService {

    private final PrivacySettingsRepository privacySettingsRepository;
    private final AuditService auditService;

    @Override
    @Transactional(readOnly = true)
    public PrivacySettingsResponse getSettings(UUID userId, Principal principal) {
        UUID requesterId = SecurityUtils.requireCurrentUserId(principal);
        authorizeOwner(requesterId, userId);

        PrivacySettings settings = getOrCreateDefault(userId);
        auditService.log(AuditAction.PRIVACY_SETTINGS_ACCESSED, userId, "privacy_settings", userId.toString(), null);
        return toResponse(settings);
    }

    @Override
    @Transactional
    public PrivacySettingsResponse updateSettings(UUID userId, UpdatePrivacySettingsRequest request, Principal principal) {
        UUID requesterId = SecurityUtils.requireCurrentUserId(principal);
        authorizeOwner(requesterId, userId);

        PrivacySettings settings = getOrCreateDefault(userId);

        boolean analyticsConsentWithdrawn = settings.isAnalyticsConsent()
                && request.analyticsConsent() != null
                && !request.analyticsConsent();

        if (request.profileVisibility() != null) {
            settings.setProfileVisibility(request.profileVisibility());
        }
        if (request.locationSharingEnabled() != null) {
            settings.setLocationSharingEnabled(request.locationSharingEnabled());
        }
        if (request.analyticsConsent() != null) {
            settings.setAnalyticsConsent(request.analyticsConsent());
        }
        if (request.dataExportOptOut() != null) {
            settings.setDataExportOptOut(request.dataExportOptOut());
        }

        PrivacySettings saved = privacySettingsRepository.save(settings);

        if (analyticsConsentWithdrawn) {
            auditService.log(AuditAction.PRIVACY_SETTINGS_UPDATED, userId, "privacy_settings",
                    "analytics_consent_withdrawn", null);
        }

        auditService.log(AuditAction.PRIVACY_SETTINGS_UPDATED, userId, "privacy_settings", userId.toString(), request);
        return toResponse(saved);
    }

    private PrivacySettings getOrCreateDefault(UUID userId) {
        return privacySettingsRepository.findByUserId(userId)
                .orElseGet(() -> privacySettingsRepository.save(PrivacySettings.defaultSettings(userId)));
    }

    private void authorizeOwner(UUID requesterId, UUID targetUserId) {
        if (!requesterId.equals(targetUserId)) {
            throw new AuthorizationException("Access denied: cannot access another user's privacy settings");
        }
    }

    private PrivacySettingsResponse toResponse(PrivacySettings settings) {
        return new PrivacySettingsResponse(
                settings.getId(),
                settings.getUserId(),
                settings.getProfileVisibility().name(),
                settings.isLocationSharingEnabled(),
                settings.isAnalyticsConsent(),
                settings.isDataExportOptOut(),
                settings.getUpdatedAt()
        );
    }
}
