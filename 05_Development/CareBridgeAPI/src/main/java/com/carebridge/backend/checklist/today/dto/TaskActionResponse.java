package com.carebridge.backend.checklist.today.dto;

import com.carebridge.backend.checklist.today.model.TaskAction;
import com.carebridge.backend.checklist.today.model.TaskKind;
import java.time.Instant;
import java.util.UUID;

public record TaskActionResponse(
        TaskKind taskKind,
        UUID taskId,
        UUID instanceId,
        TaskAction action,
        String previousStatus,
        String status,
        Instant appliedAt,
        boolean idempotentReplay,
        UUID correlationId) {

    public TaskActionResponse asReplay() {
        return new TaskActionResponse(taskKind, taskId, instanceId, action, previousStatus,
                status, appliedAt, true, correlationId);
    }
}
