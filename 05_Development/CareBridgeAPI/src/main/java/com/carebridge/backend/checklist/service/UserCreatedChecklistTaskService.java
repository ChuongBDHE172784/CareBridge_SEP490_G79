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
import com.carebridge.backend.checklist.model.ChecklistMaterializationMode;
import com.carebridge.backend.checklist.model.ChecklistOrigin;
import com.carebridge.backend.checklist.model.ChecklistRecipientRole;
import com.carebridge.backend.checklist.model.ChecklistTaskStatus;
import com.carebridge.backend.checklist.policy.ChecklistTemplateVisibilityPolicy;
import com.carebridge.backend.checklist.repository.ChecklistInstanceRepository;
import com.carebridge.backend.checklist.repository.ChecklistTaskInstanceRepository;
import com.carebridge.backend.checklist.today.policy.UnifiedTaskMutationPolicy;
import com.carebridge.backend.checklist.today.policy.UnifiedTaskAccessPolicy;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.content.entity.ChecklistTemplate;
import com.carebridge.backend.content.repository.ChecklistTemplateRepository;
import com.carebridge.backend.family.entity.CareGroup;
import com.carebridge.backend.family.entity.CareGroupMember;
import com.carebridge.backend.family.entity.CareGroupStatus;
import com.carebridge.backend.family.entity.GroupMemberRole;
import com.carebridge.backend.family.entity.InviteStatus;
import com.carebridge.backend.family.entity.PermissionFlag;
import com.carebridge.backend.family.policy.CareGroupAuthorizationPolicy;
import com.carebridge.backend.family.repository.CareGroupMemberRepository;
import com.carebridge.backend.family.repository.CareGroupRepository;
import com.carebridge.backend.journey.entity.JourneyStatus;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Creates idempotent user-owned tasks directly in the V2 checklist aggregate. */
@Service
public class UserCreatedChecklistTaskService {

    private final MotherJourneyRepository journeyRepository;
    private final BabyProfileRepository babyRepository;
    private final ChecklistInstanceRepository instanceRepository;
    private final ChecklistTaskInstanceRepository taskRepository;
    private final UnifiedTaskMutationPolicy mutationPolicy;
    private final UnifiedTaskAccessPolicy accessPolicy;
    private final AuditService auditService;
    private final ChecklistTemplateRepository templateRepository;
    private final CareGroupRepository groupRepository;
    private final CareGroupMemberRepository memberRepository;
    private final CareGroupAuthorizationPolicy groupAuthorizationPolicy;

    /** Compatibility constructor retained for focused unit tests and legacy callers. */
    public UserCreatedChecklistTaskService(
            MotherJourneyRepository journeyRepository,
            BabyProfileRepository babyRepository,
            ChecklistInstanceRepository instanceRepository,
            ChecklistTaskInstanceRepository taskRepository,
            UnifiedTaskMutationPolicy mutationPolicy,
            UnifiedTaskAccessPolicy accessPolicy,
            AuditService auditService) {
        this(journeyRepository, babyRepository, instanceRepository, taskRepository,
                mutationPolicy, accessPolicy, auditService, null, null, null, null);
    }

    public UserCreatedChecklistTaskService(
            MotherJourneyRepository journeyRepository,
            BabyProfileRepository babyRepository,
            ChecklistInstanceRepository instanceRepository,
            ChecklistTaskInstanceRepository taskRepository,
            UnifiedTaskMutationPolicy mutationPolicy,
            UnifiedTaskAccessPolicy accessPolicy,
            AuditService auditService,
            ChecklistTemplateRepository templateRepository) {
        this(journeyRepository, babyRepository, instanceRepository, taskRepository,
                mutationPolicy, accessPolicy, auditService, templateRepository,
                null, null, null);
    }

