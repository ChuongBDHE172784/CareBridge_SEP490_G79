package com.carebridge.backend.emergency;

import com.carebridge.backend.emergency.dto.request.OpenEmergencyRequest;
import com.carebridge.backend.emergency.entity.EmergencySession;
import com.carebridge.backend.emergency.event.EmergencySessionOpened;
import java.time.Instant;
import java.util.UUID;

class EmergencyTestFactory {

    static EmergencySession makeActiveSession() {
        return EmergencySession.builder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000001"))
                .userId(UUID.fromString("00000000-0000-0000-0000-000000000010"))
                .status(EmergencyStatus.ACTIVE)
                .triggerSource("MANUAL")
                .createdAt(Instant.parse("2026-06-27T08:00:00Z"))
                .build();
    }

    static OpenEmergencyRequest makeOpenRequest() {
        return OpenEmergencyRequest.builder()
                .triggerSource("MANUAL")
                .build();
    }

    static EmergencySessionOpened makeEmergencySessionOpenedEvent() {
        return new EmergencySessionOpened(
                UUID.randomUUID(),
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                UUID.fromString("00000000-0000-0000-0000-000000000010"),
                "MANUAL",
                null,
                null,
                Instant.parse("2026-06-27T08:00:00Z"));
    }
}
