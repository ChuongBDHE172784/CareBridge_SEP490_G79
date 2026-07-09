package com.carebridge.backend.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.notification.dto.FcmDeliveryResult;
import com.carebridge.backend.notification.dto.NotificationRecordResponse;
import com.carebridge.backend.notification.entity.DevicePlatform;
import com.carebridge.backend.notification.entity.DeviceToken;
import com.carebridge.backend.notification.entity.NotificationRecord;
import com.carebridge.backend.notification.entity.NotificationRecordStatus;
import com.carebridge.backend.notification.entity.NotificationType;
import com.carebridge.backend.notification.repository.DeviceTokenRepository;
import com.carebridge.backend.notification.repository.NotificationPreferenceRepository;
import com.carebridge.backend.notification.repository.NotificationRecordRepository;
import com.carebridge.backend.notification.service.impl.ReminderNotificationService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReminderNotificationServiceTest {

    @Mock private NotificationPreferenceRepository preferenceRepository;
    @Mock private DeviceTokenRepository deviceTokenRepository;
    @Mock private NotificationRecordRepository notificationRecordRepository;
    @Mock private FcmService fcmService;
    @Mock private AuditService auditService;
    @InjectMocks private ReminderNotificationService service;

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000158");
    private static final UUID REMINDER_ID = UUID.fromString("10000000-0000-0000-0000-000000000158");

    @Test
    @DisplayName("NOTIF-TC-001: preference enabled sends reminder notification and records SENT")
    void sendReminderNotification_preferenceEnabled_recordsSent() {
        when(preferenceRepository.isPushEnabled(USER_ID, NotificationType.REMINDER)).thenReturn(true);
        when(deviceTokenRepository.findByUserIdAndActiveTrue(USER_ID)).thenReturn(List.of(token("token-1")));
        when(fcmService.sendWithRetry("token-1", "Reminder", "Take vitamin", 3))
                .thenReturn(FcmDeliveryResult.success("fcm-msg-001", 1));
        when(notificationRecordRepository.save(any())).thenAnswer(inv -> withId(inv.getArgument(0)));

        NotificationRecordResponse response =
                service.sendReminderNotification(REMINDER_ID, USER_ID, "Reminder", "Take vitamin");

        assertThat(response.status()).isEqualTo("SENT");
        assertThat(response.referenceId()).isEqualTo(REMINDER_ID);
        verify(fcmService).sendWithRetry("token-1", "Reminder", "Take vitamin", 3);
    }

    @Test
    @DisplayName("NOTIF-TC-002: preference disabled skips reminder notification silently")
    void sendReminderNotification_preferenceDisabled_skips() {
        when(preferenceRepository.isPushEnabled(USER_ID, NotificationType.REMINDER)).thenReturn(false);

        NotificationRecordResponse response =
                service.sendReminderNotification(REMINDER_ID, USER_ID, "Reminder", "Take vitamin");

        assertThat(response).isNull();
        verify(fcmService, never()).sendWithRetry(any(), any(), any(), any(Integer.class));
        verify(notificationRecordRepository, never()).save(any());
    }

    @Test
    @DisplayName("NOTIF-TC-003: retry exhaustion records FAILED with attemptCount 3")
    void sendReminderNotification_retryExhausted_recordsFailed() {
        when(preferenceRepository.isPushEnabled(USER_ID, NotificationType.REMINDER)).thenReturn(true);
        when(deviceTokenRepository.findByUserIdAndActiveTrue(USER_ID)).thenReturn(List.of(token("token-1")));
        when(fcmService.sendWithRetry("token-1", "Reminder", "Take vitamin", 3))
                .thenReturn(FcmDeliveryResult.failed("SERVICE_UNAVAILABLE", 3));
        when(notificationRecordRepository.save(any())).thenAnswer(inv -> withId(inv.getArgument(0)));

        NotificationRecordResponse response =
                service.sendReminderNotification(REMINDER_ID, USER_ID, "Reminder", "Take vitamin");

        assertThat(response.status()).isEqualTo("FAILED");
        ArgumentCaptor<NotificationRecord> captor = ArgumentCaptor.forClass(NotificationRecord.class);
        verify(notificationRecordRepository).save(captor.capture());
        assertThat(captor.getValue().getAttemptCount()).isEqualTo(3);
        assertThat(captor.getValue().getFailedAt()).isNotNull();
    }

    @Test
    @DisplayName("NOTIF-TC-005: no active device token records FAILED without calling FCM")
    void sendReminderNotification_noActiveToken_recordsFailed() {
        when(preferenceRepository.isPushEnabled(USER_ID, NotificationType.REMINDER)).thenReturn(true);
        when(deviceTokenRepository.findByUserIdAndActiveTrue(USER_ID)).thenReturn(List.of());
        when(notificationRecordRepository.save(any())).thenAnswer(inv -> withId(inv.getArgument(0)));

        NotificationRecordResponse response =
                service.sendReminderNotification(REMINDER_ID, USER_ID, "Reminder", "Take vitamin");

        assertThat(response.status()).isEqualTo("FAILED");
        verify(fcmService, never()).sendWithRetry(any(), any(), any(), any(Integer.class));
    }

    @Test
    @DisplayName("NOTIF-TC-006: reminderId is persisted as referenceId")
    void sendReminderNotification_persistsReminderReference() {
        when(preferenceRepository.isPushEnabled(USER_ID, NotificationType.REMINDER)).thenReturn(true);
        when(deviceTokenRepository.findByUserIdAndActiveTrue(USER_ID)).thenReturn(List.of(token("token-1")));
        when(fcmService.sendWithRetry(any(), any(), any(), eq(3)))
                .thenReturn(FcmDeliveryResult.success("fcm-msg-001", 1));
        when(notificationRecordRepository.save(any())).thenAnswer(inv -> withId(inv.getArgument(0)));

        service.sendReminderNotification(REMINDER_ID, USER_ID, "Reminder", "Take vitamin");

        ArgumentCaptor<NotificationRecord> captor = ArgumentCaptor.forClass(NotificationRecord.class);
        verify(notificationRecordRepository).save(captor.capture());
        assertThat(captor.getValue().getReferenceId()).isEqualTo(REMINDER_ID);
        assertThat(captor.getValue().getReferenceType()).isEqualTo("REMINDER");
    }

    private DeviceToken token(String value) {
        return DeviceToken.builder()
                .id(UUID.randomUUID())
                .userId(USER_ID)
                .token(value)
                .platform(DevicePlatform.ANDROID)
                .active(true)
                .build();
    }

    private NotificationRecord withId(NotificationRecord record) {
        record.setId(UUID.randomUUID());
        if (record.getCreatedAt() == null) {
            record.setCreatedAt(Instant.now());
        }
        return record;
    }
}
