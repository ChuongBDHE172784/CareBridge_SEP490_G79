package com.carebridge.backend.audit.service;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.checklist.model.ChecklistCareContextType;
import java.util.Map;
import java.util.UUID;

/** Typed input for mutations whose audit event is part of the transaction contract. */
public record RequiredAuditEvent(
        AuditAction action,
        UUID actorUserId,
        String actorType,
        String actorService,
        UUID subjectUserId,
        String resourceType,
        UUID resourceId,
        ChecklistCareContextType careContextType,
        UUID careContextId,
        UUID previousCareContextId,
        UUID templateVersionId,
        UUID checklistTaskInstanceId,
        Map<String, Object> beforePayload,
        Map<String, Object> afterPayload,
        String reasonCode,
        UUID correlationId) {

    public RequiredAuditEvent(
            AuditAction action,
            UUID actorUserId,
            String actorType,
            String actorService,
            UUID subjectUserId,
            String resourceType,
            UUID resourceId,
            ChecklistCareContextType careContextType,
            UUID careContextId,
            UUID templateVersionId,
            UUID checklistTaskInstanceId,
            Map<String, Object> beforePayload,
            Map<String, Object> afterPayload,
            String reasonCode,
            UUID correlationId) {
        this(action, actorUserId, actorType, actorService, subjectUserId, resourceType, resourceId,
                careContextType, careContextId, null, templateVersionId, checklistTaskInstanceId,
                beforePayload, afterPayload, reasonCode, correlationId);
    }
}
