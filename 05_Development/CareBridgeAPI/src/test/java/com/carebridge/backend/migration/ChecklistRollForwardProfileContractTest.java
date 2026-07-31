package com.carebridge.backend.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ChecklistRollForwardProfileContractTest {

    private static final Path PROFILE = Path.of(
            "src/main/resources/application-supabase-roll-forward.yaml");

    @Test
    void profileUsesOnlyTheCompatibilityLocationAndStrictFlywayOrdering() throws Exception {
        String yaml = Files.readString(PROFILE);

        assertThat(yaml).contains(
                "on-profile: supabase-roll-forward",
                "locations: classpath:db/migration-roll-forward",
                "clean-disabled: true",
                "out-of-order: false",
                "validate-on-migrate: true",
                "ignore-migration-patterns: \"*:missing\"");
        assertThat(yaml).doesNotContain("classpath:db/migration,");
    }
}
