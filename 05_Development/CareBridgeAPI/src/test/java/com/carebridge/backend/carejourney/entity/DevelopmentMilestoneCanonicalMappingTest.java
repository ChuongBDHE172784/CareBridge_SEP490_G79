package com.carebridge.backend.carejourney.entity;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Column;
import jakarta.persistence.Table;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DevelopmentMilestoneCanonicalMappingTest {

    @Test
    void keepsBothBabyAndCanonicalSubjectColumnsInSync() throws Exception {
        assertThat(DevelopmentMilestone.class.getAnnotation(Table.class).name())
                .isEqualTo("development_milestones");
        assertThat(column("babyId")).isEqualTo("baby_id");
        assertThat(column("careSubjectId")).isEqualTo("care_subject_id");

        UUID babyId = UUID.randomUUID();
        DevelopmentMilestone milestone = DevelopmentMilestone.builder()
                .babyId(babyId)
                .build();

        milestone.alignCanonicalCareSubject();

        assertThat(milestone.getCareSubjectId()).isEqualTo(babyId);
    }

    private String column(String field) throws Exception {
        return DevelopmentMilestone.class.getDeclaredField(field).getAnnotation(Column.class).name();
    }
}
