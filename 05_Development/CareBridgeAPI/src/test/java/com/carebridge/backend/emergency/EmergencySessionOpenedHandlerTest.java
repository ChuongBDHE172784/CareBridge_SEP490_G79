package com.carebridge.backend.emergency;

import com.carebridge.backend.emergency.entity.EmergencyNotificationOutbox;
import com.carebridge.backend.emergency.entity.EmergencySession;
import com.carebridge.backend.emergency.event.EmergencySessionOpened;
import com.carebridge.backend.emergency.repository.EmergencyNotificationOutboxRepository;
import com.carebridge.backend.emergency.repository.IEmergencySessionRepository;
import com.carebridge.backend.emergency.service.EmergencyNotificationOutboxDeliveryService;
import com.carebridge.backend.emergency.service.EmergencyNotificationOutboxWriter;
import com.carebridge.backend.emergency.service.EmergencySessionOpenedHandler;
import com.carebridge.backend.emergency.service.FamilyAlertDeliveryOutcome;
import com.carebridge.backend.emergency.service.IFamilyAlertService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmergencySessionOpenedHandlerTest {

    private static final UUID SESSION_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000066");

    @Mock private IFamilyAlertService familyAlertService;
    @Mock private EmergencyNotificationOutboxRepository outboxRepository;
    @Mock private IEmergencySessionRepository emergencySessionRepository;

    private EmergencySessionOpenedHandler handler;
    private EmergencyNotificationOutboxWriter writer;

    @BeforeEach
    void setUp() {
        writer = new EmergencyNotificationOutboxWriter(outboxRepository, emergencySessionRepository);
        EmergencyNotificationOutboxDeliveryService deliveryService =
                new EmergencyNotificationOutboxDeliveryService(
                        writer, familyAlertService, outboxRepository);
        handler = new EmergencySessionOpenedHandler(deliveryService);
        org.mockito.Mockito.lenient()
                .when(outboxRepository.tryAcquireDeliveryLock(SESSION_ID))
                .thenReturn(true);
        org.mockito.Mockito.lenient()
                .when(familyAlertService.sendAlert(org.mockito.ArgumentMatchers.any()))
                .thenReturn(FamilyAlertDeliveryOutcome.DELIVERED);
    }

    @Test
    void openedEvent_deliveryFailure_shouldRemainPendingAndLaterRetryToDelivered() {
        Instant beforeDelivery = Instant.now();
        EmergencyNotificationOutbox outbox = pendingOutbox(beforeDelivery.minus(1, ChronoUnit.MINUTES));
        EmergencySession session = EmergencyTestFactory.makeActiveSession();
        session.setId(SESSION_ID);
        when(outboxRepository.findForUpdate(SESSION_ID)).thenReturn(Optional.of(outbox));
        when(emergencySessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
        when(familyAlertService.sendAlert(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new IllegalStateException("synthetic provider outage"))
                .thenReturn(FamilyAlertDeliveryOutcome.DELIVERED);

        assertThatCode(() -> handler.onEmergencySessionOpened(openedEvent()))
                .doesNotThrowAnyException();

        assertThat(outbox.getStatus()).isEqualTo(EmergencyNotificationOutbox.PENDING);
        assertThat(outbox.getAttemptCount()).isEqualTo(1);
        assertThat(outbox.getNextAttemptAt()).isAfter(beforeDelivery);
        assertThat(outbox.getLastErrorCode()).isEqualTo("IllegalStateException");
        assertThat(outbox.getDeliveredAt()).isNull();
        verify(familyAlertService).sendAlert(org.mockito.ArgumentMatchers.any());

        outbox.setNextAttemptAt(Instant.now().minusSeconds(1));
        handler.onEmergencySessionOpened(openedEvent());

        assertThat(outbox.getStatus()).isEqualTo(EmergencyNotificationOutbox.DELIVERED);
        assertThat(outbox.getAttemptCount()).isEqualTo(2);
        assertThat(outbox.getDeliveredAt()).isNotNull();
        assertThat(outbox.getLastErrorCode()).isNull();
        verify(familyAlertService, times(2)).sendAlert(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void openedEvent_success_shouldMarkOutboxDelivered() {
        EmergencyNotificationOutbox outbox = pendingOutbox(Instant.now().minusSeconds(1));
        EmergencySession session = EmergencyTestFactory.makeActiveSession();
        session.setId(SESSION_ID);
        when(outboxRepository.findForUpdate(SESSION_ID)).thenReturn(Optional.of(outbox));
        when(emergencySessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));

        handler.onEmergencySessionOpened(openedEvent());

        assertThat(outbox.getStatus()).isEqualTo(EmergencyNotificationOutbox.DELIVERED);
        assertThat(outbox.getAttemptCount()).isEqualTo(1);
        assertThat(outbox.getDeliveredAt()).isNotNull();
        assertThat(outbox.getLastErrorCode()).isNull();
        verify(familyAlertService).sendAlert(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void openedEvent_noRecipients_shouldSuppressOutboxWithoutMarkingDelivered() {
        EmergencyNotificationOutbox outbox = pendingOutbox(Instant.now().minusSeconds(1));
        EmergencySession session = EmergencyTestFactory.makeActiveSession();
        session.setId(SESSION_ID);
        when(outboxRepository.findForUpdate(SESSION_ID)).thenReturn(Optional.of(outbox));
        when(emergencySessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
        when(familyAlertService.sendAlert(org.mockito.ArgumentMatchers.any()))
                .thenReturn(FamilyAlertDeliveryOutcome.NO_RECIPIENTS);

        handler.onEmergencySessionOpened(openedEvent());

        assertThat(outbox.getStatus()).isEqualTo(EmergencyNotificationOutbox.SUPPRESSED);
        assertThat(outbox.getLastErrorCode()).isEqualTo("NO_RECIPIENTS");
        assertThat(outbox.getDeliveredAt()).isNull();
        assertThat(outbox.getTerminalAt()).isNotNull();
        assertThat(outbox.getClaimToken()).isNull();
    }

    @Test
    void staleWorker_shouldNotOverwriteNewerDeliveredState() {
        EmergencyNotificationOutbox outbox = pendingOutbox(Instant.now().minusSeconds(1));
        EmergencySession session = EmergencyTestFactory.makeActiveSession();
        session.setId(SESSION_ID);
        when(outboxRepository.findForUpdate(SESSION_ID)).thenReturn(Optional.of(outbox));
        when(emergencySessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));

        var firstClaim = writer.claim(SESSION_ID);
        outbox.setNextAttemptAt(Instant.now().minusSeconds(1));
        var secondClaim = writer.claim(SESSION_ID);

        assertThat(writer.markDelivered(SESSION_ID, secondClaim.claimToken())).isTrue();
        assertThat(writer.markRetry(
                SESSION_ID, firstClaim.claimToken(), "STALE_PROVIDER_FAILURE")).isFalse();
        assertThat(outbox.getStatus()).isEqualTo(EmergencyNotificationOutbox.DELIVERED);
        assertThat(outbox.getLastErrorCode()).isNull();
        assertThat(outbox.getDeliveredAt()).isNotNull();
    }

    @Test
    void staleWorker_shouldNotMarkDeliveredAfterNewerClaimWasRescheduled() {
        EmergencyNotificationOutbox outbox = pendingOutbox(Instant.now().minusSeconds(1));
        EmergencySession session = EmergencyTestFactory.makeActiveSession();
        session.setId(SESSION_ID);
        when(outboxRepository.findForUpdate(SESSION_ID)).thenReturn(Optional.of(outbox));
        when(emergencySessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));

        var firstClaim = writer.claim(SESSION_ID);
        outbox.setNextAttemptAt(Instant.now().minusSeconds(1));
        var secondClaim = writer.claim(SESSION_ID);

        assertThat(writer.markRetry(
                SESSION_ID, secondClaim.claimToken(), "PROVIDER_UNAVAILABLE")).isTrue();
        Instant scheduledRetry = outbox.getNextAttemptAt();
        assertThat(writer.markDelivered(SESSION_ID, firstClaim.claimToken())).isFalse();
        assertThat(outbox.getStatus()).isEqualTo(EmergencyNotificationOutbox.PENDING);
        assertThat(outbox.getNextAttemptAt()).isEqualTo(scheduledRetry);
        assertThat(outbox.getDeliveredAt()).isNull();
    }

    @Test
    void inactiveEmergency_shouldSuppressPendingDeliveryPermanently() {
        EmergencyNotificationOutbox outbox = pendingOutbox(Instant.now().minusSeconds(1));
        EmergencySession session = EmergencyTestFactory.makeActiveSession();
        session.setId(SESSION_ID);
        session.setStatus(EmergencyStatus.RESOLVED);
        when(outboxRepository.findForUpdate(SESSION_ID)).thenReturn(Optional.of(outbox));
        when(emergencySessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));

        handler.onEmergencySessionOpened(openedEvent());

        assertThat(outbox.getStatus()).isEqualTo(EmergencyNotificationOutbox.SUPPRESSED);
        assertThat(outbox.getLastErrorCode()).isEqualTo("EMERGENCY_NOT_ACTIVE");
        assertThat(outbox.getTerminalAt()).isNotNull();
        assertThat(outbox.getClaimToken()).isNull();
        verify(familyAlertService, never()).sendAlert(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void emergencyResolvedDuringFailedDelivery_shouldSuppressRetry() {
        EmergencyNotificationOutbox outbox = pendingOutbox(Instant.now().minusSeconds(1));
        EmergencySession session = EmergencyTestFactory.makeActiveSession();
        session.setId(SESSION_ID);
        when(outboxRepository.findForUpdate(SESSION_ID)).thenReturn(Optional.of(outbox));
        when(emergencySessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
        org.mockito.Mockito.doAnswer(invocation -> {
            session.setStatus(EmergencyStatus.RESOLVED);
            throw new IllegalStateException("synthetic provider outage");
        }).when(familyAlertService).sendAlert(org.mockito.ArgumentMatchers.any());

        handler.onEmergencySessionOpened(openedEvent());

        assertThat(outbox.getStatus()).isEqualTo(EmergencyNotificationOutbox.SUPPRESSED);
        assertThat(outbox.getLastErrorCode()).isEqualTo("EMERGENCY_NOT_ACTIVE");
        assertThat(outbox.getTerminalAt()).isNotNull();
    }

    @Test
    void claimStateFailure_afterCommit_shouldNotEscapeOrCallProvider() {
        when(outboxRepository.findForUpdate(SESSION_ID))
                .thenThrow(new IllegalStateException("synthetic database outage"));

        assertThatCode(() -> handler.onEmergencySessionOpened(openedEvent()))
                .doesNotThrowAnyException();

        verify(familyAlertService, never()).sendAlert(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void deliveryAlreadyInProgress_shouldNotClaimOrCallProvider() {
        when(outboxRepository.tryAcquireDeliveryLock(SESSION_ID)).thenReturn(false);

        assertThatCode(() -> handler.onEmergencySessionOpened(openedEvent()))
                .doesNotThrowAnyException();

        verify(outboxRepository, never()).findForUpdate(SESSION_ID);
        verify(familyAlertService, never()).sendAlert(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void deliveredStateWriteFailure_afterCommit_shouldNotEscape() {
        EmergencyNotificationOutbox outbox = pendingOutbox(Instant.now().minusSeconds(1));
        EmergencySession session = EmergencyTestFactory.makeActiveSession();
        session.setId(SESSION_ID);
        when(outboxRepository.findForUpdate(SESSION_ID))
                .thenReturn(Optional.of(outbox))
                .thenThrow(new IllegalStateException("synthetic database outage"));
        when(emergencySessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));

        assertThatCode(() -> handler.onEmergencySessionOpened(openedEvent()))
                .doesNotThrowAnyException();

        assertThat(outbox.getStatus()).isEqualTo(EmergencyNotificationOutbox.PENDING);
        assertThat(outbox.getClaimToken()).isNotNull();
        verify(familyAlertService).sendAlert(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void openedEvent_shouldRunOnlyAfterCommitWhileOutboxWritesUseNewTransactions()
            throws NoSuchMethodException {
        var handlerMethod = EmergencySessionOpenedHandler.class
                .getMethod("onEmergencySessionOpened", EmergencySessionOpened.class);
        TransactionalEventListener listener =
                handlerMethod.getAnnotation(TransactionalEventListener.class);

        assertThat(listener).isNotNull();
        assertThat(listener.phase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
        assertThat(listener.fallbackExecution()).isFalse();

        Transactional deliveryTransaction = EmergencyNotificationOutboxDeliveryService.class
                .getMethod("deliver", UUID.class)
                .getAnnotation(Transactional.class);
        assertThat(deliveryTransaction)
                .as("delivery transaction must hold the PostgreSQL advisory lock through provider I/O")
                .isNotNull();

        for (String methodName : new String[]{
                "claim", "markDelivered", "markSuppressed", "markRetry"}) {
            Class<?>[] parameterTypes = switch (methodName) {
                case "claim" -> new Class<?>[]{UUID.class};
                case "markDelivered" -> new Class<?>[]{UUID.class, UUID.class};
                default -> new Class<?>[]{UUID.class, UUID.class, String.class};
            };
            Transactional transactional = EmergencyNotificationOutboxWriter.class
                    .getMethod(methodName, parameterTypes)
                    .getAnnotation(Transactional.class);
            assertThat(transactional)
                    .as("%s must isolate the outbox state transition", methodName)
                    .isNotNull();
            assertThat(transactional.propagation()).isEqualTo(Propagation.REQUIRES_NEW);
        }
    }

    private EmergencyNotificationOutbox pendingOutbox(Instant nextAttemptAt) {
        return EmergencyNotificationOutbox.builder()
                .emergencySessionId(SESSION_ID)
                .status(EmergencyNotificationOutbox.PENDING)
                .attemptCount(0)
                .nextAttemptAt(nextAttemptAt)
                .createdAt(Instant.parse("2026-07-22T03:00:00Z"))
                .build();
    }

    private EmergencySessionOpened openedEvent() {
        return new EmergencySessionOpened(
                UUID.randomUUID(), SESSION_ID, UUID.randomUUID(),
                "AUTO_TRIAGE", null, null, Instant.now());
    }
}
