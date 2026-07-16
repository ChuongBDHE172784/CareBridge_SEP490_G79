package com.carebridge.backend.reminder;

import com.carebridge.backend.reminder.dto.TodayTaskItem;
import com.carebridge.backend.reminder.entity.RecurrenceType;
import com.carebridge.backend.reminder.entity.ReminderStatus;
import com.carebridge.backend.reminder.entity.ReminderType;
import com.carebridge.backend.reminder.repository.CareTaskRepository;
import com.carebridge.backend.reminder.repository.ReminderRepository;
import com.carebridge.backend.reminder.service.ReminderRecurrenceService;
import com.carebridge.backend.reminder.service.impl.TodayTaskServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TodayTaskServiceTest {

    @Mock private ReminderRepository reminderRepository;
    @Mock private CareTaskRepository careTaskRepository;
    private TodayTaskServiceImpl todayTaskService;

    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @BeforeEach
    void setUp() {
        todayTaskService = new TodayTaskServiceImpl(
                reminderRepository,
                careTaskRepository,
                new ReminderRecurrenceService());
    }

    @Test
    void getTodayTasks_withMixedTasks_returnsMergedSortedList() {
        var vaccination = TodayTaskTestFactory.pendingReminderToday(ReminderType.VACCINATION);
        var medication = TodayTaskTestFactory.pendingReminderToday(ReminderType.MEDICATION);
        var careTask = TodayTaskTestFactory.openCareTaskToday();

        when(reminderRepository.findByOwnerUserIdAndStatusNot(
                eq(TodayTaskTestFactory.OWNER_ID), eq(ReminderStatus.CANCELLED)))
                .thenReturn(List.of(medication, vaccination));
        when(careTaskRepository.findByAssignedToAndStatusInAndDueAtBetween(
                eq(TodayTaskTestFactory.OWNER_ID), any(), any(), any()))
                .thenReturn(List.of(careTask));

        List<TodayTaskItem> tasks = todayTaskService.getTodayTasks(TodayTaskTestFactory.OWNER_ID, VN_ZONE);

        assertThat(tasks).hasSize(3);
        assertThat(tasks.get(0).getType()).isEqualTo("VACCINATION");
        assertThat(tasks.get(0).getSourceType()).isEqualTo("REMINDER");
        assertThat(tasks.get(1).getType()).isEqualTo("MEDICATION");
        assertThat(tasks.get(2).getType()).isEqualTo("CARE_TASK");
        assertThat(tasks.get(2).getSourceType()).isEqualTo("CARE_TASK");
    }

    @Test
    void getTodayTasks_noTasksToday_returnsEmptyList() {
        when(reminderRepository.findByOwnerUserIdAndStatusNot(any(), any())).thenReturn(List.of());
        when(careTaskRepository.findByAssignedToAndStatusInAndDueAtBetween(
                any(), any(), any(), any())).thenReturn(List.of());

        var tasks = todayTaskService.getTodayTasks(TodayTaskTestFactory.OWNER_ID, VN_ZONE);

        assertThat(tasks).isEmpty();
    }

    @Test
    void getTodayTasks_nonRecurringCompletedReminder_keepsCurrentBehaviorAndIsIncluded() {
        var completed = TodayTaskTestFactory.completedReminderToday(ReminderType.MEDICATION);
        when(reminderRepository.findByOwnerUserIdAndStatusNot(any(), any())).thenReturn(List.of(completed));
        when(careTaskRepository.findByAssignedToAndStatusInAndDueAtBetween(
                any(), any(), any(), any())).thenReturn(List.of());

        var tasks = todayTaskService.getTodayTasks(TodayTaskTestFactory.OWNER_ID, VN_ZONE);

        assertThat(tasks).hasSize(1);
        assertThat(tasks.get(0).getStatus()).isEqualTo(ReminderStatus.COMPLETED.name());
    }

    @Test
    void getTodayTasks_snoozedNonRecurringReminderIncludedBySnoozedUntil() {
        var snoozed = TodayTaskTestFactory.snoozedReminderToday(ReminderType.MEDICATION);
        when(reminderRepository.findByOwnerUserIdAndStatusNot(any(), any())).thenReturn(List.of(snoozed));
        when(careTaskRepository.findByAssignedToAndStatusInAndDueAtBetween(
                any(), any(), any(), any())).thenReturn(List.of());

        var tasks = todayTaskService.getTodayTasks(TodayTaskTestFactory.OWNER_ID, VN_ZONE);

        assertThat(tasks).hasSize(1);
        assertThat(tasks.get(0).getStatus()).isEqualTo("SNOOZED");
        assertThat(tasks.get(0).getDueAt()).isEqualTo(snoozed.getSnoozedUntil());
        assertThat(tasks.get(0).getSnoozedUntil()).isEqualTo(snoozed.getSnoozedUntil());
    }

    @Test
    void getTodayTasks_sortOrder_priorityHighestFirst() {
        var appointment = TodayTaskTestFactory.pendingReminderToday(ReminderType.APPOINTMENT);
        var vaccination = TodayTaskTestFactory.pendingReminderToday(ReminderType.VACCINATION);
        var medication = TodayTaskTestFactory.pendingReminderToday(ReminderType.MEDICATION);
        var careTask = TodayTaskTestFactory.openCareTaskToday();

        when(reminderRepository.findByOwnerUserIdAndStatusNot(any(), any()))
                .thenReturn(List.of(appointment, medication, vaccination));
        when(careTaskRepository.findByAssignedToAndStatusInAndDueAtBetween(
                any(), any(), any(), any())).thenReturn(List.of(careTask));

        var tasks = todayTaskService.getTodayTasks(TodayTaskTestFactory.OWNER_ID, VN_ZONE);

        assertThat(tasks).hasSize(4);
        assertThat(tasks.get(0).getPriority()).isLessThan(tasks.get(1).getPriority());
        assertThat(tasks.get(1).getPriority()).isLessThan(tasks.get(2).getPriority());
        assertThat(tasks.get(2).getPriority()).isLessThan(tasks.get(3).getPriority());
    }

    @Test
    void getTodayTasks_samePriority_sortsByDueAtAscending() {
        Instant now = Instant.now();
        var later = TodayTaskTestFactory.pendingReminderToday(
                ReminderType.MEDICATION, now.plus(3, ChronoUnit.HOURS));
        var earlier = TodayTaskTestFactory.pendingReminderToday(
                ReminderType.MEDICATION, now.plus(1, ChronoUnit.HOURS));

        when(reminderRepository.findByOwnerUserIdAndStatusNot(any(), any()))
                .thenReturn(List.of(later, earlier));
        when(careTaskRepository.findByAssignedToAndStatusInAndDueAtBetween(
                any(), any(), any(), any())).thenReturn(List.of());

        var tasks = todayTaskService.getTodayTasks(TodayTaskTestFactory.OWNER_ID, VN_ZONE);

        assertThat(tasks).extracting(TodayTaskItem::getDueAt)
                .containsExactly(earlier.getScheduledAt(), later.getScheduledAt());
    }

    @Test
    void getTodayTasks_pendingReminderWithStaleSnoozedUntil_usesScheduledAtAsDueAt() {
        Instant now = Instant.now();
        var reminder = TodayTaskTestFactory.snoozedReminderToday(
                ReminderType.MEDICATION,
                now.plus(1, ChronoUnit.HOURS),
                now.plus(4, ChronoUnit.HOURS));
        reminder.setStatus(ReminderStatus.PENDING);

        when(reminderRepository.findByOwnerUserIdAndStatusNot(any(), any()))
                .thenReturn(List.of(reminder));
        when(careTaskRepository.findByAssignedToAndStatusInAndDueAtBetween(
                any(), any(), any(), any())).thenReturn(List.of());

        var tasks = todayTaskService.getTodayTasks(TodayTaskTestFactory.OWNER_ID, VN_ZONE);

        assertThat(tasks).extracting(TodayTaskItem::getDueAt)
                .containsExactly(reminder.getScheduledAt());
    }

    @Test
    void getTodayTasks_customTimezone_usesDayBoundariesInThatZone() {
        ZoneId utcZone = ZoneId.of("UTC");
        when(reminderRepository.findByOwnerUserIdAndStatusNot(any(), any())).thenReturn(List.of());
        when(careTaskRepository.findByAssignedToAndStatusInAndDueAtBetween(
                any(), any(), any(), any())).thenReturn(List.of());

        assertThatCode(() -> todayTaskService.getTodayTasks(TodayTaskTestFactory.OWNER_ID, utcZone))
                .doesNotThrowAnyException();
    }

    @Test
    void getTodayTasks_careTasksAssignedToOthers_notIncluded() {
        when(reminderRepository.findByOwnerUserIdAndStatusNot(any(), any())).thenReturn(List.of());
        when(careTaskRepository.findByAssignedToAndStatusInAndDueAtBetween(
                eq(TodayTaskTestFactory.OWNER_ID), any(), any(), any())).thenReturn(List.of());

        var tasks = todayTaskService.getTodayTasks(TodayTaskTestFactory.OWNER_ID, VN_ZONE);

        assertThat(tasks).isEmpty();
        verify(careTaskRepository).findByAssignedToAndStatusInAndDueAtBetween(
                eq(TodayTaskTestFactory.OWNER_ID), any(), any(), any());
    }

    @Test
    void getTodayTasks_returnedItems_haveRequiredFields() {
        var reminder = TodayTaskTestFactory.pendingReminderToday(ReminderType.MEDICATION);
        when(reminderRepository.findByOwnerUserIdAndStatusNot(any(), any())).thenReturn(List.of(reminder));
        when(careTaskRepository.findByAssignedToAndStatusInAndDueAtBetween(
                any(), any(), any(), any())).thenReturn(List.of());

        var tasks = todayTaskService.getTodayTasks(TodayTaskTestFactory.OWNER_ID, VN_ZONE);

        assertThat(tasks).hasSize(1);
        var item = tasks.get(0);
        assertThat(item.getId()).isNotNull();
        assertThat(item.getType()).isNotNull();
        assertThat(item.getSourceType()).isEqualTo("REMINDER");
        assertThat(item.getTitle()).isNotNull();
        assertThat(item.getScheduledAt()).isNotNull();
        assertThat(item.getDueAt()).isNotNull();
        assertThat(item.getStatus()).isNotNull();
        assertThat(item.getPriority()).isGreaterThan(0);
    }

    @Test
    void getTodayTasks_dailyRecurringReminder_generatesTodayOccurrence() {
        LocalDate today = LocalDate.now(VN_ZONE);
        LocalTime reminderTime = LocalTime.of(8, 30);
        var reminder = TodayTaskTestFactory.pendingReminderToday(
                ReminderType.MEDICATION,
                today.minusDays(3).atTime(reminderTime).atZone(VN_ZONE).toInstant());
        reminder.setRecurrenceType(RecurrenceType.DAILY);
        reminder.setRecurrenceEndDate(today.plusDays(2).atTime(23, 59).atZone(VN_ZONE).toInstant());

        when(reminderRepository.findByOwnerUserIdAndStatusNot(any(), any())).thenReturn(List.of(reminder));
        when(careTaskRepository.findByAssignedToAndStatusInAndDueAtBetween(
                any(), any(), any(), any())).thenReturn(List.of());

        var tasks = todayTaskService.getTodayTasks(TodayTaskTestFactory.OWNER_ID, VN_ZONE);

        Instant expectedDueAt = today.atTime(reminderTime).atZone(VN_ZONE).toInstant();
        assertThat(tasks).hasSize(1);
        assertThat(tasks.get(0).getScheduledAt()).isEqualTo(expectedDueAt);
        assertThat(tasks.get(0).getDueAt()).isEqualTo(expectedDueAt);
    }

    @Test
    void getTodayTasks_weeklyRecurringReminderWithoutEndDate_generatesTodayOccurrence() {
        LocalDate today = LocalDate.now(VN_ZONE);
        LocalTime reminderTime = LocalTime.of(8, 30);
        var reminder = TodayTaskTestFactory.pendingReminderToday(
                ReminderType.MEDICATION,
                today.minusWeeks(2).atTime(reminderTime).atZone(VN_ZONE).toInstant());
        reminder.setRecurrenceType(RecurrenceType.WEEKLY);
        reminder.setRecurrenceEndDate(null);

        when(reminderRepository.findByOwnerUserIdAndStatusNot(any(), any())).thenReturn(List.of(reminder));
        when(careTaskRepository.findByAssignedToAndStatusInAndDueAtBetween(
                any(), any(), any(), any())).thenReturn(List.of());

        var tasks = todayTaskService.getTodayTasks(TodayTaskTestFactory.OWNER_ID, VN_ZONE);

        Instant expectedDueAt = today.atTime(reminderTime).atZone(VN_ZONE).toInstant();
        assertThat(tasks).hasSize(1);
        assertThat(tasks.get(0).getScheduledAt()).isEqualTo(expectedDueAt);
        assertThat(tasks.get(0).getDueAt()).isEqualTo(expectedDueAt);
        assertThat(tasks.get(0).getStatus()).isEqualTo(ReminderStatus.PENDING.name());
    }

    @Test
    void getTodayTasks_recurringTerminalStatusFromPreviousDay_resetsReturnedStatusToPending() {
        LocalDate today = LocalDate.now(VN_ZONE);
        var reminder = TodayTaskTestFactory.completedReminderToday(ReminderType.MEDICATION);
        reminder.setScheduledAt(today.minusDays(1).atTime(8, 0).atZone(VN_ZONE).toInstant());
        reminder.setRecurrenceType(RecurrenceType.DAILY);
        reminder.setRecurrenceEndDate(today.plusDays(1).atTime(23, 59).atZone(VN_ZONE).toInstant());
        reminder.setUpdatedAt(today.minusDays(1).atTime(9, 0).atZone(VN_ZONE).toInstant());

        when(reminderRepository.findByOwnerUserIdAndStatusNot(any(), any())).thenReturn(List.of(reminder));
        when(careTaskRepository.findByAssignedToAndStatusInAndDueAtBetween(
                any(), any(), any(), any())).thenReturn(List.of());

        var tasks = todayTaskService.getTodayTasks(TodayTaskTestFactory.OWNER_ID, VN_ZONE);

        assertThat(tasks).hasSize(1);
        assertThat(tasks.get(0).getStatus()).isEqualTo(ReminderStatus.PENDING.name());
    }

    @Test
    void getTodayTasks_recurringCompletedToday_keepsCompletedStatusForToday() {
        LocalDate today = LocalDate.now(VN_ZONE);
        var reminder = TodayTaskTestFactory.completedReminderToday(ReminderType.MEDICATION);
        reminder.setScheduledAt(today.minusDays(1).atTime(8, 0).atZone(VN_ZONE).toInstant());
        reminder.setRecurrenceType(RecurrenceType.DAILY);
        reminder.setRecurrenceEndDate(today.plusDays(1).atTime(23, 59).atZone(VN_ZONE).toInstant());
        reminder.setUpdatedAt(today.atTime(9, 0).atZone(VN_ZONE).toInstant());

        when(reminderRepository.findByOwnerUserIdAndStatusNot(any(), any())).thenReturn(List.of(reminder));
        when(careTaskRepository.findByAssignedToAndStatusInAndDueAtBetween(
                any(), any(), any(), any())).thenReturn(List.of());

        var tasks = todayTaskService.getTodayTasks(TodayTaskTestFactory.OWNER_ID, VN_ZONE);

        assertThat(tasks).hasSize(1);
        assertThat(tasks.get(0).getStatus()).isEqualTo(ReminderStatus.COMPLETED.name());
    }

    @Test
    void getTodayTasks_recurringSnoozedToday_keepsSnoozedStatusAndDueAtForToday() {
        LocalDate today = LocalDate.now(VN_ZONE);
        Instant scheduledAt = today.minusDays(1).atTime(8, 0).atZone(VN_ZONE).toInstant();
        Instant snoozedUntil = today.atTime(10, 0).atZone(VN_ZONE).toInstant();
        var reminder = TodayTaskTestFactory.snoozedReminderToday(
                ReminderType.MEDICATION,
                scheduledAt,
                snoozedUntil);
        reminder.setRecurrenceType(RecurrenceType.DAILY);
        reminder.setRecurrenceEndDate(today.plusDays(1).atTime(23, 59).atZone(VN_ZONE).toInstant());
        reminder.setUpdatedAt(today.atTime(9, 0).atZone(VN_ZONE).toInstant());

        when(reminderRepository.findByOwnerUserIdAndStatusNot(any(), any())).thenReturn(List.of(reminder));
        when(careTaskRepository.findByAssignedToAndStatusInAndDueAtBetween(
                any(), any(), any(), any())).thenReturn(List.of());

        var tasks = todayTaskService.getTodayTasks(TodayTaskTestFactory.OWNER_ID, VN_ZONE);

        assertThat(tasks).hasSize(1);
        assertThat(tasks.get(0).getStatus()).isEqualTo(ReminderStatus.SNOOZED.name());
        assertThat(tasks.get(0).getDueAt()).isEqualTo(snoozedUntil);
        assertThat(tasks.get(0).getSnoozedUntil()).isEqualTo(snoozedUntil);
    }
}
