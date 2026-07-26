package com.carebridge.backend.health.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
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

    @Test
    void postpartumRoundTripsAllMigratedPayloadFields() {
        UUID submissionId = UUID.randomUUID();
        PostpartumLog log = PostpartumLog.builder()
                .submissionId(submissionId)
                .logDate(LocalDate.of(2026, 7, 26))
                .moodLevel((short) 4)
                .breastfeedingNote("comfortable")
                .status(PostpartumLogStatus.DELETED)
                .build();
        log.prepareCanonicalObservation();

        assertThat(log.getLegacySource()).isEqualTo("postpartum_logs");
        assertThat(log.getPayloadJson())
                .containsEntry("submissionId", submissionId)
                .containsEntry("moodLevel", (short) 4)
                .containsEntry("breastfeedingNote", "comfortable")
                .containsEntry("recordStatus", "DELETED");

        log.setSubmissionId(null);
        log.setMoodLevel(null);
        log.setBreastfeedingNote(null);
        log.setStatus(null);
        log.hydrateCanonicalObservation();
        assertThat(log.getSubmissionId()).isEqualTo(submissionId);
        assertThat(log.getMoodLevel()).isEqualTo((short) 4);
        assertThat(log.getBreastfeedingNote()).isEqualTo("comfortable");
        assertThat(log.getStatus()).isEqualTo(PostpartumLogStatus.DELETED);
    }
}
