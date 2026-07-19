package com.carebridge.backend.audit.policy;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.audit.entity.AuditAction;
import org.junit.jupiter.api.Test;

/**
 * UC117-TC-004 — AuditEligibilityPolicy.shouldAudit(VIEW_AUDIT_LOG) returns true
 * (ADR-AUDIT-001 meta-audit wiring, Track B).
 */
class AuditEligibilityPolicyTest {

    private final AuditEligibilityPolicy policy = new AuditEligibilityPolicy();

    @Test
    void shouldAudit_viewAuditLog_returnsTrue() {
        assertThat(policy.shouldAudit(AuditAction.VIEW_AUDIT_LOG)).isTrue();
    }

    @Test
    void shouldAudit_regressionGuard_previouslySensitiveActionsStillTrue() {
        assertThat(policy.shouldAudit(AuditAction.SECURITY_EVENT)).isTrue();
        assertThat(policy.shouldAudit(AuditAction.LOGIN)).isTrue();
        assertThat(policy.shouldAudit(AuditAction.PROFILE_VIEWED)).isTrue();
    }

    @Test
    void shouldAudit_journeyLifecycleMutations_returnsTrue() {
        assertThat(policy.shouldAudit(AuditAction.JOURNEY_CREATED)).isTrue();
        assertThat(policy.shouldAudit(AuditAction.JOURNEY_UPDATED)).isTrue();
    }
}
