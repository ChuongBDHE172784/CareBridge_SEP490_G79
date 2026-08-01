package com.carebridge.backend.checklist.today.provider;

import com.carebridge.backend.checklist.entity.ChecklistInstance;
import com.carebridge.backend.checklist.entity.ChecklistTaskInstance;
import com.carebridge.backend.checklist.model.ChecklistInstanceStatus;
import com.carebridge.backend.checklist.model.ChecklistRecipientRole;
import com.carebridge.backend.checklist.model.ChecklistTaskStatus;
import com.carebridge.backend.checklist.repository.ChecklistInstanceRepository;
import com.carebridge.backend.checklist.repository.ChecklistTaskInstanceRepository;
import com.carebridge.backend.checklist.today.dto.TodayTaskCandidate;
import com.carebridge.backend.checklist.today.model.TaskAction;
import com.carebridge.backend.checklist.today.model.TaskKind;
import com.carebridge.backend.checklist.today.policy.UnifiedTaskAccessPolicy;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ChecklistTodayTaskProvider implements TodayTaskProvider {
    private final ChecklistInstanceRepository instanceRepository;
    private final ChecklistTaskInstanceRepository taskRepository;
    private final UnifiedTaskAccessPolicy accessPolicy;

    @Override
    public TaskKind taskKind() {
        return TaskKind.CHECKLIST;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TodayTaskCandidate> findAuthorizedTasks(UUID actorUserId) {
        List<AuthorizedInstance> authorizedInstances = new ArrayList<>();
        for (var instance : instanceRepository.findByRecipientUserIdAndHistoricalAtIsNull(actorUserId)) {
            if (instance.getStatus() != ChecklistInstanceStatus.CANCELLED
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
