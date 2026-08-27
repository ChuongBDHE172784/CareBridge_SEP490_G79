package com.carebridge.backend.emergency.service;

import com.carebridge.backend.ai.event.EmergencyEscalationTriggered;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmergencyEscalationHandler {

    private static final Logger log = LoggerFactory.getLogger(EmergencyEscalationHandler.class);

    private final IEmergencyService emergencyService;

    @EventListener
    public void onEmergencyEscalationTriggered(EmergencyEscalationTriggered event) {
        log.info("Emergency escalation processing started source=AUTO_TRIAGE");
        try {
            emergencyService.openOrReuseFromTriage(event.sessionId(), event.userId());
        } catch (RuntimeException exception) {
            log.error("Emergency escalation processing failed reason={}",
                    exception.getClass().getSimpleName());
            throw exception;
        }
    }
}
