package com.carebridge.backend.checklist.today.dto;

import com.carebridge.backend.checklist.model.ChecklistCareContextType;
import com.carebridge.backend.checklist.model.ChecklistOrigin;
import com.carebridge.backend.checklist.model.ChecklistTargetSubject;
import com.carebridge.backend.checklist.model.ChecklistSupportFunction;
import com.carebridge.backend.checklist.today.model.TaskAction;
import com.carebridge.backend.checklist.today.model.TaskCadence;
import com.carebridge.backend.checklist.today.model.TaskKind;
import com.carebridge.backend.content.entity.ContentStage;
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
        ChecklistSupportFunction supportFunction,
        TaskCadence cadence,
        ContentStage stage,
        String sourceUrl) {

    public TodayTaskCandidate {
        cadence = cadence == null ? TaskCadence.ONCE : cadence;
    }

    /** Compatibility constructor for callers before sourceUrl was exposed. */
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
            ReminderType reminderType,
            String description,
            ChecklistSupportFunction supportFunction,
            TaskCadence cadence,
            ContentStage stage) {
        this(taskKind, taskId, instanceId, templateVersionId, careGroupId, careContextType,
                careContextId, title, targetSubject, origin, status, allowedActions,
                dueAt, terminalAt, reminderType, description, supportFunction, cadence, stage, null);
    }

    /** Compatibility constructor for callers before lifecycle stage was exposed. */
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
            ReminderType reminderType,
            String description,
            ChecklistSupportFunction supportFunction,
            TaskCadence cadence) {
        this(taskKind, taskId, instanceId, templateVersionId, careGroupId, careContextType,
                careContextId, title, targetSubject, origin, status, allowedActions,
                dueAt, terminalAt, reminderType, description, supportFunction, cadence, null, null);
    }

    /** Compatibility constructor for providers that expose task detail but not cadence. */
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
            ReminderType reminderType,
            String description,
            ChecklistSupportFunction supportFunction) {
        this(taskKind, taskId, instanceId, templateVersionId, careGroupId, careContextType,
                careContextId, title, targetSubject, origin, status, allowedActions,
                dueAt, terminalAt, reminderType, description, supportFunction, TaskCadence.ONCE, null);
    }

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
                dueAt, terminalAt, reminderType, null, null, TaskCadence.ONCE, null);
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
                null, null, TaskCadence.ONCE, null);
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
                null, null, TaskCadence.ONCE, null);
    }
}
