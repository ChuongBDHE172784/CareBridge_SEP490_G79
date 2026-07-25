package com.carebridge.backend.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
class Wave9AuditConsentArchiveCleanupMigrationIntegrationTest {

    private static final MigrationVersion PRE = MigrationVersion.fromVersion("20260722231800");
    private static final MigrationVersion WAVE = MigrationVersion.fromVersion("20260722231900");
    private static final String[] REMOVED = {
        "audit_logs", "consent_grants", "consultation_bookings", "consultation_price_bands",
        "consultation_sessions", "conversation_calls", "direct_conversations", "direct_messages",
        "expenses", "expert_consultation_prices", "partner_organizations"
    };

    @Container
    final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Test
    void cleanBootstrapRemovesExactlyTheElevenWave9Tables() throws Exception {
        migrate(PRE);
        long tableCountBefore = tableCount();
        migrate(WAVE);
        assertThat(tableCount()).isEqualTo(tableCountBefore - REMOVED.length);
        for (String table : REMOVED) assertThat(exists(table)).as(table).isFalse();
        assertThat(number("SELECT count(*) FROM pg_constraint WHERE contype='f' AND NOT convalidated"))
                .isZero();
    }

    @Test
    void populatedUpgradeReconcilesEveryWave9SourceAndKeepsRuntimeFieldsQueryable() throws Exception {
        migrate(PRE);
        execute("""
            INSERT INTO persons(person_id,display_name) VALUES
              ('91000000-0000-0000-0000-000000000001','Wave 9 Mother'),
              ('91000000-0000-0000-0000-000000000002','Wave 9 Expert'),
              ('91000000-0000-0000-0000-000000000003','Wave 9 Partner');
            INSERT INTO users(user_id,person_id,email,role,enabled,locked,created_at,updated_at) VALUES
              ('91000000-0000-0000-0000-000000000001','91000000-0000-0000-0000-000000000001','mother.wave9@test','MOTHER',true,false,now(),now()),
              ('91000000-0000-0000-0000-000000000002','91000000-0000-0000-0000-000000000002','expert.wave9@test','EXPERT',true,false,now(),now()),
              ('91000000-0000-0000-0000-000000000003','91000000-0000-0000-0000-000000000003','partner.wave9@test','PARTNER',true,false,now(),now());
            INSERT INTO professional_profiles
              (professional_profile_id,user_id,verification_status,trust_status,created_at,updated_at)
            VALUES ('91100000-0000-0000-0000-000000000001','91000000-0000-0000-0000-000000000002',
              'APPROVED','ACTIVE',now(),now());
            INSERT INTO expert_availability
              (availability_id,expert_profile_id,professional_profile_id,start_at,end_at,channel_type,status,created_at,updated_at)
            VALUES ('91200000-0000-0000-0000-000000000001','91100000-0000-0000-0000-000000000001',
              '91100000-0000-0000-0000-000000000001',
              now()+interval '1 day',now()+interval '1 day 30 minutes','VIDEO','AVAILABLE',now(),now());

            INSERT INTO audit_logs
              (audit_log_id,action,actor_user_id,created_at,entity_id,entity_type,ip_address,new_value_json)
            VALUES ('91300000-0000-0000-0000-000000000001','LOGIN',
              '91000000-0000-0000-0000-000000000001',now(),
              '91000000-0000-0000-0000-000000000001','USER','127.0.0.1','{"wave":9}');
            INSERT INTO consent_grants
              (id,consent_given_at,created_at,data_type,expiry_at,purpose,recipient,revoked_at,
               scope_text,updated_at,user_id,version,policy_version,evidence_key,locale)
            VALUES (91001,now(),now(),'LOCATION',now()+interval '30 days','VIEW','FAMILY',NULL,
              'emergency-location',now(),'91000000-0000-0000-0000-000000000001',1,
              'wave9-policy','91300000-0000-0000-0000-000000000002','vi');
            INSERT INTO expenses
              (expense_id,owner_user_id,category,amount,currency,expense_date,note,created_at,updated_at)
            VALUES ('91400000-0000-0000-0000-000000000001','91000000-0000-0000-0000-000000000001',
              'OTHER',125000,'VND',current_date,'wave9 expense',now(),now());

            INSERT INTO consultation_price_bands
              (price_band_id,configured_by,channel_type,duration_minutes,specialty_scope,
               minimum_price,maximum_price,commission_rate,currency,effective_from,status,created_at,updated_at)
            VALUES ('91500000-0000-0000-0000-000000000001','91000000-0000-0000-0000-000000000003',
              'VIDEO',30,'OBSTETRICS',100000,500000,0.10,'VND',now(),'ACTIVE',now(),now());
            INSERT INTO expert_consultation_prices
              (expert_price_id,expert_profile_id,price_band_id,channel_type,duration_minutes,
               price_amount,currency,effective_from,status,version_no,created_at,updated_at)
            VALUES ('91600000-0000-0000-0000-000000000001','91100000-0000-0000-0000-000000000001',
              '91500000-0000-0000-0000-000000000001','VIDEO',30,250000,'VND',now(),'ACTIVE',1,now(),now());
            INSERT INTO consultation_bookings
              (booking_id,requester_user_id,expert_profile_id,availability_id,expert_price_id,
               topic,channel_type,duration_minutes,scheduled_start,scheduled_end,
               price_snapshot_amount,commission_rate_snapshot,currency,status,created_at,updated_at)
            VALUES ('91700000-0000-0000-0000-000000000001','91000000-0000-0000-0000-000000000001',
              '91100000-0000-0000-0000-000000000001','91200000-0000-0000-0000-000000000001',
              '91600000-0000-0000-0000-000000000001','wave9 consultation','VIDEO',30,
              now()+interval '1 day',now()+interval '1 day 30 minutes',250000,0.10,'VND','CONFIRMED',now(),now());
            INSERT INTO consultation_sessions
              (session_id,booking_id,communication_room_id,started_at,ended_at,session_status,
               expert_summary,technical_log_json,created_at,updated_at)
            VALUES ('91800000-0000-0000-0000-000000000001','91700000-0000-0000-0000-000000000001',
              'wave9-room',now()-interval '30 minutes',now(),'COMPLETED','wave9 summary','{"ok":true}',now(),now());

            INSERT INTO direct_conversations
              (conversation_id,mother_user_id,expert_user_id,status,created_at,last_activity_at)
            VALUES ('91900000-0000-0000-0000-000000000001','91000000-0000-0000-0000-000000000001',
              '91000000-0000-0000-0000-000000000002','ACTIVE',now()-interval '2 minutes',now());
            INSERT INTO direct_messages
              (message_id,conversation_id,sender_user_id,client_message_id,message_type,message_body,created_at)
            VALUES ('91a00000-0000-0000-0000-000000000001','91900000-0000-0000-0000-000000000001',
              '91000000-0000-0000-0000-000000000001','91a00000-0000-0000-0000-000000000002',
              'TEXT','wave9 message',now());
            INSERT INTO conversation_calls
              (call_id,conversation_id,initiated_by_user_id,call_type,call_status,zego_room_id,
               initiated_at,answered_at,ended_at,duration_seconds,created_at)
            VALUES ('91b00000-0000-0000-0000-000000000001','91900000-0000-0000-0000-000000000001',
              '91000000-0000-0000-0000-000000000001','VIDEO','ENDED','wave9-zego',
              now()-interval '2 minutes',now()-interval '90 seconds',now(),90,now()-interval '2 minutes');

            INSERT INTO partner_organizations
              (partner_id,name,type,address,city,phone,email,status,representative_user_id,created_at,updated_at)
            VALUES ('91c00000-0000-0000-0000-000000000001','Wave 9 Partner','HOSPITAL','Address 9',
              'Can Tho','0900000009','partner.wave9@org.test','APPROVED',
              '91000000-0000-0000-0000-000000000003',now(),now());
            UPDATE care_facilities SET partner_id='91c00000-0000-0000-0000-000000000001'
             WHERE facility_id=(SELECT facility_id FROM care_facilities ORDER BY facility_id LIMIT 1);
            """);

        migrate(WAVE);

        assertThat(number("SELECT count(*) FROM audit_events WHERE audit_event_id="
                + "'91300000-0000-0000-0000-000000000001' AND event_origin='AUDIT_LOG' "
                + "AND ip_address='127.0.0.1'"))
                .isOne();
        assertThat(number("SELECT count(*) FROM data_permissions WHERE legacy_consent_id=91001 "
                + "AND permission_kind='CONSENT_GRANT' AND scope_type='LOCATION' "
                + "AND expires_at IS NOT NULL AND revoked_at IS NULL"))
                .isOne();
        assertThat(number("SELECT count(*) FROM expense_entries WHERE expense_entry_id="
                + "'91400000-0000-0000-0000-000000000001' AND amount=125000"))
                .isOne();
        assertThat(number("SELECT count(*) FROM archived_consultation_records WHERE archive_id IN ("
                + "'91500000-0000-0000-0000-000000000001','91600000-0000-0000-0000-000000000001',"
                + "'91700000-0000-0000-0000-000000000001','91800000-0000-0000-0000-000000000001')"))
                .isEqualTo(4);
        assertThat(number("SELECT count(*) FROM archived_consultation_records WHERE archive_id="
                + "'91700000-0000-0000-0000-000000000001' AND requester_user_id="
                + "'91000000-0000-0000-0000-000000000001' AND status='CONFIRMED'"))
                .isOne();
        assertThat(number("SELECT count(*) FROM archived_realtime_records WHERE archive_id IN ("
                + "'91900000-0000-0000-0000-000000000001','91a00000-0000-0000-0000-000000000001',"
                + "'91b00000-0000-0000-0000-000000000001')"))
                .isEqualTo(3);
        assertThat(number("SELECT count(*) FROM archived_realtime_records WHERE archive_id="
                + "'91a00000-0000-0000-0000-000000000001' AND message_body='wave9 message'"))
                .isOne();
        assertThat(number("SELECT count(*) FROM archived_partner_records WHERE archive_id="
                + "'91c00000-0000-0000-0000-000000000001' AND organization_status='APPROVED'"))
                .isOne();
        assertThat(number("SELECT count(*) FROM care_facilities WHERE partner_id="
                + "'91c00000-0000-0000-0000-000000000001'"))
                .isOne();
        assertThatThrownBy(() -> execute("UPDATE audit_events SET purpose='mutated' WHERE audit_event_id="
                + "'91300000-0000-0000-0000-000000000001'"))
                .hasMessageContaining("IMMUTABLE_TABLE");
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
