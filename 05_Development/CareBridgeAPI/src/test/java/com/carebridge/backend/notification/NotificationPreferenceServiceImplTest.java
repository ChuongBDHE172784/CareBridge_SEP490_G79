package com.carebridge.backend.notification;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.notification.dto.NotificationPreferenceItemDto;
import com.carebridge.backend.notification.dto.NotificationPreferencesResponse;
import com.carebridge.backend.notification.dto.UpdateNotificationPreferencesRequest;
import com.carebridge.backend.notification.entity.NotificationPreference;
import com.carebridge.backend.notification.entity.NotificationType;
import com.carebridge.backend.notification.repository.NotificationPreferenceRepository;
import com.carebridge.backend.notification.service.impl.NotificationPreferenceServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UC-10 Update Notification Preferences.
 *
 * TDD Test Spec: CB-NOTIF-IMP-010-TEST
 * Constraints enforced: C1 (userId from JWT), C2 (upsert idempotent), C6 (audit in TX).
 */
@ExtendWith(MockitoExtension.class)
class NotificationPreferenceServiceImplTest {

    @Mock private NotificationPreferenceRepository preferenceRepository;
    @Mock private AuditService auditService;
    @InjectMocks private NotificationPreferenceServiceImpl service;

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");

    // =========================================================================
    // TC-UNIT-001: getPreferences — happy path
    // =========================================================================

    @Test
    @DisplayName("TC-UNIT-001: getPreferences returns mapped list and emits audit")
    void getPreferences_withExistingRows_returnsMappedList() {
        // Given
        NotificationPreference pref = NotificationPreference.builder()
                .preferenceId(UUID.randomUUID())
                .userId(USER_ID)
                .notificationType(NotificationType.REMINDER)
                .pushEnabled(true)
                .emailEnabled(false)
                .inAppEnabled(true)
                .build();
        when(preferenceRepository.findByUserId(USER_ID)).thenReturn(List.of(pref));

        // When
        NotificationPreferencesResponse response = service.getPreferences(USER_ID);

        // Then
        assertThat(response.userId()).isEqualTo(USER_ID);
        assertThat(response.preferences()).hasSize(1);
        assertThat(response.preferences().get(0).notificationType()).isEqualTo(NotificationType.REMINDER);
        assertThat(response.preferences().get(0).pushEnabled()).isTrue();
        assertThat(response.preferences().get(0).emailEnabled()).isFalse();

        // C6: audit must be called
        verify(auditService).log(
                eq(AuditAction.NOTIFICATION_PREFERENCES_VIEWED),
                eq(USER_ID),
                eq("NotificationPreference"),
                eq(USER_ID.toString()),
                any());
    }

    @Test
    @DisplayName("TC-UNIT-001b: getPreferences with no rows returns empty list")
    void getPreferences_noRows_returnsEmptyList() {
        when(preferenceRepository.findByUserId(USER_ID)).thenReturn(List.of());

        NotificationPreferencesResponse response = service.getPreferences(USER_ID);

        assertThat(response.userId()).isEqualTo(USER_ID);
        assertThat(response.preferences()).isEmpty();
    }

    // =========================================================================
    // TC-UNIT-002: updatePreferences — happy path (upsert new row)
    // =========================================================================

    @Test
    @DisplayName("TC-UNIT-002: updatePreferences creates new row when none exists (upsert)")
    void updatePreferences_noExistingRow_createsNewPreference() {
        // Given — no existing preference
        when(preferenceRepository.findByUserIdAndNotificationType(USER_ID, NotificationType.REMINDER))
                .thenReturn(Optional.empty());

        NotificationPreference savedPref = NotificationPreference.builder()
                .preferenceId(UUID.randomUUID())
                .userId(USER_ID)
                .notificationType(NotificationType.REMINDER)
                .pushEnabled(true)
                .emailEnabled(false)
                .inAppEnabled(true)
                .build();
        when(preferenceRepository.save(any())).thenReturn(savedPref);
        when(preferenceRepository.findByUserId(USER_ID)).thenReturn(List.of(savedPref));

        UpdateNotificationPreferencesRequest request = new UpdateNotificationPreferencesRequest(
                List.of(new NotificationPreferenceItemDto(NotificationType.REMINDER, true, false, true)));

        // When
        NotificationPreferencesResponse response = service.updatePreferences(USER_ID, request);

        // Then — repository.save called once
        verify(preferenceRepository, times(1)).save(any(NotificationPreference.class));
        assertThat(response.preferences()).hasSize(1);

        // C6: audit emitted
        verify(auditService).log(
                eq(AuditAction.NOTIFICATION_PREFERENCES_UPDATED),
                eq(USER_ID),
                eq("NotificationPreference"),
                eq(USER_ID.toString()),
                any());
    }

