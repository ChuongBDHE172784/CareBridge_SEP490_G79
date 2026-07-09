package com.carebridge.backend.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.community.entity.CommunityQuestion;
import com.carebridge.backend.community.repository.CommunityQuestionRepository;
import com.carebridge.backend.notification.dto.FcmDeliveryResult;
import com.carebridge.backend.notification.entity.DevicePlatform;
import com.carebridge.backend.notification.entity.DeviceToken;
import com.carebridge.backend.notification.entity.NotificationRecord;
import com.carebridge.backend.notification.entity.NotificationType;
import com.carebridge.backend.notification.repository.DeviceTokenRepository;
import com.carebridge.backend.notification.repository.NotificationPreferenceRepository;
import com.carebridge.backend.notification.repository.NotificationRecordRepository;
import com.carebridge.backend.notification.repository.QuestionNotificationMuteRepository;
import com.carebridge.backend.notification.service.impl.CommunityReplyNotificationService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CommunityReplyNotificationServiceTest {

    @Mock private CommunityQuestionRepository questionRepository;
    @Mock private NotificationPreferenceRepository preferenceRepository;
    @Mock private QuestionNotificationMuteRepository muteRepository;
    @Mock private DeviceTokenRepository deviceTokenRepository;
    @Mock private NotificationRecordRepository notificationRecordRepository;
    @Mock private FcmService fcmService;
    @Mock private AuditService auditService;
    @InjectMocks private CommunityReplyNotificationService service;

    private static final UUID QUESTION_ID = UUID.fromString("20000000-0000-0000-0000-000000000159");
    private static final UUID ANSWER_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID OWNER_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");
    private static final UUID ANSWERER_ID = UUID.fromString("20000000-0000-0000-0000-000000000003");

    @Test
    @DisplayName("NOTIFCR-TC-001: enabled, not muted, not self reply sends notification")
    void sendReplyNotification_happyPath_recordsSent() {
        givenQuestionOwner();
        when(preferenceRepository.isPushEnabled(OWNER_ID, NotificationType.COMMUNITY_REPLY)).thenReturn(true);
        when(muteRepository.existsByUserIdAndQuestionId(OWNER_ID, QUESTION_ID)).thenReturn(false);
        when(deviceTokenRepository.findByUserIdAndActiveTrue(OWNER_ID)).thenReturn(List.of(token("owner-token")));
        when(fcmService.sendWithRetry(eq("owner-token"), any(), any(), eq(3)))
                .thenReturn(FcmDeliveryResult.success("msg-cr", 1));
        when(notificationRecordRepository.save(any())).thenAnswer(inv -> withId(inv.getArgument(0)));

        var response = service.sendReplyNotification(QUESTION_ID, ANSWER_ID, ANSWERER_ID, "Helpful answer");

        assertThat(response.status()).isEqualTo("SENT");
        verify(fcmService).sendWithRetry(eq("owner-token"), any(), any(), eq(3));
    }

    @Test
    @DisplayName("NOTIFCR-TC-002: muted question skips notification")
    void sendReplyNotification_mutedQuestion_skips() {
        givenQuestionOwner();
        when(muteRepository.existsByUserIdAndQuestionId(OWNER_ID, QUESTION_ID)).thenReturn(true);

        assertThat(service.sendReplyNotification(QUESTION_ID, ANSWER_ID, ANSWERER_ID, "Muted")).isNull();
        verify(fcmService, never()).sendWithRetry(any(), any(), any(), any(Integer.class));
    }

    @Test
    @DisplayName("NOTIFCR-TC-003: self reply skips notification")
    void sendReplyNotification_selfReply_skips() {
        givenQuestionOwner();

        assertThat(service.sendReplyNotification(QUESTION_ID, ANSWER_ID, OWNER_ID, "Self")).isNull();
        verify(fcmService, never()).sendWithRetry(any(), any(), any(), any(Integer.class));
    }

    @Test
    @DisplayName("NOTIFCR-TC-004: disabled COMMUNITY_REPLY preference skips notification")
    void sendReplyNotification_preferenceDisabled_skips() {
        givenQuestionOwner();
        when(muteRepository.existsByUserIdAndQuestionId(OWNER_ID, QUESTION_ID)).thenReturn(false);
        when(preferenceRepository.isPushEnabled(OWNER_ID, NotificationType.COMMUNITY_REPLY)).thenReturn(false);

        assertThat(service.sendReplyNotification(QUESTION_ID, ANSWER_ID, ANSWERER_ID, "Disabled")).isNull();
        verify(fcmService, never()).sendWithRetry(any(), any(), any(), any(Integer.class));
    }

    @Test
    @DisplayName("NOTIFCR-TC-005: FCM retry exhaustion records FAILED")
    void sendReplyNotification_retryExhausted_recordsFailed() {
        givenQuestionOwner();
        when(preferenceRepository.isPushEnabled(OWNER_ID, NotificationType.COMMUNITY_REPLY)).thenReturn(true);
        when(muteRepository.existsByUserIdAndQuestionId(OWNER_ID, QUESTION_ID)).thenReturn(false);
        when(deviceTokenRepository.findByUserIdAndActiveTrue(OWNER_ID)).thenReturn(List.of(token("owner-token")));
        when(fcmService.sendWithRetry(eq("owner-token"), any(), any(), eq(3)))
                .thenReturn(FcmDeliveryResult.failed("SERVICE_UNAVAILABLE", 3));
        when(notificationRecordRepository.save(any())).thenAnswer(inv -> withId(inv.getArgument(0)));

        var response = service.sendReplyNotification(QUESTION_ID, ANSWER_ID, ANSWERER_ID, "Failed");

        assertThat(response.status()).isEqualTo("FAILED");
    }

    @Test
    @DisplayName("NOTIFCR-TC-006: metadata contains questionId and answerId")
    void sendReplyNotification_persistsMetadata() {
        givenQuestionOwner();
        when(preferenceRepository.isPushEnabled(OWNER_ID, NotificationType.COMMUNITY_REPLY)).thenReturn(true);
        when(muteRepository.existsByUserIdAndQuestionId(OWNER_ID, QUESTION_ID)).thenReturn(false);
        when(deviceTokenRepository.findByUserIdAndActiveTrue(OWNER_ID)).thenReturn(List.of(token("owner-token")));
        when(fcmService.sendWithRetry(eq("owner-token"), any(), any(), eq(3)))
                .thenReturn(FcmDeliveryResult.success("msg-cr", 1));
        when(notificationRecordRepository.save(any())).thenAnswer(inv -> withId(inv.getArgument(0)));

        service.sendReplyNotification(QUESTION_ID, ANSWER_ID, ANSWERER_ID, "Metadata");

        ArgumentCaptor<NotificationRecord> captor = ArgumentCaptor.forClass(NotificationRecord.class);
        verify(notificationRecordRepository).save(captor.capture());
        assertThat(captor.getValue().getMetadata())
                .containsEntry("questionId", QUESTION_ID.toString())
                .containsEntry("answerId", ANSWER_ID.toString());
    }

    private void givenQuestionOwner() {
        when(questionRepository.findById(QUESTION_ID)).thenReturn(Optional.of(CommunityQuestion.builder()
                .id(QUESTION_ID)
                .authorId(OWNER_ID)
                .title("Question")
                .body("Body")
                .build()));
    }

    private DeviceToken token(String value) {
        return DeviceToken.builder()
                .id(UUID.randomUUID())
                .userId(OWNER_ID)
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
