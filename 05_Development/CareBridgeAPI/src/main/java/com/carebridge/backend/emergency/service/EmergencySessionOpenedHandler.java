package com.carebridge.backend.emergency.service;

import com.carebridge.backend.emergency.event.EmergencySessionOpened;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmergencySessionOpenedHandler {

    private final IFamilyAlertService familyAlertService;

    @EventListener
    public void onEmergencySessionOpened(EmergencySessionOpened event) {
        familyAlertService.sendAlert(event);
    }
}
