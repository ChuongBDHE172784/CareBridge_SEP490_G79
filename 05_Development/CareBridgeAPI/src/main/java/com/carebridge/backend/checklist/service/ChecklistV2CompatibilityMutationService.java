package com.carebridge.backend.checklist.service;

import com.carebridge.backend.checklist.model.ChecklistOrigin;
import com.carebridge.backend.checklist.repository.ChecklistInstanceRepository;
import com.carebridge.backend.checklist.repository.ChecklistTaskInstanceRepository;
import com.carebridge.backend.checklist.today.policy.UnifiedTaskAccessPolicy;
import com.carebridge.backend.checklist.today.policy.UnifiedTaskMutationPolicy;
import com.carebridge.backend.common.exception.BusinessException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Fail-closed adapter for obsolete edit/delete routes without any legacy persistence. */
@Service
@RequiredArgsConstructor
public class ChecklistV2CompatibilityMutationService {
    private final ChecklistTaskInstanceRepository taskRepository;
    private final ChecklistInstanceRepository instanceRepository;
    private final UnifiedTaskAccessPolicy accessPolicy;
    private final UnifiedTaskMutationPolicy mutationPolicy;

    @Transactional(readOnly = true)
    public void rejectUpdate(UUID taskId, UUID actorUserId) {
        rejectMutation(taskId, actorUserId);
    }

    @Transactional(readOnly = true)
    public void rejectDelete(UUID taskId, UUID actorUserId) {
        rejectMutation(taskId, actorUserId);
    }

    private void rejectMutation(UUID taskId, UUID actorUserId) {
        var task = taskRepository.findById(taskId).orElseThrow(ChecklistV2CompatibilityMutationService::notFound);
        var instance = instanceRepository.findById(task.getChecklistInstanceId())
                .orElseThrow(ChecklistV2CompatibilityMutationService::notFound);
        if (!accessPolicy.canView(instance, actorUserId)) {
            throw notFound();
        }
        mutationPolicy.requireMutable(instance.getOrigin());
        if (instance.getOrigin() == ChecklistOrigin.USER_CREATED) {
            throw new BusinessException(HttpStatus.GONE, "CHECKLIST_LEGACY_ROUTE_RETIRED",
                    "Use the unified Today task APIs");
        }
        throw new IllegalStateException("Unsupported checklist origin");
    }

    private static BusinessException notFound() {
        return new BusinessException(HttpStatus.NOT_FOUND, "TASK_NOT_FOUND", "Task not found");
    }
}