    @Autowired
    public UserCreatedChecklistTaskService(
            MotherJourneyRepository journeyRepository,
            BabyProfileRepository babyRepository,
            ChecklistInstanceRepository instanceRepository,
            ChecklistTaskInstanceRepository taskRepository,
            UnifiedTaskMutationPolicy mutationPolicy,
            UnifiedTaskAccessPolicy accessPolicy,
            AuditService auditService,
            ChecklistTemplateRepository templateRepository,
            CareGroupRepository groupRepository,
            CareGroupMemberRepository memberRepository,
            CareGroupAuthorizationPolicy groupAuthorizationPolicy) {
        this.journeyRepository = journeyRepository;
        this.babyRepository = babyRepository;
        this.instanceRepository = instanceRepository;
        this.taskRepository = taskRepository;
        this.mutationPolicy = mutationPolicy;
        this.accessPolicy = accessPolicy;
        this.auditService = auditService;
        this.templateRepository = templateRepository;
        this.groupRepository = groupRepository;
        this.memberRepository = memberRepository;
        this.groupAuthorizationPolicy = groupAuthorizationPolicy;
    }

    @Transactional
    public ChecklistItemResponse create(AddChecklistItemRequest request, UUID actorUserId) {
        return create(request, actorUserId, (short) 1);
    }

