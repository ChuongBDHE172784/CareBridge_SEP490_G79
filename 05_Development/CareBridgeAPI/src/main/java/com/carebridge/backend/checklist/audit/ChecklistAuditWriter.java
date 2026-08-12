package com.carebridge.backend.checklist.audit;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.entity.AuditLog;
import com.carebridge.backend.audit.policy.AuditEligibilityPolicy;
import com.carebridge.backend.audit.repository.AuditLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.EnumSet;
import java.util.HashSet;
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
            AuditAction.CHECKLIST_REOPENED,
            AuditAction.CHECKLIST_CANCELLED,
            AuditAction.CHECKLIST_RECONCILIATION_FAILED,
            AuditAction.CHECKLIST_MIGRATION_QUARANTINED,
            AuditAction.CHECKLIST_ACCESS_BASELINE,
            AuditAction.CHECKLIST_ACCESS_REVOKED);
    private static final Set<String> CHECKLIST_STATUSES = Set.of(
            "PENDING", "IN_PROGRESS", "COMPLETED", "SKIPPED", "CANCELLED");
    private static final Set<String> MIGRATION_QUARANTINE_REASONS = Set.of(
            "TEMPLATE_AGGREGATE_CONTRADICTION",
            "INSTANCE_AGGREGATE_CONTRADICTION",
            "TASK_PARENT_CONTRACT_MISMATCH",
            "JOURNEY_DATING_UNRESOLVED",
            "JOURNEY_DATING_CONFLICT",
            "FAMILY_MEMBER_DUPLICATE",
            "FAMILY_MEMBER_OWNER_ROLE",
            "FAMILY_ACCESS_TIMELINE_MISMATCH",
            "AUDIT_EVIDENCE_MISMATCH");
    private static final Map<ChecklistAuditResourceType, String> MIGRATION_SOURCE_KINDS = Map.of(
            ChecklistAuditResourceType.CARE_ITEM_TEMPLATE, "care_item_templates",
            ChecklistAuditResourceType.CHECKLIST_INSTANCE, "checklist_instances",
            ChecklistAuditResourceType.CHECKLIST_TASK_INSTANCE, "checklist_task_instances",
            ChecklistAuditResourceType.MOTHER_JOURNEY, "mother_journeys",
            ChecklistAuditResourceType.CARE_GROUP_MEMBER, "care_group_members");

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
                .oldValueJson(serializePayload(event.beforePayload(), event.beforeStatus()))
                .newValueJson(serializePayload(event.afterPayload(), event.afterStatus()))
                .eventOrigin(normalizeEventOrigin(event))
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
        String expectedOrigin = expectedEventOrigin(event.action());
        if (event.eventOrigin() != null && !expectedOrigin.equals(event.eventOrigin())) {
            throw new IllegalArgumentException("Checklist audit event origin is not allowlisted");
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
            case CHECKLIST_REOPENED -> {
                require(event.recipientUserId(), "Reopened checklist recipient is required");
                require(event.checklistTaskInstanceId(), "Reopened checklist task is required");
                requireStatus(event.beforeStatus(), "COMPLETED");
                requireStatus(event.afterStatus(), "PENDING");
                if (event.reasonCode() != null) {
                    throw new IllegalArgumentException("Reopened checklist reason must be absent");
                }
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
                if (event.actorType() != ChecklistAuditActorType.SYSTEM
                        || !"CHECKLIST_P2_BACKFILL".equals(event.actorService())
                        || event.actorUserId() != null) {
                    throw new IllegalArgumentException("Migration quarantine actor is invalid");
                }
                require(event.reasonCode(), "Migration quarantine reason is required");
                if (!MIGRATION_QUARANTINE_REASONS.contains(event.reasonCode())) {
                    throw new IllegalArgumentException("Migration quarantine reason is not controlled");
                }
                validateMigrationQuarantineResource(event);
                validateMigrationQuarantinePayload(event);
            }
            case CHECKLIST_ACCESS_BASELINE, CHECKLIST_ACCESS_REVOKED -> validateAccessEvent(event);
            default -> throw new IllegalArgumentException("Checklist audit action is not allowlisted");
        }
    }

    private static void validateAccessEvent(ChecklistAuditEvent event) {
        if (event.actorType() != ChecklistAuditActorType.SYSTEM
                || !"CHECKLIST_P2_BACKFILL".equals(event.actorService())) {
            throw new IllegalArgumentException("Checklist access audit actor is invalid");
        }
        require(event.reasonCode(), "Checklist access audit reason is required");
        String expectedReason = event.action() == AuditAction.CHECKLIST_ACCESS_BASELINE
                ? "LEGACY_ACCESS_BASELINE" : "FAMILY_MEMBER_DUPLICATE";
        if (!expectedReason.equals(event.reasonCode())) {
            throw new IllegalArgumentException("Checklist access audit reason is invalid");
        }
        requireResource(event, ChecklistAuditResourceType.CARE_GROUP_MEMBER, event.resourceId());
        if (event.beforePayload() == null || event.afterPayload() == null) {
            throw new IllegalArgumentException("Checklist access audit payloads are required");
        }
        validateAccessPayload(event.beforePayload(), "before");
        validateAccessPayload(event.afterPayload(), "after");
        String expectedType = event.action() == AuditAction.CHECKLIST_ACCESS_BASELINE
                ? "LEGACY_ACCESS_BASELINE" : "VIEW_REVOKED";
        if (!expectedType.equals(event.beforePayload().get("eventType"))) {
            throw new IllegalArgumentException("Checklist access audit before event type is invalid");
        }
        if (!String.valueOf(event.correlationId()).equals(event.beforePayload().get("correlationId"))) {
            throw new IllegalArgumentException("Checklist access audit before correlation does not match payload");
        }
        if (!event.afterPayload().containsKey("correlationId")
                || !String.valueOf(event.correlationId()).equals(event.afterPayload().get("correlationId"))) {
            throw new IllegalArgumentException("Checklist access audit correlation does not match payload");
        }
        if (!expectedType.equals(event.afterPayload().get("eventType"))) {
            throw new IllegalArgumentException("Checklist access audit event type is invalid");
        }
        validateAccessTransition(event);
    }

    private static void validateAccessTransition(ChecklistAuditEvent event) {
        Map<String, Object> before = event.beforePayload();
        Map<String, Object> after = event.afterPayload();
        if (!(before.get("accessEpoch") instanceof Number beforeNumber)
                || !(after.get("accessEpoch") instanceof Number afterNumber)) {
            throw new IllegalArgumentException("Checklist access audit epoch is invalid");
        }
        long beforeEpoch = beforeNumber.longValue();
        long afterEpoch = afterNumber.longValue();
        if (beforeEpoch < 0 || afterEpoch < 0
                || beforeNumber.doubleValue() != beforeEpoch
                || afterNumber.doubleValue() != afterEpoch
                || beforeEpoch == Long.MAX_VALUE
                || afterEpoch != beforeEpoch + 1) {
            throw new IllegalArgumentException("Checklist access audit epoch transition is invalid");
        }
        if (!(before.get("effectiveFrom") instanceof String beforeEffective)
                || !(after.get("effectiveFrom") instanceof String afterEffective)
                || !beforeEffective.equals(afterEffective)) {
            throw new IllegalArgumentException("Checklist access audit effective time is invalid");
        }
        try {
            Instant.parse(beforeEffective);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Checklist access audit effective time is invalid", exception);
        }
        if (event.action() == AuditAction.CHECKLIST_ACCESS_BASELINE) {
            if (!"ACCEPTED".equals(before.get("membershipStatus"))
                    || !"ACCEPTED".equals(after.get("membershipStatus"))
                    || !Boolean.TRUE.equals(before.get("checklistView"))
                    || !Boolean.TRUE.equals(after.get("checklistView"))) {
                throw new IllegalArgumentException("Checklist access baseline transition is invalid");
            }
        } else if (!"ACCEPTED".equals(before.get("membershipStatus"))
                || !"REVOKED".equals(after.get("membershipStatus"))
                || !Boolean.FALSE.equals(after.get("checklistView"))
                || !Boolean.FALSE.equals(after.get("checklistComplete"))) {
            throw new IllegalArgumentException("Checklist access revoke transition is invalid");
        }
    }

    private static void validateAccessPayload(Map<String, Object> payload, String label) {
        Set<String> expected = Set.of("schema", "eventType", "membershipStatus",
                "checklistView", "checklistComplete", "accessEpoch", "effectiveFrom", "correlationId");
        if (!new HashSet<>(payload.keySet()).equals(expected)
                || !"CHECKLIST_ACCESS_AUDIT_V1".equals(payload.get("schema"))
                || !(payload.get("eventType") instanceof String)
                || !(payload.get("membershipStatus") instanceof String)
                || !(payload.get("checklistView") instanceof Boolean)
                || !(payload.get("checklistComplete") instanceof Boolean)
                || !(payload.get("accessEpoch") instanceof Number)
                || !(payload.get("effectiveFrom") instanceof String)
                || !(payload.get("correlationId") instanceof String)) {
            throw new IllegalArgumentException("Checklist access audit " + label + " payload is invalid");
        }
        if (((Number) payload.get("accessEpoch")).longValue() < 0) {
            throw new IllegalArgumentException("Checklist access audit epoch is invalid");
        }
    }

    private static void validateMigrationQuarantineResource(ChecklistAuditEvent event) {
        if (!MIGRATION_SOURCE_KINDS.containsKey(event.resourceType())) {
            throw new IllegalArgumentException(
                    "Migration quarantine resource must identify a canonical checklist source");
        }
        require(event.resourceId(), "Migration quarantine resource id is required");
    }

    private static void validateMigrationQuarantinePayload(ChecklistAuditEvent event) {
        if (event.beforePayload() != null || event.afterPayload() == null) {
            throw new IllegalArgumentException("Migration quarantine payload shape is invalid");
        }
        Map<String, Object> payload = event.afterPayload();
        Set<String> expected = Set.of("schema", "sourceKind", "sourceIdHash", "reasonCode",
                "disposition", "correlationId", "metadata");
        if (!new HashSet<>(payload.keySet()).equals(expected)
                || !"CHECKLIST_MIGRATION_QUARANTINE_V1".equals(payload.get("schema"))
                || !(payload.get("sourceKind") instanceof String sourceKind)
                || !MIGRATION_SOURCE_KINDS.get(event.resourceType()).equals(sourceKind)
                || !(payload.get("sourceIdHash") instanceof String sourceIdHash)
                || !sourceIdHash.startsWith("md5:")
                || !event.reasonCode().equals(payload.get("reasonCode"))
                || !"UNAVAILABLE".equals(payload.get("disposition"))
                || !"REDACTED".equals(payload.get("metadata"))
                || !String.valueOf(event.correlationId()).equals(payload.get("correlationId"))) {
            throw new IllegalArgumentException("Migration quarantine payload is invalid");
        }
    }

    private static void validateStatus(String status) {
        if (status != null && !CHECKLIST_STATUSES.contains(status)) {
            throw new IllegalArgumentException("Checklist audit status is not controlled");
        }
    }

    private static String expectedEventOrigin(AuditAction action) {
        return switch (action) {
            case CHECKLIST_ACCESS_BASELINE, CHECKLIST_ACCESS_REVOKED -> "CHECKLIST_ACCESS";
            case CHECKLIST_MIGRATION_QUARANTINED -> "CHECKLIST_MIGRATION";
            default -> "AUDIT_LOG";
        };
    }

    private static String normalizeEventOrigin(ChecklistAuditEvent event) {
        String expected = expectedEventOrigin(event.action());
        return event.eventOrigin() == null ? expected : event.eventOrigin();
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

    private String serializePayload(Map<String, Object> payload, String status) {
        if (payload != null) {
            try {
                return objectMapper.writeValueAsString(payload);
            } catch (JsonProcessingException exception) {
                throw new IllegalStateException("Unable to serialize checklist audit payload", exception);
            }
        }
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
