package com.carebridge.backend.health.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.carebridge.backend.journey.entity.JourneyStatus;
import com.carebridge.backend.journey.entity.JourneyType;
import com.carebridge.backend.journey.entity.MotherJourney;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import com.carebridge.backend.recommendation.entity.RecommendationProfileStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MaternalHealthMetricBmiSynchronizerTest {

    @Mock
    private MotherJourneyRepository journeyRepository;

    private MaternalHealthMetricBmiSynchronizer synchronizer;

    @BeforeEach
    void setUp() {
        synchronizer = new MaternalHealthMetricBmiSynchronizer(journeyRepository);
    }

    @Test
    void synchronize_activeJourney_updatesBmiDomainAndDerived() {
        MotherJourney journey = MotherJourney.builder()
                .id(UUID.randomUUID())
                .careSubjectId(UUID.randomUUID())
                .ownerUserId(UUID.randomUUID())
                .journeyType(JourneyType.PREGNANCY)
                .status(JourneyStatus.ACTIVE)
                .recommendationProfileStatus(RecommendationProfileStatus.ACTIVE)
                .recommendationProfileJson(new LinkedHashMap<>())
                .build();

        BigDecimal weight = new BigDecimal("55.0");
        BigDecimal height = new BigDecimal("160.0");
        Instant measuredAt = Instant.parse("2026-09-08T08:00:00Z");

        synchronizer.synchronize(journey, weight, height, measuredAt);

        verify(journeyRepository).save(journey);

        Map<String, Object> envelope = journey.getRecommendationProfileJson();
        assertThat(envelope).containsKey("profile");
        assertThat(envelope).containsKey("derived");

        @SuppressWarnings("unchecked")
        Map<String, Object> profile = (Map<String, Object>) envelope.get("profile");
        @SuppressWarnings("unchecked")
        Map<String, Object> bmi = (Map<String, Object>) profile.get("bmi");

        assertThat(bmi.get("state")).isEqualTo("KNOWN");
        assertThat(bmi.get("weightKg")).isEqualTo(new BigDecimal("55.0"));
        assertThat(bmi.get("heightCm")).isEqualTo(new BigDecimal("160.0"));
        assertThat(bmi.get("measuredOn")).isEqualTo("2026-09-08");
        assertThat(bmi.get("weightContext")).isEqualTo("CURRENT_PREGNANCY");

        @SuppressWarnings("unchecked")
        Map<String, Object> derived = (Map<String, Object>) envelope.get("derived");
        assertThat(derived.get("bmi")).isEqualTo(new BigDecimal("21.48"));
        assertThat(derived.get("bmiCategory")).isEqualTo("HEALTHY_RANGE");
    }

    @Test
    void synchronize_inactiveProfileStatus_doesNotSave() {
        MotherJourney journey = MotherJourney.builder()
                .id(UUID.randomUUID())
                .careSubjectId(UUID.randomUUID())
                .ownerUserId(UUID.randomUUID())
                .journeyType(JourneyType.PREGNANCY)
                .status(JourneyStatus.ACTIVE)
                .recommendationProfileStatus(RecommendationProfileStatus.NOT_STARTED)
                .recommendationProfileJson(new LinkedHashMap<>())
                .build();

        synchronizer.synchronize(journey, new BigDecimal("55.0"), new BigDecimal("160.0"), Instant.now());

        // Should not save if status is NOT_STARTED
        org.mockito.Mockito.verifyNoInteractions(journeyRepository);
    }
}
