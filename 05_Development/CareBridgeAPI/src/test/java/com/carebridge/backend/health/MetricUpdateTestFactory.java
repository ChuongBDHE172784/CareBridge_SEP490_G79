package com.carebridge.backend.health;

import com.carebridge.backend.health.dto.UpdateMetricRequest;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

/** Props-isolation factory for UC26 UpdateMaternalHealthMetric unit tests. */
public final class MetricUpdateTestFactory {

    public static final UUID MOTHER_ID     = UUID.fromString("00000000-0000-0000-0000-000000000026");
    public static final UUID JOURNEY_ID    = UUID.fromString("eeeeeeee-0000-0000-0000-000000000026");
    public static final UUID METRIC_ID     = UUID.fromString("ffffffff-0000-0000-0000-000000000026");
    public static final UUID OTHER_USER    = UUID.fromString("22222222-0000-0000-0000-000000000026");
    public static final UUID MISSING_METRIC = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000026");
    public static final UUID CARE_SUBJECT_ID = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000026");

    private MetricUpdateTestFactory() {}

    public static MotherJourney makeActiveJourney() {
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

    /** Metric created 1 hour ago — within the 24h edit window. */
    public static HealthObservation makeRecentObservation() {
        return observationCreatedAt(Instant.now().minus(1, ChronoUnit.HOURS), new BigDecimal("65.0"));
    }

    /** Metric created 25 hours ago — OUTSIDE the 24h edit window. */
    public static HealthObservation makeOldObservation() {
        return observationCreatedAt(Instant.now().minus(25, ChronoUnit.HOURS), new BigDecimal("64.0"));
    }

    public static MetricDefinition makeWeightDefinition() {
        return MetricDefinition.builder()
                .metricCode("WEIGHT")
                .version(1)
                .displayName("WEIGHT")
                .observationShape(ObservationShape.POINT)
                .subjectType("MOTHER")
                .manualEntrySupported(true)
                .canonicalUnit("kg")
                .acceptedInputUnits(List.of("kg"))
                .precisionScale((short) 2)
                .active(true)
                .effectiveFrom(Instant.parse("2026-01-01T00:00:00Z"))
                .build();
    }

    private static HealthObservation observationCreatedAt(Instant createdAt, BigDecimal value) {
        var payload = new LinkedHashMap<String, Object>();
        payload.put("journeyId", JOURNEY_ID.toString());
        payload.put("recordStatus", "ACTIVE");
        return HealthObservation.builder()
                .id(METRIC_ID)
                .careSubjectId(CARE_SUBJECT_ID)
                .metricCode("WEIGHT")
                .valueNumeric(value)
                .unit("kg")
                .measuredAt(Instant.now().minus(2, ChronoUnit.HOURS))
                .sourceType(DataSource.MANUAL)
                .definitionVersion(1)
                .observationShape(ObservationShape.POINT)
                .qualityLabel("UNKNOWN")
                .context(new LinkedHashMap<>())
                .payload(payload)
                .legacySource(HealthObservation.CANONICAL_SOURCE)
                .legacyId(METRIC_ID.toString())
                .subjectType("MOTHER")
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .build();
    }

    public static UpdateMetricRequest makeUpdateWeightRequest() {
        UpdateMetricRequest req = new UpdateMetricRequest();
        req.setValueNumeric(new BigDecimal("66.0"));
        req.setNote("Corrected weight measurement");
        return req;
    }

    public static UpdateMetricRequest makeUpdateNoteOnlyRequest() {
        UpdateMetricRequest req = new UpdateMetricRequest();
        req.setNote("Note updated only");
        return req;
    }
}
