package com.carebridge.backend.checklist.today;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.checklist.distribution.EnsureEligibleChecklistAssignmentsService;
import com.carebridge.backend.checklist.model.ChecklistCareContextType;
import com.carebridge.backend.checklist.model.ChecklistOrigin;
import com.carebridge.backend.checklist.model.ChecklistTargetSubject;
import com.carebridge.backend.checklist.today.dto.TodayTaskCandidate;
import com.carebridge.backend.checklist.today.model.TaskAction;
import com.carebridge.backend.checklist.today.model.TaskKind;
import com.carebridge.backend.checklist.today.model.TaskTimeBucket;
import com.carebridge.backend.checklist.today.provider.TodayTaskProvider;
import com.carebridge.backend.checklist.today.service.UnifiedTodayTaskServiceImpl;
import com.carebridge.backend.reminder.entity.ReminderType;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class UnifiedTodayTaskServiceTest {

    private static final UUID ACTOR = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID GROUP = UUID.fromString("00000000-0000-0000-0000-000000000201");
    private static final UUID CONTEXT = UUID.fromString("00000000-0000-0000-0000-000000000301");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-03T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void chk026_aggregatesEveryKindIntoDeterministicSections() {
        TodayTaskProvider checklist = provider(TaskKind.CHECKLIST, List.of(
                candidate(TaskKind.CHECKLIST, 1, "PENDING", "2026-08-02T16:59:59Z"),
                candidate(TaskKind.CHECKLIST, 2, "COMPLETED", "2026-08-03T02:00:00Z")));
        TodayTaskProvider reminder = provider(TaskKind.REMINDER, List.of(
                candidate(TaskKind.REMINDER, 3, "PENDING", "2026-08-05T02:00:00Z")));
        TodayTaskProvider careTask = provider(TaskKind.CARE_TASK, List.of(
                candidate(TaskKind.CARE_TASK, 4, "PENDING", null)));

        var result = new UnifiedTodayTaskServiceImpl(List.of(checklist, reminder, careTask), CLOCK)
                .getTodayTasks(ACTOR, LocalDate.of(2026, 8, 3), "Asia/Ho_Chi_Minh");

        assertThat(result.sections().overdue()).extracting(item -> item.taskKind())
                .containsExactly(TaskKind.CHECKLIST);
        assertThat(result.sections().today()).extracting(item -> item.status())
                .containsExactly("COMPLETED");
        assertThat(result.sections().upcoming()).extracting(item -> item.taskKind())
                .containsExactly(TaskKind.REMINDER);
        assertThat(result.sections().upcoming()).extracting(item -> item.type())
                .containsExactly(ReminderType.MEDICATION);
        assertThat(result.sections().unscheduled()).extracting(item -> item.taskKind())
                .containsExactly(TaskKind.CARE_TASK);
        assertThat(result.counts()).extracting("overdue", "today", "upcoming", "unscheduled")
                .containsExactly(1, 1, 1, 1);
    }

    @Test
    void chk027_usesEffectiveZoneAndPlacesEveryTaskInExactlyOneBucket() {
        TodayTaskProvider provider = provider(TaskKind.CHECKLIST, List.of(
                candidate(TaskKind.CHECKLIST, 1, "PENDING", "2026-08-02T16:59:59Z"),
                candidate(TaskKind.CHECKLIST, 2, "PENDING", "2026-08-02T17:00:00Z"),
                candidate(TaskKind.CHECKLIST, 3, "PENDING", "2026-08-03T17:00:00Z"),
                candidate(TaskKind.CHECKLIST, 4, "CANCELLED", "2026-08-03T02:00:00Z"),
                candidate(TaskKind.CHECKLIST, 5, "COMPLETED", "2026-08-05T02:00:00Z")));

        var result = new UnifiedTodayTaskServiceImpl(List.of(provider), CLOCK)
                .getTodayTasks(ACTOR, LocalDate.of(2026, 8, 3), "Asia/Ho_Chi_Minh");

        assertThat(result.zoneId()).isEqualTo("Asia/Ho_Chi_Minh");
        assertThat(result.sections().overdue()).allMatch(item -> item.timeBucket() == TaskTimeBucket.OVERDUE);
        assertThat(result.sections().today()).allMatch(item -> item.timeBucket() == TaskTimeBucket.TODAY);
        assertThat(result.sections().upcoming()).allMatch(item -> item.timeBucket() == TaskTimeBucket.UPCOMING);
        assertThat(result.counts().overdue() + result.counts().today()
                + result.counts().upcoming() + result.counts().unscheduled()).isEqualTo(3);
    }

    @Test
    void chk027_invalidZoneFallsBackAndNullDateUsesEffectiveLocalDate() {
        var result = new UnifiedTodayTaskServiceImpl(List.of(), CLOCK)
                .getTodayTasks(ACTOR, null, "Not/A_Real_Zone");

        assertThat(result.zoneId()).isEqualTo("Asia/Ho_Chi_Minh");
        assertThat(result.asOf()).isEqualTo(CLOCK.instant());
        assertThat(result.horizonDays()).isEqualTo(7);
    }

    @Test
    void chk042_completedUnscheduledTaskRemainsVisibleOnCompletionDay() {
        TodayTaskProvider provider = provider(TaskKind.CHECKLIST, List.of(
                candidateWithTerminalAt(
                        TaskKind.CHECKLIST,
                        6,
                        "COMPLETED",
                        null,
                        "2026-08-03T02:00:00Z")));

        var result = new UnifiedTodayTaskServiceImpl(List.of(provider), CLOCK)
                .getTodayTasks(ACTOR, LocalDate.of(2026, 8, 3), "Asia/Ho_Chi_Minh");

        assertThat(result.sections().today()).extracting(item -> item.status())
                .containsExactly("COMPLETED");
        assertThat(result.sections().unscheduled()).isEmpty();
    }

    @Test
    void requestMaterializationAndResponseShareOneCorrelationId() {
        EnsureEligibleChecklistAssignmentsService ensure = mock(EnsureEligibleChecklistAssignmentsService.class);
        var result = new UnifiedTodayTaskServiceImpl(List.of(), null, ensure, CLOCK)
                .getTodayTasks(ACTOR, LocalDate.of(2026, 8, 3), "Asia/Ho_Chi_Minh");

        ArgumentCaptor<UUID> correlation = ArgumentCaptor.forClass(UUID.class);
        verify(ensure).ensureEligibleAssignments(
                org.mockito.ArgumentMatchers.eq(ACTOR),
                org.mockito.ArgumentMatchers.eq(LocalDate.of(2026, 8, 3)),
                org.mockito.ArgumentMatchers.eq(java.time.ZoneId.of("Asia/Ho_Chi_Minh")),
                correlation.capture());
        assertThat(result.correlationId()).isEqualTo(correlation.getValue());
    }

    private static TodayTaskProvider provider(TaskKind kind, List<TodayTaskCandidate> tasks) {
        TodayTaskProvider provider = mock(TodayTaskProvider.class);
        when(provider.taskKind()).thenReturn(kind);
        when(provider.findAuthorizedTasks(ACTOR)).thenReturn(tasks);
        return provider;
    }

    private static TodayTaskCandidate candidate(TaskKind kind, int suffix, String status, String dueAt) {
        return new TodayTaskCandidate(kind,
                UUID.fromString("00000000-0000-0000-0000-0000000004%02d".formatted(suffix)),
                UUID.fromString("00000000-0000-0000-0000-000000000501"), null, GROUP,
                ChecklistCareContextType.JOURNEY, CONTEXT, "Task " + suffix,
                ChecklistTargetSubject.MOTHER, ChecklistOrigin.SYSTEM_TEMPLATE, status,
                Set.of(TaskAction.COMPLETE), dueAt == null ? null : Instant.parse(dueAt), null,
                kind == TaskKind.REMINDER ? ReminderType.MEDICATION : null);
    }

    private static TodayTaskCandidate candidateWithTerminalAt(
            TaskKind kind,
            int suffix,
            String status,
            String dueAt,
            String terminalAt) {
        return new TodayTaskCandidate(
                kind,
                UUID.fromString("00000000-0000-0000-0000-0000000004%02d".formatted(suffix)),
                UUID.fromString("00000000-0000-0000-0000-000000000501"),
                null,
                GROUP,
                ChecklistCareContextType.JOURNEY,
                CONTEXT,
                "Task " + suffix,
                ChecklistTargetSubject.MOTHER,
                ChecklistOrigin.SYSTEM_TEMPLATE,
                status,
                Set.of(),
                dueAt == null ? null : Instant.parse(dueAt),
                terminalAt == null ? null : Instant.parse(terminalAt));
    }
}
