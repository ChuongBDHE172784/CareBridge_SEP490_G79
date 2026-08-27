package com.carebridge.backend.aimoderation.dto;

import com.carebridge.backend.aimoderation.entity.AiPolicySeverity;
import com.carebridge.backend.aimoderation.entity.AiViolationCategory;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * One validated policy match. Doubles as the JSON storage shape persisted in
 * ai_content_assessments.matches_jsonb — an immutable snapshot of the policy (id/code/version,
 * category/severity) at scan time, so later policy edits never mutate old assessments.
 * category/severity are taken from the server-side policy row (authoritative), never from the
 * model output; evidence excerpts are verified substrings of the scanned content.
 */
public record AiVerdictMatch(
        UUID policyId,
        String policyCode,
        int policyVersion,
        AiViolationCategory category,
        AiPolicySeverity severity,
        BigDecimal confidence,
        List<String> evidence,
        String explanation
) {
}
