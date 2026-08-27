package com.carebridge.backend.health.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MetricDefinitionEntityContractTest {

    @Test
    void entityCarriesVersionedShapeAndPolicyMetadata() {
        MetricDefinition definition = MetricDefinition.builder()
                .metricCode("BLOOD_PRESSURE")
                .version(1)
                .displayName("Huyết áp")
                .observationShape(ObservationShape.PAIRED_POINT)
                .canonicalUnit("mmHg")
                .acceptedInputUnits(List.of("mmHg"))
                .requiredContextSchema(Map.of("required", List.of("systolic", "diastolic")))
                .aggregationPolicy(Map.of("method", "NONE"))
                .allowedJourneyStages(List.of("PREGNANCY"))
                .effectiveFrom(Instant.parse("2026-07-31T00:00:00Z"))
                .build();

        assertThat(definition.getMetricCode()).isEqualTo("BLOOD_PRESSURE");
        assertThat(definition.getObservationShape()).isEqualTo(ObservationShape.PAIRED_POINT);
        assertThat(definition.getAcceptedInputUnits()).containsExactly("mmHg");
        assertThat(definition.getRequiredContextSchema())
                .containsEntry("required", List.of("systolic", "diastolic"));
        assertThat(definition.getAggregationPolicy()).containsEntry("method", "NONE");
        assertThat(definition.getAllowedJourneyStages()).containsExactly("PREGNANCY");
        assertThat(definition.isActive()).isTrue();
        assertThat(definition.getSubjectType()).isEqualTo("MOTHER");
    }

    @Test
    void entityAppliesDatabaseCompatibleDefaultsBeforePersistence() {
        MetricDefinition definition = MetricDefinition.builder()
                .metricCode("WEIGHT")
                .version(1)
                .displayName("Cân nặng")
                .observationShape(ObservationShape.POINT)
                .acceptedInputUnits(null)
                .requiredContextSchema(null)
                .plausibilityPolicy(null)
                .aggregationPolicy(null)
                .chartPolicy(null)
                .qualityPolicy(null)
                .allowedJourneyStages(null)
                .effectiveFrom(null)
                .build();

        definition.applyDefaults();

        assertThat(definition.getAcceptedInputUnits()).isEmpty();
        assertThat(definition.getRequiredContextSchema()).isEmpty();
        assertThat(definition.getPlausibilityPolicy()).isEmpty();
        assertThat(definition.getAggregationPolicy()).isEmpty();
        assertThat(definition.getChartPolicy()).isEmpty();
        assertThat(definition.getQualityPolicy()).isEmpty();
        assertThat(definition.getAllowedJourneyStages()).isEmpty();
        assertThat(definition.getEffectiveFrom()).isNotNull();
        assertThat(definition.getSubjectType()).isEqualTo("MOTHER");
    }
}
