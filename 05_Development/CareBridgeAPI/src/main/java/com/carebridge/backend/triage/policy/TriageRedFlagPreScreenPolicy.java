package com.carebridge.backend.triage.policy;

import com.carebridge.backend.triage.dto.request.RunIntakeRequest;
import com.carebridge.backend.triage.entity.RedFlagAction;
import com.carebridge.backend.triage.entity.RedFlagRule;
import com.carebridge.backend.triage.entity.RedFlagSeverity;
import com.carebridge.backend.triage.repository.RedFlagRuleRepository;
import com.carebridge.backend.triage.service.TriagePreScreenMetrics;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Intake-path pre-screen over admin-managed {@code red_flag_rules} (CB-TRIAGE-IMP-003).
 *
 * <p>Runs in Spring Boot BEFORE every Python AI call in the three intake flows (C1). Deliberately
 * separate from {@link TriageRedFlagPolicy} (RAG/community gate) — see TDS ADR-001: matching here is
 * diacritic-insensitive and action-aware; the RAG-side policy stays byte-identical (C4).
 *
 * <p>Classification per ADR-002 (C5):
 * <pre>
 *   RED + (ESCALATE|BLOCK) + active  -> ESCALATE_RED   (BLOCK never suppresses an intake)
 *   RED + WARN, or YELLOW (any)      -> ANNOTATE_ONLY
 *   GREEN (any), inactive, no match  -> NO_MATCH
 * </pre>
 * Most-severe-wins when multiple rules match.
 *
 * <p>NEVER throws on rule-lookup failure — degrades to a NO_MATCH/degraded result (ADR-003,
 * BR-SAFETY-TRFP-002). Read-through, no caching (ADR-004 / C7).
 *
 * @version 1.0
 */
@Component
@RequiredArgsConstructor
public class TriageRedFlagPreScreenPolicy {

    private static final Logger log = LoggerFactory.getLogger(TriageRedFlagPreScreenPolicy.class);

    /**
     * Same emergency guidance wording as the existing RAG-side gate (TriageRedFlagPolicy) —
     * duplicated on purpose so that file stays untouched (C4).
     */
    public static final String EMERGENCY_GUIDANCE =
            "Đây có thể là tình huống khẩn cấp y tế. "
                    + "Hãy gọi 115 hoặc đến cơ sở y tế gần nhất ngay lập tức. Đừng chờ đợi.";

    private final RedFlagRuleRepository redFlagRuleRepository;
    private final TriagePreScreenMetrics metrics;

    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    /**
     * Screens an aggregated free-text string against ACTIVE red_flag_rules.
     * Matching: normalize(keyword) contained in normalize(text) (ADR-005 / C6).
     * Blank input never touches the repository (boundary precedent: TriageRedFlagPolicy).
     */
    public PreScreenResult screen(String aggregatedText) {
        if (aggregatedText == null || aggregatedText.isBlank()) {
            return PreScreenResult.noMatch();
        }
        List<RedFlagRule> activeRules;
        try {
            // C7/ADR-004: read-through, one query per invocation, no caching.
            activeRules = redFlagRuleRepository.findByActiveTrue();
        } catch (RuntimeException exception) {
            // C3/ADR-003: degrade to no-op — never throw, never delay the intake.
            // No symptom text in this log line (PDPA hygiene, TDS §14.2).
            log.warn("red_flag_rules lookup failed; triage pre-screen degraded to no-op reason={}",
                    exception.getClass().getSimpleName());
            metrics.recordDegraded("screen");
            return PreScreenResult.degradedNoMatch();
        }
        String normalizedText = normalize(aggregatedText);
        List<String> escalateKeywords = new ArrayList<>();
        List<UUID> escalateRuleIds = new ArrayList<>();
        List<String> annotateKeywords = new ArrayList<>();
        List<UUID> annotateRuleIds = new ArrayList<>();
        for (RedFlagRule rule : activeRules) {
            if (!rule.isActive()) {
                continue; // defensive — the derived query already filters inactive rules
            }
            PreScreenOutcome classification = classify(rule);
            if (classification == PreScreenOutcome.NO_MATCH) {
                continue; // GREEN rules stay inert (ADR-002 / UC-110 ADR-003)
            }
            String normalizedKeyword = rule.getKeyword() == null ? "" : normalize(rule.getKeyword());
            if (normalizedKeyword.isBlank() || !normalizedText.contains(normalizedKeyword)) {
                continue;
            }
            if (classification == PreScreenOutcome.ESCALATE_RED) {
                escalateKeywords.add(rule.getKeyword());
                escalateRuleIds.add(rule.getId());
            } else {
                annotateKeywords.add(rule.getKeyword());
                annotateRuleIds.add(rule.getId());
            }
        }
        // Most-severe-wins (ADR-002): annotation is moot when the AI will not be called.
        if (!escalateRuleIds.isEmpty()) {
            return new PreScreenResult(
                    PreScreenOutcome.ESCALATE_RED, escalateKeywords, escalateRuleIds, false);
        }
        if (!annotateRuleIds.isEmpty()) {
            return new PreScreenResult(
                    PreScreenOutcome.ANNOTATE_ONLY, annotateKeywords, annotateRuleIds, false);
        }
        return PreScreenResult.noMatch();
    }