    @Test
    @DisplayName("TC-UNIT-002b: updatePreferences updates existing row (upsert idempotent — C2)")
    void updatePreferences_existingRow_updatesInPlace() {
        // Given — existing preference with push=true
        NotificationPreference existing = NotificationPreference.builder()
                .preferenceId(UUID.randomUUID())
                .userId(USER_ID)
                .notificationType(NotificationType.REMINDER)
                .pushEnabled(true)
                .emailEnabled(true)
                .inAppEnabled(true)
                .build();
        when(preferenceRepository.findByUserIdAndNotificationType(USER_ID, NotificationType.REMINDER))
                .thenReturn(Optional.of(existing));

        NotificationPreference updated = NotificationPreference.builder()
                .preferenceId(existing.getPreferenceId())
                .userId(USER_ID)
                .notificationType(NotificationType.REMINDER)
                .pushEnabled(false)  // changed
                .emailEnabled(true)
                .inAppEnabled(true)
                .build();
        when(preferenceRepository.save(any())).thenReturn(updated);
        when(preferenceRepository.findByUserId(USER_ID)).thenReturn(List.of(updated));

        UpdateNotificationPreferencesRequest request = new UpdateNotificationPreferencesRequest(
                List.of(new NotificationPreferenceItemDto(NotificationType.REMINDER, false, null, null)));

        // When
        service.updatePreferences(USER_ID, request);

        // Then — captured save has pushEnabled=false
        ArgumentCaptor<NotificationPreference> captor = ArgumentCaptor.forClass(NotificationPreference.class);
        verify(preferenceRepository).save(captor.capture());
        assertThat(captor.getValue().getPushEnabled()).isFalse();
    }

    @Test
    @DisplayName("TC-UNIT-003: updatePreferences with 3 items calls save 3 times")
    void updatePreferences_multipleItems_callsSaveForEach() {
        // Given
        when(preferenceRepository.findByUserIdAndNotificationType(any(), any()))
                .thenReturn(Optional.empty());
        when(preferenceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(preferenceRepository.findByUserId(USER_ID)).thenReturn(List.of());

        UpdateNotificationPreferencesRequest request = new UpdateNotificationPreferencesRequest(List.of(
                new NotificationPreferenceItemDto(NotificationType.REMINDER, true, false, true),
                new NotificationPreferenceItemDto(NotificationType.COMMUNITY_REPLY, false, false, false),
                new NotificationPreferenceItemDto(NotificationType.EMERGENCY, true, true, true)));

        // When
        service.updatePreferences(USER_ID, request);

        // Then — save called exactly 3 times (one per preference item)
        verify(preferenceRepository, times(3)).save(any(NotificationPreference.class));
    }

    // =========================================================================
    // TC-UNIT-004: isPushEnabled — preference gate (UC-158/159/160/161)
    // =========================================================================

    @Test
    @DisplayName("TC-UNIT-004a: isPushEnabled returns true when push_enabled=true in DB")
    void isPushEnabled_preferenceExists_pushTrue_returnsTrue() {
        NotificationPreference pref = NotificationPreference.builder()
                .pushEnabled(true).build();
        when(preferenceRepository.findByUserIdAndNotificationType(USER_ID, NotificationType.REMINDER))
                .thenReturn(Optional.of(pref));

        assertThat(service.isPushEnabled(USER_ID, NotificationType.REMINDER)).isTrue();
    }

    @Test
    @DisplayName("TC-UNIT-004b: isPushEnabled returns false when push_enabled=false in DB")
    void isPushEnabled_preferenceExists_pushFalse_returnsFalse() {
        NotificationPreference pref = NotificationPreference.builder()
                .pushEnabled(false).build();
        when(preferenceRepository.findByUserIdAndNotificationType(USER_ID, NotificationType.REMINDER))
                .thenReturn(Optional.of(pref));

        assertThat(service.isPushEnabled(USER_ID, NotificationType.REMINDER)).isFalse();
    }

    @Test
    @DisplayName("TC-UNIT-004c: isPushEnabled defaults to true when no row exists (opt-out model)")
    void isPushEnabled_noRow_defaultsToTrue() {
        when(preferenceRepository.findByUserIdAndNotificationType(USER_ID, NotificationType.REMINDER))
                .thenReturn(Optional.empty());

        assertThat(service.isPushEnabled(USER_ID, NotificationType.REMINDER)).isTrue();
    }
}
