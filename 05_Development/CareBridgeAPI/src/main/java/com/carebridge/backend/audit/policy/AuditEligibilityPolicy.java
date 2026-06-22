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
            AuditAction.SECURITY_EVENT);

    public boolean shouldAudit(AuditAction action) {
        return SENSITIVE_ACTIONS.contains(action);
    }
}
