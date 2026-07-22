package com.carebridge.backend.content;

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
class Wave7ContentModerationCleanupMigrationIntegrationTest {

    private static final MigrationVersion PRE = MigrationVersion.fromVersion("20260722231600");
    private static final MigrationVersion WAVE = MigrationVersion.fromVersion("20260722231700");
    private static final String[] REMOVED = {
        "content_reports", "moderation_actions", "content_sources", "security_event_notes"
    };

    @Container
    final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Test
    void cleanBootstrapRemovesFourWave7Tables() throws Exception {
        migrate(PRE);
        assertThat(tableCount()).isEqualTo(96);
        migrate(WAVE);
        assertThat(tableCount()).isEqualTo(92);
        for (String table : REMOVED) assertThat(exists(table)).as(table).isFalse();
        assertThat(number("SELECT count(*) FROM pg_constraint WHERE contype='f' AND NOT convalidated"))
                .isZero();
    }

    @Test
    void populatedUpgradePreservesReportLifecycleStandaloneActionsSourcesAndSecurityNotes() throws Exception {
        migrate(PRE);
        execute("""
            INSERT INTO persons(person_id,display_name) VALUES
              ('71000000-0000-0000-0000-000000000001','Wave 7 Reporter'),
              ('71000000-0000-0000-0000-000000000002','Wave 7 Moderator');
            INSERT INTO users(user_id,person_id,email,role,enabled,locked,created_at,updated_at) VALUES
              ('71000000-0000-0000-0000-000000000001','71000000-0000-0000-0000-000000000001','reporter.wave7@test','MOTHER',true,false,now(),now()),
              ('71000000-0000-0000-0000-000000000002','71000000-0000-0000-0000-000000000002','moderator.wave7@test','MODERATOR',true,false,now(),now());
            INSERT INTO content_items
              (content_item_id,content_type,title,body,status,version_no,author_user_id,created_at,updated_at)
            VALUES ('71100000-0000-0000-0000-000000000001','ARTICLE','Wave 7 content','Body',
              'PUBLISHED',1,'71000000-0000-0000-0000-000000000001',now(),now());
            INSERT INTO content_sources(content_item_id,source_title,source_url,source_publisher)
            VALUES ('71100000-0000-0000-0000-000000000001','MOH guidance',
              'https://moh.gov.vn/wave7','MOH');
            INSERT INTO content_reports
              (report_id,target_id,target_type,status,category,report_source,description,
               reporter_user_id,assigned_moderator_id,created_at,resolved_at,updated_at,reverted_at,reverted_by)
            VALUES ('71200000-0000-0000-0000-000000000001','71100000-0000-0000-0000-000000000001',
              'CONTENT','RESOLVED','UNSAFE_ADVICE','USER','unsafe fixture',
              '71000000-0000-0000-0000-000000000001','71000000-0000-0000-0000-000000000002',
              now()-interval '1 hour',now(),now(),now(),'71000000-0000-0000-0000-000000000002');
            INSERT INTO moderation_actions
              (moderation_action_id,report_id,target_id,target_type,action_type,moderator_user_id,
               reason,action_at,expires_at)
            VALUES
              ('71300000-0000-0000-0000-000000000001','71200000-0000-0000-0000-000000000001',
               '71100000-0000-0000-0000-000000000001','CONTENT','HIDE',
               '71000000-0000-0000-0000-000000000002','resolved report',now(),NULL),
              ('71300000-0000-0000-0000-000000000002',NULL,
               '71100000-0000-0000-0000-000000000001','CONTENT','UNDO',
               '71000000-0000-0000-0000-000000000002','standalone history',now(),NULL);
            INSERT INTO security_events
              (id,occurred_at,event_type,user_id,details,severity,status)
            VALUES (7101,now(),'SUSPICIOUS_ACTIVITY','71000000-0000-0000-0000-000000000001',
              'fixture','HIGH','UNDER_REVIEW');
            INSERT INTO security_event_notes(note_id,event_id,author_id,note_text,created_at)
            VALUES ('71400000-0000-0000-0000-000000000001',7101,
              '71000000-0000-0000-0000-000000000002','investigation note',now());
            """);

        migrate(WAVE);

        assertThat(number("SELECT count(*) FROM moderation_cases WHERE moderation_case_id="
                + "'71200000-0000-0000-0000-000000000001' AND report_source='USER' "
                + "AND status='RESOLVED' AND reverted_at IS NOT NULL AND reverted_by IS NOT NULL"))
                .isOne();
        assertThat(number("SELECT count(*) FROM moderation_events WHERE moderation_event_id IN "
                + "('71300000-0000-0000-0000-000000000001','71300000-0000-0000-0000-000000000002')"))
                .isEqualTo(2);
        assertThat(number("SELECT count(*) FROM moderation_events WHERE moderation_event_id="
                + "'71300000-0000-0000-0000-000000000002' AND moderation_case_id IS NULL "
                + "AND action_type='UNDO'"))
                .isOne();
        assertThat(number("SELECT count(*) FROM content_item_sources WHERE content_item_id="
                + "'71100000-0000-0000-0000-000000000001' AND source_url='https://moh.gov.vn/wave7'"))
                .isOne();
        assertThat(number("SELECT count(*) FROM audit_events WHERE audit_event_id="
                + "'71400000-0000-0000-0000-000000000001' AND event_category="
                + "'SECURITY_INVESTIGATION_NOTE' AND security_event_id=7101 "
                + "AND note_text='investigation note'"))
                .isOne();
        assertThat(number("SELECT count(*) FROM audit_events a LEFT JOIN security_events e "
                + "ON e.id=a.security_event_id WHERE a.event_category='SECURITY_INVESTIGATION_NOTE' "
                + "AND e.id IS NULL"))
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
