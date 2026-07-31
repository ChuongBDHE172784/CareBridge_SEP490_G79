package com.carebridge.backend.health;

import com.carebridge.backend.health.dto.AddMetricRequest;
import com.carebridge.backend.health.entity.DataSource;
import com.carebridge.backend.health.entity.HealthObservation;
import com.carebridge.backend.health.entity.MetricDefinition;
import com.carebridge.backend.health.entity.MetricType;
import com.carebridge.backend.health.entity.ObservationShape;
import com.carebridge.backend.journey.entity.JourneyStatus;
import com.carebridge.backend.journey.entity.JourneyType;
import com.carebridge.backend.journey.entity.MotherJourney;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

/** Props-isolation factory for UC25 AddMaternalHealthMetric unit tests. */
public final class MetricTestFactory {

    public static final UUID MOTHER_ID      = UUID.fromString("00000000-0000-0000-0000-000000000025");
    public static final UUID JOURNEY_ID     = UUID.fromString("dddddddd-0000-0000-0000-000000000025");
    public static final UUID OTHER_USER     = UUID.fromString("11111111-0000-0000-0000-000000000025");
    public static final UUID OTHER_JOURNEY  = UUID.fromString("cccccccc-0000-0000-0000-000000000025");
    public static final UUID METRIC_ID      = UUID.fromString("eeeeeeee-0000-0000-0000-000000000025");
    public static final UUID CARE_SUBJECT_ID = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000025");

    private MetricTestFactory() {}

    public static MotherJourney makeActiveJourney() {
        return MotherJourney.builder()
                .id(JOURNEY_ID)
                .ownerUserId(MOTHER_ID)
                .careSubjectId(CARE_SUBJECT_ID)
                .journeyType(JourneyType.PREGNANCY)
                .status(JourneyStatus.ACTIVE)
                .build();
    }

    public static MotherJourney makeCompletedJourney() {
        return MotherJourney.builder()
                .id(JOURNEY_ID)
                .ownerUserId(MOTHER_ID)
                .careSubjectId(CARE_SUBJECT_ID)
                .journeyType(JourneyType.PREGNANCY)
                .status(JourneyStatus.COMPLETED)
                .build();
    }

    public static MotherJourney makeOtherUsersJourney() {
        return MotherJourney.builder()
                .id(OTHER_JOURNEY)
                .ownerUserId(OTHER_USER)
                .careSubjectId(CARE_SUBJECT_ID)
                .journeyType(JourneyType.PREGNANCY)
                .status(JourneyStatus.ACTIVE)
                .build();
    }

    public static AddMetricRequest makeWeightRequest() {
        AddMetricRequest req = new AddMetricRequest();
        req.setMetricType(MetricType.WEIGHT);
        req.setValueNumeric(new BigDecimal("65.5"));
        req.setUnit("kg");
        req.setMeasuredAt(Instant.now().minusSeconds(300));
        req.setSourceType(DataSource.MANUAL);
        return req;
    }

    public static AddMetricRequest makeBloodPressureRequest() {
        AddMetricRequest req = new AddMetricRequest();
        req.setMetricType(MetricType.BLOOD_PRESSURE_DIASTOLIC);
        req.setValueNumeric(new BigDecimal("80"));
        req.setValueSecondary(new BigDecimal("120"));
        req.setUnit("mmHg");
        req.setMeasuredAt(Instant.now().minusSeconds(300));
        req.setSourceType(DataSource.MANUAL);
        return req;
    }

    public static AddMetricRequest makeBloodPressureMissingSecondary() {
        AddMetricRequest req = new AddMetricRequest();
        req.setMetricType(MetricType.BLOOD_PRESSURE_DIASTOLIC);
        req.setValueNumeric(new BigDecimal("80"));
        // valueSecondary intentionally null — must be rejected (METRIC-005)
        req.setUnit("mmHg");
        req.setMeasuredAt(Instant.now().minusSeconds(300));
        return req;
    }

    public static AddMetricRequest makeFutureMeasuredAtRequest() {
        AddMetricRequest req = new AddMetricRequest();
        req.setMetricType(MetricType.WEIGHT);
        req.setValueNumeric(new BigDecimal("65.0"));
        req.setUnit("kg");
        req.setMeasuredAt(Instant.now().plusSeconds(600)); // 10 minutes in future
        return req;
    }

    public static HealthObservation makeSavedObservation() {
        var payload = new LinkedHashMap<String, Object>();
        payload.put("journeyId", JOURNEY_ID.toString());
        payload.put("recordStatus", "ACTIVE");
        return HealthObservation.builder()
                .id(METRIC_ID)
                .careSubjectId(CARE_SUBJECT_ID)
                .metricCode("WEIGHT")
                .valueNumeric(new BigDecimal("65.5"))
                .unit("kg")
                .measuredAt(Instant.now().minusSeconds(300))
                .sourceType(DataSource.MANUAL)
                .definitionVersion(1)
                .observationShape(ObservationShape.POINT)
                .qualityLabel("UNKNOWN")
                .context(new LinkedHashMap<>())
                .payload(payload)
                .legacySource(HealthObservation.CANONICAL_SOURCE)
                .legacyId(METRIC_ID.toString())
                .subjectType("MOTHER")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    public static MetricDefinition makeWeightDefinition() {
        return definition("WEIGHT", ObservationShape.POINT, "kg");
    }

    public static MetricDefinition makeBloodPressureDefinition() {
        return definition("BLOOD_PRESSURE", ObservationShape.PAIRED_POINT, "mmHg");
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
