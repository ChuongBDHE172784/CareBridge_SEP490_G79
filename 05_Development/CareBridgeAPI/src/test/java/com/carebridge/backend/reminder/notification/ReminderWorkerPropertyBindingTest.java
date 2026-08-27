package com.carebridge.backend.reminder.notification;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ReminderWorkerPropertyBindingTest {

    /**
     * The reminder-schedule worker and the appointment worker must each own a dedicated switch:
     * neither may be chained to the shared {@code CAREBRIDGE_FCM_ENABLED} flag, otherwise turning
     * one feature off silently takes the other down with it. Both now ship enabled by default —
     * a missed care reminder is the worse failure for this product — and each keeps its own
     * stale-backlog grace window so one blocked queue cannot starve the other.
     */
    @Test
    void reminderWorkersHaveIndependentEnablementFlags() throws IOException {
        String yaml = Files.readString(
                Path.of("src/main/resources/application.yaml"), StandardCharsets.UTF_8);

        assertThat(yaml)
                .contains("enabled: ${REMINDER_SCHEDULE_NOTIFICATION_ENABLED:true}")
                .contains("enabled: ${APPOINTMENT_NOTIFICATION_ENABLED:true}")
                .doesNotContain("${REMINDER_SCHEDULE_NOTIFICATION_ENABLED:${CAREBRIDGE_FCM_ENABLED")
                .doesNotContain("${APPOINTMENT_NOTIFICATION_ENABLED:${CAREBRIDGE_FCM_ENABLED")
                .contains("stale-backlog-grace-minutes: ${REMINDER_SCHEDULE_STALE_BACKLOG_GRACE_MINUTES:60}")
                .contains("stale-backlog-grace-minutes: ${APPOINTMENT_NOTIFICATION_STALE_BACKLOG_GRACE_MINUTES:60}");
    }
}
