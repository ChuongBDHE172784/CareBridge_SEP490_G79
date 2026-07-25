package com.carebridge.backend.triage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.triage.entity.IntakeSession;
import com.carebridge.backend.triage.exception.TriageException;
import com.carebridge.backend.triage.repository.IIntakeSessionRepository;
import com.carebridge.backend.triage.service.LifecycleIntakeBindingService;
import com.carebridge.backend.triage.service.LifecycleSafetyMetrics;
import com.carebridge.backend.triage.service.TriageContinuationService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class TriageContinuationServiceTest {
    private IIntakeSessionRepository repository;
    private LifecycleIntakeBindingService bindingService;
    private TriageContinuationService service;
    private UUID ownerId;
    private UUID token;

    @BeforeEach
    void setUp() {
        repository = mock(IIntakeSessionRepository.class);
        bindingService = mock(LifecycleIntakeBindingService.class);
        service = new TriageContinuationService(repository, bindingService);
        ownerId = UUID.randomUUID();
        token = UUID.randomUUID();
    }

    @Test
    void malformedTokens_areNeutral404AndNeverReachPersistence() {
        assertNeutral404(() -> service.resolve(ownerId, "not-a-token"));
        assertNeutral404(() -> service.acknowledge(ownerId, null));
        verify(repository, never()).findByUserIdAndContinuationToken(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(repository, never()).findForUpdateByUserIdAndContinuationToken(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void ownedButIneligibleOrigin_isNeutral409() {
        IntakeSession session = activeSession();
        when(repository.findByUserIdAndContinuationToken(ownerId, token))
                .thenReturn(Optional.of(session));
        doThrow(new BusinessException(HttpStatus.CONFLICT, "LIFECYCLE_CONSENT_INVALID", "invalid"))
                .when(bindingService).revalidate(session);

        assertThatThrownBy(() -> service.resolve(ownerId, token.toString()))
                .isInstanceOfSatisfying(TriageException.class, error -> {
                    org.assertj.core.api.Assertions.assertThat(error.getHttpStatus())
                            .isEqualTo(HttpStatus.CONFLICT);
                    org.assertj.core.api.Assertions.assertThat(error.getCode())
                            .isEqualTo("TRIAGE-015");
                });
    }

    @Test
    void infrastructureFailure_isNotMisreportedAsInvalidConsent() {
        IntakeSession session = activeSession();
        when(repository.findByUserIdAndContinuationToken(ownerId, token))
                .thenReturn(Optional.of(session));
        doThrow(new IllegalStateException("database unavailable"))
                .when(bindingService).revalidate(session);

        assertThatThrownBy(() -> service.resolve(ownerId, token.toString()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("database unavailable");
    }

    @Test
    void sameOwnerAcknowledgeReplay_isHarmlessEvenAfterExpiry() {
        IntakeSession session = activeSession();
        session.setContinuationAcknowledgedAt(Instant.now().minusSeconds(10));
        session.setContinuationExpiresAt(Instant.now().minusSeconds(1));
        when(repository.findForUpdateByUserIdAndContinuationToken(ownerId, token))
                .thenReturn(Optional.of(session));

        service.acknowledge(ownerId, token.toString());

        verify(repository, never()).save(session);
        verify(bindingService, never()).revalidate(session);
    }

    @Test
    void resolve_preterminalOrMissingRisk_isNeutral404WithoutOriginDisclosure() {
        for (IntakeStatus status : new IntakeStatus[] {
                IntakeStatus.PENDING, IntakeStatus.PROCESSING,
                IntakeStatus.NEED_MORE_INFO, IntakeStatus.FAILED}) {
            IntakeSession session = activeSession();
            session.setStatus(status);
            when(repository.findByUserIdAndContinuationToken(ownerId, token))
                    .thenReturn(Optional.of(session));

            assertNeutral404(() -> service.resolve(ownerId, token.toString()));
            verify(bindingService, never()).revalidate(session);
        }

        IntakeSession missingRisk = activeSession();
        missingRisk.setRiskLevel(null);
        when(repository.findByUserIdAndContinuationToken(ownerId, token))
                .thenReturn(Optional.of(missingRisk));

        assertNeutral404(() -> service.resolve(ownerId, token.toString()));
        verify(bindingService, never()).revalidate(missingRisk);
    }

    @Test
    void acknowledge_preterminalOrMissingRisk_isNeutral404AndDoesNotPersist() {
        IntakeSession preterminal = activeSession();
        preterminal.setStatus(IntakeStatus.NEED_MORE_INFO);
        when(repository.findForUpdateByUserIdAndContinuationToken(ownerId, token))
                .thenReturn(Optional.of(preterminal));

        assertNeutral404(() -> service.acknowledge(ownerId, token.toString()));
        verify(repository, never()).save(preterminal);
        verify(bindingService, never()).revalidate(preterminal);

        IntakeSession missingRisk = activeSession();
        missingRisk.setRiskLevel(null);
        when(repository.findForUpdateByUserIdAndContinuationToken(ownerId, token))
                .thenReturn(Optional.of(missingRisk));

        assertNeutral404(() -> service.acknowledge(ownerId, token.toString()));
        verify(repository, never()).save(missingRisk);
        verify(bindingService, never()).revalidate(missingRisk);
    }

    @Test
    void firstSuccessfulAcknowledge_recordsRecoveredMetricOnce() {
        Instant now = Instant.parse("2026-07-23T00:00:00Z");
        LifecycleSafetyMetrics metrics = new LifecycleSafetyMetrics();
        service = new TriageContinuationService(
                repository, bindingService, metrics, Clock.fixed(now, ZoneOffset.UTC));
        IntakeSession session = activeSession();
        session.setContinuationExpiresAt(now.plusSeconds(600));
        when(repository.findForUpdateByUserIdAndContinuationToken(ownerId, token))
                .thenReturn(Optional.of(session));

        service.acknowledge(ownerId, token.toString());

        assertThat(metrics.count(
                LifecycleSafetyMetrics.Boundary.CONTINUATION,
                LifecycleSafetyMetrics.Outcome.ACKNOWLEDGED)).isEqualTo(1);
        assertThat(session.getContinuationAcknowledgedAt()).isEqualTo(now);
        verify(repository).save(session);
    }

    private IntakeSession activeSession() {
        return IntakeSession.builder()
                .id(UUID.randomUUID())
                .userId(ownerId)
                .status(IntakeStatus.COMPLETED)
                .stage(TriageStage.POSTPARTUM)
                .riskLevel(RiskLevel.GREEN)
                .originDashboard(OriginDashboard.MOTHER_JOURNEY)
                .journeyId(UUID.randomUUID())
                .originReferenceId(UUID.randomUUID())
                .continuationToken(token)
                .continuationExpiresAt(Instant.now().plusSeconds(600))
                .build();
    }

    private void assertNeutral404(org.assertj.core.api.ThrowableAssert.ThrowingCallable call) {
        assertThatThrownBy(call)
                .isInstanceOfSatisfying(TriageException.class, error -> {
                    org.assertj.core.api.Assertions.assertThat(error.getHttpStatus())
                            .isEqualTo(HttpStatus.NOT_FOUND);
                    org.assertj.core.api.Assertions.assertThat(error.getCode())
                            .isEqualTo("TRIAGE-014");
                });
    }
}
