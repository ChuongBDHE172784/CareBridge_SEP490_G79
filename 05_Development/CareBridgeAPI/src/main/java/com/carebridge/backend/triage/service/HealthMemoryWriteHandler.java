package com.carebridge.backend.triage.service;

import com.carebridge.backend.triage.event.IntakeSessionCompleted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * CB-TRIAGE-THMC-IMP-001 — ADR-THMC-001: writes a health-context memory AFTER the
 * triage completion transaction commits, mirroring IntakeSessionCompletedHandler.
 * Failures are caught and logged (no PII) — a memory-write bug must never fail or
 * roll back a COMPLETED triage session (BR-SAFETY / BR-THMC-004).
 *
 * @version 1.0
 */
@Component
public class HealthMemoryWriteHandler {

    private static final Logger log = LoggerFactory.getLogger(HealthMemoryWriteHandler.class);

    private final HealthMemoryService healthMemoryService;

    public HealthMemoryWriteHandler(HealthMemoryService healthMemoryService) {
        this.healthMemoryService = healthMemoryService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onIntakeSessionCompleted(IntakeSessionCompleted event) {
        try {
            healthMemoryService.writeFromCompletedSession(event.sessionId(), event.userId());
        } catch (RuntimeException exception) {
            // Catch-and-log (IntakeSessionCompletedHandler precedent). No summary text,
            // no user identifiers — class name only (PDPA log hygiene, TDS §14.2).
            log.warn("Health memory write failed after commit reason={}",
                    exception.getClass().getSimpleName());
        }
    }
}
