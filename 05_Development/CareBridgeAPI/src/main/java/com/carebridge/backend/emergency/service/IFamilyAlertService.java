package com.carebridge.backend.emergency.service;

import com.carebridge.backend.emergency.event.EmergencySessionOpened;
import com.carebridge.backend.emergency.event.EmergencySessionRealertRequested;

public interface IFamilyAlertService {
    void sendAlert(EmergencySessionOpened event);

    void sendRealert(EmergencySessionRealertRequested event);
}
