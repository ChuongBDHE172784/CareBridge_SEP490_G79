package com.carebridge.backend.triage;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers(disabledWithoutDocker = true)
class Wave4TriageKnowledgeCleanupMigrationIntegrationTest {

    private static final MigrationVersion PRE = MigrationVersion.fromVersion("20260722231300");
    private static final MigrationVersion WAVE = MigrationVersion.fromVersion("20260722231400");
    private static final String[] REMOVED = {
        "intake_sessions", "structured_intake_data", "evidence_sources",
        "evidence_source_review_log", "health_memory_entries"
    };

    @Container
    final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Test
    void cleanBootstrapRemovesFiveWave4Tables() throws Exception {
        migrate(PRE);
        assertThat(tableCount()).isEqualTo(111);
        migrate(WAVE);
        assertThat(tableCount()).isEqualTo(106);
        for (String table : REMOVED) assertThat(exists(table)).as(table).isFalse();
        assertThat(number("SELECT count(*) FROM pg_constraint WHERE contype='f' AND NOT convalidated"))
                .isZero();
    }

    @Test
    void populatedUpgradePreservesSessionStructureEmergencyContextAndKnowledgeHistory() throws Exception {
        migrate(PRE);
        execute("""
            INSERT INTO persons(person_id,display_name)
            VALUES ('41000000-0000-0000-0000-000000000001','Wave 4 Mother');
            INSERT INTO users(user_id,person_id,email,role,enabled,locked,created_at,updated_at)
            VALUES ('41000000-0000-0000-0000-000000000001','41000000-0000-0000-0000-000000000001',
              'mother.wave4@test','MOTHER',true,false,now(),now());
            INSERT INTO intake_sessions
              (id,user_id,mother_profile_id,stage,client_request_id,symptoms,raw_ai_response,
               risk_level,status,disclaimer,created_at,completed_at,created_by)
            VALUES ('41100000-0000-0000-0000-000000000001','41000000-0000-0000-0000-000000000001',
              '41000000-0000-0000-0000-000000000001','PREGNANCY','wave4-request','bleeding',
              '{"stage":"PREGNANCY","claims":["urgent"],"citations":["https://moh.gov.vn"]}',
              'RED','COMPLETED','v4-disclaimer',now(),now(),'41000000-0000-0000-0000-000000000001');
            INSERT INTO structured_intake_data
              (id,session_id,symptom_list,duration_days,intensity,emergency_flag,extracted_at,created_by)
            VALUES ('41200000-0000-0000-0000-000000000001','41100000-0000-0000-0000-000000000001',
              '["bleeding"]',1,'HIGH',true,now(),'SYSTEM');
            INSERT INTO evidence_sources
              (id,domain,base_url,organization,category,status,discovery_mode,applicable_stages,
               added_by,reviewed_by,reviewed_at,notes,created_at,updated_at)
            VALUES ('41300000-0000-0000-0000-000000000001','wave4.moh.gov.vn',
              'https://wave4.moh.gov.vn','MOH Wave 4','GOVERNMENT','APPROVED','MANUAL_ADMIN_ADD',
              'PREGNANCY','41000000-0000-0000-0000-000000000001',
              '41000000-0000-0000-0000-000000000001',now(),'approved fixture',now(),now());
            INSERT INTO evidence_source_review_log
              (id,evidence_source_id,previous_status,new_status,actor_user_id,actor_role,notes,changed_at)
            VALUES ('41400000-0000-0000-0000-000000000001','41300000-0000-0000-0000-000000000001',
              'PENDING_REVIEW','APPROVED','41000000-0000-0000-0000-000000000001','CONTENT_ADMIN',
              'review fixture',now());
            INSERT INTO health_memory_entries
              (id,user_id,mother_profile_id,related_stage,summary_text,source_session_id,
               created_at,expires_at)
            VALUES ('41500000-0000-0000-0000-000000000001','41000000-0000-0000-0000-000000000001',
              '41000000-0000-0000-0000-000000000001','PREGNANCY','minimized summary',
              '41100000-0000-0000-0000-000000000001',now(),now()+interval '30 days');
            """);

        migrate(WAVE);

        assertThat(number("SELECT count(*) FROM triage_sessions WHERE triage_session_id="
                + "'41100000-0000-0000-0000-000000000001' AND emergency AND emergency_flag "
                + "AND stage='PREGNANCY' AND disclaimer_text='v4-disclaimer' "
                + "AND symptom_list='[\"bleeding\"]'::jsonb AND result_jsonb ? 'rawAiResponse'"))
                .isOne();
        assertThat(number("SELECT count(*) FROM knowledge_sources WHERE knowledge_source_id="
                + "'41300000-0000-0000-0000-000000000001' AND status='APPROVED'"))
                .isOne();
        assertThat(number("SELECT count(*) FROM knowledge_source_reviews WHERE review_id="
                + "'41400000-0000-0000-0000-000000000001' AND previous_status='PENDING_REVIEW'"))
                .isOne();
        assertThat(number("SELECT count(*) FROM health_context_memories WHERE memory_id="
                + "'41500000-0000-0000-0000-000000000001' AND triage_session_id="
                + "'41100000-0000-0000-0000-000000000001'"))
                .isOne();
        assertThat(number("SELECT count(*) FROM health_context_memories h LEFT JOIN triage_sessions t "
                + "ON t.triage_session_id=h.triage_session_id "
                + "WHERE h.triage_session_id IS NOT NULL AND t.triage_session_id IS NULL"))
                .isZero();
    }

    private void migrate(MigrationVersion target) {
        Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration").target(target).load().migrate();
    }

    private long tableCount() throws Exception {
        return number("SELECT count(*) FROM information_schema.tables "
                + "WHERE table_schema='public' AND table_type='BASE TABLE'");
    }

    private boolean exists(String table) throws Exception {
        try (Connection c=connection(); Statement s=c.createStatement(); ResultSet r=s.executeQuery(
                "SELECT to_regclass('public."+table+"') IS NOT NULL")) { r.next(); return r.getBoolean(1); }
    }

    private long number(String sql) throws Exception {
        try (Connection c=connection(); Statement s=c.createStatement(); ResultSet r=s.executeQuery(sql)) {
            r.next(); return r.getLong(1);
        }
    }

    private void execute(String sql) throws Exception {
        try (Connection c=connection(); Statement s=c.createStatement()) { s.execute(sql); }
    }

    private Connection connection() throws Exception {
        return DriverManager.getConnection(postgres.getJdbcUrl(),postgres.getUsername(),postgres.getPassword());
    }
}
