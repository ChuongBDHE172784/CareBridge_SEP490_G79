package com.carebridge.backend.checklist.service;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.baby.repository.BabyProfileRepository;
import com.carebridge.backend.checklist.dto.AddChecklistItemRequest;
import com.carebridge.backend.checklist.dto.ChecklistItemResponse;
import com.carebridge.backend.checklist.entity.ChecklistInstance;
import com.carebridge.backend.checklist.entity.ChecklistTaskInstance;
import com.carebridge.backend.checklist.entity.ChecklistCategory;
import com.carebridge.backend.checklist.key.ChecklistDistributionKeyFactory;
import com.carebridge.backend.checklist.model.ChecklistCareContextType;
import com.carebridge.backend.checklist.model.ChecklistInstanceStatus;
import com.carebridge.backend.checklist.model.ChecklistOrigin;
import com.carebridge.backend.checklist.model.ChecklistRecipientRole;
import com.carebridge.backend.checklist.model.ChecklistTaskStatus;
import com.carebridge.backend.checklist.repository.ChecklistInstanceRepository;
import com.carebridge.backend.checklist.repository.ChecklistTaskInstanceRepository;
import com.carebridge.backend.checklist.today.policy.UnifiedTaskMutationPolicy;
import com.carebridge.backend.checklist.today.policy.UnifiedTaskAccessPolicy;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.journey.entity.JourneyStatus;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import java.util.List;
import java.util.Comparator;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Creates idempotent user-owned tasks directly in the V2 checklist aggregate. */
@Service
@RequiredArgsConstructor
public class UserCreatedChecklistTaskService {

    private final MotherJourneyRepository journeyRepository;
    private final BabyProfileRepository babyRepository;
    private final ChecklistInstanceRepository instanceRepository;
    private final ChecklistTaskInstanceRepository taskRepository;
    private final UnifiedTaskMutationPolicy mutationPolicy;
    private final UnifiedTaskAccessPolicy accessPolicy;
    private final AuditService auditService;

    @Transactional
    public ChecklistItemResponse create(AddChecklistItemRequest request, UUID actorUserId) {
        if (request == null || actorUserId == null) {
            throw invalid("CHECKLIST-001", "Checklist task request is required");
        }
        if ((request.journeyId() == null) == (request.babyId() == null)) {
            throw invalid("CHECKLIST_CONTEXT_REQUIRED", "Exactly one checklist care context is required");
        }
        mutationPolicy.requireUserCreatedTarget(ChecklistOrigin.USER_CREATED, request.targetSubject());
        if (request.clientTaskId() == null) {
            throw invalid("CHECKLIST_CLIENT_TASK_ID_REQUIRED", "clientTaskId is required");
        }

        ResolvedContext context = resolveContext(request, actorUserId);
        String instanceKey = ChecklistDistributionKeyFactory.userCreatedInstanceKey(
                actorUserId, ChecklistRecipientRole.MOTHER.name(), null,
                context.type().name(), context.id(), null, null);
        String lifecycleKey = ChecklistDistributionKeyFactory.lifecycleScopeKey(
                null, actorUserId, ChecklistRecipientRole.MOTHER.name(), null,
                context.type().name(), context.id());
        instanceRepository.acquireDistributionKeyLock(lifecycleKey);
        ChecklistInstance instance = instanceRepository.findByDistributionKey(instanceKey).orElse(null);
        if (instance == null) {
            List<ChecklistInstance> legacy = instanceRepository.findAllByLogicalPersonalIdentity(
                            actorUserId, ChecklistRecipientRole.MOTHER, context.type(), context.id(),
                            null, ChecklistOrigin.USER_CREATED)
                    .stream()
                    .filter(candidate -> candidate.getStatus() != ChecklistInstanceStatus.CANCELLED)
                    .filter(candidate -> candidate.getCareGroupId() == null)
                    .toList();
            if (!legacy.isEmpty()) {
                instance = legacy.stream()
                        .min(Comparator
                                .comparing((ChecklistInstance value) -> value.getCareGroupId() != null)
                                .thenComparing(ChecklistInstance::getCreatedAt,
                                        Comparator.nullsLast(Comparator.naturalOrder()))
                                .thenComparing(ChecklistInstance::getId,
                                        Comparator.nullsLast(Comparator.naturalOrder())))
                        .orElseThrow();
                instance = instanceRepository.findForUpdateById(instance.getId())
                        .orElseThrow(UserCreatedChecklistTaskService::contextUnavailable);
            }
        }
        if (instance == null) {
            instance = instanceRepository.saveAndFlush(ChecklistInstance.builder()
                        .distributionKey(instanceKey)
                        .recipientUserId(actorUserId)
                        .recipientRole(ChecklistRecipientRole.MOTHER)
                        .careGroupId(null)
                        .careContextType(context.type())
                        .careContextId(context.id())
                        .contextOwnerUserId(actorUserId)
                        .origin(ChecklistOrigin.USER_CREATED)
                        .status(ChecklistInstanceStatus.PENDING)
                        .build());
        } else {
            instance = requireCanonicalParent(instance, actorUserId, context);
        }

        ChecklistInstance parent = instance;
        List<ChecklistTaskInstance> lockedTasks = taskRepository
                .findAllForUpdateByChecklistInstanceIdOrderByTaskKey(parent.getId());
        String taskKey = ChecklistDistributionKeyFactory.userCreatedChildKey(
                parent.getId(), request.clientTaskId());
        ChecklistTaskInstance task = lockedTasks.stream()
                .filter(existing -> taskKey.equals(existing.getTaskKey()))
                .findFirst()
                .map(existing -> requireIdempotentPayload(existing, request, parent))
                .orElseGet(() -> createTask(parent, taskKey, request, actorUserId));
        return response(parent, task);
    }

