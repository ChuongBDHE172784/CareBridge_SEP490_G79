package com.carebridge.backend.triage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.baby.entity.BabyProfile;
import com.carebridge.backend.baby.entity.BabyProfileStatus;
import com.carebridge.backend.baby.repository.BabyProfileRepository;
import com.carebridge.backend.consent.repository.ConsentGrantRepository;
import com.carebridge.backend.journey.entity.JourneyStatus;
import com.carebridge.backend.journey.entity.JourneyType;
import com.carebridge.backend.journey.entity.MotherJourney;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import com.carebridge.backend.journey.repository.MotherBaselineContextRepository;
import com.carebridge.backend.journey.service.LifecycleConsentValidator;
import com.carebridge.backend.triage.dto.request.StartIntakeConversationRequest;
import com.carebridge.backend.triage.entity.IntakeSession;
import com.carebridge.backend.triage.exception.TriageException;
import com.carebridge.backend.triage.service.LifecycleIntakeBindingService;
import com.carebridge.backend.triage.service.LifecycleSafetyMetrics;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LifecycleIntakeBindingServiceTest {
    private static final UUID OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000067");
    private static final UUID JOURNEY_ID = UUID.fromString("10000000-0000-0000-0000-000000000067");
    private static final UUID BABY_ID = UUID.fromString("20000000-0000-0000-0000-000000000067");

    private LifecycleConsentValidator consentValidator;
    private MotherJourneyRepository journeyRepository;
    private BabyProfileRepository babyRepository;
    private LifecycleIntakeBindingService service;

    @BeforeEach
    void setUp() {
        consentValidator = mock(LifecycleConsentValidator.class);
        journeyRepository = mock(MotherJourneyRepository.class);
        babyRepository = mock(BabyProfileRepository.class);
        service = new LifecycleIntakeBindingService(
                consentValidator, journeyRepository, babyRepository, Duration.ofDays(7));
        when(journeyRepository.findById(JOURNEY_ID)).thenReturn(Optional.of(activeJourney()));
    }

    @Test
    void lifecycleBoundStart_withoutClientRequestId_isRejectedBeforeOriginLookupOrTokenCreation() {
        StartIntakeConversationRequest request = babyRequest(TriageStage.INFANT, null);

        assertThatThrownBy(() -> service.bindForStart(request, TriageStage.INFANT, OWNER_ID))
                .isInstanceOfSatisfying(TriageException.class, error -> {
                    assertThat(error.getHttpStatus()).isEqualTo(org.springframework.http.HttpStatus.BAD_REQUEST);
                    assertThat(error.getCode()).isEqualTo("TRIAGE-012");
                });

        verify(consentValidator, never()).ensureEligibleForMutation(OWNER_ID);
        verify(journeyRepository, never()).findById(JOURNEY_ID);
        verify(babyRepository, never()).findByIdAndOwnerUserId(BABY_ID, OWNER_ID);
    }

    @Test
    void bindingValidation_doesNotCountCreatedBeforeNewSessionPersistence() {
        LifecycleSafetyMetrics metrics = new LifecycleSafetyMetrics();
        LifecycleIntakeBindingService measuredService = new LifecycleIntakeBindingService(
                consentValidator, journeyRepository, babyRepository,
                new com.carebridge.backend.baby.policy.BabyTriageStageClassifier(),
                metrics, Duration.ofDays(7), Clock.systemUTC());
        when(babyRepository.findByIdAndOwnerUserId(BABY_ID, OWNER_ID))
                .thenReturn(Optional.of(activeBaby(LocalDate.now().minusMonths(6))));

        measuredService.bindForStart(
                babyRequest(TriageStage.INFANT, "story67-created-boundary"),
                TriageStage.INFANT, OWNER_ID);

        assertThat(metrics.count(
                LifecycleSafetyMetrics.Boundary.CONTINUATION,
                LifecycleSafetyMetrics.Outcome.CREATED)).isZero();
    }

    @Test
    void nonPositiveContinuationTtl_isRejectedAtConstruction() {
        assertThatThrownBy(() -> new LifecycleIntakeBindingService(
                consentValidator, journeyRepository, babyRepository, Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
        assertThatThrownBy(() -> new LifecycleIntakeBindingService(
                consentValidator, journeyRepository, babyRepository, Duration.ofSeconds(-1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
    }

    @Test
    void babyOrigin_usesExistingCompletedMonthClassifierForInfantAndToddler() {
        LocalDate today = LocalDate.now();

        assertBabyStageAccepted(today.minusMonths(12).plusDays(1), TriageStage.INFANT);
        assertBabyStageAccepted(today.minusMonths(12), TriageStage.TODDLER);
        assertBabyStageAccepted(today.minusMonths(25).plusDays(1), TriageStage.TODDLER);
    }

    @Test
    void babyOrigin_startAndRevalidationDoNotRequireMotherLifecycleConsent() {
        MotherBaselineContextRepository baselineRepository =
                mock(MotherBaselineContextRepository.class);
        ConsentGrantRepository consentRepository = mock(ConsentGrantRepository.class);
        LifecycleConsentValidator realMotherConsentValidator =
                new LifecycleConsentValidator(baselineRepository, consentRepository);
        LifecycleIntakeBindingService babyService = new LifecycleIntakeBindingService(
                realMotherConsentValidator, journeyRepository, babyRepository, Duration.ofDays(7));
        when(babyRepository.findByIdAndOwnerUserId(BABY_ID, OWNER_ID))
                .thenReturn(Optional.of(activeBaby(LocalDate.now().minusMonths(6))));

        var binding = babyService.bindForStart(
                babyRequest(TriageStage.INFANT, "baby-without-mother-consent"),
                TriageStage.INFANT, OWNER_ID);

        assertThat(binding.originDashboard()).isEqualTo(OriginDashboard.BABY_PROFILE);
        assertThat(binding.journeyId()).isNull();
        babyService.revalidate(IntakeSession.builder()
                .userId(OWNER_ID)
                .babyProfileId(BABY_ID)
                .journeyId(null)
                .stage(TriageStage.INFANT)
                .originDashboard(OriginDashboard.BABY_PROFILE)
                .originReferenceId(BABY_ID)
                .build());
        verify(baselineRepository, never()).findTopByOwnerUserIdOrderByRevisionDesc(OWNER_ID);
        verify(consentRepository, never()).acquireLifecycleOwnerLock(OWNER_ID);
    }

    @Test
    void babyOrigin_rejectsRequestedStageMismatchAndEstablishedUpperBoundary() {
        LocalDate today = LocalDate.now();

        assertBabyStageRejected(today.minusMonths(12).plusDays(1), TriageStage.TODDLER);
        assertBabyStageRejected(today.minusMonths(12), TriageStage.INFANT);
        assertBabyStageRejected(today.minusMonths(25), TriageStage.TODDLER);
    }

    @Test
    void terminalRenewal_contractExposesFixedClockAndPreservesStableToken() throws Exception {
        Constructor<?> clockConstructor = Arrays.stream(LifecycleIntakeBindingService.class.getConstructors())
                .filter(candidate -> Arrays.asList(candidate.getParameterTypes()).contains(Clock.class))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Lifecycle binding must support a fixed Clock"));
        Method renew = LifecycleIntakeBindingService.class.getMethod("renewForTerminal", IntakeSession.class);

        Instant now = Instant.parse("2026-07-22T12:00:00Z");
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);
        Object[] arguments = Arrays.stream(clockConstructor.getParameterTypes())
                .map(type -> constructorArgument(type, clock))
                .toArray();
        LifecycleIntakeBindingService fixedClockService =
                (LifecycleIntakeBindingService) clockConstructor.newInstance(arguments);
        UUID stableToken = UUID.randomUUID();
        IntakeSession session = IntakeSession.builder()
                .journeyId(null)
                .continuationToken(stableToken)
                .continuationExpiresAt(now.minusSeconds(1))
                .build();

        renew.invoke(fixedClockService, session);

        assertThat(session.getContinuationToken()).isEqualTo(stableToken);
        assertThat(session.getContinuationExpiresAt()).isEqualTo(now.plus(Duration.ofDays(7)));
    }

    @Test
    void terminalRenewal_unacknowledgedNearExpiry_isExtendedToFullTtlWithoutRecoveryMetric() {
        Instant now = Instant.parse("2026-07-22T12:00:00Z");
        LifecycleSafetyMetrics metrics = new LifecycleSafetyMetrics();
        LifecycleIntakeBindingService fixedClockService = new LifecycleIntakeBindingService(
                consentValidator, journeyRepository, babyRepository,
                new com.carebridge.backend.baby.policy.BabyTriageStageClassifier(),
                metrics, Duration.ofDays(7), Clock.fixed(now, ZoneOffset.UTC));
        UUID stableToken = UUID.randomUUID();
        IntakeSession session = IntakeSession.builder()
                .journeyId(JOURNEY_ID)
                .continuationToken(stableToken)
                .continuationExpiresAt(now.plusSeconds(60))
                .build();

        fixedClockService.renewForTerminal(session);

        assertThat(session.getContinuationToken()).isEqualTo(stableToken);
        assertThat(session.getContinuationExpiresAt()).isEqualTo(now.plus(Duration.ofDays(7)));
        assertThat(metrics.count(
                LifecycleSafetyMetrics.Boundary.CONTINUATION,
                LifecycleSafetyMetrics.Outcome.RECOVERED)).isZero();
    }

    @Test
    void terminalRenewal_acknowledgedContinuation_preservesExistingExpiry() {
        Instant now = Instant.parse("2026-07-22T12:00:00Z");
        LifecycleSafetyMetrics metrics = new LifecycleSafetyMetrics();
        LifecycleIntakeBindingService fixedClockService = new LifecycleIntakeBindingService(
                consentValidator, journeyRepository, babyRepository,
                new com.carebridge.backend.baby.policy.BabyTriageStageClassifier(),
                metrics, Duration.ofDays(7), Clock.fixed(now, ZoneOffset.UTC));
        Instant existingExpiry = now.plusSeconds(60);
        IntakeSession session = IntakeSession.builder()
                .journeyId(JOURNEY_ID)
                .continuationToken(UUID.randomUUID())
                .continuationExpiresAt(existingExpiry)
                .continuationAcknowledgedAt(now.minusSeconds(1))
                .build();

        fixedClockService.renewForTerminal(session);

        assertThat(session.getContinuationExpiresAt()).isEqualTo(existingExpiry);
        assertThat(metrics.count(
                LifecycleSafetyMetrics.Boundary.CONTINUATION,
                LifecycleSafetyMetrics.Outcome.RECOVERED)).isZero();
    }

    private void assertBabyStageAccepted(LocalDate birthDate, TriageStage requestedStage) {
        when(babyRepository.findByIdAndOwnerUserId(BABY_ID, OWNER_ID))
                .thenReturn(Optional.of(activeBaby(birthDate)));

        assertThat(service.bindForStart(
                babyRequest(requestedStage, "story67-client-request"), requestedStage, OWNER_ID).stage())
                .isEqualTo(requestedStage);
    }

    private void assertBabyStageRejected(LocalDate birthDate, TriageStage requestedStage) {
        when(babyRepository.findByIdAndOwnerUserId(BABY_ID, OWNER_ID))
                .thenReturn(Optional.of(activeBaby(birthDate)));

        assertThatThrownBy(() -> service.bindForStart(
                babyRequest(requestedStage, "story67-client-request"), requestedStage, OWNER_ID))
                .isInstanceOfSatisfying(TriageException.class,
                        error -> assertThat(error.getCode()).isEqualTo("TRIAGE-015"));
    }

    private StartIntakeConversationRequest babyRequest(TriageStage stage, String clientRequestId) {
        return StartIntakeConversationRequest.builder()
                .clientRequestId(clientRequestId)
                .stage(stage)
                .babyProfileId(BABY_ID)
                .journeyId(null)
                .originDashboard(OriginDashboard.BABY_PROFILE)
                .originReferenceId(BABY_ID)
                .build();
    }

    private MotherJourney activeJourney() {
        return MotherJourney.builder()
                .id(JOURNEY_ID)
                .ownerUserId(OWNER_ID)
                .journeyType(JourneyType.POSTPARTUM)
                .status(JourneyStatus.ACTIVE)
                .build();
    }

    private BabyProfile activeBaby(LocalDate birthDate) {
        return BabyProfile.builder()
                .id(BABY_ID)
                .ownerUserId(OWNER_ID)
                .birthDate(birthDate)
                .status(BabyProfileStatus.ACTIVE)
                .build();
    }

    private Object constructorArgument(Class<?> type, Clock clock) {
        if (type == LifecycleConsentValidator.class) return consentValidator;
        if (type == MotherJourneyRepository.class) return journeyRepository;
        if (type == BabyProfileRepository.class) return babyRepository;
        if (type == Duration.class) return Duration.ofDays(7);
        if (type == Clock.class) return clock;
        return mock(type);
    }
}
