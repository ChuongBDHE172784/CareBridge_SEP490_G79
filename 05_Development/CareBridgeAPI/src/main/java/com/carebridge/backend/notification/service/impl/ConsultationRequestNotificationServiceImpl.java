package com.carebridge.backend.notification.service.impl;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.notification.dto.FcmDeliveryResult;
import com.carebridge.backend.notification.entity.DeviceToken;
import com.carebridge.backend.notification.entity.NotificationRecord;
import com.carebridge.backend.notification.entity.NotificationRecordStatus;
import com.carebridge.backend.notification.entity.NotificationType;
import com.carebridge.backend.notification.repository.DeviceTokenRepository;
import com.carebridge.backend.notification.repository.NotificationPreferenceRepository;
import com.carebridge.backend.notification.repository.NotificationRecordRepository;
import com.carebridge.backend.notification.service.FcmService;
import com.carebridge.backend.notification.service.IConsultationRequestNotificationService;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ConsultationRequestNotificationServiceImpl
        implements IConsultationRequestNotificationService {

    private static final int MAX_ATTEMPTS = 3;

    private final DeviceTokenRepository deviceTokenRepository;
    private final NotificationRecordRepository recordRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final ConsultationRequestNotificationWriter writer;
    private final FcmService fcmService;
    private final AuditService auditService;
    private final Clock clock;

    @Autowired
    public ConsultationRequestNotificationServiceImpl(
            DeviceTokenRepository deviceTokenRepository,
            NotificationRecordRepository recordRepository,
            NotificationPreferenceRepository preferenceRepository,
            ConsultationRequestNotificationWriter writer,
            FcmService fcmService,
            AuditService auditService) {
        this(deviceTokenRepository, recordRepository, preferenceRepository, writer,
                fcmService, auditService, Clock.systemDefaultZone());
    }

    ConsultationRequestNotificationServiceImpl(
            DeviceTokenRepository deviceTokenRepository,
            NotificationRecordRepository recordRepository,
            NotificationPreferenceRepository preferenceRepository,
            ConsultationRequestNotificationWriter writer,
            FcmService fcmService,
            AuditService auditService,
            Clock clock) {
        this.deviceTokenRepository = deviceTokenRepository;
        this.recordRepository = recordRepository;
        this.preferenceRepository = preferenceRepository;
        this.writer = writer;
        this.fcmService = fcmService;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Override
    public void notifyCreated(UUID expertUserId, UUID requesterUserId, UUID requestId) {
        enqueue(expertUserId, requestId, "REQUEST_CREATED",
                "Yêu cầu tư vấn mới", "Bạn có một yêu cầu tư vấn mới");
    }

    @Override
    public void notifyAccepted(UUID requesterUserId, UUID expertUserId, UUID requestId) {
        enqueue(requesterUserId, requestId, "REQUEST_ACCEPTED",
                "Yêu cầu đã được chấp nhận", "Chuyên gia đã chấp nhận yêu cầu tư vấn của bạn");
    }

    @Override
    public void notifyRejected(UUID requesterUserId, UUID expertUserId, UUID requestId) {
        enqueue(requesterUserId, requestId, "REQUEST_REJECTED",
                "Yêu cầu đã bị từ chối", "Chuyên gia đã phản hồi yêu cầu tư vấn của bạn");
    }

    @Override
    public void notifyCancelled(UUID expertUserId, UUID requesterUserId, UUID requestId) {
        enqueue(expertUserId, requestId, "REQUEST_CANCELLED",
                "Yêu cầu đã được hủy", "Người gửi đã hủy yêu cầu tư vấn");
    }

    @Override
    public void notifyExpired(UUID requesterUserId, UUID expertUserId, UUID requestId) {
        enqueue(requesterUserId, requestId, "REQUEST_EXPIRED",
                "Yêu cầu đã hết hạn", "Yêu cầu tư vấn của bạn đã hết hạn");
    }

    @Override
    public void retryPendingNotifications() {
        for (UUID id : writer.findPendingIds(50)) {
            recordRepository.findById(id).ifPresent(this::deliver);
        }
    }

    private void enqueue(
            UUID recipientUserId,
            UUID requestId,
            String eventType,
            String title,
            String body) {
        if (!preferenceRepository.isPushEnabled(recipientUserId, NotificationType.CONSULTATION)) {
            return;
        }
        NotificationRecord candidate = NotificationRecord.builder()
                .id(UUID.randomUUID())
                .userId(recipientUserId)
                .type(NotificationType.CONSULTATION)
                .title(title)
                .body(body)
                .referenceId(requestId)
                .referenceType("CONSULTATION_REQUEST")
                .status(NotificationRecordStatus.PENDING)
                .attemptCount(0)
                .createdAt(clock.instant())
                .metadata(Map.of("eventType", eventType))
                .build();
        if (writer.insertIfAbsent(candidate)) {
            deliver(candidate);
        }
    }

    private void deliver(NotificationRecord record) {
        UUID claimToken = writer.claim(record.getId());
        if (claimToken == null) {
            return;
        }
        List<DeviceToken> tokens =
                deviceTokenRepository.findByUserIdAndActiveTrue(record.getUserId());
        if (tokens.isEmpty()) {
            record.setStatus(NotificationRecordStatus.FAILED);
            record.setAttemptCount(0);
            record.setFailedAt(clock.instant());
        } else {
            try {
                FcmDeliveryResult delivery = fcmService.sendWithRetry(
                        tokens.getFirst().getToken(),
                        record.getTitle(),
                        record.getBody(),
                        Map.of(
                                "type", "CONSULTATION_REQUEST",
                                "requestId", record.getReferenceId().toString()),
                        MAX_ATTEMPTS);
                applyDelivery(record, delivery);
            } catch (RuntimeException ex) {
                record.setStatus(NotificationRecordStatus.FAILED);
                record.setAttemptCount(0);
                record.setFailedAt(clock.instant());
            }
        }
        if (writer.complete(record, claimToken)) {
            audit(record);
        }
    }

    private void applyDelivery(NotificationRecord record, FcmDeliveryResult delivery) {
        record.setAttemptCount(delivery.attempts());
        if (delivery.success()) {
            record.setStatus(NotificationRecordStatus.SENT);
            record.setFcmMessageId(delivery.messageId());
            record.setSentAt(Instant.now(clock));
        } else {
            record.setStatus(NotificationRecordStatus.FAILED);
            record.setFailedAt(Instant.now(clock));
        }
    }

    private void audit(NotificationRecord record) {
        AuditAction action = record.getStatus() == NotificationRecordStatus.FAILED
                ? AuditAction.NOTIFICATION_FAILED
                : AuditAction.NOTIFICATION_SENT;
        try {
            auditService.log(
                    action,
                    record.getUserId(),
                    "NotificationRecord",
                    record.getId().toString(),
                    record.getType().name());
        } catch (RuntimeException ignored) {
            // Delivery state remains durable even if audit logging is unavailable.
        }
    }
}