    @Transactional(readOnly = true)
    public List<ChecklistItemResponse> listAuthorized(
            UUID actorUserId, UUID journeyId, UUID babyId) {
        if (journeyId != null && babyId != null) {
            throw invalid("CHECKLIST_CONTEXT_REQUIRED", "Checklist filters are mutually exclusive");
        }
        List<ChecklistInstance> instances = instanceRepository.findByRecipientUserId(actorUserId).stream()
                .filter(instance -> instance.getStatus() != ChecklistInstanceStatus.CANCELLED)
                .filter(instance -> accessPolicy.canView(instance, actorUserId))
                .filter(instance -> journeyId == null
                        || instance.getCareContextType() == ChecklistCareContextType.JOURNEY
                        && journeyId.equals(instance.getCareContextId()))
                .filter(instance -> babyId == null
                        || instance.getCareContextType() == ChecklistCareContextType.BABY
                        && babyId.equals(instance.getCareContextId()))
                .toList();
        if (instances.isEmpty()) {
            return List.of();
        }
        java.util.Map<UUID, ChecklistInstance> byId = instances.stream()
                .collect(java.util.stream.Collectors.toMap(ChecklistInstance::getId, value -> value));
        return taskRepository.findAllByChecklistInstanceIds(byId.keySet().stream().toList()).stream()
                .filter(task -> task.getStatus() != ChecklistTaskStatus.CANCELLED)
                .map(task -> response(byId.get(task.getChecklistInstanceId()), task))
                .toList();
    }

    private ResolvedContext resolveContext(AddChecklistItemRequest request, UUID actorUserId) {
        if (request.journeyId() != null) {
            journeyRepository.findByIdAndOwnerUserIdAndStatus(
                            request.journeyId(), actorUserId, JourneyStatus.ACTIVE)
                    .orElseThrow(UserCreatedChecklistTaskService::contextUnavailable);
            return new ResolvedContext(ChecklistCareContextType.JOURNEY, request.journeyId());
        }
        babyRepository.findOwnedActiveByIdForUpdate(request.babyId(), actorUserId)
                .orElseThrow(UserCreatedChecklistTaskService::contextUnavailable);
        return new ResolvedContext(ChecklistCareContextType.BABY, request.babyId());
    }

