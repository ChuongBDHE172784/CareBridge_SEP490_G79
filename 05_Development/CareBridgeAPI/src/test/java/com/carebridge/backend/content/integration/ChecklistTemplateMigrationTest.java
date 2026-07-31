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

    /** The canonical post-20260719180000 migration is pinned for release traceability. */
    @Test
    void uc82_69_int_005_appliedMigrationRemainsByteIdentical() throws Exception {
        byte[] bytes = Files.readAllBytes(Path.of(
                "src/main/resources/db/migration/"
                        + "V20260731060000__canonical_post_20260719180000_schema.sql"));
        String sha = HexFormat.of().withUpperCase().formatHex(
                MessageDigest.getInstance("SHA-256").digest(bytes));
        assertThat(sha).isEqualTo("3F118248E946E3461DAFB64640F033364C4F8882CC21E087C53A5AB64F23BD3E");
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
