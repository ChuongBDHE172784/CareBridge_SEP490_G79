package com.carebridge.backend.privacy.service;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.AuthorizationException;
import com.carebridge.backend.privacy.dto.PrivacySettingsResponse;
import com.carebridge.backend.privacy.dto.UpdatePrivacySettingsRequest;
import com.carebridge.backend.privacy.entity.PrivacySettings;
import com.carebridge.backend.privacy.entity.ProfileVisibility;
import com.carebridge.backend.privacy.repository.PrivacySettingsRepository;
import com.carebridge.backend.privacy.service.impl.PrivacySettingsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.security.Principal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PrivacySettingsServiceTest {

    private PrivacySettingsService service;
    private PrivacySettingsRepository privacySettingsRepository;
    private AuditService auditService;

    @BeforeEach
    void setUp() {
        privacySettingsRepository = mock(PrivacySettingsRepository.class);
        auditService = mock(AuditService.class);
        service = new PrivacySettingsServiceImpl(privacySettingsRepository, auditService);
    }

    private Principal mockPrincipal(UUID userId) {
        Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn(userId.toString());
        return principal;
    }

    private PrivacySettings buildSettings(UUID userId) {
        return PrivacySettings.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .profileVisibility(ProfileVisibility.FRIENDS_ONLY)
                .locationSharingEnabled(false)
                .analyticsConsent(false)
                .dataExportOptOut(false)
                .build();
    }

    @Test
    @DisplayName("PRIV-TC-001: getSettings returns existing settings for owner")
    void getSettings_existingSettings_returnsResponse() {
        UUID userId = UUID.randomUUID();
        PrivacySettings settings = buildSettings(userId);
        Principal principal = mockPrincipal(userId);

        when(privacySettingsRepository.findByUserId(userId)).thenReturn(Optional.of(settings));

        PrivacySettingsResponse response = service.getSettings(userId, principal);

        assertThat(response).isNotNull();
        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.profileVisibility()).isEqualTo("FRIENDS_ONLY");
        verify(auditService).log(eq(AuditAction.PRIVACY_SETTINGS_ACCESSED), eq(userId), any(), any(), any());
    }

    @Test
    @DisplayName("PRIV-TC-002: getSettings creates defaults when no settings exist")
    void getSettings_noSettings_createsDefaults() {
        UUID userId = UUID.randomUUID();
        Principal principal = mockPrincipal(userId);
        PrivacySettings defaults = PrivacySettings.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .profileVisibility(ProfileVisibility.FRIENDS_ONLY)
                .locationSharingEnabled(false)
                .analyticsConsent(false)
                .dataExportOptOut(false)
                .build();

        when(privacySettingsRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(privacySettingsRepository.save(any(PrivacySettings.class))).thenReturn(defaults);

        PrivacySettingsResponse response = service.getSettings(userId, principal);

        assertThat(response).isNotNull();
        assertThat(response.profileVisibility()).isEqualTo("FRIENDS_ONLY");
        verify(privacySettingsRepository).save(any(PrivacySettings.class));
    }

    @Test
    @DisplayName("PRIV-TC-003: updateSettings saves and audits for owner")
    void updateSettings_validRequest_savesAndAudits() {
        UUID userId = UUID.randomUUID();
        Principal principal = mockPrincipal(userId);
        PrivacySettings settings = buildSettings(userId);
        UpdatePrivacySettingsRequest request = new UpdatePrivacySettingsRequest(
                ProfileVisibility.PUBLIC, true, true, false);

        when(privacySettingsRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(settings));

        PrivacySettingsResponse response = service.updateSettings(userId, request, principal);

        assertThat(response).isNotNull();
        verify(privacySettingsRepository).patchFields(
                userId, ProfileVisibility.PUBLIC, true, true, false);
        verify(auditService, atLeastOnce()).log(eq(AuditAction.PRIVACY_SETTINGS_UPDATED), eq(userId), any(), any(), any());
    }

    @Test
    @DisplayName("PRIV-TC-004: getSettings throws AuthorizationException for wrong user")
    void getSettings_differentUser_throwsAuthorizationException() {
        UUID userId = UUID.randomUUID();
        UUID attackerId = UUID.randomUUID();
        Principal principal = mockPrincipal(attackerId);

        assertThatThrownBy(() -> service.getSettings(userId, principal))
                .isInstanceOf(AuthorizationException.class)
                .hasMessageContaining("Access denied");

        verifyNoInteractions(privacySettingsRepository);
    }

    @Test
    @DisplayName("PRIV-TC-005: updateSettings throws AuthorizationException for wrong user")
    void updateSettings_differentUser_throwsAuthorizationException() {
        UUID userId = UUID.randomUUID();
        UUID attackerId = UUID.randomUUID();
        Principal principal = mockPrincipal(attackerId);
        UpdatePrivacySettingsRequest request = new UpdatePrivacySettingsRequest(
                ProfileVisibility.PRIVATE, null, null, null);

        assertThatThrownBy(() -> service.updateSettings(userId, request, principal))
                .isInstanceOf(AuthorizationException.class)
                .hasMessageContaining("Access denied");

        verifyNoInteractions(privacySettingsRepository);
    }

    @Test
    @DisplayName("PRIV-TC-006: updateSettings detects analytics consent withdrawal and audits it")
    void updateSettings_analyticsConsentWithdrawn_auditsWithdrawal() {
        UUID userId = UUID.randomUUID();
        Principal principal = mockPrincipal(userId);

        // Settings currently has analyticsConsent=true
        PrivacySettings settings = PrivacySettings.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .profileVisibility(ProfileVisibility.FRIENDS_ONLY)
                .locationSharingEnabled(false)
                .analyticsConsent(true)
                .dataExportOptOut(false)
                .build();

        // Request withdraws analyticsConsent (sets to false)
        UpdatePrivacySettingsRequest request = new UpdatePrivacySettingsRequest(
                null, null, false, null);

        when(privacySettingsRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(settings));

        service.updateSettings(userId, request, principal);

        // Should audit analytics_consent_withdrawn + normal update
        ArgumentCaptor<String> detailCaptor = ArgumentCaptor.forClass(String.class);
        verify(auditService, atLeastOnce()).log(
                eq(AuditAction.PRIVACY_SETTINGS_UPDATED), eq(userId), any(), detailCaptor.capture(), any());
        assertThat(detailCaptor.getAllValues()).contains("analytics_consent_withdrawn");
    }

    @Test
    @DisplayName("PRIV-TC-007: updateSettings null fields are ignored (partial update)")
    void updateSettings_nullFields_areIgnored() {
        UUID userId = UUID.randomUUID();
        Principal principal = mockPrincipal(userId);

        PrivacySettings settings = buildSettings(userId);
        // Only update profileVisibility
        UpdatePrivacySettingsRequest request = new UpdatePrivacySettingsRequest(
                ProfileVisibility.PRIVATE, null, null, null);

        when(privacySettingsRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(settings));

        service.updateSettings(userId, request, principal);

        // locationSharingEnabled, analyticsConsent, dataExportOptOut should stay as original
        verify(privacySettingsRepository).patchFields(
                userId, ProfileVisibility.PRIVATE, null, null, null);
        assertThat(settings.getProfileVisibility()).isEqualTo(ProfileVisibility.PRIVATE);
        assertThat(settings.isLocationSharingEnabled()).isFalse();
        assertThat(settings.isAnalyticsConsent()).isFalse();
    }
}
