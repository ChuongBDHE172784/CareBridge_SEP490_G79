package com.carebridge.backend.emergency.service;

import com.carebridge.backend.emergency.event.EmergencySessionOpened;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class EmergencySessionOpenedHandler {

    private final IFamilyAlertService familyAlertService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEmergencySessionOpened(EmergencySessionOpened event) {
        familyAlertService.sendAlert(event);
    }
}
