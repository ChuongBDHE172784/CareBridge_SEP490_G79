package com.carebridge.backend.checklist.today.dto;

import com.carebridge.backend.checklist.model.ChecklistCareContextType;
import com.carebridge.backend.checklist.model.ChecklistOrigin;
import com.carebridge.backend.checklist.model.ChecklistTargetSubject;
import com.carebridge.backend.checklist.model.ChecklistSupportFunction;
import com.carebridge.backend.checklist.today.model.TaskAction;
import com.carebridge.backend.checklist.today.model.TaskKind;
import com.carebridge.backend.reminder.entity.ReminderType;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record TodayTaskCandidate(
        TaskKind taskKind,
        UUID taskId,
        UUID instanceId,
        UUID templateVersionId,
        UUID careGroupId,
        ChecklistCareContextType careContextType,
        UUID careContextId,
        String title,
        ChecklistTargetSubject targetSubject,
        ChecklistOrigin origin,
        String status,
        Set<TaskAction> allowedActions,
        Instant dueAt,
        Instant terminalAt,
        ReminderType reminderType,
        String description,
        ChecklistSupportFunction supportFunction) {

    /** Compatibility constructor for providers that do not expose task detail. */
    public TodayTaskCandidate(
            TaskKind taskKind,
            UUID taskId,
            UUID instanceId,
            UUID templateVersionId,
            UUID careGroupId,
            ChecklistCareContextType careContextType,
            UUID careContextId,
            String title,
            ChecklistTargetSubject targetSubject,
            ChecklistOrigin origin,
            String status,
            Set<TaskAction> allowedActions,
            Instant dueAt,
            Instant terminalAt,
            ReminderType reminderType) {
        this(taskKind, taskId, instanceId, templateVersionId, careGroupId, careContextType,
                careContextId, title, targetSubject, origin, status, allowedActions,
                dueAt, terminalAt, reminderType, null, null);
    }

    public TodayTaskCandidate(
            TaskKind taskKind,
            UUID taskId,
            UUID instanceId,
            UUID templateVersionId,
            UUID careGroupId,
            ChecklistCareContextType careContextType,
            UUID careContextId,
            String title,
            ChecklistTargetSubject targetSubject,
            ChecklistOrigin origin,
            String status,
            Set<TaskAction> allowedActions,
            Instant dueAt) {
        this(taskKind, taskId, instanceId, templateVersionId, careGroupId, careContextType,
                careContextId, title, targetSubject, origin, status, allowedActions, dueAt, null, null,
                null, null);
    }

    public TodayTaskCandidate(
            TaskKind taskKind,
            UUID taskId,
            UUID instanceId,
            UUID templateVersionId,
            UUID careGroupId,
            ChecklistCareContextType careContextType,
            UUID careContextId,
            String title,
            ChecklistTargetSubject targetSubject,
            ChecklistOrigin origin,
            String status,
            Set<TaskAction> allowedActions,
            Instant dueAt,
            Instant terminalAt) {
        this(taskKind, taskId, instanceId, templateVersionId, careGroupId, careContextType,
                careContextId, title, targetSubject, origin, status, allowedActions, dueAt, terminalAt, null,
                null, null);
    }
}
