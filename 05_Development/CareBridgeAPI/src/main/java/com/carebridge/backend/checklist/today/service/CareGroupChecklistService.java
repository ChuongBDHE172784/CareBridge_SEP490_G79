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
import com.carebridge.backend.checklist.today.dto.CurrentChecklistActionResponse;
import com.carebridge.backend.checklist.today.dto.TaskActionRequest;
import com.carebridge.backend.checklist.today.dto.TaskActionResponse;
import com.carebridge.backend.checklist.today.policy.CareGroupChecklistScopeResolver;
import com.carebridge.backend.checklist.today.policy.CareGroupChecklistScopeResolver.CareGroupChecklistScope;
import com.carebridge.backend.checklist.today.model.TaskKind;
import com.carebridge.backend.checklist.today.model.TaskAction;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.content.entity.ChecklistTemplate;
import com.carebridge.backend.content.entity.ChecklistTemplateStatus;
import com.carebridge.backend.content.repository.ChecklistTemplateRepository;
import java.time.LocalDate;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Projects the mother's canonical checklist rows into one selected FAMILY scope. */
@Service
public class CareGroupChecklistService {
    private final UnifiedTodayTaskService unifiedTodayTaskService;
    private final CareGroupChecklistScopeResolver scopeResolver;
    private final UnifiedTaskActionFacade actionFacade;
    private final ChecklistTemplateRepository templateRepository;
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

