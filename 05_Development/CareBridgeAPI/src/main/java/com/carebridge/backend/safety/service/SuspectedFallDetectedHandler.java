package com.carebridge.backend.safety.service;

import com.carebridge.backend.safety.event.SuspectedFallDetected;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Observes the completed escalation for logging/telemetry.
 *
 * <p>FallDetectionService opens and links the idempotent emergency session
 * before publishing this event. This handler deliberately does not open a
 * second flow.</p>
 */
@Component
@Slf4j
public class SuspectedFallDetectedHandler {

    @EventListener
    public void onSuspectedFallDetected(SuspectedFallDetected event) {
        log.info("Observed escalated suspected fall [{}] for user [{}]",
                event.safetyEventId(), event.userId());
    }
}
