package com.carebridge.backend.safety.service;

import com.carebridge.backend.emergency.dto.request.OpenEmergencyRequest;
import com.carebridge.backend.emergency.service.IEmergencyService;
import com.carebridge.backend.safety.SafetyEventType;
import com.carebridge.backend.safety.event.SuspectedFallDetected;
import com.carebridge.backend.safety.repository.ISafetyConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * UC-141: Open Emergency Support from Alert.
 * Closes the gap between UC-136 (detection) and the emergency flow — only
 * escalates on SUSPECTED_FALL (the higher-confidence classification, not
 * SUSPECTED_IMPACT) and only when the user opted into
 * SafetyMonitoringConfig.emergencyAutoAlert, to avoid auto-dialing emergency
 * services on a minor false positive.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SuspectedFallDetectedHandler {

    private final ISafetyConfigRepository safetyConfigRepository;
    private final IEmergencyService emergencyService;

    @EventListener
    public void onSuspectedFallDetected(SuspectedFallDetected event) {
        if (!SafetyEventType.SUSPECTED_FALL.name().equals(event.eventType())) {
            return;
        }

        boolean autoAlertEnabled = safetyConfigRepository.findByUserId(event.userId())
                .map(com.carebridge.backend.safety.entity.SafetyMonitoringConfig::isEmergencyAutoAlert)
                .orElse(false);

        if (!autoAlertEnabled) {
            log.info("Suspected fall for user [{}] but emergencyAutoAlert is disabled — skipping auto-escalation", event.userId());
            return;
        }

        OpenEmergencyRequest request = OpenEmergencyRequest.builder()
                .triggerSource("FALL_DETECTION")
                .userLatitude(event.latitude())
                .userLongitude(event.longitude())
                .build();

        emergencyService.openFlow(request, event.userId());
        log.info("Auto-escalated suspected fall [{}] to emergency flow for user [{}]", event.safetyEventId(), event.userId());
    }
}
