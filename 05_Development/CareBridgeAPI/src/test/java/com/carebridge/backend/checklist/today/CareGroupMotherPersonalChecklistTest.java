package com.carebridge.backend.checklist.today;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.carebridge.backend.checklist.model.ChecklistCareContextType;
import com.carebridge.backend.checklist.model.ChecklistOrigin;
import com.carebridge.backend.checklist.repository.ChecklistInstanceRepository;
import com.carebridge.backend.checklist.repository.ChecklistTaskInstanceRepository;
import com.carebridge.backend.checklist.today.dto.CurrentChecklistResponse;
import com.carebridge.backend.checklist.today.dto.CurrentChecklistTaskResponse;
import com.carebridge.backend.checklist.today.dto.TodayTaskCounts;
import com.carebridge.backend.checklist.today.dto.TodayTaskItemResponse;
import com.carebridge.backend.checklist.today.dto.TodayTaskSections;
import com.carebridge.backend.checklist.today.dto.TodayTasksResponse;
import com.carebridge.backend.checklist.today.model.TaskAction;
import com.carebridge.backend.checklist.today.policy.CareGroupChecklistScopeResolver;
import com.carebridge.backend.checklist.today.policy.CareGroupChecklistScopeResolver.CareGroupChecklistScope;
import com.carebridge.backend.checklist.today.policy.CareGroupChecklistScopeResolver.LinkedContext;
import com.carebridge.backend.checklist.today.service.CareGroupChecklistService;
import com.carebridge.backend.checklist.today.service.UnifiedTodayTaskService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CareGroupMotherPersonalChecklistTest {
    private static final UUID MOTHER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID FAMILY_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID GROUP_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID JOURNEY_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    private UnifiedTodayTaskService unifiedTodayTaskService;
    private CareGroupChecklistScopeResolver scopeResolver;
    private ChecklistInstanceRepository instanceRepository;
    private ChecklistTaskInstanceRepository taskRepository;
    private CareGroupChecklistService service;

    private CareGroupChecklistScope scope;

    @BeforeEach
    void setUp() {
        unifiedTodayTaskService = mock(UnifiedTodayTaskService.class);
        scopeResolver = mock(CareGroupChecklistScopeResolver.class);
        instanceRepository = mock(ChecklistInstanceRepository.class);
        taskRepository = mock(ChecklistTaskInstanceRepository.class);

        service = new CareGroupChecklistService(
                unifiedTodayTaskService,
                scopeResolver,
                null,
                null,
                instanceRepository,
                taskRepository,
                false);

        scope = new CareGroupChecklistScope(
                GROUP_ID,
                "Family Care Group",
                MOTHER_ID,
                List.of(new LinkedContext(ChecklistCareContextType.JOURNEY, JOURNEY_ID)));
    }

    @Test
    void projectsMotherSystemAndPersonalTasksWithSyncedStatusAndNoActionsForFamily() {
        when(scopeResolver.resolveView(FAMILY_ID, GROUP_ID)).thenReturn(scope);

        UUID systemTaskId = UUID.randomUUID();
        UUID personalTaskId = UUID.randomUUID();

        TodayTaskItemResponse systemTask = new TodayTaskItemResponse(
                com.carebridge.backend.checklist.today.model.TaskKind.CHECKLIST,
                systemTaskId, UUID.randomUUID(), UUID.randomUUID(),
                null, ChecklistCareContextType.JOURNEY, JOURNEY_ID,
                "Family Care Group", "Thai kỳ 12 tuần",
                "Uống vitamin và axit folic",
                com.carebridge.backend.checklist.model.ChecklistTargetSubject.MOTHER,
                ChecklistOrigin.SYSTEM_TEMPLATE,
                "COMPLETED",
                com.carebridge.backend.checklist.today.model.TaskTimeBucket.TODAY,
                java.util.Set.of(TaskAction.REOPEN),
                Instant.now(), null, "Mô tả bổ sung vitamin", null, null, null);

        TodayTaskItemResponse personalTask = new TodayTaskItemResponse(
                com.carebridge.backend.checklist.today.model.TaskKind.CHECKLIST,
                personalTaskId, UUID.randomUUID(), null,
                null, ChecklistCareContextType.JOURNEY, JOURNEY_ID,
                "Family Care Group", null,
                "Mua sữa bầu dinh dưỡng",
                com.carebridge.backend.checklist.model.ChecklistTargetSubject.MOTHER,
                ChecklistOrigin.USER_CREATED,
                "PENDING",
                com.carebridge.backend.checklist.today.model.TaskTimeBucket.TODAY,
                java.util.Set.of(TaskAction.COMPLETE),
                Instant.now(), null, "Hộp 800g tại siêu thị", null, null, null);

        TodayTasksResponse motherResponse = new TodayTasksResponse(
                Instant.now(), "Asia/Ho_Chi_Minh", 7,
                new TodayTaskSections(List.of(), List.of(systemTask, personalTask), List.of(), List.of()),
                new TodayTaskCounts(0, 2, 0, 0), UUID.randomUUID(), null);

        when(unifiedTodayTaskService.getTodayTasks(eq(MOTHER_ID), any(), any(), any(), eq(false)))
                .thenReturn(motherResponse);
        when(instanceRepository.findByRecipientUserIdAndHistoricalAtIsNull(FAMILY_ID))
                .thenReturn(List.of());

        CurrentChecklistResponse response = service.getCurrentTasks(
                FAMILY_ID, GROUP_ID, LocalDate.now(), "Asia/Ho_Chi_Minh");

        assertThat(response).isNotNull();
        List<CurrentChecklistTaskResponse> todayTasks = response.sections().today();
        assertThat(todayTasks).hasSize(2);

        CurrentChecklistTaskResponse projectedSystem = todayTasks.stream()
                .filter(t -> t.taskId().equals(systemTaskId))
                .findFirst().orElseThrow();
        assertThat(projectedSystem.title()).isEqualTo("Uống vitamin và axit folic");
        assertThat(projectedSystem.origin()).isEqualTo(ChecklistOrigin.SYSTEM_TEMPLATE);
        assertThat(projectedSystem.status()).isEqualTo("COMPLETED");
        // Family cannot tick or modify mother's system task
        assertThat(projectedSystem.allowedActions()).isEmpty();

        CurrentChecklistTaskResponse projectedPersonal = todayTasks.stream()
                .filter(t -> t.taskId().equals(personalTaskId))
                .findFirst().orElseThrow();
        assertThat(projectedPersonal.title()).isEqualTo("Mua sữa bầu dinh dưỡng");
        assertThat(projectedPersonal.origin()).isEqualTo(ChecklistOrigin.USER_CREATED);
        assertThat(projectedPersonal.status()).isEqualTo("PENDING");
        // Family cannot tick or modify mother's personal task
        assertThat(projectedPersonal.allowedActions()).isEmpty();
    }

    @Test
    void filtersOutMotherTasksOutsideCareGroupScope() {
        when(scopeResolver.resolveView(FAMILY_ID, GROUP_ID)).thenReturn(scope);

        UUID otherJourneyId = UUID.randomUUID();
        UUID otherTaskId = UUID.randomUUID();

        TodayTaskItemResponse otherTask = new TodayTaskItemResponse(
                com.carebridge.backend.checklist.today.model.TaskKind.CHECKLIST,
                otherTaskId, UUID.randomUUID(), UUID.randomUUID(),
                null, ChecklistCareContextType.JOURNEY, otherJourneyId,
                "Other Care Group", "Khác",
                "Việc không thuộc nhóm này",
                com.carebridge.backend.checklist.model.ChecklistTargetSubject.MOTHER,
                ChecklistOrigin.SYSTEM_TEMPLATE,
                "PENDING",
                com.carebridge.backend.checklist.today.model.TaskTimeBucket.TODAY,
                java.util.Set.of(TaskAction.COMPLETE),
                Instant.now(), null, "Mô tả", null, null, null);

        TodayTasksResponse motherResponse = new TodayTasksResponse(
                Instant.now(), "Asia/Ho_Chi_Minh", 7,
                new TodayTaskSections(List.of(), List.of(otherTask), List.of(), List.of()),
                new TodayTaskCounts(0, 1, 0, 0), UUID.randomUUID(), null);

        when(unifiedTodayTaskService.getTodayTasks(eq(MOTHER_ID), any(), any(), any(), eq(false)))
                .thenReturn(motherResponse);
        when(instanceRepository.findByRecipientUserIdAndHistoricalAtIsNull(FAMILY_ID))
                .thenReturn(List.of());

        CurrentChecklistResponse response = service.getCurrentTasks(
                FAMILY_ID, GROUP_ID, LocalDate.now(), "Asia/Ho_Chi_Minh");

        assertThat(response).isNotNull();
        assertThat(response.sections().today()).isEmpty();
    }
}
