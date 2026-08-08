package com.carebridge.backend.triage;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.testsupport.EmbeddedPostgresRoleFixture;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.sql.Connection;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

/**
 * The maternal keywords reach the pre-screen table, on a real Docker-free PostgreSQL 18.
 *
 * <p>A migration that applies cleanly is not the same as a migration that has an effect: these
 * rows are what {@code TriageRedFlagPreScreenPolicy} reads, and the screen is the layer that
 * still works when the Python engine is degraded. Postpartum haemorrhage and self-harm reached
 * it unrecognised before V20260808100000, so the presence and the RED/ESCALATE classification
 * of each row is asserted rather than assumed.
 */
@EnabledOnOs(OS.WINDOWS)
class MaternalRedFlagKeywordSeedEmbeddedPostgresTest {

    /** Added by V20260808100000; every one must be live and escalating. */
    private static final List<String> ADDED_KEYWORDS = List.of(
            "băng huyết",
            "ướt đẫm băng",
            "thấm ướt băng",
            "máu ồ ạt",
            "mất máu nhiều",
            "không muốn sống",
            "không thiết sống",
            "kết liễu");

    /**
     * Deliberately absent — their accent-free form is an everyday word, and this layer
     * escalates rather than annotates. See the migration's header for the reasoning; if one
     * of these is ever added, that decision should be made knowingly, not by drift.
     */
    private static final List<String> EXCLUDED_KEYWORDS = List.of(
            "tự tử", "tự sát", "ra máu nhiều");

    @Test
    @Timeout(180)
    void maternalKeywordsAreSeededActiveAndEscalating() throws Exception {
        try (EmbeddedPostgres postgres = EmbeddedPostgres.builder()
                .setPGStartupWait(Duration.ofSeconds(30))
                .start()) {
            EmbeddedPostgresRoleFixture.provision(postgres.getPostgresDatabase());
            Flyway.configure()
                    .dataSource(postgres.getPostgresDatabase())
                    .locations("classpath:db/migration")
                    .load()
                    .migrate();

            List<String> missing = new ArrayList<>();
            List<String> misclassified = new ArrayList<>();
            try (Connection connection = postgres.getPostgresDatabase().getConnection();
                 var statement = connection.prepareStatement(
                         "select severity, action, is_active from public.red_flag_rules"
                                 + " where keyword = ?")) {
                for (String keyword : ADDED_KEYWORDS) {
                    statement.setString(1, keyword);
                    try (var row = statement.executeQuery()) {
                        if (!row.next()) {
                            missing.add(keyword);
                            continue;
                        }
                        if (!"RED".equals(row.getString(1))
                                || !"ESCALATE".equals(row.getString(2))
                                || !row.getBoolean(3)) {
                            misclassified.add(keyword);
                        }
                    }
                }
                for (String keyword : EXCLUDED_KEYWORDS) {
                    statement.setString(1, keyword);
                    try (var row = statement.executeQuery()) {
                        assertThat(row.next())
                                .as("%s must stay out of the pre-screen table", keyword)
                                .isFalse();
                    }
                }
            }

            assertThat(missing).as("keywords absent from red_flag_rules").isEmpty();
            assertThat(misclassified).as("keywords not RED/ESCALATE/active").isEmpty();
        }
    }
}
