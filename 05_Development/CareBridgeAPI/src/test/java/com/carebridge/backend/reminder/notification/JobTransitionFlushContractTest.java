package com.carebridge.backend.reminder.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.reminder.notification.repository.AppointmentNotificationJobRepository;
import com.carebridge.backend.reminder.schedule.repository.ReminderScheduleJobRepository;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Modifying;

/**
 * The terminal job update carries a foreign key to notification_records.
 * Its bulk JPQL update must flush pending notification inserts first.
 */
class JobTransitionFlushContractTest {

    @Test
    void reminderScheduleTransitionFlushesBeforeBulkUpdate() throws Exception {
        assertFlushesBeforeBulkUpdate(ReminderScheduleJobRepository.class);
    }

    @Test
    void appointmentTransitionFlushesBeforeBulkUpdate() throws Exception {
        assertFlushesBeforeBulkUpdate(AppointmentNotificationJobRepository.class);
    }

    private static void assertFlushesBeforeBulkUpdate(Class<?> repositoryType) throws Exception {
        Method method = java.util.Arrays.stream(repositoryType.getMethods())
                .filter(candidate -> candidate.getName().equals("transitionAfterProcessing"))
                .findFirst()
                .orElseThrow();
        Modifying modifying = method.getAnnotation(Modifying.class);

        assertThat(modifying).isNotNull();
        assertThat(modifying.flushAutomatically()).isTrue();
        assertThat(modifying.clearAutomatically()).isTrue();
    }
}
