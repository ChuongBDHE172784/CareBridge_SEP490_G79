package com.carebridge.backend.health;

import com.carebridge.backend.health.entity.DataSource;
import com.carebridge.backend.health.entity.HealthObservation;
import com.carebridge.backend.health.entity.MetricDefinition;
import com.carebridge.backend.health.entity.ObservationShape;
import com.carebridge.backend.journey.entity.JourneyStatus;
import com.carebridge.backend.journey.entity.JourneyType;
import com.carebridge.backend.journey.entity.MotherJourney;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

/** Props-isolation factory for UC27 ViewMaternalHealthTrend unit tests. */
public final class MetricTrendTestFactory {

    public static final UUID MOTHER_ID  = UUID.fromString("00000000-0000-0000-0000-000000000027");
    public static final UUID JOURNEY_ID = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000027");
    public static final UUID OTHER_USER = UUID.fromString("33333333-0000-0000-0000-000000000027");
    public static final UUID CARE_SUBJECT_ID = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000027");

    private MetricTrendTestFactory() {}

    public static MotherJourney makeJourney() {
        return MotherJourney.builder()
                .id(JOURNEY_ID)
                .ownerUserId(MOTHER_ID)
                .careSubjectId(CARE_SUBJECT_ID)
                .journeyType(JourneyType.PREGNANCY)
                .status(JourneyStatus.ACTIVE)
                .build();
    }

    public static MotherJourney makeOtherUsersJourney() {
        return MotherJourney.builder()
                .id(JOURNEY_ID)
                .ownerUserId(OTHER_USER)
                .careSubjectId(CARE_SUBJECT_ID)
                .journeyType(JourneyType.PREGNANCY)
                .status(JourneyStatus.ACTIVE)
                .build();
    }

    /** Creates count WEIGHT metrics, spaced 1 day apart, in ascending order. */
    public static List<HealthObservation> makeWeightObservations(int count) {
        List<HealthObservation> observations = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            observations.add(observation(
                    "WEIGHT",
                    new BigDecimal("62").add(new BigDecimal(i)),
                    null,
                    "kg",
                    ObservationShape.POINT,
                    Instant.now().minus(count - i, ChronoUnit.DAYS)));
        }
        return observations;
    }

    /** Creates count BLOOD_PRESSURE_DIASTOLIC metrics with both valueNumeric and valueSecondary. */
    public static List<HealthObservation> makeBloodPressureObservations(int count) {
        List<HealthObservation> observations = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            observations.add(observation(
                    "BLOOD_PRESSURE",
                    new BigDecimal("120").add(new BigDecimal(i)),
                    new BigDecimal("80").add(new BigDecimal(i)),
                    "mmHg",
                    ObservationShape.PAIRED_POINT,
                    Instant.now().minus(count - i, ChronoUnit.DAYS)));
        }
        return observations;
    }

    public static MetricDefinition makeWeightDefinition() {
        return definition("WEIGHT", ObservationShape.POINT, "kg");
    }

    public static MetricDefinition makeBloodPressureDefinition() {
        return definition("BLOOD_PRESSURE", ObservationShape.PAIRED_POINT, "mmHg");
    }

    private static HealthObservation observation(
            String code,
            BigDecimal primary,
            BigDecimal secondary,
            String unit,
            ObservationShape shape,
            Instant measuredAt) {
        return HealthObservation.builder()
                .id(UUID.randomUUID())
                .careSubjectId(CARE_SUBJECT_ID)
                .metricCode(code)
                .valueNumeric(primary)
                .valueSecondary(secondary)
                .unit(unit)
                .measuredAt(measuredAt)
                .sourceType(DataSource.MANUAL)
                .definitionVersion(1)
                .observationShape(shape)
                .qualityLabel("UNKNOWN")
                .context(new LinkedHashMap<>())
                .payload(new LinkedHashMap<>())
                .legacySource(HealthObservation.CANONICAL_SOURCE)
                .legacyId(UUID.randomUUID().toString())
                .subjectType("MOTHER")
                .build();
    }

    private static MetricDefinition definition(String code, ObservationShape shape, String unit) {
        return MetricDefinition.builder()
                .metricCode(code)
                .version(1)
                .displayName(code)
                .observationShape(shape)
                .subjectType("MOTHER")
                .manualEntrySupported(true)
                .canonicalUnit(unit)
                .acceptedInputUnits(List.of(unit))
                .precisionScale((short) 2)
                .active(true)
                .effectiveFrom(Instant.parse("2026-01-01T00:00:00Z"))
                .build();
    }
}
