package com.carebridge.backend.reminder.entity;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Column;
import org.junit.jupiter.api.Test;

class ReminderCanonicalMappingTest {
    @Test
    void mapsWave6ReminderColumnsExactly() throws Exception {
        assertThat(column("journeyId")).isEqualTo("journey_id");
        assertThat(column("babyId")).isEqualTo("baby_id");
        assertThat(column("scheduledAt")).isEqualTo("scheduled_at");
        assertThat(column("recurrenceType")).isEqualTo("recurrence_type");
        assertThat(column("recurrenceEndDate")).isEqualTo("recurrence_end_date");
        assertThat(column("fcmJobId")).isEqualTo("fcm_job_id");
        assertThat(Reminder.builder().build().getTaskType()).isEqualTo("SCHEDULED_REMINDER");
    }

    private String column(String field) throws Exception {
        return Reminder.class.getDeclaredField(field).getAnnotation(Column.class).name();
    }
}
