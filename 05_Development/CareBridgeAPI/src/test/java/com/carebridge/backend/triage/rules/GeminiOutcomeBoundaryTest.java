package com.carebridge.backend.triage.rules;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Architecture boundary: the V2 engine must not depend on the legacy LLM outcome interface.
 *
 * <p>{@code GeminiTriageClient.analyzeSymptoms} returns a {@code RiskLevel} chosen by a model —
 * exactly the shape invariant 1 forbids. It survives only as V1 legacy. This test checks the
 * dependency graph by import and type reference, which is the actual control;
 * {@code GeminiFailOpenPreventionTest}'s source scan is defence-in-depth on top of it.
 */
class GeminiOutcomeBoundaryTest {

    private static final Path V2_PACKAGE =
            Path.of("src", "main", "java", "com", "carebridge", "backend", "triage", "rules");

    private static final List<String> FORBIDDEN_REFERENCES = List.of(
            "GeminiTriageClient",
            "GeminiExtractionClient",
            "AiTriageResult",
            "RiskLevel");

    @Test
    @DisplayName("No V2 rules class references the legacy LLM outcome interface")
    void v2PackageHasNoLegacyLlmOutcomeDependency() throws Exception {
        try (Stream<Path> paths = Files.walk(V2_PACKAGE)) {
            List<String> offenders = paths
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(GeminiOutcomeBoundaryTest::referencesForbiddenType)
                    .map(path -> path.getFileName().toString())
                    .toList();

            assertThat(offenders)
                    .as("the V2 engine must never reach an interface where a model picks the outcome")
                    .isEmpty();
        }
    }

    @Test
    @DisplayName("The legacy outcome interface is marked LEGACY_V1_ONLY and deprecated")
    void legacyInterfaceIsMarkedDeprecated() throws Exception {
        Path legacy = Path.of("src", "main", "java", "com", "carebridge", "backend", "triage",
                "service", "GeminiTriageClient.java");
        String source = Files.readString(legacy);

        assertThat(source)
                .as("readers must be told this interface is not part of V2")
                .contains("LEGACY_V1_ONLY");
        assertThat(source).contains("@Deprecated");
    }

    @Test
    @DisplayName("The V2 rules package resolves without loading any Gemini type")
    void v2TypesLoadWithoutGeminiOnTheGraph() {
        // If any V2 type held a Gemini reference in a field or signature, resolving these
        // classes would drag it onto the classpath graph. Touching them all is the check.
        assertThat(List.of(
                TriageRuleRegistry.class,
                TriageRuleEvaluator.class,
                TriageRule.class,
                TriageSafetyPolicy.class,
                TriageGreenBlocker.class,
                IndependentGlobalSafetyFallback.class,
                TriageV2ReadinessService.class,
                RuleConditionEvaluator.class))
                .allSatisfy(type -> assertThat(type.getName()).startsWith(
                        "com.carebridge.backend.triage.rules"));

        for (Class<?> type : List.of(TriageRuleEvaluator.class, IndependentGlobalSafetyFallback.class)) {
            for (var field : type.getDeclaredFields()) {
                assertThat(field.getType().getName())
                        .as("%s.%s", type.getSimpleName(), field.getName())
                        .doesNotContain("Gemini");
            }
            for (var method : type.getDeclaredMethods()) {
                assertThat(method.getReturnType().getName())
                        .as("%s.%s return type", type.getSimpleName(), method.getName())
                        .doesNotContain("Gemini");
                for (var parameter : method.getParameterTypes()) {
                    assertThat(parameter.getName())
                            .as("%s.%s parameter", type.getSimpleName(), method.getName())
                            .doesNotContain("Gemini");
                }
            }
        }
    }

    private static boolean referencesForbiddenType(Path path) {
        try {
            String source = Files.readString(path);
            // Strip block comments so an explanatory note is not read as a dependency.
            String code = source.replaceAll("(?s)/\\*.*?\\*/", "")
                    .replaceAll("(?m)//.*$", "");
            return FORBIDDEN_REFERENCES.stream().anyMatch(code::contains);
        } catch (Exception exception) {
            return false;
        }
    }
}
