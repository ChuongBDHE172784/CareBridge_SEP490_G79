package com.carebridge.backend.triage.rules;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Governance migration: legacy {@code status: "APPROVED"} → {@code releaseStatus: "ACTIVE"}.
 *
 * <p>The old field name reads as clinical approval, which never happened — no clinician
 * reviewed these rules (see AI_TRIAGE_DECISIONS.md D-011). Older artifacts must still
 * load, but the legacy value maps to a RELEASE flag only: it must never be read as
 * validation, and it must not weaken fail-closed loading.
 */
class LegacyGovernanceCompatibilityTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    @DisplayName("The canonical registry no longer ships the legacy APPROVED status")
    void canonicalRegistryUsesReleaseStatus() throws Exception {
        JsonNode document = readCanonical();
        assertThat(document.get("rulesetVersion").asText()).isEqualTo("2.2.0");
        for (JsonNode rule : document.get("rules")) {
            String ruleId = rule.get("ruleId").asText();
            assertThat(rule.hasNonNull("releaseStatus")).as(ruleId).isTrue();
            assertThat(rule.get("releaseStatus").asText()).as(ruleId).isEqualTo("ACTIVE");
            assertThat(rule.has("status"))
                    .as("%s must not carry the deprecated status field", ruleId)
                    .isFalse();
            assertThat(rule.get("clinicalValidationStatus").asText()).as(ruleId)
                    .isEqualTo("NOT_CLINICALLY_VALIDATED");
        }
    }

    @Test
    @DisplayName("A legacy artifact with status=APPROVED still loads, mapped to ACTIVE")
    void legacyApprovedArtifactStillLoads(@org.junit.jupiter.api.io.TempDir Path temp)
            throws Exception {
        int canonicalRuleCount = ruleCount(readCanonical());
        TriageRuleRegistry legacy = loadFrom(temp, toLegacy(readCanonical()));

        // Counted from the artifact rather than pinned: this test is about the legacy status
        // mapping, so growing the rule set must not read as a governance regression.
        assertThat(legacy.rules()).hasSize(canonicalRuleCount);
        assertThat(legacy.rules()).allSatisfy(rule ->
                assertThat(rule.status()).isEqualTo("ACTIVE"));
        assertThat(legacy.skippedRuleIds()).isEmpty();
    }

    @Test
    @DisplayName("Legacy APPROVED never becomes a clinical validation claim")
    void legacyApprovedIsNotClinicalValidation(@org.junit.jupiter.api.io.TempDir Path temp)
            throws Exception {
        JsonNode legacyDocument = toLegacy(readCanonical());
        loadFrom(temp, legacyDocument);

        // The mapping touches release state only; the validation field is untouched.
        for (JsonNode rule : legacyDocument.get("rules")) {
            assertThat(rule.get("clinicalValidationStatus").asText())
                    .as(rule.get("ruleId").asText())
                    .isEqualTo("NOT_CLINICALLY_VALIDATED");
        }
    }

    @Test
    @DisplayName("A legacy artifact whose releasable rule is invalid still fails closed")
    void legacyArtifactStillFailsClosedOnInvalidRule(
            @org.junit.jupiter.api.io.TempDir Path temp) throws Exception {
        ObjectNode document = (ObjectNode) toLegacy(readCanonical());
        // Break one releasable rule: backwards compatibility must not become leniency.
        ((ObjectNode) document.get("rules").get(0))
                .set("condition", MAPPER.readTree("{\"expression\":\"anything\"}"));

        assertThatThrownBy(() -> loadFrom(temp, document))
                .isInstanceOf(TriageRuleRegistry.RegistryIntegrityException.class)
                .hasMessageContaining("is invalid");
    }

    @Test
    @DisplayName("An unrecognised release status is refused, not defaulted")
    void unrecognisedReleaseStatusIsRefused(@org.junit.jupiter.api.io.TempDir Path temp)
            throws Exception {
        ObjectNode document = (ObjectNode) readCanonical();
        ((ObjectNode) document.get("rules").get(0)).put("releaseStatus", "SOMETHING_ELSE");

        assertThatThrownBy(() -> loadFrom(temp, document))
                .isInstanceOf(TriageRuleRegistry.RegistryIntegrityException.class)
                .hasMessageContaining("unrecognised releaseStatus");
    }

    @Test
    @DisplayName("A rule carrying neither field is refused rather than assumed releasable")
    void missingReleaseStatusIsRefused(@org.junit.jupiter.api.io.TempDir Path temp)
            throws Exception {
        ObjectNode document = (ObjectNode) readCanonical();
        ((ObjectNode) document.get("rules").get(0)).remove("releaseStatus");

        assertThatThrownBy(() -> loadFrom(temp, document))
                .isInstanceOf(TriageRuleRegistry.RegistryIntegrityException.class);
    }

    @Test
    @DisplayName("Legacy DRAFT and RETIRED stay skippable")
    void legacyDraftAndRetiredRemainSkippable(@org.junit.jupiter.api.io.TempDir Path temp)
            throws Exception {
        ObjectNode document = (ObjectNode) toLegacy(readCanonical());
        int canonicalRuleCount = ruleCount(document);
        ((ObjectNode) document.get("rules").get(canonicalRuleCount - 1)).put("status", "DRAFT");

        TriageRuleRegistry legacy = loadFrom(temp, document);
        assertThat(legacy.skippedRuleIds()).hasSize(1);
        assertThat(legacy.rules()).hasSize(canonicalRuleCount - 1);
    }

    // ------------------------------------------------------------------ helpers

    private static int ruleCount(JsonNode document) {
        return document.get("rules").size();
    }

    private static JsonNode readCanonical() throws Exception {
        try (InputStream stream =
                     new ClassPathResource(TriageRuleRegistry.REGISTRY_RESOURCE).getInputStream()) {
            return MAPPER.readTree(stream);
        }
    }

    /** Rewrites a v2.2.0 document back into the pre-migration shape. */
    private static JsonNode toLegacy(JsonNode document) {
        for (JsonNode rule : document.get("rules")) {
            ObjectNode node = (ObjectNode) rule;
            node.remove("releaseStatus");
            node.put("status", "APPROVED");
        }
        ((ObjectNode) document).put("rulesetVersion", "2.1.0");
        return document;
    }

    /**
     * Writes the document to a temp classpath-less location and loads it. The registry reads
     * from the classpath, so the file is staged into target/test-classes with its digest.
     */
    private static TriageRuleRegistry loadFrom(Path temp, JsonNode document) throws Exception {
        Path testClasses = Path.of("target", "test-classes", "triage");
        Files.createDirectories(testClasses);
        String name = "legacy-" + Math.abs(document.hashCode()) + ".json";
        byte[] bytes = MAPPER.writerWithDefaultPrettyPrinter()
                .writeValueAsString(document).getBytes(StandardCharsets.UTF_8);
        Files.write(testClasses.resolve(name), bytes);
        Files.writeString(testClasses.resolve(name + ".sha256"), sha256(bytes));
        return new TriageRuleRegistry("triage/" + name, TriageRuleRegistry.MANIFEST_RESOURCE,
                java.time.LocalDate.now());
    }

    private static String sha256(byte[] payload) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(payload));
    }
}
