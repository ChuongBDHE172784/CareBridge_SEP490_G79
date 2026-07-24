package com.carebridge.backend.testsupport;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

@EnabledIfSystemProperty(named = "gate0.enabled", matches = "true")
class FlywayMigrationChainTest {

    @Test
    void repositoryMigrationChainHasNoGateFailures() throws Exception {
        var manifest = DatabaseGate0Support.inspectRepository();
        Path manifestPath = DatabaseGate0Support.writeManifest("repository-manifest.json", manifest);

        assertThat(manifestPath).exists();
        assertThat(manifest.gateFailures())
                .as("Gate 0 repository failures: %s", manifest.gateFailures())
                .isEmpty();
    }
}
