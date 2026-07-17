package com.carebridge.backend.notification.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.notification.dto.FcmDeliveryResult;
import com.carebridge.backend.notification.entity.DevicePlatform;
import com.carebridge.backend.notification.entity.DeviceToken;
import com.carebridge.backend.notification.entity.NotificationRecord;
import com.carebridge.backend.notification.entity.NotificationRecordStatus;
import com.carebridge.backend.notification.entity.NotificationType;
import com.carebridge.backend.notification.repository.DeviceTokenRepository;
import com.carebridge.backend.notification.repository.NotificationRecordRepository;
import com.carebridge.backend.notification.repository.NotificationPreferenceRepository;
import com.carebridge.backend.notification.service.FcmService;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DirectMessageNotificationServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private DeviceTokenRepository deviceTokenRepository;
    @Mock private NotificationRecordRepository notificationRecordRepository;
    @Mock private NotificationPreferenceRepository preferenceRepository;
    @Mock private NotificationRecordWriter notificationRecordWriter;
    @Mock private FcmService fcmService;
    @Mock private AuditService auditService;

    private DirectMessageNotificationServiceImpl service;
    private final Instant fixedNow = Instant.parse("2026-07-16T08:00:00Z");
    private final Clock fixedClock = Clock.fixed(fixedNow, ZoneOffset.UTC);

    private static final UUID RECIPIENT_ID = UUID.randomUUID();
    private static final UUID SENDER_ID = UUID.randomUUID();
    private static final UUID CONVERSATION_ID = UUID.randomUUID();
    private static final UUID MESSAGE_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new DirectMessageNotificationServiceImpl(
                userRepository, deviceTokenRepository, notificationRecordRepository, preferenceRepository, notificationRecordWriter,
                fcmService, auditService, fixedClock);
        org.mockito.Mockito.lenient().when(preferenceRepository.isPushEnabled(any(), eq(NotificationType.MESSAGE)))
                .thenReturn(true);
        org.mockito.Mockito.lenient().when(notificationRecordWriter.insertIfAbsent(any())).thenReturn(true);
        org.mockito.Mockito.lenient().when(notificationRecordWriter.claim(any())).thenReturn(true);
        org.mockito.Mockito.lenient().when(notificationRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private static DeviceToken token(String value) {
        return DeviceToken.builder().id(UUID.randomUUID()).userId(RECIPIENT_ID).token(value)
                .platform(DevicePlatform.ANDROID).active(true).build();
    }

    // MEDI-TC-014a (service half) — body contains ONLY the sender's display name, never the
    // original message content (C4 / PDPA data minimization).
    @Test
    void notifyNewMessage_bodyContainsSenderNameOnly_notMessageBody() {
        when(userRepository.findById(SENDER_ID))
                .thenReturn(Optional.of(User.builder().id(SENDER_ID).name("BS. Nguyễn Văn A").build()));
        when(deviceTokenRepository.findByUserIdAndActiveTrue(RECIPIENT_ID)).thenReturn(List.of(token("tok")));
        when(fcmService.sendWithRetry(eq("tok"), any(), any(), eq(3))).thenReturn(FcmDeliveryResult.success("fcm-1", 1));

        service.notifyNewMessage(RECIPIENT_ID, SENDER_ID, CONVERSATION_ID, MESSAGE_ID);

        ArgumentCaptor<NotificationRecord> captor = ArgumentCaptor.forClass(NotificationRecord.class);
        verify(notificationRecordWriter).complete(captor.capture());
        NotificationRecord saved = captor.getValue();
        assertThat(saved.getBody()).contains("BS. Nguyễn Văn A");
        assertThat(saved.getType()).isEqualTo(NotificationType.MESSAGE);
        assertThat(saved.getReferenceId()).isEqualTo(MESSAGE_ID);
        assertThat(saved.getReferenceType()).isEqualTo("DIRECT_MESSAGE");
        assertThat(saved.getMetadata()).containsEntry("conversationId", CONVERSATION_ID.toString());
    }

    // C10 — duplicate insert (listener ran twice) short-circuits before FCM is ever called.
    @Test
    void notifyNewMessage_duplicateInsert_neverCallsFcmOrSaves() {
        when(notificationRecordWriter.insertIfAbsent(any())).thenReturn(false);

        service.notifyNewMessage(RECIPIENT_ID, SENDER_ID, CONVERSATION_ID, MESSAGE_ID);

        verify(fcmService, never()).sendWithRetry(any(), any(), any(), any(Integer.class));
        verify(notificationRecordWriter, never()).complete(any());
    }

    @Test
    void notifyNewMessage_messagePreferenceDisabled_skipsRecordAndFcm() {
        when(preferenceRepository.isPushEnabled(RECIPIENT_ID, NotificationType.MESSAGE)).thenReturn(false);

        service.notifyNewMessage(RECIPIENT_ID, SENDER_ID, CONVERSATION_ID, MESSAGE_ID);

        verify(notificationRecordWriter, never()).insertIfAbsent(any());
        verify(fcmService, never()).sendWithRetry(any(), any(), any(), any(Integer.class));
        verify(notificationRecordWriter, never()).complete(any());
    }

    // No active device token — still exactly 1 FAILED row, attemptCount=0 (no FCM call attempted)
    @Test
    void notifyNewMessage_noDeviceToken_recordsFailedWithZeroAttempts() {
        when(deviceTokenRepository.findByUserIdAndActiveTrue(RECIPIENT_ID)).thenReturn(List.of());

        service.notifyNewMessage(RECIPIENT_ID, SENDER_ID, CONVERSATION_ID, MESSAGE_ID);

        ArgumentCaptor<NotificationRecord> captor = ArgumentCaptor.forClass(NotificationRecord.class);
        verify(notificationRecordWriter).complete(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(NotificationRecordStatus.FAILED);
        assertThat(captor.getValue().getAttemptCount()).isEqualTo(0);
        assertThat(captor.getValue().getFailedAt()).isNotNull();
        verify(fcmService, never()).sendWithRetry(any(), any(), any(), any(Integer.class));
        verify(auditService).log(eq(AuditAction.NOTIFICATION_FAILED), eq(RECIPIENT_ID), any(), any(), any());
    }

    // MEDI-TC-014b (unit half) — graceful failure: attemptCount == delivery.attempts(), NOT 0.
    @Test
    void notifyNewMessage_gracefulFcmFailure_attemptCountFromDeliveryResult() {
        when(deviceTokenRepository.findByUserIdAndActiveTrue(RECIPIENT_ID)).thenReturn(List.of(token("tok")));
        when(fcmService.sendWithRetry(eq("tok"), any(), any(), eq(3)))
                .thenReturn(FcmDeliveryResult.failed("TOKEN_EXPIRED", 3));

        service.notifyNewMessage(RECIPIENT_ID, SENDER_ID, CONVERSATION_ID, MESSAGE_ID);

        ArgumentCaptor<NotificationRecord> captor = ArgumentCaptor.forClass(NotificationRecord.class);
        verify(notificationRecordWriter).complete(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(NotificationRecordStatus.FAILED);
        assertThat(captor.getValue().getAttemptCount()).isEqualTo(3); // exact delivery.attempts() value
        assertThat(captor.getValue().getFailedAt()).isNotNull();
    }

    // MEDI-TC-014b (unit half) — sendWithRetry throws: sentinel attemptCount=0, distinct from the
    // graceful-failure case above (2 separate oracles, ADR-MEDI-004 mục 4).
    @Test
    void notifyNewMessage_fcmThrows_attemptCountSentinelZero_stillExactlyOneFailedRow() {
        when(deviceTokenRepository.findByUserIdAndActiveTrue(RECIPIENT_ID)).thenReturn(List.of(token("tok")));
        when(fcmService.sendWithRetry(eq("tok"), any(), any(), eq(3)))
                .thenThrow(new RuntimeException("network unreachable"));

        service.notifyNewMessage(RECIPIENT_ID, SENDER_ID, CONVERSATION_ID, MESSAGE_ID);

        ArgumentCaptor<NotificationRecord> captor = ArgumentCaptor.forClass(NotificationRecord.class);
        verify(notificationRecordWriter).complete(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(NotificationRecordStatus.FAILED);
        assertThat(captor.getValue().getAttemptCount()).isEqualTo(0); // sentinel, not "0 successes"
        assertThat(captor.getValue().getFailedAt()).isNotNull();
        verify(notificationRecordWriter, org.mockito.Mockito.times(1)).complete(any()); // never 0 rows
    }

    // Success branch — SENT, fcmMessageId + sentAt populated, audit NOTIFICATION_SENT
    @Test
    void notifyNewMessage_success_recordsSentWithFcmMessageId() {
        when(deviceTokenRepository.findByUserIdAndActiveTrue(RECIPIENT_ID)).thenReturn(List.of(token("tok")));
        when(fcmService.sendWithRetry(eq("tok"), any(), any(), eq(3))).thenReturn(FcmDeliveryResult.success("fcm-123", 1));

        service.notifyNewMessage(RECIPIENT_ID, SENDER_ID, CONVERSATION_ID, MESSAGE_ID);

        ArgumentCaptor<NotificationRecord> captor = ArgumentCaptor.forClass(NotificationRecord.class);
        verify(notificationRecordWriter).complete(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(NotificationRecordStatus.SENT);
        assertThat(captor.getValue().getFcmMessageId()).isEqualTo("fcm-123");
        assertThat(captor.getValue().getSentAt()).isNotNull();
        verify(auditService).log(eq(AuditAction.NOTIFICATION_SENT), eq(RECIPIENT_ID), any(), any(), any());
    }
}
