package com.carebridge.backend.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers(disabledWithoutDocker = true)
class Wave1AccountAuthCleanupMigrationIntegrationTest {
    private static final MigrationVersion PRE_WAVE = MigrationVersion.fromVersion("20260722231000");
    private static final MigrationVersion WAVE = MigrationVersion.fromVersion("20260722231100");
    private static final String[] REMOVED = {"user_profiles","baby_profiles","refresh_tokens","user_sessions",
            "token_blacklist","notification_preferences","privacy_settings","otp_verifications",
            "password_reset_tokens","roles","user_roles"};
    private static final String[] TARGET = {"persons","care_subjects","users","user_identities","auth_sessions",
            "auth_revocations","auth_challenges","account_deletion_requests","mother_journeys","mother_journey_events",
            "maternal_observations","maternal_exercise_sessions","care_logs","growth_measurements","development_milestones",
            "vaccination_records","vaccination_schedules","community_profiles","community_topics","community_content",
            "community_interactions","professional_profiles","specialties","professional_specialties","expert_credentials",
            "expert_availability","expert_location_shares","expert_contribution_events","triage_sessions","triage_session_evidence",
            "red_flag_rules","health_context_memories","knowledge_sources","knowledge_source_reviews","health_records",
            "attachments","health_record_attachments","device_connections","health_observations","care_groups",
            "care_group_members","scheduled_care_items","family_tasks","preparation_checklist_items","care_item_templates",
            "content_items","content_item_topics","content_item_sources","moderation_cases","moderation_events",
            "notification_records","device_tokens","safety_configs","safety_monitoring_sessions","safety_events",
            "safety_event_actions","emergency_contacts","administrative_areas","care_facilities","nearby_support_requests",
            "nearby_support_responses","audit_events","security_events","data_permissions","system_configurations",
            "expense_entries","archived_consultation_records","archived_realtime_records","archived_partner_records",
            "flyway_schema_history"};

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    @BeforeEach
    void reset() throws Exception { execute("DROP SCHEMA public CASCADE; CREATE SCHEMA public"); }

    @Test
    void cleanBootstrapDropsOnlyWave1LegacyTablesAndKeepsCanonicalIntegrity() throws Exception {
        migrate(PRE_WAVE);
        migrate(WAVE);

        for (String table : REMOVED) assertThat(exists(table)).as(table).isFalse();
        for (String table : TARGET) assertThat(exists(table)).as(table).isTrue();
        assertThat(number("SELECT count(*) FROM information_schema.tables WHERE table_schema='public' AND table_type='BASE TABLE'"))
                .isEqualTo(133);
        assertThat(number("SELECT count(*) FROM information_schema.tables WHERE table_schema='public' " +
                "AND table_type='BASE TABLE' AND table_name <> ALL(ARRAY['" + String.join("','", TARGET) + "'])"))
                .as("transitional tables outside the approved target")
                .isEqualTo(63);
        assertThat(number("SELECT count(*) FROM users u LEFT JOIN persons p ON p.person_id=u.person_id WHERE p.person_id IS NULL"))
                .isZero();
        assertThat(number("SELECT count(*) FROM care_subjects c LEFT JOIN persons p ON p.person_id=c.person_id WHERE p.person_id IS NULL"))
                .isZero();
        assertThat(number("SELECT count(*) FROM pg_constraint c WHERE c.contype='f' AND c.confrelid=ANY(ARRAY[" +
                "to_regclass('public.user_profiles'),to_regclass('public.baby_profiles'),to_regclass('public.refresh_tokens')," +
                "to_regclass('public.user_sessions'),to_regclass('public.token_blacklist')]::oid[])"))
                .isZero();
    }

