package com.carebridge.backend.checklist.today.service;

import com.carebridge.backend.checklist.model.ChecklistOrigin;
import com.carebridge.backend.checklist.model.ChecklistRecipientRole;
import com.carebridge.backend.checklist.model.ChecklistTaskStatus;
import com.carebridge.backend.checklist.entity.ChecklistInstance;
import com.carebridge.backend.checklist.entity.ChecklistTaskInstance;
import com.carebridge.backend.checklist.repository.ChecklistInstanceRepository;
import com.carebridge.backend.checklist.repository.ChecklistTaskInstanceRepository;
import com.carebridge.backend.checklist.today.dto.CurrentChecklistResponse;
import com.carebridge.backend.checklist.today.dto.CurrentChecklistSections;
import com.carebridge.backend.checklist.today.dto.CurrentChecklistTaskResponse;
import com.carebridge.backend.checklist.today.dto.TodayTaskCounts;
import com.carebridge.backend.checklist.today.dto.TodayTaskItemResponse;
import com.carebridge.backend.checklist.today.dto.TodayTaskSections;
import com.carebridge.backend.checklist.today.dto.TodayTasksResponse;
import com.carebridge.backend.checklist.today.dto.CurrentChecklistActionResponse;
import com.carebridge.backend.checklist.today.dto.TaskActionRequest;
import com.carebridge.backend.checklist.today.dto.TaskActionResponse;
import com.carebridge.backend.checklist.today.policy.CareGroupChecklistScopeResolver;
import com.carebridge.backend.checklist.today.policy.CareGroupChecklistScopeResolver.CareGroupChecklistScope;
import com.carebridge.backend.checklist.today.model.TaskKind;
import com.carebridge.backend.checklist.today.model.TaskAction;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.content.repository.ChecklistTemplateRepository;
import java.time.LocalDate;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Projects epoch-bound FAMILY checklist rows into one selected care-group scope. */
@Service
public class CareGroupChecklistService {
    private final UnifiedTodayTaskService unifiedTodayTaskService;
    private final CareGroupChecklistScopeResolver scopeResolver;
    private final UnifiedTaskActionFacade actionFacade;
    private final ChecklistInstanceRepository instanceRepository;
    private final ChecklistTaskInstanceRepository taskRepository;
    private final boolean lockScope;

    @Autowired
    public CareGroupChecklistService(
            UnifiedTodayTaskService unifiedTodayTaskService,
            CareGroupChecklistScopeResolver scopeResolver,
            UnifiedTaskActionFacade actionFacade,
            ChecklistTemplateRepository templateRepository,
            ChecklistInstanceRepository instanceRepository,
            ChecklistTaskInstanceRepository taskRepository) {
        this(unifiedTodayTaskService, scopeResolver, actionFacade, templateRepository,
                instanceRepository, taskRepository, true);
    }

    public CareGroupChecklistService(
            UnifiedTodayTaskService unifiedTodayTaskService,
            CareGroupChecklistScopeResolver scopeResolver,
            UnifiedTaskActionFacade actionFacade,
            ChecklistTemplateRepository templateRepository,
            ChecklistInstanceRepository instanceRepository,
            ChecklistTaskInstanceRepository taskRepository,
            boolean lockScope) {
        this.unifiedTodayTaskService = unifiedTodayTaskService;
        this.scopeResolver = scopeResolver;
        this.actionFacade = actionFacade;
        this.instanceRepository = instanceRepository;
        this.taskRepository = taskRepository;
        this.lockScope = lockScope;
    }

    /** Compatibility constructor retained for callers that only provide template metadata. */
    public CareGroupChecklistService(
            UnifiedTodayTaskService unifiedTodayTaskService,
            CareGroupChecklistScopeResolver scopeResolver,
            UnifiedTaskActionFacade actionFacade,
            ChecklistTemplateRepository templateRepository) {
        this(unifiedTodayTaskService, scopeResolver, actionFacade, templateRepository,
                null, null, true);
    }

    /** Compatibility constructor for lightweight service tests. */
    public CareGroupChecklistService(
        UnifiedTodayTaskService unifiedTodayTaskService,
            CareGroupChecklistScopeResolver scopeResolver,
            UnifiedTaskActionFacade actionFacade) {
        this(unifiedTodayTaskService, scopeResolver, actionFacade, null, null, null, false);
    }

