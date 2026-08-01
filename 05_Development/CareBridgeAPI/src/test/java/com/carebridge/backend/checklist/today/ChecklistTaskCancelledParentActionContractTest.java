package com.carebridge.backend.checklist.today;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.checklist.audit.ChecklistAuditWriter;
import com.carebridge.backend.checklist.entity.ChecklistInstance;
import com.carebridge.backend.checklist.entity.ChecklistTaskInstance;
import com.carebridge.backend.checklist.model.ChecklistInstanceStatus;
import com.carebridge.backend.checklist.model.ChecklistCareContextType;
import com.carebridge.backend.checklist.model.ChecklistRecipientRole;
import com.carebridge.backend.checklist.model.ChecklistTaskStatus;
import com.carebridge.backend.checklist.repository.ChecklistActionCommandRepository;
import com.carebridge.backend.checklist.repository.ChecklistInstanceRepository;
import com.carebridge.backend.checklist.repository.ChecklistTaskInstanceRepository;
import com.carebridge.backend.checklist.today.dto.TaskActionRequest;
import com.carebridge.backend.checklist.today.model.TaskAction;
import com.carebridge.backend.checklist.today.model.TaskKind;
import com.carebridge.backend.checklist.today.policy.UnifiedTaskAccessPolicy;
import com.carebridge.backend.checklist.today.provider.AuthorizedTask;
import com.carebridge.backend.checklist.today.provider.ChecklistTaskActionHandler;
import com.carebridge.backend.checklist.today.service.UnifiedTaskActionFacade;
import com.carebridge.backend.common.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.junit.jupiter.api.Test;

class ChecklistTaskCancelledParentActionContractTest {

