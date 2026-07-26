package com.carebridge.backend.reminder;

import com.carebridge.backend.reminder.service.ITriageFollowUpService;
import com.carebridge.backend.reminder.service.TriageYellowFollowUpHandler;
import com.carebridge.backend.triage.RiskLevel;
import com.carebridge.backend.triage.event.IntakeSessionCompleted;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import static com.carebridge.backend.reminder.TriageFollowUpTestFactory.T0;
import static com.carebridge.backend.reminder.TriageFollowUpTestFactory.makeEvent;
import static com.carebridge.backend.reminder.TriageFollowUpTestFactory.makeYellowEvent;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * CB-TYFU-TDD-001 — TYFU-TC-02 / TYFU-TC-03 / TYFU-TC-09 / TYFU-TC-10.
 * Oracles: BR-TYFU-001 (YELLOW only), BR-SAFETY (RED untouched), BR-TYFU-003
 * (containment), ADR-TYFU-002 (AFTER_COMMIT wiring); pattern precedent
 * IntakeSessionCompletedHandlerTest.
 */
@ExtendWith(MockitoExtension.class)
class TriageYellowFollowUpHandlerTest {

    @Mock
    private ITriageFollowUpService followUpService;

    @InjectMocks
    private TriageYellowFollowUpHandler handler;

    // ── TYFU-TC-02 — GREEN creates nothing (BR-TYFU-001) ────────────────────────

    @Test
    void tyfuTc02_greenCompletion_neverInvokesFollowUpService() {
        IntakeSessionCompleted event = makeEvent(RiskLevel.GREEN, T0); // FX-003

        handler.onIntakeSessionCompleted(event);

        verifyNoInteractions(followUpService);
    }

    // ── TYFU-TC-03 — RED creates nothing, emergency path untouched (BR-SAFETY) ──

    @Test
    void tyfuTc03_redCompletion_neverInvokesFollowUpService_andReturnsNormally() {
        IntakeSessionCompleted event = makeEvent(RiskLevel.RED, T0); // FX-003

        assertThatCode(() -> handler.onIntakeSessionCompleted(event))
                .doesNotThrowAnyException();

        verifyNoInteractions(followUpService);
    }

    // ── TYFU-TC-09 — service failure contained, never rethrown (BR-TYFU-003) ────

    @Test
    void tyfuTc09_serviceFailure_isContained_neverEscapesAfterCommitHandler() {
        IntakeSessionCompleted event = makeYellowEvent(); // FX-002
        doThrow(new IllegalStateException("synthetic persistence failure"))
                .when(followUpService).scheduleFollowUp(event);

        assertThatCode(() -> handler.onIntakeSessionCompleted(event))
                .doesNotThrowAnyException();

        verify(followUpService, times(1)).scheduleFollowUp(event);
    }

    // ── TYFU-TC-10 — AFTER_COMMIT annotation contract (ADR-TYFU-002) ────────────

    @Test
    void tyfuTc10_handlerIsWiredAfterCommit_withoutFallbackExecution() throws Exception {
        var method = TriageYellowFollowUpHandler.class
                .getMethod("onIntakeSessionCompleted", IntakeSessionCompleted.class);

        TransactionalEventListener listener = method.getAnnotation(TransactionalEventListener.class);

        assertThat(listener).isNotNull();
        assertThat(listener.phase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
        assertThat(listener.fallbackExecution()).isFalse();
    }
}
