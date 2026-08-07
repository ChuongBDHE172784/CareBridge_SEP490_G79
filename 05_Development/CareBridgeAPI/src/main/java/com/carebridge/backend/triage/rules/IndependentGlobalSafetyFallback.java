package com.carebridge.backend.triage.rules;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
        Map<String, String> matched = new LinkedHashMap<>();
        if (signals != null) {
            for (Map.Entry<String, String> entry : GLOBAL_DANGER_SIGNALS.entrySet()) {
                Object raw = signals.get(entry.getKey());
                if (isCurrentlyPresent(raw)) {
                    matched.put(entry.getKey(), entry.getValue());
                }
            }
        }

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
