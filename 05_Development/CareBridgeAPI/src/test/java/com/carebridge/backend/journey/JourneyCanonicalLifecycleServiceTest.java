package com.carebridge.backend.journey;

import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.journey.entity.*;
import com.carebridge.backend.journey.policy.JourneyTransitionPolicy;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import com.carebridge.backend.journey.repository.MotherJourneyTransitionRepository;
import com.carebridge.backend.journey.service.impl.JourneyTransitionServiceImpl;
import com.carebridge.backend.journey.service.IJourneyOnboardingService;
import com.carebridge.backend.security.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JourneyCanonicalLifecycleServiceTest {

    @Mock MotherJourneyRepository journeyRepository;
    @Mock MotherJourneyTransitionRepository transitionRepository;
    @Mock UserRepository userRepository;
    @Mock AuditService auditService;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock IJourneyOnboardingService onboardingService;

    private JourneyTransitionPolicy policy;
    private JourneyTransitionServiceImpl service;

    @BeforeEach
    void setUp() {
        policy = new JourneyTransitionPolicy();
        Clock clock = Clock.fixed(JourneyLifecycleTestFactory.NOW, ZoneOffset.UTC);
        service = new JourneyTransitionServiceImpl(
                journeyRepository,
                transitionRepository,
                userRepository,
                auditService,
                policy,
                eventPublisher,
                onboardingService,
                clock);
    }

    @Test
    void jrnTc001_createFirstCanonicalLifecyclePersistsCurrentAndHistory() {
        when(userRepository.findById(JourneyLifecycleTestFactory.MOTHER_ID))
                .thenReturn(Optional.of(JourneyLifecycleTestFactory.mother()));
        when(journeyRepository.existsByOwnerUserIdAndStatusAndJourneyTypeIn(
                any(), any(), any())).thenReturn(false);
        when(journeyRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            MotherJourney journey = invocation.getArgument(0);
            journey.setId(JourneyLifecycleTestFactory.JOURNEY_ID);
            return journey;
        });
        when(transitionRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.createJourney(
                JourneyLifecycleTestFactory.pregnancyCreate(),
                JourneyLifecycleTestFactory.MOTHER_ID);

        assertThat(response.getStatus()).isEqualTo(JourneyStatus.ACTIVE.name());
        assertThat(response.getVersion()).isZero();
        ArgumentCaptor<MotherJourneyTransition> history =
                ArgumentCaptor.forClass(MotherJourneyTransition.class);
        verify(transitionRepository).saveAndFlush(history.capture());
        assertThat(history.getValue().getEventType()).isEqualTo(JourneyTransitionType.CREATED);
        assertThat(history.getValue().getActorUserId())
                .isEqualTo(JourneyLifecycleTestFactory.MOTHER_ID);
    }

    @Test
    void jrnTc002_existingCanonicalLifecycleIsRejected() {
        when(userRepository.findById(JourneyLifecycleTestFactory.MOTHER_ID))
                .thenReturn(Optional.of(JourneyLifecycleTestFactory.mother()));
        when(journeyRepository.existsByOwnerUserIdAndStatusAndJourneyTypeIn(
                any(), any(), any())).thenReturn(true);

        assertJourneyError("JOURNEY-015", HttpStatus.CONFLICT,
                () -> service.createJourney(
                        JourneyLifecycleTestFactory.pregnancyCreate(),
                        JourneyLifecycleTestFactory.MOTHER_ID));
        verify(journeyRepository, never()).save(any());
        verify(transitionRepository, never()).saveAndFlush(any());
    }

    @Test
    void postpartumCreate_acceptsSelfReportedNonFutureRecoveryStart() {
        when(userRepository.findById(JourneyLifecycleTestFactory.MOTHER_ID))
                .thenReturn(Optional.of(JourneyLifecycleTestFactory.mother()));
        when(journeyRepository.existsByOwnerUserIdAndStatusAndJourneyTypeIn(
                any(), any(), any())).thenReturn(false);
        when(journeyRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            MotherJourney journey = invocation.getArgument(0);
            journey.setId(JourneyLifecycleTestFactory.JOURNEY_ID);
            return journey;
        });
        when(transitionRepository.saveAndFlush(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.createJourney(
                JourneyLifecycleTestFactory.postpartumCreate(),
                JourneyLifecycleTestFactory.MOTHER_ID);

        assertThat(response.getJourneyType()).isEqualTo(JourneyType.POSTPARTUM.name());
        ArgumentCaptor<MotherJourney> journey = ArgumentCaptor.forClass(MotherJourney.class);
        verify(journeyRepository).saveAndFlush(journey.capture());
        assertThat(journey.getValue().getDateSource())
                .isEqualTo(JourneyDateSource.SELF_REPORTED);
        assertThat(journey.getValue().getDateConfidence())
                .isEqualTo(JourneyDateConfidence.CONFIRMED);
        assertThat(journey.getValue().getDeliveryDate()).isNull();
        assertThat(journey.getValue().getPregnancyOutcome()).isNull();
    }

    @Test
    void postpartumCreate_rejectsFutureRecoveryStartWithoutPersistence() {
        when(userRepository.findById(JourneyLifecycleTestFactory.MOTHER_ID))
                .thenReturn(Optional.of(JourneyLifecycleTestFactory.mother()));
        var request = JourneyLifecycleTestFactory.postpartumCreate();
        request.setStartDate(java.time.LocalDate.of(2026, 7, 19));

        assertJourneyError("POSTPARTUM_START_DATE_FUTURE", HttpStatus.BAD_REQUEST,
                () -> service.createJourney(request, JourneyLifecycleTestFactory.MOTHER_ID));

        verify(journeyRepository, never()).saveAndFlush(any());
        verifyNoInteractions(transitionRepository, auditService, eventPublisher);
    }

    @Test
    void postpartumCreate_acceptsLocalTodayAcrossUtcDateBoundary() {
        Clock boundaryClock = Clock.fixed(
                Instant.parse("2026-07-18T18:30:00Z"),
                ZoneOffset.UTC);
        var boundaryService = new JourneyTransitionServiceImpl(
                journeyRepository,
                transitionRepository,
                userRepository,
                auditService,
                policy,
                eventPublisher,
                onboardingService,
                boundaryClock);
        var request = JourneyLifecycleTestFactory.postpartumCreate();
        request.setStartDate(java.time.LocalDate.of(2026, 7, 19));
        when(userRepository.findById(JourneyLifecycleTestFactory.MOTHER_ID))
                .thenReturn(Optional.of(JourneyLifecycleTestFactory.mother()));
        when(journeyRepository.existsByOwnerUserIdAndStatusAndJourneyTypeIn(
                any(), any(), any())).thenReturn(false);
        when(journeyRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            MotherJourney journey = invocation.getArgument(0);
            journey.setId(JourneyLifecycleTestFactory.JOURNEY_ID);
            return journey;
        });
        when(transitionRepository.saveAndFlush(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = boundaryService.createJourney(
                request, JourneyLifecycleTestFactory.MOTHER_ID);

        assertThat(response.getStartDate()).isEqualTo("2026-07-19");
    }

    @Test
    void postpartumCreate_rejectsMissingOrUnsupportedProvenance() {
        when(userRepository.findById(JourneyLifecycleTestFactory.MOTHER_ID))
                .thenReturn(Optional.of(JourneyLifecycleTestFactory.mother()));
        var missingSource = JourneyLifecycleTestFactory.postpartumCreate();
        missingSource.setDateSource(null);
        var unknownConfidence = JourneyLifecycleTestFactory.postpartumCreate();
        unknownConfidence.setDateConfidence(JourneyDateConfidence.UNKNOWN);

        assertJourneyError("POSTPARTUM_PROVENANCE_INVALID", HttpStatus.BAD_REQUEST,
                () -> service.createJourney(missingSource, JourneyLifecycleTestFactory.MOTHER_ID));
        assertJourneyError("POSTPARTUM_PROVENANCE_INVALID", HttpStatus.BAD_REQUEST,
                () -> service.createJourney(unknownConfidence, JourneyLifecycleTestFactory.MOTHER_ID));

        verify(journeyRepository, never()).saveAndFlush(any());
        verifyNoInteractions(transitionRepository, auditService, eventPublisher);
    }

    @Test
    void jrnTc003_dateCorrectionRecordsPreviousAndNewValues() {
        MotherJourney current = JourneyLifecycleTestFactory.activePregnancy();
        when(journeyRepository.findById(current.getId())).thenReturn(Optional.of(current));
        when(journeyRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            MotherJourney saved = invocation.getArgument(0);
            saved.setVersion(1L);
            return saved;
        });
        when(transitionRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.updateJourney(
                JourneyLifecycleTestFactory.MOTHER_ID,
                current.getId(),
                JourneyLifecycleTestFactory.dateCorrection());

        assertThat(response.getVersion()).isEqualTo(1L);
        ArgumentCaptor<MotherJourneyTransition> history =
                ArgumentCaptor.forClass(MotherJourneyTransition.class);
        verify(transitionRepository).saveAndFlush(history.capture());
        assertThat(history.getValue().getChanges())
                .containsKey("lastMenstrualDate")
                .doesNotContainKeys("notes", "ownerUserId");
        assertThat(history.getValue().getChanges().get("lastMenstrualDate"))
                .isEqualTo(java.util.Map.of(
                        "previous", "2026-06-01",
                        "new", "2026-06-02"));
    }

    @Test
    void jrnTc004_invalidOrTerminalTransitionHasNoSideEffects() {
        MotherJourney completed = JourneyLifecycleTestFactory.completedJourney();
        when(journeyRepository.findById(completed.getId())).thenReturn(Optional.of(completed));

        assertJourneyError("JOURNEY-012", HttpStatus.BAD_REQUEST,
                () -> service.updateJourney(
                        JourneyLifecycleTestFactory.MOTHER_ID,
                        completed.getId(),
                        JourneyLifecycleTestFactory.invalidPostpartumTransition()));
        verify(journeyRepository, never()).save(any());
        verify(transitionRepository, never()).saveAndFlush(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void jrnTc005_historyFailureStopsAuditAndEvent() {
        MotherJourney current = JourneyLifecycleTestFactory.activePregnancy();
        when(journeyRepository.findById(current.getId())).thenReturn(Optional.of(current));
        when(journeyRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(transitionRepository.saveAndFlush(any()))
                .thenThrow(new IllegalStateException("synthetic history failure"));

        assertThatThrownBy(() -> service.updateJourney(
                JourneyLifecycleTestFactory.MOTHER_ID,
                current.getId(),
                JourneyLifecycleTestFactory.dateCorrection()))
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(auditService, eventPublisher);
    }

    @Test
    void jrnTc006_dateChangeWithoutProvenanceIsRejected() {
        MotherJourney current = JourneyLifecycleTestFactory.activePregnancy();
        when(journeyRepository.findById(current.getId())).thenReturn(Optional.of(current));

        assertJourneyError("JOURNEY-018", HttpStatus.BAD_REQUEST,
                () -> service.updateJourney(
                        JourneyLifecycleTestFactory.MOTHER_ID,
                        current.getId(),
                        JourneyLifecycleTestFactory.dateCorrectionWithoutProvenance()));
        verify(transitionRepository, never()).saveAndFlush(any());
    }

    @Test
    void jrnTc007_babyCareIsReadableButNotCanonical() {
        assertThat(policy.isCanonical(JourneyType.BABY_CARE)).isFalse();
        assertThat(policy.isCanonical(JourneyType.PREGNANCY)).isTrue();
    }

    @Test
    void createJourney_unassignedRolePromotesMotherBeforePersisting() {
        var unassigned = com.carebridge.backend.security.entity.User.builder()
                .id(JourneyLifecycleTestFactory.MOTHER_ID)
                .build();
        when(userRepository.findById(JourneyLifecycleTestFactory.MOTHER_ID))
                .thenReturn(Optional.of(unassigned));
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(journeyRepository.existsByOwnerUserIdAndStatusAndJourneyTypeIn(
                any(), any(), any())).thenReturn(false);
        when(journeyRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            MotherJourney journey = invocation.getArgument(0);
            journey.setId(JourneyLifecycleTestFactory.JOURNEY_ID);
            return journey;
        });
        when(transitionRepository.saveAndFlush(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.createJourney(
                JourneyLifecycleTestFactory.pregnancyCreate(),
                JourneyLifecycleTestFactory.MOTHER_ID);

        verify(userRepository).save(argThat(user -> user.getRole()
                == com.carebridge.backend.security.rbac.Role.MOTHER));
    }

    @Test
    void updateJourney_invalidLifecycleConsentFailsBeforeJourneyLookupOrMutation() {
        doThrow(new BusinessException(
                HttpStatus.CONFLICT,
                "LIFECYCLE_CONSENT_INVALID",
                "Your lifecycle consent needs to be reviewed"))
                .when(onboardingService)
                .ensureEligible(JourneyLifecycleTestFactory.MOTHER_ID);

        assertJourneyError("LIFECYCLE_CONSENT_INVALID", HttpStatus.CONFLICT,
                () -> service.updateJourney(
                        JourneyLifecycleTestFactory.MOTHER_ID,
                        JourneyLifecycleTestFactory.JOURNEY_ID,
                        JourneyLifecycleTestFactory.dateCorrection()));

        verifyNoInteractions(journeyRepository, transitionRepository, auditService, eventPublisher);
    }

    @Test
    void updateJourney_semanticNoOpIsRejectedBeforePersistence() {
        MotherJourney current = JourneyLifecycleTestFactory.activePregnancy();
        when(journeyRepository.findById(current.getId())).thenReturn(Optional.of(current));

        assertJourneyError("JOURNEY-020", HttpStatus.BAD_REQUEST,
                () -> service.updateJourney(
                        JourneyLifecycleTestFactory.MOTHER_ID,
                        current.getId(),
                        new com.carebridge.backend.journey.dto.UpdateJourneyRequest()));

        verify(journeyRepository, never()).saveAndFlush(any());
        verifyNoInteractions(transitionRepository, auditService, eventPublisher);
    }

    @Test
    void updateJourney_notesOnlyUsesDetailsChangedWithoutPersistingNotesInHistory() {
        MotherJourney current = JourneyLifecycleTestFactory.activePregnancy();
        when(journeyRepository.findById(current.getId())).thenReturn(Optional.of(current));
        when(journeyRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            MotherJourney saved = invocation.getArgument(0);
            saved.setVersion(1L);
            return saved;
        });
        when(transitionRepository.saveAndFlush(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        var request = new com.carebridge.backend.journey.dto.UpdateJourneyRequest();
        request.setNotes("Updated private note");
        request.setChangeReason("NOTE_CORRECTION");

        service.updateJourney(JourneyLifecycleTestFactory.MOTHER_ID, current.getId(), request);

        ArgumentCaptor<MotherJourneyTransition> history =
                ArgumentCaptor.forClass(MotherJourneyTransition.class);
        verify(transitionRepository).saveAndFlush(history.capture());
        assertThat(history.getValue().getEventType())
                .isEqualTo(JourneyTransitionType.DETAILS_CHANGED);
        assertThat(history.getValue().getChanges()).isEmpty();
    }

    @Test
    void updateJourney_unknownStatusIsRejected() {
        MotherJourney current = JourneyLifecycleTestFactory.activePregnancy();
        when(journeyRepository.findById(current.getId())).thenReturn(Optional.of(current));
        var request = new com.carebridge.backend.journey.dto.UpdateJourneyRequest();
        request.setStatus("garbage");

        assertJourneyError("JOURNEY-021", HttpStatus.BAD_REQUEST,
                () -> service.updateJourney(
                        JourneyLifecycleTestFactory.MOTHER_ID, current.getId(), request));
        verify(journeyRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateJourney_effectiveAtMoreThanFiveMinutesAheadIsRejected() {
        MotherJourney current = JourneyLifecycleTestFactory.activePregnancy();
        when(journeyRepository.findById(current.getId())).thenReturn(Optional.of(current));
        var request = JourneyLifecycleTestFactory.dateCorrection();
        request.setEffectiveAt(JourneyLifecycleTestFactory.NOW.plusSeconds(301));

        assertJourneyError("JOURNEY-019", HttpStatus.BAD_REQUEST,
                () -> service.updateJourney(
                        JourneyLifecycleTestFactory.MOTHER_ID, current.getId(), request));
        verify(journeyRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateJourney_preservesExplicitAdjustedDueDateWhenLmpIsProvided() {
        MotherJourney current = JourneyLifecycleTestFactory.activePregnancy();
        when(journeyRepository.findById(current.getId())).thenReturn(Optional.of(current));
        when(journeyRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            MotherJourney saved = invocation.getArgument(0);
            saved.setVersion(1L);
            return saved;
        });
        when(transitionRepository.saveAndFlush(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        var request = JourneyLifecycleTestFactory.dateCorrection();
        request.setEstimatedDueDate(java.time.LocalDate.of(2027, 3, 10));

        var response = service.updateJourney(
                JourneyLifecycleTestFactory.MOTHER_ID, current.getId(), request);

        assertThat(response.getEstimatedDueDate())
                .isEqualTo(java.time.LocalDate.of(2027, 3, 10));
    }

    @Test
    void getHistory_returnsRequestedPageMetadata() {
        MotherJourney current = JourneyLifecycleTestFactory.activePregnancy();
        when(journeyRepository.findById(current.getId())).thenReturn(Optional.of(current));
        var transition = MotherJourneyTransition.builder()
                .eventType(JourneyTransitionType.CREATED)
                .journeyId(current.getId())
                .source(JourneyDateSource.SELF_REPORTED)
                .effectiveAt(JourneyLifecycleTestFactory.NOW)
                .journeyVersion(0L)
                .build();
        var pageable = PageRequest.of(1, 5);
        when(transitionRepository.findByJourneyIdOrderByRecordedAtDesc(
                current.getId(), pageable))
                .thenReturn(new PageImpl<>(List.of(transition, transition), pageable, 7));

        var response = service.getHistory(
                JourneyLifecycleTestFactory.MOTHER_ID, current.getId(), pageable);

        assertThat(response.getItems()).hasSize(2);
        assertThat(response.getPage()).isEqualTo(1);
        assertThat(response.getSize()).isEqualTo(5);
        assertThat(response.getTotalElements()).isEqualTo(7);
    }

    private void assertJourneyError(
            String code, HttpStatus status, ThrowingCallable callable) {
        assertThatThrownBy(callable)
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> {
                    BusinessException exception = (BusinessException) error;
                    assertThat(exception.getCode()).isEqualTo(code);
                    assertThat(exception.getHttpStatus()).isEqualTo(status);
                });
    }
}
