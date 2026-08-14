package com.carebridge.backend.checklist.sequence;

import com.carebridge.backend.checklist.distribution.ChecklistDistributionCommand;
import com.carebridge.backend.checklist.distribution.ChecklistDistributionItem;
import com.carebridge.backend.checklist.distribution.ChecklistDistributionRecipient;
import com.carebridge.backend.checklist.distribution.ChecklistDistributionService;
import com.carebridge.backend.checklist.distribution.ChecklistLifecycleDates;
import com.carebridge.backend.checklist.distribution.ChecklistLifecycleEligibility;
import com.carebridge.backend.checklist.entity.ChecklistActionCommand;
import com.carebridge.backend.checklist.entity.ChecklistInstance;
import com.carebridge.backend.checklist.entity.ChecklistTaskInstance;
import com.carebridge.backend.checklist.key.ChecklistDistributionKeyFactory;
import com.carebridge.backend.checklist.model.ChecklistAnchorType;
import com.carebridge.backend.checklist.model.ChecklistCareContextType;
import com.carebridge.backend.checklist.model.ChecklistInstanceStatus;
import com.carebridge.backend.checklist.model.ChecklistOrigin;
import com.carebridge.backend.checklist.model.ChecklistRecipientRole;
import com.carebridge.backend.checklist.model.ChecklistRangeUnit;
import com.carebridge.backend.checklist.model.ChecklistTaskStatus;
import com.carebridge.backend.checklist.repository.ChecklistActionCommandRepository;
import com.carebridge.backend.checklist.repository.ChecklistInstanceRepository;
import com.carebridge.backend.checklist.repository.ChecklistTaskInstanceRepository;
import com.carebridge.backend.checklist.today.dto.TodaySequenceProjection;
import com.carebridge.backend.content.entity.ChecklistItem;
import com.carebridge.backend.content.entity.ChecklistTemplate;
import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.repository.ChecklistItemRepository;
import com.carebridge.backend.content.repository.ChecklistTemplateRepository;
import com.carebridge.backend.common.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Explicit, owner-authorized and idempotent sequence switch. */
@Service
public class ChecklistSequenceAdvanceService {
    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final String ACTION_KIND = "CHECKLIST_SEQUENCE";
    private static final String HISTORY_REASON = "SEQUENCE_STEP_COMPLETED";

    private final ChecklistSequenceResolver resolver;
    private final ChecklistInstanceRepository instanceRepository;
    private final ChecklistTaskInstanceRepository taskRepository;
    private final ChecklistTemplateRepository templateRepository;
    private final ChecklistItemRepository itemRepository;
    private final ChecklistDistributionService distributionService;
    private final ChecklistActionCommandRepository commandRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public ChecklistSequenceAdvanceService(
            ChecklistSequenceResolver resolver,
            ChecklistInstanceRepository instanceRepository,
            ChecklistTaskInstanceRepository taskRepository,
            ChecklistTemplateRepository templateRepository,
            ChecklistItemRepository itemRepository,
            ChecklistDistributionService distributionService,
            ChecklistActionCommandRepository commandRepository,
            ObjectMapper objectMapper) {
        this(resolver, instanceRepository, taskRepository, templateRepository, itemRepository,
                distributionService, commandRepository, objectMapper, Clock.systemUTC());
    }

