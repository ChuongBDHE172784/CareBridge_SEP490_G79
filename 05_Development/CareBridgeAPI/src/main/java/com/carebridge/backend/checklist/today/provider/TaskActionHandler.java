package com.carebridge.backend.checklist.today.provider;

import com.carebridge.backend.checklist.today.dto.TaskActionResponse;
import com.carebridge.backend.checklist.today.model.TaskAction;
import com.carebridge.backend.checklist.today.model.TaskKind;
import java.time.Instant;
import java.util.UUID;

public interface TaskActionHandler {
    TaskKind taskKind();
    AuthorizedTask authorize(UUID actorUserId, UUID taskId);

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

    TaskActionResponse apply(AuthorizedTask task, UUID actorUserId, TaskAction action,
                             String reason, Instant appliedAt, UUID correlationId);
}
