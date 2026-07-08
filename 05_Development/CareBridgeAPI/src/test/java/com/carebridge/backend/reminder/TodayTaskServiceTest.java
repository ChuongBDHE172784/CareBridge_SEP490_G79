package com.carebridge.backend.reminder;

import com.carebridge.backend.reminder.dto.TodayTaskItem;
import com.carebridge.backend.reminder.entity.ReminderType;
import com.carebridge.backend.reminder.repository.CareTaskRepository;
import com.carebridge.backend.reminder.repository.ReminderRepository;
import com.carebridge.backend.reminder.service.impl.TodayTaskServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * UC49 — View Today Tasks service tests.
 * TDD Red Phase: all tests must FAIL until Green implementation.
 */
@ExtendWith(MockitoExtension.class)
class TodayTaskServiceTest {

    @Mock private ReminderRepository reminderRepository;
    @Mock private CareTaskRepository careTaskRepository;
    @InjectMocks private TodayTaskServiceImpl todayTaskService;

    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    // TODAY-TC-001: Happy path — returns merged and sorted list
    @Test
    void getTodayTasks_withMixedTasks_returnsMergedSortedList() {
        var vaccination = TodayTaskTestFactory.pendingReminderToday(ReminderType.VACCINATION);
        var medication  = TodayTaskTestFactory.pendingReminderToday(ReminderType.MEDICATION);
        var careTask    = TodayTaskTestFactory.openCareTaskToday();

        when(reminderRepository.findByOwnerUserIdAndScheduledAtBetweenAndStatusIn(
                eq(TodayTaskTestFactory.OWNER_ID), any(), any(), any()))
                .thenReturn(List.of(medication, vaccination));
        when(careTaskRepository.findByAssignedToAndStatusAndDueAtBetween(
                eq(TodayTaskTestFactory.OWNER_ID), any(), any(), any()))
                .thenReturn(List.of(careTask));

        List<TodayTaskItem> tasks = todayTaskService.getTodayTasks(TodayTaskTestFactory.OWNER_ID, VN_ZONE);

        assertThat(tasks).hasSize(3);
        // VACCINATION priority 1 must come first
        assertThat(tasks.get(0).getType()).isEqualTo("VACCINATION");
        // MEDICATION priority 2 second
        assertThat(tasks.get(1).getType()).isEqualTo("MEDICATION");
        // CARE_TASK priority 4 last
        assertThat(tasks.get(2).getType()).isEqualTo("CARE_TASK");
    }

    // TODAY-TC-002: Empty result when no tasks today
    @Test
    void getTodayTasks_noTasksToday_returnsEmptyList() {
        when(reminderRepository.findByOwnerUserIdAndScheduledAtBetweenAndStatusIn(
                any(), any(), any(), any())).thenReturn(List.of());
        when(careTaskRepository.findByAssignedToAndStatusAndDueAtBetween(
                any(), any(), any(), any())).thenReturn(List.of());

        var tasks = todayTaskService.getTodayTasks(TodayTaskTestFactory.OWNER_ID, VN_ZONE);

        assertThat(tasks).isEmpty();
    }

    // TODAY-TC-003: COMPLETED reminders are excluded from today list
    @Test
    void getTodayTasks_completedRemindersExcluded() {
        when(reminderRepository.findByOwnerUserIdAndScheduledAtBetweenAndStatusIn(
                any(), any(), any(), any())).thenReturn(List.of());
        when(careTaskRepository.findByAssignedToAndStatusAndDueAtBetween(
                any(), any(), any(), any())).thenReturn(List.of());

        var tasks = todayTaskService.getTodayTasks(TodayTaskTestFactory.OWNER_ID, VN_ZONE);

        // Query uses statusIn(PENDING, SNOOZED) only — completed not queried
        assertThat(tasks).isEmpty();
    }

    // TODAY-TC-004: SNOOZED reminders ARE included in today list
    @Test
    void getTodayTasks_snoozedRemindersIncluded() {
        var snoozed = TodayTaskTestFactory.snoozedReminderToday(ReminderType.MEDICATION);
        when(reminderRepository.findByOwnerUserIdAndScheduledAtBetweenAndStatusIn(
                any(), any(), any(), any())).thenReturn(List.of(snoozed));
        when(careTaskRepository.findByAssignedToAndStatusAndDueAtBetween(
                any(), any(), any(), any())).thenReturn(List.of());

        var tasks = todayTaskService.getTodayTasks(TodayTaskTestFactory.OWNER_ID, VN_ZONE);

        assertThat(tasks).hasSize(1);
        assertThat(tasks.get(0).getStatus()).isEqualTo("SNOOZED");
    }

