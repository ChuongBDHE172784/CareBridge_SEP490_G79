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
        NormalizedText normalizedText = normalizeText(aggregatedText);
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
            if (!matches(normalizedText, rule.getKeyword())) {
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

    /** One input in both spellings, character-aligned. See {@link #stripAligned}. */
    private record NormalizedText(String accented, String stripped) {
    }

    /**
     * ADR-005/C6: lowercase(Locale.ROOT) → collapse whitespace → trim, kept in both the
     * accented and the diacritic-free spelling. Both are derived from the same lowercased,
     * whitespace-collapsed string, so their character offsets agree — which is what
     * {@link #matches} relies on.
     */
    private static NormalizedText normalizeText(String value) {
        String accented = WHITESPACE
                .matcher(Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFC))
                .replaceAll(" ")
                .trim();
        return new NormalizedText(accented, stripAligned(accented));
    }

    /**
     * Strips diacritics one character at a time so the result keeps the input's length.
     * Also maps 'đ'→'d' (NFD does not decompose U+0111) — same handling as the existing
     * normalizeAnswerToken precedent in TriageService, required for BR-SAFETY-TRFP-004
     * ("ngã đập đầu" ↔ "nga dap dau").
     */
    private static String stripAligned(String value) {
        StringBuilder result = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character == 'đ') {
                result.append('d');
                continue;
            }
            String base = DIACRITICS
                    .matcher(Normalizer.normalize(String.valueOf(character), Normalizer.Form.NFD))
                    .replaceAll("");
            // Anything that does not reduce to exactly one character is left alone rather than
            // silently shifting every offset after it.
            result.append(base.length() == 1 ? base : character);
        }
        return result.toString();
    }

    /**
     * Whether {@code rawKeyword} occurs in {@code text}, with diacritics respected.
     *
     * <p>Stripping diacritics from both sides merges unrelated Vietnamese words onto one ASCII
     * form, and this screen escalates rather than annotates: the seeded keyword "co giật"
     * (seizure) matched "có giặt" ("does the washing"), which short-circuits the whole intake
     * into an emergency. So a keyword written WITH diacritics only matches accented text, or a
     * span the writer left accent-free — where the two genuinely cannot be told apart and the
     * clinical reading is the safe one.
     *
     * <p>A keyword stored WITHOUT diacritics stays permissive and matches either spelling.
     * That direction is required by BR-SAFETY-TRFP-004 (TRFP-TC-003) and is the admin's own
     * choice of how to write the rule; nothing here can recover an intent they did not spell.
     */
    private static boolean matches(NormalizedText text, String rawKeyword) {
        if (rawKeyword == null || rawKeyword.isBlank()) {
            return false;
        }
        NormalizedText keyword = normalizeText(rawKeyword);
        if (keyword.accented().isBlank()) {
            return false;
        }
        boolean keywordCarriesDiacritics = !keyword.accented().equals(keyword.stripped());
        if (!keywordCarriesDiacritics) {
            return text.stripped().contains(keyword.stripped());
        }
        if (text.accented().contains(keyword.accented())) {
            return true;
        }
        for (int index = text.stripped().indexOf(keyword.stripped());
                index >= 0;
                index = text.stripped().indexOf(keyword.stripped(), index + 1)) {
            int end = index + keyword.stripped().length();
            if (text.accented().substring(index, end).equals(text.stripped().substring(index, end))) {
                return true;
            }
        }
        return false;
    }

    private static void addIfPresent(List<String> parts, String value) {
        if (value != null && !value.isBlank()) {
            parts.add(value);
        }
    }
}
