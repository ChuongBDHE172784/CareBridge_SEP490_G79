package com.carebridge.backend.safety;

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
class Wave8SafetyFacilityCleanupMigrationIntegrationTest {

    private static final MigrationVersion PRE = MigrationVersion.fromVersion("20260722231700");
    private static final MigrationVersion WAVE = MigrationVersion.fromVersion("20260722231800");
    private static final String[] REMOVED = {
        "care_facility_legacy_ids", "emergency_alert_attempts", "emergency_alert_deliveries",
        "emergency_map_handoffs", "emergency_sessions", "family_alert_log",
        "imu_monitoring_sessions", "imu_safety_events", "location_snapshots",
        "safety_event_responses", "safety_monitoring_config"
    };

    @Container
    final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Test
    void cleanBootstrapRemovesElevenWave8Tables() throws Exception {
        migrate(PRE);
        assertThat(tableCount()).isEqualTo(92);
        migrate(WAVE);
        assertThat(tableCount()).isEqualTo(81);
        for (String table : REMOVED) assertThat(exists(table)).as(table).isFalse();
        assertThat(number("SELECT count(*) FROM pg_constraint WHERE contype='f' AND NOT convalidated"))
                .isZero();
    }

    @Test
    void populatedUpgradePreservesQueryableSafetyDeliveryLocationAndHandoffState() throws Exception {
        migrate(PRE);
        execute("""
            INSERT INTO persons(person_id,display_name) VALUES
              ('81000000-0000-0000-0000-000000000001','Wave 8 Owner'),
              ('81000000-0000-0000-0000-000000000002','Wave 8 Family');
            INSERT INTO users(user_id,person_id,email,role,enabled,locked,created_at,updated_at) VALUES
              ('81000000-0000-0000-0000-000000000001','81000000-0000-0000-0000-000000000001','owner.wave8@test','MOTHER',true,false,now(),now()),
              ('81000000-0000-0000-0000-000000000002','81000000-0000-0000-0000-000000000002','family.wave8@test','FAMILY',true,false,now(),now());
            INSERT INTO safety_monitoring_config
              (id,user_id,fall_detection_enabled,sensitivity_level,emergency_auto_alert,
               countdown_seconds,sensor_permission_granted,sensor_permission_recorded_at,updated_at,updated_by)
            VALUES ('81100000-0000-0000-0000-000000000001','81000000-0000-0000-0000-000000000001',
              true,'HIGH',true,30,true,now(),now(),'81000000-0000-0000-0000-000000000001');
            INSERT INTO imu_monitoring_sessions
              (id,user_id,status,sensitivity_level,started_at,created_by)
            VALUES ('81200000-0000-0000-0000-000000000001','81000000-0000-0000-0000-000000000001',
              'ACTIVE','HIGH',now()-interval '5 minutes','81000000-0000-0000-0000-000000000001');
            INSERT INTO emergency_sessions
              (id,user_id,status,trigger_source,user_latitude,user_longitude,created_at,created_by)
            VALUES ('81300000-0000-0000-0000-000000000001','81000000-0000-0000-0000-000000000001',
              'ACTIVE','FALL_DETECTION',10.1234567,106.1234567,now()-interval '2 minutes',
              '81000000-0000-0000-0000-000000000001');
            INSERT INTO imu_safety_events
              (id,user_id,imu_session_id,event_type,magnitude,user_latitude,user_longitude,
               detected_at,status,signal_key,response_type,responded_at,emergency_session_id,created_by)
            VALUES ('81400000-0000-0000-0000-000000000001','81000000-0000-0000-0000-000000000001',
              '81200000-0000-0000-0000-000000000001','SUSPECTED_FALL',9.8100,10.1234567,
              106.1234567,now()-interval '3 minutes','ESCALATED','wave8-signal','NEED_HELP',now(),
              '81300000-0000-0000-0000-000000000001','SYSTEM');
            INSERT INTO safety_event_responses
              (id,safety_event_id,owner_user_id,response_type,reason,responded_at,created_by,actor_type)
            VALUES ('81500000-0000-0000-0000-000000000001','81400000-0000-0000-0000-000000000001',
              '81000000-0000-0000-0000-000000000001','NEED_HELP','fixture response',now(),
              '81000000-0000-0000-0000-000000000001','OWNER');
            INSERT INTO notification_records
              (id,user_id,type,title,body,status,attempt_count,channel,is_read,created_at,updated_at)
            VALUES ('81600000-0000-0000-0000-000000000001','81000000-0000-0000-0000-000000000002',
              'EMERGENCY','Wave 8','Emergency fixture','SENT',1,'PUSH',false,now(),now());
            INSERT INTO device_tokens(id,user_id,token,platform,active,created_at,updated_at)
            VALUES ('81700000-0000-0000-0000-000000000001','81000000-0000-0000-0000-000000000002',
              'wave8-device-token','ANDROID',true,now(),now());
            INSERT INTO emergency_alert_deliveries
              (id,emergency_session_id,recipient_user_id,device_token_id,notification_record_id,
               delivery_status,attempt_count,fcm_message_id,created_at,delivered_at)
            VALUES ('81800000-0000-0000-0000-000000000001','81300000-0000-0000-0000-000000000001',
              '81000000-0000-0000-0000-000000000002','81700000-0000-0000-0000-000000000001',
              '81600000-0000-0000-0000-000000000001','DELIVERED',2,'wave8-fcm',now(),now());
            INSERT INTO emergency_alert_attempts
              (emergency_session_id,status,started_at,completed_at,lease_expires_at,attempt_number,
               successful_recipient_count,failed_recipient_count,updated_at)
            VALUES ('81300000-0000-0000-0000-000000000001','SENT',now()-interval '1 minute',now(),
              now()+interval '1 minute',2,1,0,now());
            INSERT INTO family_alert_log(id,session_id,sent_at,recipient_count,location_included,created_by)
            VALUES ('81900000-0000-0000-0000-000000000001','81300000-0000-0000-0000-000000000001',
              now(),1,true,'SYSTEM');
            INSERT INTO emergency_map_handoffs
              (handoff_id,user_id,risk_level,user_latitude,user_longitude,summary,status,created_at,updated_at)
            VALUES ('81a00000-0000-0000-0000-000000000001','81000000-0000-0000-0000-000000000001',
              'RED',10.12345678,106.12345678,'wave8 handoff','OPEN',now(),now());
            INSERT INTO location_snapshots
              (location_snapshot_id,user_id,context_type,context_id,latitude,longitude,
               accuracy_meters,captured_at,expires_at,consent_status)
            VALUES ('81b00000-0000-0000-0000-000000000001','81000000-0000-0000-0000-000000000001',
              'EMERGENCY','81300000-0000-0000-0000-000000000001',10.12345678,106.12345678,
              4.25,now(),now()+interval '1 hour','GRANTED');
            """);

        migrate(WAVE);

        assertThat(number("SELECT count(*) FROM safety_configs WHERE safety_config_id="
                + "'81100000-0000-0000-0000-000000000001' AND sensor_permission_granted"))
                .isOne();
        assertThat(number("SELECT count(*) FROM safety_events WHERE safety_event_id="
                + "'81400000-0000-0000-0000-000000000001' AND record_type='IMU_EVENT' "
                + "AND magnitude=9.8100 AND signal_key='wave8-signal'"))
                .isOne();
        assertThat(number("SELECT count(*) FROM safety_events WHERE safety_event_id="
                + "'81300000-0000-0000-0000-000000000001' AND record_type='EMERGENCY_SESSION' "
                + "AND event_type='FALL_DETECTION'"))
                .isOne();
        assertThat(number("SELECT count(*) FROM safety_event_actions WHERE "
                + "safety_event_action_id='81800000-0000-0000-0000-000000000001' "
                + "AND action_type='DELIVERY' AND device_token_id="
                + "'81700000-0000-0000-0000-000000000001' AND attempt_number=2"))
                .isOne();
        assertThat(number("SELECT count(*) FROM safety_event_actions WHERE "
                + "safety_event_action_id='81b00000-0000-0000-0000-000000000001' "
                + "AND action_type='LOCATION_SNAPSHOT' AND latitude=10.12345678 "
                + "AND consent_status='GRANTED'"))
                .isOne();
        assertThat(number("SELECT count(*) FROM safety_event_actions WHERE "
                + "safety_event_action_id='81a00000-0000-0000-0000-000000000001' "
                + "AND action_type='MAP_HANDOFF' AND action_status='OPEN'"))
                .isOne();
        assertThat(number("SELECT count(*) FROM safety_event_actions a LEFT JOIN safety_events e "
                + "ON e.safety_event_id=a.safety_event_id WHERE a.safety_event_id IS NOT NULL "
                + "AND e.safety_event_id IS NULL"))
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
