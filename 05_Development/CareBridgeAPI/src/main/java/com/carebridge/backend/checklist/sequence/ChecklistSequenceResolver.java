package com.carebridge.backend.checklist.sequence;

import com.carebridge.backend.checklist.entity.ChecklistInstance;
import com.carebridge.backend.checklist.entity.ChecklistTaskInstance;
import com.carebridge.backend.checklist.model.ChecklistCareContextType;
import com.carebridge.backend.checklist.model.ChecklistOrigin;
import com.carebridge.backend.checklist.model.ChecklistRecipientRole;
import com.carebridge.backend.checklist.model.ChecklistTaskStatus;
import com.carebridge.backend.checklist.repository.ChecklistInstanceRepository;
import com.carebridge.backend.checklist.repository.ChecklistTaskInstanceRepository;
import com.carebridge.backend.checklist.today.dto.TodaySequenceNextSet;
import com.carebridge.backend.checklist.today.dto.TodaySequenceProjection;
import com.carebridge.backend.content.entity.ChecklistTemplate;
import com.carebridge.backend.content.entity.ChecklistTemplateStatus;
import com.carebridge.backend.content.entity.ChecklistTemplateType;
import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.repository.ChecklistItemRepository;
import com.carebridge.backend.content.repository.ChecklistTemplateRepository;
import com.carebridge.backend.journey.entity.JourneyStatus;
import com.carebridge.backend.journey.entity.JourneyType;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/** Resolves the server-owned sequence without inferring an advance choice from a read. */
@Service
public class ChecklistSequenceResolver {
    private static final String CONFIGURATION_BLOCKED = "CONFIGURATION_BLOCKED";
    private static final int MAX_SEQUENCE_POSITION = 1000;

    private final ChecklistTemplateRepository templateRepository;
    private final ChecklistInstanceRepository instanceRepository;
    private final ChecklistTaskInstanceRepository taskRepository;
    private final MotherJourneyRepository journeyRepository;
    private final ChecklistItemRepository itemRepository;

    public ChecklistSequenceResolver(
            ChecklistTemplateRepository templateRepository,
            ChecklistInstanceRepository instanceRepository,
            ChecklistTaskInstanceRepository taskRepository) {
        this(templateRepository, instanceRepository, taskRepository, null, null);
    }

    @Autowired
    public ChecklistSequenceResolver(
            ChecklistTemplateRepository templateRepository,
            ChecklistInstanceRepository instanceRepository,
            ChecklistTaskInstanceRepository taskRepository,
            MotherJourneyRepository journeyRepository,
            ChecklistItemRepository itemRepository) {
        this.templateRepository = templateRepository;
        this.instanceRepository = instanceRepository;
        this.taskRepository = taskRepository;
        this.journeyRepository = journeyRepository;
        this.itemRepository = itemRepository;
    }

    public ChecklistSequenceResolver(
            ChecklistTemplateRepository templateRepository,
            ChecklistInstanceRepository instanceRepository,
            ChecklistTaskInstanceRepository taskRepository,
            MotherJourneyRepository journeyRepository) {
        this(templateRepository, instanceRepository, taskRepository, journeyRepository, null);
    }

