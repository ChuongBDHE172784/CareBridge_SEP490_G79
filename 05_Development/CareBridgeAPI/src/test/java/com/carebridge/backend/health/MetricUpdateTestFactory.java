package com.carebridge.backend.health;

import com.carebridge.backend.health.dto.UpdateMetricRequest;
import com.carebridge.backend.health.entity.MaternalHealthMetric;
import com.carebridge.backend.health.entity.MetricStatus;
import com.carebridge.backend.health.entity.MetricType;
import com.carebridge.backend.journey.entity.JourneyStatus;
import com.carebridge.backend.journey.entity.JourneyType;
import com.carebridge.backend.journey.entity.MotherJourney;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/** Props-isolation factory for UC26 UpdateMaternalHealthMetric unit tests. */
public final class MetricUpdateTestFactory {

    public static final UUID MOTHER_ID     = UUID.fromString("00000000-0000-0000-0000-000000000026");
    public static final UUID JOURNEY_ID    = UUID.fromString("eeeeeeee-0000-0000-0000-000000000026");
    public static final UUID METRIC_ID     = UUID.fromString("ffffffff-0000-0000-0000-000000000026");
    public static final UUID OTHER_USER    = UUID.fromString("22222222-0000-0000-0000-000000000026");
    public static final UUID MISSING_METRIC = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000026");

    private MetricUpdateTestFactory() {}

    public static MotherJourney makeActiveJourney() {
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

    /** Metric created 1 hour ago — within the 24h edit window. */
    public static MaternalHealthMetric makeRecentMetric() {
        return MaternalHealthMetric.builder()
                .id(METRIC_ID)
                .journeyId(JOURNEY_ID)
                .metricType(MetricType.WEIGHT)
                .valueNumeric(new BigDecimal("65.0"))
                .unit("kg")
                .measuredAt(Instant.now().minus(2, ChronoUnit.HOURS))
                .status(MetricStatus.ACTIVE)
                .createdAt(Instant.now().minus(1, ChronoUnit.HOURS))
                .updatedAt(Instant.now().minus(1, ChronoUnit.HOURS))
                .build();
    }

    /** Metric created 25 hours ago — OUTSIDE the 24h edit window. */
    public static MaternalHealthMetric makeOldMetric() {
        return MaternalHealthMetric.builder()
                .id(METRIC_ID)
                .journeyId(JOURNEY_ID)
                .metricType(MetricType.WEIGHT)
                .valueNumeric(new BigDecimal("64.0"))
                .unit("kg")
                .measuredAt(Instant.now().minus(26, ChronoUnit.HOURS))
                .status(MetricStatus.ACTIVE)
                .createdAt(Instant.now().minus(25, ChronoUnit.HOURS))
                .updatedAt(Instant.now().minus(25, ChronoUnit.HOURS))
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
