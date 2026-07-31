package com.carebridge.backend.checklist.audit;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.entity.AuditLog;
import com.carebridge.backend.audit.policy.AuditEligibilityPolicy;
import com.carebridge.backend.audit.repository.AuditLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Strict checklist audit boundary. Required audit failures abort the surrounding transaction. */
@Component
@RequiredArgsConstructor
public class ChecklistAuditWriter {

    private static final Set<AuditAction> CHECKLIST_ACTIONS = EnumSet.of(
            AuditAction.CHECKLIST_TEMPLATE_DECIDED,
            AuditAction.CHECKLIST_DISTRIBUTED,
            AuditAction.CHECKLIST_ASSIGNED,
            AuditAction.CHECKLIST_COMPLETED,
            AuditAction.CHECKLIST_SKIPPED,
            AuditAction.CHECKLIST_CANCELLED,
            AuditAction.CHECKLIST_RECONCILIATION_FAILED,
            AuditAction.CHECKLIST_MIGRATION_QUARANTINED);
    private static final Set<String> CHECKLIST_STATUSES = Set.of(
            "PENDING", "IN_PROGRESS", "COMPLETED", "SKIPPED", "CANCELLED");

    private final AuditLogRepository repository;
    private final AuditEligibilityPolicy policy;
    private final ObjectMapper objectMapper;

    @Transactional
    public void write(ChecklistAuditEvent event) {
        validate(event);
        AuditLog log = AuditLog.builder()
                .createdAt(Instant.now())
                .actorUserId(event.actorUserId())
                .actorType(event.actorType().name())
                .actorService(event.actorService())
                .entityType(event.resourceType().name())
                .entityId(event.resourceId())
                .subjectUserId(event.recipientUserId())
                .action(event.action())
                .careContextType(event.careContextType())
                .careContextId(event.careContextId())
                .templateVersionId(event.templateVersionId())
                .checklistTaskInstanceId(event.checklistTaskInstanceId())
                .reasonCode(event.reasonCode())
                .correlationId(event.correlationId())
                .oldValueJson(serializeStatus(event.beforeStatus()))
                .newValueJson(serializeStatus(event.afterStatus()))
                .build();
        repository.save(log);
    }

    private void validate(ChecklistAuditEvent event) {
        if (event == null || event.action() == null || !CHECKLIST_ACTIONS.contains(event.action())) {
            throw new IllegalArgumentException("Checklist audit action is not allowlisted");
        }
        if (!policy.shouldAudit(event.action())) {
            throw new IllegalStateException("Checklist audit action is not eligible");
        }
        if (event.correlationId() == null) {
            throw new IllegalArgumentException("Checklist audit correlation is required");
        }
        if (event.actorType() == null) {
            throw new IllegalArgumentException("Checklist audit actor type is required");
        }
        if (event.actorType() == ChecklistAuditActorType.USER && event.actorUserId() == null) {
            throw new IllegalArgumentException("User checklist audit actor is required");
        }
        if (event.actorType() == ChecklistAuditActorType.USER && event.actorService() != null) {
            throw new IllegalArgumentException("User checklist audit actor cannot name a service");
        }
        if (event.actorType() != ChecklistAuditActorType.USER
                && (event.actorService() == null || event.actorService().isBlank())) {
            throw new IllegalArgumentException("System checklist audit service is required");
        }
        if (event.actorType() != ChecklistAuditActorType.USER && event.actorUserId() != null) {
            throw new IllegalArgumentException("System checklist audit actor cannot name a user");
        }
        if ((event.resourceType() == null) != (event.resourceId() == null)) {
            throw new IllegalArgumentException("Checklist audit resource type and id must be paired");
        }
        if (event.resourceType() == null) {
            throw new IllegalArgumentException("Checklist audit resource is required");
        }
        if ((event.careContextType() == null) != (event.careContextId() == null)) {
            throw new IllegalArgumentException("Checklist context type and id must be paired");
        }
        if (event.reasonCode() != null && !event.reasonCode().matches("[A-Z0-9_]{1,80}")) {
            throw new IllegalArgumentException("Checklist audit reason code is not controlled");
        }
        validateStatus(event.beforeStatus());
        validateStatus(event.afterStatus());
        validateActionShape(event);
    }

