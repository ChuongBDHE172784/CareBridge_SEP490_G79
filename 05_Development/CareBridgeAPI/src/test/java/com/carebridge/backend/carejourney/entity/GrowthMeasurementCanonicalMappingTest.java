package com.carebridge.backend.carejourney.entity;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Column;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;

class GrowthMeasurementCanonicalMappingTest {
    @Test
    void keepsDedicatedCanonicalGrowthTableAndSoftDeleteFields() throws Exception {
        assertThat(GrowthMeasurement.class.getAnnotation(Table.class).name())
                .isEqualTo("growth_measurements");
        assertThat(column("babyId")).isEqualTo("care_subject_id");
        assertThat(column("headCircumferenceCm")).isEqualTo("head_circumference_cm");
        assertThat(column("deletedAt")).isEqualTo("deleted_at");
    }

    private String column(String field) throws Exception {
        return GrowthMeasurement.class.getDeclaredField(field).getAnnotation(Column.class).name();
    }
}