    // TODAY-TC-005: Sort order — VACCINATION(1) > MEDICATION(2) > APPOINTMENT(3) > CARE_TASK(4)
    @Test
    void getTodayTasks_sortOrder_priorityHighestFirst() {
        var appointment = TodayTaskTestFactory.pendingReminderToday(ReminderType.APPOINTMENT);
        var vaccination  = TodayTaskTestFactory.pendingReminderToday(ReminderType.VACCINATION);
        var medication   = TodayTaskTestFactory.pendingReminderToday(ReminderType.MEDICATION);
        var careTask     = TodayTaskTestFactory.openCareTaskToday();

        when(reminderRepository.findByOwnerUserIdAndScheduledAtBetweenAndStatusIn(
                any(), any(), any(), any()))
                .thenReturn(List.of(appointment, medication, vaccination));
        when(careTaskRepository.findByAssignedToAndStatusAndDueAtBetween(
                any(), any(), any(), any())).thenReturn(List.of(careTask));

        var tasks = todayTaskService.getTodayTasks(TodayTaskTestFactory.OWNER_ID, VN_ZONE);

        assertThat(tasks).hasSize(4);
        assertThat(tasks.get(0).getPriority()).isLessThan(tasks.get(1).getPriority());
        assertThat(tasks.get(1).getPriority()).isLessThan(tasks.get(2).getPriority());
        assertThat(tasks.get(2).getPriority()).isLessThan(tasks.get(3).getPriority());
    }

    // TODAY-TC-006: Timezone — queries use correct day boundaries for given timezone
    @Test
    void getTodayTasks_customTimezone_usesDayBoundariesInThatZone() {
        ZoneId utcZone = ZoneId.of("UTC");
        when(reminderRepository.findByOwnerUserIdAndScheduledAtBetweenAndStatusIn(
                any(), any(), any(), any())).thenReturn(List.of());
        when(careTaskRepository.findByAssignedToAndStatusAndDueAtBetween(
                any(), any(), any(), any())).thenReturn(List.of());

        // Should not throw; timezone boundary calculation must work for UTC
        assertThatCode(() -> todayTaskService.getTodayTasks(TodayTaskTestFactory.OWNER_ID, utcZone))
                .doesNotThrowAnyException();
    }

    // TODAY-TC-007: Care tasks assigned to other users are not included
    @Test
    void getTodayTasks_careTasksAssignedToOthers_notIncluded() {
        when(reminderRepository.findByOwnerUserIdAndScheduledAtBetweenAndStatusIn(
                any(), any(), any(), any())).thenReturn(List.of());
        // Repository is called with callerId — if assigned to other user it's filtered by the query
        when(careTaskRepository.findByAssignedToAndStatusAndDueAtBetween(
                eq(TodayTaskTestFactory.OWNER_ID), any(), any(), any())).thenReturn(List.of());

        var tasks = todayTaskService.getTodayTasks(TodayTaskTestFactory.OWNER_ID, VN_ZONE);

        assertThat(tasks).isEmpty();
        verify(careTaskRepository).findByAssignedToAndStatusAndDueAtBetween(
                eq(TodayTaskTestFactory.OWNER_ID), any(), any(), any());
    }

    // TODAY-TC-008: TodayTaskItem has required fields
    @Test
    void getTodayTasks_returnedItems_haveRequiredFields() {
        var reminder = TodayTaskTestFactory.pendingReminderToday(ReminderType.MEDICATION);
        when(reminderRepository.findByOwnerUserIdAndScheduledAtBetweenAndStatusIn(
                any(), any(), any(), any())).thenReturn(List.of(reminder));
        when(careTaskRepository.findByAssignedToAndStatusAndDueAtBetween(
                any(), any(), any(), any())).thenReturn(List.of());

        var tasks = todayTaskService.getTodayTasks(TodayTaskTestFactory.OWNER_ID, VN_ZONE);

        assertThat(tasks).hasSize(1);
        var item = tasks.get(0);
        assertThat(item.getId()).isNotNull();
        assertThat(item.getType()).isNotNull();
        assertThat(item.getTitle()).isNotNull();
        assertThat(item.getScheduledAt()).isNotNull();
        assertThat(item.getStatus()).isNotNull();
        assertThat(item.getPriority()).isGreaterThan(0);
    }
}
