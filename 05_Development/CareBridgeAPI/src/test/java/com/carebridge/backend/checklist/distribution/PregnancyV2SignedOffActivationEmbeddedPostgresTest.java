package com.carebridge.backend.checklist.distribution;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.testsupport.EmbeddedPostgresRoleFixture;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.sql.Connection;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

/** Current-chain evidence that technical review, not provenance sign-off, gates activation. */
@EnabledOnOs(OS.WINDOWS)
class PregnancyV2SignedOffActivationEmbeddedPostgresTest {

    private static final String APPROVER = "10000000-0000-0000-0000-000000000001";

    @Test
    @Timeout(180)
    void pendingProvenanceMetadataIsAcceptedForOneDisposableRootWhileOtherRootsRemainPending() throws Exception {
        try (EmbeddedPostgres postgres = EmbeddedPostgres.builder()
                .setPGStartupWait(Duration.ofSeconds(30))
                .setServerConfig("max_connections", "100")
                .start()) {
            var dataSource = postgres.getPostgresDatabase();
            EmbeddedPostgresRoleFixture.provision(dataSource);
            Flyway flyway = Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:db/migration")
                    .cleanDisabled(false)
                    .load();
            assertThat(flyway.migrate().success).isTrue();

            try (Connection connection = dataSource.getConnection()) {
                String rootId;
                try (var statement = connection.createStatement();
                     var roots = statement.executeQuery("""
                             select template_id
                              from public.care_item_templates
                             where entry_type = 'TEMPLATE_ROOT'
                               and stage = 'PREGNANCY'
                               and checklist_contract_version = 2
                               and checklist_metadata_jsonb ->> 'importBatchId'
                                   = 'PREGNANCY_WHO_20260812_V1'
                              order by template_id
                              limit 1
                             """)) {
                    assertThat(roots.next()).isTrue();
                    rootId = roots.getString(1);
                }

                assertThat(count(connection, """
                        select count(*)
                          from public.care_item_templates
                         where stage='PREGNANCY'
                           and checklist_contract_version=2
                           and entry_type='TEMPLATE_ROOT'
                           and checklist_metadata_jsonb ->> 'importBatchId'
                               = 'PREGNANCY_WHO_20260812_V1'
                           and content_status='DRAFT'
                           and distribution_enabled=false
                           and checklist_metadata_jsonb ->> 'provenanceStatus'
                               = 'PENDING_CLINICAL_COPY_SIGN_OFF'
                        """)).isEqualTo(16);

                executeActivationFixture(connection, rootId);
                assertThat(count(connection, """
                        select count(*)
                          from public.care_item_templates
                         where template_id='%s'::uuid
                           and content_status='APPROVED'
                           and distribution_enabled=true
                        """.formatted(rootId))).isOne();

                assertThat(count(connection, """
                        select count(*)
                          from public.care_item_templates
                         where stage='PREGNANCY'
                           and checklist_contract_version=2
                           and entry_type='TEMPLATE_ROOT'
                           and checklist_metadata_jsonb ->> 'importBatchId'
                               = 'PREGNANCY_WHO_20260812_V1'
                           and content_status='DRAFT'
                           and distribution_enabled=false
                           and checklist_metadata_jsonb ->> 'provenanceStatus'
                               = 'PENDING_CLINICAL_COPY_SIGN_OFF'
                        """)).isEqualTo(15);

                setPendingProvenanceState(connection, rootId);
                assertThat(count(connection, """
                        select count(*)
                          from public.care_item_templates
                         where template_id='%s'::uuid
                           and content_status='APPROVED'
                           and distribution_enabled=true
                        """.formatted(rootId))).isOne();
            }
        }
    }

    private static void setPendingProvenanceState(Connection connection, String rootId) throws Exception {
        try (var statement = connection.createStatement()) {
            statement.executeUpdate("""
                    update public.care_item_templates
                       set checklist_metadata_jsonb = jsonb_set(
                               checklist_metadata_jsonb,
                               '{provenanceStatus}',
                               to_jsonb('PENDING_CLINICAL_COPY_SIGN_OFF'::text),
                               true),
                           content_status='APPROVED',
                           distribution_enabled=true
                     where template_id='%s'::uuid
                    """.formatted(rootId));
        }
    }

    private static void executeActivationFixture(Connection connection, String rootId) throws Exception {
        try (var stream = PregnancyV2SignedOffActivationEmbeddedPostgresTest.class
                .getResourceAsStream("/checklist/pregnancy-v2-signed-off-fixture.sql")) {
            assertThat(stream).isNotNull();
            String sql = new String(stream.readAllBytes(), StandardCharsets.UTF_8)
                    .replace("__ROOT_ID__", rootId)
                    .replace("__APPROVER__", APPROVER);
            ScriptUtils.executeSqlScript(connection,
                    new ByteArrayResource(sql.getBytes(StandardCharsets.UTF_8)));
        }
    }

    private static long count(Connection connection, String sql) throws Exception {
        try (var statement = connection.createStatement(); var result = statement.executeQuery(sql)) {
            assertThat(result.next()).isTrue();
            return result.getLong(1);
        }
    }
}
