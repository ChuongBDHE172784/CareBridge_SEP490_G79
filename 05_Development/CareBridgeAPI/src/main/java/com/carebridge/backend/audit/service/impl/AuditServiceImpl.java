package com.carebridge.backend.audit.service.impl;

import com.carebridge.backend.audit.dto.response.AuditLogResponse;
import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.entity.AuditLog;
import com.carebridge.backend.audit.mapper.AuditLogMapper;
import com.carebridge.backend.audit.policy.AuditEligibilityPolicy;
import com.carebridge.backend.audit.repository.AuditLogRepository;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.audit.service.RequiredAuditEvent;
import com.carebridge.backend.checklist.model.ChecklistCareContextType;
import com.carebridge.backend.family.entity.CareTaskStatus;
import com.carebridge.backend.reminder.entity.ReminderStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AuditServiceImpl implements AuditService {

    private static final Set<String> ALLOWED_ACTOR_TYPES = Set.of("USER", "SYSTEM", "SERVICE");
    private static final Set<String> RELINK_PAYLOAD_KEYS = Set.of("careContextType", "careContextId");
    private static final Set<String> TASK_PAYLOAD_KEYS = Set.of("status", "action", "careGroupId");
    private static final Set<String> TASK_ACTIONS = Set.of("COMPLETE", "SKIP");
    private static final Set<String> REMINDER_ACTIONABLE_STATUSES = Set.of(
            ReminderStatus.PENDING.name(), ReminderStatus.SNOOZED.name());
    private static final Set<String> REMINDER_STATUSES = Stream.of(ReminderStatus.values())
            .map(Enum::name)
            .collect(Collectors.toUnmodifiableSet());
    private static final Set<String> CARE_TASK_STATUSES = Stream.concat(
                    Stream.of(CareTaskStatus.values()).map(Enum::name),
                    Stream.of("PENDING", "COMPLETED"))
            .collect(Collectors.toUnmodifiableSet());

    private static final Set<AuditAction> STRICT_ACTIONS = EnumSet.of(
            AuditAction.CARE_TASK_STATUS_UPDATED,
            AuditAction.CARE_GROUP_CONTEXT_RELINKED,
            AuditAction.REMINDER_COMPLETED,
            AuditAction.REMINDER_SKIPPED,
            AuditAction.CHECKLIST_QUARANTINE_VIEWED,
            AuditAction.CHECKLIST_QUARANTINE_RESOLVED,
            AuditAction.CHECKLIST_RETENTION_PURGED,
            AuditAction.CHECKLIST_TEMPLATE_DECIDED,
            AuditAction.CHECKLIST_DISTRIBUTED,
            AuditAction.CHECKLIST_ASSIGNED,
            AuditAction.CHECKLIST_COMPLETED,
            AuditAction.CHECKLIST_SKIPPED,
            AuditAction.CHECKLIST_CANCELLED,
            AuditAction.CHECKLIST_RECONCILIATION_FAILED,
            AuditAction.CHECKLIST_MIGRATION_QUARANTINED);

    private static final Set<AuditAction> CHECKLIST_WRITER_ACTIONS = EnumSet.of(
            AuditAction.CHECKLIST_TEMPLATE_DECIDED,
            AuditAction.CHECKLIST_DISTRIBUTED,
            AuditAction.CHECKLIST_ASSIGNED,
            AuditAction.CHECKLIST_COMPLETED,
            AuditAction.CHECKLIST_SKIPPED,
            AuditAction.CHECKLIST_CANCELLED,
            AuditAction.CHECKLIST_RECONCILIATION_FAILED,
            AuditAction.CHECKLIST_MIGRATION_QUARANTINED);

    private final AuditLogRepository auditLogRepository;
    private final AuditLogMapper auditLogMapper;
    private final AuditEligibilityPolicy auditEligibilityPolicy;
    private final ObjectMapper objectMapper;

    @Override
    public void log(AuditAction action, java.util.UUID userId, String resourceType, String resourceId, Object details) {
        log(action, userId, resourceType, resourceId, details, null, null);
    }

    @Override
    public void log(AuditAction action, java.util.UUID userId, String resourceType, String resourceId,
                    Object details, String reasonCode, java.util.UUID correlationId) {
        if (action != null && STRICT_ACTIONS.contains(action)) {
            throw new IllegalArgumentException("Use logRequired for strict audit actions");
        }
        if (!auditEligibilityPolicy.shouldAudit(action)) {
            return;
        }
        AuditLog log = AuditLog.builder()
                .createdAt(Instant.now())
                .actorUserId(userId)
                .action(action)
                .entityType(resourceType)
                .entityId(parseUuidSafely(resourceId))
                .reasonCode(reasonCode)
                .correlationId(correlationId)
                .newValueJson(toJson(action, details))
                .build();
        auditLogRepository.save(log);
    }

    @Override
    public void logRequired(RequiredAuditEvent event) {
        validateRequired(event);
        AuditLog log = AuditLog.builder()
                .createdAt(Instant.now())
                .actorUserId(event.actorUserId())
                .actorType(event.actorType())
                .actorService(event.actorService())
                .subjectUserId(event.subjectUserId())
                .action(event.action())
                .entityType(event.resourceType())
                .entityId(event.resourceId())
                .careContextType(event.careContextType())
                .careContextId(event.careContextId())
                .templateVersionId(event.templateVersionId())
                .checklistTaskInstanceId(event.checklistTaskInstanceId())
                .reasonCode(event.reasonCode())
                .correlationId(event.correlationId())
                .oldValueJson(toJsonStrict(event.beforePayload()))
                .newValueJson(toJsonStrict(event.afterPayload()))
                .build();
        auditLogRepository.save(log);
    }

    @Override
    public void log(AuditAction action, String userId, String resourceId, Object details) {
        java.util.UUID parsedUserId = userId == null ? null : java.util.UUID.fromString(userId);
        log(action, parsedUserId, null, resourceId, details);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> search(
            java.util.UUID userId,
            AuditAction action,
            Instant fromDate,
            Instant toDate,
            Pageable pageable) {
        return auditLogRepository.search(userId, action, fromDate, toDate, pageable)
                .map(auditLogMapper::toResponse);
    }

    private java.util.UUID parseUuidSafely(String value) {
        if (value == null) return null;
        try {
            return java.util.UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String toJson(AuditAction action, Object details) {
        if (details == null) return null;
        try {
            return objectMapper.writeValueAsString(details);
        } catch (Exception e) {
            if (STRICT_ACTIONS.contains(action)) {
                throw new IllegalStateException("Unable to serialize required audit details", e);
            }
            log.warn("AuditService: could not serialize details to JSON — storing as null: {}", e.getMessage());
            return null;
        }
    }

    private String toJsonStrict(Object details) {
        if (details == null) return null;
        try {
            return objectMapper.writeValueAsString(details);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to serialize required audit details", exception);
        }
    }

    private void validateRequired(RequiredAuditEvent event) {
        if (event == null || event.action() == null || !auditEligibilityPolicy.shouldAudit(event.action())) {
            throw new IllegalStateException("Required audit action is not eligible");
        }
        if (!STRICT_ACTIONS.contains(event.action())) {
            throw new IllegalArgumentException("Required audit action has no typed boundary");
        }
        if (event.correlationId() == null) {
            throw new IllegalArgumentException("Required audit correlation is required");
        }
        if (event.actorType() == null || !ALLOWED_ACTOR_TYPES.contains(event.actorType())) {
            throw new IllegalArgumentException("Required audit actor type is invalid");
        }
        if ("USER".equals(event.actorType())) {
            if (event.actorUserId() == null || event.actorService() != null) {
                throw new IllegalArgumentException("User audit actor is invalid");
            }
        } else if (event.actorService() == null
                || !event.actorService().matches("[A-Z][A-Z0-9_]{1,79}")
                || event.actorUserId() != null) {
            throw new IllegalArgumentException("Service audit actor is invalid");
        }
        if ((event.careContextType() == null) != (event.careContextId() == null)) {
            throw new IllegalArgumentException("Audit context type and id must be paired");
        }
        if (event.reasonCode() != null && !event.reasonCode().matches("[A-Z0-9_]{1,80}")) {
            throw new IllegalArgumentException("Required audit reason is not controlled");
        }
        if (event.action() == AuditAction.CARE_TASK_STATUS_UPDATED
                || event.action() == AuditAction.REMINDER_COMPLETED
                || event.action() == AuditAction.REMINDER_SKIPPED) {
            if (event.subjectUserId() == null || event.resourceId() == null
                    || event.beforePayload() == null
                    || event.afterPayload() == null || event.reasonCode() == null) {
                throw new IllegalArgumentException("Typed task audit fields are required");
            }
            if (event.action() == AuditAction.CARE_TASK_STATUS_UPDATED
                    && event.careContextId() == null) {
                throw new IllegalArgumentException("Typed care-task audit context is required");
            }
            if (event.action() == AuditAction.CARE_TASK_STATUS_UPDATED
                    && !Set.of("CARE_TASK", "CareTask").contains(event.resourceType())) {
                throw new IllegalArgumentException("Typed care-task audit resource is invalid");
            }
            if ((event.action() == AuditAction.REMINDER_COMPLETED
                    || event.action() == AuditAction.REMINDER_SKIPPED)
                    && !"ReminderOccurrence".equals(event.resourceType())) {
                throw new IllegalArgumentException("Typed reminder audit resource is invalid");
            }
            validateTaskPayload(event.beforePayload(), "before");
            validateTaskPayload(event.afterPayload(), "after");
            if (!event.beforePayload().keySet().equals(event.afterPayload().keySet())) {
                throw new IllegalArgumentException("Typed task audit payload shapes must match");
            }
            validateTaskTransition(event);
        }
        if (event.action() == AuditAction.CARE_GROUP_CONTEXT_RELINKED) {
            if (!"USER".equals(event.actorType())
                    || event.actorUserId() == null
                    || event.actorService() != null
                    || event.subjectUserId() == null
                    || !event.subjectUserId().equals(event.actorUserId())
                    || !"CARE_GROUP_CONTEXT".equals(event.resourceType())
                    || event.resourceId() == null
                    || event.careContextType() != ChecklistCareContextType.JOURNEY
                    || event.careContextId() == null
                    || event.previousCareContextId() == null
                    || event.previousCareContextId().equals(event.careContextId())
                    || event.templateVersionId() != null
                    || event.checklistTaskInstanceId() != null
                    || event.beforePayload() == null
                    || event.afterPayload() == null) {
                throw new IllegalArgumentException("Typed care-group relink audit fields are required");
            }
            validateJourneyPayload(event.beforePayload(), event.previousCareContextId(), "before");
            validateJourneyPayload(event.afterPayload(), event.careContextId(), "after");
        }
        if (event.action() == AuditAction.CHECKLIST_QUARANTINE_VIEWED) {
            if (!"USER".equals(event.actorType())
                    || event.subjectUserId() == null
                    || !event.subjectUserId().equals(event.actorUserId())
                    || !"CHECKLIST_MIGRATION_QUARANTINE_COLLECTION".equals(event.resourceType())
                    || event.resourceId() != null
                    || event.afterPayload() == null
                    || !"OPERATIONS_READ".equals(event.reasonCode())) {
                throw new IllegalArgumentException("Typed quarantine-view audit fields are required");
            }
        }
        if (event.action() == AuditAction.CHECKLIST_QUARANTINE_RESOLVED) {
            if (!"USER".equals(event.actorType())
                    || event.subjectUserId() == null
                    || !event.subjectUserId().equals(event.actorUserId())
                    || !"CHECKLIST_MIGRATION_QUARANTINE".equals(event.resourceType())
                    || event.resourceId() == null
                    || event.afterPayload() == null
                    || event.reasonCode() == null) {
                throw new IllegalArgumentException("Typed quarantine-resolution audit fields are required");
            }
        }
        if (CHECKLIST_WRITER_ACTIONS.contains(event.action())) {
            throw new IllegalArgumentException("Use ChecklistAuditWriter for checklist lifecycle audit actions");
        }
        if (event.action() == AuditAction.CHECKLIST_RETENTION_PURGED) {
            throw new IllegalArgumentException("Use the retention purge audit transaction boundary");
        }
    }

    private static void validateJourneyPayload(
            Map<String, Object> payload,
            java.util.UUID expectedJourneyId,
            String label) {
        if (!payload.keySet().equals(RELINK_PAYLOAD_KEYS)) {
            throw new IllegalArgumentException("Typed care-group relink " + label + " payload is invalid");
        }
        if (!ChecklistCareContextType.JOURNEY.name().equals(payload.get("careContextType"))) {
            throw new IllegalArgumentException("Typed care-group relink " + label + " payload is invalid");
        }
        Object rawContextId = payload.get("careContextId");
        if (!(rawContextId instanceof java.util.UUID contextId)) {
            throw new IllegalArgumentException("Typed care-group relink " + label + " payload is invalid");
        }
        if (expectedJourneyId != null && !expectedJourneyId.equals(contextId)) {
            throw new IllegalArgumentException("Typed care-group relink " + label + " payload is invalid");
        }
    }

    private static void validateTaskPayload(Map<String, Object> payload, String label) {
        if (!payload.containsKey("status") || !TASK_PAYLOAD_KEYS.containsAll(payload.keySet())) {
            throw new IllegalArgumentException("Typed task audit " + label + " payload is invalid");
        }
        Object status = payload.get("status");
        if (!(status instanceof String statusText) || !statusText.matches("[A-Z][A-Z0-9_]{0,79}")) {
            throw new IllegalArgumentException("Typed task audit " + label + " status is invalid");
        }
        Object action = payload.get("action");
        if (payload.containsKey("action")
                && (!(action instanceof String actionText) || !TASK_ACTIONS.contains(actionText))) {
            throw new IllegalArgumentException("Typed task audit " + label + " action is invalid");
        }
        Object careGroupId = payload.get("careGroupId");
        if (payload.containsKey("careGroupId") && !(careGroupId instanceof java.util.UUID)) {
            throw new IllegalArgumentException("Typed task audit " + label + " care group is invalid");
        }
    }

    private static void validateTaskTransition(RequiredAuditEvent event) {
        String beforeStatus = (String) event.beforePayload().get("status");
        String afterStatus = (String) event.afterPayload().get("status");
        Object beforeAction = event.beforePayload().get("action");
        Object afterAction = event.afterPayload().get("action");

        if (!java.util.Objects.equals(beforeAction, afterAction)) {
            throw new IllegalArgumentException("Typed task audit actions must match");
        }

        if (event.beforePayload().containsKey("careGroupId")
                && !java.util.Objects.equals(
                        event.beforePayload().get("careGroupId"), event.afterPayload().get("careGroupId"))) {
            throw new IllegalArgumentException("Typed task audit care group must match");
        }

        if (event.action() == AuditAction.REMINDER_COMPLETED
                || event.action() == AuditAction.REMINDER_SKIPPED) {
            if (!REMINDER_STATUSES.contains(beforeStatus) || !REMINDER_STATUSES.contains(afterStatus)) {
                throw new IllegalArgumentException("Typed reminder audit status is invalid");
            }
            String expectedAfter = event.action() == AuditAction.REMINDER_COMPLETED
                    ? ReminderStatus.COMPLETED.name()
                    : ReminderStatus.SKIPPED.name();
            String expectedAction = event.action() == AuditAction.REMINDER_COMPLETED ? "COMPLETE" : "SKIP";
            if (beforeAction != null && !expectedAction.equals(beforeAction)) {
                throw new IllegalArgumentException("Typed reminder audit action is invalid");
            }
            if (!REMINDER_ACTIONABLE_STATUSES.contains(beforeStatus) || !expectedAfter.equals(afterStatus)) {
                throw new IllegalArgumentException("Typed reminder audit transition is invalid");
            }
            return;
        }

        if (!CARE_TASK_STATUSES.contains(beforeStatus) || !CARE_TASK_STATUSES.contains(afterStatus)) {
            throw new IllegalArgumentException("Typed care-task audit status is invalid");
        }

        CareTaskStatus before = normalizeCareTaskStatus(beforeStatus);
        CareTaskStatus after = normalizeCareTaskStatus(afterStatus);
        if (!before.canTransitionTo(after)) {
            throw new IllegalArgumentException("Typed care-task audit transition is invalid");
        }

        if (beforeAction instanceof String action) {
            if (!Set.of(CareTaskStatus.OPEN, CareTaskStatus.IN_PROGRESS, CareTaskStatus.NEEDS_SUPPORT)
                    .contains(before)) {
                throw new IllegalArgumentException("Typed care-task audit transition is invalid");
            }
            if ("COMPLETE".equals(action) && after != CareTaskStatus.DONE) {
                throw new IllegalArgumentException("Typed care-task audit transition is invalid");
            }
            if ("SKIP".equals(action) && after != CareTaskStatus.CANCELLED) {
                throw new IllegalArgumentException("Typed care-task audit transition is invalid");
            }
        }
    }

    private static CareTaskStatus normalizeCareTaskStatus(String status) {
        return switch (status) {
            case "PENDING" -> CareTaskStatus.OPEN;
            case "COMPLETED" -> CareTaskStatus.DONE;
            default -> CareTaskStatus.valueOf(status);
        };
    }
}
