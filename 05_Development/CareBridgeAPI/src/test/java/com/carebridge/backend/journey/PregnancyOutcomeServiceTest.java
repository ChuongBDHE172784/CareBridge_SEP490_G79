package com.carebridge.backend.journey;

import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.journey.dto.RecordPregnancyOutcomeRequest;
import com.carebridge.backend.journey.entity.*;
import com.carebridge.backend.journey.policy.JourneyTransitionPolicy;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import com.carebridge.backend.journey.repository.MotherJourneyTransitionRepository;
import com.carebridge.backend.journey.repository.PregnancyOutcomeEvidenceRepository;
import com.carebridge.backend.journey.service.IJourneyOnboardingService;
import com.carebridge.backend.journey.service.impl.JourneyTransitionServiceImpl;
import com.carebridge.backend.security.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Optional;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PregnancyOutcomeServiceTest {

    private static final UUID OWNER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID JOURNEY_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID SUBMISSION_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final Instant NOW = Instant.parse("2026-07-19T08:00:00Z");

    @Mock MotherJourneyRepository journeyRepository;
    @Mock MotherJourneyTransitionRepository transitionRepository;
    @Mock PregnancyOutcomeEvidenceRepository outcomeRepository;
    @Mock UserRepository userRepository;
    @Mock AuditService auditService;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock IJourneyOnboardingService onboardingService;

    private JourneyTransitionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new JourneyTransitionServiceImpl(
                journeyRepository,
                transitionRepository,
                outcomeRepository,
                userRepository,
                auditService,
                new JourneyTransitionPolicy(),
                eventPublisher,
                onboardingService,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void liveBirthRecordsEvidenceAndTransitionsSameJourneyToPostpartum() {
        MotherJourney journey = activePregnancy();
        when(journeyRepository.findByIdForUpdate(JOURNEY_ID)).thenReturn(Optional.of(journey));
        when(outcomeRepository.findByJourneyIdAndSubmissionId(JOURNEY_ID, SUBMISSION_ID))
                .thenReturn(Optional.empty());
        when(outcomeRepository.findFirstByJourneyIdOrderByRevisionNumberDesc(JOURNEY_ID))
                .thenReturn(Optional.empty());
        when(journeyRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            MotherJourney saved = invocation.getArgument(0);
            saved.setVersion(4L);
            return saved;
        });
        when(outcomeRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            PregnancyOutcomeEvidence evidence = invocation.getArgument(0);
            evidence.setId(UUID.fromString("40000000-0000-0000-0000-000000000001"));
            return evidence;
        });
        when(transitionRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            MotherJourneyTransition transition = invocation.getArgument(0);
            transition.setId(UUID.fromString("50000000-0000-0000-0000-000000000001"));
            return transition;
        });

        var response = service.recordPregnancyOutcome(
                OWNER_ID,
                JOURNEY_ID,
                request(PregnancyOutcomeType.LIVE_BIRTH, LocalDate.of(2026, 7, 18)));

        assertThat(response.getOutcomeType()).isEqualTo(PregnancyOutcomeType.LIVE_BIRTH);
        assertThat(response.getJourneyType()).isEqualTo(JourneyType.POSTPARTUM);
        assertThat(response.isBabyActionsEligible()).isTrue();
        assertThat(journey.getJourneyType()).isEqualTo(JourneyType.POSTPARTUM);
        assertThat(journey.getDeliveryDate()).isEqualTo(LocalDate.of(2026, 7, 18));
        ArgumentCaptor<MotherJourneyTransition> transition =
                ArgumentCaptor.forClass(MotherJourneyTransition.class);
        verify(transitionRepository).saveAndFlush(transition.capture());
        assertThat(transition.getValue().getEventType())
                .isEqualTo(JourneyTransitionType.OUTCOME_RECORDED);
        assertThat(transition.getValue().getChanges())
                .containsKeys("pregnancyOutcome", "journeyType", "deliveryDate");
    }

    @Test
    void pregnancyLossTransitionsWithoutDeliveryDateOrBabySemantics() {
        MotherJourney journey = activePregnancy();
        when(journeyRepository.findByIdForUpdate(JOURNEY_ID)).thenReturn(Optional.of(journey));
        when(outcomeRepository.findByJourneyIdAndSubmissionId(JOURNEY_ID, SUBMISSION_ID))
                .thenReturn(Optional.empty());
        when(outcomeRepository.findFirstByJourneyIdOrderByRevisionNumberDesc(JOURNEY_ID))
                .thenReturn(Optional.empty());
        when(journeyRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            MotherJourney saved = invocation.getArgument(0);
            saved.setVersion(4L);
            return saved;
        });
        when(outcomeRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            PregnancyOutcomeEvidence evidence = invocation.getArgument(0);
            evidence.setId(UUID.randomUUID());
            return evidence;
        });
        when(transitionRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            MotherJourneyTransition transition = invocation.getArgument(0);
            transition.setId(UUID.randomUUID());
            return transition;
        });

        var response = service.recordPregnancyOutcome(
                OWNER_ID,
                JOURNEY_ID,
                request(PregnancyOutcomeType.PREGNANCY_LOSS, null));

        assertThat(response.getJourneyType()).isEqualTo(JourneyType.POSTPARTUM);
        assertThat(response.isBabyActionsEligible()).isFalse();
        assertThat(journey.getDeliveryDate()).isNull();
    }

    @Test
    void ongoingKeepsPregnancyAndDoesNotWriteDeliveryDate() {
        MotherJourney journey = activePregnancy();
        when(journeyRepository.findByIdForUpdate(JOURNEY_ID)).thenReturn(Optional.of(journey));
        when(outcomeRepository.findByJourneyIdAndSubmissionId(JOURNEY_ID, SUBMISSION_ID))
                .thenReturn(Optional.empty());
        when(outcomeRepository.findFirstByJourneyIdOrderByRevisionNumberDesc(JOURNEY_ID))
                .thenReturn(Optional.empty());
        when(journeyRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            MotherJourney saved = invocation.getArgument(0);
            saved.setVersion(4L);
            return saved;
        });
        when(outcomeRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            PregnancyOutcomeEvidence evidence = invocation.getArgument(0);
            evidence.setId(UUID.randomUUID());
            return evidence;
        });
        when(transitionRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            MotherJourneyTransition transition = invocation.getArgument(0);
            transition.setId(UUID.randomUUID());
            return transition;
        });

        var response = service.recordPregnancyOutcome(
                OWNER_ID, JOURNEY_ID, request(PregnancyOutcomeType.ONGOING, null));

        assertThat(response.getJourneyType()).isEqualTo(JourneyType.PREGNANCY);
        assertThat(journey.getDeliveryDate()).isNull();
        verify(journeyRepository).saveAndFlush(any());
    }

    @Test
    void liveBirthRequiresOutcomeDateAndHasNoSideEffects() {
        when(journeyRepository.findByIdForUpdate(JOURNEY_ID))
                .thenReturn(Optional.of(activePregnancy()));
        when(outcomeRepository.findByJourneyIdAndSubmissionId(JOURNEY_ID, SUBMISSION_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.recordPregnancyOutcome(
                OWNER_ID, JOURNEY_ID, request(PregnancyOutcomeType.LIVE_BIRTH, null)))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getCode())
                                .isEqualTo("OUTCOME_DATE_REQUIRED"));

        verify(outcomeRepository, never()).saveAndFlush(any());
        verify(transitionRepository, never()).saveAndFlush(any());
        verify(journeyRepository, never()).saveAndFlush(any());
    }

    @Test
    void identicalSubmissionReturnsPriorResultButChangedPayloadConflicts() {
        PregnancyOutcomeEvidence prior = PregnancyOutcomeEvidence.builder()
                .id(UUID.randomUUID())
                .journeyId(JOURNEY_ID)
                .ownerUserId(OWNER_ID)
                .submissionId(SUBMISSION_ID)
                .outcomeType(PregnancyOutcomeType.ONGOING)
                .source(JourneyDateSource.SELF_REPORTED)
                .reason("Outcome confirmed")
                .effectiveAt(NOW)
                .recordedAt(NOW)
                .revisionNumber(1)
                .journeyVersion(3L)
                .semanticHash(semanticHash(PregnancyOutcomeType.ONGOING, null, false))
                .build();
        when(journeyRepository.findByIdForUpdate(JOURNEY_ID))
                .thenReturn(Optional.of(activePregnancy()));
        when(outcomeRepository.findByJourneyIdAndSubmissionId(JOURNEY_ID, SUBMISSION_ID))
                .thenReturn(Optional.of(prior));

        var replay = service.recordPregnancyOutcome(
                OWNER_ID, JOURNEY_ID, request(PregnancyOutcomeType.ONGOING, null));
        assertThat(replay.getEvidenceId()).isEqualTo(prior.getId());
        verify(outcomeRepository, never()).saveAndFlush(any());

        assertThatThrownBy(() -> service.recordPregnancyOutcome(
                OWNER_ID,
                JOURNEY_ID,
                request(PregnancyOutcomeType.PREGNANCY_LOSS, null)))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getCode())
                                .isEqualTo("OUTCOME_SUBMISSION_CONFLICT"));
    }

    @Test
    void staleVersionFailsBeforeAnyMutation() {
        MotherJourney journey = activePregnancy();
        journey.setVersion(4L);
        when(journeyRepository.findByIdForUpdate(JOURNEY_ID)).thenReturn(Optional.of(journey));
        when(outcomeRepository.findByJourneyIdAndSubmissionId(JOURNEY_ID, SUBMISSION_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.recordPregnancyOutcome(
                OWNER_ID, JOURNEY_ID, request(PregnancyOutcomeType.UNKNOWN, null)))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getCode())
                                .isEqualTo("JOURNEY_VERSION_CONFLICT"));

        verify(journeyRepository, never()).saveAndFlush(any());
        verify(outcomeRepository, never()).saveAndFlush(any());
        verify(transitionRepository, never()).saveAndFlush(any());
        verifyNoInteractions(auditService, eventPublisher);
    }

    @Test
    void correctingLiveBirthToLossClearsDeliveryDate() {
        MotherJourney journey = activePregnancy();
        journey.setJourneyType(JourneyType.POSTPARTUM);
        journey.setPregnancyOutcome(PregnancyOutcomeType.LIVE_BIRTH);
        journey.setPregnancyOutcomeDate(LocalDate.of(2026, 7, 18));
        journey.setDeliveryDate(LocalDate.of(2026, 7, 18));
        PregnancyOutcomeEvidence prior = PregnancyOutcomeEvidence.builder()
                .id(UUID.randomUUID())
                .journeyId(JOURNEY_ID)
                .ownerUserId(OWNER_ID)
                .submissionId(UUID.randomUUID())
                .outcomeType(PregnancyOutcomeType.LIVE_BIRTH)
                .outcomeDate(LocalDate.of(2026, 7, 18))
                .source(JourneyDateSource.SELF_REPORTED)
                .actorUserId(OWNER_ID)
                .reason("Outcome confirmed")
                .effectiveAt(NOW)
                .revisionNumber(1)
                .journeyVersion(3L)
                .semanticHash("prior")
                .build();
        var correction = request(PregnancyOutcomeType.PREGNANCY_LOSS, null);
        correction.setCorrection(true);
        when(journeyRepository.findByIdForUpdate(JOURNEY_ID)).thenReturn(Optional.of(journey));
        when(outcomeRepository.findByJourneyIdAndSubmissionId(JOURNEY_ID, SUBMISSION_ID))
                .thenReturn(Optional.empty());
        when(outcomeRepository.findFirstByJourneyIdOrderByRevisionNumberDesc(JOURNEY_ID))
                .thenReturn(Optional.of(prior));
        when(journeyRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(outcomeRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            PregnancyOutcomeEvidence evidence = invocation.getArgument(0);
            evidence.setId(UUID.randomUUID());
            return evidence;
        });
        when(transitionRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            MotherJourneyTransition transition = invocation.getArgument(0);
            transition.setId(UUID.randomUUID());
            return transition;
        });

        service.recordPregnancyOutcome(OWNER_ID, JOURNEY_ID, correction);

        assertThat(journey.getPregnancyOutcome()).isEqualTo(PregnancyOutcomeType.PREGNANCY_LOSS);
        assertThat(journey.getDeliveryDate()).isNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void auditContainsOnlyRedactedOutcomeMetadata() {
        MotherJourney journey = activePregnancy();
        when(journeyRepository.findByIdForUpdate(JOURNEY_ID)).thenReturn(Optional.of(journey));
        when(outcomeRepository.findByJourneyIdAndSubmissionId(JOURNEY_ID, SUBMISSION_ID))
                .thenReturn(Optional.empty());
        when(outcomeRepository.findFirstByJourneyIdOrderByRevisionNumberDesc(JOURNEY_ID))
                .thenReturn(Optional.empty());
        when(journeyRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            MotherJourney saved = invocation.getArgument(0);
            saved.setVersion(4L);
            return saved;
        });
        when(outcomeRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            PregnancyOutcomeEvidence evidence = invocation.getArgument(0);
            evidence.setId(UUID.randomUUID());
            return evidence;
        });
        when(transitionRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            MotherJourneyTransition transition = invocation.getArgument(0);
            transition.setId(UUID.randomUUID());
            return transition;
        });

        service.recordPregnancyOutcome(
                OWNER_ID, JOURNEY_ID, request(PregnancyOutcomeType.STILLBIRTH, null));

        ArgumentCaptor<Map<String, Object>> details = ArgumentCaptor.forClass(Map.class);
        verify(auditService).log(
                eq(AuditAction.PREGNANCY_OUTCOME_RECORDED),
                eq(OWNER_ID),
                eq("MotherJourney"),
                eq(JOURNEY_ID.toString()),
                details.capture());
        assertThat(details.getValue())
                .containsOnlyKeys("outcomeType", "revisionNumber", "journeyVersion")
                .doesNotContainKeys("reason", "outcomeDate", "submissionId");
    }

    private MotherJourney activePregnancy() {
        return MotherJourney.builder()
                .id(JOURNEY_ID)
                .ownerUserId(OWNER_ID)
                .journeyType(JourneyType.PREGNANCY)
                .status(JourneyStatus.ACTIVE)
                .version(3L)
                .build();
    }

    private RecordPregnancyOutcomeRequest request(
            PregnancyOutcomeType outcomeType, LocalDate outcomeDate) {
        var request = new RecordPregnancyOutcomeRequest();
        request.setSubmissionId(SUBMISSION_ID);
        request.setExpectedJourneyVersion(3L);
        request.setOutcomeType(outcomeType);
        request.setOutcomeDate(outcomeDate);
        request.setSource(JourneyDateSource.SELF_REPORTED);
        request.setReason("Outcome confirmed");
        request.setEffectiveAt(NOW);
        return request;
    }

    private String semanticHash(
            PregnancyOutcomeType outcomeType, LocalDate outcomeDate, boolean correction) {
        String canonical = String.join(
                "|",
                outcomeType.name(),
                outcomeDate == null ? "" : outcomeDate.toString(),
                JourneyDateSource.SELF_REPORTED.name(),
                "Outcome confirmed",
                NOW.toString(),
                Boolean.toString(correction));
        try {
            return java.util.HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }
}
