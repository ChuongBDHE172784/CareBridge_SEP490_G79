package com.carebridge.backend.emergency.service.impl;

import com.carebridge.backend.emergency.entity.FamilyAlertLog;
import com.carebridge.backend.emergency.event.EmergencySessionOpened;
import com.carebridge.backend.emergency.event.FamilyAlertSent;
import com.carebridge.backend.emergency.repository.IFamilyAlertLogRepository;
import com.carebridge.backend.emergency.service.FamilyMemberPort;
import com.carebridge.backend.emergency.service.FcmNotificationPort;
import com.carebridge.backend.emergency.service.IFamilyAlertService;
import com.carebridge.backend.emergency.service.LocationConsentPort;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
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
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void sendAlert(EmergencySessionOpened event) {
        // UC65 C1: idempotent — skip if alert already sent for this session
        if (familyAlertLogRepository.existsBySessionId(event.sessionId())) {
            log.info("Alert already sent for session [{}] — skipping", event.sessionId());
            return;
        }

        // UC65 C5: send to ALL family members
        List<String> fcmTokens = familyMemberPort.getFamilyFcmTokens(event.userId());
        if (fcmTokens.isEmpty()) {
            log.info("No family members registered for user [{}]", event.userId());
            return;
        }

        // UC65 C2: include location ONLY if consent=true (PDPA)
        boolean hasConsent = locationConsentPort.hasLocationConsent(event.userId());
        Map<String, String> payload = new HashMap<>();
        payload.put("type", "EMERGENCY_ALERT");
        payload.put("sessionId", event.sessionId().toString());
        payload.put("triggerSource", event.triggerSource());

        if (hasConsent && event.latitude() != null && event.longitude() != null) {
            payload.put("latitude", event.latitude().toPlainString());
            payload.put("longitude", event.longitude().toPlainString());
        }

        boolean locationIncluded = hasConsent && event.latitude() != null;

        // UC65 C4: FCM failure must NOT block the service
        try {
            fcmNotificationPort.sendBatch(fcmTokens, payload);
        } catch (Exception e) {
            log.warn("FCM send failed for session [{}]: {}", event.sessionId(), e.getMessage());
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
    }
}
