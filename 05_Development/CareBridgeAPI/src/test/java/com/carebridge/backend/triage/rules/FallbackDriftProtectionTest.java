package com.carebridge.backend.triage.rules;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Drift guard between the registry and the independent fallback.
 *
 * <p>{@link IndependentGlobalSafetyFallback} hard-codes its danger-signal list on purpose: it
 * must not read the artifact that may itself be broken. The cost of that decision is silent
 * drift — someone adds a `globalRed` signal to the registry, forgets the fallback, and the
 * degraded path quietly stops covering it. This test is the compensating control.
 */
class FallbackDriftProtectionTest {

    @Test
    @DisplayName("Every registry globalRed signal is covered by the independent fallback")
    void fallbackCoversEveryGlobalRedSignal() throws Exception {
        Set<String> registryGlobalRed = new LinkedHashSet<>();
        try (InputStream stream =
                     new ClassPathResource(TriageRuleRegistry.REGISTRY_RESOURCE).getInputStream()) {
            JsonNode document = new ObjectMapper().readTree(stream);
            for (JsonNode signal : document.path("signalCatalog")) {
                if (signal.path("globalRed").asBoolean(false)) {
                    registryGlobalRed.add(signal.get("code").asText());
                }
            }
        }

        assertThat(registryGlobalRed)
                .as("the registry should declare at least one globalRed signal")
                .isNotEmpty();

        assertThat(IndependentGlobalSafetyFallback.GLOBAL_DANGER_SIGNALS.keySet())
                .as("a globalRed signal missing from the fallback would go unscreened whenever "
                        + "the registry is unavailable — add it to GLOBAL_DANGER_SIGNALS")
                .containsAll(registryGlobalRed);
    }

    @Test
    @DisplayName("The fallback does not screen signals the registry no longer treats as globalRed")
    void fallbackDoesNotOverreachBeyondTheRegistry() throws Exception {
        Set<String> registryGlobalRed = new LinkedHashSet<>();
        try (InputStream stream =
                     new ClassPathResource(TriageRuleRegistry.REGISTRY_RESOURCE).getInputStream()) {
            JsonNode document = new ObjectMapper().readTree(stream);
            for (JsonNode signal : document.path("signalCatalog")) {
                if (signal.path("globalRed").asBoolean(false)) {
                    registryGlobalRed.add(signal.get("code").asText());
                }
            }
        }

        // Over-coverage is the safer direction, so this is an equality check reported as a
        // review prompt rather than a hard safety failure: if they diverge, a human decides
        // whether the registry or the fallback is the one that moved.
        assertThat(IndependentGlobalSafetyFallback.GLOBAL_DANGER_SIGNALS.keySet())
                .as("fallback and registry globalRed sets have diverged — reconcile deliberately")
                .containsExactlyInAnyOrderElementsOf(registryGlobalRed);
    }
}
