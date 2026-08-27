package com.carebridge.backend.checklist.today.provider;

import com.carebridge.backend.checklist.model.ChecklistCareContextType;
import com.carebridge.backend.checklist.today.dto.TodayTaskCandidate;
import com.carebridge.backend.checklist.today.model.TaskAction;
import com.carebridge.backend.checklist.today.model.TaskKind;
import com.carebridge.backend.family.entity.CareTaskStatus;
import com.carebridge.backend.family.entity.CareGroupStatus;
import com.carebridge.backend.family.entity.PermissionFlag;
import com.carebridge.backend.family.policy.CareGroupAuthorizationPolicy;
import com.carebridge.backend.family.repository.CareGroupRepository;
import com.carebridge.backend.family.repository.CareTaskRepository;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CareTaskTodayTaskProvider implements TodayTaskProvider {
    private final CareTaskRepository taskRepository;
    private final CareGroupRepository groupRepository;
    private final CareGroupAuthorizationPolicy authorizationPolicy;

    @Override
    public TaskKind taskKind() {
        return TaskKind.CARE_TASK;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TodayTaskCandidate> findAuthorizedTasks(UUID actorUserId) {
        List<com.carebridge.backend.family.entity.CareTask> tasks = taskRepository.findByAssignedTo(actorUserId);
        Set<UUID> groupIds = tasks.stream()
                .map(com.carebridge.backend.family.entity.CareTask::getCareGroupId)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        Map<UUID, com.carebridge.backend.family.entity.CareGroup> groupsById = new HashMap<>();
        groupRepository.findAllById(groupIds).forEach(group -> groupsById.put(group.getId(), group));

        Map<UUID, GroupAccess> accessByGroupId = new HashMap<>();
        for (var group : groupsById.values()) {
            boolean owner = actorUserId.equals(group.getOwnerUserId());
            boolean canView = owner || authorizationPolicy.hasPermission(
                    group.getId(), actorUserId, PermissionFlag.CHECKLIST_VIEW);
            boolean canComplete = canView && (owner || authorizationPolicy.hasPermission(
                    group.getId(), actorUserId, PermissionFlag.CHECKLIST_COMPLETE));
            accessByGroupId.put(group.getId(), new GroupAccess(canView, canComplete));
        }

        return tasks.stream()
                .map(task -> toCandidate(task, groupsById.get(task.getCareGroupId()),
                        accessByGroupId.get(task.getCareGroupId())))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private TodayTaskCandidate toCandidate(com.carebridge.backend.family.entity.CareTask task,
                                           com.carebridge.backend.family.entity.CareGroup group,
                                           GroupAccess access) {
        if (group == null || access == null || !access.canView()
                || group.getStatus() != CareGroupStatus.ACTIVE || !hasExplicitCurrentContext(task, group)) {
            return null;
        }
        UUID contextId = task.getBabyId() != null ? task.getBabyId() : task.getJourneyId();
        ChecklistCareContextType contextType = task.getBabyId() != null
                ? ChecklistCareContextType.BABY : ChecklistCareContextType.JOURNEY;
        if (contextId == null || task.getOrigin() == null || task.getTargetSubject() == null) {
            return null;
        }
        Set<TaskAction> actions = EnumSet.noneOf(TaskAction.class);
        if (access.canComplete() && (task.getStatus() == CareTaskStatus.OPEN
                || task.getStatus() == CareTaskStatus.IN_PROGRESS
                || task.getStatus() == CareTaskStatus.NEEDS_SUPPORT)) {
            actions.add(TaskAction.COMPLETE);
        }
        return new TodayTaskCandidate(TaskKind.CARE_TASK, task.getId(), null, null,
                group.getId(), contextType, contextId, task.getTitle(), task.getTargetSubject(),
                task.getOrigin(), normalize(task.getStatus()), actions, task.getDueAt());
    }

    private static boolean hasExplicitCurrentContext(
            com.carebridge.backend.family.entity.CareTask task,
            com.carebridge.backend.family.entity.CareGroup group) {
        boolean hasJourney = task.getJourneyId() != null;
        boolean hasBaby = task.getBabyId() != null;
        if (hasJourney == hasBaby) {
            return false;
        }
        return hasJourney
                ? task.getJourneyId().equals(group.getLinkedJourneyId())
                : task.getBabyId().equals(group.getLinkedBabyProfileId());
    }

    private static String normalize(CareTaskStatus status) {
        return switch (status) {
            case OPEN -> "PENDING";
            case IN_PROGRESS, NEEDS_SUPPORT -> "IN_PROGRESS";
            case DONE -> "COMPLETED";
            case CANCELLED -> "CANCELLED";
        };
    }

    private record GroupAccess(boolean canView, boolean canComplete) {
    }
}
