package com.carebridge.backend.ai.service;

import com.carebridge.backend.triage.event.IntakeSessionCompleted;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class IntakeSessionCompletedHandler {

    private static final Logger log = LoggerFactory.getLogger(IntakeSessionCompletedHandler.class);

    private final IStructuredIntakeService structuredIntakeService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onIntakeSessionCompleted(IntakeSessionCompleted event) {
        try {
            structuredIntakeService.extract(event);
        } catch (RuntimeException exception) {
            log.warn("Structured intake side work failed after commit reason={}",
                    exception.getClass().getSimpleName());
        }
    }
}
