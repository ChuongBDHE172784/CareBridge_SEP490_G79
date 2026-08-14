package com.carebridge.backend.checklist.today.dto;

import com.carebridge.backend.checklist.model.ChecklistCareContextType;
import com.carebridge.backend.checklist.model.ChecklistOrigin;
import com.carebridge.backend.checklist.model.ChecklistTargetSubject;
import com.carebridge.backend.checklist.model.ChecklistSupportFunction;
import com.carebridge.backend.checklist.today.model.TaskAction;
import com.carebridge.backend.checklist.today.model.TaskCadence;
import com.carebridge.backend.checklist.today.model.TaskKind;
import com.carebridge.backend.checklist.today.model.TaskTimeBucket;
import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.reminder.entity.ReminderType;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record TodayTaskItemResponse(
        TaskKind taskKind,
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
        ReminderType type,
        String description,
        ChecklistSupportFunction supportFunction,
        TaskCadence cadence,
        ContentStage stage) {

    public TodayTaskItemResponse {
        cadence = cadence == null ? TaskCadence.ONCE : cadence;
    }

    /** Compatibility constructor for response consumers before lifecycle stage was exposed. */
    public TodayTaskItemResponse(
            TaskKind taskKind,
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
            ReminderType type,
            String description,
            ChecklistSupportFunction supportFunction,
            TaskCadence cadence) {
        this(taskKind, taskId, instanceId, templateVersionId, careGroupId, careContextType,
                careContextId, careGroupLabel, careContextLabel, title, targetSubject,
                origin, status, timeBucket, allowedActions, dueAt, type, description,
                supportFunction, cadence, null);
    }

    /** Compatibility constructor for response consumers before cadence was exposed. */
    public TodayTaskItemResponse(
            TaskKind taskKind,
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
            ReminderType type,
            String description,
            ChecklistSupportFunction supportFunction) {
        this(taskKind, taskId, instanceId, templateVersionId, careGroupId, careContextType,
                careContextId, careGroupLabel, careContextLabel, title, targetSubject,
                origin, status, timeBucket, allowedActions, dueAt, type, description,
                supportFunction, TaskCadence.ONCE, null);
    }

    /** Compatibility constructor for response consumers before task detail was added. */
    public TodayTaskItemResponse(
            TaskKind taskKind,
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
            ReminderType type) {
        this(taskKind, taskId, instanceId, templateVersionId, careGroupId, careContextType,
                careContextId, careGroupLabel, careContextLabel, title, targetSubject,
                origin, status, timeBucket, allowedActions, dueAt, type, null, null,
                TaskCadence.ONCE, null);
    }
}
