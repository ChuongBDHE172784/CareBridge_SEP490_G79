package com.carebridge.backend.checklist.history.service;

import com.carebridge.backend.baby.entity.BabyProfile;
import com.carebridge.backend.baby.repository.BabyProfileRepository;
import com.carebridge.backend.checklist.distribution.ChecklistHistoryReconciliationService;
import com.carebridge.backend.checklist.entity.ChecklistInstance;
import com.carebridge.backend.checklist.entity.ChecklistTaskInstance;
import com.carebridge.backend.checklist.history.dto.ChecklistHistoryItemResponse;
import com.carebridge.backend.checklist.history.dto.ChecklistHistoryPageResponse;
import com.carebridge.backend.checklist.history.dto.ChecklistHistoryTaskResponse;
import com.carebridge.backend.checklist.model.ChecklistCareContextType;
import com.carebridge.backend.checklist.model.ChecklistTargetSubject;
import com.carebridge.backend.checklist.policy.ChecklistTemplateVisibilityPolicy;
import com.carebridge.backend.checklist.repository.ChecklistInstanceRepository;
import com.carebridge.backend.checklist.repository.ChecklistTaskInstanceRepository;
import com.carebridge.backend.checklist.today.policy.CareGroupChecklistScopeResolver;
import com.carebridge.backend.checklist.today.policy.CareGroupChecklistScopeResolver.CareGroupChecklistScope;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.content.entity.ChecklistTemplate;
import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.repository.ChecklistTemplateRepository;
import com.carebridge.backend.journey.entity.MotherJourney;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChecklistHistoryService {

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;

    private final ChecklistHistoryReconciliationService reconciliationService;
    private final ChecklistInstanceRepository instanceRepository;
    private final ChecklistTaskInstanceRepository taskRepository;
    private final ChecklistTemplateRepository templateRepository;
    private final MotherJourneyRepository journeyRepository;
    private final BabyProfileRepository babyRepository;
    private final CareGroupChecklistScopeResolver scopeResolver;
    private final Clock clock;

    @Autowired
    public ChecklistHistoryService(
            ChecklistHistoryReconciliationService reconciliationService,
            ChecklistInstanceRepository instanceRepository,
            ChecklistTaskInstanceRepository taskRepository,
            ChecklistTemplateRepository templateRepository,
            MotherJourneyRepository journeyRepository,
            BabyProfileRepository babyRepository,
            CareGroupChecklistScopeResolver scopeResolver) {
        this(reconciliationService, instanceRepository, taskRepository, templateRepository,
                journeyRepository, babyRepository, scopeResolver, Clock.systemUTC());
    }

    public ChecklistHistoryService(
            ChecklistHistoryReconciliationService reconciliationService,
            ChecklistInstanceRepository instanceRepository,
            ChecklistTaskInstanceRepository taskRepository,
            ChecklistTemplateRepository templateRepository,
            MotherJourneyRepository journeyRepository,
            BabyProfileRepository babyRepository) {
        this(reconciliationService, instanceRepository, taskRepository, templateRepository,
                journeyRepository, babyRepository, null, Clock.systemUTC());
    }

    public ChecklistHistoryService(
            ChecklistHistoryReconciliationService reconciliationService,
            ChecklistInstanceRepository instanceRepository,
            ChecklistTaskInstanceRepository taskRepository,
            ChecklistTemplateRepository templateRepository,
            MotherJourneyRepository journeyRepository,
            BabyProfileRepository babyRepository,
            Clock clock) {
        this(reconciliationService, instanceRepository, taskRepository, templateRepository,
                journeyRepository, babyRepository, null, clock);
    }

    public ChecklistHistoryService(
            ChecklistHistoryReconciliationService reconciliationService,
            ChecklistInstanceRepository instanceRepository,
            ChecklistTaskInstanceRepository taskRepository,
            ChecklistTemplateRepository templateRepository,
            MotherJourneyRepository journeyRepository,
            BabyProfileRepository babyRepository,
            CareGroupChecklistScopeResolver scopeResolver,
            Clock clock) {
        this.reconciliationService = reconciliationService;
        this.instanceRepository = instanceRepository;
        this.taskRepository = taskRepository;
        this.templateRepository = templateRepository;
        this.journeyRepository = journeyRepository;
        this.babyRepository = babyRepository;
        this.scopeResolver = scopeResolver;
        this.clock = clock;
    }

    @Transactional
    public ChecklistHistoryPageResponse listHistory(
            UUID ownerUserId,
            ChecklistTargetSubject targetSubject,
            int page,
            int size) {
        int safePage = Math.max(0, page);
        int safeSize = clampSize(size);
        // History is a read projection.  Lifecycle closure and catch-up are
        // performed by the app-independent repair job (and the outcome event
        // listener); opening or date-browsing this endpoint must not create or
        // mutate occurrences as a side effect.

        Page<ChecklistInstance> historyPage = instanceRepository.findOwnerHistory(
                ownerUserId, targetSubject, PageRequest.of(safePage, safeSize));
        return materialize(ownerUserId, targetSubject, historyPage);
    }

    @Transactional
    public ChecklistHistoryPageResponse listSharedHistory(
            UUID actorUserId,
            UUID careGroupId,
            ChecklistTargetSubject targetSubject,
            int page,
            int size) {
        if (scopeResolver == null) {
            throw new IllegalStateException("Care-group checklist scope resolver is unavailable");
        }
        CareGroupChecklistScope scope = scopeResolver.resolveViewForUpdate(actorUserId, careGroupId);
        if (scope == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "CHECKLIST_NOT_FOUND",
                    "Checklist scope not found");
        }
        int safePage = Math.max(0, page);
        int safeSize = clampSize(size);
        // Family History is recipient-owned and epoch-bound.  Do not project
        // the Mother's historical rows into this scope: a later grant must not
        // regain access to work created before the member's current VIEW epoch.
        Page<ChecklistInstance> historyPage = instanceRepository.findFamilyHistory(
                actorUserId, careGroupId, scope.ownerUserId(), scope.linkedJourneyId(),
                scope.linkedBabyProfileId(), targetSubject, PageRequest.of(safePage, safeSize));
        CareGroupChecklistScope currentScope = scopeResolver.resolveViewForUpdate(actorUserId, careGroupId);
        if (currentScope == null
                || !scope.ownerUserId().equals(currentScope.ownerUserId())
                || !scope.linkedContexts().equals(currentScope.linkedContexts())) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "CHECKLIST_NOT_FOUND",
                    "Checklist scope not found");
        }
        return materialize(scope.ownerUserId(), targetSubject, historyPage);
    }

    private ChecklistHistoryPageResponse materialize(
            UUID ownerUserId,
            ChecklistTargetSubject targetSubject,
            Page<ChecklistInstance> historyPage) {
        List<ChecklistInstance> fetchedInstances = historyPage.getContent();
        Map<UUID, ChecklistTemplate> fetchedTemplates = templatesByVersion(fetchedInstances);
        List<ChecklistInstance> instances = fetchedInstances.stream()
                .filter(instance -> isVisibleTemplate(instance, fetchedTemplates))
                .toList();
        if (instances.isEmpty()) {
            return new ChecklistHistoryPageResponse(
                    List.of(), historyPage.getNumber(), historyPage.getSize(),
                    historyPage.getTotalElements(), historyPage.getTotalPages());
        }

        Map<UUID, List<ChecklistTaskInstance>> tasksByInstance = tasksByInstance(instances, targetSubject);
        Map<UUID, ChecklistTemplate> templatesByVersion = fetchedTemplates;
        Map<UUID, String> journeyLabels = journeyLabels(ownerUserId, instances);
        Map<UUID, String> babyLabels = babyLabels(ownerUserId, instances);

        List<ChecklistHistoryItemResponse> items = instances.stream()
                .map(instance -> toResponse(
                        instance,
                        tasksByInstance.getOrDefault(instance.getId(), List.of()),
                        instance.getTemplateVersionId() == null
                                ? null
                                : templatesByVersion.get(instance.getTemplateVersionId()),
                        journeyLabels,
                        babyLabels,
                        targetSubject))
                .toList();
        return new ChecklistHistoryPageResponse(
                items,
                historyPage.getNumber(),
                historyPage.getSize(),
                historyPage.getTotalElements(),
                historyPage.getTotalPages());
    }

    private Map<UUID, List<ChecklistTaskInstance>> tasksByInstance(
            List<ChecklistInstance> instances,
            ChecklistTargetSubject targetSubject) {
        List<UUID> instanceIds = instances.stream().map(ChecklistInstance::getId).toList();
        return taskRepository.findAllByChecklistInstanceIds(instanceIds).stream()
                .filter(task -> targetSubject == null || task.getTargetSubject() == targetSubject)
                .collect(Collectors.groupingBy(
                        ChecklistTaskInstance::getChecklistInstanceId,
                        LinkedHashMap::new,
                        Collectors.collectingAndThen(Collectors.toList(), tasks -> tasks.stream()
                                .sorted(Comparator.comparing(
                                                ChecklistTaskInstance::getDisplayOrder,
                                                Comparator.nullsLast(Comparator.naturalOrder()))
                                        .thenComparing(ChecklistTaskInstance::getId))
                                .toList())));
    }

    private Map<UUID, ChecklistTemplate> templatesByVersion(List<ChecklistInstance> instances) {
        Set<UUID> templateVersionIds = instances.stream()
                .map(ChecklistInstance::getTemplateVersionId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (templateVersionIds.isEmpty()) {
            return Map.of();
        }
        return templateRepository.findAllByTemplateVersionIdIn(templateVersionIds).stream()
                .collect(Collectors.toMap(ChecklistTemplate::getTemplateVersionId, Function.identity(),
                        (left, right) -> left));
    }

    private boolean isVisibleTemplate(
            ChecklistInstance instance,
            Map<UUID, ChecklistTemplate> templatesByVersion) {
        UUID templateVersionId = instance.getTemplateVersionId();
        ChecklistTemplate template = templateVersionId == null
                ? null
                : templatesByVersion.get(templateVersionId);
        return ChecklistTemplateVisibilityPolicy.isVisible(instance, template);
    }

    private Map<UUID, String> journeyLabels(UUID ownerUserId, Collection<ChecklistInstance> instances) {
        Set<UUID> journeyIds = contextIds(instances, ChecklistCareContextType.JOURNEY);
        if (journeyIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, String> labels = new LinkedHashMap<>();
        StreamSupport.stream(journeyRepository.findAllById(journeyIds).spliterator(), false)
                .filter(journey -> ownerUserId.equals(journey.getOwnerUserId()))
                .forEach(journey -> labels.put(journey.getId(),
                        journey.getJourneyType() == null ? null : journey.getJourneyType().name()));
        return labels;
    }

    private Map<UUID, String> babyLabels(UUID ownerUserId, Collection<ChecklistInstance> instances) {
        Set<UUID> babyIds = contextIds(instances, ChecklistCareContextType.BABY);
        if (babyIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, String> labels = new LinkedHashMap<>();
        StreamSupport.stream(babyRepository.findAllById(babyIds).spliterator(), false)
                .filter(baby -> ownerUserId.equals(baby.getOwnerUserId()))
                .forEach(baby -> labels.put(baby.getId(), baby.getNickname()));
        return labels;
    }

    private static Set<UUID> contextIds(
            Collection<ChecklistInstance> instances,
            ChecklistCareContextType contextType) {
        return instances.stream()
                .filter(instance -> instance.getCareContextType() == contextType)
                .map(ChecklistInstance::getCareContextId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private ChecklistHistoryItemResponse toResponse(
            ChecklistInstance instance,
            List<ChecklistTaskInstance> tasks,
            ChecklistTemplate template,
            Map<UUID, String> journeyLabels,
            Map<UUID, String> babyLabels,
            ChecklistTargetSubject filterSubject) {
        List<ChecklistHistoryTaskResponse> taskResponses = tasks.stream()
                .map(task -> new ChecklistHistoryTaskResponse(
                        task.getId(),
                        task.getTitleSnapshot(),
                        task.getStatus().name(),
                        task.getCompletedAt(),
                        task.getSkippedAt(),
                        task.getCancelledAt(),
                        task.getDisplayOrder(),
                        Boolean.TRUE.equals(task.getRequired())))
                .toList();
        ChecklistTargetSubject groupSubject = filterSubject == null ? commonSubject(tasks) : filterSubject;
        return new ChecklistHistoryItemResponse(
                instance.getId(),
                instance.getTemplateVersionId(),
                template == null ? null : template.getName(),
                displayStage(instance, template),
                groupSubject == null ? null : groupSubject.name(),
                instance.getCareContextType() == null ? null : instance.getCareContextType().name(),
                instance.getCareContextId(),
                contextLabel(instance, journeyLabels, babyLabels),
                instance.getWindowStart(),
                instance.getWindowEnd(),
                instance.getHistoricalAt(),
                instance.getHistoryReasonCode(),
                taskResponses);
    }

    private static ChecklistTargetSubject commonSubject(List<ChecklistTaskInstance> tasks) {
        ChecklistTargetSubject subject = null;
        for (ChecklistTaskInstance task : tasks) {
            if (subject == null) {
                subject = task.getTargetSubject();
            } else if (subject != task.getTargetSubject()) {
                return null;
            }
        }
        return subject;
    }

    private static String displayStage(ChecklistInstance instance, ChecklistTemplate template) {
        if (template == null || template.getStage() == null) {
            return null;
        }
        if (instance.getCareContextType() == ChecklistCareContextType.BABY
                && template.getStage() == ContentStage.POSTPARTUM) {
            return "BABY_CARE";
        }
        return template.getStage().name();
    }

    private static String contextLabel(
            ChecklistInstance instance,
            Map<UUID, String> journeyLabels,
            Map<UUID, String> babyLabels) {
        if (instance.getCareContextType() == ChecklistCareContextType.JOURNEY) {
            return journeyLabels.get(instance.getCareContextId());
        }
        if (instance.getCareContextType() == ChecklistCareContextType.BABY) {
            return babyLabels.get(instance.getCareContextId());
        }
        return null;
    }

    private static int clampSize(int size) {
        if (size <= 0) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

}
