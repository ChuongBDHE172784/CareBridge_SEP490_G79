package com.carebridge.backend.checklist.today.dto;

import com.carebridge.backend.checklist.today.model.TaskAction;
import java.time.Instant;
import java.util.UUID;

/** Checklist-only action response; the mixed task-kind discriminator is omitted. */
public record CurrentChecklistActionResponse(
        UUID taskId,
        UUID instanceId,
        TaskAction action,
        String previousStatus,
        String status,
        Instant appliedAt,
        boolean idempotentReplay,
        UUID correlationId) {
}
