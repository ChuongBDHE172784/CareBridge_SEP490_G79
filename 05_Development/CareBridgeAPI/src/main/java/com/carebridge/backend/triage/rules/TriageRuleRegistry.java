package com.carebridge.backend.triage.rules;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Loads the canonical triage rule registry (v2.2.0) from the classpath copy — fail-closed.
 *
 * <p>Fail-closed is the whole point. An earlier draft dropped an invalid rule with a warning
 * and carried on; that is unsafe, because a missing emergency rule silently becomes "no rule
 * matched", which is one step from GREEN. Now any ACTIVE rule that fails validation, any
 * missing critical rule, any duplicate id and any digest mismatch makes V2 UNAVAILABLE. Only
 * DRAFT/DISABLED/RETIRED rules may be skipped quietly.
 *
 * <p>Kept in exact parity with {@code app/rules/registry.py}.
 */
/*
 * Deliberately NOT a @Component. Its constructor throws on any integrity problem, which is
 * correct for the engine but wrong as a Spring bean: context creation would fail and a bad
 * triage artifact would take every unrelated module down with it. TriageV2ReadinessService
 * constructs it and converts failure into a readiness state.
 */
public class TriageRuleRegistry {

    private static final Logger log = LoggerFactory.getLogger(TriageRuleRegistry.class);

    public static final String REGISTRY_RESOURCE = "triage/triage_rules_v2.json";
    public static final String MANIFEST_RESOURCE = "triage/required_rule_manifest.json";

    static final Set<String> V2_STAGES = Set.of(
            "PRECONCEPTION", "POSSIBLE_PREGNANCY", "PREGNANCY", "POSTPARTUM");
    static final Set<String> V2_OUTCOMES = Set.of(
            "RED", "YELLOW", "GREEN", "NEEDS_MORE_INFO", "OUT_OF_SCOPE");
    static final Set<String> RELEASABLE_STATUSES = Set.of("ACTIVE");
    static final Set<String> SKIPPABLE_STATUSES = Set.of("DRAFT", "RETIRED", "DISABLED");

    /**
     * Legacy artifacts (ruleset &lt;= 2.1.0) carried {@code status: "APPROVED"}. That name reads
     * as clinical approval, which never happened — see AI_TRIAGE_V2_DECISIONS.md D-011. The
     * value is still accepted so older artifacts load, but it maps to a RELEASE flag only and
     * never to any validation claim.
     */
    private static final Map<String, String> LEGACY_STATUS_TO_RELEASE_STATUS = Map.of(
            "APPROVED", "ACTIVE",
            "DRAFT", "DRAFT",
            "RETIRED", "RETIRED");

    private static final List<String> REQUIRED_RULE_KEYS = List.of(
            "ruleId", "ruleVersion", "ruleType", "stages", "outcome", "priority",
            "decisionOrder", "stopOnMatch", "conditionType", "condition", "reasonCode",
            "actionCode");

    /**
     * Reads {@code releaseStatus}, falling back to the deprecated {@code status} field.
     * Returns {@code null} when neither is present — the caller treats that as an integrity
     * failure rather than defaulting to something releasable.
     */
    private static String releaseStatusOf(JsonNode payload, String ruleId) {
        if (payload.hasNonNull("releaseStatus")) {
            return payload.get("releaseStatus").asText();
        }
        String legacy = payload.path("status").asText(null);
        if (legacy == null) {
            return null;
        }
        String mapped = LEGACY_STATUS_TO_RELEASE_STATUS.get(legacy);
        if (mapped == null) {
            return legacy;  // unrecognised: let the caller reject it
        }
        log.warn("DEPRECATED: rule {} uses legacy status={}; mapped to releaseStatus={}."
                        + " Legacy APPROVED means RELEASE-ENABLED only and never clinical"
                        + " validation (clinicalValidationStatus stays NOT_CLINICALLY_VALIDATED)."
                        + " Re-generate the artifact.",
                ruleId, legacy, mapped);
        return mapped;
    }

    private static final Map<String, Integer> OUTCOME_SEVERITY = Map.of(
            "RED", 0, "YELLOW", 1, "NEEDS_MORE_INFO", 2, "OUT_OF_SCOPE", 3, "GREEN", 4);

    /** Raised whenever the registry cannot be trusted. V2 must be reported UNAVAILABLE. */
    public static final class RegistryIntegrityException extends RuntimeException {
        public RegistryIntegrityException(String message) {
            super(message);
        }
    }

