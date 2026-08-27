package com.carebridge.backend.notification;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.notification.dto.NotificationRecordResponse;
import com.carebridge.backend.notification.dto.SendNotificationRequest;
import com.carebridge.backend.notification.entity.DeviceToken;
import com.carebridge.backend.notification.entity.DevicePlatform;
import com.carebridge.backend.notification.entity.NotificationRecord;
import com.carebridge.backend.notification.entity.NotificationRecordStatus;
import com.carebridge.backend.notification.entity.NotificationType;
import com.carebridge.backend.notification.repository.DeviceTokenRepository;
import com.carebridge.backend.notification.repository.NotificationRecordRepository;
import com.carebridge.backend.notification.service.FcmService;
import com.carebridge.backend.notification.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UC-128 Send Automated Notification (internal send flow).
 *
 * UC-158 / UC-159 / UC-160 / UC-161 all trigger this via NotificationService.send().
 * Preference gate is checked by the caller (ReminderNotificationService etc.) before
 * delegating here; these tests focus on the send + record + audit logic.
 */
@ExtendWith(MockitoExtension.class)
class NotificationSendServiceTest {

    @Mock private DeviceTokenRepository deviceTokenRepository;
    @Mock private NotificationRecordRepository notificationRecordRepository;
    @Mock private FcmService fcmService;
    @Mock private AuditService auditService;
    @InjectMocks private NotificationServiceImpl service;

    private static final UUID RECIPIENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000128");
    private static final UUID REMINDER_ID  = UUID.fromString("00000000-0000-0000-0000-000000000158");

    // =========================================================================
    // UC128-TC-001: Happy path — FCM delivery succeeds, record saved as SENT
    // =========================================================================

    @Test
    @DisplayName("UC128-TC-001: send with active device token → FCM called, record SENT, audit logged")
    void send_withActiveToken_recordsSentStatusAndAudits() throws Exception {
        // Given
        DeviceToken token = DeviceToken.builder()
                .id(UUID.randomUUID())
                .userId(RECIPIENT_ID)
                .token("fcm-token-abc123")
                .platform(DevicePlatform.ANDROID)
                .active(true)
                .build();
        when(deviceTokenRepository.findByUserIdAndActiveTrue(RECIPIENT_ID)).thenReturn(List.of(token));
        when(fcmService.sendToToken(anyString(), anyString(), anyString())).thenReturn("fcm-msg-001");

        NotificationRecord saved = NotificationRecord.builder()
                .id(UUID.randomUUID())
                .userId(RECIPIENT_ID)
                .type(NotificationType.REMINDER)
                .title("Nhắc uống vitamin")
                .body("Đã đến giờ uống vitamin")
                .referenceId(REMINDER_ID)
                .referenceType("REMINDER")
                .status(NotificationRecordStatus.SENT)
                .fcmMessageId("fcm-msg-001")
                .attemptCount(1)
                .createdAt(Instant.now())
                .build();
        when(notificationRecordRepository.save(any())).thenReturn(saved);

        SendNotificationRequest request = new SendNotificationRequest(
                RECIPIENT_ID, NotificationType.REMINDER,
                "Nhắc uống vitamin", "Đã đến giờ uống vitamin",
                REMINDER_ID, "REMINDER");

        // When
        NotificationRecordResponse response = service.send(request);

        // Then
        assertThat(response.status()).isEqualTo("SENT");
        verify(fcmService).sendToToken(eq("fcm-token-abc123"), anyString(), anyString());

        // Audit: NOTIFICATION_SENT logged
        verify(auditService).log(
                eq(AuditAction.NOTIFICATION_SENT),
                eq(RECIPIENT_ID),
                eq("notification"),
                anyString(),
                eq("REMINDER"));
    }

    // =========================================================================
    // UC128-TC-002: No active device token → record FAILED, audit NOTIFICATION_FAILED
    // =========================================================================

    @Test
    @DisplayName("UC128-TC-002: send with no device tokens → record FAILED, no FCM call")
    void send_noActiveToken_recordsFailedStatusAndAudits() {
        // Given
        when(deviceTokenRepository.findByUserIdAndActiveTrue(RECIPIENT_ID)).thenReturn(List.of());

        NotificationRecord failed = NotificationRecord.builder()
                .id(UUID.randomUUID())
                .userId(RECIPIENT_ID)
                .type(NotificationType.REMINDER)
                .title("Test")
                .body("Body")
                .status(NotificationRecordStatus.FAILED)
                .attemptCount(1)
                .createdAt(Instant.now())
                .build();
        when(notificationRecordRepository.save(any())).thenReturn(failed);

        SendNotificationRequest request = new SendNotificationRequest(
                RECIPIENT_ID, NotificationType.REMINDER, "Test", "Body", null, null);

        // When
        NotificationRecordResponse response = service.send(request);

        // Then
        assertThat(response.status()).isEqualTo("FAILED");
        verifyNoInteractions(fcmService);
        verify(auditService).log(
                eq(AuditAction.NOTIFICATION_FAILED),
                eq(RECIPIENT_ID),
                eq("notification"),
                anyString(),
                any());
    }

