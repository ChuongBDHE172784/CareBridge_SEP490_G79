package com.carebridge.backend.safety;

import com.carebridge.backend.safety.event.SuspectedFallDetected;
import com.carebridge.backend.safety.service.SuspectedFallDetectedHandler;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;

class SuspectedFallDetectedHandlerTest {

    private final SuspectedFallDetectedHandler handler = new SuspectedFallDetectedHandler();

    private SuspectedFallDetected event(String eventType) {
        return new SuspectedFallDetected(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                eventType, 33.5, BigDecimal.valueOf(10.1), BigDecimal.valueOf(106.2), Instant.now());
    }

    @Test
    void onSuspectedFallDetected_isTelemetryOnlyAndDoesNotOpenASecondEmergencyFlow() {
        SuspectedFallDetected evt = event("SUSPECTED_FALL");

        assertThatCode(() -> handler.onSuspectedFallDetected(evt)).doesNotThrowAnyException();
    }

    @Test
    void onSuspectedFallDetected_acceptsOtherTelemetryWithoutSideEffects() {
        SuspectedFallDetected evt = event("SUSPECTED_IMPACT");

        assertThatCode(() -> handler.onSuspectedFallDetected(evt)).doesNotThrowAnyException();
    }
}
