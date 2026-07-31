package com.carebridge.backend.health;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class MetricDefinitionMigrationContractTest {

    private static final String DEFINITIONS_MIGRATION =
            "/db/migration-legacy/V20260731040000__add_health_metric_definitions.sql";
    private static final String OBSERVATIONS_MIGRATION =
            "/db/migration-legacy/V20260731050000__extend_health_observations_for_p0_metrics.sql";

    @Test
    void migrationCreatesExactlyOneAdditiveDefinitionsTable() throws IOException {
        String migration = readMigration();

        assertThat(countMatches(migration, "(?i)\\bCREATE\\s+TABLE\\b")).isEqualTo(1);
        assertThat(migration)
                .contains("CREATE TABLE public.health_metric_definitions")
                .contains("health_metric_definitions_active_code_uk")
                .contains("UNIQUE (metric_code, version)")
                .contains("'POINT', 'PAIRED_POINT', 'SESSION', 'INTERVAL_AGGREGATE'")
                .doesNotContain("DROP TABLE")
                .doesNotContain("DELETE FROM")
                .doesNotContain("ALTER TABLE public.health_observations");
    }

    @Test
    void migrationSeedsCanonicalCatalogWithoutUnrestrictedOther() throws IOException {
        String migration = readMigration();

        assertThat(migration)
                .contains("'WEIGHT'")
                .contains("'BLOOD_PRESSURE'")
                .contains("'BLOOD_GLUCOSE'")
                .contains("'FETAL_MOVEMENT_SESSION'")
                .contains("'MATERNAL_HEART_RATE'")
                .contains("'SLEEP_SESSION'")
                .contains("'STEPS'")
                .contains("'SPO2'")
                .contains("'TEMPERATURE'")
                .doesNotContain("'OTHER'");
    }

    @Test
    void observationMigrationAddsCanonicalP0ColumnsWithoutDestructiveChanges() throws IOException {
        String migration = readMigration(OBSERVATIONS_MIGRATION);

        assertThat(migration)
                .contains("ALTER TABLE public.health_observations")
                .contains("ADD COLUMN IF NOT EXISTS period_start timestamptz")
                .contains("ADD COLUMN IF NOT EXISTS period_end timestamptz")
                .contains("ADD COLUMN IF NOT EXISTS context_jsonb jsonb")
                .contains("ADD COLUMN IF NOT EXISTS original_unit varchar(30)")
                .contains("ADD COLUMN IF NOT EXISTS definition_version integer")
                .contains("ADD COLUMN IF NOT EXISTS observation_shape varchar(30)")
                .contains("care_subject_id, observation_type, observed_at")
                .contains("legacy_source = 'maternal_health_observations'")
                .doesNotContain("DROP TABLE")
                .doesNotContain("DROP COLUMN")
                .doesNotContain("DELETE FROM");
    }

    private String readMigration() throws IOException {
        return readMigration(DEFINITIONS_MIGRATION);
    }

    private String readMigration(String resource) throws IOException {
        try (var stream = getClass().getResourceAsStream(resource)) {
            assertThat(stream).isNotNull();
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private long countMatches(String value, String regularExpression) {
        return Pattern.compile(regularExpression).matcher(value).results().count();
    }
}