    public ChecklistSequenceAdvanceService(
            ChecklistSequenceResolver resolver,
            ChecklistInstanceRepository instanceRepository,
            ChecklistTaskInstanceRepository taskRepository,
            ChecklistTemplateRepository templateRepository,
            ChecklistItemRepository itemRepository,
            ChecklistDistributionService distributionService,
            ChecklistActionCommandRepository commandRepository,
            ObjectMapper objectMapper,
            Clock clock) {
        this.resolver = resolver;
        this.instanceRepository = instanceRepository;
        this.taskRepository = taskRepository;
        this.templateRepository = templateRepository;
        this.itemRepository = itemRepository;
        this.distributionService = distributionService;
        this.commandRepository = commandRepository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional
    public ChecklistSequenceAdvanceResponse advance(
            UUID actorUserId, ChecklistSequenceAdvanceRequest request) {
        if (actorUserId == null || request == null || request.currentInstanceId() == null
                || request.clientRequestId() == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "SEQUENCE_ADVANCE_INVALID",
                    "currentInstanceId and clientRequestId are required");
        }
        ChecklistInstance discovered = instanceRepository.findById(request.currentInstanceId())
                .orElseThrow(this::notFound);
        if (!isOwnerMother(discovered, actorUserId)) {
            throw notFound();
        }
        commandRepository.acquireIdempotencyClaimLock(
                actorUserId + "|" + ACTION_KIND + "|" + request.clientRequestId());
        commandRepository.acquireTaskActionLock("SEQUENCE_ADVANCE|" + request.currentInstanceId());
        String payloadHash = hash(request.currentInstanceId());
        var requestExisting = commandRepository.findFirstByActorUserIdAndTaskKindAndClientRequestId(
                actorUserId, ACTION_KIND, request.clientRequestId());
        if (requestExisting.isPresent()) {
            ChecklistActionCommand command = requestExisting.get();
            if (!request.currentInstanceId().equals(command.getTaskId())
                    || !payloadHash.equals(command.getPayloadHash())) {
                throw new BusinessException(HttpStatus.CONFLICT, "IDEMPOTENCY_KEY_REUSE",
                        "clientRequestId was already used with a different payload");
            }
            return readResult(command.getResultJson());
        }
        var existing = commandRepository.findByActorUserIdAndTaskKindAndTaskIdAndClientRequestId(
                actorUserId, ACTION_KIND, request.currentInstanceId(), request.clientRequestId());
        if (existing.isPresent()) {
            ChecklistActionCommand command = existing.get();
            if (!payloadHash.equals(command.getPayloadHash())) {
                throw new BusinessException(HttpStatus.CONFLICT, "IDEMPOTENCY_KEY_REUSE",
                        "clientRequestId was already used with a different payload");
            }
            return readResult(command.getResultJson());
        }

        instanceRepository.acquireDistributionKeyLock(lifecycleKey(discovered));
        ChecklistInstance current = instanceRepository.findForUpdateById(discovered.getId())
                .orElseThrow(this::notFound);
        if (current.getHistoricalAt() != null) {
            if (!HISTORY_REASON.equals(current.getHistoryReasonCode())) {
                throw new BusinessException(HttpStatus.CONFLICT, "SEQUENCE_STALE_CURRENT",
                        "The checklist is no longer the current sequence set");
            }
            TodaySequenceProjection converged = resolver.resolve(actorUserId);
            if (converged == null || converged.currentInstanceId() == null) {
                throw new BusinessException(HttpStatus.CONFLICT, "SEQUENCE_STALE_CURRENT",
                        "The checklist is no longer the current sequence set");
            }
            ChecklistSequenceAdvanceResponse response = new ChecklistSequenceAdvanceResponse(
                    current.getId(), converged.currentInstanceId(), current.getHistoricalAt(), converged);
            storeCommand(actorUserId, current.getId(), request.clientRequestId(), payloadHash,
                    response, current.getHistoricalAt());
            return response;
        }
        TodaySequenceProjection projection = resolver.resolve(actorUserId);
        if (projection == null || !Objects.equals(projection.currentInstanceId(), current.getId())) {
            throw new BusinessException(HttpStatus.CONFLICT, "SEQUENCE_STALE_CURRENT",
                    "The checklist is no longer the current sequence set");
        }
        if (projection.sequenceState() != ChecklistSequenceState.READY_TO_ADVANCE
                || !projection.advanceAvailable()) {
            throw new BusinessException(HttpStatus.CONFLICT, "SEQUENCE_NOT_READY",
                    "The checklist is not ready to advance");
        }
        ChecklistSequenceResolver.Chain chain = resolver.resolveChain();
        ChecklistTemplate successor = chain.activeByPosition().get(projection.currentPosition() + 1);
        if (successor == null) {
            throw new BusinessException(HttpStatus.CONFLICT, "CONFIGURATION_BLOCKED",
                    "The next checklist set is not configured");
        }
        List<ChecklistTaskInstance> tasks = taskRepository
                .findAllForUpdateByChecklistInstanceIdOrderByTaskKey(current.getId());
        long required = tasks.stream().filter(task -> Boolean.TRUE.equals(task.getRequired())).count();
        long completed = tasks.stream().filter(task -> Boolean.TRUE.equals(task.getRequired()))
                .filter(task -> task.getStatus() == ChecklistTaskStatus.COMPLETED).count();
        if (required == 0 || required != completed) {
            throw new BusinessException(HttpStatus.CONFLICT, "SEQUENCE_NOT_READY",
                    "All required checklist tasks must be completed");
        }

        UUID correlationId = UUID.randomUUID();
        ChecklistDistributionCommand command = distributionCommand(current, successor, correlationId);
        var distribution = distributionService.distributeDetailed(command);
        if (distribution == null || distribution.total().conflicts() > 0
                || distribution.total().failures() > 0
                || distribution.total().createdInstances() + distribution.total().existingInstances() < 1) {
            throw new BusinessException(HttpStatus.CONFLICT, "CONFIGURATION_BLOCKED",
                    "The next checklist set could not be materialized");
        }
        ChecklistInstance successorInstance = instanceRepository
                .findAllByLogicalPersonalIdentity(actorUserId, ChecklistRecipientRole.MOTHER, current.getCareContextType(),
                        current.getCareContextId(), successor.getTemplateVersionId(), ChecklistOrigin.SYSTEM_TEMPLATE)
                .stream()
                .filter(instance -> instance.getHistoricalAt() == null)
                .filter(instance -> instance.getStatus() != ChecklistInstanceStatus.CANCELLED)
                .findFirst()
                .orElseThrow(() -> new BusinessException(HttpStatus.CONFLICT, "CONFIGURATION_BLOCKED",
                        "The next checklist set could not be materialized"));
        Instant advancedAt = clock.instant();
        current.setHistoricalAt(advancedAt);
        current.setHistoryReasonCode(HISTORY_REASON);
        instanceRepository.save(current);
        TodaySequenceProjection resultProjection = resolver.resolve(actorUserId);
        ChecklistSequenceAdvanceResponse response = new ChecklistSequenceAdvanceResponse(
                current.getId(), successorInstance.getId(), advancedAt, resultProjection);
        storeCommand(actorUserId, current.getId(), request.clientRequestId(), payloadHash,
                response, advancedAt);
        return response;
    }

