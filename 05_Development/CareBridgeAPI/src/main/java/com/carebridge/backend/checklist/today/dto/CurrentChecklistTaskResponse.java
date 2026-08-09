package com.carebridge.backend.checklist.today.dto;

import com.carebridge.backend.checklist.model.ChecklistCareContextType;
import com.carebridge.backend.checklist.model.ChecklistOrigin;
import com.carebridge.backend.checklist.model.ChecklistTargetSubject;
import com.carebridge.backend.checklist.model.ChecklistSupportFunction;
import com.carebridge.backend.checklist.today.model.TaskAction;
import com.carebridge.backend.checklist.today.model.TaskTimeBucket;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/** Checklist-only wire item; task-product discriminator fields are omitted. */
public record CurrentChecklistTaskResponse(
        UUID taskId,
        UUID instanceId,
        UUID templateVersionId,
        UUID careGroupId,
        ChecklistCareContextType careContextType,
        UUID careContextId,
        String careGroupLabel,
        String careContextLabel,
        String title,
        ChecklistTargetSubject targetSubject,
        ChecklistOrigin origin,
        String status,
        TaskTimeBucket timeBucket,
        Set<TaskAction> allowedActions,
        Instant dueAt,
        String description,
        ChecklistSupportFunction supportFunction) {

    /** Compatibility constructor for callers using the original checklist projection. */
    public CurrentChecklistTaskResponse(
            UUID taskId,
            UUID instanceId,
            UUID templateVersionId,
            UUID careGroupId,
            ChecklistCareContextType careContextType,
            UUID careContextId,
            String careGroupLabel,
            String careContextLabel,
            String title,
            ChecklistTargetSubject targetSubject,
            ChecklistOrigin origin,
            String status,
            TaskTimeBucket timeBucket,
            Set<TaskAction> allowedActions,
            Instant dueAt) {
        this(taskId, instanceId, templateVersionId, careGroupId, careContextType, careContextId,
                careGroupLabel, careContextLabel, title, targetSubject, origin, status,
                timeBucket, allowedActions, dueAt, null, null);
    }
}
