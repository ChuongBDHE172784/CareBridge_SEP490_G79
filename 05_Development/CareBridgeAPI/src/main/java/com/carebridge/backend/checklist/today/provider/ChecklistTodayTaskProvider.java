package com.carebridge.backend.checklist.today.provider;

import com.carebridge.backend.checklist.entity.ChecklistInstance;
import com.carebridge.backend.checklist.entity.ChecklistTaskInstance;
import com.carebridge.backend.checklist.model.ChecklistInstanceStatus;
import com.carebridge.backend.checklist.model.ChecklistRecipientRole;
import com.carebridge.backend.checklist.model.ChecklistTaskStatus;
import com.carebridge.backend.checklist.policy.ChecklistTemplateVisibilityPolicy;
import com.carebridge.backend.checklist.repository.ChecklistInstanceRepository;
import com.carebridge.backend.checklist.repository.ChecklistTaskInstanceRepository;
import com.carebridge.backend.checklist.sequence.ChecklistSequenceResolver;
import com.carebridge.backend.checklist.distribution.ChecklistCurrentScopePolicy;
import com.carebridge.backend.checklist.today.dto.TodaySequenceProjection;
import com.carebridge.backend.checklist.today.dto.TodayTaskCandidate;
import com.carebridge.backend.checklist.today.model.TaskAction;
import com.carebridge.backend.checklist.today.model.TaskKind;
import com.carebridge.backend.checklist.today.policy.UnifiedTaskAccessPolicy;
import com.carebridge.backend.content.entity.ChecklistTemplate;
import com.carebridge.backend.content.entity.ChecklistTemplateType;
import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.repository.ChecklistTemplateRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ChecklistTodayTaskProvider implements TodayTaskProvider {
    private final ChecklistInstanceRepository instanceRepository;
    private final ChecklistTaskInstanceRepository taskRepository;
    private final UnifiedTaskAccessPolicy accessPolicy;
    private final ChecklistSequenceResolver sequenceResolver;
    private final ChecklistTemplateRepository templateRepository;
    private final ChecklistCurrentScopePolicy currentScopePolicy;

    public ChecklistTodayTaskProvider(
            ChecklistInstanceRepository instanceRepository,
            ChecklistTaskInstanceRepository taskRepository,
            UnifiedTaskAccessPolicy accessPolicy) {
        this(instanceRepository, taskRepository, accessPolicy, null, null, null);
    }

    public ChecklistTodayTaskProvider(
            ChecklistInstanceRepository instanceRepository,
            ChecklistTaskInstanceRepository taskRepository,
            UnifiedTaskAccessPolicy accessPolicy,
            ChecklistSequenceResolver sequenceResolver,
            ChecklistTemplateRepository templateRepository) {
        this(instanceRepository, taskRepository, accessPolicy, sequenceResolver,
                templateRepository, null);
    }

    @Autowired
    public ChecklistTodayTaskProvider(
            ChecklistInstanceRepository instanceRepository,
            ChecklistTaskInstanceRepository taskRepository,
            UnifiedTaskAccessPolicy accessPolicy,
            ChecklistSequenceResolver sequenceResolver,
            ChecklistTemplateRepository templateRepository,
            ChecklistCurrentScopePolicy currentScopePolicy) {
        this.instanceRepository = instanceRepository;
        this.taskRepository = taskRepository;
        this.accessPolicy = accessPolicy;
        this.sequenceResolver = sequenceResolver;
        this.templateRepository = templateRepository;
        this.currentScopePolicy = currentScopePolicy;
    }

    @Override
    public TaskKind taskKind() {
        return TaskKind.CHECKLIST;
    }

    @Override
    public boolean supportsDateAwareRead() {
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TodayTaskCandidate> findAuthorizedTasks(UUID actorUserId) {
        return findAuthorizedTasks(actorUserId, null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TodayTaskCandidate> findAuthorizedTasks(
            UUID actorUserId, LocalDate effectiveDate, ZoneId zone) {
        TodaySequenceProjection sequence = sequenceResolver == null
                ? null : sequenceResolver.resolve(actorUserId);
        List<ChecklistInstance> currentInstances = instanceRepository
                .findByRecipientUserIdAndHistoricalAtIsNull(actorUserId);
        Map<UUID, ChecklistTemplate> templatesByVersion = templatesByVersion(currentInstances);
        List<AuthorizedInstance> authorizedInstances = new ArrayList<>();
        for (var instance : currentInstances) {
            if (instance.getStatus() != ChecklistInstanceStatus.CANCELLED
                    && !Boolean.FALSE.equals(instance.getWasActionable())
                    && isCurrentRead(instance, effectiveDate)
                    && isVisibleTemplate(instance, templatesByVersion)
                    && isVisibleSequenceInstance(instance, sequence, templatesByVersion)
                    && accessPolicy.canView(instance, actorUserId)) {
                authorizedInstances.add(new AuthorizedInstance(
                        instance, accessPolicy.canComplete(instance, actorUserId)));
            }
        }
        if (authorizedInstances.isEmpty()) {
            return List.of();
        }

        List<UUID> instanceIds = authorizedInstances.stream()
                .map(authorized -> authorized.instance().getId())
                .toList();
        Map<UUID, List<ChecklistTaskInstance>> tasksByInstanceId = new HashMap<>();
        for (var task : taskRepository.findAllByChecklistInstanceIds(instanceIds)) {
            tasksByInstanceId.computeIfAbsent(task.getChecklistInstanceId(), ignored -> new ArrayList<>())
                    .add(task);
        }

        List<TodayTaskCandidate> result = new ArrayList<>();
        for (var authorized : authorizedInstances) {
            ChecklistInstance instance = authorized.instance();
            for (var task : tasksByInstanceId.getOrDefault(instance.getId(), List.of())) {
                if (task.getStatus() == ChecklistTaskStatus.CANCELLED) {
                    continue;
                }
                Set<TaskAction> actions = EnumSet.noneOf(TaskAction.class);
                if (authorized.canAct() && (task.getStatus() == ChecklistTaskStatus.PENDING
                        || task.getStatus() == ChecklistTaskStatus.IN_PROGRESS)) {
                    actions.add(TaskAction.COMPLETE);
                } else if (authorized.canAct() && task.getStatus() == ChecklistTaskStatus.COMPLETED) {
                    actions.add(TaskAction.REOPEN);
                }
                UUID presentedCareGroupId = instance.getRecipientRole()
                                == ChecklistRecipientRole.MOTHER
                        ? null
                        : instance.getCareGroupId();
                result.add(new TodayTaskCandidate(TaskKind.CHECKLIST, task.getId(), instance.getId(),
                        instance.getTemplateVersionId(), presentedCareGroupId,
                        instance.getCareContextType(), instance.getCareContextId(),
                        task.getTitleSnapshot(), task.getTargetSubject(), instance.getOrigin(),
                        task.getStatus().name(), actions, task.getDueAt(), terminalAt(task), null,
                        task.getDescriptionSnapshot(), task.getSupportFunction()));
            }
        }
        return List.copyOf(result);
    }

    private boolean isCurrentRead(ChecklistInstance instance, LocalDate effectiveDate) {
        return effectiveDate == null
                || currentScopePolicy == null
                || !currentScopePolicy.isHistoryManaged(instance)
                || currentScopePolicy.isCurrent(instance, effectiveDate);
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

    private boolean isVisibleTemplate(
            ChecklistInstance instance,
            Map<UUID, ChecklistTemplate> templatesByVersion) {
        if (templateRepository == null) {
            // Preserve the lightweight compatibility constructor used by callers
            // that do not provide template metadata.
            return true;
        }
        UUID templateVersionId = instance.getTemplateVersionId();
        ChecklistTemplate template = templateVersionId == null
                ? null
                : templatesByVersion.get(templateVersionId);
        return ChecklistTemplateVisibilityPolicy.isVisible(instance, template);
    }

    private boolean isVisibleSequenceInstance(
            ChecklistInstance instance,
            TodaySequenceProjection sequence,
            Map<UUID, ChecklistTemplate> templatesByVersion) {
        if (sequence == null || !isSequenceInstance(instance, templatesByVersion)) {
            return true;
        }
        // A blocked projection deliberately has no current instance, so all sequence
        // tasks are hidden rather than leaking a locked/future set.
        return sequence.currentInstanceId() != null
                && sequence.currentInstanceId().equals(instance.getId());
    }

    private boolean isSequenceInstance(
            ChecklistInstance instance,
            Map<UUID, ChecklistTemplate> templatesByVersion) {
        if (templateRepository == null || instance.getTemplateVersionId() == null) {
            return false;
        }
        ChecklistTemplate template = templatesByVersion.get(instance.getTemplateVersionId());
        return template != null
                && template.getStage() == ContentStage.PRE_PREGNANCY
                && template.getTemplateType() == ChecklistTemplateType.MANDATORY
                && template.getRecipientScope()
                    == com.carebridge.backend.checklist.model.ChecklistRecipientScope.MOTHER
                && template.getSequencePosition() != null
                && template.getSequencePosition() > 0
                && instance.getRecipientRole() == ChecklistRecipientRole.MOTHER
                && instance.getOrigin() == com.carebridge.backend.checklist.model.ChecklistOrigin.SYSTEM_TEMPLATE;
    }

    private static Instant terminalAt(ChecklistTaskInstance task) {
        if (task.getCompletedAt() != null) {
            return task.getCompletedAt();
        }
        if (task.getSkippedAt() != null) {
            return task.getSkippedAt();
        }
        return task.getCancelledAt();
    }

    private record AuthorizedInstance(ChecklistInstance instance, boolean canAct) {
    }
}
