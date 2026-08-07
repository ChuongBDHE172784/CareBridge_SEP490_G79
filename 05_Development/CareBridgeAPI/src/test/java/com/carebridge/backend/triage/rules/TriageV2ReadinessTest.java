package com.carebridge.backend.triage.rules;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * A broken triage rule set must degrade the triage module, not the application.
 *
 * <p>Before this split, {@code TriageRuleRegistry} was a {@code @Component} whose constructor
 * threw, so an invalid artifact failed Spring context creation and took every unrelated
 * module with it. These tests pin the new boundary: loading failure becomes a readiness
 * state, the evaluator is absent (so nothing runs on a partial rule set), and the
 * independent fallback still answers.
 */
class TriageV2ReadinessTest {

    private static final String MISSING = "triage/does-not-exist.json";
    private static final String MANIFEST = TriageRuleRegistry.MANIFEST_RESOURCE;

    @Test
    @DisplayName("A valid registry reports READY and exposes an evaluator")
    void validRegistryIsReady() {
        TriageV2ReadinessService service = new TriageV2ReadinessService();

        assertThat(service.readiness()).isEqualTo(TriageV2Readiness.READY);
        assertThat(service.isReady()).isTrue();
        assertThat(service.evaluator()).isPresent();
        assertThat(service.registry()).isPresent();
        assertThat(service.technicalStatus()).isEqualTo(TriageV2Readiness.TechnicalStatus.READY);
    }

    @Test
    @DisplayName("An invalid registry does not throw — construction still succeeds")
    void invalidRegistryDoesNotBringDownConstruction() {
        assertThatCode(() -> new TriageV2ReadinessService(MISSING, MANIFEST))
                .as("registry failure must not propagate as a bean-creation failure")
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("An invalid registry reports FALLBACK_ONLY and exposes no evaluator")
    void invalidRegistryIsFallbackOnlyWithNoEvaluator() {
        TriageV2ReadinessService service = new TriageV2ReadinessService(MISSING, MANIFEST);

        assertThat(service.isReady()).isFalse();
        assertThat(service.readiness()).isEqualTo(TriageV2Readiness.REGISTRY_INVALID);
        assertThat(service.technicalStatus())
                .isEqualTo(TriageV2Readiness.TechnicalStatus.FALLBACK_ONLY);
        assertThat(service.evaluator())
                .as("nothing may run on a partial or absent rule set")
                .isEmpty();
    }

    @Test
    @DisplayName("With the registry down, a global danger signal is still RED")
    void globalDangerStillRedWhileRegistryIsDown() {
        TriageV2ReadinessService service = new TriageV2ReadinessService(MISSING, MANIFEST);
        assertThat(service.evaluator()).isEmpty();

        var verdict = new IndependentGlobalSafetyFallback()
                .screen(Map.of("SEVERE_BREATHING_DIFFICULTY", "PRESENT"));

        assertThat(verdict.outcome()).isEqualTo("RED");
        assertThat(verdict.stopConversation()).isTrue();
    }

    @Test
    @DisplayName("With the registry down and benign input, the answer is never GREEN")
    void registryDownWithBenignInputIsNeverGreen() {
        var verdict = new IndependentGlobalSafetyFallback().screen(Map.of("SEIZURE", "ABSENT"));
        assertThat(verdict.outcome()).isEqualTo("NEEDS_MORE_INFO");
    }

    @Test
    @DisplayName("Technical, release and validation status are three independent axes")
    void statusAxesAreIndependent() {
        Map<String, Object> report = new TriageV2ReadinessService().statusReport();

        assertThat(report.get("technicalStatus")).isEqualTo("READY");
        assertThat(report.get("publicReleaseStatus"))
                .as("a green build must never imply the module may face real users")
                .isEqualTo("BLOCKED");
        assertThat(report.get("clinicalValidationStatus")).isEqualTo("NOT_CLINICALLY_VALIDATED");
        assertThat(report.get("greenRuntimeStatus")).isEqualTo("DISABLED");
        assertThat(report.get("greenEligibilityStatus")).isEqualTo("BLOCKED_BY_SOURCE_COVERAGE");
        assertThat(report.get("reasons").toString())
                .contains("SOURCE_VERIFICATION_PENDING")
                .contains("NOT_CLINICALLY_VALIDATED")
                .contains("GREEN_RELEASE_GATE_DISABLED");
    }

    @Test
    @DisplayName("Registry and evaluator are not Spring components")
    void engineTypesAreNotSpringBeans() {
        assertThat(TriageRuleRegistry.class
                .isAnnotationPresent(org.springframework.stereotype.Component.class))
                .as("a throwing constructor must not be a bean")
                .isFalse();
        assertThat(TriageRuleEvaluator.class
                .isAnnotationPresent(org.springframework.stereotype.Component.class))
                .isFalse();
    }
}