    private ChecklistDistributionCommand distributionCommand(
            ChecklistInstance current, ChecklistTemplate successor, UUID correlationId) {
        List<ChecklistDistributionItem> items = itemRepository.findByTemplate_IdOrderByOrder(successor.getId()).stream()
                .map(this::item)
                .toList();
        ChecklistLifecycleEligibility eligibility = new InlineEligibility(
                ContentStage.PRE_PREGNANCY.name(), ChecklistAnchorType.NONE,
                ChecklistRangeUnit.DAY, 0, 0);
        return new ChecklistDistributionCommand(
                successor.getTemplateLineageId() == null ? successor.getId() : successor.getTemplateLineageId(),
                successor.getTemplateVersionId(), null, actorOwner(current),
                current.getCareContextType(), current.getCareContextId(), current.getContextOwnerUserId(),
                ContentStage.PRE_PREGNANCY, eligibility,
                new ChecklistLifecycleDates(null, null, null, null),
                java.time.LocalDate.now(clock), DEFAULT_ZONE,
                List.of(new ChecklistDistributionRecipient(actorOwner(current), ChecklistRecipientRole.MOTHER,
                        true, true, true)), items, correlationId, null, null,
                successor.getChecklistContractVersion());
    }

    private ChecklistDistributionItem item(ChecklistItem item) {
        return new ChecklistDistributionItem(item.getId(), item.getItemText(),
                item.getOrder() == null ? 0 : item.getOrder(), Boolean.TRUE.equals(item.getIsRequired()),
                item.getTargetSubject(), item.getDueAnchorType(), item.getDueOffsetStart(), item.getDueOffsetUnit(),
                item.getDescription(), item.getSupportFunction());
    }

    private static UUID actorOwner(ChecklistInstance instance) {
        return instance.getContextOwnerUserId();
    }

    private static boolean isOwnerMother(ChecklistInstance instance, UUID actorUserId) {
        return actorUserId.equals(instance.getRecipientUserId())
                && actorUserId.equals(instance.getContextOwnerUserId())
                && instance.getRecipientRole() == ChecklistRecipientRole.MOTHER
                && instance.getOrigin() == ChecklistOrigin.SYSTEM_TEMPLATE
                && instance.getCareContextType() == ChecklistCareContextType.JOURNEY
                ;
    }

    private static String lifecycleKey(ChecklistInstance instance) {
        return ChecklistDistributionKeyFactory.lifecycleScopeKey(
                instance.getTemplateVersionId(), instance.getRecipientUserId(),
                instance.getRecipientRole().name(), instance.getCareGroupId(),
                instance.getCareContextType().name(), instance.getCareContextId());
    }

    private ChecklistSequenceAdvanceResponse readResult(String json) {
        try {
            return objectMapper.readValue(json, ChecklistSequenceAdvanceResponse.class).asReplay();
        } catch (Exception exception) {
            throw new IllegalStateException("Stored sequence advance result is unreadable", exception);
        }
    }

    private void storeCommand(
            UUID actorUserId,
            UUID instanceId,
            UUID requestId,
            String payloadHash,
            ChecklistSequenceAdvanceResponse response,
            Instant appliedAt) {
        try {
            commandRepository.save(ChecklistActionCommand.builder()
                    .actorUserId(actorUserId)
                    .taskKind(ACTION_KIND)
                    .taskId(instanceId)
                    .clientRequestId(requestId)
                    .payloadHash(payloadHash)
                    .actionType("ADVANCE")
                    .resultStatus("APPLIED")
                    .resultJson(objectMapper.writeValueAsString(response))
                    .appliedAt(appliedAt)
                    .retainUntil(appliedAt.plusSeconds(7L * 365 * 24 * 3600))
                    .build());
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to persist sequence advance command", exception);
        }
    }

    private static String hash(UUID instanceId) {
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest((ACTION_KIND + "|" + instanceId).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private BusinessException notFound() {
        return new BusinessException(HttpStatus.NOT_FOUND, "TASK_NOT_FOUND", "Task not found");
    }

    private record InlineEligibility(
            String stage,
            ChecklistAnchorType anchorType,
            ChecklistRangeUnit rangeUnit,
            Integer startInclusive,
            Integer endInclusive) implements ChecklistLifecycleEligibility {
        @Override public String getStage() { return stage; }
        @Override public ChecklistAnchorType getAnchorType() { return anchorType; }
        @Override public ChecklistRangeUnit getRangeUnit() { return rangeUnit; }
        @Override public Integer getStartInclusive() { return startInclusive; }
        @Override public Integer getEndInclusive() { return endInclusive; }
        @Override public Boolean getActive() { return true; }
    }
}
