package com.carebridge.backend.testsupport;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

@EnabledIfSystemProperty(named = "gate0.enabled", matches = "true")
class DatabaseGate0ManifestTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Test
    void externalDatabaseProducesPassingReadOnlyManifest() throws Exception {
        var config = DatabaseGate0Support.externalConfigFromEnvironment();
        var manifest = DatabaseGate0Support.auditExternal(config);
        String endpointLabel = manifest.endpointSha256() == null
                ? "unconfigured"
                : manifest.endpointSha256().substring(0, 12);
        Path manifestPath = DatabaseGate0Support.writeManifest(
                "external-" + manifest.environment() + "-" + endpointLabel + ".json",
                manifest);
        String json = Files.readString(manifestPath);
        JsonNode manifestTree = JSON.readTree(json);

        assertNotPresentWhenConfigured(manifestTree, config.url());
        assertNotPresentWhenConfigured(manifestTree, config.username());
        assertNotPresentWhenConfigured(manifestTree, config.password());
        assertThat(manifest.gateFailures())
                .as("Gate 0 external audit failures: %s", manifest.gateFailures())
                .isEmpty();
        assertThat(manifest.transactionReadOnly()).isTrue();
        assertThat(manifest.rollbackConfirmed()).isTrue();
    }

    private static void assertNotPresentWhenConfigured(JsonNode node, String secret) {
        if (secret != null && !secret.isBlank() && containsForbiddenText(node, secret)) {
            throw new AssertionError("Gate 0 manifest contains forbidden credential material");
        }
    }

    private static boolean containsForbiddenText(JsonNode node, String secret) {
        if (node.isTextual()) {
            return node.textValue().contains(secret);
        }
        if (node.isContainerNode()) {
            for (JsonNode child : node) {
                if (containsForbiddenText(child, secret)) {
                    return true;
                }
            }
        }
        return false;
    }
}
