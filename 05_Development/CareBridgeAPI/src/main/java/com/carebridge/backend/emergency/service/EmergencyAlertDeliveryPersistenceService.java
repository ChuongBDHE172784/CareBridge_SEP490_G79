package com.carebridge.backend.emergency.service;

import com.carebridge.backend.emergency.entity.EmergencyAlertDelivery;
import com.carebridge.backend.emergency.event.EmergencySessionOpened;
import com.carebridge.backend.emergency.repository.EmergencyAlertDeliveryRepository;
import com.carebridge.backend.notification.dto.FcmDeliveryResult;
import com.carebridge.backend.notification.entity.NotificationRecord;
import com.carebridge.backend.notification.entity.NotificationRecordStatus;
import com.carebridge.backend.notification.entity.NotificationType;
import com.carebridge.backend.notification.repository.NotificationRecordRepository;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmergencyAlertDeliveryPersistenceService {
    private final EmergencyAlertDeliveryRepository deliveryRepository;
    private final NotificationRecordRepository notificationRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PreparedAlertDelivery prepare(
            EmergencySessionOpened event,
            AlertRecipientEndpoint recipient,
            UUID sharedNotificationId) {
        var existing = deliveryRepository.findByEmergencySessionIdAndDeviceTokenId(
                event.sessionId(), recipient.deviceTokenId());
        if (existing.isPresent()) {
            EmergencyAlertDelivery delivery = existing.get();
            boolean successful = "SENT".equals(delivery.getDeliveryStatus())
                    || "DELIVERED".equals(delivery.getDeliveryStatus());
            if (!successful) {
                delivery.setDeliveryStatus("PENDING");
                delivery.setFailureCode(null);
                delivery.setFcmMessageId(null);
                delivery.setDeliveredAt(null);
                deliveryRepository.save(delivery);
            }
            return new PreparedAlertDelivery(delivery.getId(), delivery.getNotificationRecordId(),
                    successful, delivery.getAttemptCount());
        }

        NotificationRecord notification = sharedNotificationId == null
                ? notificationRepository.findByUserIdAndReferenceIdAndTypeAndReferenceType(
                        recipient.userId(), event.sessionId(), NotificationType.EMERGENCY, "EMERGENCY_SESSION")
                    .orElseGet(() -> notificationRepository.save(NotificationRecord.builder()
                            .userId(recipient.userId())
                            .type(NotificationType.EMERGENCY)
                            .title("Cảnh báo khẩn cấp từ CareBridge")
                            .body("Vui lòng kiểm tra tình trạng người thân ngay.")
                            .referenceId(event.sessionId())
                            .referenceType("EMERGENCY_SESSION")
                            .status(NotificationRecordStatus.PENDING)
                            .attemptCount(0)
                            .createdAt(Instant.now())
                            .metadata(Map.of("triggerSource", event.triggerSource()))
                            .build()))
                : notificationRepository.findById(sharedNotificationId).orElseThrow();

        EmergencyAlertDelivery delivery = deliveryRepository.save(EmergencyAlertDelivery.builder()
                .emergencySessionId(event.sessionId())
                .recipientUserId(recipient.userId())
                .deviceTokenId(recipient.deviceTokenId())
                .notificationRecordId(notification.getId())
                .deliveryStatus("PENDING")
                .attemptCount(0)
                .createdAt(Instant.now())
                .build());
        return new PreparedAlertDelivery(delivery.getId(), notification.getId(), false, 0);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void complete(UUID deliveryId, FcmDeliveryResult result) {
        EmergencyAlertDelivery delivery = deliveryRepository.findById(deliveryId).orElseThrow();
        Instant now = Instant.now();
        delivery.setDeliveryStatus(result.success() ? "SENT" : "FAILED");
        delivery.setAttemptCount(delivery.getAttemptCount() + result.attempts());
        delivery.setFcmMessageId(result.messageId());
        delivery.setFailureCode(truncate(result.errorCode()));
        delivery.setDeliveredAt(result.success() ? now : null);
        deliveryRepository.save(delivery);

        NotificationRecord notification = notificationRepository.findById(delivery.getNotificationRecordId())
                .orElseThrow();
        notification.setAttemptCount(notification.getAttemptCount() + result.attempts());
        if (result.success()) {
            notification.setStatus(NotificationRecordStatus.SENT);
            notification.setSentAt(now);
            notification.setFailedAt(null);
        } else if (notification.getStatus() != NotificationRecordStatus.SENT) {
            notification.setStatus(NotificationRecordStatus.FAILED);
            notification.setFailedAt(now);
        }
        notificationRepository.save(notification);
    }

    private String truncate(String value) {
        return value == null ? null : value.substring(0, Math.min(120, value.length()));
    }
}
