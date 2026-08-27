package com.carebridge.backend.reminder.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.reminder.schedule.dto.CreateReminderScheduleRequest;
import com.carebridge.backend.reminder.schedule.dto.ReminderScheduleResponse;
import com.carebridge.backend.reminder.schedule.entity.ReminderSchedule;
import com.carebridge.backend.reminder.job.entity.NotificationJob;
import com.carebridge.backend.reminder.job.entity.NotificationJobType;
import com.carebridge.backend.reminder.job.repository.NotificationJobRepository;
import com.carebridge.backend.reminder.schedule.repository.ReminderScheduleRepository;
import com.carebridge.backend.reminder.schedule.service.ReminderScheduleServiceImpl;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReminderScheduleServiceTest {
    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID SCHEDULE = UUID.fromString("00000000-0000-0000-0000-000000000102");
    private static final Instant NOW = Instant.parse("2026-08-02T00:00:00Z");

    @Mock private ReminderScheduleRepository scheduleRepository;
    @Mock private NotificationJobRepository jobRepository;
    private ReminderScheduleServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ReminderScheduleServiceImpl(scheduleRepository, jobRepository,
                Clock.fixed(NOW, ZoneOffset.UTC));
        lenient().when(scheduleRepository.save(any(ReminderSchedule.class))).thenAnswer(invocation -> {
            ReminderSchedule schedule = invocation.getArgument(0);
            if (schedule.getId() == null) schedule.setId(SCHEDULE);
            return schedule;
        });
        lenient().when(jobRepository.existsByJobTypeAndScheduleIdAndScheduleRevisionAndOccurrenceDateAndLocalTime(eq(NotificationJobType.REMINDER_SCHEDULE), 
                any(), anyLong(), any(), any())).thenReturn(false);
        // Times now live on the schedule aggregate; there is no child table to stub.
    }

    @Test
    void create_dailySchedule_normalizesTimesAndMaterializesOneJobPerFutureTime() {
        ReminderScheduleResponse result = service.create(OWNER,
                new CreateReminderScheduleRequest("Cho con bú",
                        List.of("12:00", "07:00", "12:00"), "Asia/Ho_Chi_Minh",
                        com.carebridge.backend.reminder.schedule.entity.ReminderScheduleRecurrence.DAILY,
                        java.time.LocalDate.of(2026, 8, 3), null, true));

        assertThat(result.times()).containsExactly("07:00", "12:00");
        assertThat(result.revision()).isEqualTo(1L);
        ArgumentCaptor<NotificationJob> jobs = ArgumentCaptor.forClass(NotificationJob.class);
        verify(jobRepository, times(72)).save(jobs.capture());
        assertThat(jobs.getAllValues()).allSatisfy(job -> {
            assertThat(job.getScheduleId()).isEqualTo(SCHEDULE);
            assertThat(job.getScheduleRevision()).isEqualTo(1L);
            assertThat(job.getDueAt()).isAfter(NOW);
        });
    }

    @Test
    void create_rejectsInvalidTimezone() {
        assertThatThrownBy(() -> service.create(OWNER,
                new CreateReminderScheduleRequest("Test", List.of("07:00"), "Invalid/Zone",
                        null, null, null, true)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("timeZone");
    }
}
