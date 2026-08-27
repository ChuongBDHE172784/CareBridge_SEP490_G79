package com.carebridge.backend.checklist.today.provider;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.audit.service.RequiredAuditEvent;
import com.carebridge.backend.checklist.model.ChecklistCareContextType;
import com.carebridge.backend.checklist.today.dto.TaskActionResponse;
import com.carebridge.backend.checklist.today.model.TaskAction;
import com.carebridge.backend.checklist.today.model.TaskKind;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.family.entity.CareTaskStatus;
import com.carebridge.backend.family.entity.CareGroupStatus;
import com.carebridge.backend.family.entity.PermissionFlag;
import com.carebridge.backend.family.policy.CareGroupAuthorizationPolicy;
import com.carebridge.backend.family.repository.CareGroupRepository;
import com.carebridge.backend.family.repository.CareTaskRepository;
import java.time.Instant;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CareTaskActionHandler implements TaskActionHandler {
    private final CareTaskRepository taskRepository;
    private final CareGroupRepository groupRepository;
    private final CareGroupAuthorizationPolicy authorizationPolicy;
    private final AuditService auditService;

    @Override
    public TaskKind taskKind() {
        return TaskKind.CARE_TASK;
    }

    @Override
    public AuthorizedTask authorize(UUID actorUserId, UUID taskId) {
        var task = taskRepository.findById(taskId).orElseThrow(CareTaskActionHandler::notFound);
        var group = groupRepository.findById(task.getCareGroupId()).orElseThrow(CareTaskActionHandler::notFound);
        if (group.getStatus() != CareGroupStatus.ACTIVE || !hasCurrentContext(task, group)) {
            throw notFound();
        }
        boolean owner = actorUserId.equals(group.getOwnerUserId());
        boolean permittedFamily = authorizationPolicy.hasPermission(
                group.getId(), actorUserId, PermissionFlag.CHECKLIST_VIEW)
                && authorizationPolicy.hasPermission(
                group.getId(), actorUserId, PermissionFlag.CHECKLIST_COMPLETE);
        if (!actorUserId.equals(task.getAssignedTo()) || (!owner && !permittedFamily)) {
            throw notFound();
        }
        Set<TaskAction> actions = EnumSet.noneOf(TaskAction.class);
        if (task.getStatus() == CareTaskStatus.OPEN || task.getStatus() == CareTaskStatus.IN_PROGRESS
                || task.getStatus() == CareTaskStatus.NEEDS_SUPPORT) {
            actions.add(TaskAction.COMPLETE);
        }
        return new AuthorizedTask(TaskKind.CARE_TASK, taskId, null,
                normalize(task.getStatus()), actions);
    }

    @Override
    public TaskActionResponse apply(AuthorizedTask authorized, UUID actorUserId, TaskAction action,
                                    String reason, Instant appliedAt, UUID correlationId) {
        var task = taskRepository.findById(authorized.taskId()).orElseThrow(CareTaskActionHandler::notFound);
        task.setStatus(CareTaskStatus.DONE);
        task.setCompletedAt(appliedAt);
        taskRepository.save(task);
        String reasonCode = reason == null ? "USER_ACTION" : reason;
        ChecklistCareContextType contextType = task.getBabyId() != null
                ? ChecklistCareContextType.BABY : ChecklistCareContextType.JOURNEY;
        UUID contextId = task.getBabyId() != null ? task.getBabyId() : task.getJourneyId();
        auditService.logRequired(new RequiredAuditEvent(
                AuditAction.CARE_TASK_STATUS_UPDATED,
                actorUserId,
                "USER",
                null,
                task.getAssignedTo(),
                "CareTask",
                task.getId(),
                contextType,
                contextId,
                null,
                null,
                Map.of("status", authorized.status(), "action", action.name()),
                Map.of("status", "COMPLETED", "action", action.name()),
                reasonCode,
                correlationId));
        return new TaskActionResponse(TaskKind.CARE_TASK, task.getId(), null, action,
                authorized.status(), "COMPLETED", appliedAt, false, correlationId);
    }

    private static String normalize(CareTaskStatus status) {
        return status == CareTaskStatus.OPEN ? "PENDING"
                : status == CareTaskStatus.DONE ? "COMPLETED"
                : status.name();
    }

    private static boolean hasCurrentContext(
            com.carebridge.backend.family.entity.CareTask task,
            com.carebridge.backend.family.entity.CareGroup group) {
        boolean hasJourney = task.getJourneyId() != null;
        boolean hasBaby = task.getBabyId() != null;
        if (hasJourney == hasBaby) {
            return false;
        }
        return hasJourney
                ? Objects.equals(task.getJourneyId(), group.getLinkedJourneyId())
                : Objects.equals(task.getBabyId(), group.getLinkedBabyProfileId());
    }

    private static BusinessException notFound() {
        return new BusinessException(HttpStatus.NOT_FOUND, "TASK_NOT_FOUND", "Task not found");
    }
}
