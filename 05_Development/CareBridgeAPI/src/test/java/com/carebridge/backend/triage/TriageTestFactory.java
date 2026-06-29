package com.carebridge.backend.triage;

import com.carebridge.backend.triage.dto.request.RunIntakeRequest;
import com.carebridge.backend.triage.entity.IntakeSession;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Consumer;

class TriageTestFactory {

    static IntakeSession makeIntakeSession() {
        return IntakeSession.builder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000001"))
                .userId(UUID.fromString("00000000-0000-0000-0000-000000000010"))
                .symptoms("SYNTHETIC_SYMPTOMS_TEST_DATA")
                .status(IntakeStatus.PENDING)
                .createdAt(Instant.parse("2026-06-27T08:00:00Z"))
                .createdBy(UUID.fromString("00000000-0000-0000-0000-000000000010"))
                .build();
    }

    static IntakeSession makeIntakeSession(Consumer<IntakeSession> overrides) {
        IntakeSession session = makeIntakeSession();
        overrides.accept(session);
        return session;
    }

    static RunIntakeRequest makeRunIntakeRequest() {
        RunIntakeRequest req = new RunIntakeRequest();
        req.setSymptoms("Đau đầu nhẹ, sốt 37.5°C — SYNTHETIC");
        return req;
    }
}
