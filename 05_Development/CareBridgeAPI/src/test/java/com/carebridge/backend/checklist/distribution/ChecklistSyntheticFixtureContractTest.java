package com.carebridge.backend.checklist.distribution;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class ChecklistSyntheticFixtureContractTest {

    @Test
    void fixtureDeclaresDeterministicProductionRepresentativeVolumesAndPhases() throws Exception {
        try (InputStream resource = getClass().getResourceAsStream(
                "/checklist/chk041-production-representative-fixture.sql")) {
            assertThat(resource).as("CHK-041 fixture resource").isNotNull();
            String sql = new String(resource.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(sql)
                    .contains("-- CHK041:PRE_EXPAND")
                    .contains("-- CHK041:POST_EXPAND")
                    .contains("-- CHK041:CHALLENGE")
                    .contains("generate_series(1, 10002)")
                    .contains("generate_series(1, 500)")
                    .contains("generate_series(1, 20)")
                    .contains("'10000000-0000-0000-0000-000000000006'::uuid")
                    .contains("CASE WHEN n <= 250 THEN 'MOTHER' ELSE 'BABY' END")
                    .doesNotContain("'PENDING', 'USER_CREATED', 'MOTHER'")
                    .doesNotContain("gen_random_uuid()")
                    .doesNotContain("random()");
        }
    }
}
