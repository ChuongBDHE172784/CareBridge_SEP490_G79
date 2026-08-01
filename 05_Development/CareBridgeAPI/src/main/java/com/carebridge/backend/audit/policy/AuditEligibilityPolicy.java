package com.carebridge.backend.audit.policy;

import com.carebridge.backend.audit.entity.AuditAction;
import java.util.EnumSet;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class AuditEligibilityPolicy {

    private static final Set<AuditAction> SENSITIVE_ACTIONS = EnumSet.of(
            AuditAction.LOGIN,
            AuditAction.FEDERATED_LOGIN,
            AuditAction.FEDERATED_REGISTRATION,
            AuditAction.FEDERATED_IDENTITY_LINKED,
            AuditAction.LOGOUT,
            AuditAction.OTP_SENT,
            AuditAction.OTP_RESENT,
            AuditAction.OTP_VERIFIED,
            AuditAction.CONSENT_GRANTED,
            AuditAction.CONSENT_REVOKED,
            AuditAction.CREATE_HEALTH_RECORD,
            AuditAction.VIEW_HEALTH_RECORD,
            AuditAction.JOURNEY_CREATED,
            AuditAction.JOURNEY_UPDATED,
            AuditAction.BABY_JOURNEY_LINK_ACCEPTED,
            AuditAction.BABY_JOURNEY_LINK_REJECTED,
            AuditAction.EXPERT_VERIFICATION,
            AuditAction.MODERATION_ACTION,
            AuditAction.AI_TRIAGE,
            AuditAction.PAYMENT,
            AuditAction.SECURITY_EVENT,
            AuditAction.PROFILE_VIEWED,
            AuditAction.PROFILE_UPDATED,
            AuditAction.USER_ACCOUNT_STATUS_CHANGED,
            AuditAction.STAFF_ACCOUNT_CREATED,
            AuditAction.ROLE_PERMISSION_UPDATED,
            AuditAction.POSTURE_CONFIG_CREATED,
            AuditAction.POSTURE_CONFIG_UPDATED,
            AuditAction.POSTURE_CONFIG_ACTIVATED,
            AuditAction.EXERCISE_CREATED,
            AuditAction.EXERCISE_UPDATED,
            AuditAction.EXERCISE_ACTIVATED,
            AuditAction.EXERCISE_DISABLED,
            AuditAction.DIRECT_CONVERSATION_OPENED,
            AuditAction.DIRECT_MESSAGE_SENT,
            AuditAction.DIRECT_CHAT_ACCESS_DENIED,
            AuditAction.DIRECT_CALL_INITIATED,
            AuditAction.DIRECT_CALL_STATE_CHANGED,
            AuditAction.DIRECT_CALL_ACCESS_DENIED,
            AuditAction.DIRECT_CALL_MISSED_BY_TIMEOUT,
            AuditAction.FIREBASE_CUSTOM_TOKEN_ISSUED,
            AuditAction.CHECKLIST_ITEM_ADDED,
            // Version history is projected from immutable audit events; these snapshots are
            // intentionally retained for the content-management workspace.
            AuditAction.CONTENT_CREATED,
            AuditAction.CONTENT_UPDATED,
            AuditAction.CHECKLIST_TEMPLATE_CREATED,
            AuditAction.CHECKLIST_TEMPLATE_UPDATED,
            AuditAction.CHECKLIST_TEMPLATE_ARCHIVED,
            AuditAction.CHECKLIST_TEMPLATE_DECIDED,
            AuditAction.CHECKLIST_DISTRIBUTED,
            AuditAction.CHECKLIST_ASSIGNED,
            AuditAction.CHECKLIST_COMPLETED,
            AuditAction.CHECKLIST_SKIPPED,
            AuditAction.CHECKLIST_REOPENED,
            AuditAction.CHECKLIST_CANCELLED,
            AuditAction.CHECKLIST_RECONCILIATION_FAILED,
            AuditAction.CHECKLIST_MIGRATION_QUARANTINED,
            AuditAction.CHECKLIST_QUARANTINE_VIEWED,
            AuditAction.CHECKLIST_QUARANTINE_RESOLVED,
            AuditAction.CHECKLIST_RETENTION_PURGED,
            // CB-TYFU-TDD-001 (TriageYellowFollowUp TDS §NFR "Audit — 100% via audit_log
            // query", Luật 91/2025): REMINDER_CREATED must persist to audit_events; without
            // eligibility the service's auditService.log(...) call was silently dropped.
            AuditAction.REMINDER_CREATED,
            AuditAction.REMINDER_COMPLETED,
            AuditAction.REMINDER_SKIPPED,
            AuditAction.CARE_TASK_STATUS_UPDATED,
            AuditAction.CARE_GROUP_CONTEXT_RELINKED,
            AuditAction.SAFETY_MONITORING_ENABLED,
            AuditAction.SAFETY_MONITORING_DISABLED,
            AuditAction.SAFETY_EVENT_RECORDED,
            AuditAction.SAFETY_EVENT_RESPONDED,
            AuditAction.SAFETY_EVENT_ESCALATED,
            AuditAction.EMERGENCY_ALERT_DELIVERY,
            AuditAction.AI_POLICY_CREATED,
            AuditAction.AI_POLICY_UPDATED,
            AuditAction.AI_POLICY_STATUS_CHANGED,
            AuditAction.AI_POLICY_TEST_RUN,
            AuditAction.AI_SCAN_COMPLETED,
            AuditAction.AI_SCAN_FAILED,
            AuditAction.AI_CASE_CREATED,
            AuditAction.AI_FEEDBACK_SUBMITTED,
            AuditAction.AI_RESCAN_REQUESTED,
            AuditAction.REPORT_CLAIMED,
            AuditAction.REPORT_RELEASED,
            AuditAction.SYSTEM_CONFIGURATION_UPDATED,
            AuditAction.VIEW_AUDIT_LOG);

    public boolean shouldAudit(AuditAction action) {
        return SENSITIVE_ACTIONS.contains(action);
    }
}
