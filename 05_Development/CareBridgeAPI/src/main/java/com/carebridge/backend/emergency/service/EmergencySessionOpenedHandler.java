package com.carebridge.backend.emergency.service;

import com.carebridge.backend.emergency.event.EmergencySessionOpened;
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
    private final EmergencyNotificationOutboxDeliveryService deliveryService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEmergencySessionOpened(EmergencySessionOpened event) {
        try {
            deliveryService.deliver(event.sessionId());
        } catch (RuntimeException exception) {
            // The emergency transaction is already committed; optional delivery must never
            // surface as an API failure. The durable PENDING row remains available for retry.
            log.error("Emergency notification after-commit dispatch failed reason={}",
                    exception.getClass().getSimpleName());
        }
    }
}