    @Transactional
    public CurrentChecklistResponse getCurrentTasks(
            UUID actorUserId, UUID careGroupId, LocalDate date, String timezoneHeader) {
        CareGroupChecklistScope scope = requireView(actorUserId, careGroupId);
        // Mother-owned tasks (both SYSTEM_TEMPLATE and USER_CREATED) are queried using
        // the care group owner's userId so that suggestions and personal tasks are synced.
        UUID motherUserId = scope.ownerUserId();
        TodayTasksResponse motherResponse = unifiedTodayTaskService.getTodayTasks(
                motherUserId, date, timezoneHeader, Set.of(TaskKind.CHECKLIST), false);
        TodayTaskSections motherSections = motherResponse.sections();

        List<CurrentChecklistTaskResponse> overdue = mapTasks(motherSections.overdue(), scope);
        List<CurrentChecklistTaskResponse> today = mapTasks(motherSections.today(), scope);
        List<CurrentChecklistTaskResponse> upcoming = mapTasks(motherSections.upcoming(), scope);
        List<CurrentChecklistTaskResponse> unscheduled = new ArrayList<>(mapTasks(motherSections.unscheduled(), scope));

        // In addition, include any personal tasks created by the family member themselves
        List<CurrentChecklistTaskResponse> memberPersonalTasks = mapMemberPersonalTasks(actorUserId, scope);
        unscheduled.addAll(memberPersonalTasks);

        CurrentChecklistSections sections = new CurrentChecklistSections(
                overdue, today, upcoming, List.copyOf(unscheduled));
        requireSameScope(actorUserId, careGroupId, scope);
        TodayTaskCounts counts = new TodayTaskCounts(
                sections.overdue().size(), sections.today().size(),
                sections.upcoming().size(), sections.unscheduled().size());
        return new CurrentChecklistResponse(
                motherResponse.asOf(), motherResponse.zoneId(), motherResponse.horizonDays(),
                sections, counts, motherResponse.correlationId(), null);
    }

