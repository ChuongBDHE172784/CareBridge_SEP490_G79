package com.carebridge.backend.notification.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.notification.dto.FcmDeliveryResult;
import com.carebridge.backend.notification.entity.DeviceToken;
import com.carebridge.backend.notification.entity.NotificationRecord;
import com.carebridge.backend.notification.entity.NotificationType;
import com.carebridge.backend.notification.repository.DeviceTokenRepository;
import com.carebridge.backend.notification.repository.NotificationPreferenceRepository;
import com.carebridge.backend.notification.repository.NotificationRecordRepository;
import com.carebridge.backend.notification.service.FcmService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConsultationRequestNotificationServiceImplTest {

    private static final UUID RECIPIENT_ID = UUID.randomUUID();
    private static final UUID REQUEST_ID = UUID.randomUUID();

    @Mock private DeviceTokenRepository deviceTokenRepository;
    @Mock private NotificationRecordRepository recordRepository;
    @Mock private NotificationPreferenceRepository preferenceRepository;
    @Mock private ConsultationRequestNotificationWriter writer;
    @Mock private FcmService fcmService;
    @Mock private AuditService auditService;

    private ConsultationRequestNotificationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ConsultationRequestNotificationServiceImpl(
                deviceTokenRepository,
                recordRepository,
                preferenceRepository,
                writer,
                fcmService,
                auditService,
                Clock.fixed(Instant.parse("2026-07-16T12:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void createdNotificationUsesConsultationRecordContractAndMinimalFcmData() {
        when(preferenceRepository.isPushEnabled(RECIPIENT_ID, NotificationType.CONSULTATION))
                .thenReturn(true);
        when(writer.insertIfAbsent(any())).thenReturn(true);
        when(writer.claim(any())).thenReturn(true);
        when(deviceTokenRepository.findByUserIdAndActiveTrue(RECIPIENT_ID))
                .thenReturn(List.of(DeviceToken.builder()
                        .userId(RECIPIENT_ID)
                        .token("fcm-token")
                        .active(true)
                        .build()));
        when(fcmService.sendWithRetry(
                        eq("fcm-token"),
                        any(),
                        any(),
                        any(),
                        eq(3)))
                .thenReturn(FcmDeliveryResult.success("message-id", 1));

        service.notifyCreated(RECIPIENT_ID, UUID.randomUUID(), REQUEST_ID);

        ArgumentCaptor<NotificationRecord> record =
                ArgumentCaptor.forClass(NotificationRecord.class);
        verify(writer).insertIfAbsent(record.capture());
        assertThat(record.getValue().getType()).isEqualTo(NotificationType.CONSULTATION);
        assertThat(record.getValue().getReferenceType())
                .isEqualTo("CONSULTATION_REQUEST");
        assertThat(record.getValue().getReferenceId()).isEqualTo(REQUEST_ID);
        assertThat(record.getValue().getMetadata())
                .containsEntry("eventType", "REQUEST_CREATED");
        assertThat(record.getValue().getTitle()).doesNotContain("Sensitive topic");
        assertThat(record.getValue().getBody()).doesNotContain("Sensitive description");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> data =
                ArgumentCaptor.forClass(Map.class);
        verify(fcmService).sendWithRetry(
                eq("fcm-token"), any(), any(), data.capture(), eq(3));
        assertThat(data.getValue()).containsExactlyInAnyOrderEntriesOf(Map.of(
                "type", "CONSULTATION_REQUEST",
                "requestId", REQUEST_ID.toString()));
        assertThat(data.getValue()).doesNotContainKeys("topic", "description");
    }
}
