package com.carebridge.backend.emergency;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.carebridge.backend.ai.event.EmergencyEscalationTriggered;
import com.carebridge.backend.emergency.dto.request.OpenEmergencyRequest;
import com.carebridge.backend.emergency.service.EmergencyEscalationHandler;
import com.carebridge.backend.emergency.service.IEmergencyService;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmergencyEscalationHandlerTest {

    @Mock
    private IEmergencyService emergencyService;

    @InjectMocks
    private EmergencyEscalationHandler handler;

    @Test
    void onEmergencyEscalationTriggered_shouldForwardCanonicalIntakeIdToIdempotentBoundary() {
        UUID intakeSessionId = UUID.fromString("00000000-0000-0000-0000-000000000040");
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000010");
        EmergencyEscalationTriggered event = new EmergencyEscalationTriggered(
                UUID.fromString("00000000-0000-0000-0000-000000000050"),
                intakeSessionId,
                userId,
                "AUTO_TRIAGE",
                Instant.parse("2026-07-22T03:00:00Z"));

        handler.onEmergencyEscalationTriggered(event);

        verify(emergencyService).openOrReuseFromTriage(intakeSessionId, userId);
        verify(emergencyService, never()).openFlow(org.mockito.ArgumentMatchers.any(OpenEmergencyRequest.class),
                org.mockito.ArgumentMatchers.any(UUID.class));
    }
}
