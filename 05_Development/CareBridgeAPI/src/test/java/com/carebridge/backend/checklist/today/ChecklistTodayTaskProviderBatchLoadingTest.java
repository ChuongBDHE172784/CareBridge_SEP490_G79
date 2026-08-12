package com.carebridge.backend.checklist.today;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.checklist.entity.ChecklistInstance;
import com.carebridge.backend.checklist.entity.ChecklistTaskInstance;
import com.carebridge.backend.checklist.model.ChecklistCareContextType;
import com.carebridge.backend.checklist.model.ChecklistInstanceStatus;
import com.carebridge.backend.checklist.model.ChecklistOrigin;
import com.carebridge.backend.checklist.model.ChecklistRecipientRole;
import com.carebridge.backend.checklist.model.ChecklistTargetSubject;
import com.carebridge.backend.checklist.model.ChecklistTaskStatus;
import com.carebridge.backend.checklist.repository.ChecklistInstanceRepository;
import com.carebridge.backend.checklist.repository.ChecklistTaskInstanceRepository;
import com.carebridge.backend.checklist.today.policy.UnifiedTaskAccessPolicy;
import com.carebridge.backend.checklist.today.model.TaskAction;
import com.carebridge.backend.checklist.today.provider.ChecklistTodayTaskProvider;
import com.carebridge.backend.content.entity.ChecklistTemplate;
import com.carebridge.backend.content.entity.ChecklistTemplateStatus;
import com.carebridge.backend.content.repository.ChecklistTemplateRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ChecklistTodayTaskProviderBatchLoadingTest {
    private static final UUID ACTOR_ID = uuid(101);
    private static final UUID FIRST_INSTANCE_ID = uuid(201);
    private static final UUID SECOND_INSTANCE_ID = uuid(202);
    private static final UUID FIRST_TASK_ID = uuid(301);
    private static final UUID SECOND_TASK_ID = uuid(302);

    @Test
    void loadsChildrenForAllAuthorizedParentsWithOneRepositoryCall() {
        ChecklistInstance first = instance(FIRST_INSTANCE_ID, uuid(401));
        ChecklistInstance second = instance(SECOND_INSTANCE_ID, uuid(402));
        ChecklistTaskInstance firstTask = task(FIRST_TASK_ID, FIRST_INSTANCE_ID, 1);
        ChecklistTaskInstance secondTask = task(SECOND_TASK_ID, SECOND_INSTANCE_ID, 1);
        ChecklistInstanceRepository instances = mock(ChecklistInstanceRepository.class);
        ChecklistTaskInstanceRepository tasks = mock(ChecklistTaskInstanceRepository.class);
        UnifiedTaskAccessPolicy access = mock(UnifiedTaskAccessPolicy.class);
        when(instances.findByRecipientUserIdAndHistoricalAtIsNull(ACTOR_ID)).thenReturn(List.of(first, second));
        when(access.canView(first, ACTOR_ID)).thenReturn(true);
        when(access.canView(second, ACTOR_ID)).thenReturn(true);
        when(access.canComplete(first, ACTOR_ID)).thenReturn(true);
        when(access.canComplete(second, ACTOR_ID)).thenReturn(true);
        when(tasks.findAllByChecklistInstanceIds(List.of(FIRST_INSTANCE_ID, SECOND_INSTANCE_ID)))
                .thenReturn(List.of(firstTask, secondTask));

        var candidates = new ChecklistTodayTaskProvider(instances, tasks, access)
                .findAuthorizedTasks(ACTOR_ID);

        verify(tasks).findAllByChecklistInstanceIds(List.of(FIRST_INSTANCE_ID, SECOND_INSTANCE_ID));
        verify(tasks, never()).findByChecklistInstanceIdOrderByDisplayOrder(any(UUID.class));
        assertThat(candidates)
                .extracting(candidate -> candidate.taskId())
                .containsExactly(FIRST_TASK_ID, SECOND_TASK_ID);
        assertThat(candidates)
                .extracting(candidate -> candidate.instanceId())
                .containsExactly(FIRST_INSTANCE_ID, SECOND_INSTANCE_ID);
        assertThat(candidates)
                .extracting(candidate -> candidate.careGroupId())
                .containsOnlyNulls();
    }

    @Test
    void completedChecklistAdvertisesReopenAndCancelledChildrenAreHidden() {
        ChecklistInstance instance = instance(FIRST_INSTANCE_ID, uuid(401));
        ChecklistTaskInstance completed = task(FIRST_TASK_ID, FIRST_INSTANCE_ID, 1);
        completed.setStatus(ChecklistTaskStatus.COMPLETED);
        completed.setCompletedAt(java.time.Instant.parse("2026-08-03T08:00:00Z"));
        ChecklistTaskInstance cancelled = task(SECOND_TASK_ID, FIRST_INSTANCE_ID, 2);
        cancelled.setStatus(ChecklistTaskStatus.CANCELLED);
        ChecklistInstanceRepository instances = mock(ChecklistInstanceRepository.class);
        ChecklistTaskInstanceRepository tasks = mock(ChecklistTaskInstanceRepository.class);
        UnifiedTaskAccessPolicy access = mock(UnifiedTaskAccessPolicy.class);
        when(instances.findByRecipientUserIdAndHistoricalAtIsNull(ACTOR_ID)).thenReturn(List.of(instance));
        when(access.canView(instance, ACTOR_ID)).thenReturn(true);
        when(access.canComplete(instance, ACTOR_ID)).thenReturn(true);
        when(tasks.findAllByChecklistInstanceIds(List.of(FIRST_INSTANCE_ID)))
                .thenReturn(List.of(completed, cancelled));

        var candidates = new ChecklistTodayTaskProvider(instances, tasks, access)
                .findAuthorizedTasks(ACTOR_ID);

        assertThat(candidates).hasSize(1);
        assertThat(candidates.getFirst().taskId()).isEqualTo(FIRST_TASK_ID);
        assertThat(candidates.getFirst().allowedActions())
                .containsExactly(TaskAction.REOPEN);
    }

    @Test
    void archivedTemplateInstanceIsExcludedBeforeLoadingTasks() {
        ChecklistInstance instance = instance(FIRST_INSTANCE_ID, uuid(401));
        ChecklistInstanceRepository instances = mock(ChecklistInstanceRepository.class);
        ChecklistTaskInstanceRepository tasks = mock(ChecklistTaskInstanceRepository.class);
        UnifiedTaskAccessPolicy access = mock(UnifiedTaskAccessPolicy.class);
        ChecklistTemplateRepository templates = mock(ChecklistTemplateRepository.class);
        ChecklistTemplate archived = ChecklistTemplate.builder()
                .templateVersionId(instance.getTemplateVersionId())
                .status(ChecklistTemplateStatus.ARCHIVED)
                .build();
        when(instances.findByRecipientUserIdAndHistoricalAtIsNull(ACTOR_ID)).thenReturn(List.of(instance));
        when(templates.findAllByTemplateVersionIdIn(List.of(instance.getTemplateVersionId())))
                .thenReturn(List.of(archived));

        var candidates = new ChecklistTodayTaskProvider(instances, tasks, access, null, templates)
                .findAuthorizedTasks(ACTOR_ID);

        assertThat(candidates).isEmpty();
        verify(tasks, never()).findAllByChecklistInstanceIds(any());
        verify(access, never()).canView(instance, ACTOR_ID);
    }

    @Test
    void nonActionableCatchUpRowIsNotProjectedIntoTodayEvenIfHistoricalMarkerIsMissing() {
        ChecklistInstance instance = instance(FIRST_INSTANCE_ID, uuid(401));
        instance.setWasActionable(Boolean.FALSE);
        ChecklistInstanceRepository instances = mock(ChecklistInstanceRepository.class);
        ChecklistTaskInstanceRepository tasks = mock(ChecklistTaskInstanceRepository.class);
        UnifiedTaskAccessPolicy access = mock(UnifiedTaskAccessPolicy.class);
        when(instances.findByRecipientUserIdAndHistoricalAtIsNull(ACTOR_ID)).thenReturn(List.of(instance));

        var candidates = new ChecklistTodayTaskProvider(instances, tasks, access)
                .findAuthorizedTasks(ACTOR_ID);

        assertThat(candidates).isEmpty();
        verify(tasks, never()).findAllByChecklistInstanceIds(any());
        verify(access, never()).canView(instance, ACTOR_ID);
    }

    private static ChecklistInstance instance(UUID instanceId, UUID contextId) {
        return ChecklistInstance.builder()
                .id(instanceId)
                .templateVersionId(uuid(501))
                .recipientUserId(ACTOR_ID)
                .recipientRole(ChecklistRecipientRole.MOTHER)
                .careGroupId(uuid(601))
                .careContextType(ChecklistCareContextType.JOURNEY)
                .careContextId(contextId)
                .contextOwnerUserId(ACTOR_ID)
                .origin(ChecklistOrigin.SYSTEM_TEMPLATE)
                .status(ChecklistInstanceStatus.PENDING)
                .build();
    }

    private static ChecklistTaskInstance task(UUID taskId, UUID instanceId, int displayOrder) {
        return ChecklistTaskInstance.builder()
                .id(taskId)
                .checklistInstanceId(instanceId)
                .titleSnapshot("Task " + taskId)
                .displayOrder(displayOrder)
                .targetSubject(ChecklistTargetSubject.MOTHER)
                .status(ChecklistTaskStatus.PENDING)
                .build();
    }

    private static UUID uuid(int suffix) {
        return UUID.fromString("00000000-0000-0000-0000-" + String.format("%012d", suffix));
    }
}
