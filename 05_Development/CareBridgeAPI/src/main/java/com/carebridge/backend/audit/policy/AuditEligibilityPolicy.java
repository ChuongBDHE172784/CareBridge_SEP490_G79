package com.carebridge.backend.audit.policy;

import com.carebridge.backend.audit.entity.AuditAction;
import java.util.EnumSet;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class AuditEligibilityPolicy {

    private static final Set<AuditAction> SENSITIVE_ACTIONS = EnumSet.of(
            AuditAction.LOGIN,
            AuditAction.LOGOUT,
            AuditAction.OTP_SENT,
            AuditAction.OTP_RESENT,
            AuditAction.OTP_VERIFIED,
            AuditAction.CONSENT_GRANTED,
            AuditAction.CONSENT_REVOKED,
            AuditAction.CREATE_HEALTH_RECORD,
            AuditAction.VIEW_HEALTH_RECORD,
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
            AuditAction.VIEW_AUDIT_LOG);

    public boolean shouldAudit(AuditAction action) {
        return SENSITIVE_ACTIONS.contains(action);
    }
}
