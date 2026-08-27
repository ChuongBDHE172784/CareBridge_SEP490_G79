package com.carebridge.backend.emergency.service;

import com.carebridge.backend.emergency.event.EmergencySessionOpened;
import com.carebridge.backend.emergency.event.EmergencySessionRealertRequested;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class EmergencySessionOpenedHandler {

    private static final Logger log = LoggerFactory.getLogger(EmergencySessionOpenedHandler.class);
    private final IFamilyAlertService familyAlertService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEmergencySessionOpened(EmergencySessionOpened event) {
        try {
            familyAlertService.sendAlert(event);
        } catch (RuntimeException exception) {
            // The emergency session is already durable. Canonical safety-event attempts
            // remain retryable after their lease expires, so delivery must not fail the API.
            log.error("Emergency notification after-commit dispatch failed reason={}",
                    exception.getClass().getSimpleName());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEmergencySessionRealertRequested(EmergencySessionRealertRequested event) {
        try {
            familyAlertService.sendRealert(event);
        } catch (RuntimeException exception) {
            log.error("Emergency re-alert after-commit dispatch failed reason={}",
                    exception.getClass().getSimpleName());
        }
    }
}
