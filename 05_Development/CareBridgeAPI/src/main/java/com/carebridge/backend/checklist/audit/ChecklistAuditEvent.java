package com.carebridge.backend.checklist.audit;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.checklist.model.ChecklistCareContextType;
import java.util.UUID;

/** Typed, allowlisted checklist audit payload; arbitrary details are intentionally absent. */
public record ChecklistAuditEvent(
        AuditAction action,
        UUID actorUserId,
        ChecklistAuditActorType actorType,
        String actorService,
        ChecklistAuditResourceType resourceType,
        UUID resourceId,
        UUID recipientUserId,
        ChecklistCareContextType careContextType,
        UUID careContextId,
        UUID templateVersionId,
        UUID checklistTaskInstanceId,
        String beforeStatus,
        String afterStatus,
        String reasonCode,
        UUID correlationId) {
}
