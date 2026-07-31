package com.carebridge.backend.reminder.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.notification.repository.NotificationPreferenceRepository;
import com.carebridge.backend.reminder.entity.RecurrenceType;
import com.carebridge.backend.reminder.entity.Reminder;
import com.carebridge.backend.reminder.entity.ReminderStatus;
import com.carebridge.backend.reminder.entity.ReminderType;
import com.carebridge.backend.reminder.notification.entity.AppointmentNotificationConfig;
import com.carebridge.backend.reminder.notification.entity.AppointmentNotificationJob;
import com.carebridge.backend.reminder.notification.entity.AppointmentNotificationJobStatus;
import com.carebridge.backend.reminder.notification.repository.AppointmentNotificationConfigRepository;
import com.carebridge.backend.reminder.notification.repository.AppointmentNotificationJobRepository;
import com.carebridge.backend.reminder.notification.repository.AppointmentNotificationRuleRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AppointmentNotificationScheduleServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-30T00:00:00Z");
    private static final UUID REMINDER_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000102");

    @Mock private AppointmentNotificationConfigRepository configRepository;
    @Mock private AppointmentNotificationRuleRepository ruleRepository;
    @Mock private AppointmentNotificationJobRepository jobRepository;
    @Mock private NotificationPreferenceRepository preferenceRepository;

    private AppointmentNotificationScheduleService service;

    @BeforeEach
    void setUp() {
        service = new AppointmentNotificationScheduleService(
                configRepository,
                ruleRepository,
                jobRepository,
                preferenceRepository,
                new AppointmentNotificationRuleValidator(),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void createSnapshot_materializesOnlyFutureMilestones() {
        Reminder reminder = appointment(NOW.plusSeconds(24 * 60 * 60));
        when(jobRepository.existsByReminderIdAndOccurrenceIdAndConfigRevisionAndOffsetMinutes(
                eq(REMINDER_ID), any(UUID.class), eq(1L), anyInt())).thenReturn(false);

        List<Integer> effective = service.createSnapshot(
                reminder, List.of(-1440, -30, 0, 15), "Asia/Ho_Chi_Minh");

        assertThat(effective).containsExactly(-1440, -30, 0, 15);
        ArgumentCaptor<AppointmentNotificationJob> jobs =
                ArgumentCaptor.forClass(AppointmentNotificationJob.class);
        verify(jobRepository, org.mockito.Mockito.times(3)).save(jobs.capture());
        assertThat(jobs.getAllValues())
                .extracting(AppointmentNotificationJob::getOffsetMinutes)
                .containsExactly(-30, 0, 15);
        assertThat(jobs.getAllValues())
                .allSatisfy(job -> {
                    assertThat(job.getStatus()).isEqualTo(AppointmentNotificationJobStatus.PENDING);
                    assertThat(job.getDueAt()).isAfter(NOW);
                    assertThat(job.getNextAttemptAt()).isEqualTo(job.getDueAt());
                });
    }

    @Test
    void createSnapshot_explicitEmptyOffsetsDisablesAppointmentNotifications() {
        Reminder reminder = appointment(NOW.plusSeconds(24 * 60 * 60));

        List<Integer> effective = service.createSnapshot(reminder, List.of(), null);

        assertThat(effective).isEmpty();
        verify(jobRepository, never()).save(any(AppointmentNotificationJob.class));
        verify(ruleRepository).deleteByReminderId(REMINDER_ID);
        verify(ruleRepository).saveAll(List.of());
    }

    @Test
    void createSnapshot_usesExplicitlySavedEmptyGlobalDefaults() {
        Reminder reminder = appointment(NOW.plusSeconds(24 * 60 * 60));
        when(preferenceRepository.findAppointmentReminderDefaults(USER_ID)).thenReturn(List.of());
        when(preferenceRepository.hasAppointmentReminderDefaults(USER_ID)).thenReturn(true);

        List<Integer> effective = service.createSnapshot(reminder, null, null);

        assertThat(effective).isEmpty();
        verify(jobRepository, never()).save(any(AppointmentNotificationJob.class));
    }

    @Test
    void reschedule_incrementsRevisionAndCancelsObsoleteActiveJobs() {
        Reminder reminder = appointment(NOW.plusSeconds(60 * 60));
        AppointmentNotificationConfig config = AppointmentNotificationConfig.builder()
                .reminderId(REMINDER_ID)
                .timeZone("Asia/Ho_Chi_Minh")
                .configRevision(2L)
                .createdAt(NOW.minusSeconds(60))
                .updatedAt(NOW.minusSeconds(60))
                .build();
        when(configRepository.findById(REMINDER_ID)).thenReturn(Optional.of(config));
        when(jobRepository.existsByReminderIdAndOccurrenceIdAndConfigRevisionAndOffsetMinutes(
                eq(REMINDER_ID), any(UUID.class), eq(3L), eq(-30))).thenReturn(false);

        service.reschedule(reminder, List.of(-30), true, null);

        assertThat(config.getConfigRevision()).isEqualTo(3L);
        verify(jobRepository).cancelObsoleteRevisions(
                eq(REMINDER_ID),
                eq(3L),
                any(),
                eq(AppointmentNotificationJobStatus.CANCELLED),
                eq(NOW));
        verify(jobRepository).save(any(AppointmentNotificationJob.class));
    }

    @Test
    void extendHorizon_doesNotDuplicateExistingMilestones() {
        Reminder reminder = appointment(NOW.plusSeconds(60 * 60));
        AppointmentNotificationConfig config = AppointmentNotificationConfig.builder()
                .reminderId(REMINDER_ID)
                .timeZone("Asia/Ho_Chi_Minh")
                .configRevision(1L)
                .createdAt(NOW)
                .updatedAt(NOW)
                .build();
        when(configRepository.findById(REMINDER_ID)).thenReturn(Optional.of(config));
        when(ruleRepository.findByReminderIdOrderByOffsetMinutesAsc(REMINDER_ID))
                .thenReturn(List.of(com.carebridge.backend.reminder.notification.entity.AppointmentNotificationRule.builder()
                        .reminderId(REMINDER_ID)
                        .offsetMinutes(-30)
                        .sortOrder(0)
                        .createdAt(NOW)
                        .updatedAt(NOW)
                        .build()));
        when(jobRepository.existsByReminderIdAndOccurrenceIdAndConfigRevisionAndOffsetMinutes(
                eq(REMINDER_ID), any(UUID.class), eq(1L), eq(-30))).thenReturn(true);

        service.extendHorizon(reminder);

        verify(jobRepository, never()).save(any(AppointmentNotificationJob.class));
    }

    private Reminder appointment(Instant scheduledAt) {
        return Reminder.builder()
                .id(REMINDER_ID)
                .ownerUserId(USER_ID)
                .reminderType(ReminderType.APPOINTMENT)
                .title("Prenatal appointment")
                .scheduledAt(scheduledAt)
                .recurrenceType(RecurrenceType.NONE)
                .status(ReminderStatus.PENDING)
                .occurrenceGeneration(0L)
                .build();
    }
}
