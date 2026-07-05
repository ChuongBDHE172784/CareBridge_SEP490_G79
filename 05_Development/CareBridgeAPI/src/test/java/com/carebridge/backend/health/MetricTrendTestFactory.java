package com.carebridge.backend.health;

import com.carebridge.backend.health.entity.MaternalHealthMetric;
import com.carebridge.backend.health.entity.MetricStatus;
import com.carebridge.backend.health.entity.MetricType;
import com.carebridge.backend.journey.entity.JourneyStatus;
import com.carebridge.backend.journey.entity.JourneyType;
import com.carebridge.backend.journey.entity.MotherJourney;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Props-isolation factory for UC27 ViewMaternalHealthTrend unit tests. */
public final class MetricTrendTestFactory {

    public static final UUID MOTHER_ID  = UUID.fromString("00000000-0000-0000-0000-000000000027");
    public static final UUID JOURNEY_ID = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000027");
    public static final UUID OTHER_USER = UUID.fromString("33333333-0000-0000-0000-000000000027");

    private MetricTrendTestFactory() {}

    public static MotherJourney makeJourney() {
        return MotherJourney.builder()
                .id(JOURNEY_ID)
                .ownerUserId(MOTHER_ID)
                .journeyType(JourneyType.PREGNANCY)
                .status(JourneyStatus.ACTIVE)
                .build();
    }

    public static MotherJourney makeOtherUsersJourney() {
        return MotherJourney.builder()
                .id(JOURNEY_ID)
                .ownerUserId(OTHER_USER)
                .journeyType(JourneyType.PREGNANCY)
                .status(JourneyStatus.ACTIVE)
                .build();
    }

    /** Creates count WEIGHT metrics, spaced 1 day apart, in ascending order. */
    public static List<MaternalHealthMetric> makeWeightMetrics(int count) {
        List<MaternalHealthMetric> metrics = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            metrics.add(MaternalHealthMetric.builder()
                    .id(UUID.randomUUID())
                    .journeyId(JOURNEY_ID)
                    .metricType(MetricType.WEIGHT)
                    .valueNumeric(new BigDecimal("62").add(new BigDecimal(i)))
                    .unit("kg")
                    .measuredAt(Instant.now().minus(count - i, ChronoUnit.DAYS))
                    .status(MetricStatus.ACTIVE)
                    .build());
        }
        return metrics;
    }

    /** Creates count BLOOD_PRESSURE_DIASTOLIC metrics with both valueNumeric and valueSecondary. */
    public static List<MaternalHealthMetric> makeBloodPressureMetrics(int count) {
        List<MaternalHealthMetric> metrics = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            metrics.add(MaternalHealthMetric.builder()
                    .id(UUID.randomUUID())
                    .journeyId(JOURNEY_ID)
                    .metricType(MetricType.BLOOD_PRESSURE_DIASTOLIC)
                    .valueNumeric(new BigDecimal("80").add(new BigDecimal(i)))
                    .valueSecondary(new BigDecimal("120").add(new BigDecimal(i)))
                    .unit("mmHg")
                    .measuredAt(Instant.now().minus(count - i, ChronoUnit.DAYS))
                    .status(MetricStatus.ACTIVE)
                    .build());
        }
        return metrics;
    }
}
