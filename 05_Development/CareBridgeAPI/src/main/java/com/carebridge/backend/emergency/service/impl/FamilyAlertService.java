package com.carebridge.backend.emergency.service.impl;

import com.carebridge.backend.emergency.entity.FamilyAlertLog;
import com.carebridge.backend.emergency.event.EmergencySessionOpened;
import com.carebridge.backend.emergency.event.FamilyAlertSent;
import com.carebridge.backend.emergency.repository.IFamilyAlertLogRepository;
import com.carebridge.backend.emergency.service.FamilyMemberPort;
import com.carebridge.backend.emergency.service.FamilyAlertDeliveryOutcome;
import com.carebridge.backend.emergency.service.FcmNotificationPort;
import com.carebridge.backend.emergency.service.IFamilyAlertService;
import com.carebridge.backend.emergency.service.LocationConsentPort;
import com.carebridge.backend.emergency.service.SmsFallbackPort;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class FamilyAlertService implements IFamilyAlertService {

    private static final Logger log = LoggerFactory.getLogger(FamilyAlertService.class);

    private final IFamilyAlertLogRepository familyAlertLogRepository;
    private final FamilyMemberPort familyMemberPort;
    private final FcmNotificationPort fcmNotificationPort;
    private final LocationConsentPort locationConsentPort;
    private final SmsFallbackPort smsFallbackPort;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public FamilyAlertDeliveryOutcome sendAlert(EmergencySessionOpened event) {
        // UC65 C1: idempotent — skip if alert already sent for this session
        if (familyAlertLogRepository.existsBySessionId(event.sessionId())) {
            log.info("Family alert delivery outcome=ALREADY_SENT");
            return FamilyAlertDeliveryOutcome.ALREADY_SENT;
        }

        // UC65 C5: send to ALL family members
        List<String> fcmTokens = familyMemberPort.getFamilyFcmTokens(event.userId());
        if (fcmTokens.isEmpty()) {
            log.info("Family alert delivery outcome=NO_RECIPIENTS");
            return FamilyAlertDeliveryOutcome.NO_RECIPIENTS;
        }

        // UC65 C2: include location ONLY if consent=true (PDPA)
        boolean hasConsent = locationConsentPort.hasLocationConsent(event.userId());
        Map<String, String> payload = new HashMap<>();
        payload.put("type", "EMERGENCY_ALERT");
        payload.put("sessionId", event.sessionId().toString());
        payload.put("safetyEventId", event.sessionId().toString());
        payload.put("userId", event.userId().toString());
        payload.put("triggerSource", event.triggerSource());
        payload.put("detectedAt", event.openedAt().toString());

        if (hasConsent && event.latitude() != null && event.longitude() != null) {
            payload.put("latitude", event.latitude().toPlainString());
            payload.put("longitude", event.longitude().toPlainString());
        }

        boolean locationIncluded = hasConsent && event.latitude() != null && event.longitude() != null;

        // UC65 C4: FCM failure must NOT block the service
        try {
            fcmNotificationPort.sendBatch(fcmTokens, payload);
        } catch (Exception e) {
            log.warn("Family alert FCM delivery failed reason={}", e.getClass().getSimpleName());
            smsFallbackPort.sendFallback(event.userId(), event.sessionId(),
                    "Emergency alert fallback triggered. Please check CareBridge immediately.");
        }

        FamilyAlertLog alertLog = FamilyAlertLog.builder()
                .sessionId(event.sessionId())
                .sentAt(Instant.now())
                .recipientCount(fcmTokens.size())
                .locationIncluded(locationIncluded)
                .createdBy("SYSTEM")
                .build();
        familyAlertLogRepository.save(alertLog);

        eventPublisher.publishEvent(new FamilyAlertSent(
                UUID.randomUUID(), event.sessionId(), event.userId(),
                fcmTokens.size(), locationIncluded, Instant.now()));
        return FamilyAlertDeliveryOutcome.DELIVERED;
    }
}
