package com.carebridge.backend.notification.service.impl;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.notification.dto.ConsultationNotificationEventType;
import com.carebridge.backend.notification.dto.ConsultationNotificationPayload;
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
import com.carebridge.backend.notification.service.IConsultationNotificationService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConsultationNotificationService implements IConsultationNotificationService {

    private final NotificationPreferenceRepository preferenceRepository;
    private final DeviceTokenRepository deviceTokenRepository;
    private final NotificationRecordRepository notificationRecordRepository;
    private final FcmService fcmService;
    private final AuditService auditService;

    @Override
    @Transactional
    public List<NotificationRecordResponse> sendConsultationNotification(ConsultationNotificationPayload payload) {
        List<NotificationRecordResponse> responses = new ArrayList<>();
        Map<String, String> data = buildPayload(payload);
        for (UUID recipientId : resolveRecipients(payload.motherUserId(), payload.expertUserId(), payload.eventType())) {
            if (!preferenceRepository.isPushEnabled(recipientId, NotificationType.CONSULTATION)) {
                continue;
            }

            NotificationRecord record = NotificationRecord.builder()
                    .userId(recipientId)
                    .type(NotificationType.CONSULTATION)
                    .title(payload.title())
                    .body(payload.body())
                    .referenceId(payload.consultationId())
                    .referenceType("CONSULTATION")
                    .metadata(data)
                    .build();

            List<DeviceToken> tokens = deviceTokenRepository.findByUserIdAndActiveTrue(recipientId);
            if (tokens.isEmpty()) {
                record.setStatus(NotificationRecordStatus.FAILED);
                record.setAttemptCount(0);
                record.setFailedAt(Instant.now());
                responses.add(saveAndAudit(record));
                continue;
            }

            FcmDeliveryResult delivery = fcmService.sendWithRetry(
                    tokens.getFirst().getToken(), payload.title(), payload.body(), 3);
            applyDelivery(record, delivery);
            responses.add(saveAndAudit(record));
        }
        return responses;
    }

    @Override
    public List<UUID> resolveRecipients(UUID motherUserId, UUID expertUserId, ConsultationNotificationEventType eventType) {
        return switch (eventType) {
            case EXPERT_JOINED -> List.of(motherUserId);
            case CONSULTATION_BOOKED, CONSULTATION_REMINDER, CONSULTATION_CANCELLED -> List.of(motherUserId, expertUserId);
        };
    }

    @Override
    public Map<String, String> buildPayload(ConsultationNotificationPayload payload) {
        Map<String, String> safeData = new LinkedHashMap<>();
        safeData.put("consultationId", payload.consultationId().toString());
        safeData.put("eventType", payload.eventType().name());
        safeData.put("deepLink", buildDeepLink(payload.consultationId(), payload.eventType()));
        if (payload.data() != null) {
            payload.data().forEach((key, value) -> {
                if (isSafePayloadEntry(key, value)) {
                    safeData.put(key, value);
                }
            });
        }
        return safeData;
    }

    @Override
    public String buildDeepLink(UUID consultationId, ConsultationNotificationEventType eventType) {
        return "carebridge://consultations/" + consultationId + "?event=" + eventType.name();
    }

    private boolean isSafePayloadEntry(String key, String value) {
        String safeKey = key == null ? "" : key.toLowerCase();
        String safeValue = value == null ? "" : value.toLowerCase();
        return !safeKey.contains("token")
                && !safeKey.contains("zego")
                && !safeValue.contains("token")
                && !safeValue.contains("zego");
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
