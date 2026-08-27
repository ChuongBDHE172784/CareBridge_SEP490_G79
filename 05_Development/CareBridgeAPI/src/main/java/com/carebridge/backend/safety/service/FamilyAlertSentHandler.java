package com.carebridge.backend.safety.service;

import com.carebridge.backend.emergency.event.FamilyAlertSent;
import com.carebridge.backend.safety.SafetyEventStatus;
import com.carebridge.backend.safety.repository.ISafetyEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class FamilyAlertSentHandler {

    private final ISafetyEventRepository safetyEventRepository;

    @EventListener
    @Transactional
    public void onFamilyAlertSent(FamilyAlertSent event) {
        if (event.recipientCount() <= 0) {
            return;
        }

        safetyEventRepository.transitionAlertSentByEmergencySessionId(
                event.sessionId(),
                SafetyEventStatus.ESCALATION_REQUESTED,
                SafetyEventStatus.EMERGENCY_ALERT_SENT);
    }
}
