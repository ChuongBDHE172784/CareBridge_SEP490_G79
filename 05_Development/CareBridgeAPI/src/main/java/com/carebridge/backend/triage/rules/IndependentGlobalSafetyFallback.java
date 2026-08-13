package com.carebridge.backend.triage.rules;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.text.Normalizer;
import java.util.Locale;

/**
 * A last-resort safety screen that keeps working when everything else is broken.
 *
 * <p>It deliberately depends on <em>nothing</em>: not {@link TriageRuleRegistry}, not the
 * Python service, not Gemini, not LangGraph, not RAG, not Supabase, not the evidence
 * registry, not even a repository. The existing {@code TriageRedFlagPreScreenPolicy} could
 * not fill this role because it reads {@code RedFlagRuleRepository}, so a database or
 * registry outage would take it down too.
 *
 * <p>It is NOT a third rule engine. It recognises only the global danger signals that the
 * V2 registry already carries as {@code globalRed}, with no new thresholds and no new
 * clinical logic. Its whole purpose is that "the rule set failed to load" must never be the
 * reason a seizure goes unanswered.
 *
 * <p>Not clinically validated. Its output is informational risk orientation, not triage.
 */
@Component
public class IndependentGlobalSafetyFallback {

    /**
     * Fixed mirror of the signals the V2 registry marks {@code globalRed}, plus the signals
     * covered by the two active internal safety policies. It is hard-coded on purpose: the
     * fallback must not need to read the artifact that may be the thing that is broken.
     * Adding to this list is a governance change, not a code tidy-up.
     */
    static final Map<String, String> GLOBAL_DANGER_SIGNALS = Map.of(
            "ALTERED_CONSCIOUSNESS", "GLOBAL_IMMEDIATE_DANGER",
            "SEIZURE", "GLOBAL_IMMEDIATE_DANGER",
            "SEVERE_BREATHING_DIFFICULTY", "GLOBAL_IMMEDIATE_DANGER",
            "CYANOSIS", "CYANOSIS_EMERGENCY_SIGNAL",
            "SELF_HARM_IDEATION", "SAFETY_RISK_SELF_OR_INFANT_HARM",
            "SELF_HARM_INTENT_OR_PLAN", "SAFETY_RISK_SELF_OR_INFANT_HARM",
            "HARM_TO_BABY_IDEATION", "SAFETY_RISK_SELF_OR_INFANT_HARM",
            "CANNOT_ENSURE_OWN_SAFETY", "SAFETY_RISK_SELF_OR_INFANT_HARM");

    private static final String ACTION_EMERGENCY = "IMMEDIATE_EMERGENCY_ASSESSMENT";
    private static final String ACTION_SAFETY_SUPPORT = "IMMEDIATE_SAFETY_SUPPORT";
    private static final String ACTION_UNAVAILABLE = "ROUTE_TO_HEALTHCARE_WORKER";

    /** The fallback's verdict. Only RED or NEEDS_MORE_INFO are reachable — never GREEN. */
    public record FallbackVerdict(
            String outcome,
            boolean stopConversation,
            String actionCode,
            List<String> reasonCodes,
            List<String> matchedSignals,
            String completionReason) {
    }

    /**
     * Screen the structured signals only.
     *
     * <p>No caller booleans are accepted: there is deliberately no {@code relevance} or
     * {@code datasetComplete} parameter, so a caller cannot talk this screen out of firing.
     *
     * @param signals signal code → presence. A value may also be a map carrying
     *                {@code presence} and {@code current}; a signal explicitly marked
     *                {@code current=false} is history and is ignored, because a past
     *                seizure is not a seizure happening now.
     */
    public FallbackVerdict screen(Map<String, Object> signals) {
        return screenWithLatestMessage(signals, null);
    }

    /**
     * Screen trusted structured signals and a bounded latest message. Text matching is deliberately
     * limited to unambiguous global danger phrases so the fallback remains independent of Python.
     */
    public FallbackVerdict screenWithLatestMessage(Map<String, Object> signals, String latestMessage) {
        Map<String, String> matched = new LinkedHashMap<>();
        if (signals != null) {
            for (Map.Entry<String, String> entry : GLOBAL_DANGER_SIGNALS.entrySet()) {
                Object raw = signals.get(entry.getKey());
                if (isCurrentlyPresent(raw)) {
                    matched.put(entry.getKey(), entry.getValue());
                }
            }
        }
        detectTextDanger(latestMessage, matched);

        if (!matched.isEmpty()) {
            boolean safetyOnly = matched.values().stream()
                    .allMatch("SAFETY_RISK_SELF_OR_INFANT_HARM"::equals);
            return new FallbackVerdict(
                    "RED",
                    true,
                    safetyOnly ? ACTION_SAFETY_SUPPORT : ACTION_EMERGENCY,
                    List.copyOf(new java.util.LinkedHashSet<>(matched.values())),
                    List.copyOf(matched.keySet()),
                    "GLOBAL_SAFETY_SIGNAL_PRESENT");
        }

        // Nothing explicit. Absence of evidence is not evidence of safety: never GREEN,
        // never an invented YELLOW, and never OUT_OF_SCOPE just because data is thin.
        return new FallbackVerdict(
                "NEEDS_MORE_INFO",
                true,
                ACTION_UNAVAILABLE,
                List.of("TRIAGE_V2_UNAVAILABLE"),
                List.of(),
                "V2_UNAVAILABLE_NO_GLOBAL_SIGNAL");
    }

