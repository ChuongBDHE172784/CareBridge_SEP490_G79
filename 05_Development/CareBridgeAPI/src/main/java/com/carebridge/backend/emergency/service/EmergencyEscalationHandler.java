package com.carebridge.backend.emergency.service;

import com.carebridge.backend.ai.event.EmergencyEscalationTriggered;
import com.carebridge.backend.emergency.dto.request.OpenEmergencyRequest;
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
        log.info("Emergency escalation triggered for session [{}] user [{}]",
                event.sessionId(), event.userId());
        OpenEmergencyRequest request = OpenEmergencyRequest.builder()
                .triggerSource(event.triggerSource())
                .build();
        emergencyService.openFlow(request, event.userId());
    }
}
