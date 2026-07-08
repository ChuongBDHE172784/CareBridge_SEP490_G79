package com.carebridge.backend.triage;

import com.carebridge.backend.triage.dto.request.CreateRedFlagRuleRequest;
import com.carebridge.backend.triage.dto.request.UpdateRedFlagRuleRequest;
import com.carebridge.backend.triage.entity.RedFlagAction;
import com.carebridge.backend.triage.entity.RedFlagRule;
import com.carebridge.backend.triage.entity.RedFlagSeverity;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.Consumer;

// CASE 2.0 Props Isolation Pattern — each @Test uses makeXxx()
class RedFlagRuleTestFactory {

    static final UUID SYSTEM_ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-0000000000aa");

    // Baseline system-default rule — mirrors FX-001 (Test-Spec §3 TDS-05)
    static RedFlagRule makeSystemDefaultRule() {
        RedFlagRule rule = new RedFlagRule();
        rule.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        rule.setKeyword("chảy máu nhiều");
        rule.setSeverity(RedFlagSeverity.RED);
        rule.setAction(RedFlagAction.ESCALATE);
        rule.setActive(true);
        rule.setSystemDefault(true);
        rule.setCreatedBy(null);
        rule.setCreatedAt(LocalDateTime.now());
        rule.setUpdatedAt(LocalDateTime.now());
        return rule;
    }

    // Baseline admin-created (non-default) rule
    static RedFlagRule makeAdminRule() {
        RedFlagRule rule = new RedFlagRule();
        rule.setId(UUID.fromString("00000000-0000-0000-0000-000000000002"));
        rule.setKeyword("từ khoá thử nghiệm khẩn cấp");
        rule.setSeverity(RedFlagSeverity.RED);
        rule.setAction(RedFlagAction.ESCALATE);
        rule.setActive(true);
        rule.setSystemDefault(false);
        rule.setCreatedBy(SYSTEM_ADMIN_ID);
        rule.setCreatedAt(LocalDateTime.now());
        rule.setUpdatedAt(LocalDateTime.now());
        return rule;
    }

    // Overload to override specific fields — no shared mutable instance between tests
    static RedFlagRule makeAdminRule(Consumer<RedFlagRule> overrides) {
        RedFlagRule rule = makeAdminRule();
        overrides.accept(rule);
        return rule;
    }

    static CreateRedFlagRuleRequest makeCreateRequest() {
        return new CreateRedFlagRuleRequest("ra máu nhiều khi mang thai", RedFlagSeverity.RED, RedFlagAction.ESCALATE);
    }

    static UpdateRedFlagRuleRequest makeDeactivateRequest() {
        return new UpdateRedFlagRuleRequest(null, null, null, false);
    }
}
