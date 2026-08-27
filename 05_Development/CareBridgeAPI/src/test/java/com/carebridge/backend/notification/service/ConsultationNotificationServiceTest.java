package com.carebridge.backend.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.notification.dto.ConsultationNotificationEventType;
import com.carebridge.backend.notification.dto.ConsultationNotificationPayload;
import com.carebridge.backend.notification.dto.FcmDeliveryResult;
import com.carebridge.backend.notification.entity.DevicePlatform;
import com.carebridge.backend.notification.entity.DeviceToken;
import com.carebridge.backend.notification.entity.NotificationRecord;
import com.carebridge.backend.notification.entity.NotificationType;
import com.carebridge.backend.notification.repository.DeviceTokenRepository;
import com.carebridge.backend.notification.repository.NotificationPreferenceRepository;
import com.carebridge.backend.notification.repository.NotificationRecordRepository;
import com.carebridge.backend.notification.service.impl.ConsultationNotificationService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConsultationNotificationServiceTest {

    @Mock private NotificationPreferenceRepository preferenceRepository;
    @Mock private DeviceTokenRepository deviceTokenRepository;
    @Mock private NotificationRecordRepository notificationRecordRepository;
    @Mock private FcmService fcmService;
    @Mock private AuditService auditService;
    @InjectMocks private ConsultationNotificationService service;

    private static final UUID CONSULTATION_ID = UUID.fromString("30000000-0000-0000-0000-000000000160");
    private static final UUID MOTHER_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID EXPERT_ID = UUID.fromString("30000000-0000-0000-0000-000000000002");

    @Test
    @DisplayName("CONNOTIF-TC-001: EXPERT_JOINED resolves only MOTHER recipient")
    void resolveRecipients_expertJoined_returnsMotherOnly() {
        assertThat(service.resolveRecipients(MOTHER_ID, EXPERT_ID, ConsultationNotificationEventType.EXPERT_JOINED))
                .containsExactly(MOTHER_ID);
    }

    @Test
    @DisplayName("CONNOTIF-TC-002: CONSULTATION_BOOKED resolves MOTHER and EXPERT")
    void resolveRecipients_booked_returnsBothParticipants() {
        assertThat(service.resolveRecipients(MOTHER_ID, EXPERT_ID, ConsultationNotificationEventType.CONSULTATION_BOOKED))
                .containsExactlyInAnyOrder(MOTHER_ID, EXPERT_ID);
    }

    @Test
    @DisplayName("CONNOTIF-TC-003: payload excludes ZegoCloud token")
    void buildPayload_excludesZegoToken() {
        Map<String, String> data = service.buildPayload(payload(ConsultationNotificationEventType.EXPERT_JOINED));

        assertThat(data.keySet()).noneMatch(key -> key.toLowerCase().contains("zego") || key.toLowerCase().contains("token"));
        assertThat(data.values()).noneMatch(value -> value != null && value.toLowerCase().contains("zego"));
    }

    @Test
    @DisplayName("CONNOTIF-TC-004: deep link includes consultation ID and event route")
    void buildDeepLink_containsConsultationRoute() {
        String link = service.buildDeepLink(CONSULTATION_ID, ConsultationNotificationEventType.EXPERT_JOINED);

        assertThat(link).contains(CONSULTATION_ID.toString());
        assertThat(link).contains("consultation");
    }

    @Test
    @DisplayName("CONNOTIF-TC-005: retry exhaustion records FAILED")
    void sendConsultationNotification_retryExhausted_recordsFailed() {
        when(preferenceRepository.isPushEnabled(MOTHER_ID, NotificationType.CONSULTATION)).thenReturn(true);
        when(deviceTokenRepository.findByUserIdAndActiveTrue(MOTHER_ID)).thenReturn(List.of(token(MOTHER_ID)));
        when(fcmService.sendWithRetry(any(), any(), any(), eq(3)))
                .thenReturn(FcmDeliveryResult.failed("SERVICE_UNAVAILABLE", 3));
        when(notificationRecordRepository.save(any())).thenAnswer(inv -> withId(inv.getArgument(0)));

        var responses = service.sendConsultationNotification(payload(ConsultationNotificationEventType.EXPERT_JOINED));

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).status()).isEqualTo("FAILED");
    }

    @Test
    @DisplayName("CONNOTIF-TC-006: CONSULTATION_CANCELLED sends to all participants without throwing")
    void sendConsultationNotification_cancelled_sendsToParticipants() {
        when(preferenceRepository.isPushEnabled(any(), eq(NotificationType.CONSULTATION))).thenReturn(true);
        when(deviceTokenRepository.findByUserIdAndActiveTrue(any())).thenAnswer(inv -> List.of(token(inv.getArgument(0))));
        when(fcmService.sendWithRetry(any(), any(), any(), eq(3)))
                .thenReturn(FcmDeliveryResult.success("msg", 1));
        when(notificationRecordRepository.save(any())).thenAnswer(inv -> withId(inv.getArgument(0)));

        assertThatNoException().isThrownBy(() ->
                service.sendConsultationNotification(payload(ConsultationNotificationEventType.CONSULTATION_CANCELLED)));
    }

    private ConsultationNotificationPayload payload(ConsultationNotificationEventType eventType) {
        return new ConsultationNotificationPayload(
                CONSULTATION_ID,
                MOTHER_ID,
                EXPERT_ID,
                eventType,
                "Consultation update",
                "Your consultation status changed",
                Map.of("zegoToken", "must-not-leak"));
    }

    private DeviceToken token(UUID userId) {
        return DeviceToken.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .token("token-" + userId)
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