    private final String rulesetVersion;
    private final String ruleMatrixVersion;
    private final String rulesetSha256;
    private final int maximumQuestionRounds;
    private final boolean greenEnabled;
    private final List<TriageRule> rules;
    private final List<TriageSafetyPolicy> safetyPolicies;
    private final List<TriageGreenBlocker> greenBlockers;
    private final Map<String, String> signalDisplayText;
    private final List<String> skippedRuleIds;

    public TriageRuleRegistry() {
        this(REGISTRY_RESOURCE, MANIFEST_RESOURCE, LocalDate.now());
    }

    TriageRuleRegistry(String resourcePath, String manifestPath, LocalDate today) {
        byte[] payload = read(resourcePath);
        this.rulesetSha256 = verifyDigest(resourcePath, payload);
        JsonNode document = parse(resourcePath, payload);

        List<TriageRule> loaded = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        for (JsonNode rulePayload : document.path("rules")) {
            String ruleId = rulePayload.path("ruleId").asText(null);
            String status = releaseStatusOf(rulePayload, ruleId);
            if (status != null && SKIPPABLE_STATUSES.contains(status)) {
                skipped.add(ruleId);
                log.info("triage rule skipped rule_id={} status={}", ruleId, status);
                continue;
            }
            if (status == null || !RELEASABLE_STATUSES.contains(status)) {
                throw new RegistryIntegrityException(
                        "rule " + ruleId + " has unrecognised releaseStatus " + status
                                + "; refusing to load the registry");
            }
            if (!seen.add(ruleId)) {
                throw new RegistryIntegrityException("duplicate ruleId " + ruleId);
            }
            String reason = validate(rulePayload, today);
            if (reason != null) {
                // An ACTIVE rule that will not parse is a safety failure, not a warning.
                throw new RegistryIntegrityException(
                        "ACTIVE rule " + ruleId + " is invalid: " + reason);
            }
            loaded.add(toRule(rulePayload));
        }

        if (loaded.isEmpty()) {
            throw new RegistryIntegrityException("no releasable triage rules were loaded");
        }

        List<TriageSafetyPolicy> policies = new ArrayList<>();
        for (JsonNode item : document.path("safetyPolicies")) {
            TriageSafetyPolicy policy = toPolicy(item);
            LocalDate expiry = parseDate(policy.expiresAt());
            if (policy.enabled() && expiry != null && expiry.isBefore(today)) {
                log.warn("safety policy past its expiry policy_id={} expired_at={}"
                        + " — clinical review overdue", policy.policyId(), policy.expiresAt());
            }
            policies.add(policy);
        }

        List<TriageGreenBlocker> blockers = new ArrayList<>();
        for (JsonNode item : document.path("greenSafetyBlockers")) {
            if (!item.has("blockerId")) continue;
            blockers.add(new TriageGreenBlocker(
                    item.get("blockerId").asText(),
                    item.path("title").asText(item.get("blockerId").asText()),
                    textList(item.path("stages")),
                    item.get("condition"),
                    item.path("predicate").asText(null),
                    item.path("reasonCode").asText(),
                    textList(item.path("questionIds"))));
        }

        enforceRequiredManifest(manifestPath, seen, policies);

        Map<String, String> displayText = new LinkedHashMap<>();
        for (JsonNode signal : document.path("signalCatalog")) {
            if (signal.hasNonNull("code") && signal.hasNonNull("displayText")) {
                displayText.put(signal.get("code").asText(), signal.get("displayText").asText());
            }
        }

        this.rulesetVersion = document.path("rulesetVersion").asText();
        this.ruleMatrixVersion = document.path("ruleMatrixVersion").asText();
        this.maximumQuestionRounds = document.path("scope").path("maximumQuestionRounds").asInt(3);
        this.greenEnabled = document.path("releaseGates").path("greenEnabled").asBoolean(false);
        this.rules = List.copyOf(loaded);
        this.safetyPolicies = List.copyOf(policies);
        this.greenBlockers = List.copyOf(blockers);
        this.signalDisplayText = Map.copyOf(displayText);
        this.skippedRuleIds = List.copyOf(skipped);
    }

