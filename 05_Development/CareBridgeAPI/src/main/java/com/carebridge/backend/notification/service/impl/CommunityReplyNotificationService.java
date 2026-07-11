package com.carebridge.backend.notification.service.impl;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.ResourceNotFoundException;
import com.carebridge.backend.community.entity.CommunityQuestion;
import com.carebridge.backend.community.repository.CommunityQuestionRepository;
import com.carebridge.backend.notification.dto.FcmDeliveryResult;
import com.carebridge.backend.notification.dto.NotificationRecordResponse;
import com.carebridge.backend.notification.entity.DeviceToken;
import com.carebridge.backend.notification.entity.NotificationRecord;
import com.carebridge.backend.notification.entity.NotificationRecordStatus;
import com.carebridge.backend.notification.entity.NotificationType;
import com.carebridge.backend.notification.entity.QuestionNotificationMute;
import com.carebridge.backend.notification.repository.DeviceTokenRepository;
import com.carebridge.backend.notification.repository.NotificationPreferenceRepository;
import com.carebridge.backend.notification.repository.NotificationRecordRepository;
import com.carebridge.backend.notification.repository.QuestionNotificationMuteRepository;
import com.carebridge.backend.notification.service.FcmService;
import com.carebridge.backend.notification.service.ICommunityReplyNotificationService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommunityReplyNotificationService implements ICommunityReplyNotificationService {

    private final CommunityQuestionRepository questionRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final QuestionNotificationMuteRepository muteRepository;
    private final DeviceTokenRepository deviceTokenRepository;
    private final NotificationRecordRepository notificationRecordRepository;
    private final FcmService fcmService;
    private final AuditService auditService;

    @Override
    @Transactional
    public NotificationRecordResponse sendReplyNotification(
            UUID questionId, UUID answerId, UUID answererId, String answerPreview) {
        CommunityQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Community question not found: " + questionId));
        UUID ownerId = question.getAuthorId();
        if (ownerId.equals(answererId)
                || muteRepository.existsByUserIdAndQuestionId(ownerId, questionId)
                || !preferenceRepository.isPushEnabled(ownerId, NotificationType.COMMUNITY_REPLY)) {
            return null;
        }

        String title = "New reply to your question";
        String body = answerPreview == null || answerPreview.isBlank()
                ? "Someone replied to your community question."
                : answerPreview;
        NotificationRecord record = NotificationRecord.builder()
                .userId(ownerId)
                .type(NotificationType.COMMUNITY_REPLY)
                .title(title)
                .body(body)
                .referenceId(answerId)
                .referenceType("COMMUNITY_ANSWER")
                .metadata(Map.of(
                        "questionId", questionId.toString(),
                        "answerId", answerId.toString()))
                .build();

        List<DeviceToken> tokens = deviceTokenRepository.findByUserIdAndActiveTrue(ownerId);
        if (tokens.isEmpty()) {
            record.setStatus(NotificationRecordStatus.FAILED);
            record.setAttemptCount(0);
            record.setFailedAt(Instant.now());
            return saveAndAudit(record);
        }

        FcmDeliveryResult delivery = fcmService.sendWithRetry(tokens.getFirst().getToken(), title, body, 3);
        applyDelivery(record, delivery);
        return saveAndAudit(record);
    }

    @Override
    @Transactional
    public void muteQuestion(UUID userId, UUID questionId) {
        if (!muteRepository.existsByUserIdAndQuestionId(userId, questionId)) {
            muteRepository.save(QuestionNotificationMute.builder()
                    .userId(userId)
                    .questionId(questionId)
                    .build());
        }
    }

    @Override
    @Transactional
    public void unmuteQuestion(UUID userId, UUID questionId) {
        muteRepository.deleteByUserIdAndQuestionId(userId, questionId);
    }

    private void applyDelivery(NotificationRecord record, FcmDeliveryResult delivery) {
        record.setAttemptCount(delivery.attempts());
        if (delivery.success()) {
            record.setStatus(NotificationRecordStatus.SENT);
            record.setFcmMessageId(delivery.messageId());
            record.setSentAt(Instant.now());
        } else {
            record.setStatus(NotificationRecordStatus.FAILED);
            record.setFailedAt(Instant.now());
        }
    }

    private NotificationRecordResponse saveAndAudit(NotificationRecord record) {
        NotificationRecord saved = notificationRecordRepository.save(record);
        AuditAction action = saved.getStatus() == NotificationRecordStatus.FAILED
                ? AuditAction.NOTIFICATION_FAILED
                : AuditAction.NOTIFICATION_SENT;
        auditService.log(action, saved.getUserId(), "NotificationRecord", saved.getId().toString(),
                saved.getType().name());
        return toResponse(saved);
    }

    private NotificationRecordResponse toResponse(NotificationRecord record) {
        return new NotificationRecordResponse(
                record.getId(),
                record.getUserId(),
                record.getType().name(),
                record.getTitle(),
                record.getBody(),
                record.getReferenceId(),
                record.getReferenceType(),
                record.getStatus().name(),
                record.getCreatedAt(),
                record.getSentAt()
        );
    }
}
