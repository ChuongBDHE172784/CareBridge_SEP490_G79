package com.carebridge.backend.reminder.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.reminder.job.repository.NotificationJobRepository;
import com.carebridge.backend.reminder.job.repository.NotificationJobRepository;
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
        assertFlushesBeforeBulkUpdate(NotificationJobRepository.class);
    }

    @Test
    void appointmentTransitionFlushesBeforeBulkUpdate() throws Exception {
        assertFlushesBeforeBulkUpdate(NotificationJobRepository.class);
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