    private void enforceRequiredManifest(
            String manifestPath, Set<String> loadedRuleIds, List<TriageSafetyPolicy> policies) {
        byte[] manifestBytes;
        try {
            manifestBytes = read(manifestPath);
        } catch (RegistryIntegrityException absent) {
            log.warn("required rule manifest not found at {}; critical-rule enforcement skipped",
                    manifestPath);
            return;
        }
        JsonNode manifest = parse(manifestPath, manifestBytes);

        List<String> missing = new ArrayList<>();
        for (JsonNode ruleId : manifest.path("criticalRuleIds")) {
            if (!loadedRuleIds.contains(ruleId.asText())) {
                missing.add(ruleId.asText());
            }
        }
        if (!missing.isEmpty()) {
            throw new RegistryIntegrityException("critical rules missing from registry: " + missing);
        }

        Set<String> enabledPolicies = new LinkedHashSet<>();
        policies.stream().filter(TriageSafetyPolicy::enabled)
                .forEach(policy -> enabledPolicies.add(policy.policyId()));
        List<String> missingPolicies = new ArrayList<>();
        for (JsonNode policyId : manifest.path("criticalPolicyIds")) {
            if (!enabledPolicies.contains(policyId.asText())) {
                missingPolicies.add(policyId.asText());
            }
        }
        if (!missingPolicies.isEmpty()) {
            throw new RegistryIntegrityException(
                    "critical safety policies missing or disabled: " + missingPolicies);
        }
    }

    private static byte[] read(String resourcePath) {
        try (InputStream stream = new ClassPathResource(resourcePath).getInputStream()) {
            return stream.readAllBytes();
        } catch (IOException exception) {
            throw new RegistryIntegrityException(
                    "cannot read " + resourcePath + ": " + exception.getMessage());
        }
    }

    private static JsonNode parse(String resourcePath, byte[] payload) {
        try {
            return new ObjectMapper().readTree(payload);
        } catch (IOException exception) {
            throw new RegistryIntegrityException(
                    "cannot parse " + resourcePath + ": " + exception.getMessage());
        }
    }

    private static String verifyDigest(String resourcePath, byte[] payload) {
        String actual;
        try {
            actual = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(payload));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
        String expected = new String(read(resourcePath + ".sha256"), StandardCharsets.UTF_8).trim();
        if (!expected.equals(actual)) {
            throw new RegistryIntegrityException(
                    "triage rule registry " + resourcePath + " does not match its canonical digest;"
                            + " the runtime copy is generated — re-run"
                            + " DevTools/sync_triage_rule_registry.py instead of editing it");
        }
        return actual;
    }

    /** Returns {@code null} when the rule is loadable, else a human-readable reason. */
    private static String validate(JsonNode payload, LocalDate today) {
        for (String key : REQUIRED_RULE_KEYS) {
            if (!payload.has(key)) {
                return "missing required key " + key;
            }
        }
        if (payload.path("approvedAt").asText("").isEmpty()) {
            // Provenance timestamp for the internal review, not a clinical sign-off date.
            return "releasable rule has no approvedAt timestamp";
        }
        String ruleVersion = payload.get("ruleVersion").asText("");
        if (ruleVersion.chars().filter(character -> character == '.').count() != 2) {
            return "malformed ruleVersion " + ruleVersion;
        }
        String outcome = payload.get("outcome").asText();
        if (!V2_OUTCOMES.contains(outcome)) {
            return "outcome " + outcome + " is outside the V2 contract";
        }
        JsonNode stages = payload.get("stages");
        if (!stages.isArray() || stages.isEmpty()) {
            return "stages must be a non-empty array";
        }
        for (JsonNode stage : stages) {
            if (!V2_STAGES.contains(stage.asText())) {
                // Pediatric or any other legacy stage must never enter the reproductive graph.
                return "stage outside V2 scope: " + stage.asText();
            }
        }
        int priority = payload.get("priority").asInt(-1);
        if (priority < 0 || priority > 100) {
            return "priority out of range: " + priority;
        }
        if (!payload.get("decisionOrder").isInt()) {
            return "decisionOrder must be an integer";
        }
        for (JsonNode predicate : payload.path("exclusionPredicates")) {
            if (!RuleConditionEvaluator.EXCLUSION_PREDICATES.contains(predicate.asText())) {
                return "unknown exclusion predicate " + predicate.asText();
            }
        }
        if (!isEffective(payload, today)) {
            return "outside its effective window";
        }
        try {
            String conditionType = payload.get("conditionType").asText();
            if ("ENGINE".equals(conditionType)) {
                RuleConditionEvaluator.validateEnginePredicate(payload.get("condition"), "condition");
            } else if ("SIGNAL".equals(conditionType)) {
                RuleConditionEvaluator.validateCondition(payload.get("condition"), "condition");
            } else {
                return "unknown conditionType " + conditionType;
            }
        } catch (RuleConditionEvaluator.ConditionSchemaException exception) {
            return "invalid condition: " + exception.getMessage();
        }
        return null;
    }

