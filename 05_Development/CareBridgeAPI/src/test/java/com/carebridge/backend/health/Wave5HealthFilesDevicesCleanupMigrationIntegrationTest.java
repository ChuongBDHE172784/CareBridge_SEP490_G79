package com.carebridge.backend.health;

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
class Wave5HealthFilesDevicesCleanupMigrationIntegrationTest {

    private static final MigrationVersion PRE = MigrationVersion.fromVersion("20260722231400");
    private static final MigrationVersion WAVE = MigrationVersion.fromVersion("20260722231500");
    private static final String[] REMOVED = {
        "uploaded_files", "health_record_files", "health_device_connections",
        "device_measurements", "health_summaries"
    };

    @Container
    final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Test
    void cleanBootstrapRemovesFiveWave5Tables() throws Exception {
        migrate(PRE);
        assertThat(tableCount()).isEqualTo(106);
        migrate(WAVE);
        assertThat(tableCount()).isEqualTo(101);
        for (String table : REMOVED) assertThat(exists(table)).as(table).isFalse();
        assertThat(number("SELECT count(*) FROM pg_constraint WHERE contype='f' AND NOT convalidated"))
                .isZero();
    }

    @Test
    void populatedUpgradePreservesFileBindingDeviceChartFieldsAndSummaryPayload() throws Exception {
        migrate(PRE);
        execute("""
            INSERT INTO persons(person_id,display_name)
            VALUES ('51000000-0000-0000-0000-000000000001','Wave 5 Mother');
            INSERT INTO users(user_id,person_id,email,role,enabled,locked,created_at,updated_at)
            VALUES ('51000000-0000-0000-0000-000000000001','51000000-0000-0000-0000-000000000001',
              'mother.wave5@test','MOTHER',true,false,now(),now());
            INSERT INTO health_records
              (health_record_id,owner_user_id,record_type,title,record_date,status,created_at,updated_at)
            VALUES ('51100000-0000-0000-0000-000000000001','51000000-0000-0000-0000-000000000001',
              'LAB_RESULT','Wave 5 record',current_date,'ACTIVE',now(),now());
            INSERT INTO uploaded_files
              (file_id,owner_user_id,storage_key,original_name,mime_type,file_size_bytes,status,created_at,updated_at)
            VALUES ('51200000-0000-0000-0000-000000000001','51000000-0000-0000-0000-000000000001',
              'wave5/report.pdf','report.pdf','application/pdf',1024,'ACTIVE',now(),now());
            INSERT INTO health_record_files(id,health_record_id,file_id,display_order,created_at)
            VALUES ('51300000-0000-0000-0000-000000000001','51100000-0000-0000-0000-000000000001',
              '51200000-0000-0000-0000-000000000001',2,now());
            INSERT INTO health_device_connections
              (connection_id,user_id,provider_name,device_name,scopes_json,token_reference,
               consent_granted_at,last_synced_at,status,created_at,updated_at)
            VALUES ('51400000-0000-0000-0000-000000000001','51000000-0000-0000-0000-000000000001',
              'WAVE5','Watch','{"read":["heart_rate"]}','vault://wave5',now(),now(),'ACTIVE',now(),now());
            INSERT INTO device_measurements
              (device_measurement_id,connection_id,measurement_type,value_numeric,value_secondary,
               unit,measured_at,quality_label,raw_metadata_json,created_at,updated_at)
            VALUES ('51500000-0000-0000-0000-000000000001','51400000-0000-0000-0000-000000000001',
              'BLOOD_PRESSURE',120,80,'mmHg',now(),'GOOD','{"source":"watch"}',now(),now());
            INSERT INTO health_summaries
              (summary_id,owner_user_id,summary_period,period_start,period_end,summary_json,
               generated_by,status,created_at,updated_at)
            VALUES ('51600000-0000-0000-0000-000000000001','51000000-0000-0000-0000-000000000001',
              'WEEKLY',current_date-7,current_date,'{"trend":"stable"}','MOTHER','ACTIVE',now(),now());
            """);

        migrate(WAVE);

        assertThat(number("SELECT count(*) FROM attachments WHERE attachment_id="
                + "'51200000-0000-0000-0000-000000000001' AND file_size_bytes=1024"))
                .isOne();
        assertThat(number("SELECT count(*) FROM health_record_attachments WHERE "
                + "health_record_attachment_id='51300000-0000-0000-0000-000000000001' "
                + "AND attachment_id='51200000-0000-0000-0000-000000000001' AND display_order=2"))
                .isOne();
        assertThat(number("SELECT count(*) FROM device_connections WHERE device_connection_id="
                + "'51400000-0000-0000-0000-000000000001' AND scopes_jsonb ? 'read'"))
                .isOne();
        assertThat(number("SELECT count(*) FROM health_observations WHERE health_observation_id="
                + "'51500000-0000-0000-0000-000000000001' AND observation_type='BLOOD_PRESSURE' "
                + "AND value_numeric=120 AND value_secondary=80 AND unit='mmHg'"))
                .isOne();
        assertThat(number("SELECT count(*) FROM health_records WHERE health_record_id="
                + "'51600000-0000-0000-0000-000000000001' AND record_type='SUMMARY' "
                + "AND summary_period='WEEKLY' AND summary_json->>'trend'='stable'"))
                .isOne();
        assertThat(number("SELECT count(*) FROM health_record_attachments l LEFT JOIN attachments a "
                + "ON a.attachment_id=l.attachment_id WHERE a.attachment_id IS NULL"))
                .isZero();
        assertThat(number("SELECT count(*) FROM health_observations o LEFT JOIN device_connections c "
                + "ON c.device_connection_id=o.device_connection_id "
                + "WHERE o.device_connection_id IS NOT NULL AND c.device_connection_id IS NULL"))
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
