package com.carebridge.backend.recommendation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.community.repository.CommunityTopicRepository;
import com.carebridge.backend.consent.entity.ConsentDataType;
import com.carebridge.backend.consent.entity.ConsentGrant;
import com.carebridge.backend.consent.entity.ConsentPurpose;
import com.carebridge.backend.consent.repository.ConsentGrantRepository;
import com.carebridge.backend.content.repository.ContentRepository;
import com.carebridge.backend.journey.entity.JourneyStatus;
import com.carebridge.backend.journey.entity.JourneyType;
import com.carebridge.backend.journey.entity.MotherJourney;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import com.carebridge.backend.journey.repository.PregnancyOutcomeEvidenceRepository;
import com.carebridge.backend.recommendation.entity.RecommendationProfileStatus;
import com.carebridge.backend.recommendation.service.RecommendationContextResolver;
import com.carebridge.backend.recommendation.service.RecommendationEligibilityPolicy;
import com.carebridge.backend.recommendation.service.RecommendationProfileValidator;
import com.carebridge.backend.recommendation.service.RecommendationRanker;
import com.carebridge.backend.recommendation.service.RecommendationService;
import com.carebridge.backend.recommendation.service.ValidatedRecommendationProfile;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.mockito.Mockito.mock;

/** Minimum privacy gate: sensitive profile values never enter audit details. */
class RecommendationPrivacyBoundaryTest {

    private static final UUID OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID JOURNEY_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID SUBMISSION_ID = UUID.fromString("00000000-0000-0000-0000-000000000201");
    private static final Instant NOW = Instant.parse("2026-08-02T00:00:00Z");

    private MotherJourneyRepository journeyRepository;
    private PregnancyOutcomeEvidenceRepository outcomeEvidenceRepository;
    private UserRepository userRepository;
    private ContentRepository contentRepository;
    private CommunityTopicRepository topicRepository;
    private ConsentGrantRepository consentGrantRepository;
    private AuditService auditService;
    private RecommendationProfileValidator validator;
    private RecommendationService service;

    @BeforeEach
    void setUp() {
        journeyRepository = mock(MotherJourneyRepository.class);
        outcomeEvidenceRepository = mock(PregnancyOutcomeEvidenceRepository.class);
        userRepository = mock(UserRepository.class);
        contentRepository = mock(ContentRepository.class);
        topicRepository = mock(CommunityTopicRepository.class);
        consentGrantRepository = mock(ConsentGrantRepository.class);
        auditService = mock(AuditService.class);
        validator = mock(RecommendationProfileValidator.class);

        service = new RecommendationService(
                journeyRepository,
                outcomeEvidenceRepository,
                userRepository,
                contentRepository,
                topicRepository,
                consentGrantRepository,
                auditService,
                new ObjectMapper(),
                validator,
                new RecommendationContextResolver(Clock.fixed(NOW, ZoneOffset.UTC)),
                new RecommendationEligibilityPolicy(),
                new RecommendationRanker(),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void profileAuditPayloadContainsOnlyAllowlistedMetadata() {
        MotherJourney journey = MotherJourney.builder()
                .id(JOURNEY_ID)
                .ownerUserId(OWNER_ID)
                .careSubjectId(UUID.randomUUID())
                .journeyType(JourneyType.PRE_PREGNANCY)
                .status(JourneyStatus.ACTIVE)
                .recommendationProfileStatus(RecommendationProfileStatus.NOT_STARTED)
                .recommendationProfileJson(new LinkedHashMap<>())
                .build();
        User user = User.builder().id(OWNER_ID).dateOfBirth(LocalDate.of(1990, 1, 1)).build();
        Map<String, Object> profile = Map.of(
                "bmi", Map.of("heightCm", "BMI_RAW_CANARY", "weightKg", "WEIGHT_RAW_CANARY"));
        ValidatedRecommendationProfile validated = new ValidatedRecommendationProfile(
                SUBMISSION_ID, profile, Map.of("bmiCategory", "HEALTHY_RANGE"), Set.of(), "{}");
        ConsentGrant activeGrant = ConsentGrant.builder()
                .id(42L)
                .userId(OWNER_ID)
                .dataType(ConsentDataType.SENSITIVE_DATA)
                .purpose(ConsentPurpose.PERSONALIZE)
                .scope(RecommendationService.CONSENT_SCOPE)
                .policyVersion(RecommendationConstants.POLICY_VERSION)
                .evidenceKey(SUBMISSION_ID)
                .consentGivenAt(NOW)
                .expiryAt(NOW.plusSeconds(86_400))
                .status("ACTIVE")
                .build();

        when(journeyRepository.findCanonicalForUpdate(OWNER_ID)).thenReturn(Optional.of(journey));
        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(user));
        when(consentGrantRepository.findRecommendationGrantByEvidence(
                OWNER_ID, SUBMISSION_ID, RecommendationService.CONSENT_SCOPE))
                .thenReturn(Optional.empty());
        when(consentGrantRepository.findRecommendationGrants(OWNER_ID, RecommendationService.CONSENT_SCOPE))
                .thenReturn(List.of());
        when(consentGrantRepository.saveAndFlush(any(ConsentGrant.class))).thenReturn(activeGrant);
        when(consentGrantRepository.findLatestRecommendationGrant(
                eq(OWNER_ID), eq(RecommendationService.CONSENT_SCOPE), any()))
                .thenReturn(List.of(activeGrant));
        when(journeyRepository.saveAndFlush(any(MotherJourney.class))).thenReturn(journey);
        when(validator.validateAccept(any(), eq(JourneyType.PRE_PREGNANCY), eq(user.getDateOfBirth())))
                .thenReturn(validated);

        service.putProfile(OWNER_ID, new ObjectMapper().createObjectNode());

        ArgumentCaptor<Object> details = ArgumentCaptor.forClass(Object.class);
        verify(auditService, times(2)).log(
                any(AuditAction.class), eq(OWNER_ID), eq("MotherJourney"), eq(JOURNEY_ID.toString()), details.capture());

        Set<String> allowlistedKeys = Set.of(
                "eventKind", "profileSchemaVersion", "profileRevision", "policyVersion", "journeyId",
                "profileStatus", "occurredAt", "correlationId", "consentGrantId");
        for (Object value : details.getAllValues()) {
            assertThat(value).isInstanceOf(Map.class);
            Map<?, ?> payload = (Map<?, ?>) value;
            assertThat(payload.keySet().stream().map(String::valueOf).toList())
                    .containsOnlyElementsOf(allowlistedKeys);
            assertThat(payload.toString())
                    .doesNotContain("BMI_RAW_CANARY")
                    .doesNotContain("WEIGHT_RAW_CANARY");
        }
    }
}
