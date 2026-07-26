package com.carebridge.backend.emergency.service;

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
            UUID sharedNotificationId,
            EmergencyAlertClaim claim) {
        var existing = deliveryRepository.findSuccessful(event.sessionId(), recipient.deviceTokenId());
        if (existing.isPresent()) {
            var delivery = existing.get();
            return new PreparedAlertDelivery(delivery.actionId(), delivery.notificationRecordId(),
                    true, delivery.attempts());
        }

        NotificationRecord notification = sharedNotificationId == null
                ? notificationRepository.findByUserIdAndReferenceIdAndTypeAndReferenceType(
                        recipient.userId(), event.sessionId(), NotificationType.EMERGENCY, "EMERGENCY_SESSION")
                    .orElseGet(() -> notificationRepository.saveAndFlush(NotificationRecord.builder()
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

        var delivery = deliveryRepository.insertIntent(
                claim, recipient.userId(), recipient.deviceTokenId(), notification.getId());
        return new PreparedAlertDelivery(delivery.actionId(), notification.getId(), false, 0);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public boolean complete(
            PreparedAlertDelivery prepared,
            EmergencyAlertClaim claim,
            FcmDeliveryResult result) {
        boolean appended = deliveryRepository.appendResult(
                prepared.deliveryId(), claim, result.success(), result.attempts(),
                result.messageId(), truncate(result.errorCode()));
        if (!appended) {
            return false;
        }
        Instant now = Instant.now();
        NotificationRecord notification = notificationRepository.findById(prepared.notificationRecordId())
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
        return true;
    }

    private String truncate(String value) {
        return value == null ? null : value.substring(0, Math.min(120, value.length()));
    }
}
