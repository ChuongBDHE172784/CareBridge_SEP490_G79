package com.carebridge.backend.reminder.notification;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AppointmentNotificationMigrationContractTest {

    @Test
    void migrationDefinesDurableRulesJobsAndMilestoneIdempotency() throws IOException {
        String migration;
        try (var stream = getClass().getResourceAsStream(
                "/db/migration-legacy/V20260730020000__add_appointment_notification_scheduling.sql")) {
            assertThat(stream).isNotNull();
            migration = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }

        assertThat(migration)
                .contains("appointment_notification_configs")
                .contains("appointment_notification_rules")
                .contains("appointment_notification_jobs")
                .contains("UNIQUE (reminder_id, occurrence_id, config_revision, offset_minutes)")
                .contains("uq_notification_records_appointment_milestone")
                .contains("offset_minutes BETWEEN -43200 AND 10080");
    }
}
