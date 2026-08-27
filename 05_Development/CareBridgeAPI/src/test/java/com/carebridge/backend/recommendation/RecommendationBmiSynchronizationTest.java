package com.carebridge.backend.recommendation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.community.repository.CommunityTopicRepository;
import com.carebridge.backend.consent.entity.ConsentDataType;
import com.carebridge.backend.consent.entity.ConsentGrant;
import com.carebridge.backend.consent.entity.ConsentPurpose;
import com.carebridge.backend.consent.repository.ConsentGrantRepository;
import com.carebridge.backend.content.repository.ContentRepository;
import com.carebridge.backend.health.service.RecommendationBmiObservationSynchronizer;
import com.carebridge.backend.journey.entity.JourneyStatus;
import com.carebridge.backend.journey.entity.JourneyType;
import com.carebridge.backend.journey.entity.MotherJourney;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import com.carebridge.backend.journey.repository.MotherJourneyTransitionRepository;
import com.carebridge.backend.journey.repository.PregnancyOutcomeEvidenceRepository;
import com.carebridge.backend.recommendation.entity.RecommendationProfileStatus;
import com.carebridge.backend.recommendation.exception.RecommendationException;
import com.carebridge.backend.recommendation.service.RecommendationContextResolver;
import com.carebridge.backend.recommendation.service.RecommendationEligibilityPolicy;
import com.carebridge.backend.recommendation.service.RecommendationProfileValidator;
import com.carebridge.backend.recommendation.service.RecommendationRanker;
import com.carebridge.backend.recommendation.service.RecommendationService;
import com.carebridge.backend.recommendation.service.ValidatedRecommendationProfile;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class RecommendationBmiSynchronizationTest {

    private static final UUID OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000006401");
    private static final UUID JOURNEY_ID = UUID.fromString("00000000-0000-0000-0000-000000006402");
    private static final UUID CARE_SUBJECT_ID = UUID.fromString("00000000-0000-0000-0000-000000006403");
    private static final UUID SUBMISSION_ID = UUID.fromString("00000000-0000-0000-0000-000000006404");
    private static final Instant NOW = Instant.parse("2026-08-10T00:00:00Z");

    @Mock private MotherJourneyRepository journeyRepository;
    @Mock private PregnancyOutcomeEvidenceRepository outcomeEvidenceRepository;
    @Mock private MotherJourneyTransitionRepository transitionRepository;
    @Mock private UserRepository userRepository;
    @Mock private ContentRepository contentRepository;
    @Mock private CommunityTopicRepository topicRepository;
    @Mock private ConsentGrantRepository consentGrantRepository;
    @Mock private AuditService auditService;
    @Mock private RecommendationProfileValidator validator;
    @Mock private RecommendationContextResolver contextResolver;
    @Mock private RecommendationEligibilityPolicy eligibilityPolicy;
    @Mock private RecommendationRanker ranker;
    @Mock private RecommendationBmiObservationSynchronizer bmiSynchronizer;

    private ObjectMapper objectMapper;
    private RecommendationService service;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new RecommendationService(
                journeyRepository,
                outcomeEvidenceRepository,
                transitionRepository,
                userRepository,
                contentRepository,
                topicRepository,
                consentGrantRepository,
                auditService,
                objectMapper,
                validator,
                contextResolver,
                eligibilityPolicy,
                ranker,
                bmiSynchronizer,
                Clock.fixed(NOW, ZoneOffset.UTC),
                true);
    }

    @Test
    void newAcceptedProfileSynchronizesBeforeJourneyProfileCommit() throws Exception {
        MotherJourney journey = newJourney();
        ValidatedRecommendationProfile validated = validatedProfile(SUBMISSION_ID, "55.0");
        ConsentGrant consent = activeConsent(SUBMISSION_ID);
        stubNewSubmission(journey, validated, consent);

        service.putProfile(OWNER_ID, acceptedRequest());

        InOrder ordered = inOrder(bmiSynchronizer, journeyRepository);
        ordered.verify(bmiSynchronizer).synchronize(journey, SUBMISSION_ID, validated.profile());
        ordered.verify(journeyRepository).saveAndFlush(journey);
        assertThat(journey.getRecommendationProfileStatus()).isEqualTo(RecommendationProfileStatus.ACTIVE);
        assertThat(journey.getRecommendationProfileJson())
                .containsEntry("submissionId", SUBMISSION_ID.toString())
                .containsEntry("profile", validated.profile());
    }

    @Test
    void exactReplayInvokesRepairWithoutCreatingNewConsentOrProfileRevision() throws Exception {
        ValidatedRecommendationProfile validated = validatedProfile(SUBMISSION_ID, "55.0");
        MotherJourney journey = committedJourney(validated);
        when(journeyRepository.findCanonicalForUpdate(OWNER_ID)).thenReturn(Optional.of(journey));
        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(mock(User.class)));
        when(validator.validateAccept(any(), eq(JourneyType.PREGNANCY), isNull())).thenReturn(validated);
        when(consentGrantRepository.findLatestRecommendationGrant(
                eq(OWNER_ID), eq(RecommendationService.CONSENT_SCOPE), any()))
                .thenReturn(List.of(activeConsent(SUBMISSION_ID)));

        service.putProfile(OWNER_ID, acceptedRequest());

        verify(bmiSynchronizer).synchronize(journey, SUBMISSION_ID, validated.profile());
        verify(consentGrantRepository, never()).saveAndFlush(any());
        verify(journeyRepository, never()).saveAndFlush(any());
        assertThat(journey.getRecommendationProfileJson().get("revision")).isEqualTo(1);
    }

    @Test
    void sameSubmissionWithDifferentCanonicalBmiStillConflictsWithoutSynchronization() throws Exception {
        ValidatedRecommendationProfile committed = validatedProfile(SUBMISSION_ID, "55.0");
        ValidatedRecommendationProfile changed = validatedProfile(SUBMISSION_ID, "56.0");
        MotherJourney journey = committedJourney(committed);
        when(journeyRepository.findCanonicalForUpdate(OWNER_ID)).thenReturn(Optional.of(journey));
        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(mock(User.class)));
        when(validator.validateAccept(any(), eq(JourneyType.PREGNANCY), isNull())).thenReturn(changed);

        assertThatThrownBy(() -> service.putProfile(OWNER_ID, acceptedRequest()))
                .isInstanceOf(RecommendationException.class)
                .extracting("code")
                .isEqualTo("RECOMMENDATION_SUBMISSION_CONFLICT");

        verify(bmiSynchronizer, never()).synchronize(any(), any(), any());
        verify(consentGrantRepository, never()).saveAndFlush(any());
        verify(journeyRepository, never()).saveAndFlush(any());
    }

    @Test
    void synchronizationFailureStopsProfileCommitAndDoesNotExposeMeasurements() throws Exception {
        MotherJourney journey = newJourney();
        ValidatedRecommendationProfile validated = validatedProfile(SUBMISSION_ID, "55.0");
        stubNewSubmission(journey, validated, activeConsent(SUBMISSION_ID));
        doThrow(new IllegalStateException("Journey BMI synchronization unavailable"))
                .when(bmiSynchronizer).synchronize(journey, SUBMISSION_ID, validated.profile());

        assertThatThrownBy(() -> service.putProfile(OWNER_ID, acceptedRequest()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Journey BMI synchronization unavailable")
                .hasMessageNotContaining("55.0")
                .hasMessageNotContaining("160.0");

        verify(journeyRepository, never()).saveAndFlush(any());
    }

    private void stubNewSubmission(
            MotherJourney journey,
            ValidatedRecommendationProfile validated,
            ConsentGrant consent) {
        when(journeyRepository.findCanonicalForUpdate(OWNER_ID)).thenReturn(Optional.of(journey));
        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(mock(User.class)));
        when(validator.validateAccept(any(), eq(JourneyType.PREGNANCY), isNull())).thenReturn(validated);
        when(consentGrantRepository.findRecommendationGrantByEvidence(
                OWNER_ID, SUBMISSION_ID, RecommendationService.CONSENT_SCOPE)).thenReturn(Optional.empty());
        when(consentGrantRepository.findRecommendationGrants(
                OWNER_ID, RecommendationService.CONSENT_SCOPE)).thenReturn(List.of());
        when(consentGrantRepository.saveAndFlush(any())).thenReturn(consent);
        lenient().when(consentGrantRepository.findLatestRecommendationGrant(
                eq(OWNER_ID), eq(RecommendationService.CONSENT_SCOPE), any()))
                .thenReturn(List.of(consent));
    }

    private MotherJourney newJourney() {
        return MotherJourney.builder()
                .id(JOURNEY_ID)
                .ownerUserId(OWNER_ID)
                .careSubjectId(CARE_SUBJECT_ID)
                .journeyType(JourneyType.PREGNANCY)
                .status(JourneyStatus.ACTIVE)
                .recommendationProfileJson(new LinkedHashMap<>())
                .recommendationProfileStatus(RecommendationProfileStatus.NOT_STARTED)
                .build();
    }

    private MotherJourney committedJourney(ValidatedRecommendationProfile validated) {
        MotherJourney journey = newJourney();
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("schemaVersion", RecommendationConstants.SCHEMA_VERSION);
        envelope.put("revision", 1);
        envelope.put("submissionId", validated.submissionId().toString());
        envelope.put("completedAt", NOW.toString());
        envelope.put("updatedAt", NOW.toString());
        envelope.put("profile", validated.profile());
        envelope.put("derived", validated.derived());
        envelope.put("consentGrantId", 6401L);
        journey.setRecommendationProfileJson(envelope);
        journey.setRecommendationProfileVersion((short) RecommendationConstants.SCHEMA_VERSION);
        journey.setRecommendationProfileCompletedAt(NOW);
        journey.setRecommendationProfileStatus(RecommendationProfileStatus.ACTIVE);
        return journey;
    }

    private ValidatedRecommendationProfile validatedProfile(UUID submissionId, String weight) throws Exception {
        Map<String, Object> bmi = new TreeMap<>();
        bmi.put("heightCm", new BigDecimal("160.0"));
        bmi.put("measuredOn", "2026-08-10");
        bmi.put("state", "KNOWN");
        bmi.put("weightContext", "CURRENT_PREGNANCY");
        bmi.put("weightKg", new BigDecimal(weight));
        Map<String, Object> reproductive = new TreeMap<>();
        reproductive.put("state", "UNKNOWN");
        Map<String, Object> profile = new TreeMap<>();
        profile.put("bmi", bmi);
        profile.put("reproductiveHistory", reproductive);
        return new ValidatedRecommendationProfile(
                submissionId,
                profile,
                Map.of("bmi", new BigDecimal("21.48")),
                Set.of(),
                objectMapper.writeValueAsString(profile));
    }

    private ConsentGrant activeConsent(UUID submissionId) {
        return ConsentGrant.builder()
                .id(6401L)
                .userId(OWNER_ID)
                .dataType(ConsentDataType.SENSITIVE_DATA)
                .purpose(ConsentPurpose.PERSONALIZE)
                .scope(RecommendationService.CONSENT_SCOPE)
                .policyVersion(RecommendationConstants.POLICY_VERSION)
                .evidenceKey(submissionId)
                .consentGivenAt(NOW)
                .expiryAt(NOW.plusSeconds(86400))
                .status("ACTIVE")
                .build();
    }

    private com.fasterxml.jackson.databind.JsonNode acceptedRequest() {
        return objectMapper.createObjectNode().put("consentAccepted", true);
    }
}