    private CareGroupChecklistScope requireView(UUID actorUserId, UUID careGroupId) {
        CareGroupChecklistScope scope = lockScope
                ? scopeResolver.resolveViewForUpdate(actorUserId, careGroupId)
                : scopeResolver.resolveView(actorUserId, careGroupId);
        if (scope == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "CHECKLIST_NOT_FOUND",
                    "Checklist scope not found");
        }
        return scope;
    }

    private static List<CurrentChecklistTaskResponse> mapTasks(
            List<TodayTaskItemResponse> items,
            CareGroupChecklistScope scope) {
        return items.stream()
                .filter(item -> isTaskInCareGroupScope(item, scope))
                .map(item -> new CurrentChecklistTaskResponse(
                        item.taskId(), item.instanceId(), item.templateVersionId(),
                        scope.careGroupId(), item.careContextType(), item.careContextId(),
                        scope.careGroupLabel(), item.careContextLabel(), item.title(),
                        item.targetSubject(), item.origin(), item.status(), item.timeBucket(),
                        Set.of(), // Family cannot tick mother's tasks!
                        item.dueAt(),
                        item.description(), item.supportFunction()))
                .toList();
    }

    private static boolean isTaskInCareGroupScope(
            TodayTaskItemResponse item,
            CareGroupChecklistScope scope) {
        if (item.careGroupId() != null && !scope.careGroupId().equals(item.careGroupId())) {
            return false;
        }
        if (item.careContextType() != null && item.careContextId() != null) {
            return scope.includes(item.careContextType(), item.careContextId());
        }
        return true;
    }

    /** Projects the selected member's private FAMILY USER_CREATED tasks. */
    private List<CurrentChecklistTaskResponse> mapMemberPersonalTasks(
            UUID actorUserId, CareGroupChecklistScope scope) {
        if (instanceRepository == null || taskRepository == null) {
            return List.of();
        }
        // Family member's own user-created tasks in this care group
        List<ChecklistInstance> memberParents = instanceRepository
                .findByRecipientUserIdAndHistoricalAtIsNull(actorUserId).stream()
                .filter(instance -> instance.getRecipientRole() == ChecklistRecipientRole.FAMILY)
                .filter(instance -> instance.getOrigin() == ChecklistOrigin.USER_CREATED)
                .filter(instance -> scope.careGroupId().equals(instance.getCareGroupId()))
                .filter(instance -> instance.getStatus() != com.carebridge.backend.checklist.model.ChecklistInstanceStatus.CANCELLED)
                .filter(instance -> instance.getCareContextId() == null || scope.includes(instance.getCareContextType(), instance.getCareContextId()))
                .toList();

        if (memberParents.isEmpty()) {
            return List.of();
        }
        Map<UUID, ChecklistInstance> parentsById = memberParents.stream()
                .collect(Collectors.toMap(ChecklistInstance::getId, value -> value, (a, b) -> a));
        return taskRepository.findAllByChecklistInstanceIds(parentsById.keySet().stream().toList()).stream()
                .filter(task -> task.getStatus() != ChecklistTaskStatus.CANCELLED)
                .map(task -> {
                    ChecklistInstance parent = parentsById.get(task.getChecklistInstanceId());
                    return personalTaskResponse(parent, task, scope, true);
                })
                .toList();
    }

    private static CurrentChecklistTaskResponse personalTaskResponse(
            ChecklistInstance parent,
            ChecklistTaskInstance task,
            CareGroupChecklistScope scope,
            boolean canMutate) {
        Set<TaskAction> actions = new java.util.LinkedHashSet<>();
        if (canMutate) {
            if (task.getStatus() == ChecklistTaskStatus.PENDING
                    || task.getStatus() == ChecklistTaskStatus.IN_PROGRESS) {
                actions.add(TaskAction.COMPLETE);
            } else if (task.getStatus() == ChecklistTaskStatus.COMPLETED) {
                actions.add(TaskAction.REOPEN);
            }
        }
        return new CurrentChecklistTaskResponse(
                task.getId(), parent.getId(), null, scope.careGroupId(),
                parent.getCareContextType(), parent.getCareContextId(),
                scope.careGroupLabel(), null, task.getTitleSnapshot(), task.getTargetSubject(),
                ChecklistOrigin.USER_CREATED, task.getStatus().name(),
                com.carebridge.backend.checklist.today.model.TaskTimeBucket.UNSCHEDULED,
                Set.copyOf(actions), task.getDueAt(), task.getDescriptionSnapshot(), task.getSupportFunction());
    }

    private void requireSameScope(
            UUID actorUserId,
            UUID careGroupId,
            CareGroupChecklistScope initial) {
        CareGroupChecklistScope current = lockScope
                ? scopeResolver.resolveViewForUpdate(actorUserId, careGroupId)
                : scopeResolver.resolveView(actorUserId, careGroupId);
        if (current == null
                || !initial.careGroupId().equals(current.careGroupId())
                || !initial.ownerUserId().equals(current.ownerUserId())
                || !initial.linkedContexts().equals(current.linkedContexts())) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "CHECKLIST_NOT_FOUND",
                    "Checklist scope not found");
        }
    }

    @Transactional
    public CurrentChecklistActionResponse applyAction(
            UUID actorUserId,
            UUID careGroupId,
            UUID taskId,
            TaskActionRequest request) {
        // View permission is enough to enter this endpoint: the action handler
        // applies the stored Family member/epoch boundary to a direct Family
        // occurrence, while a member's own USER_CREATED row is mutable with
        // view only. Mother-owned projection ids are not actionable.
        boolean permitted = (lockScope
                ? scopeResolver.resolveViewForUpdate(actorUserId, careGroupId)
                : scopeResolver.resolveView(actorUserId, careGroupId)) != null;
        if (!permitted) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "TASK_NOT_FOUND", "Task not found");
        }
        if (request == null || (request.action() != TaskAction.COMPLETE
                && request.action() != TaskAction.REOPEN)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "CHECKLIST_ACTION_INVALID",
                    "Only COMPLETE and REOPEN are supported for checklist tasks");
        }
        TaskActionResponse response = actionFacade.apply(
                actorUserId, TaskKind.CHECKLIST, taskId, request, careGroupId);
        return new CurrentChecklistActionResponse(response.taskId(), response.instanceId(),
                response.action(), response.previousStatus(), response.status(),
                response.appliedAt(), response.idempotentReplay(), response.correlationId());
    }
}
