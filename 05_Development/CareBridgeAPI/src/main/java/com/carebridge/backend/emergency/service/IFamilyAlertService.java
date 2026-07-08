package com.carebridge.backend.emergency.service;

import com.carebridge.backend.emergency.event.EmergencySessionOpened;

public interface IFamilyAlertService {
    void sendAlert(EmergencySessionOpened event);
}
