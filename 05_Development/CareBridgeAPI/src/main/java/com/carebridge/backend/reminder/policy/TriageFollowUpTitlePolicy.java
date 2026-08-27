package com.carebridge.backend.reminder.policy;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * CB-TYFU-IMP-001 ADR-TYFU-006 — pure domain rule mapping canonical symptom codes
 * (SymptomNormalizer vocabulary) to a fixed Vietnamese follow-up title.
 * First match wins in the policy's own fixed priority order — never the caller's
 * input order; unmapped/empty/null input yields the generic fallback (BR-TYFU-004:
 * raw free text is never used as a title).
 *
 * @version 1.0
 */
@Component
public class TriageFollowUpTitlePolicy {

    /** ADR-TYFU-006 fallback row. */
    static final String GENERIC_TITLE = "Theo dõi lại tình trạng sức khỏe của bé sau sàng lọc AI";

    /** ADR-TYFU-006 priority table — order is significant (first match wins). */
    private record TitleRule(Set<String> codes, String title) {
    }

    private static final List<TitleRule> PRIORITY_RULES = List.of(
            new TitleRule(Set.of("fever", "high_fever"),
                    "Kiểm tra lại thân nhiệt của bé"),
            new TitleRule(Set.of("vomiting", "persistent_vomiting"),
                    "Kiểm tra lại tình trạng nôn trớ của bé"),
            new TitleRule(Set.of("diarrhea", "mild_dehydration", "severe_dehydration"),
                    "Kiểm tra lại tình trạng đi ngoài và dấu hiệu mất nước của bé"),
            new TitleRule(Set.of("cough", "runny_nose", "difficulty_breathing"),
                    "Kiểm tra lại tình trạng ho và nhịp thở của bé"));

    /**
     * Maps canonical symptom codes to a follow-up title per the ADR-TYFU-006 priority
     * table; returns the generic fallback for null/empty/unmapped input.
     * Never returns null; result length &lt;= 255 (DDL {@code title varchar(255)}).
     */
    public String deriveTitle(List<String> canonicalSymptoms) {
        if (canonicalSymptoms == null || canonicalSymptoms.isEmpty()) {
            return GENERIC_TITLE;
        }
        for (TitleRule rule : PRIORITY_RULES) {
            if (canonicalSymptoms.stream().anyMatch(rule.codes()::contains)) {
                return rule.title();
            }
        }
        return GENERIC_TITLE;
    }
}
