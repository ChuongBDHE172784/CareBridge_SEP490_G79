package com.carebridge.backend.triage.rules;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The fallback exists for exactly one scenario: everything else is broken and someone is
 * describing a seizure. These tests pin that it still answers, and that it never invents
 * reassurance when it has nothing to go on.
 */
class IndependentGlobalSafetyFallbackTest {

    private final IndependentGlobalSafetyFallback fallback = new IndependentGlobalSafetyFallback();

    @ParameterizedTest
    @ValueSource(strings = {
            "SEVERE_BREATHING_DIFFICULTY", "SEIZURE", "ALTERED_CONSCIOUSNESS", "CYANOSIS"})
    @DisplayName("A global danger signal is RED with no registry, no Python and no Gemini")
    void globalDangerSignalIsRedWithoutAnyCollaborator(String signal) {
        var verdict = fallback.screen(Map.of(signal, "PRESENT"));

        assertThat(verdict.outcome()).isEqualTo("RED");
        assertThat(verdict.stopConversation()).isTrue();
        assertThat(verdict.matchedSignals()).contains(signal);
        assertThat(verdict.completionReason()).isEqualTo("GLOBAL_SAFETY_SIGNAL_PRESENT");
    }

    @Test
    @DisplayName("Self-harm signals route to safety support, not to emergency assessment")
    void selfHarmRoutesToSafetySupport() {
        var verdict = fallback.screen(Map.of("SELF_HARM_INTENT_OR_PLAN", "PRESENT"));

        assertThat(verdict.outcome()).isEqualTo("RED");
        assertThat(verdict.actionCode()).isEqualTo("IMMEDIATE_SAFETY_SUPPORT");
        assertThat(verdict.reasonCodes()).containsExactly("SAFETY_RISK_SELF_OR_INFANT_HARM");
    }

    @Test
    @DisplayName("All-unknown input yields NEEDS_MORE_INFO — never GREEN, never OUT_OF_SCOPE")
    void unknownInputNeverYieldsGreenOrOutOfScope() {
        var verdict = fallback.screen(Map.of(
                "SEIZURE", "UNKNOWN",
                "SEVERE_BREATHING_DIFFICULTY", "UNAWARE_OR_UNMEASURABLE",
                "ALTERED_CONSCIOUSNESS", "CONFLICTED"));

        assertThat(verdict.outcome()).isEqualTo("NEEDS_MORE_INFO");
        assertThat(verdict.outcome()).isNotIn("GREEN", "OUT_OF_SCOPE", "YELLOW");
        assertThat(verdict.completionReason()).isEqualTo("V2_UNAVAILABLE_NO_GLOBAL_SIGNAL");
    }

    @Test
    @DisplayName("An empty payload yields NEEDS_MORE_INFO, not GREEN")
    void emptyPayloadNeverYieldsGreen() {
        assertThat(fallback.screen(Map.of()).outcome()).isEqualTo("NEEDS_MORE_INFO");
        assertThat(fallback.screen(null).outcome()).isEqualTo("NEEDS_MORE_INFO");
    }

    @Test
    @DisplayName("Explicitly denied signals do not fire")
    void explicitAbsenceDoesNotFire() {
        var verdict = fallback.screen(Map.of("SEIZURE", "ABSENT", "CYANOSIS", "ABSENT"));
        assertThat(verdict.outcome()).isEqualTo("NEEDS_MORE_INFO");
        assertThat(verdict.matchedSignals()).isEmpty();
    }

    @Test
    @DisplayName("A historical signal is not treated as a current one")
    void historicalSignalIsNotCurrent() {
        var historical = fallback.screen(Map.of(
                "SEIZURE", Map.of("presence", "PRESENT", "current", false)));
        assertThat(historical.outcome())
                .as("a past seizure carried in from health memory is not a seizure now")
                .isEqualTo("NEEDS_MORE_INFO");

        var current = fallback.screen(Map.of(
                "SEIZURE", Map.of("presence", "PRESENT", "current", true)));
        assertThat(current.outcome()).isEqualTo("RED");
    }

    @Test
    @DisplayName("There is no caller flag that can suppress the screen")
    void noCallerFlagCanSuppressTheScreen() {
        // The method takes signals only — there is deliberately no relevance or
        // datasetComplete parameter, so a caller cannot talk this screen out of firing.
        assertThat(IndependentGlobalSafetyFallback.class.getDeclaredMethods())
                .filteredOn(method -> method.getName().equals("screen"))
                .allSatisfy(method -> assertThat(method.getParameterCount()).isEqualTo(1));

        var verdict = fallback.screen(Map.of("SEVERE_BREATHING_DIFFICULTY", "PRESENT"));
        assertThat(verdict.outcome()).isEqualTo("RED");
    }

    @Test
    @DisplayName("The fallback declares no collaborator fields at all")
    void fallbackHasNoDependencies() {
        assertThat(IndependentGlobalSafetyFallback.class.getDeclaredFields())
                .as("any injected collaborator would make this screen fail with the thing it backs up")
                .allSatisfy(field -> assertThat(java.lang.reflect.Modifier.isStatic(field.getModifiers()))
                        .isTrue());
    }

    @Test
    @DisplayName("Only RED and NEEDS_MORE_INFO are reachable outcomes")
    void onlyTwoOutcomesAreReachable() {
        List<String> outcomes = List.of(
                fallback.screen(Map.of("SEIZURE", "PRESENT")).outcome(),
                fallback.screen(Map.of("SEIZURE", "ABSENT")).outcome(),
                fallback.screen(Map.of()).outcome());
        assertThat(outcomes).containsOnly("RED", "NEEDS_MORE_INFO");
    }
}
