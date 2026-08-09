package com.carebridge.backend.emergency.service.impl;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.emergency.event.EmergencySessionOpened;
import com.carebridge.backend.emergency.event.FamilyAlertSent;
import com.carebridge.backend.emergency.service.AlertRecipientEndpoint;
import com.carebridge.backend.emergency.service.EmergencyAlertAttemptService;
import com.carebridge.backend.emergency.service.EmergencyAlertClaim;
import com.carebridge.backend.emergency.service.EmergencyAlertProviderFence;
import com.carebridge.backend.emergency.service.EmergencyAlertDeliveryPersistenceService;
import com.carebridge.backend.emergency.service.FamilyMemberPort;
import com.carebridge.backend.emergency.service.FcmNotificationPort;
import com.carebridge.backend.emergency.service.FencedAlertDelivery;
import com.carebridge.backend.emergency.service.IFamilyAlertService;
import com.carebridge.backend.emergency.service.LocationConsentPort;
import com.carebridge.backend.emergency.service.PreparedAlertDelivery;
import com.carebridge.backend.emergency.service.SmsFallbackPort;
import com.carebridge.backend.notification.dto.FcmDeliveryResult;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FamilyAlertService implements IFamilyAlertService {
    private static final Logger log = LoggerFactory.getLogger(FamilyAlertService.class);

    private final FamilyMemberPort familyMemberPort;
    private final FcmNotificationPort fcmNotificationPort;
    private final LocationConsentPort locationConsentPort;
    private final SmsFallbackPort smsFallbackPort;
    private final ApplicationEventPublisher eventPublisher;
    private final EmergencyAlertAttemptService alertAttemptService;
    private final EmergencyAlertProviderFence providerFence;
    private final EmergencyAlertDeliveryPersistenceService deliveryPersistenceService;
    private final AuditService auditService;

    @Override
    public void sendAlert(EmergencySessionOpened event) {
        var claimed = alertAttemptService.claim(event.sessionId());
        if (claimed.isEmpty()) {
            log.info("Alert attempt is complete or leased for session [{}] — skipping replay", event.sessionId());
            return;
        }
        EmergencyAlertClaim claim = claimed.get();

        List<AlertRecipientEndpoint> recipients = familyMemberPort.getFamilyAlertRecipients(event.userId());
        if (recipients.isEmpty()) {
            alertAttemptService.complete(claim, "NO_RECIPIENTS", 0, 0, false);
            return;
        }

        boolean locationIncluded = locationConsentPort.hasLocationConsent(event.userId())
                && event.latitude() != null && event.longitude() != null;
        Map<String, String> payload = payload(event, locationIncluded);
        Map<RecipientScope, UUID> notificationByRecipient = new HashMap<>();
        Map<UUID, Boolean> recipientSucceeded = new HashMap<>();

        for (AlertRecipientEndpoint recipient : recipients) {
            if (!alertAttemptService.renew(claim)) {
                log.info("Alert claim became stale or resolved before delivery intent for session [{}]",
                        event.sessionId());
                return;
            }
            RecipientScope recipientScope = new RecipientScope(
                    recipient.userId(), recipient.careGroupId());
            PreparedAlertDelivery prepared = deliveryPersistenceService.prepare(
                    event, recipient, notificationByRecipient.get(recipientScope), claim);
            notificationByRecipient.putIfAbsent(recipientScope, prepared.notificationRecordId());
            if (prepared.alreadySuccessful()) {
                recipientSucceeded.merge(recipient.userId(), true, Boolean::logicalOr);
                continue;
            }
            Map<String, String> deliveryPayload = new HashMap<>(payload);
            deliveryPayload.put("deliveryActionId", prepared.deliveryId().toString());

            auditDeliveryAttempt(event, recipient, prepared.deliveryId());

            var fencedDelivery = providerFence.execute(claim, () -> {
                FcmDeliveryResult providerResult;
                try {
                    providerResult = fcmNotificationPort.send(
                            recipient.token(), deliveryPayload);
                } catch (Exception exception) {
                    log.warn(
                            "FCM send failed session=[{}] providerCode=FCM_EXCEPTION exceptionType={}",
                            event.sessionId(), exception.getClass().getSimpleName());
                    providerResult = FcmDeliveryResult.failed("FCM_EXCEPTION", 1);
                }
                boolean recorded = deliveryPersistenceService.complete(prepared, claim, providerResult);
                return new FencedAlertDelivery(providerResult, recorded);
            });
            if (fencedDelivery.isEmpty()) {
                log.info("Alert claim became stale or resolved at provider fence for session [{}]",
                        event.sessionId());
                return;
            }
            FencedAlertDelivery outcome = fencedDelivery.get();
            if (!outcome.recorded()) {
                log.info("Alert claim became stale or resolved after send for session [{}]",
                        event.sessionId());
                return;
            }
            FcmDeliveryResult result = outcome.result();
            recipientSucceeded.merge(recipient.userId(), result.success(), Boolean::logicalOr);
        }

        int successfulRecipients = (int) recipientSucceeded.values().stream()
                .filter(Boolean::booleanValue).count();
        int failedRecipients = recipientSucceeded.size() - successfulRecipients;
        String status = successfulRecipients == 0 ? "FAILED" : failedRecipients == 0 ? "SENT" : "PARTIAL";
        if (!alertAttemptService.complete(claim, status,
                successfulRecipients, failedRecipients, locationIncluded)) {
            log.info("Alert claim could not complete because its fence is stale for session [{}]",
                    event.sessionId());
            return;
        }

        if (successfulRecipients == 0) {
            smsFallbackPort.sendFallback(event.userId(), event.sessionId(),
                    "Emergency alert fallback triggered. Please check CareBridge immediately.");
            return;
        }
        eventPublisher.publishEvent(new FamilyAlertSent(
                UUID.randomUUID(), event.sessionId(), event.userId(),
                successfulRecipients, locationIncluded, Instant.now()));
    }

    private Map<String, String> payload(EmergencySessionOpened event, boolean locationIncluded) {
        Map<String, String> payload = new HashMap<>();
        payload.put("type", "EMERGENCY_ALERT");
        payload.put("sessionId", event.sessionId().toString());
        payload.put("safetyEventId", event.sessionId().toString());
        payload.put("userId", event.userId().toString());
        payload.put("triggerSource", event.triggerSource());
        payload.put("detectedAt", event.openedAt().toString());
        if (locationIncluded) {
            payload.put("latitude", event.latitude().toPlainString());
            payload.put("longitude", event.longitude().toPlainString());
        }
        return payload;
    }

    private void auditDeliveryAttempt(EmergencySessionOpened event, AlertRecipientEndpoint recipient,
                                      UUID deliveryId) {
        auditService.log(AuditAction.EMERGENCY_ALERT_DELIVERY, event.userId(),
                "EmergencyAlertDelivery", deliveryId.toString(),
                Map.of("recipientUserId", recipient.userId().toString(),
                        "emergencySessionId", event.sessionId().toString(),
                        "status", "PENDING"));
    }

    private record RecipientScope(UUID userId, UUID careGroupId) {
    }
}
