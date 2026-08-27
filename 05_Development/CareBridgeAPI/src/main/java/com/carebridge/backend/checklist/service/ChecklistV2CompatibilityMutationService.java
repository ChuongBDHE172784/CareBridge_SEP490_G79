package com.carebridge.backend.checklist.service;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.checklist.audit.ChecklistAuditActorType;
import com.carebridge.backend.checklist.audit.ChecklistAuditEvent;
import com.carebridge.backend.checklist.audit.ChecklistAuditResourceType;
import com.carebridge.backend.checklist.audit.ChecklistAuditWriter;
import com.carebridge.backend.checklist.entity.ChecklistInstance;
import com.carebridge.backend.checklist.entity.ChecklistTaskInstance;
import com.carebridge.backend.checklist.key.ChecklistDistributionKeyFactory;
import com.carebridge.backend.checklist.model.ChecklistOrigin;
import com.carebridge.backend.checklist.model.ChecklistInstanceStatus;
import com.carebridge.backend.checklist.model.ChecklistTaskStatus;
import com.carebridge.backend.checklist.repository.ChecklistInstanceRepository;
import com.carebridge.backend.checklist.repository.ChecklistTaskInstanceRepository;
import com.carebridge.backend.checklist.today.policy.UnifiedTaskAccessPolicy;
import com.carebridge.backend.checklist.today.policy.UnifiedTaskMutationPolicy;
import com.carebridge.backend.common.exception.BusinessException;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Fail-closed adapter for obsolete edit/delete routes without any legacy persistence. */
@Service
@RequiredArgsConstructor
public class ChecklistV2CompatibilityMutationService {
    private final ChecklistTaskInstanceRepository taskRepository;
    private final ChecklistInstanceRepository instanceRepository;
    private final UnifiedTaskAccessPolicy accessPolicy;
    private final UnifiedTaskMutationPolicy mutationPolicy;
    private final ChecklistAuditWriter auditWriter;
    private final EntityManager entityManager;

    @Transactional(readOnly = true)
    public void rejectUpdate(UUID taskId, UUID actorUserId) {
        rejectMutation(taskId, actorUserId);
    }

    @Transactional(readOnly = true)
    public void rejectDelete(UUID taskId, UUID actorUserId) {
        rejectMutation(taskId, actorUserId);
    }

    /**
     * Removes a user-created Today checklist child without deleting its audit/history row.
     * System-origin children are rejected by the shared mutation policy.
     */
    @Transactional
    public void delete(UUID taskId, UUID actorUserId) {
        ChecklistTaskInstance discoveredTask = taskRepository.findById(taskId)
                .orElseThrow(ChecklistV2CompatibilityMutationService::notFound);
        ChecklistInstance discoveredInstance = instanceRepository.findById(discoveredTask.getChecklistInstanceId())
                .orElseThrow(ChecklistV2CompatibilityMutationService::notFound);
        if (discoveredInstance.getStatus() == ChecklistInstanceStatus.CANCELLED
                || !accessPolicy.canView(discoveredInstance, actorUserId)) {
            throw notFound();
        }
        mutationPolicy.requireMutable(discoveredInstance.getOrigin());

        instanceRepository.acquireDistributionKeyLock(lifecycleKey(discoveredInstance));
        entityManager.clear();
        ChecklistInstance instance = instanceRepository.findForUpdateById(discoveredInstance.getId())
                .orElseThrow(ChecklistV2CompatibilityMutationService::notFound);
        if (instance.getStatus() == ChecklistInstanceStatus.CANCELLED
                || !accessPolicy.canView(instance, actorUserId)) {
            throw notFound();
        }
        mutationPolicy.requireMutable(instance.getOrigin());

        List<ChecklistTaskInstance> lockedTasks = taskRepository
                .findAllForUpdateByChecklistInstanceIdOrderByTaskKey(instance.getId());
        ChecklistTaskInstance task = lockedTasks.stream()
                .filter(candidate -> taskId.equals(candidate.getId()))
                .findFirst()
                .orElseThrow(ChecklistV2CompatibilityMutationService::notFound);
        if (task.getStatus() == ChecklistTaskStatus.CANCELLED) {
            return;
        }
        if (instance.getOrigin() != ChecklistOrigin.USER_CREATED) {
            throw new BusinessException(HttpStatus.CONFLICT, "SYSTEM_TASK_IMMUTABLE",
                    "System tasks cannot be edited or deleted");
        }

        Instant deletedAt = Instant.now();
        String previousStatus = task.getStatus().name();
        task.setStatus(ChecklistTaskStatus.CANCELLED);
        task.setCancelledAt(deletedAt);
        task.setCompletedAt(null);
        task.setSkippedAt(null);
        task.setActionReasonCode("USER_DELETED");
        taskRepository.save(task);
        String parentPreviousStatus = reconcileParentAfterDelete(instance, lockedTasks, deletedAt);
        UUID correlationId = UUID.randomUUID();
        auditWriter.write(new ChecklistAuditEvent(
                AuditAction.CHECKLIST_CANCELLED,
                actorUserId,
                ChecklistAuditActorType.USER,
                null,
                ChecklistAuditResourceType.CHECKLIST_TASK_INSTANCE,
                task.getId(),
                instance.getRecipientUserId(),
                instance.getCareContextType(),
                instance.getCareContextId(),
                instance.getTemplateVersionId(),
                task.getId(),
                previousStatus,
                ChecklistTaskStatus.CANCELLED.name(),
                "USER_DELETED",
                correlationId));
        if (parentPreviousStatus != null) {
            auditWriter.write(new ChecklistAuditEvent(
                    AuditAction.CHECKLIST_CANCELLED,
                    actorUserId,
                    ChecklistAuditActorType.USER,
                    null,
                    ChecklistAuditResourceType.CHECKLIST_INSTANCE,
                    instance.getId(),
                    instance.getRecipientUserId(),
                    instance.getCareContextType(),
                    instance.getCareContextId(),
                    instance.getTemplateVersionId(),
                    null,
                    parentPreviousStatus,
                    ChecklistInstanceStatus.CANCELLED.name(),
                    "USER_DELETED",
                    correlationId));
        }
    }

