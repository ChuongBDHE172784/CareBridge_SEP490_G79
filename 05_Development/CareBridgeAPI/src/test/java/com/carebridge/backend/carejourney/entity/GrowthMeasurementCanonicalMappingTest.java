package com.carebridge.backend.carejourney.entity;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.lang.reflect.Field;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

/**
 * Guards the wave-13 cutover decision (V3 §3.12): growth readings live in
 * {@code health_observations}, and {@link GrowthMeasurement} is only the session-shaped
 * projection the DTOs and the growth chart are built from.
 *
 * <p>This test used to assert the opposite — that the class mapped a dedicated
 * {@code growth_measurements} table. It is inverted rather than deleted because the thing
 * worth protecting is unchanged: which store owns growth data. Re-adding {@code @Entity} here
 * would resurrect a second source of truth, and {@code ddl-auto: validate} would not catch it
 * while the source table still exists.
 */
class GrowthMeasurementCanonicalMappingTest {

    @Test
    void isAProjectionRatherThanASecondSourceOfTruth() {
        assertThat(GrowthMeasurement.class.getAnnotation(Entity.class))
                .as("growth readings belong to health_observations, not a table of their own")
                .isNull();
        assertThat(GrowthMeasurement.class.getAnnotation(Table.class)).isNull();
        assertThat(Arrays.stream(GrowthMeasurement.class.getDeclaredFields())
                .anyMatch(field -> field.getAnnotation(jakarta.persistence.Column.class) != null))
                .as("no field may carry a JPA column mapping")
                .isFalse();
    }

    @Test
    void keepsTheSessionShapeTheProjectionIsBuiltFrom() throws Exception {
        // GrowthMeasurementStore reconstructs these from up to three observation rows sharing
        // a measurement_group_id; losing one silently empties part of the growth chart.
        for (String field : new String[] {
                "growthMeasurementId", "babyId", "careSubjectId", "measuredDate",
                "weightKg", "heightCm", "headCircumferenceCm", "sourceType", "note", "deletedAt"}) {
            Field declared = GrowthMeasurement.class.getDeclaredField(field);
            assertThat(declared).as(field).isNotNull();
        }
    }

    @Test
    void mirrorsTheTwoIdentifiersCallersMayUseInterchangeably() {
        GrowthMeasurement fromBabyId = GrowthMeasurement.builder()
                .babyId(java.util.UUID.randomUUID()).build();
        fromBabyId.alignCanonicalCareSubject();
        assertThat(fromBabyId.getCareSubjectId()).isEqualTo(fromBabyId.getBabyId());

        GrowthMeasurement fromCareSubject = GrowthMeasurement.builder()
                .careSubjectId(java.util.UUID.randomUUID()).build();
        fromCareSubject.alignCanonicalCareSubject();
        assertThat(fromCareSubject.getBabyId()).isEqualTo(fromCareSubject.getCareSubjectId());
    }
}