    // =========================================================================
    // UC128-TC-003: FCM throws exception → record FAILED, audit NOTIFICATION_FAILED
    // =========================================================================

    @Test
    @DisplayName("UC128-TC-003: FCM delivery throws → record FAILED, audit NOTIFICATION_FAILED")
    void send_fcmThrows_recordsFailedStatus() throws Exception {
        // Given
        DeviceToken token = DeviceToken.builder()
                .id(UUID.randomUUID()).userId(RECIPIENT_ID)
                .token("bad-token").platform(DevicePlatform.ANDROID).active(true)
                .build();
        when(deviceTokenRepository.findByUserIdAndActiveTrue(RECIPIENT_ID)).thenReturn(List.of(token));
        when(fcmService.sendToToken(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("FCM unavailable"));

        ArgumentCaptor<NotificationRecord> captor = ArgumentCaptor.forClass(NotificationRecord.class);
        when(notificationRecordRepository.save(captor.capture())).thenAnswer(inv -> {
            NotificationRecord r = inv.getArgument(0);
            NotificationRecord copy = NotificationRecord.builder()
                    .id(UUID.randomUUID()).userId(r.getUserId()).type(r.getType())
                    .title(r.getTitle()).body(r.getBody())
                    .status(r.getStatus()).attemptCount(r.getAttemptCount())
                    .createdAt(Instant.now()).build();
            return copy;
        });

        SendNotificationRequest request = new SendNotificationRequest(
                RECIPIENT_ID, NotificationType.REMINDER, "Test", "Body", null, null);

        // When
        NotificationRecordResponse response = service.send(request);

        // Then — status must be FAILED, not SENT
        assertThat(response.status()).isEqualTo("FAILED");
        verify(auditService).log(eq(AuditAction.NOTIFICATION_FAILED), any(), any(), any(), any());
    }

    // =========================================================================
    // UC128-TC-004: reference fields are persisted (UC-158 reminderId link)
    // =========================================================================

    @Test
    @DisplayName("UC128-TC-004: send with referenceId persists referenceId and referenceType")
    void send_withReferenceId_persistsReferenceFields() throws Exception {
        // Given
        DeviceToken token = DeviceToken.builder()
                .id(UUID.randomUUID()).userId(RECIPIENT_ID)
                .token("fcm-token").platform(DevicePlatform.ANDROID).active(true)
                .build();
        when(deviceTokenRepository.findByUserIdAndActiveTrue(RECIPIENT_ID)).thenReturn(List.of(token));
        when(fcmService.sendToToken(anyString(), anyString(), anyString())).thenReturn("msg-id");

        ArgumentCaptor<NotificationRecord> captor = ArgumentCaptor.forClass(NotificationRecord.class);
        when(notificationRecordRepository.save(captor.capture())).thenAnswer(inv -> {
            NotificationRecord r = inv.getArgument(0);
            // Build a copy with id + createdAt filled in (simulates DB-generated fields)
            NotificationRecord copy = NotificationRecord.builder()
                    .id(UUID.randomUUID())
                    .userId(r.getUserId())
                    .type(r.getType())
                    .title(r.getTitle())
                    .body(r.getBody())
                    .referenceId(r.getReferenceId())
                    .referenceType(r.getReferenceType())
                    .status(r.getStatus())
                    .fcmMessageId(r.getFcmMessageId())
                    .attemptCount(r.getAttemptCount())
                    .createdAt(Instant.now())
                    .build();
            return copy;
        });

        SendNotificationRequest request = new SendNotificationRequest(
                RECIPIENT_ID, NotificationType.REMINDER,
                "Reminder title", "Reminder body",
                REMINDER_ID, "REMINDER");

        // When
        service.send(request);

        // Then — referenceId and referenceType captured
        NotificationRecord persisted = captor.getValue();
        assertThat(persisted.getReferenceId()).isEqualTo(REMINDER_ID);
        assertThat(persisted.getReferenceType()).isEqualTo("REMINDER");
    }
}
