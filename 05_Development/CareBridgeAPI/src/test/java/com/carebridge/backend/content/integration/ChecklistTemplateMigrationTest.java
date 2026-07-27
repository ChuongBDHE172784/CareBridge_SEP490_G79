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
     * The historical V1__init_schema.sql was consolidated into the single canonical
     * convergence migration, so the byte-pin protects the release artifact: once
     * applied, any edit would break Flyway checksum validation.
     */
    @Test
    void uc82_69_int_005_appliedMigrationRemainsByteIdentical() throws Exception {
        byte[] bytes = Files.readAllBytes(Path.of(
                "src/main/resources/db/migration/V20260727010000__canonical_schema_convergence.sql"));
        String sha = HexFormat.of().withUpperCase().formatHex(
                MessageDigest.getInstance("SHA-256").digest(bytes));
        assertThat(sha).isEqualTo("A49474D4980612B02480530A703DEB34C9810ECCCAC611E21F9F5347BFB39506");
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
