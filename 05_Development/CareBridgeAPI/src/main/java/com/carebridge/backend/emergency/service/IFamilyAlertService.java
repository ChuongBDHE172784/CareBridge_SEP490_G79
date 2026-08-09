package com.carebridge.backend.emergency.service;

import com.carebridge.backend.emergency.event.EmergencySessionOpened;
import com.carebridge.backend.emergency.event.EmergencySessionRealertRequested;

public interface IFamilyAlertService {
    void sendAlert(EmergencySessionOpened event);

    default void sendRealert(EmergencySessionRealertRequested event) {
        throw new UnsupportedOperationException("Emergency re-alert delivery is not configured");
    }
}