    @Transactional(readOnly = true)
    public TodaySequenceProjection resolve(UUID ownerUserId) {
        if (ownerUserId == null) {
            return null;
        }
        // The sequence is scoped to the canonical active PRE_PREGNANCY Journey.  Without
        // this guard a Mother in a pregnancy/postpartum Journey would receive a synthetic
        // "position 1" projection simply because sequence templates exist globally.
        final UUID canonicalJourneyId;
        if (journeyRepository != null) {
            Optional<com.carebridge.backend.journey.entity.MotherJourney> canonical = Optional.ofNullable(
                            journeyRepository.findCanonical(ownerUserId))
                    .orElse(Optional.empty())
                    .filter(journey -> journey.getStatus() == JourneyStatus.ACTIVE)
                    .filter(journey -> journey.getJourneyType() == JourneyType.PRE_PREGNANCY);
            if (canonical.isEmpty()) {
                return null;
            }
            canonicalJourneyId = canonical.get().getId();
        } else {
            canonicalJourneyId = null;
        }
        Chain chain = resolveChain();
        if (chain.positions().isEmpty()) {
            return null;
        }
        if (!chain.valid()) {
            return blocked(chain.reason());
        }

        List<ChecklistInstance> currentInstances = instanceRepository
                .findByRecipientUserIdAndHistoricalAtIsNull(ownerUserId).stream()
                .filter(this::isPersonalSystemInstance)
                .filter(instance -> instance.getStatus()
                        != com.carebridge.backend.checklist.model.ChecklistInstanceStatus.CANCELLED)
                .filter(instance -> canonicalJourneyId == null
                        || Objects.equals(instance.getCareContextId(), canonicalJourneyId))
                .filter(instance -> chain.versionIds().contains(instance.getTemplateVersionId()))
                .sorted(Comparator.comparing(ChecklistInstance::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        // A legacy position-zero instance keeps the user in the legacy cohort.
        boolean legacyCurrent = instanceRepository.findByRecipientUserIdAndHistoricalAtIsNull(ownerUserId).stream()
                .filter(this::isPersonalSystemInstance)
                .filter(instance -> instance.getStatus()
                        != com.carebridge.backend.checklist.model.ChecklistInstanceStatus.CANCELLED)
                .filter(instance -> canonicalJourneyId == null
                        || Objects.equals(instance.getCareContextId(), canonicalJourneyId))
                .anyMatch(instance -> {
                    ChecklistTemplate template = template(instance.getTemplateVersionId());
                    return template != null && position(template) == 0;
                });
        if (legacyCurrent) {
            return null;
        }

        if (currentInstances.isEmpty()) {
            return new TodaySequenceProjection(
                    ChecklistSequenceState.ACTIVE, null, null, null, 1,
                    chain.totalPositions(), 0, false, next(chain, 1), false, null);
        }
        if (currentInstances.size() > 1) {
            return blocked("DUPLICATE_CURRENT_INSTANCE");
        }

        ChecklistInstance current = currentInstances.get(0);
        if (journeyRepository != null
                && (current.getCareContextType() != ChecklistCareContextType.JOURNEY
                    || journeyRepository.findById(current.getCareContextId())
                        .filter(journey -> journey.getStatus() == JourneyStatus.ACTIVE)
                        .filter(journey -> journey.getJourneyType() == JourneyType.PRE_PREGNANCY)
                        .isEmpty())) {
            return null;
        }
        ChecklistTemplate currentTemplate = template(current.getTemplateVersionId());
        int currentPosition = position(currentTemplate);
        if (currentTemplate == null || !chain.positions().containsKey(currentPosition)) {
            return blocked("POSITION_NOT_CONFIGURED");
        }
        if (currentPosition > 1) {
            List<ChecklistInstance> allInstances = instanceRepository.findByRecipientUserId(ownerUserId).stream()
                    .filter(this::isPersonalSystemInstance)
                    .filter(instance -> canonicalJourneyId == null
                            || Objects.equals(instance.getCareContextId(), canonicalJourneyId))
                    .toList();
            for (int expected = 1; expected < currentPosition; expected++) {
                int expectedPosition = expected;
                boolean predecessorAdvanced = allInstances.stream()
                        .filter(instance -> instance.getHistoricalAt() != null)
                        .filter(instance -> "SEQUENCE_STEP_COMPLETED"
                                .equals(instance.getHistoryReasonCode()))
                        .map(instance -> template(instance.getTemplateVersionId()))
                        .anyMatch(template -> position(template) == expectedPosition);
                if (!predecessorAdvanced) {
                    return blocked("PREDECESSOR_NOT_ADVANCED");
                }
            }
        }
        List<ChecklistTaskInstance> tasks = taskRepository
                .findByChecklistInstanceIdOrderByDisplayOrder(current.getId());
        long requiredCount = tasks.stream().filter(task -> Boolean.TRUE.equals(task.getRequired())).count();
        long completedRequired = tasks.stream()
                .filter(task -> Boolean.TRUE.equals(task.getRequired()))
                .filter(task -> task.getStatus() == ChecklistTaskStatus.COMPLETED)
                .count();
        boolean qualified = requiredCount > 0 && requiredCount == completedRequired;
        int qualifiedPositions = Math.max(0, currentPosition - (qualified ? 0 : 1));
        ChecklistTemplate successor = chain.activeByPosition().get(currentPosition + 1);
        boolean successorExpected = chain.positions().containsKey(currentPosition + 1);
        if (qualified && successorExpected && successor == null) {
            return projection(current, currentTemplate, currentPosition, chain, qualifiedPositions,
                    ChecklistSequenceState.CONFIGURATION_BLOCKED, false, null, false,
                    "SUCCESSOR_NOT_ACTIVE");
        }
        if (!qualified) {
            return projection(current, currentTemplate, currentPosition, chain, qualifiedPositions,
                    ChecklistSequenceState.ACTIVE, false, successor, false, null);
        }
        if (successor != null) {
            return projection(current, currentTemplate, currentPosition, chain,
                    currentPosition, ChecklistSequenceState.READY_TO_ADVANCE, true, successor, false, null);
        }
        return projection(current, currentTemplate, currentPosition, chain,
                currentPosition, ChecklistSequenceState.SEQUENCE_COMPLETE, false, null, true, null);
    }

    public Chain resolveChain() {
        List<ChecklistTemplate> approved = templateRepository
                .findAllDistributionEnabledByStageAndStatus(ContentStage.PRE_PREGNANCY,
                        ChecklistTemplateStatus.APPROVED).stream()
                .filter(this::isSequenceTemplate)
                .toList();
        List<ChecklistTemplate> archived = templateRepository
                .findByStageAndStatusOrderByUpdatedAtDesc(ContentStage.PRE_PREGNANCY,
                        ChecklistTemplateStatus.ARCHIVED).stream()
                .filter(this::isSequenceEvidenceTemplate)
                .toList();
        Map<Integer, ChecklistTemplate> active = new LinkedHashMap<>();
        Map<Integer, ChecklistTemplate> evidence = new LinkedHashMap<>();
        String reason = null;
        for (ChecklistTemplate template : approved) {
            int position = position(template);
            if (position > MAX_SEQUENCE_POSITION) {
                reason = "POSITION_OUT_OF_RANGE";
                continue;
            }
            if (position <= 0) {
                continue;
            }
            if (active.putIfAbsent(position, template) != null) {
                reason = "DUPLICATE_POSITION";
            }
            evidence.putIfAbsent(position, template);
        }
        for (ChecklistTemplate template : archived) {
            int position = position(template);
            if (position > MAX_SEQUENCE_POSITION) {
                reason = "POSITION_OUT_OF_RANGE";
                continue;
            }
            if (position > 0) {
                evidence.putIfAbsent(position, template);
            }
        }
        // A positive chain cannot safely coexist with an active position-zero mandatory
        // candidate: reconciliation would otherwise materialize a legacy instance beside
        // the sequence and the user's cohort could not be determined deterministically.
        boolean activeLegacyCandidate = templateRepository
                .findAllDistributionEnabledByStageAndStatus(ContentStage.PRE_PREGNANCY,
                        ChecklistTemplateStatus.APPROVED).stream()
                .filter(this::isMotherPreconceptionCandidate)
                .anyMatch(template -> position(template) <= 0);
        if (!active.isEmpty() && activeLegacyCandidate) {
            reason = "LEGACY_TEMPLATE_MIXED";
        }
        if (!evidence.isEmpty()) {
            int max = evidence.keySet().stream().max(Integer::compareTo).orElse(0);
            for (int expected = 1; expected <= max; expected++) {
                if (!evidence.containsKey(expected)) {
                    reason = "POSITION_GAP";
                    break;
                }
                if (!active.containsKey(expected)) {
                    reason = "POSITION_NOT_ACTIVE";
                    break;
                }
            }
        }
        if (itemRepository != null) {
            for (ChecklistTemplate template : active.values()) {
                boolean hasRequired = itemRepository.findByTemplate_IdOrderByOrder(template.getId()).stream()
                        .anyMatch(item -> Boolean.TRUE.equals(item.getIsRequired()));
                if (!hasRequired) {
                    reason = "REQUIRED_ITEM_MISSING";
                    break;
                }
            }
        }
        Set<UUID> versions = java.util.stream.Stream.concat(approved.stream(), archived.stream())
                .map(ChecklistTemplate::getTemplateVersionId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        return new Chain(active, evidence, versions, reason == null,
                reason == null ? null : reason);
    }

    private TodaySequenceProjection projection(
            ChecklistInstance current,
            ChecklistTemplate currentTemplate,
            int currentPosition,
            Chain chain,
            int qualifiedPositions,
            ChecklistSequenceState state,
            boolean advanceAvailable,
            ChecklistTemplate successor,
            boolean sequenceComplete,
            String blockedReason) {
        return new TodaySequenceProjection(state, current.getId(), current.getTemplateVersionId(),
                currentTemplate == null ? null : currentTemplate.getName(), currentPosition,
                chain.totalPositions(), qualifiedPositions, advanceAvailable,
                successor == null ? null : new TodaySequenceNextSet(successor.getName(), position(successor)),
                sequenceComplete, blockedReason);
    }

    private TodaySequenceProjection blocked(String reason) {
        return new TodaySequenceProjection(ChecklistSequenceState.CONFIGURATION_BLOCKED,
                null, null, null, null, null, null, false, null, false,
                reason == null ? CONFIGURATION_BLOCKED : reason);
    }

    private static TodaySequenceNextSet next(Chain chain, int position) {
        ChecklistTemplate next = chain.activeByPosition().get(position);
        return next == null ? null : new TodaySequenceNextSet(next.getName(), position(next));
    }

    private ChecklistTemplate template(UUID versionId) {
        return versionId == null ? null : templateRepository.findByTemplateVersionId(versionId).orElse(null);
    }

    private boolean isSequenceTemplate(ChecklistTemplate template) {
        return isSequenceScopeTemplate(template)
                && Boolean.TRUE.equals(template.getDistributionEnabled())
                && template.getSequencePosition() != null && template.getSequencePosition() > 0;
    }

    private boolean isSequenceEvidenceTemplate(ChecklistTemplate template) {
        return isSequenceScopeTemplate(template)
                && template.getSequencePosition() != null && template.getSequencePosition() > 0;
    }

    private boolean isSequenceScopeTemplate(ChecklistTemplate template) {
        return template != null && template.getStage() == ContentStage.PRE_PREGNANCY
                && template.getTemplateType() == ChecklistTemplateType.MANDATORY
                && template.getRecipientScope() == com.carebridge.backend.checklist.model.ChecklistRecipientScope.MOTHER;
    }

    private boolean isMotherPreconceptionCandidate(ChecklistTemplate template) {
        return template != null
                && template.getStage() == ContentStage.PRE_PREGNANCY
                && template.getTemplateType() == ChecklistTemplateType.MANDATORY
                && Boolean.TRUE.equals(template.getDistributionEnabled())
                && (template.getRecipientScope()
                        == com.carebridge.backend.checklist.model.ChecklistRecipientScope.MOTHER
                    || template.getRecipientScope()
                        == com.carebridge.backend.checklist.model.ChecklistRecipientScope.BOTH);
    }

    private boolean isPersonalSystemInstance(ChecklistInstance instance) {
        return instance.getRecipientRole() == ChecklistRecipientRole.MOTHER
                && instance.getOrigin() == ChecklistOrigin.SYSTEM_TEMPLATE
                && instance.getCareContextType() == ChecklistCareContextType.JOURNEY;
    }

    private static int position(ChecklistTemplate template) {
        return template == null || template.getSequencePosition() == null ? 0 : template.getSequencePosition();
    }

    public record Chain(
            Map<Integer, ChecklistTemplate> activeByPosition,
            Map<Integer, ChecklistTemplate> positions,
            Set<UUID> versionIds,
            boolean valid,
            String reason) {
        public int totalPositions() {
            return positions.keySet().stream().max(Integer::compareTo).orElse(0);
        }
    }
}
