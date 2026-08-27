package com.carebridge.backend.checklist.today.provider;

import com.carebridge.backend.checklist.today.dto.TaskActionResponse;
import com.carebridge.backend.checklist.today.model.TaskAction;
import com.carebridge.backend.checklist.today.model.TaskKind;
import com.carebridge.backend.common.exception.BusinessException;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public interface TaskActionHandler {
    TaskKind taskKind();
    AuthorizedTask authorize(UUID actorUserId, UUID taskId);

    /** Authorization for an explicitly scoped resource. Existing handlers keep
     * their owner/caller behavior when no group scope is supplied. */
    default AuthorizedTask authorize(UUID actorUserId, UUID taskId, UUID careGroupId) {
        if (careGroupId != null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "TASK_NOT_FOUND", "Task not found");
        }
        return authorize(actorUserId, taskId);
    }

    /**
     * Re-authorizes a durable reminder replay by its immutable definition identity.
     * Providers without historical occurrence aliases retain the ordinary path.
     */
    default AuthorizedTask authorizeReplay(UUID actorUserId, UUID taskId, UUID instanceId) {
        return authorize(actorUserId, taskId);
    }

    /** Identity used to serialize all terminal mutations of the same resource. */
    default UUID actionScopeId(AuthorizedTask task) {
        return task.taskId();
    }

    /** Re-authorizes after the action-scope lock has been acquired. */
    default AuthorizedTask authorizeForUpdate(UUID actorUserId, AuthorizedTask task) {
        return authorize(actorUserId, task.taskId());
    }

    default AuthorizedTask authorizeForUpdate(
            UUID actorUserId, AuthorizedTask task, UUID careGroupId) {
        return careGroupId == null
                ? authorizeForUpdate(actorUserId, task)
                : throwScopedTaskNotFound();
    }

    private static AuthorizedTask throwScopedTaskNotFound() {
        throw new BusinessException(HttpStatus.NOT_FOUND, "TASK_NOT_FOUND", "Task not found");
    }

    TaskActionResponse apply(AuthorizedTask task, UUID actorUserId, TaskAction action,
                             String reason, Instant appliedAt, UUID correlationId);
}