    private CareGroupChecklistService(
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
        this.templateRepository = templateRepository;
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
        boolean canComplete = (lockScope
                ? scopeResolver.resolveCompleteForUpdate(actorUserId, careGroupId)
                : scopeResolver.resolveComplete(actorUserId, careGroupId)) != null;
        var ownerResponse = unifiedTodayTaskService.getTodayTasks(
                scope.ownerUserId(), date, timezoneHeader, Set.of(TaskKind.CHECKLIST), false);
        TodayTaskSections ownerSections = ownerResponse.sections();
        Map<UUID, ChecklistTemplate> shareableTemplates = shareableTemplates(ownerSections);
        List<CurrentChecklistTaskResponse> personalTasks = mapPersonalFamilyTasks(
                actorUserId, scope);
        List<CurrentChecklistTaskResponse> unscheduled = new ArrayList<>(
                map(ownerSections.unscheduled(), scope, canComplete, shareableTemplates));
        unscheduled.addAll(personalTasks);
        CurrentChecklistSections sections = new CurrentChecklistSections(
                map(ownerSections.overdue(), scope, canComplete, shareableTemplates),
                map(ownerSections.today(), scope, canComplete, shareableTemplates),
                map(ownerSections.upcoming(), scope, canComplete, shareableTemplates),
                List.copyOf(unscheduled));
        requireSameScope(actorUserId, careGroupId, scope);
        TodayTaskCounts counts = new TodayTaskCounts(
                sections.overdue().size(), sections.today().size(),
                sections.upcoming().size(), sections.unscheduled().size());
        // Sequence controls are deliberately Mother-only; the owner's sequence is
        // still applied by the provider before this projection.
        return new CurrentChecklistResponse(
                ownerResponse.asOf(), ownerResponse.zoneId(), ownerResponse.horizonDays(),
                sections, counts, ownerResponse.correlationId(), null);
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

    private static List<CurrentChecklistTaskResponse> map(
            List<TodayTaskItemResponse> items,
            CareGroupChecklistScope scope,
            boolean canComplete,
            Map<UUID, ChecklistTemplate> shareableTemplates) {
        return items.stream()
                .filter(item -> item.origin() == ChecklistOrigin.SYSTEM_TEMPLATE)
                .filter(item -> item.careGroupId() == null)
                .filter(item -> scope.includes(item.careContextType(), item.careContextId()))
                .filter(item -> item.templateVersionId() != null
                        && shareableTemplates.containsKey(item.templateVersionId()))
                .map(item -> new CurrentChecklistTaskResponse(
                        item.taskId(), item.instanceId(), item.templateVersionId(),
                        scope.careGroupId(), item.careContextType(), item.careContextId(),
                        scope.careGroupLabel(), item.careContextLabel(), item.title(),
                        item.targetSubject(), item.origin(), item.status(), item.timeBucket(),
                canComplete ? item.allowedActions() : java.util.Set.of(), item.dueAt(),
                item.description(), item.supportFunction()))
                .toList();
    }

    /** Projects only the selected member's private FAMILY USER_CREATED tasks. */
    private List<CurrentChecklistTaskResponse> mapPersonalFamilyTasks(
            UUID actorUserId, CareGroupChecklistScope scope) {
        if (instanceRepository == null || taskRepository == null) {
            return List.of();
        }
        List<ChecklistInstance> parents = instanceRepository
                .findByRecipientUserIdAndHistoricalAtIsNull(actorUserId).stream()
                .filter(instance -> instance.getRecipientRole() == ChecklistRecipientRole.FAMILY)
                .filter(instance -> instance.getOrigin() == ChecklistOrigin.USER_CREATED)
                .filter(instance -> scope.careGroupId().equals(instance.getCareGroupId()))
                .filter(instance -> instance.getStatus() != com.carebridge.backend.checklist.model.ChecklistInstanceStatus.CANCELLED)
                .filter(instance -> scope.includes(instance.getCareContextType(), instance.getCareContextId()))
                .toList();
        if (parents.isEmpty()) {
            return List.of();
        }
        Map<UUID, ChecklistInstance> parentsById = parents.stream()
                .collect(Collectors.toMap(ChecklistInstance::getId, value -> value));
        return taskRepository.findAllByChecklistInstanceIds(parentsById.keySet().stream().toList()).stream()
                .filter(task -> task.getStatus() != ChecklistTaskStatus.CANCELLED)
                .map(task -> personalTaskResponse(parentsById.get(task.getChecklistInstanceId()), task, scope))
                .toList();
    }

    private static CurrentChecklistTaskResponse personalTaskResponse(
            ChecklistInstance parent,
            ChecklistTaskInstance task,
            CareGroupChecklistScope scope) {
        Set<TaskAction> actions = new java.util.LinkedHashSet<>();
        if (task.getStatus() == ChecklistTaskStatus.PENDING
                || task.getStatus() == ChecklistTaskStatus.IN_PROGRESS) {
            actions.add(TaskAction.COMPLETE);
        } else if (task.getStatus() == ChecklistTaskStatus.COMPLETED) {
            actions.add(TaskAction.REOPEN);
        }
        return new CurrentChecklistTaskResponse(
                task.getId(), parent.getId(), null, scope.careGroupId(),
                parent.getCareContextType(), parent.getCareContextId(),
                scope.careGroupLabel(), null, task.getTitleSnapshot(), task.getTargetSubject(),
                ChecklistOrigin.USER_CREATED, task.getStatus().name(),
                com.carebridge.backend.checklist.today.model.TaskTimeBucket.UNSCHEDULED,
                Set.copyOf(actions), task.getDueAt(), task.getDescriptionSnapshot(), task.getSupportFunction());
    }

    private Map<UUID, ChecklistTemplate> shareableTemplates(TodayTaskSections sections) {
        if (templateRepository == null) {
            // Compatibility tests without metadata retain the provider's visibility policy.
            Map<UUID, ChecklistTemplate> result = new LinkedHashMap<>();
            allItems(sections)
                    .map(TodayTaskItemResponse::templateVersionId)
                    .filter(java.util.Objects::nonNull)
                    .forEach(id -> result.put(id, null));
            return result;
        }
        List<UUID> versionIds = allItems(sections)
                .filter(item -> item.origin() == ChecklistOrigin.SYSTEM_TEMPLATE)
                .map(item -> item.templateVersionId())
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (versionIds.isEmpty()) {
            return Map.of();
        }
        return templateRepository.findAllByTemplateVersionIdIn(versionIds).stream()
                .filter(template -> template.getStatus() == ChecklistTemplateStatus.APPROVED)
                .filter(template -> template.getRecipientScope()
                        == com.carebridge.backend.checklist.model.ChecklistRecipientScope.MOTHER
                        || template.getRecipientScope()
                        == com.carebridge.backend.checklist.model.ChecklistRecipientScope.BOTH)
                .collect(Collectors.toMap(ChecklistTemplate::getTemplateVersionId,
                        Function.identity(), (left, right) -> left));
    }

    private static Stream<TodayTaskItemResponse> allItems(TodayTaskSections sections) {
        return Stream.of(
                        sections.overdue(), sections.today(), sections.upcoming(), sections.unscheduled())
                .flatMap(List::stream);
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
        // applies the stricter CHECKLIST_COMPLETE rule to the mother's shared
        // rows, while a member's own USER_CREATED row is mutable with view only.
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