    private static boolean isEffective(JsonNode payload, LocalDate today) {
        LocalDate from = parseDate(payload.path("effectiveFrom").asText(null));
        if (from == null || from.isAfter(today)) {
            return false;
        }
        LocalDate to = parseDate(payload.path("effectiveTo").asText(null));
        return to == null || !to.isBefore(today);
    }

    private static LocalDate parseDate(String value) {
        if (value == null || value.isBlank() || "null".equals(value)) {
            return null;
        }
        try {
            return LocalDate.parse(value.substring(0, Math.min(10, value.length())));
        } catch (DateTimeParseException | IndexOutOfBoundsException exception) {
            return null;
        }
    }

    private static TriageRule toRule(JsonNode payload) {
        return new TriageRule(
                payload.get("ruleId").asText(),
                payload.get("ruleVersion").asText(),
                payload.get("ruleType").asText(),
                payload.path("title").asText(payload.get("ruleId").asText()),
                textList(payload.get("stages")),
                payload.get("outcome").asText(),
                payload.get("priority").asInt(),
                payload.get("decisionOrder").asInt(),
                payload.get("stopOnMatch").asBoolean(),
                payload.get("conditionType").asText(),
                payload.get("condition"),
                textList(payload.path("requiredFields")),
                textList(payload.path("questionIds")),
                textList(payload.path("exclusionPredicates")),
                payload.get("reasonCode").asText(),
                payload.get("actionCode").asText(),
                textList(payload.path("sourceIds")),
                releaseStatusOf(payload, payload.path("ruleId").asText(null)),
                payload.path("requiresReleaseGate").asText(null));
    }

    private static TriageSafetyPolicy toPolicy(JsonNode item) {
        return new TriageSafetyPolicy(
                item.get("policyId").asText(),
                item.get("policyType").asText(),
                item.path("title").asText(item.get("policyId").asText()),
                textList(item.get("stages")),
                item.get("outcome").asText(),
                item.path("priority").asInt(100),
                item.path("decisionOrder").asInt(0),
                item.path("stopConversation").asBoolean(true),
                item.get("condition"),
                item.get("reasonCode").asText(),
                item.get("actionCode").asText(),
                item.get("status").asText(),
                item.path("enabled").asBoolean(false),
                item.path("reviewDueAt").asText(null),
                item.path("expiresAt").asText(null));
    }

    private static List<String> textList(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>(node.size());
        node.forEach(element -> values.add(element.asText()));
        return List.copyOf(values);
    }

    public String rulesetVersion() {
        return rulesetVersion;
    }

    public String ruleMatrixVersion() {
        return ruleMatrixVersion;
    }

    public String rulesetSha256() {
        return rulesetSha256;
    }

    public int maximumQuestionRounds() {
        return maximumQuestionRounds;
    }

    public boolean greenEnabled() {
        return greenEnabled;
    }

    public List<TriageRule> rules() {
        return rules;
    }

    public List<TriageSafetyPolicy> safetyPolicies() {
        return safetyPolicies;
    }

    public List<TriageGreenBlocker> greenBlockers() {
        return greenBlockers;
    }

    public List<String> skippedRuleIds() {
        return skippedRuleIds;
    }

    public Map<String, String> signalDisplayText() {
        return signalDisplayText;
    }

    public List<TriageRule> forStage(String stage) {
        return rules.stream().filter(rule -> rule.appliesToStage(stage)).toList();
    }

    public List<TriageSafetyPolicy> policiesForStage(String stage) {
        return safetyPolicies.stream()
                .filter(policy -> policy.enabled() && policy.appliesToStage(stage))
                .toList();
    }

    public List<TriageGreenBlocker> blockersForStage(String stage) {
        return greenBlockers.stream().filter(blocker -> blocker.appliesToStage(stage)).toList();
    }

    public Optional<TriageRule> byId(String ruleId) {
        return rules.stream().filter(rule -> rule.ruleId().equals(ruleId)).findFirst();
    }

    /**
     * Explicit, file-order-independent ordering: outcome severity, then priority descending,
     * then the declared decisionOrder, then ruleId. Reordering the JSON must not change a
     * clinical decision.
     */
    public static List<TriageRule> sortedByPrecedence(List<TriageRule> candidates) {
        return candidates.stream()
                .sorted(Comparator
                        .comparingInt((TriageRule rule) -> OUTCOME_SEVERITY.getOrDefault(rule.outcome(), 99))
                        .thenComparing(Comparator.comparingInt(TriageRule::priority).reversed())
                        .thenComparingInt(TriageRule::decisionOrder)
                        .thenComparing(TriageRule::ruleId))
                .toList();
    }
}
