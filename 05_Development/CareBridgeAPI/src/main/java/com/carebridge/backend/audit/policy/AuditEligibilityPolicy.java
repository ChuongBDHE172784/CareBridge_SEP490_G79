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
            AuditAction.SAFETY_MONITORING_ENABLED,
            AuditAction.SAFETY_MONITORING_DISABLED,
            AuditAction.SAFETY_EVENT_RECORDED,
            AuditAction.SAFETY_EVENT_RESPONDED,
            AuditAction.SAFETY_EVENT_ESCALATED,
            AuditAction.EMERGENCY_ALERT_DELIVERY,
            AuditAction.VIEW_AUDIT_LOG);

    public boolean shouldAudit(AuditAction action) {
        return SENSITIVE_ACTIONS.contains(action);
    }
}
