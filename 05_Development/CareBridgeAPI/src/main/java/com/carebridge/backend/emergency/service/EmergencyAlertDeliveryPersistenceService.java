package com.carebridge.backend.emergency.service;

import com.carebridge.backend.emergency.event.EmergencySessionOpened;
import com.carebridge.backend.emergency.event.EmergencySessionRealertRequested;
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
        return prepare(event, recipient, sharedNotificationId, claim, false, false);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PreparedAlertDelivery prepareRealert(
            EmergencySessionRealertRequested event,
            AlertRecipientEndpoint recipient,
            UUID sharedNotificationId,
            EmergencyAlertClaim claim) {
        return prepare(
                new EmergencySessionOpened(event.eventId(), event.sessionId(), event.userId(),
                        event.triggerSource(), event.latitude(), event.longitude(), event.occurredAt()),
                recipient, sharedNotificationId, claim, true, true);
    }

    private PreparedAlertDelivery prepare(
            EmergencySessionOpened event,
            AlertRecipientEndpoint recipient,
            UUID sharedNotificationId,
            EmergencyAlertClaim claim,
            boolean resendSuccessfulDelivery,
            boolean createNewNotification) {
        NotificationRecord notification = sharedNotificationId == null
                ? createNewNotification
                ? createNotification(event, recipient)
                : notificationRepository.findByUserIdAndReferenceIdAndTypeAndReferenceTypeAndCareGroupId(
                        recipient.userId(), event.sessionId(), NotificationType.EMERGENCY,
                        "EMERGENCY_SESSION", recipient.careGroupId())
                    .orElseGet(() -> createNotification(event, recipient))
                : notificationRepository.findById(sharedNotificationId).orElseThrow();

        var existing = resendSuccessfulDelivery
                ? java.util.Optional.<EmergencyAlertDeliveryRepository.DeliveryAction>empty()
                : deliveryRepository.findSuccessful(event.sessionId(), recipient.deviceTokenId());
        if (existing.isPresent()) {
            var delivery = existing.get();
            if (notification.getStatus() != NotificationRecordStatus.SENT) {
                notification.setStatus(NotificationRecordStatus.SENT);
                notification.setSentAt(Instant.now());
                notification.setFailedAt(null);
                notificationRepository.save(notification);
            }
            return new PreparedAlertDelivery(delivery.actionId(), notification.getId(),
                    true, delivery.attempts());
        }

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

    private NotificationRecord createNotification(
            EmergencySessionOpened event, AlertRecipientEndpoint recipient) {
        return notificationRepository.saveAndFlush(NotificationRecord.builder()
                .userId(recipient.userId())
                .type(NotificationType.EMERGENCY)
                .title("Cảnh báo khẩn cấp từ CareBridge")
                .body("Vui lòng kiểm tra tình trạng người thân ngay.")
                .referenceId(event.sessionId())
                .referenceType("EMERGENCY_SESSION")
                .careGroupId(recipient.careGroupId())
                .status(NotificationRecordStatus.PENDING)
                .attemptCount(0)
                .createdAt(Instant.now())
                .metadata(Map.of("triggerSource", event.triggerSource()))
                .build());
    }

    private String truncate(String value) {
        return value == null ? null : value.substring(0, Math.min(120, value.length()));
    }
}
