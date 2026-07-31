package com.carebridge.backend.checklist.distribution;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.testsupport.EmbeddedPostgresRoleFixture;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.sql.Connection;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

@EnabledOnOs(OS.WINDOWS)
class ChecklistSchemaSimplificationLiveUpgradeEmbeddedPostgresTest {

    private static final String PRE_EXPANSION_VERSION = "20260731010000";

    @Test
    @Timeout(180)
    void legacyRootsForEveryStageUpgradeWithoutLosingReviewGate() throws Exception {
        try (EmbeddedPostgres postgres = EmbeddedPostgres.builder()
                .setPGStartupWait(Duration.ofSeconds(30))
                .setServerConfig("max_connections", "100")
                .start()) {
            EmbeddedPostgresRoleFixture.provision(postgres.getPostgresDatabase());

            Flyway preExpansion = flyway(postgres, PRE_EXPANSION_VERSION);
            assertThat(preExpansion.migrate().success).isTrue();
            seedLegacyRoots(postgres);

            Flyway upgraded = flyway(postgres, null);
            assertThat(upgraded.migrate().success).isTrue();
            assertThat(upgraded.validateWithResult().validationSuccessful).isTrue();

            try (Connection connection = postgres.getPostgresDatabase().getConnection();
                    var statement = connection.createStatement();
                    var result = statement.executeQuery("""
                            select stage, recipient_scope, eligibility_anchor_type,
                                   eligibility_range_unit, eligibility_start_inclusive,
                                   eligibility_end_inclusive, migration_review_required
                              from care_item_templates
                             where entry_type='TEMPLATE_ROOT'
                             order by stage
                            """)) {
                int count = 0;
                while (result.next()) {
                    count++;
                    assertThat(result.getString("recipient_scope")).isEqualTo("MOTHER");
                    assertThat(result.getString("eligibility_anchor_type")).isEqualTo("NONE");
                    assertThat(result.getString("eligibility_range_unit")).isEqualTo("DAY");
                    assertThat(result.getInt("eligibility_start_inclusive")).isZero();
                    assertThat(result.getInt("eligibility_end_inclusive")).isEqualTo(Integer.MAX_VALUE);
                    assertThat(result.getBoolean("migration_review_required")).isTrue();
                }
                assertThat(count).isEqualTo(4);
            }
        }
    }

    private void seedLegacyRoots(EmbeddedPostgres postgres) throws Exception {
        List<String> stages = List.of("PRE_PREGNANCY", "PREGNANCY", "POSTPARTUM", "BABY_CARE");
        try (Connection connection = postgres.getPostgresDatabase().getConnection()) {
            connection.setAutoCommit(false);
            for (String stage : stages) {
                UUID templateId = UUID.randomUUID();
                UUID versionId = UUID.randomUUID();
                try (var root = connection.prepareStatement("""
                        insert into care_item_templates
                            (template_id, entry_type, title, stage, is_active, version,
                             template_status, content_status, template_lineage_id,
                             template_version_id, substage_id, migration_review_required,
                             distribution_enabled, template_type, created_at, updated_at)
                        values (?, 'TEMPLATE_ROOT', ?, ?, true, 1,
                                'ACTIVE', 'PENDING_REVIEW', ?, ?,
                                (select substage_id from checklist_substages where code=?),
                                true, false, 'MANDATORY', now(), now())
                        """)) {
                    root.setObject(1, templateId);
                    root.setString(2, "Legacy " + stage);
                    root.setString(3, stage);
                    root.setObject(4, templateId);
                    root.setObject(5, versionId);
                    root.setString(6, "LEGACY_" + stage);
                    assertThat(root.executeUpdate()).isOne();
                }
                try (var role = connection.prepareStatement("""
                        insert into checklist_template_recipient_roles
                            (template_version_id, recipient_role, created_at)
                        values (?, 'MOTHER', now())
                        """)) {
                    role.setObject(1, versionId);
                    assertThat(role.executeUpdate()).isOne();
                }
            }
            connection.commit();
        }
    }

    private Flyway flyway(EmbeddedPostgres postgres, String target) {
        var configuration = Flyway.configure()
                .dataSource(postgres.getPostgresDatabase())
                .locations("classpath:db/migration-legacy")
                .cleanDisabled(true)
                .outOfOrder(false)
                .validateOnMigrate(true)
                .ignoreMigrationPatterns("*:future");
        if (target != null) {
            configuration.target(target);
        }
        return configuration.load();
    }
}
