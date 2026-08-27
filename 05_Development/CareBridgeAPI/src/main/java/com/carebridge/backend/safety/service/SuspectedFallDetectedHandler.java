package com.carebridge.backend.safety.service;

import com.carebridge.backend.safety.event.SuspectedFallDetected;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Observes a durably recorded suspected fall for logging/telemetry.
 *
 * <p>This handler deliberately does not open an emergency flow. Escalation
 * remains controlled by the user's response or countdown workflow.</p>
 */
@Component
@Slf4j
public class SuspectedFallDetectedHandler {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSuspectedFallDetected(SuspectedFallDetected event) {
        log.info("Observed persisted suspected fall [{}] for user [{}]",
                event.safetyEventId(), event.userId());
    }
}