    private ChecklistTaskInstance createTask(
            ChecklistInstance instance,
            String taskKey,
            AddChecklistItemRequest request,
            UUID actorUserId) {
        if (instance.getStatus() == ChecklistInstanceStatus.CANCELLED) {
            // Deleting the last user-created child cancels the aggregate. A later
            // new clientTaskId starts a fresh personal checklist epoch in place.
            instance.setStatus(ChecklistInstanceStatus.PENDING);
            instance.setCompletedAt(null);
            instance.setCancelledAt(null);
            instance.setCancellationReasonCode(null);
            instanceRepository.save(instance);
        } else if (instance.getStatus() == ChecklistInstanceStatus.COMPLETED) {
            instance.setStatus(ChecklistInstanceStatus.IN_PROGRESS);
            instance.setCompletedAt(null);
            instanceRepository.save(instance);
        }
        ChecklistTaskInstance task = taskRepository.saveAndFlush(ChecklistTaskInstance.builder()
                .checklistInstanceId(instance.getId())
                .taskKey(taskKey)
                .titleSnapshot(request.itemText().trim())
                .displayOrder(request.itemOrder() == null ? 0 : request.itemOrder())
                .required(Boolean.FALSE)
                .category(request.category() == null ? ChecklistCategory.GENERAL : request.category())
                .targetSubject(request.targetSubject())
                .status(ChecklistTaskStatus.PENDING)
                .build());
        auditService.log(AuditAction.CHECKLIST_ITEM_ADDED, actorUserId,
                "ChecklistTaskInstance", task.getId().toString(), "user_created");
        return task;
    }

    private static ChecklistInstance requireCanonicalParent(
            ChecklistInstance instance,
            UUID actorUserId,
            ResolvedContext context) {
        if (instance.getOrigin() != ChecklistOrigin.USER_CREATED
                || instance.getRecipientRole() != ChecklistRecipientRole.MOTHER
                || instance.getCareGroupId() != null
                || !actorUserId.equals(instance.getRecipientUserId())
                || !actorUserId.equals(instance.getContextOwnerUserId())
                || instance.getCareContextType() != context.type()
                || !context.id().equals(instance.getCareContextId())) {
            throw new BusinessException(HttpStatus.CONFLICT, "CHECKLIST_KEY_CONFLICT",
                    "Checklist key resolves to a different canonical payload");
        }
        return instance;
    }

    private static ChecklistTaskInstance requireIdempotentPayload(
            ChecklistTaskInstance task,
            AddChecklistItemRequest request,
            ChecklistInstance instance) {
        int displayOrder = request.itemOrder() == null ? 0 : request.itemOrder();
        if (!instance.getId().equals(task.getChecklistInstanceId())
                || !request.itemText().trim().equals(task.getTitleSnapshot())
                || request.targetSubject() != task.getTargetSubject()
                || (request.category() == null ? ChecklistCategory.GENERAL : request.category())
                        != task.getCategory()
                || !Integer.valueOf(displayOrder).equals(task.getDisplayOrder())) {
            throw new BusinessException(HttpStatus.CONFLICT, "IDEMPOTENCY_KEY_REUSE",
                    "clientTaskId was already used with a different payload");
        }
        return task;
    }

    private static ChecklistItemResponse response(
            ChecklistInstance instance,
            ChecklistTaskInstance task) {
        return new ChecklistItemResponse(task.getId(), instance.getRecipientUserId(),
                instance.getCareContextType() == ChecklistCareContextType.JOURNEY
                        ? instance.getCareContextId() : null,
                instance.getCareContextType() == ChecklistCareContextType.BABY
                        ? instance.getCareContextId() : null,
                task.getTemplateItemVersionId(), null, task.getRequired(), task.getTitleSnapshot(),
                task.getCategory().name(),
                task.getStatus() == ChecklistTaskStatus.COMPLETED, task.getCompletedAt(),
                task.getDisplayOrder(), task.getCreatedAt(), task.getTargetSubject().name(),
                instance.getOrigin().name());
    }

    private static BusinessException invalid(String code, String message) {
        return new BusinessException(HttpStatus.BAD_REQUEST, code, message);
    }

    private static BusinessException contextUnavailable() {
        return new BusinessException(HttpStatus.NOT_FOUND, "CHECKLIST_CONTEXT_UNAVAILABLE",
                "Checklist care context is unavailable");
    }

    private record ResolvedContext(ChecklistCareContextType type, UUID id) {
    }
}
