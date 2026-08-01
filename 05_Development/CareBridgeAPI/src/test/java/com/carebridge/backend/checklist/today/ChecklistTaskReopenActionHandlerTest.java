package com.carebridge.backend.checklist.today;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.checklist.audit.ChecklistAuditEvent;
import com.carebridge.backend.checklist.audit.ChecklistAuditWriter;
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
import com.carebridge.backend.checklist.today.model.TaskAction;
import com.carebridge.backend.checklist.today.provider.AuthorizedTask;
import com.carebridge.backend.checklist.today.provider.ChecklistTaskActionHandler;
import com.carebridge.backend.checklist.today.policy.UnifiedTaskAccessPolicy;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ChecklistTaskReopenActionHandlerTest {

    @Test
    void reopensCompletedTaskClearsMetadataAndReopensParent() {
        UUID actor = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        UUID instanceId = UUID.randomUUID();
        Instant appliedAt = Instant.parse("2026-08-03T12:00:00Z");
        ChecklistTaskInstance task = ChecklistTaskInstance.builder()
                .id(taskId)
                .checklistInstanceId(instanceId)
                .taskKey("task-key")
                .titleSnapshot("Pack water")
                .targetSubject(ChecklistTargetSubject.MOTHER)
                .status(ChecklistTaskStatus.COMPLETED)
                .completedAt(Instant.parse("2026-08-03T08:00:00Z"))
                .actionReasonCode("OLD_REASON")
                .build();
        ChecklistInstance instance = ChecklistInstance.builder()
                .id(instanceId)
                .templateVersionId(UUID.randomUUID())
                .recipientUserId(actor)
                .recipientRole(ChecklistRecipientRole.MOTHER)
                .careContextType(ChecklistCareContextType.JOURNEY)
                .careContextId(UUID.randomUUID())
                .contextOwnerUserId(actor)
                .origin(ChecklistOrigin.SYSTEM_TEMPLATE)
                .status(ChecklistInstanceStatus.COMPLETED)
                .completedAt(Instant.parse("2026-08-03T08:00:00Z"))
                .build();
        ChecklistTaskInstanceRepository tasks = mock(ChecklistTaskInstanceRepository.class);
        ChecklistInstanceRepository instances = mock(ChecklistInstanceRepository.class);
        UnifiedTaskAccessPolicy access = mock(UnifiedTaskAccessPolicy.class);
        ChecklistAuditWriter audit = mock(ChecklistAuditWriter.class);
        when(tasks.findById(taskId)).thenReturn(Optional.of(task));
        when(instances.findById(instanceId)).thenReturn(Optional.of(instance));
        when(instances.findForUpdateById(instanceId)).thenReturn(Optional.of(instance));
        when(tasks.findAllForUpdateByChecklistInstanceIdOrderByTaskKey(instanceId))
                .thenReturn(List.of(task));
        when(access.canComplete(instance, actor)).thenReturn(true);
        ChecklistTaskActionHandler handler = new ChecklistTaskActionHandler(
                tasks, instances, access, audit, mock(jakarta.persistence.EntityManager.class));

        AuthorizedTask authorized = handler.authorize(actor, taskId);
        assertThat(authorized.allowedActions()).containsExactly(TaskAction.REOPEN);

        var response = handler.apply(authorized, actor, TaskAction.REOPEN, null,
                appliedAt, UUID.randomUUID());

        assertThat(response.action()).isEqualTo(TaskAction.REOPEN);
        assertThat(response.previousStatus()).isEqualTo("COMPLETED");
        assertThat(response.status()).isEqualTo("PENDING");
        assertThat(task.getStatus()).isEqualTo(ChecklistTaskStatus.PENDING);
        assertThat(task.getCompletedAt()).isNull();
        assertThat(task.getSkippedAt()).isNull();
        assertThat(task.getCancelledAt()).isNull();
        assertThat(task.getActionReasonCode()).isNull();
        assertThat(instance.getStatus()).isEqualTo(ChecklistInstanceStatus.IN_PROGRESS);
        assertThat(instance.getCompletedAt()).isNull();

        ArgumentCaptor<ChecklistAuditEvent> event = ArgumentCaptor.forClass(ChecklistAuditEvent.class);
        verify(audit).write(event.capture());
        assertThat(event.getValue().action()).isEqualTo(AuditAction.CHECKLIST_REOPENED);
        assertThat(event.getValue().beforeStatus()).isEqualTo("COMPLETED");
        assertThat(event.getValue().afterStatus()).isEqualTo("PENDING");
        assertThat(event.getValue().reasonCode()).isNull();
    }
}
