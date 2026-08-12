package com.carebridge.backend.journey;

import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.journey.dto.CreateJourneyRequest;
import com.carebridge.backend.journey.dto.UpdateJourneyRequest;
import com.carebridge.backend.journey.entity.GestationalDatingBasis;
import com.carebridge.backend.journey.entity.JourneyDateConfidence;
import com.carebridge.backend.journey.entity.JourneyDateSource;
import com.carebridge.backend.journey.entity.JourneyStatus;
import com.carebridge.backend.journey.entity.JourneyType;
import com.carebridge.backend.journey.entity.MotherJourney;
import com.carebridge.backend.journey.entity.MotherJourneyTransition;
import com.carebridge.backend.journey.policy.JourneyTransitionPolicy;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import com.carebridge.backend.journey.repository.MotherJourneyTransitionRepository;
import com.carebridge.backend.journey.service.IJourneyOnboardingService;
import com.carebridge.backend.journey.service.impl.JourneyTransitionServiceImpl;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.rbac.Role;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JourneyGestationalDatingServiceTest {

    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000000111");
    private static final UUID JOURNEY = UUID.fromString("00000000-0000-0000-0000-000000000222");
    private static final Instant NOW = Instant.parse("2026-07-18T03:00:00Z");

    @Mock MotherJourneyRepository journeys;
    @Mock MotherJourneyTransitionRepository transitions;
    @Mock UserRepository users;
    @Mock AuditService audit;
    @Mock ApplicationEventPublisher events;
    @Mock IJourneyOnboardingService onboarding;

    private JourneyTransitionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new JourneyTransitionServiceImpl(
                journeys,
                transitions,
                users,
                audit,
                new JourneyTransitionPolicy(),
                events,
                onboarding,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void createPersistsResolvedAuthorityWithServerEffectiveInstant() {
        when(users.findById(OWNER)).thenReturn(Optional.of(
                User.builder().id(OWNER).role(Role.MOTHER).build()));
        when(transitions.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(journeys.existsByOwnerUserIdAndStatusAndJourneyTypeIn(any(), any(), any()))
                .thenReturn(false);
        when(journeys.saveAndFlush(any())).thenAnswer(invocation -> {
            MotherJourney value = invocation.getArgument(0);
            value.setId(JOURNEY);
            return value;
        });

        CreateJourneyRequest request = new CreateJourneyRequest();
        request.setJourneyType(JourneyType.PREGNANCY);
        request.setStartDate(LocalDate.of(2026, 7, 18));
        request.setLastMenstrualDate(LocalDate.of(2026, 6, 1));
        request.setDateSource(JourneyDateSource.SELF_REPORTED);
        request.setDateConfidence(JourneyDateConfidence.ESTIMATED);
        request.setEffectiveAt(NOW.minusSeconds(3600));

        service.createJourney(request, OWNER);

        ArgumentCaptor<MotherJourney> journey = ArgumentCaptor.forClass(MotherJourney.class);
        verify(journeys).saveAndFlush(journey.capture());
        assertThat(journey.getValue().getGestationalDatingBasis())
                .isEqualTo(GestationalDatingBasis.LMP);
        assertThat(journey.getValue().getGestationalDatingRevision()).isEqualTo(1L);
        assertThat(journey.getValue().getGestationalDatingEffectiveAt()).isEqualTo(NOW);

        ArgumentCaptor<MotherJourneyTransition> transition =
                ArgumentCaptor.forClass(MotherJourneyTransition.class);
        verify(transitions).saveAndFlush(transition.capture());
        assertThat(transition.getValue().getGestationalDatingRevision()).isEqualTo(1L);
        assertThat(transition.getValue().getCanonicalLmp())
                .isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(transition.getValue().getEffectiveAt()).isEqualTo(NOW);
    }

    @Test
    void resolvedV1SameAnchorIsSemanticNoOp() {
        MotherJourney current = resolvedJourney();
        when(journeys.findById(JOURNEY)).thenReturn(Optional.of(current));

        UpdateJourneyRequest request = new UpdateJourneyRequest();
        request.setLastMenstrualDate(LocalDate.of(2026, 6, 1));
        request.setDateSource(JourneyDateSource.SELF_REPORTED);
        request.setDateConfidence(JourneyDateConfidence.ESTIMATED);

        var result = service.updateJourney(OWNER, JOURNEY, request);

        assertThat(result.getGestationalDatingRevision()).isEqualTo(2L);
        verify(journeys, never()).saveAndFlush(any());
        verify(transitions, never()).saveAndFlush(any());
    }

    @Test
    void resolvedV1CorrectionUsesTimelineMaximumAndServerInstant() {
        MotherJourney current = resolvedJourney();
        when(journeys.findById(JOURNEY)).thenReturn(Optional.of(current));
        when(transitions.findMaxGestationalDatingRevision(JOURNEY)).thenReturn(7L);
        when(transitions.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(journeys.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateJourneyRequest request = new UpdateJourneyRequest();
        request.setLastMenstrualDate(LocalDate.of(2026, 6, 2));
        request.setDateSource(JourneyDateSource.SELF_REPORTED);
        request.setDateConfidence(JourneyDateConfidence.ESTIMATED);

        service.updateJourney(OWNER, JOURNEY, request);

        assertThat(current.getGestationalDatingRevision()).isEqualTo(8L);
        assertThat(current.getGestationalDatingEffectiveAt()).isEqualTo(NOW);
    }

    @Test
    void v2CreateUsesDatingBasisWithoutLegacyProvenanceFields() {
        when(users.findById(OWNER)).thenReturn(Optional.of(
                User.builder().id(OWNER).role(Role.MOTHER).build()));
        when(transitions.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(journeys.existsByOwnerUserIdAndStatusAndJourneyTypeIn(any(), any(), any()))
                .thenReturn(false);
        when(journeys.saveAndFlush(any())).thenAnswer(invocation -> {
            MotherJourney value = invocation.getArgument(0);
            value.setId(JOURNEY);
            return value;
        });

        CreateJourneyRequest request = new CreateJourneyRequest();
        request.setJourneyType(JourneyType.PREGNANCY);
        request.setStartDate(LocalDate.of(2026, 7, 18));
        request.setDatingBasis(GestationalDatingBasis.EDD);
        request.setEstimatedDueDate(LocalDate.of(2027, 3, 8));
        request.setChecklistContractVersion(2);

        var response = service.createJourney(request, OWNER);

        assertThat(response.getGestationalDatingBasis()).isEqualTo(GestationalDatingBasis.EDD);
        assertThat(response.getCanonicalLmp()).isEqualTo(LocalDate.of(2026, 6, 1));
    }

    @Test
    void v2PostpartumEpochWritesEpochEventAndFreshRevision() {
        MotherJourney current = resolvedJourney();
        current.setJourneyType(JourneyType.POSTPARTUM);
        current.setPregnancyOutcome(com.carebridge.backend.journey.entity.PregnancyOutcomeType.LIVE_BIRTH);
        current.setDeliveryDate(LocalDate.of(2026, 7, 1));
        when(journeys.findById(JOURNEY)).thenReturn(Optional.of(current));
        when(transitions.findMaxGestationalDatingRevision(JOURNEY)).thenReturn(4L);
        when(journeys.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateJourneyRequest request = new UpdateJourneyRequest();
        request.setJourneyType(JourneyType.PREGNANCY);
        request.setDatingBasis(GestationalDatingBasis.LMP);
        request.setLastMenstrualDate(LocalDate.of(2026, 7, 2));
        request.setChecklistContractVersion(2);

        service.updateJourney(OWNER, JOURNEY, request);

        ArgumentCaptor<MotherJourneyTransition> transition =
                ArgumentCaptor.forClass(MotherJourneyTransition.class);
        verify(transitions).saveAndFlush(transition.capture());
        assertThat(transition.getValue().getEventType())
                .isEqualTo(com.carebridge.backend.journey.entity.JourneyTransitionType.PREGNANCY_EPOCH_STARTED);
        assertThat(transition.getValue().getGestationalDatingRevision()).isEqualTo(5L);
        assertThat(current.getGestationalDatingRevision()).isEqualTo(5L);
        assertThat(current.getGestationalDatingEffectiveAt()).isEqualTo(NOW);
    }

    @Test
    void v1NewEpochMayStartUnresolvedAndPersistsCanonicalEpochPayload() {
        MotherJourney current = resolvedJourney();
        current.setJourneyType(JourneyType.POSTPARTUM);
        current.setPregnancyOutcome(com.carebridge.backend.journey.entity.PregnancyOutcomeType.LIVE_BIRTH);
        current.setDeliveryDate(LocalDate.of(2026, 7, 1));
        when(journeys.findById(JOURNEY)).thenReturn(Optional.of(current));
        when(journeys.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateJourneyRequest request = new UpdateJourneyRequest();
        request.setJourneyType(JourneyType.PREGNANCY);

        service.updateJourney(OWNER, JOURNEY, request);

        assertThat(current.getLastMenstrualDate()).isNull();
        assertThat(current.getEstimatedDueDate()).isNull();
        assertThat(current.getGestationalDatingBasis()).isNull();
        ArgumentCaptor<MotherJourneyTransition> transition =
                ArgumentCaptor.forClass(MotherJourneyTransition.class);
        verify(transitions).saveAndFlush(transition.capture());
        assertThat(transition.getValue().getPregnancyEpochStarted()).isTrue();
    }

    @Test
    void prePregnancyToPregnancyStartsCanonicalEpochWithGenericReason() {
        MotherJourney current = MotherJourney.builder()
                .id(JOURNEY)
                .ownerUserId(OWNER)
                .journeyType(JourneyType.PRE_PREGNANCY)
                .status(JourneyStatus.ACTIVE)
                .version(1L)
                .dateSource(JourneyDateSource.CLINICIAN_CONFIRMED)
                .dateConfidence(JourneyDateConfidence.CONFIRMED)
                .build();
        when(journeys.findById(JOURNEY)).thenReturn(Optional.of(current));
        when(journeys.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(transitions.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateJourneyRequest request = new UpdateJourneyRequest();
        request.setJourneyType(JourneyType.PREGNANCY);
        request.setDatingBasis(GestationalDatingBasis.LMP);
        request.setLastMenstrualDate(LocalDate.of(2026, 6, 1));
        request.setChecklistContractVersion(2);

        service.updateJourney(OWNER, JOURNEY, request);

        ArgumentCaptor<MotherJourneyTransition> transition =
                ArgumentCaptor.forClass(MotherJourneyTransition.class);
        verify(transitions).saveAndFlush(transition.capture());
        assertThat(transition.getValue().getEventType())
                .isEqualTo(com.carebridge.backend.journey.entity.JourneyTransitionType.PREGNANCY_EPOCH_STARTED);
        assertThat(transition.getValue().getReason()).isEqualTo("PREGNANCY_EPOCH_STARTED");
        assertThat(current.getDateSource()).isNull();
        assertThat(current.getDateConfidence()).isNull();
    }

    @Test
    void v2BasisChangeWithSameAnchorIsDatingCorrection() {
        MotherJourney current = resolvedJourney();
        current.setGestationalDatingBasis(GestationalDatingBasis.LMP_DERIVED_FROM_EDD);
        current.setLastMenstrualDate(null);
        when(journeys.findById(JOURNEY)).thenReturn(Optional.of(current));
        when(transitions.findMaxGestationalDatingRevision(JOURNEY)).thenReturn(2L);
        when(transitions.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(journeys.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateJourneyRequest request = new UpdateJourneyRequest();
        request.setChecklistContractVersion(2);
        request.setDatingBasis(GestationalDatingBasis.EDD);
        request.setEstimatedDueDate(LocalDate.of(2027, 3, 8));

        service.updateJourney(OWNER, JOURNEY, request);

        ArgumentCaptor<MotherJourneyTransition> transition =
                ArgumentCaptor.forClass(MotherJourneyTransition.class);
        verify(transitions).saveAndFlush(transition.capture());
        assertThat(transition.getValue().getEventType())
                .isEqualTo(com.carebridge.backend.journey.entity.JourneyTransitionType.DATING_CORRECTED);
        assertThat(transition.getValue().getSupersedesDatingRevision()).isEqualTo(2L);
        assertThat(current.getGestationalDatingRevision()).isEqualTo(3L);
    }

    @Test
    void completingPregnancyClearsActiveDatingAuthorityAndPlan() {
        MotherJourney current = resolvedJourney();
        when(journeys.findById(JOURNEY)).thenReturn(Optional.of(current));
        when(journeys.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(transitions.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateJourneyRequest request = new UpdateJourneyRequest();
        request.setStatus("COMPLETED");
        request.setDeliveryDate(LocalDate.of(2026, 7, 18));
        request.setDateSource(JourneyDateSource.SELF_REPORTED);
        request.setDateConfidence(JourneyDateConfidence.CONFIRMED);

        var response = service.updateJourney(OWNER, JOURNEY, request);

        assertThat(current.getGestationalDatingBasis()).isNull();
        assertThat(current.getGestationalDatingRevision()).isNull();
        assertThat(response.getPlan()).isNull();
        assertThat(response.getCompletedGestationalWeek()).isNull();
    }

    private MotherJourney resolvedJourney() {
        return MotherJourney.builder()
                .id(JOURNEY)
                .ownerUserId(OWNER)
                .journeyType(JourneyType.PREGNANCY)
                .status(JourneyStatus.ACTIVE)
                .lastMenstrualDate(LocalDate.of(2026, 6, 1))
                .estimatedDueDate(LocalDate.of(2027, 3, 8))
                .dateSource(JourneyDateSource.SELF_REPORTED)
                .dateConfidence(JourneyDateConfidence.ESTIMATED)
                .gestationalDatingBasis(GestationalDatingBasis.LMP)
                .gestationalDatingRevision(2L)
                .gestationalDatingEffectiveAt(NOW.minusSeconds(60))
                .version(1L)
                .build();
    }
}
