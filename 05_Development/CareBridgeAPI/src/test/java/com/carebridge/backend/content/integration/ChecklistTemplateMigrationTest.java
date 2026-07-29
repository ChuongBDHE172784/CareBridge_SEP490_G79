package com.carebridge.backend.content.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.content.support.Story69TestFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

/** RED/preservation contracts for INT-005. */
class ChecklistTemplateMigrationTest {

    /**
     * The applied V1 schema migration is immutable: changing it would invalidate
     * Flyway checksums for existing environments. New changes belong in a later
     * append-only migration.
     */
    @Test
    void uc82_69_int_005_appliedMigrationRemainsByteIdentical() throws Exception {
        byte[] bytes = Files.readAllBytes(Path.of(
                "src/main/resources/db/migration/V1__init_schema.sql"));
        String sha = HexFormat.of().withUpperCase().formatHex(
                MessageDigest.getInstance("SHA-256").digest(bytes));
        assertThat(sha).isEqualTo("BC00A8466E9687BA48A2CB2DA114E750A7CF9DCD50208323F086239DC0141788");
    }

    @Test
    void uc82_69_int_005_templateStatusUsesDedicatedFiveValueJavaEnum() {
        Class<?> status = Story69TestFactory.loadClass(
                        "com.carebridge.backend.content.entity.ChecklistTemplateStatus")
                .orElse(null);
        assertThat(status).isNotNull();
        assertThat(status.getEnumConstants()).extracting(Object::toString)
                .containsExactly("DRAFT", "PENDING_REVIEW", "APPROVED", "REJECTED", "ARCHIVED");
    }
}
