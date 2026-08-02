package com.carebridge.backend.checklist.today.provider;

import com.carebridge.backend.checklist.entity.ChecklistInstance;
import com.carebridge.backend.checklist.entity.ChecklistTaskInstance;
import com.carebridge.backend.checklist.model.ChecklistInstanceStatus;
import com.carebridge.backend.checklist.model.ChecklistRecipientRole;
import com.carebridge.backend.checklist.model.ChecklistTaskStatus;
import com.carebridge.backend.checklist.repository.ChecklistInstanceRepository;
import com.carebridge.backend.checklist.repository.ChecklistTaskInstanceRepository;
import com.carebridge.backend.checklist.sequence.ChecklistSequenceResolver;
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
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
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

    public ChecklistTodayTaskProvider(
            ChecklistInstanceRepository instanceRepository,
            ChecklistTaskInstanceRepository taskRepository,
            UnifiedTaskAccessPolicy accessPolicy) {
        this(instanceRepository, taskRepository, accessPolicy, null, null);
    }

    @Autowired
    public ChecklistTodayTaskProvider(
            ChecklistInstanceRepository instanceRepository,
            ChecklistTaskInstanceRepository taskRepository,
            UnifiedTaskAccessPolicy accessPolicy,
            ChecklistSequenceResolver sequenceResolver,
            ChecklistTemplateRepository templateRepository) {
        this.instanceRepository = instanceRepository;
        this.taskRepository = taskRepository;
        this.accessPolicy = accessPolicy;
        this.sequenceResolver = sequenceResolver;
        this.templateRepository = templateRepository;
    }

    @Override
    public TaskKind taskKind() {
        return TaskKind.CHECKLIST;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TodayTaskCandidate> findAuthorizedTasks(UUID actorUserId) {
        TodaySequenceProjection sequence = sequenceResolver == null
                ? null : sequenceResolver.resolve(actorUserId);
        List<AuthorizedInstance> authorizedInstances = new ArrayList<>();
        for (var instance : instanceRepository.findByRecipientUserIdAndHistoricalAtIsNull(actorUserId)) {
            if (instance.getStatus() != ChecklistInstanceStatus.CANCELLED
                    && isVisibleSequenceInstance(instance, sequence)
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
                        task.getStatus().name(), actions, task.getDueAt(), terminalAt(task)));
            }
        }
        return List.copyOf(result);
    }

    private boolean isVisibleSequenceInstance(
            ChecklistInstance instance, TodaySequenceProjection sequence) {
        if (sequence == null || !isSequenceInstance(instance)) {
            return true;
        }
        // A blocked projection deliberately has no current instance, so all sequence
        // tasks are hidden rather than leaking a locked/future set.
        return sequence.currentInstanceId() != null
                && sequence.currentInstanceId().equals(instance.getId());
    }

    private boolean isSequenceInstance(ChecklistInstance instance) {
        if (templateRepository == null || instance.getTemplateVersionId() == null) {
            return false;
        }
        ChecklistTemplate template = templateRepository.findByTemplateVersionId(
                instance.getTemplateVersionId()).orElse(null);
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