    private static void detectTextDanger(String message, Map<String, String> matched) {
        if (message == null || message.isBlank()) return;
        String text = fold(message.substring(0, Math.min(message.length(), 2000)));
        addTextSignal(text, matched, "SEIZURE", "GLOBAL_IMMEDIATE_DANGER",
                List.of("dang co giat", "bi co giat", "len con co giat", "is seizing", "having a seizure"),
                List.of("khong co giat", "khong bi co giat", "not seizing", "no seizure"));
        addTextSignal(text, matched, "ALTERED_CONSCIOUSNESS", "GLOBAL_IMMEDIATE_DANGER",
                List.of("bat tinh", "khong danh thuc duoc", "mat y thuc", "unconscious", "cannot wake"),
                List.of("khong bat tinh", "van tinh tao", "not unconscious"));
        addTextSignal(text, matched, "SEVERE_BREATHING_DIFFICULTY", "GLOBAL_IMMEDIATE_DANGER",
                List.of("khong tho duoc", "ngung tho", "tho rat kho", "cannot breathe", "stopped breathing"),
                List.of("khong kho tho", "tho binh thuong", "breathing normally"));
        addTextSignal(text, matched, "CYANOSIS", "CYANOSIS_EMERGENCY_SIGNAL",
                List.of("tim tai", "moi tim", "blue lips", "turning blue"),
                List.of("khong tim tai", "moi khong tim"));
        addTextSignal(text, matched, "SELF_HARM_IDEATION", "SAFETY_RISK_SELF_OR_INFANT_HARM",
                List.of("muon tu tu", "muon lam hai ban than", "suicidal", "harm myself"),
                List.of("khong muon tu tu", "khong lam hai ban than"));
        addTextSignal(text, matched, "HARM_TO_BABY_IDEATION", "SAFETY_RISK_SELF_OR_INFANT_HARM",
                List.of("muon lam hai em be", "muon lam hai con", "harm my baby"),
                List.of("khong muon lam hai em be", "khong lam hai con"));
    }

    private static void addTextSignal(String text, Map<String, String> matched,
                                      String signal, String reason,
                                      List<String> positives, List<String> negatives) {
        if (positives.stream().anyMatch(positive ->
                containsOccurrenceOutsideNegation(text, positive, negatives))) {
            matched.putIfAbsent(signal, reason);
        }
    }

    private static boolean containsOccurrenceOutsideNegation(
            String text, String positive, List<String> negatives) {
        for (int positiveAt = text.indexOf(positive); positiveAt >= 0;
             positiveAt = text.indexOf(positive, positiveAt + 1)) {
            int positiveEnd = positiveAt + positive.length();
            boolean negated = false;
            for (String negative : negatives) {
                for (int negativeAt = text.indexOf(negative); negativeAt >= 0;
                     negativeAt = text.indexOf(negative, negativeAt + 1)) {
                    int negativeEnd = negativeAt + negative.length();
                    if (positiveAt < negativeEnd && negativeAt < positiveEnd) {
                        negated = true;
                        break;
                    }
                }
                if (negated) break;
            }
            if (!negated) return true;
        }
        return false;
    }

    private static String fold(String value) {
        String normalized = Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}", "").replace('đ', 'd')
                .replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim();
    }

    /** True only for a signal the user reports as present AND current. */
    private static boolean isCurrentlyPresent(Object raw) {
        if (raw == null) {
            return false;
        }
        if (raw instanceof Map<?, ?> observation) {
            Object current = observation.get("current");
            if (Boolean.FALSE.equals(current) || "false".equals(current)) {
                // Historical signal (e.g. carried in from health memory) — not current.
                return false;
            }
            return Presence.parse(observation.get("presence")) == Presence.PRESENT;
        }
        return Presence.parse(raw) == Presence.PRESENT;
    }
}
