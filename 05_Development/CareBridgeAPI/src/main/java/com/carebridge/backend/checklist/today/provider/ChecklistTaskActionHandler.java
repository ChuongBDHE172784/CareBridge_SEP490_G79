package com.carebridge.backend.checklist.today.provider;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.checklist.audit.ChecklistAuditActorType;
import com.carebridge.backend.checklist.audit.ChecklistAuditEvent;
import com.carebridge.backend.checklist.audit.ChecklistAuditResourceType;
import com.carebridge.backend.checklist.audit.ChecklistAuditWriter;
import com.carebridge.backend.checklist.model.ChecklistInstanceStatus;
import com.carebridge.backend.checklist.model.ChecklistTaskStatus;
import com.carebridge.backend.checklist.key.ChecklistDistributionKeyFactory;
import com.carebridge.backend.checklist.entity.ChecklistInstance;
import com.carebridge.backend.checklist.entity.ChecklistTaskInstance;
import com.carebridge.backend.checklist.repository.ChecklistInstanceRepository;
import com.carebridge.backend.checklist.repository.ChecklistTaskInstanceRepository;
import com.carebridge.backend.checklist.today.dto.TaskActionResponse;
import com.carebridge.backend.checklist.today.model.TaskAction;
import com.carebridge.backend.checklist.today.model.TaskKind;
import com.carebridge.backend.checklist.today.policy.UnifiedTaskAccessPolicy;
import com.carebridge.backend.common.exception.BusinessException;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChecklistTaskActionHandler implements TaskActionHandler {
    private final ChecklistTaskInstanceRepository taskRepository;
    private final ChecklistInstanceRepository instanceRepository;
    private final UnifiedTaskAccessPolicy accessPolicy;
    private final ChecklistAuditWriter auditWriter;
    private final EntityManager entityManager;

    @Override
    public TaskKind taskKind() {
        return TaskKind.CHECKLIST;
    }

    @Override
    public AuthorizedTask authorize(UUID actorUserId, UUID taskId) {
        var aggregate = lockAggregate(taskId);
        var task = aggregate.task();
        var instance = aggregate.instance();
        if (instance.getStatus() == ChecklistInstanceStatus.CANCELLED) {
            throw notFound();
        }
        if (!accessPolicy.canComplete(instance, actorUserId)) {
            throw notFound();
        }
        Set<TaskAction> actions = EnumSet.noneOf(TaskAction.class);
        if (task.getStatus() == ChecklistTaskStatus.PENDING
                || task.getStatus() == ChecklistTaskStatus.IN_PROGRESS) {
            actions.add(TaskAction.COMPLETE);
            actions.add(TaskAction.SKIP);
        }
        return new AuthorizedTask(TaskKind.CHECKLIST, taskId, instance.getId(),
                task.getStatus().name(), actions);
    }

    @Override
    public TaskActionResponse apply(AuthorizedTask authorized, UUID actorUserId, TaskAction action,
                                    String reason, Instant appliedAt, UUID correlationId) {
        var aggregate = lockAggregate(authorized.taskId());
        var task = aggregate.task();
        var instance = aggregate.instance();
        if (authorized.instanceId() == null
                || !authorized.instanceId().equals(instance.getId())
                || instance.getStatus() == ChecklistInstanceStatus.CANCELLED) {
            throw notFound();
        }
        String previousStatus = task.getStatus().name();
        ChecklistTaskStatus nextStatus;
        AuditAction auditAction;
        if (action == TaskAction.COMPLETE) {
            nextStatus = ChecklistTaskStatus.COMPLETED;
            auditAction = AuditAction.CHECKLIST_COMPLETED;
            task.setCompletedAt(appliedAt);
            task.setSkippedAt(null);
            task.setActionReasonCode(null);
        } else {
            nextStatus = ChecklistTaskStatus.SKIPPED;
            auditAction = AuditAction.CHECKLIST_SKIPPED;
            task.setSkippedAt(appliedAt);
            task.setCompletedAt(null);
            task.setActionReasonCode(reason);
        }
        task.setStatus(nextStatus);
        taskRepository.save(task);
        updateParentStatus(instance, aggregate.tasks(), appliedAt);

        auditWriter.write(new ChecklistAuditEvent(auditAction, actorUserId,
                ChecklistAuditActorType.USER, null,
                ChecklistAuditResourceType.CHECKLIST_TASK_INSTANCE, task.getId(),
                instance.getRecipientUserId(),
                instance.getCareContextType(), instance.getCareContextId(),
                instance.getTemplateVersionId(), task.getId(), previousStatus,
                nextStatus.name(), reason, correlationId));
        return new TaskActionResponse(TaskKind.CHECKLIST, task.getId(), instance.getId(), action,
                previousStatus, nextStatus.name(), appliedAt, false, correlationId);
    }

    private void updateParentStatus(com.carebridge.backend.checklist.entity.ChecklistInstance instance,
                                    List<ChecklistTaskInstance> lockedTasks,
                                    Instant appliedAt) {
        boolean allTerminal = lockedTasks.stream()
                .allMatch(task -> task.getStatus() == ChecklistTaskStatus.COMPLETED
                        || task.getStatus() == ChecklistTaskStatus.SKIPPED
                        || task.getStatus() == ChecklistTaskStatus.CANCELLED);
        if (allTerminal) {
            instance.setStatus(ChecklistInstanceStatus.COMPLETED);
            instance.setCompletedAt(appliedAt);
        } else if (instance.getStatus() == ChecklistInstanceStatus.PENDING) {
            instance.setStatus(ChecklistInstanceStatus.IN_PROGRESS);
        }
        instanceRepository.save(instance);
    }

    private LockedAggregate lockAggregate(UUID taskId) {
        ChecklistTaskInstance discoveredTask = taskRepository.findById(taskId)
                .orElseThrow(ChecklistTaskActionHandler::notFound);
        ChecklistInstance discoveredInstance = instanceRepository.findById(discoveredTask.getChecklistInstanceId())
                .orElseThrow(ChecklistTaskActionHandler::notFound);
        String discoveredLifecycleKey = lifecycleKey(discoveredInstance);
        instanceRepository.acquireDistributionKeyLock(discoveredLifecycleKey);
        entityManager.clear();

        ChecklistInstance lockedInstance = instanceRepository.findForUpdateById(discoveredInstance.getId())
                .orElseThrow(ChecklistTaskActionHandler::notFound);
        if (!discoveredLifecycleKey.equals(lifecycleKey(lockedInstance))) {
            throw notFound();
        }

        List<ChecklistTaskInstance> lockedTasks = taskRepository
                .findAllForUpdateByChecklistInstanceIdOrderByTaskKey(lockedInstance.getId());
        ChecklistTaskInstance lockedTask = lockedTasks.stream()
                .filter(task -> taskId.equals(task.getId()))
                .findFirst()
                .orElseThrow(ChecklistTaskActionHandler::notFound);
        if (!lockedInstance.getId().equals(lockedTask.getChecklistInstanceId())) {
            throw notFound();
        }
        return new LockedAggregate(lockedTask, lockedInstance, List.copyOf(lockedTasks));
    }

    private static String lifecycleKey(ChecklistInstance instance) {
        return ChecklistDistributionKeyFactory.lifecycleScopeKey(
                instance.getTemplateVersionId(), instance.getRecipientUserId(),
                instance.getRecipientRole().name(), instance.getCareGroupId(),
                instance.getCareContextType().name(), instance.getCareContextId());
    }

    private record LockedAggregate(
            ChecklistTaskInstance task,
            ChecklistInstance instance,
            List<ChecklistTaskInstance> tasks) {
    }

    private static BusinessException notFound() {
        return new BusinessException(HttpStatus.NOT_FOUND, "TASK_NOT_FOUND", "Task not found");
    }
}