    private void validateActionShape(ChecklistAuditEvent event) {
        switch (event.action()) {
            case CHECKLIST_TEMPLATE_DECIDED -> {
                require(event.templateVersionId(), "Checklist template decision version is required");
                require(event.reasonCode(), "Checklist template decision reason is required");
                requireResource(event, ChecklistAuditResourceType.CHECKLIST_TEMPLATE_VERSION,
                        event.templateVersionId());
            }
            case CHECKLIST_DISTRIBUTED -> {
                require(event.recipientUserId(), "Distributed checklist recipient is required");
                require(event.templateVersionId(), "Distributed checklist template version is required");
                require(event.careContextId(), "Distributed checklist context is required");
                requireStatus(event.afterStatus(), "PENDING");
                requireResource(event, ChecklistAuditResourceType.CHECKLIST_INSTANCE, event.resourceId());
            }
            case CHECKLIST_ASSIGNED -> {
                require(event.recipientUserId(), "Assigned checklist recipient is required");
                require(event.templateVersionId(), "Assigned checklist template version is required");
                require(event.checklistTaskInstanceId(), "Assigned checklist task is required");
                requireStatus(event.afterStatus(), "PENDING");
                requireResource(event, ChecklistAuditResourceType.CHECKLIST_TASK_INSTANCE,
                        event.checklistTaskInstanceId());
            }
            case CHECKLIST_COMPLETED -> {
                require(event.recipientUserId(), "Completed checklist recipient is required");
                require(event.checklistTaskInstanceId(), "Completed checklist task is required");
                require(event.beforeStatus(), "Completed checklist previous status is required");
                requireStatus(event.afterStatus(), "COMPLETED");
                requireResource(event, ChecklistAuditResourceType.CHECKLIST_TASK_INSTANCE,
                        event.checklistTaskInstanceId());
            }
            case CHECKLIST_SKIPPED -> {
                require(event.recipientUserId(), "Skipped checklist recipient is required");
                require(event.checklistTaskInstanceId(), "Skipped checklist task is required");
                require(event.beforeStatus(), "Skipped checklist previous status is required");
                requireStatus(event.afterStatus(), "SKIPPED");
                require(event.reasonCode(), "Skipped checklist reason is required");
                requireResource(event, ChecklistAuditResourceType.CHECKLIST_TASK_INSTANCE,
                        event.checklistTaskInstanceId());
            }
            case CHECKLIST_CANCELLED -> {
                require(event.recipientUserId(), "Cancelled checklist recipient is required");
                require(event.beforeStatus(), "Cancelled checklist previous status is required");
                requireStatus(event.afterStatus(), "CANCELLED");
                require(event.reasonCode(), "Cancelled checklist reason is required");
                if (event.resourceType() == ChecklistAuditResourceType.CHECKLIST_TASK_INSTANCE) {
                    require(event.checklistTaskInstanceId(), "Cancelled checklist task is required");
                    requireResource(event, ChecklistAuditResourceType.CHECKLIST_TASK_INSTANCE,
                            event.checklistTaskInstanceId());
                } else {
                    if (event.checklistTaskInstanceId() != null) {
                        throw new IllegalArgumentException("Cancelled checklist instance cannot name a task");
                    }
                    requireResource(event, ChecklistAuditResourceType.CHECKLIST_INSTANCE, event.resourceId());
                }
            }
            case CHECKLIST_RECONCILIATION_FAILED -> {
                require(event.careContextId(), "Failed reconciliation context is required");
                require(event.reasonCode(), "Failed reconciliation reason is required");
                requireResource(event, ChecklistAuditResourceType.CARE_CONTEXT, event.careContextId());
            }
            case CHECKLIST_MIGRATION_QUARANTINED -> {
                    require(event.reasonCode(), "Migration quarantine reason is required");
                requireResource(event, ChecklistAuditResourceType.MIGRATION_SOURCE, event.resourceId());
            }
            default -> throw new IllegalArgumentException("Checklist audit action is not allowlisted");
        }
    }

    private static void validateStatus(String status) {
        if (status != null && !CHECKLIST_STATUSES.contains(status)) {
            throw new IllegalArgumentException("Checklist audit status is not controlled");
        }
    }

    private static void requireStatus(String actual, String expected) {
        if (!expected.equals(actual)) {
            throw new IllegalArgumentException("Checklist audit target status must be " + expected);
        }
    }

    private static void require(Object value, String message) {
        if (value == null || value instanceof String text && text.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    private static void requireResource(
            ChecklistAuditEvent event,
            ChecklistAuditResourceType expectedType,
            UUID expectedId) {
        require(expectedId, "Checklist audit resource id is required");
        if (event.resourceType() != expectedType || !expectedId.equals(event.resourceId())) {
            throw new IllegalArgumentException(
                    "Checklist audit resource must be " + expectedType + " with the canonical id");
        }
    }

    private String serializeStatus(String status) {
        if (status == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(Map.of("status", status));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize checklist audit status", exception);
        }
    }
}
