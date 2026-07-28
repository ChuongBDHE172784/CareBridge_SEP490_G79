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
     * V1 is applied and immutable; forward changes belong in later migrations.
     */
    @Test
    void uc82_69_int_005_appliedMigrationRemainsByteIdentical() throws Exception {
        byte[] bytes = Files.readAllBytes(Path.of(
                "src/main/resources/db/migration/V1__init_schema.sql"));
        String sha = HexFormat.of().withUpperCase().formatHex(
                MessageDigest.getInstance("SHA-256").digest(bytes));
        assertThat(sha).isEqualTo("42613A129101EB8B1E1D655AA6D25DAA7DD06C14D46DF1F08BD5353F10B9121A");
    }

    @Test
    void appliedReferenceDataMigrationRemainsByteIdentical() throws Exception {
        byte[] bytes = Files.readAllBytes(Path.of(
                "src/main/resources/db/migration/V2__seed_reference_data.sql"));
        String sha = HexFormat.of().withUpperCase().formatHex(
                MessageDigest.getInstance("SHA-256").digest(bytes));
        assertThat(sha).isEqualTo("8148557B41330236E903561150A7AB1D832BFDBC6517EF2D09BA42CA18A5E0B9");
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
