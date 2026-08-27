package com.carebridge.backend.aimoderation.policy;

import com.carebridge.backend.aimoderation.dto.AiVerdict;
import com.carebridge.backend.aimoderation.dto.AiVerdictMatch;
import com.carebridge.backend.aimoderation.entity.AiClassification;
import com.carebridge.backend.aimoderation.entity.AiModerationPolicy;
import com.carebridge.backend.aimoderation.entity.AiPolicySeverity;
import com.carebridge.backend.content.entity.CasePriority;
import com.carebridge.backend.content.entity.ReportCategory;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Server-side decision matrix. The model's recommendedAction is ignored for enforcement —
 * only classification + per-policy confidence thresholds decide whether a human-review case
 * is opened, and at which priority. AI never suspends, hides or restricts anything here; the
 * strongest possible outcome is a PENDING case awaiting a moderator.
 */
@Component
public class AiModerationDecisionPolicy {

    public record CaseDecision(boolean createCase, CasePriority priority, ReportCategory reportCategory,
                               String primaryPolicyCode) {

        public static CaseDecision none() {
            return new CaseDecision(false, null, null, null);
        }
    }

    private final BigDecimal reviewConfidenceThreshold;

    public AiModerationDecisionPolicy(
            @Value("${carebridge.gemini.moderation.review-confidence-threshold:0.5}") double reviewConfidenceThreshold) {
        this.reviewConfidenceThreshold = BigDecimal.valueOf(reviewConfidenceThreshold);
    }

    public CaseDecision decide(AiVerdict verdict, Map<String, AiModerationPolicy> policiesByCode) {
        if (verdict == null || verdict.classification() == AiClassification.SAFE) {
            return CaseDecision.none();
        }

        List<AiVerdictMatch> qualified = verdict.matchedPolicies().stream()
                .filter(match -> {
                    AiModerationPolicy policy = policiesByCode.get(match.policyCode());
                    return policy != null && match.confidence().compareTo(policy.getConfidenceThreshold()) >= 0;
                })
                .toList();

        if (!qualified.isEmpty()) {
            AiVerdictMatch top = qualified.stream()
                    .max(Comparator.comparing(AiVerdictMatch::severity)
                            .thenComparing(AiVerdictMatch::confidence))
                    .orElseThrow();
            AiModerationPolicy policy = policiesByCode.get(top.policyCode());
            return new CaseDecision(true, priorityFor(top.severity()), policy.getReportCategory(), top.policyCode());
        }

        // UNCERTAIN (or VIOLATION downgraded by the parser) above the global review threshold
        // still deserves human eyes, at normal priority.
        if (verdict.confidence() != null && verdict.confidence().compareTo(reviewConfidenceThreshold) >= 0) {
            AiVerdictMatch best = verdict.matchedPolicies().stream()
                    .max(Comparator.comparing(AiVerdictMatch::confidence))
                    .orElse(null);
            ReportCategory category = best != null && policiesByCode.containsKey(best.policyCode())
                    ? policiesByCode.get(best.policyCode()).getReportCategory()
                    : ReportCategory.OTHER;
            return new CaseDecision(true, CasePriority.NORMAL, category,
                    best != null ? best.policyCode() : null);
        }

        return CaseDecision.none();
    }

    private static CasePriority priorityFor(AiPolicySeverity severity) {
        return switch (severity) {
            case CRITICAL -> CasePriority.URGENT;
            case HIGH -> CasePriority.HIGH;
            case MEDIUM, LOW -> CasePriority.NORMAL;
        };
    }
}
