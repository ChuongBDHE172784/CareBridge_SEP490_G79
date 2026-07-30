package com.carebridge.backend.safety.service;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.carebridge.backend.emergency.event.FamilyAlertSent;
import com.carebridge.backend.safety.SafetyEventStatus;
import com.carebridge.backend.safety.repository.ISafetyEventRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FamilyAlertSentHandlerTest {

    private static final UUID SESSION_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID USER_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000102");

    @Mock private ISafetyEventRepository safetyEventRepository;
    private FamilyAlertSentHandler handler;

    @BeforeEach
    void setUp() {
        handler = new FamilyAlertSentHandler(safetyEventRepository);
    }

    @Test
    void successfulDeliveryTransitionsOnlyEscalationRequestedEvents() {
        FamilyAlertSent event = event(2);

        handler.onFamilyAlertSent(event);

        verify(safetyEventRepository).transitionAlertSentByEmergencySessionId(
                SESSION_ID,
                SafetyEventStatus.ESCALATION_REQUESTED,
                SafetyEventStatus.EMERGENCY_ALERT_SENT);
    }

    @Test
    void noSuccessfulRecipientsLeavesEscalationUnchanged() {
        handler.onFamilyAlertSent(event(0));

        verifyNoInteractions(safetyEventRepository);
    }

    @Test
    void replayIsIdempotentBecauseConditionalTransitionCannotMatchTwice() {
        FamilyAlertSent event = event(1);
        when(safetyEventRepository.transitionAlertSentByEmergencySessionId(
                SESSION_ID,
                SafetyEventStatus.ESCALATION_REQUESTED,
                SafetyEventStatus.EMERGENCY_ALERT_SENT))
                .thenReturn(1, 0);

        handler.onFamilyAlertSent(event);
        handler.onFamilyAlertSent(event);

        verify(safetyEventRepository, org.mockito.Mockito.times(2))
                .transitionAlertSentByEmergencySessionId(
                        SESSION_ID,
                        SafetyEventStatus.ESCALATION_REQUESTED,
                        SafetyEventStatus.EMERGENCY_ALERT_SENT);
        verify(safetyEventRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    private static FamilyAlertSent event(int recipientCount) {
        return new FamilyAlertSent(
                UUID.randomUUID(),
                SESSION_ID,
                USER_ID,
                recipientCount,
                false,
                Instant.parse("2026-07-28T00:00:00Z"));
    }
}
