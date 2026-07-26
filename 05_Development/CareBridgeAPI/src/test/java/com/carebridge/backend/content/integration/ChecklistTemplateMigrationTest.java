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

    @Test
    void uc82_69_int_005_v1RemainsByteIdentical() throws Exception {
        byte[] bytes = Files.readAllBytes(Path.of("src/main/resources/db/migration/V1__init_schema.sql"));
        String sha = HexFormat.of().withUpperCase().formatHex(
                MessageDigest.getInstance("SHA-256").digest(bytes));
        assertThat(sha).isEqualTo("A1B20BB1B4ED6037E853C627D8A21E4369B4CBB96B412BF068AA0E4FAFE5D021");
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
