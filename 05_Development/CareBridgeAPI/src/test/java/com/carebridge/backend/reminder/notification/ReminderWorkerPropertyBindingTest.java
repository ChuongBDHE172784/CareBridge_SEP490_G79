package com.carebridge.backend.reminder.notification;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ReminderWorkerPropertyBindingTest {

    @Test
    void reminderWorkersHaveIndependentFailClosedOptInFlags() throws IOException {
        String yaml = Files.readString(
                Path.of("src/main/resources/application.yaml"), StandardCharsets.UTF_8);

        assertThat(yaml)
                .contains("enabled: ${REMINDER_SCHEDULE_NOTIFICATION_ENABLED:false}")
                .contains("enabled: ${APPOINTMENT_NOTIFICATION_ENABLED:false}")
                .doesNotContain("${REMINDER_SCHEDULE_NOTIFICATION_ENABLED:${CAREBRIDGE_FCM_ENABLED:false}}")
                .contains("stale-backlog-grace-minutes: ${REMINDER_SCHEDULE_STALE_BACKLOG_GRACE_MINUTES:60}")
                .contains("stale-backlog-grace-minutes: ${APPOINTMENT_NOTIFICATION_STALE_BACKLOG_GRACE_MINUTES:60}");
    }
}
