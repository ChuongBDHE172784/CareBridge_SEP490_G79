package com.carebridge.backend.checklist.today;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.carebridge.backend.checklist.entity.ChecklistInstance;
import com.carebridge.backend.checklist.entity.ChecklistTaskInstance;
import com.carebridge.backend.checklist.model.ChecklistCareContextType;
import com.carebridge.backend.checklist.model.ChecklistInstanceStatus;
import com.carebridge.backend.checklist.model.ChecklistOrigin;
import com.carebridge.backend.checklist.model.ChecklistRecipientRole;
import com.carebridge.backend.checklist.model.ChecklistTaskStatus;
import com.carebridge.backend.checklist.repository.ChecklistInstanceRepository;
import com.carebridge.backend.checklist.repository.ChecklistTaskInstanceRepository;
import com.carebridge.backend.checklist.today.dto.CurrentChecklistResponse;
import com.carebridge.backend.checklist.today.dto.CurrentChecklistTaskResponse;
import com.carebridge.backend.checklist.today.dto.TodayTaskCounts;
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
    void projectsMotherPersonalTasksWhenFamilyHasChecklistViewOnly() {
        when(scopeResolver.resolveView(FAMILY_ID, GROUP_ID)).thenReturn(scope);
        when(scopeResolver.resolveComplete(FAMILY_ID, GROUP_ID)).thenReturn(null); // view only

        TodayTasksResponse emptyFamilyResponse = new TodayTasksResponse(
                Instant.now(), "Asia/Ho_Chi_Minh", 7,
                new TodayTaskSections(List.of(), List.of(), List.of(), List.of()),
                new TodayTaskCounts(0, 0, 0, 0), UUID.randomUUID(), null);
        when(unifiedTodayTaskService.getTodayTasks(eq(FAMILY_ID), any(), any(), any(), eq(false)))
                .thenReturn(emptyFamilyResponse);

        // Mother has a user-created task for this journey
        UUID motherInstanceId = UUID.randomUUID();
        ChecklistInstance motherInstance = ChecklistInstance.builder()
                .id(motherInstanceId)
                .recipientUserId(MOTHER_ID)
                .contextOwnerUserId(MOTHER_ID)
                .recipientRole(ChecklistRecipientRole.MOTHER)
                .origin(ChecklistOrigin.USER_CREATED)
                .careContextType(ChecklistCareContextType.JOURNEY)
                .careContextId(JOURNEY_ID)
                .status(ChecklistInstanceStatus.IN_PROGRESS)
                .build();

        UUID motherTaskId = UUID.randomUUID();
        ChecklistTaskInstance motherTask = ChecklistTaskInstance.builder()
                .id(motherTaskId)
                .checklistInstanceId(motherInstanceId)
                .titleSnapshot("Mua sữa bầu dinh dưỡng")
                .descriptionSnapshot("Hộp 800g tại siêu thị")
                .status(ChecklistTaskStatus.PENDING)
                .build();

        when(instanceRepository.findByRecipientUserIdAndHistoricalAtIsNull(MOTHER_ID))
                .thenReturn(List.of(motherInstance));
        when(instanceRepository.findByRecipientUserIdAndHistoricalAtIsNull(FAMILY_ID))
                .thenReturn(List.of());
        when(taskRepository.findAllByChecklistInstanceIds(List.of(motherInstanceId)))
                .thenReturn(List.of(motherTask));

        CurrentChecklistResponse response = service.getCurrentTasks(
                FAMILY_ID, GROUP_ID, LocalDate.now(), "Asia/Ho_Chi_Minh");

        assertThat(response).isNotNull();
        List<CurrentChecklistTaskResponse> unscheduled = response.sections().unscheduled();
        assertThat(unscheduled).hasSize(1);

        CurrentChecklistTaskResponse projected = unscheduled.get(0);
        assertThat(projected.taskId()).isEqualTo(motherTaskId);
        assertThat(projected.title()).isEqualTo("Mua sữa bầu dinh dưỡng");
        assertThat(projected.origin()).isEqualTo(ChecklistOrigin.USER_CREATED);
        // View-only family member should have no actions on mother's task
        assertThat(projected.allowedActions()).isEmpty();
    }

    @Test
    void allowsCompletionOnMotherPersonalTaskWhenFamilyHasChecklistComplete() {
        when(scopeResolver.resolveView(FAMILY_ID, GROUP_ID)).thenReturn(scope);
        when(scopeResolver.resolveComplete(FAMILY_ID, GROUP_ID)).thenReturn(scope); // has complete grant

        TodayTasksResponse emptyFamilyResponse = new TodayTasksResponse(
                Instant.now(), "Asia/Ho_Chi_Minh", 7,
                new TodayTaskSections(List.of(), List.of(), List.of(), List.of()),
                new TodayTaskCounts(0, 0, 0, 0), UUID.randomUUID(), null);
        when(unifiedTodayTaskService.getTodayTasks(eq(FAMILY_ID), any(), any(), any(), eq(false)))
                .thenReturn(emptyFamilyResponse);

        UUID motherInstanceId = UUID.randomUUID();
        ChecklistInstance motherInstance = ChecklistInstance.builder()
                .id(motherInstanceId)
                .recipientUserId(MOTHER_ID)
                .contextOwnerUserId(MOTHER_ID)
                .recipientRole(ChecklistRecipientRole.MOTHER)
                .origin(ChecklistOrigin.USER_CREATED)
                .careContextType(ChecklistCareContextType.JOURNEY)
                .careContextId(JOURNEY_ID)
                .status(ChecklistInstanceStatus.IN_PROGRESS)
                .build();

        UUID motherTaskId = UUID.randomUUID();
        ChecklistTaskInstance motherTask = ChecklistTaskInstance.builder()
                .id(motherTaskId)
                .checklistInstanceId(motherInstanceId)
                .titleSnapshot("Chuẩn bị giỏ đồ đi sinh")
                .status(ChecklistTaskStatus.PENDING)
                .build();

        when(instanceRepository.findByRecipientUserIdAndHistoricalAtIsNull(MOTHER_ID))
                .thenReturn(List.of(motherInstance));
        when(instanceRepository.findByRecipientUserIdAndHistoricalAtIsNull(FAMILY_ID))
                .thenReturn(List.of());
        when(taskRepository.findAllByChecklistInstanceIds(List.of(motherInstanceId)))
                .thenReturn(List.of(motherTask));

        CurrentChecklistResponse response = service.getCurrentTasks(
                FAMILY_ID, GROUP_ID, LocalDate.now(), "Asia/Ho_Chi_Minh");

        assertThat(response).isNotNull();
        List<CurrentChecklistTaskResponse> unscheduled = response.sections().unscheduled();
        assertThat(unscheduled).hasSize(1);

        CurrentChecklistTaskResponse projected = unscheduled.get(0);
        assertThat(projected.taskId()).isEqualTo(motherTaskId);
        // Family member with complete permission can complete mother's pending personal task
        assertThat(projected.allowedActions()).contains(TaskAction.COMPLETE);
    }
}
