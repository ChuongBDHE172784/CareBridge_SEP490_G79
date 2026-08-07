package com.carebridge.backend.triage.rules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Loads the V2 rule registry without letting its failure take the application down.
 *
 * <p>Previously {@code TriageRuleRegistry} was itself a {@code @Component} whose constructor
 * threw on any integrity problem. That is the right behaviour for the triage engine — a
 * partially-loaded rule set must never serve traffic — but it was the wrong blast radius:
 * Spring context creation failed, so a bad triage artifact took every unrelated module with
 * it. Here the failure is caught and turned into a readiness state instead.
 *
 * <p>Fail-closed is preserved: when loading fails there is no evaluator at all, so nothing
 * can run on a partial registry, and callers must fall back to
 * {@link IndependentGlobalSafetyFallback}.
 */
@Component
public class TriageV2ReadinessService {

    private static final Logger log = LoggerFactory.getLogger(TriageV2ReadinessService.class);

    private final TriageRuleRegistry registry;
    private final TriageRuleEvaluator evaluator;
    private final TriageV2Readiness readiness;
    private final List<String> reasons;

    public TriageV2ReadinessService() {
        this(TriageRuleRegistry.REGISTRY_RESOURCE, TriageRuleRegistry.MANIFEST_RESOURCE);
    }

    TriageV2ReadinessService(String registryResource, String manifestResource) {
        TriageRuleRegistry loaded = null;
        TriageV2Readiness state;
        List<String> collected = new ArrayList<>();
        try {
            loaded = new TriageRuleRegistry(registryResource, manifestResource, java.time.LocalDate.now());
            state = TriageV2Readiness.READY;
        } catch (RuntimeException failure) {
            String message = String.valueOf(failure.getMessage());
            state = classify(message);
            collected.add(state.name() + ": " + message);
            log.error("Triage V2 registry unavailable readiness={} reason={}", state, message);
        }
        this.registry = loaded;
        this.evaluator = loaded == null ? null : new TriageRuleEvaluator(loaded);
        this.readiness = state;
        this.reasons = List.copyOf(collected);
    }

    private static TriageV2Readiness classify(String message) {
        String lower = message == null ? "" : message.toLowerCase(java.util.Locale.ROOT);
        if (lower.contains("critical rules missing") || lower.contains("critical safety policies")) {
            return TriageV2Readiness.CRITICAL_RULE_MISSING;
        }
        if (lower.contains("digest") || lower.contains("hash")) {
            return TriageV2Readiness.RULESET_HASH_MISMATCH;
        }
        return TriageV2Readiness.REGISTRY_INVALID;
    }

    public TriageV2Readiness readiness() {
        return readiness;
    }

    public boolean isReady() {
        return readiness == TriageV2Readiness.READY;
    }

    public Optional<TriageRuleRegistry> registry() {
        return Optional.ofNullable(registry);
    }

    /** Absent whenever the registry did not load — nothing may run on a partial rule set. */
    public Optional<TriageRuleEvaluator> evaluator() {
        return Optional.ofNullable(evaluator);
    }

    public TriageV2Readiness.TechnicalStatus technicalStatus() {
        return isReady()
                ? TriageV2Readiness.TechnicalStatus.READY
                : TriageV2Readiness.TechnicalStatus.FALLBACK_ONLY;
    }

    /**
     * Always BLOCKED today. The rule set is DEV_REVIEWED and NOT_CLINICALLY_VALIDATED, the
     * renderer wording is provisional and source verification is incomplete — none of which
     * is a technical fault, and none of which may be inferred away from a green build.
     */
    public TriageV2Readiness.PublicReleaseStatus publicReleaseStatus() {
        return TriageV2Readiness.PublicReleaseStatus.BLOCKED;
    }

    /** Composite view: technical, release and validation status are three separate axes. */
    public Map<String, Object> statusReport() {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("technicalStatus", technicalStatus().name());
        report.put("publicReleaseStatus", publicReleaseStatus().name());
        report.put("greenRuntimeStatus", TriageV2Readiness.GreenReleaseStatus.DISABLED.name());
        report.put("greenEligibilityStatus",
                TriageV2Readiness.GreenReleaseStatus.BLOCKED_BY_SOURCE_COVERAGE.name());
        report.put("clinicalValidationStatus", "NOT_CLINICALLY_VALIDATED");
        List<String> allReasons = new ArrayList<>(reasons);
        allReasons.add("SOURCE_VERIFICATION_PENDING");
        allReasons.add("PROVISIONAL_RENDERER");
        allReasons.add("NOT_CLINICALLY_VALIDATED");
        if (registry != null && !registry.greenEnabled()) {
            allReasons.add("GREEN_RELEASE_GATE_DISABLED");
        }
        report.put("reasons", List.copyOf(allReasons));
        return report;
    }
}