    /**
     * Convenience overload for the one-shot flow: aggregates the same String fields as
     * SymptomNormalizer.toSearchText — symptomList, symptoms, duration, feedingStatus,
     * breathingStatus, consciousnessStatus, vomiting, diarrhea, rash, parentFreeText,
     * dehydrationSigns — then delegates to {@link #screen(String)}.
     */
    public PreScreenResult screen(RunIntakeRequest request) {
        if (request == null) {
            return PreScreenResult.noMatch();
        }
        List<String> parts = new ArrayList<>();
        if (request.getSymptomList() != null) {
            parts.addAll(request.getSymptomList());
        }
        addIfPresent(parts, request.getSymptoms());
        addIfPresent(parts, request.getDuration());
        addIfPresent(parts, request.getFeedingStatus());
        addIfPresent(parts, request.getBreathingStatus());
        addIfPresent(parts, request.getConsciousnessStatus());
        addIfPresent(parts, request.getVomiting());
        addIfPresent(parts, request.getDiarrhea());
        addIfPresent(parts, request.getRash());
        addIfPresent(parts, request.getParentFreeText());
        if (request.getDehydrationSigns() != null) {
            parts.addAll(request.getDehydrationSigns());
        }
        return screen(String.join(" ", parts));
    }

    /**
     * ADR-002/C5 classification of a rule's runtime effect on the intake path.
     * BLOCK is treated exactly like ESCALATE — suppressing an intake would delay
     * emergency routing (BR-SAFETY), so "block" can only ever mean "escalate" here.
     */
    private PreScreenOutcome classify(RedFlagRule rule) {
        if (rule.getSeverity() == RedFlagSeverity.RED) {
            return rule.getAction() == RedFlagAction.WARN
                    ? PreScreenOutcome.ANNOTATE_ONLY
                    : PreScreenOutcome.ESCALATE_RED; // ESCALATE and BLOCK
        }
        if (rule.getSeverity() == RedFlagSeverity.YELLOW) {
            return PreScreenOutcome.ANNOTATE_ONLY;
        }
        return PreScreenOutcome.NO_MATCH; // GREEN — configuration-only
    }

    /**
     * ADR-005/C6: lowercase(Locale.ROOT) → NFD → strip {@code \p{M}+} → collapse whitespace →
     * trim, applied to BOTH keyword and text. Additionally maps 'đ'→'d' (NFD does not decompose
     * U+0111) — same handling as the existing normalizeAnswerToken precedent in TriageService,
     * required for BR-SAFETY-TRFP-004 ("ngã đập đầu" ↔ "nga dap dau").
     */
    private static String normalize(String value) {
        String decomposed = Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        String stripped = DIACRITICS.matcher(decomposed).replaceAll("").replace('đ', 'd');
        return WHITESPACE.matcher(stripped).replaceAll(" ").trim();
    }

    private static void addIfPresent(List<String> parts, String value) {
        if (value != null && !value.isBlank()) {
            parts.add(value);
        }
    }
}
