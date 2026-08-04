package com.carebridge.backend.checklist.today.provider;

import com.carebridge.backend.checklist.today.model.TaskAction;
import com.carebridge.backend.checklist.today.model.TaskKind;
import java.util.Set;
import java.util.UUID;

public record AuthorizedTask(
        TaskKind taskKind,
        UUID taskId,
        UUID instanceId,
        String status,
        Set<TaskAction> allowedActions,
        UUID authorizationCareGroupId) {

    public AuthorizedTask(
            TaskKind taskKind,
            UUID taskId,
            UUID instanceId,
            String status,
            Set<TaskAction> allowedActions) {
        this(taskKind, taskId, instanceId, status, allowedActions, null);
    }
}
