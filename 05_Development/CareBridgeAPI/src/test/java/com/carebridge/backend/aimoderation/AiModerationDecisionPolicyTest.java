package com.carebridge.backend.aimoderation;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.aimoderation.dto.AiVerdict;
import com.carebridge.backend.aimoderation.dto.AiVerdictMatch;
import com.carebridge.backend.aimoderation.entity.AiClassification;
import com.carebridge.backend.aimoderation.entity.AiModerationPolicy;
import com.carebridge.backend.aimoderation.entity.AiPolicySeverity;
import com.carebridge.backend.aimoderation.entity.AiRecommendedAction;
import com.carebridge.backend.aimoderation.entity.AiViolationCategory;
import com.carebridge.backend.aimoderation.policy.AiModerationDecisionPolicy;
import com.carebridge.backend.aimoderation.policy.AiModerationDecisionPolicy.CaseDecision;
import com.carebridge.backend.content.entity.CasePriority;
import com.carebridge.backend.content.entity.ReportCategory;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AiModerationDecisionPolicyTest {

    private final AiModerationDecisionPolicy policy = new AiModerationDecisionPolicy(0.5);

    private static AiModerationPolicy policyRow(String code, AiPolicySeverity severity, String threshold,
            AiViolationCategory category, ReportCategory reportCategory) {
        return AiModerationPolicy.builder()
                .id(UUID.randomUUID())
                .policyCode(code)
                .name(code)
                .detectionGuidance("g")
                .violationCategory(category)
                .reportCategory(reportCategory)
                .severity(severity)
                .applicableTargetTypes("QUESTION,ANSWER,CONTENT")
                .confidenceThreshold(new BigDecimal(threshold))
                .build();
    }

    private static final Map<String, AiModerationPolicy> POLICIES = Map.of(
            "SPAM_ADVERTISING", policyRow("SPAM_ADVERTISING", AiPolicySeverity.MEDIUM, "0.70",
                    AiViolationCategory.SPAM_ADVERTISING, ReportCategory.SPAM),
            "DANGEROUS_MEDICAL_ADVICE", policyRow("DANGEROUS_MEDICAL_ADVICE", AiPolicySeverity.HIGH, "0.65",
                    AiViolationCategory.DANGEROUS_MEDICAL_ADVICE, ReportCategory.UNSAFE_ADVICE),
            "CHILD_SAFETY", policyRow("CHILD_SAFETY", AiPolicySeverity.CRITICAL, "0.50",
                    AiViolationCategory.CHILD_SAFETY, ReportCategory.OTHER));

    private static AiVerdictMatch match(String code, String confidence) {
        AiModerationPolicy row = POLICIES.get(code);
        return new AiVerdictMatch(row.getId(), code, row.getVersion(), row.getViolationCategory(),
                row.getSeverity(), new BigDecimal(confidence), List.of(), null);
    }

    // Scenario 15 (scope 1): a benign symptom description classified SAFE never creates a case
    @Test
    void safeClassification_createsNoCase() {
        AiVerdict verdict = new AiVerdict(AiClassification.SAFE, null, new BigDecimal("0.95"),
                List.of(), AiRecommendedAction.NO_ACTION, null);
        assertThat(policy.decide(verdict, POLICIES).createCase()).isFalse();
    }

    // A manipulated recommendedAction cannot force enforcement — SAFE stays SAFE
    @Test
    void inconsistentRecommendation_isIgnored_serverDecides() {
        AiVerdict verdict = new AiVerdict(AiClassification.SAFE, null, new BigDecimal("0.95"),
                List.of(), AiRecommendedAction.ESCALATE, "model asks for escalation anyway");
        assertThat(policy.decide(verdict, POLICIES).createCase()).isFalse();
    }

    @Test
    void spamAboveThreshold_createsNormalPriorityCase() {
        AiVerdict verdict = new AiVerdict(AiClassification.VIOLATION, AiPolicySeverity.MEDIUM,
                new BigDecimal("0.9"), List.of(match("SPAM_ADVERTISING", "0.85")),
                AiRecommendedAction.REVIEW, null);
        CaseDecision decision = policy.decide(verdict, POLICIES);
        assertThat(decision.createCase()).isTrue();
        assertThat(decision.priority()).isEqualTo(CasePriority.NORMAL);
        assertThat(decision.reportCategory()).isEqualTo(ReportCategory.SPAM);
    }

    // Scenario 16 (scope 1): dangerous medical ADVICE (not a symptom description) still goes
    // to priority review via the semantic classifier — the only remaining automated path.
    @Test
    void dangerousMedicalAdvice_createsHighPriorityCase() {
        AiVerdict verdict = new AiVerdict(AiClassification.VIOLATION, AiPolicySeverity.HIGH,
                new BigDecimal("0.8"), List.of(match("DANGEROUS_MEDICAL_ADVICE", "0.8")),
                AiRecommendedAction.PRIORITY_REVIEW, null);
        CaseDecision decision = policy.decide(verdict, POLICIES);
        assertThat(decision.createCase()).isTrue();
        assertThat(decision.priority()).isEqualTo(CasePriority.HIGH);
        assertThat(decision.reportCategory()).isEqualTo(ReportCategory.UNSAFE_ADVICE);
    }

    @Test
    void criticalMatch_createsUrgentPriorityCase() {
        AiVerdict verdict = new AiVerdict(AiClassification.VIOLATION, AiPolicySeverity.CRITICAL,
                new BigDecimal("0.7"), List.of(match("CHILD_SAFETY", "0.6")),
                AiRecommendedAction.ESCALATE, null);
        CaseDecision decision = policy.decide(verdict, POLICIES);
        assertThat(decision.createCase()).isTrue();
        assertThat(decision.priority()).isEqualTo(CasePriority.URGENT);
    }

    @Test
    void lowConfidence_belowAllThresholds_createsNoCase() {
        AiVerdict verdict = new AiVerdict(AiClassification.UNCERTAIN, null, new BigDecimal("0.3"),
                List.of(match("SPAM_ADVERTISING", "0.3")), AiRecommendedAction.NO_ACTION, null);
        assertThat(policy.decide(verdict, POLICIES).createCase()).isFalse();
    }

    @Test
    void uncertainAboveReviewThreshold_createsNormalCase() {
        AiVerdict verdict = new AiVerdict(AiClassification.UNCERTAIN, null, new BigDecimal("0.6"),
                List.of(match("SPAM_ADVERTISING", "0.6")), AiRecommendedAction.REVIEW, null);
        CaseDecision decision = policy.decide(verdict, POLICIES);
        assertThat(decision.createCase()).isTrue();
        assertThat(decision.priority()).isEqualTo(CasePriority.NORMAL);
    }
}
