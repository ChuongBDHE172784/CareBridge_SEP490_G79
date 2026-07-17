package com.carebridge.backend.security.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.policy.AuditEligibilityPolicy;
import org.junit.jupiter.api.Test;

class FederatedIdentityLinkAuditPolicyTest {

    private final AuditEligibilityPolicy policy = new AuditEligibilityPolicy();

    @Test
    void federatedAuthenticationAndIdentityLinkActions_areAuditable() {
        assertThat(policy.shouldAudit(AuditAction.FEDERATED_LOGIN)).isTrue();
        assertThat(policy.shouldAudit(AuditAction.FEDERATED_REGISTRATION)).isTrue();
        assertThat(policy.shouldAudit(AuditAction.FEDERATED_IDENTITY_LINKED)).isTrue();
    }
}
