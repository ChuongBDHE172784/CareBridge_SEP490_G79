package com.carebridge.backend.notification.service.impl;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.notification.dto.FcmDeliveryResult;
import com.carebridge.backend.notification.dto.NotificationRecordResponse;
import com.carebridge.backend.notification.entity.DeviceToken;
import com.carebridge.backend.notification.entity.NotificationRecord;
import com.carebridge.backend.notification.entity.NotificationRecordStatus;
import com.carebridge.backend.notification.entity.NotificationType;
import com.carebridge.backend.notification.repository.DeviceTokenRepository;
import com.carebridge.backend.notification.repository.NotificationPreferenceRepository;
import com.carebridge.backend.notification.repository.NotificationRecordRepository;
import com.carebridge.backend.notification.service.FcmService;
import com.carebridge.backend.notification.service.IReminderNotificationService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReminderNotificationService implements IReminderNotificationService {

    private final NotificationPreferenceRepository preferenceRepository;
    private final DeviceTokenRepository deviceTokenRepository;
    private final NotificationRecordRepository notificationRecordRepository;
    private final FcmService fcmService;
    private final AuditService auditService;

    @Override
    @Transactional
    public NotificationRecordResponse sendReminderNotification(UUID reminderId, UUID userId, String title, String body) {
        if (!preferenceRepository.isPushEnabled(userId, NotificationType.REMINDER)) {
            return null;
        }

        List<DeviceToken> tokens = deviceTokenRepository.findByUserIdAndActiveTrue(userId);
        NotificationRecord record = baseRecord(userId, NotificationType.REMINDER, title, body, reminderId, "REMINDER");
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

    private NotificationRecord baseRecord(
            UUID userId, NotificationType type, String title, String body, UUID referenceId, String referenceType) {
        return NotificationRecord.builder()
                .userId(userId)
                .type(type)
                .title(title)
                .body(body)
                .referenceId(referenceId)
                .referenceType(referenceType)
                .build();
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
                record.getSentAt(),
                record.getMetadata()
        );
    }
}
