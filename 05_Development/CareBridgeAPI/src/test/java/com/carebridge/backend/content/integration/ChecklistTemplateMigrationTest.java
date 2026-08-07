package com.carebridge.backend.content.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.content.support.Story69TestFactory;
import com.carebridge.backend.testsupport.MigrationLocator;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

/** RED/preservation contracts for INT-005. */
class ChecklistTemplateMigrationTest {

    /**
     * The canonical post-20260719180000 migration is pinned for release traceability: once the
     * chain is published, editing the file must break the build.
     *
     * <p>Pin refreshed 2026-08-07 after the canonical chain was re-versioned
     * (V20260731060000 → V20260731070000). The migration is resolved by description so a future
     * re-version does not silently turn this into a missing-file error.
     */
    @Test
    void uc82_69_int_005_appliedMigrationRemainsByteIdentical() throws Exception {
        byte[] bytes = Files.readAllBytes(
                MigrationLocator.byDescription("canonical_post_20260719180000_schema"));
        String sha = HexFormat.of().withUpperCase().formatHex(
                MessageDigest.getInstance("SHA-256").digest(bytes));
        assertThat(sha).isEqualTo("CA629ACE8BCA95912132C435BA3323F8369D4FD4D09200910D18603C757BA0E4");
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