    private String reconcileParentAfterDelete(
            ChecklistInstance instance,
            List<ChecklistTaskInstance> tasks,
            Instant changedAt) {
        boolean allCancelled = !tasks.isEmpty() && tasks.stream()
                .allMatch(task -> task.getStatus() == ChecklistTaskStatus.CANCELLED);
        boolean allTerminal = !tasks.isEmpty() && tasks.stream()
                .allMatch(task -> task.getStatus() == ChecklistTaskStatus.COMPLETED
                        || task.getStatus() == ChecklistTaskStatus.SKIPPED
                        || task.getStatus() == ChecklistTaskStatus.CANCELLED);
        String previousStatus = instance.getStatus().name();
        if (allCancelled) {
            instance.setStatus(ChecklistInstanceStatus.CANCELLED);
            instance.setCompletedAt(null);
            instance.setCancelledAt(changedAt);
            instance.setCancellationReasonCode("USER_DELETED");
        } else if (allTerminal) {
            instance.setStatus(ChecklistInstanceStatus.COMPLETED);
            if (instance.getCompletedAt() == null) {
                instance.setCompletedAt(changedAt);
            }
            instance.setCancelledAt(null);
            instance.setCancellationReasonCode(null);
        } else {
            instance.setStatus(ChecklistInstanceStatus.IN_PROGRESS);
            instance.setCompletedAt(null);
            instance.setCancelledAt(null);
            instance.setCancellationReasonCode(null);
        }
        instanceRepository.save(instance);
        return allCancelled && !ChecklistInstanceStatus.CANCELLED.name().equals(previousStatus)
                ? previousStatus : null;
    }

    private static String lifecycleKey(ChecklistInstance instance) {
        return ChecklistDistributionKeyFactory.lifecycleScopeKey(
                instance.getTemplateVersionId(), instance.getRecipientUserId(),
                instance.getRecipientRole().name(), instance.getCareGroupId(),
                instance.getCareContextType().name(), instance.getCareContextId());
    }

    private void rejectMutation(UUID taskId, UUID actorUserId) {
        var task = taskRepository.findById(taskId).orElseThrow(ChecklistV2CompatibilityMutationService::notFound);
        var instance = instanceRepository.findById(task.getChecklistInstanceId())
                .orElseThrow(ChecklistV2CompatibilityMutationService::notFound);
        if (!accessPolicy.canView(instance, actorUserId)) {
            throw notFound();
        }
        mutationPolicy.requireMutable(instance.getOrigin());
        if (instance.getOrigin() == ChecklistOrigin.USER_CREATED) {
            throw new BusinessException(HttpStatus.GONE, "CHECKLIST_LEGACY_ROUTE_RETIRED",
                    "Use the unified Today task APIs");
        }
        throw new IllegalStateException("Unsupported checklist origin");
    }

    private static BusinessException notFound() {
        return new BusinessException(HttpStatus.NOT_FOUND, "TASK_NOT_FOUND", "Task not found");
    }
}