    /** Creates a user-owned task in the explicitly negotiated V1/V2 namespace. */
    @Transactional
    public ChecklistItemResponse create(
            AddChecklistItemRequest request,
            UUID actorUserId,
            short contractVersion) {
        if (request == null || actorUserId == null) {
            throw invalid("CHECKLIST-001", "Checklist task request is required");
        }
        if (contractVersion != 1 && contractVersion != 2) {
            throw invalid("CHECKLIST_CONTRACT_VERSION_UNSUPPORTED",
                    "Unsupported checklist contract version");
        }
        if (request.careGroupId() == null
                && (request.journeyId() == null) == (request.babyId() == null)) {
            throw invalid("CHECKLIST_CONTEXT_REQUIRED", "Exactly one checklist care context is required");
        }
        if (request.careGroupId() != null
                && (request.journeyId() != null || request.babyId() != null)) {
            throw invalid("CHECKLIST_CONTEXT_CONFLICT",
                    "Family checklist context is resolved from the care group");
        }
        mutationPolicy.requireUserCreatedTarget(
                ChecklistOrigin.USER_CREATED, request.targetSubject(), contractVersion);
        if (request.clientTaskId() == null) {
            throw invalid("CHECKLIST_CLIENT_TASK_ID_REQUIRED", "clientTaskId is required");
        }

        CreateScope scope = request.careGroupId() == null
                ? resolveMotherScope(request, actorUserId)
                : resolveFamilyScope(request.careGroupId(), actorUserId);
        ResolvedContext context = scope.context();
        String instanceKey = ChecklistDistributionKeyFactory.userCreatedInstanceKey(
                actorUserId, scope.recipientRole().name(), scope.careGroupId(),
                context.type().name(), context.id(), null, null, contractVersion);
        String lifecycleKey = ChecklistDistributionKeyFactory.lifecycleScopeKey(
                null, actorUserId, scope.recipientRole().name(), scope.careGroupId(),
                context.type().name(), context.id());
        instanceRepository.acquireDistributionKeyLock(lifecycleKey);
        ChecklistInstance instance = instanceRepository.findByDistributionKey(instanceKey).orElse(null);
        if (contractVersion == 1
                && instance == null && scope.recipientRole() == ChecklistRecipientRole.MOTHER) {
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
                        .recipientRole(scope.recipientRole())
                        .careGroupId(scope.careGroupId())
                        .careGroupMemberId(scope.careGroupMemberId())
                        .checklistAccessEpoch(scope.checklistAccessEpoch())
                        .careContextType(context.type())
                        .careContextId(context.id())
                        .contextOwnerUserId(scope.contextOwnerUserId())
                        .origin(ChecklistOrigin.USER_CREATED)
                        .keyVersion(contractVersion == 2 ? "v2" : "v1")
                        .checklistContractVersion(contractVersion == 2 ? (short) 2 : null)
                        // V2 parents must carry the complete occurrence metadata
                        // shape even for an unscheduled user-created task.  These
                        // sentinel values identify the interactive, always-current
                        // personal occurrence without changing the aggregate model.
                        .periodKey(contractVersion == 2 ? "USER_CREATED" : null)
                        .scheduleZoneId(contractVersion == 2 ? "UTC" : null)
                        .materializationMode(contractVersion == 2
                                ? ChecklistMaterializationMode.INTERACTIVE : null)
                        .wasActionable(contractVersion == 2 ? Boolean.TRUE : null)
                        .status(ChecklistInstanceStatus.PENDING)
                        .build());
        } else {
            instance = requireCanonicalParent(instance, actorUserId, scope, contractVersion);
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
        List<ChecklistInstance> discovered = instanceRepository.findByRecipientUserId(actorUserId);
        Map<UUID, ChecklistTemplate> templatesByVersion = templatesByVersion(discovered);
        List<ChecklistInstance> instances = discovered.stream()
                .filter(instance -> instance.getStatus() != ChecklistInstanceStatus.CANCELLED)
                .filter(instance -> isVisibleTemplate(instance, templatesByVersion))
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

    private boolean isVisibleTemplate(
            ChecklistInstance instance,
            Map<UUID, ChecklistTemplate> templatesByVersion) {
        if (templateRepository == null) {
            // Preserve the seven-argument compatibility constructor used by
            // callers that do not provide template metadata.
            return true;
        }
        UUID templateVersionId = instance.getTemplateVersionId();
        // A user-created instance has no template version, and templatesByVersion is
        // an immutable Map when there is nothing to look up — Map.of().get(null)
        // throws, so the null case must not reach the map at all.
        ChecklistTemplate template = templateVersionId == null
                ? null
                : templatesByVersion.get(templateVersionId);
        return ChecklistTemplateVisibilityPolicy.isVisible(instance, template);
    }

    private Map<UUID, ChecklistTemplate> templatesByVersion(Collection<ChecklistInstance> instances) {
        if (templateRepository == null) {
            return Map.of();
        }
        List<UUID> versionIds = instances.stream()
                .map(ChecklistInstance::getTemplateVersionId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (versionIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, ChecklistTemplate> result = new LinkedHashMap<>();
        for (ChecklistTemplate template : templateRepository.findAllByTemplateVersionIdIn(versionIds)) {
            result.putIfAbsent(template.getTemplateVersionId(), template);
        }
        return result;
    }

    private ResolvedContext resolveMotherContext(AddChecklistItemRequest request, UUID actorUserId) {
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

    private CreateScope resolveMotherScope(AddChecklistItemRequest request, UUID actorUserId) {
        return new CreateScope(
                 ChecklistRecipientRole.MOTHER,
                 null,
                 null,
                 null,
                 actorUserId,
                 resolveMotherContext(request, actorUserId));
    }

    private CreateScope resolveFamilyScope(UUID careGroupId, UUID actorUserId) {
        if (groupRepository == null || memberRepository == null || groupAuthorizationPolicy == null) {
            throw contextUnavailable();
        }
        CareGroup group = groupRepository.findByIdAndStatus(careGroupId, CareGroupStatus.ACTIVE)
                .orElseThrow(UserCreatedChecklistTaskService::contextUnavailable);
        CareGroupMember member = memberRepository.findByCareGroupIdAndUserId(careGroupId, actorUserId)
                .filter(candidate -> candidate.getInviteStatus() == InviteStatus.ACCEPTED)
                .filter(candidate -> candidate.getMemberRole() != GroupMemberRole.OWNER)
                .orElseThrow(UserCreatedChecklistTaskService::contextUnavailable);
        if (member.getId() == null || member.getChecklistAccessEpoch() == null) {
            // P2 makes the membership binding part of the Family parent shape.
            // A row without a durable access epoch cannot be projected safely;
            // fail closed before the database trigger turns it into a generic
            // integrity error.
            throw contextUnavailable();
        }
        if (!groupAuthorizationPolicy.hasPermission(careGroupId, actorUserId, PermissionFlag.CHECKLIST_VIEW)) {
            throw contextUnavailable();
        }
        // A group may expose both lifecycle contexts. Keep the same canonical
        // preference as the mother's composer (journey first), while falling
        // back to an active linked baby when the journey has ended.
        if (group.getLinkedJourneyId() != null) {
            var journey = journeyRepository.findByIdAndOwnerUserIdAndStatus(
                    group.getLinkedJourneyId(), group.getOwnerUserId(), JourneyStatus.ACTIVE)
                    .orElse(null);
            if (journey != null) {
                return new CreateScope(
                        ChecklistRecipientRole.FAMILY,
                        careGroupId,
                        member.getId(),
                        member.getChecklistAccessEpoch(),
                        group.getOwnerUserId(),
                        new ResolvedContext(ChecklistCareContextType.JOURNEY, group.getLinkedJourneyId()));
            }
        }
        if (group.getLinkedBabyProfileId() != null) {
            var baby = babyRepository.findByIdAndOwnerUserId(
                            group.getLinkedBabyProfileId(), group.getOwnerUserId())
                    .filter(candidate -> candidate.getStatus()
                            == com.carebridge.backend.baby.entity.BabyProfileStatus.ACTIVE)
                    .orElse(null);
            if (baby != null) {
                return new CreateScope(
                        ChecklistRecipientRole.FAMILY,
                        careGroupId,
                        member.getId(),
                        member.getChecklistAccessEpoch(),
                        group.getOwnerUserId(),
                        new ResolvedContext(ChecklistCareContextType.BABY, group.getLinkedBabyProfileId()));
            }
        }
        throw contextUnavailable();
    }

    private ChecklistTaskInstance createTask(
            ChecklistInstance instance,
            String taskKey,
            AddChecklistItemRequest request,
            UUID actorUserId) {
        short contractVersion = instance.getChecklistContractVersion() == null
                ? (short) 1
                : instance.getChecklistContractVersion();
        if (contractVersion != 1 && contractVersion != 2) {
            throw invalid("CHECKLIST_CONTRACT_VERSION_UNSUPPORTED",
                    "Unsupported checklist contract version");
        }
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
                .targetSubject(contractVersion == 2 ? null : request.targetSubject())
                .keyVersion(contractVersion == 2 ? "v2" : "v1")
                .checklistContractVersion(contractVersion == 2 ? (short) 2 : null)
                .status(ChecklistTaskStatus.PENDING)
                .build());
        auditService.log(AuditAction.CHECKLIST_ITEM_ADDED, actorUserId,
                "ChecklistTaskInstance", task.getId().toString(), "user_created");
        return task;
    }

    private static ChecklistInstance requireCanonicalParent(
            ChecklistInstance instance,
            UUID actorUserId,
            CreateScope scope,
            short contractVersion) {
        ResolvedContext context = scope.context();
        short persistedContractVersion = instance.getChecklistContractVersion() == null
                ? (short) 1
                : instance.getChecklistContractVersion();
        if (instance.getOrigin() != ChecklistOrigin.USER_CREATED
                || persistedContractVersion != contractVersion
                || instance.getRecipientRole() != scope.recipientRole()
                || !java.util.Objects.equals(instance.getCareGroupId(), scope.careGroupId())
                || !java.util.Objects.equals(instance.getCareGroupMemberId(), scope.careGroupMemberId())
                || !java.util.Objects.equals(instance.getChecklistAccessEpoch(), scope.checklistAccessEpoch())
                || !actorUserId.equals(instance.getRecipientUserId())
                || !scope.contextOwnerUserId().equals(instance.getContextOwnerUserId())
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
        short contractVersion = instance.getChecklistContractVersion() == null
                ? (short) 1
                : instance.getChecklistContractVersion();
        boolean targetMatches = contractVersion == 2
                || request.targetSubject() == task.getTargetSubject();
        if (!instance.getId().equals(task.getChecklistInstanceId())
                || !request.itemText().trim().equals(task.getTitleSnapshot())
                || !targetMatches
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
                task.getDisplayOrder(), task.getCreatedAt(),
                task.getTargetSubject() == null ? null : task.getTargetSubject().name(),
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

    private record CreateScope(
            ChecklistRecipientRole recipientRole,
            UUID careGroupId,
            UUID careGroupMemberId,
            Long checklistAccessEpoch,
            UUID contextOwnerUserId,
            ResolvedContext context) {
    }
}
