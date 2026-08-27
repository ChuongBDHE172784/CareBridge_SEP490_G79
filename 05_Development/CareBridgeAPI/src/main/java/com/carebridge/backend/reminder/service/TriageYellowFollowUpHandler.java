package com.carebridge.backend.reminder.service;

import com.carebridge.backend.triage.RiskLevel;
import com.carebridge.backend.triage.event.IntakeSessionCompleted;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * CB-TYFU-IMP-001 ADR-TYFU-002 — AFTER_COMMIT consumer of {@link IntakeSessionCompleted};
 * mirrors {@code ai/service/IntakeSessionCompletedHandler}.
 *
 * @version 1.0
 */
@Component
@RequiredArgsConstructor
public class TriageYellowFollowUpHandler {

    private static final Logger log = LoggerFactory.getLogger(TriageYellowFollowUpHandler.class);

    private final ITriageFollowUpService followUpService;

    /**
     * BR-TYFU-001: YELLOW only — GREEN/RED are no-ops (RED emergency routing untouched,
     * BR-SAFETY). BR-TYFU-003: failures are contained — WARN with exception class name
     * only (no PII), never rethrown, so sibling AFTER_COMMIT listeners are undisturbed.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onIntakeSessionCompleted(IntakeSessionCompleted event) {
        if (event.riskLevel() != RiskLevel.YELLOW) {
            return;
        }
        try {
            followUpService.scheduleFollowUp(event);
        } catch (RuntimeException exception) {
            log.warn("Triage follow-up scheduling failed after commit code=TYFU-003 sessionId={} reason={}",
                    event.sessionId(), exception.getClass().getSimpleName());
        }
    }
}