    @Test
    void upgradeReconcilesLiveRowsFromEveryWave1Source() throws Exception {
        migrate(PRE_WAVE);
        execute("""
            INSERT INTO persons(person_id,display_name) VALUES ('10000000-0000-0000-0000-000000000001','Before');
            INSERT INTO users(user_id,person_id,email,full_name,password_hash,phone,role,enabled,locked,created_at,updated_at)
            VALUES ('10000000-0000-0000-0000-000000000001','10000000-0000-0000-0000-000000000001',
                    'wave1@example.test','Wave One','hash','+84123456789','MOTHER',true,false,now(),now());
            INSERT INTO user_profiles(profile_id,user_id,display_name,area,created_at,updated_at)
            VALUES ('11000000-0000-0000-0000-000000000001','10000000-0000-0000-0000-000000000001','After','HCM',now(),now());
            INSERT INTO baby_profiles(baby_id,owner_user_id,nickname,status,is_active,created_at,updated_at)
            VALUES ('12000000-0000-0000-0000-000000000001','10000000-0000-0000-0000-000000000001','Baby','ACTIVE',true,now(),now());
            INSERT INTO notification_preferences(preference_id,user_id,notification_type,push_enabled,email_enabled,in_app_enabled,created_at,updated_at)
            VALUES ('13000000-0000-0000-0000-000000000001','10000000-0000-0000-0000-000000000001','REMINDER',false,true,true,now(),now());
            INSERT INTO privacy_settings(id,user_id,profile_visibility,location_sharing_enabled,analytics_consent,data_export_opt_out)
            VALUES ('14000000-0000-0000-0000-000000000001','10000000-0000-0000-0000-000000000001','PRIVATE',true,false,true);
            INSERT INTO user_sessions(session_id,user_id,refresh_token_hash,device_name,browser,last_activity_at,expires_at,status,created_at,updated_at,is_current,revoked)
            VALUES ('15000000-0000-0000-0000-000000000001','10000000-0000-0000-0000-000000000001',repeat('a',64),'phone','app',now(),now()+interval '1 day','active',now(),now(),true,false);
            INSERT INTO refresh_tokens(id,user_id,token,token_hash,expires_at,revoked,created_at)
            VALUES (9001,'10000000-0000-0000-0000-000000000001','raw-refresh',repeat('b',64),now()+interval '1 day',false,now());
            INSERT INTO token_blacklist(id,token_hash,expires_at,revoked_at,reason)
            VALUES ('16000000-0000-0000-0000-000000000001',repeat('c',64),now()+interval '1 day',now(),'logout');
            INSERT INTO otp_verifications(id,user_id,code_hash,phone,purpose,expires_at,verified,attempts,created_at)
            VALUES (9002,'10000000-0000-0000-0000-000000000001',repeat('d',64),'+84123456789','LOGIN',now()+interval '5 min',false,1,now());
            INSERT INTO password_reset_tokens(id,user_id,token_hash,expires_at,attempt_count,created_at)
            VALUES ('17000000-0000-0000-0000-000000000001','10000000-0000-0000-0000-000000000001',repeat('e',64),now()+interval '5 min',0,now());
            """);

        migrate(WAVE);

        assertThat(number("SELECT count(*) FROM persons WHERE person_id='10000000-0000-0000-0000-000000000001' AND display_name='After' AND area='HCM'" )).isOne();
        assertThat(number("SELECT count(*) FROM care_subjects WHERE care_subject_id='12000000-0000-0000-0000-000000000001' AND subject_type='BABY'" )).isOne();
        assertThat(number("SELECT count(*) FROM auth_sessions WHERE user_id='10000000-0000-0000-0000-000000000001'" )).isEqualTo(2);
        assertThat(number("SELECT count(*) FROM auth_revocations WHERE token_hash=repeat('c',64)" )).isOne();
        assertThat(number("SELECT count(*) FROM auth_challenges WHERE user_id='10000000-0000-0000-0000-000000000001'" )).isEqualTo(2);
        assertThat(number("SELECT count(*) FROM users WHERE user_id='10000000-0000-0000-0000-000000000001' " +
                "AND settings_jsonb#>>'{privacy,profileVisibility}'='PRIVATE' " +
                "AND settings_jsonb#>>'{notifications,REMINDER,pushEnabled}'='false'" )).isOne();
    }

    private void migrate(MigrationVersion target) {
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration").target(target).load().migrate();
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
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(),POSTGRES.getUsername(),POSTGRES.getPassword());
    }
}
