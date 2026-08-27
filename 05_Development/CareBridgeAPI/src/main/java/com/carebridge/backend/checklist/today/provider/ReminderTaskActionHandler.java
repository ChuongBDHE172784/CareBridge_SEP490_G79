package com.carebridge.backend.checklist.today.provider;

import com.carebridge.backend.checklist.today.dto.TaskActionResponse;
import com.carebridge.backend.checklist.repository.ReminderOccurrenceAliasRepository;
import com.carebridge.backend.checklist.today.model.TaskAction;
import com.carebridge.backend.checklist.today.model.TaskKind;
import com.carebridge.backend.checklist.today.policy.ReminderAccessPolicy;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.family.entity.CareGroup;
import com.carebridge.backend.reminder.entity.ReminderStatus;
import com.carebridge.backend.reminder.entity.Reminder;
import com.carebridge.backend.reminder.repository.ReminderRepository;
import com.carebridge.backend.reminder.service.IReminderService;
import com.carebridge.backend.reminder.service.ReminderActionAuditContext;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class ReminderTaskActionHandler implements TaskActionHandler {
    private final ReminderRepository reminderRepository;
    private final ReminderOccurrenceAliasRepository occurrenceAliasRepository;
    private final IReminderService reminderService;
    private final ReminderAccessPolicy accessPolicy;
    private final EntityManager entityManager;

    @Autowired
    public ReminderTaskActionHandler(
            ReminderRepository reminderRepository,
            ReminderOccurrenceAliasRepository occurrenceAliasRepository,
            IReminderService reminderService,
            ReminderAccessPolicy accessPolicy,
            EntityManager entityManager) {
        this.reminderRepository = reminderRepository;
        this.occurrenceAliasRepository = occurrenceAliasRepository;
        this.reminderService = reminderService;
        this.accessPolicy = accessPolicy;
        this.entityManager = entityManager;
    }

    /** Compatibility constructor for policy-focused unit tests. */
    public ReminderTaskActionHandler(
            ReminderRepository reminderRepository,
            ReminderOccurrenceAliasRepository occurrenceAliasRepository,
            IReminderService reminderService,
            ReminderAccessPolicy accessPolicy) {
        this(reminderRepository, occurrenceAliasRepository, reminderService, accessPolicy, null);
    }

    /** Compatibility constructor for alias-focused owner-only unit tests. */
    public ReminderTaskActionHandler(
            ReminderRepository reminderRepository,
            ReminderOccurrenceAliasRepository occurrenceAliasRepository,
            IReminderService reminderService) {
        this(reminderRepository, occurrenceAliasRepository, reminderService, null, null);
    }

    /** Compatibility constructor for legacy unit tests; production always has the alias repository. */
    public ReminderTaskActionHandler(ReminderRepository reminderRepository, IReminderService reminderService) {
        this(reminderRepository, null, reminderService, null, null);
    }

    @Override
    public TaskKind taskKind() {
        return TaskKind.REMINDER;
    }

    @Override
    public AuthorizedTask authorize(UUID actorUserId, UUID taskId) {
        var direct = reminderRepository.findById(taskId)
                .filter(reminder -> actorUserId.equals(reminder.getOwnerUserId()))
                .filter(reminder -> canComplete(reminder, actorUserId));
        var aliased = resolveAlias(taskId)
                .flatMap(alias -> reminderRepository.findById(alias.getReminderDefinitionId())
                        .filter(reminder -> aliasMatchesCurrentDefinition(alias, reminder)))
                .filter(reminder -> canComplete(reminder, actorUserId));
        var reminder = direct.or(() -> aliased).orElseGet(() -> reminderRepository
                .findByOwnerUserIdOrderByScheduledAtDesc(actorUserId).stream()
                .filter(candidate -> taskId.equals(ReminderOccurrenceIdFactory.create(
                        candidate.getId(), candidate.getScheduledAt(),
                        candidate.getOccurrenceGeneration())))
                .filter(candidate -> canComplete(candidate, actorUserId))
                .findFirst()
                .orElseThrow(ReminderTaskActionHandler::notFound));
        Set<TaskAction> actions = EnumSet.noneOf(TaskAction.class);
        if (reminder.getStatus() == ReminderStatus.PENDING
                || reminder.getStatus() == ReminderStatus.SNOOZED) {
            actions.add(TaskAction.COMPLETE);
            actions.add(TaskAction.SKIP);
        }
        // A durable alias is the public identity of the requested occurrence.
        // Rescheduling the definition must not rewrite that historical task ID.
        UUID occurrenceId = aliased.isPresent()
                ? taskId
                : ReminderOccurrenceIdFactory.create(
                        reminder.getId(), reminder.getScheduledAt(),
                        reminder.getOccurrenceGeneration());
        return new AuthorizedTask(TaskKind.REMINDER, occurrenceId, reminder.getId(),
                reminder.getStatus().name(), actions);
    }

    @Override
    public AuthorizedTask authorizeReplay(UUID actorUserId, UUID taskId, UUID instanceId) {
        var reminder = reminderRepository.findById(instanceId)
                .filter(candidate -> canComplete(candidate, actorUserId))
                .orElseThrow(ReminderTaskActionHandler::notFound);
        return new AuthorizedTask(TaskKind.REMINDER, taskId, reminder.getId(),
                reminder.getStatus().name(), Set.of());
    }

    @Override
    public UUID actionScopeId(AuthorizedTask task) {
        return definitionId(task);
    }

    @Override
    public AuthorizedTask authorizeForUpdate(UUID actorUserId, AuthorizedTask task) {
        UUID reminderId = definitionId(task);
        Reminder reminder;
        ReminderStatus lockedStatus;
        if (entityManager == null) {
            reminder = reminderRepository.findById(reminderId)
                    .orElseThrow(ReminderTaskActionHandler::notFound);
            lockedStatus = reminder.getStatus();
        } else {
            lockedStatus = reminderRepository.findStatusByIdForUpdate(reminderId)
                    .map(ReminderStatus::valueOf)
                    .orElseThrow(ReminderTaskActionHandler::notFound);
            entityManager.clear();
            reminder = reminderRepository.findById(reminderId)
                    .orElseThrow(ReminderTaskActionHandler::notFound);
        }
        if (!canComplete(reminder, actorUserId) || !occurrenceStillCurrent(task, reminder)) {
            throw ReminderTaskActionHandler.notFound();
        }
        Set<TaskAction> actions = EnumSet.noneOf(TaskAction.class);
        if (lockedStatus == ReminderStatus.PENDING || lockedStatus == ReminderStatus.SNOOZED) {
            actions.add(TaskAction.COMPLETE);
            actions.add(TaskAction.SKIP);
        }
        return new AuthorizedTask(
                TaskKind.REMINDER, task.taskId(), reminder.getId(), lockedStatus.name(), actions);
    }

    @Override
    public TaskActionResponse apply(AuthorizedTask task, UUID actorUserId, TaskAction action,
                                    String reason, Instant appliedAt, UUID correlationId) {
        Reminder reminder = reminderRepository.findById(definitionId(task))
                .filter(candidate -> canComplete(candidate, actorUserId))
                .orElseThrow(ReminderTaskActionHandler::notFound);
        UUID careGroupId = accessPolicy == null ? null
                : accessPolicy.presentationGroup(reminder, actorUserId)
                        .map(CareGroup::getId).orElse(null);
        ReminderActionAuditContext auditContext = new ReminderActionAuditContext(
                task.taskId(), action == TaskAction.COMPLETE ? "USER_ACTION" : reason,
                correlationId, actorUserId, careGroupId);
        if (action == TaskAction.COMPLETE) {
            reminderService.completeReminder(reminder.getId(), reminder.getOwnerUserId(), auditContext);
        } else {
            reminderService.skipReminder(reminder.getId(), reminder.getOwnerUserId(), auditContext);
        }
        return new TaskActionResponse(TaskKind.REMINDER, task.taskId(), null, action,
                task.status(), action == TaskAction.COMPLETE ? "COMPLETED" : "SKIPPED",
                appliedAt, false, correlationId);
    }

    private static UUID definitionId(AuthorizedTask task) {
        return task.instanceId() == null ? task.taskId() : task.instanceId();
    }

    private Optional<com.carebridge.backend.checklist.entity.ReminderOccurrenceAlias> resolveAlias(
            UUID taskId) {
        if (occurrenceAliasRepository == null) {
            return Optional.empty();
        }
        return occurrenceAliasRepository.findByOccurrenceId(taskId);
    }

    private boolean occurrenceStillCurrent(AuthorizedTask task, Reminder reminder) {
        if (task.taskId().equals(reminder.getId())) {
            return true;
        }
        if (occurrenceAliasRepository == null) {
            return task.taskId().equals(ReminderOccurrenceIdFactory.create(
                    reminder.getId(), reminder.getScheduledAt(), reminder.getOccurrenceGeneration()));
        }
        return resolveAlias(task.taskId())
                .filter(alias -> aliasMatchesCurrentDefinition(alias, reminder))
                .isPresent();
    }

    private static boolean aliasMatchesCurrentDefinition(
            com.carebridge.backend.checklist.entity.ReminderOccurrenceAlias alias,
            Reminder reminder) {
        return reminder.getId().equals(alias.getReminderDefinitionId())
                && reminder.getOwnerUserId().equals(alias.getOwnerUserId())
                && reminder.getOccurrenceGeneration() == alias.getOccurrenceGeneration();
    }

    private boolean canComplete(Reminder reminder, UUID actorUserId) {
        return accessPolicy == null
                ? actorUserId.equals(reminder.getOwnerUserId())
                : accessPolicy.canComplete(reminder, actorUserId);
    }

    private static BusinessException notFound() {
        return new BusinessException(HttpStatus.NOT_FOUND, "TASK_NOT_FOUND", "Task not found");
    }
}
