package com.carebridge.backend.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.health.entity.DataSource;
import com.carebridge.backend.health.entity.HealthObservation;
import com.carebridge.backend.health.repository.HealthObservationRepository;
import com.carebridge.backend.health.service.RecommendationBmiObservationSynchronizer;
import com.carebridge.backend.journey.entity.JourneyStatus;
import com.carebridge.backend.journey.entity.JourneyType;
import com.carebridge.backend.journey.entity.MotherJourney;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecommendationBmiObservationSynchronizerTest {

    private static final UUID JOURNEY_ID = UUID.fromString("00000000-0000-0000-0000-000000006301");
    private static final UUID OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000006302");
    private static final UUID CARE_SUBJECT_ID = UUID.fromString("00000000-0000-0000-0000-000000006303");
    private static final UUID SUBMISSION_ID = UUID.fromString("00000000-0000-0000-0000-000000006304");
    private static final String LEGACY_ID = "recommendation-bmi:" + SUBMISSION_ID;

    @Mock
    private HealthObservationRepository observationRepository;

    private RecommendationBmiObservationSynchronizer synchronizer;

    @BeforeEach
    void setUp() {
        synchronizer = new RecommendationBmiObservationSynchronizer(observationRepository);
    }

    @Test
    void knownBmiPersistsOneCanonicalOwnerScopedObservation() {
        when(observationRepository.findByLegacySourceAndLegacyId(
                HealthObservation.CANONICAL_SOURCE, LEGACY_ID)).thenReturn(Optional.empty());

        synchronizer.synchronize(journey(), SUBMISSION_ID, knownProfile());

        ArgumentCaptor<HealthObservation> captor = ArgumentCaptor.forClass(HealthObservation.class);
        verify(observationRepository).save(captor.capture());
        HealthObservation saved = captor.getValue();
        assertThat(saved.getCareSubjectId()).isEqualTo(CARE_SUBJECT_ID);
        assertThat(saved.getMetricCode()).isEqualTo("BMI");
        assertThat(saved.getValueNumeric()).isEqualByComparingTo("21.48");
        assertThat(saved.getUnit()).isEqualTo("kg/m²");
        assertThat(saved.getMeasuredAt()).isEqualTo(Instant.parse("2026-08-09T17:00:00Z"));
        assertThat(saved.getSourceType()).isEqualTo(DataSource.MANUAL);
        assertThat(saved.getLegacySource()).isEqualTo(HealthObservation.CANONICAL_SOURCE);
        assertThat(saved.getLegacyId()).isEqualTo(LEGACY_ID);
        assertThat(saved.getSourceRecordId()).isNull();
        assertThat(saved.getContext()).containsExactly(
                Map.entry("weightKg", new BigDecimal("55.0")),
                Map.entry("heightCm", new BigDecimal("160.0")),
                Map.entry("weightContext", "CURRENT_PREGNANCY"));
        assertThat(saved.getPayload())
                .containsEntry("journeyId", JOURNEY_ID.toString())
                .containsEntry("recommendationSubmissionId", SUBMISSION_ID.toString())
                .containsEntry("source", "RECOMMENDATION_PROFILE")
                .doesNotContainKeys("weightKg", "heightCm");
    }

    @Test
    void exactReplayRepairsOnlyWhenIdentityIsMissingAndNeverDuplicates() {
        when(observationRepository.findByLegacySourceAndLegacyId(
                HealthObservation.CANONICAL_SOURCE, LEGACY_ID))
                .thenReturn(Optional.of(HealthObservation.builder()
                        .careSubjectId(CARE_SUBJECT_ID)
                        .metricCode("BMI")
                        .legacySource(HealthObservation.CANONICAL_SOURCE)
                        .legacyId(LEGACY_ID)
                        .build()));

        synchronizer.synchronize(journey(), SUBMISSION_ID, knownProfile());

        verify(observationRepository, never()).save(any());
    }

    @Test
    void unknownBmiDoesNotCreateOrDeleteJourneyHistory() {
        synchronizer.synchronize(journey(), SUBMISSION_ID, Map.of(
                "bmi", Map.of("state", "UNKNOWN")));

        verify(observationRepository, never()).findByLegacySourceAndLegacyId(any(), any());
        verify(observationRepository, never()).save(any());
        verify(observationRepository, never()).delete(any());
    }

    @Test
    void existingIdentityOwnedByAnotherCareSubjectCannotSuppressCurrentJourney() {
        when(observationRepository.findByLegacySourceAndLegacyId(
                HealthObservation.CANONICAL_SOURCE, LEGACY_ID))
                .thenReturn(Optional.of(HealthObservation.builder()
                        .careSubjectId(UUID.fromString("00000000-0000-0000-0000-000000006399"))
                        .metricCode("BMI")
                        .legacySource(HealthObservation.CANONICAL_SOURCE)
                        .legacyId(LEGACY_ID)
                        .build()));

        assertThatThrownBy(() -> synchronizer.synchronize(journey(), SUBMISSION_ID, knownProfile()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Recommendation BMI identity is owned by another observation context");
        verify(observationRepository, never()).save(any());
    }

    @Test
    void knownBmiRequiresSubmissionIdentity() {
        assertThatThrownBy(() -> synchronizer.synchronize(journey(), null, knownProfile()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Recommendation BMI submission identity is unavailable");
        verify(observationRepository, never()).findByLegacySourceAndLegacyId(any(), any());
        verify(observationRepository, never()).save(any());
    }

    private MotherJourney journey() {
        return MotherJourney.builder()
                .id(JOURNEY_ID)
                .ownerUserId(OWNER_ID)
                .careSubjectId(CARE_SUBJECT_ID)
                .journeyType(JourneyType.PREGNANCY)
                .status(JourneyStatus.ACTIVE)
                .build();
    }

    private Map<String, Object> knownProfile() {
        return Map.of("bmi", Map.of(
                "state", "KNOWN",
                "weightKg", new BigDecimal("55.0"),
                "heightCm", new BigDecimal("160.0"),
                "weightContext", "CURRENT_PREGNANCY",
                "measuredOn", "2026-08-10"));
    }
}