    @Test
    void unauthorizedActionAgainstTaskWhoseParentIsCancelledIsDeniedAsNotFound() {
        UUID actorId = UUID.fromString("00000000-0000-0000-0000-000000000101");
        UUID instanceId = UUID.fromString("00000000-0000-0000-0000-000000000201");
        UUID taskId = UUID.fromString("00000000-0000-0000-0000-000000000301");
        ChecklistTaskInstanceRepository tasks = mock(ChecklistTaskInstanceRepository.class);
        ChecklistInstanceRepository instances = mock(ChecklistInstanceRepository.class);
        UnifiedTaskAccessPolicy access = mock(UnifiedTaskAccessPolicy.class);
        ChecklistTaskInstance task = ChecklistTaskInstance.builder()
                .id(taskId)
                .checklistInstanceId(instanceId)
                .status(ChecklistTaskStatus.PENDING)
                .build();
        ChecklistInstance parent = ChecklistInstance.builder()
                .id(instanceId)
                .templateVersionId(UUID.fromString("00000000-0000-0000-0000-000000000401"))
                .recipientUserId(actorId)
                .recipientRole(ChecklistRecipientRole.MOTHER)
                .careGroupId(UUID.fromString("00000000-0000-0000-0000-000000000501"))
                .careContextType(ChecklistCareContextType.JOURNEY)
                .careContextId(UUID.fromString("00000000-0000-0000-0000-000000000601"))
                .status(ChecklistInstanceStatus.CANCELLED)
                .build();
        when(tasks.findById(taskId)).thenReturn(Optional.of(task));
        when(instances.findById(instanceId)).thenReturn(Optional.of(parent));
        when(instances.findForUpdateById(instanceId)).thenReturn(Optional.of(parent));
        when(tasks.findAllForUpdateByChecklistInstanceIdOrderByTaskKey(instanceId))
                .thenReturn(List.of(task));
        when(access.canComplete(parent, actorId)).thenReturn(false);
        var handler = new ChecklistTaskActionHandler(tasks, instances, access,
                mock(ChecklistAuditWriter.class), mock(jakarta.persistence.EntityManager.class));

        assertThatThrownBy(() -> handler.authorize(actorId, taskId))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("TASK_NOT_FOUND"));
        var lockOrder = inOrder(instances, tasks);
        lockOrder.verify(instances).acquireDistributionKeyLock(anyString());
        lockOrder.verify(instances).findForUpdateById(instanceId);
        lockOrder.verify(tasks).findAllForUpdateByChecklistInstanceIdOrderByTaskKey(instanceId);
    }

    @Test
    void authorizedReopenAgainstCancelledParentReturnsTerminalConflict() {
        UUID actorId = UUID.fromString("00000000-0000-0000-0000-000000000102");
        UUID instanceId = UUID.fromString("00000000-0000-0000-0000-000000000202");
        UUID taskId = UUID.fromString("00000000-0000-0000-0000-000000000302");
        UUID requestId = UUID.fromString("00000000-0000-0000-0000-000000000402");
        ChecklistTaskInstanceRepository tasks = mock(ChecklistTaskInstanceRepository.class);
        ChecklistInstanceRepository instances = mock(ChecklistInstanceRepository.class);
        UnifiedTaskAccessPolicy access = mock(UnifiedTaskAccessPolicy.class);
        ChecklistAuditWriter audit = mock(ChecklistAuditWriter.class);
        ChecklistActionCommandRepository commands = mock(ChecklistActionCommandRepository.class);
        ChecklistTaskInstance task = ChecklistTaskInstance.builder()
                .id(taskId)
                .checklistInstanceId(instanceId)
                .status(ChecklistTaskStatus.PENDING)
                .build();
        ChecklistInstance parent = ChecklistInstance.builder()
                .id(instanceId)
                .templateVersionId(UUID.fromString("00000000-0000-0000-0000-000000000403"))
                .recipientUserId(actorId)
                .recipientRole(ChecklistRecipientRole.MOTHER)
                .careGroupId(UUID.fromString("00000000-0000-0000-0000-000000000502"))
                .careContextType(ChecklistCareContextType.JOURNEY)
                .careContextId(UUID.fromString("00000000-0000-0000-0000-000000000602"))
                .status(ChecklistInstanceStatus.CANCELLED)
                .build();
        when(tasks.findById(taskId)).thenReturn(Optional.of(task));
        when(instances.findById(instanceId)).thenReturn(Optional.of(parent));
        when(instances.findForUpdateById(instanceId)).thenReturn(Optional.of(parent));
        when(tasks.findAllForUpdateByChecklistInstanceIdOrderByTaskKey(instanceId))
                .thenReturn(List.of(task));
        when(access.canComplete(parent, actorId)).thenReturn(true);
        when(commands.findByActorUserIdAndTaskKindAndTaskIdAndClientRequestId(
                actorId, "CHECKLIST", taskId, requestId)).thenReturn(Optional.empty());
        var handler = new ChecklistTaskActionHandler(tasks, instances, access,
                audit, mock(jakarta.persistence.EntityManager.class));
        var facade = new UnifiedTaskActionFacade(List.of(handler), commands,
                new ObjectMapper().findAndRegisterModules(),
                Clock.fixed(Instant.parse("2026-08-03T12:00:00Z"), ZoneOffset.UTC));

        assertThatThrownBy(() -> facade.apply(actorId, TaskKind.CHECKLIST, taskId,
                new TaskActionRequest(TaskAction.REOPEN, requestId, null)))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(exception.getCode()).isEqualTo("TASK_ALREADY_TERMINAL");
                });
        verify(tasks, never()).save(any());
        verify(instances, never()).save(any());
        verify(audit, never()).write(any());
    }

    @Test
    void lockedTaskThatMovesToAnotherParentIsDeniedWithoutMutation() {
        UUID actorId = UUID.fromString("00000000-0000-0000-0000-000000000111");
        UUID discoveredInstanceId = UUID.fromString("00000000-0000-0000-0000-000000000211");
        UUID refreshedInstanceId = UUID.fromString("00000000-0000-0000-0000-000000000212");
        UUID taskId = UUID.fromString("00000000-0000-0000-0000-000000000311");
        ChecklistTaskInstanceRepository tasks = mock(ChecklistTaskInstanceRepository.class);
        ChecklistInstanceRepository instances = mock(ChecklistInstanceRepository.class);
        UnifiedTaskAccessPolicy access = mock(UnifiedTaskAccessPolicy.class);
        ChecklistAuditWriter audit = mock(ChecklistAuditWriter.class);
        ChecklistTaskInstance task = ChecklistTaskInstance.builder()
                .id(taskId)
                .checklistInstanceId(discoveredInstanceId)
                .status(ChecklistTaskStatus.PENDING)
                .build();
        ChecklistTaskInstance movedTask = ChecklistTaskInstance.builder()
                .id(taskId)
                .checklistInstanceId(refreshedInstanceId)
                .status(ChecklistTaskStatus.PENDING)
                .build();
        ChecklistInstance parent = parent(discoveredInstanceId, actorId, ChecklistInstanceStatus.PENDING);
        when(tasks.findById(taskId)).thenReturn(Optional.of(task));
        when(instances.findById(discoveredInstanceId)).thenReturn(Optional.of(parent));
        when(instances.findForUpdateById(discoveredInstanceId)).thenReturn(Optional.of(parent));
        when(tasks.findAllForUpdateByChecklistInstanceIdOrderByTaskKey(discoveredInstanceId))
                .thenReturn(List.of(movedTask));
        var handler = new ChecklistTaskActionHandler(
                tasks, instances, access, audit, mock(jakarta.persistence.EntityManager.class));

        assertThatThrownBy(() -> handler.authorize(actorId, taskId))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("TASK_NOT_FOUND"));
        verify(tasks, never()).save(any());
        verify(instances, never()).save(any());
        verify(audit, never()).write(any());
    }

    @Test
    void applyRejectsAuthorizedInstanceMismatchWithoutMutation() {
        UUID actorId = UUID.fromString("00000000-0000-0000-0000-000000000121");
        UUID instanceId = UUID.fromString("00000000-0000-0000-0000-000000000221");
        UUID authorizedInstanceId = UUID.fromString("00000000-0000-0000-0000-000000000222");
        UUID taskId = UUID.fromString("00000000-0000-0000-0000-000000000321");
        ChecklistTaskInstanceRepository tasks = mock(ChecklistTaskInstanceRepository.class);
        ChecklistInstanceRepository instances = mock(ChecklistInstanceRepository.class);
        ChecklistAuditWriter audit = mock(ChecklistAuditWriter.class);
        ChecklistTaskInstance task = ChecklistTaskInstance.builder()
                .id(taskId)
                .checklistInstanceId(instanceId)
                .status(ChecklistTaskStatus.PENDING)
                .build();
        ChecklistInstance parent = parent(instanceId, actorId, ChecklistInstanceStatus.PENDING);
        when(tasks.findById(taskId)).thenReturn(Optional.of(task));
        when(instances.findById(instanceId)).thenReturn(Optional.of(parent));
        when(instances.findForUpdateById(instanceId)).thenReturn(Optional.of(parent));
        when(tasks.findAllForUpdateByChecklistInstanceIdOrderByTaskKey(instanceId))
                .thenReturn(List.of(task));
        var handler = new ChecklistTaskActionHandler(
                tasks, instances, mock(UnifiedTaskAccessPolicy.class), audit,
                mock(jakarta.persistence.EntityManager.class));
        AuthorizedTask authorized = new AuthorizedTask(
                TaskKind.CHECKLIST, taskId, authorizedInstanceId, "PENDING", Set.of(TaskAction.COMPLETE));

        assertThatThrownBy(() -> handler.apply(
                authorized, actorId, TaskAction.COMPLETE, null,
                Instant.parse("2026-07-29T12:00:00Z"), UUID.randomUUID()))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("TASK_NOT_FOUND"));
        verify(tasks, never()).save(any());
        verify(instances, never()).save(any());
        verify(audit, never()).write(any());
    }

    private static ChecklistInstance parent(
            UUID instanceId, UUID actorId, ChecklistInstanceStatus status) {
        return ChecklistInstance.builder()
                .id(instanceId)
                .templateVersionId(UUID.fromString("00000000-0000-0000-0000-000000000421"))
                .recipientUserId(actorId)
                .recipientRole(ChecklistRecipientRole.MOTHER)
                .careGroupId(UUID.fromString("00000000-0000-0000-0000-000000000521"))
                .careContextType(ChecklistCareContextType.JOURNEY)
                .careContextId(UUID.fromString("00000000-0000-0000-0000-000000000621"))
                .status(status)
                .build();
    }
}
