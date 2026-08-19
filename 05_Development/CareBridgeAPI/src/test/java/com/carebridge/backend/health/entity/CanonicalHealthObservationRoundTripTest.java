package com.carebridge.backend.health.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class CanonicalHealthObservationRoundTripTest {

    @Test
    void maternalMetricMatchesMigratedLegacySourceAndPayload() {
        UUID sourceReference = UUID.randomUUID();
        MaternalHealthMetric metric = MaternalHealthMetric.builder()
                .sourceReferenceId(sourceReference)
                .status(MetricStatus.DELETED)
                .build();
        metric.prepareCanonicalObservation();

        assertThat(metric.getLegacySource()).isEqualTo("maternal_health_metrics");
        assertThat(metric.getPayloadJson())
                .containsEntry("sourceReferenceId", sourceReference)
                .containsEntry("recordStatus", "DELETED");

        metric.setSourceReferenceId(null);
        metric.setStatus(null);
        metric.hydrateCanonicalObservation();
        assertThat(metric.getSourceReferenceId()).isEqualTo(sourceReference);
        assertThat(metric.getStatus()).isEqualTo(MetricStatus.DELETED);
    }
}
