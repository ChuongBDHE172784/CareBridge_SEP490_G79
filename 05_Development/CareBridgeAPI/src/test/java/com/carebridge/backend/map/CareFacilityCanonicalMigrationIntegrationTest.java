package com.carebridge.backend.map;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers(disabledWithoutDocker = true)
class CareFacilityCanonicalMigrationIntegrationTest {

    private static final MigrationVersion PRE_BATCH_5 =
            MigrationVersion.fromVersion("20260722020600");

    @Container
    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:16-alpine");

    @BeforeEach
    void resetSchema() throws Exception {
        execute("DROP SCHEMA public CASCADE; CREATE SCHEMA public");
    }

    @Test
    void migratesTwentyHospitalsAndTwoExpertReferencesWithoutPartnerOrCoordinates() throws Exception {
        migrateTo(PRE_BATCH_5);
        installAuditedLiveDriftFixture();

        migrateTo(null);

        assertThat(scalar("SELECT to_regclass('public.hospitals') IS NULL")).isEqualTo("t");
        assertThat(scalar("SELECT count(*) FROM care_facilities")).isEqualTo("25");
        assertThat(scalar("SELECT to_regclass('public.care_facility_legacy_ids') IS NULL"))
                .isEqualTo("t");
        assertThat(scalar("""
                SELECT count(*) FROM care_facilities
                 WHERE source_type='LEGACY_IMPORT' AND verification_status='UNVERIFIED'
                   AND facility_type='HOSPITAL' AND partner_id IS NULL
                   AND latitude IS NULL AND longitude IS NULL AND is_searchable=false
                """)).isEqualTo("20");
        assertThat(scalar("SELECT count(*) FROM professional_profiles WHERE facility_id IS NOT NULL"))
                .isEqualTo("2");
        assertThat(scalar("""
                SELECT count(*) FROM information_schema.columns
                 WHERE table_schema='public' AND table_name='expert_profiles' AND column_name='hospital_id'
                """)).isEqualTo("0");
    }

    @Test
    void unknownHospitalLevelIsPreservedAsCanonicalFacilityMetadata() throws Exception {
        migrateTo(PRE_BATCH_5);
        installAuditedLiveDriftFixture();
        execute("UPDATE hospitals SET level='Unknown' WHERE hospital_id='H0000001'");

        migrateTo(null);

        assertThat(scalar("SELECT to_regclass('public.hospitals') IS NULL")).isEqualTo("t");
        assertThat(scalar("""
                SELECT count(*) FROM care_facilities
                 WHERE external_source_id='H0000001' AND facility_level='Unknown'
                """)).isEqualTo("1");
        assertThat(scalar("SELECT count(*) FROM care_facilities")).isEqualTo("25");
        assertThat(scalar("SELECT to_regclass('public.care_facility_legacy_ids') IS NULL")).isEqualTo("t");
    }

    @Test
    void materializedViewDependencyBlocksBeforeDropAndRollsBack() throws Exception {
        migrateTo(PRE_BATCH_5);
        installAuditedLiveDriftFixture();
        execute("CREATE MATERIALIZED VIEW retained_hospitals AS SELECT hospital_id FROM hospitals");

        assertThatThrownBy(() -> migrateTo(null))
                .rootCause()
                .hasMessageContaining("cannot drop table hospitals because other objects depend on it");

        assertThat(scalar("SELECT to_regclass('public.hospitals') IS NOT NULL")).isEqualTo("t");
        assertThat(scalar("SELECT to_regclass('public.retained_hospitals') IS NOT NULL")).isEqualTo("t");
        assertThat(scalar("SELECT count(*) FROM care_facilities")).isEqualTo("5");
        assertThat(scalar("SELECT to_regclass('public.care_facility_legacy_ids') IS NULL")).isEqualTo("t");
    }

    private void installAuditedLiveDriftFixture() throws Exception {
        execute("""
                INSERT INTO provinces (province_id, name)
                VALUES ('92', 'Fixture province')
                ON CONFLICT (province_id) DO NOTHING;
                INSERT INTO districts (district_id, province_id, name)
                VALUES ('916', '92', 'Fixture district')
                ON CONFLICT (district_id) DO NOTHING;
                ALTER TABLE expert_profiles DROP CONSTRAINT fk_expert_profile_hospital;
                ALTER TABLE expert_profiles DROP COLUMN hospital_id;
                ALTER TABLE medical_contributions
                    DROP CONSTRAINT medical_contributions_hospital_id_fkey;
                DROP TABLE hospitals;
                CREATE TABLE hospitals (
                    hospital_id varchar(8) PRIMARY KEY,
                    address text,
                    district_id varchar(4),
                    is_active boolean NOT NULL,
                    level varchar(20),
                    name varchar(200) NOT NULL,
                    phone varchar(20),
                    province_id varchar(2) NOT NULL,
                    type varchar(30)
                );
                INSERT INTO hospitals
                    (hospital_id,address,district_id,is_active,level,name,phone,province_id,type)
                SELECT 'H' || lpad(n::text,7,'0'), 'Address ' || n, '916', true, 'Hạng I',
                       'Legacy hospital ' || n, '0292' || lpad(n::text,7,'0'), '92',
                       CASE WHEN n=20 THEN 'Quân đội' ELSE 'Công lập' END
                  FROM generate_series(1,20) n;
                ALTER TABLE expert_profiles ADD COLUMN hospital_id varchar(8);
                ALTER TABLE expert_profiles ADD CONSTRAINT fk_expert_profile_hospital
                    FOREIGN KEY (hospital_id) REFERENCES hospitals(hospital_id);
                ALTER TABLE medical_contributions
                    ADD CONSTRAINT medical_contributions_hospital_id_fkey
                    FOREIGN KEY (hospital_id) REFERENCES hospitals(hospital_id);
                INSERT INTO users (user_id,email,enabled,locked,role,created_at,updated_at)
                VALUES
                  ('00000000-0000-0000-0000-000000005001','facility-expert-1@test',true,false,'EXPERT',now(),now()),
                  ('00000000-0000-0000-0000-000000005002','facility-expert-2@test',true,false,'EXPERT',now(),now());
                INSERT INTO expert_profiles
                    (expert_profile_id,user_id,verification_status,trust_status,hospital_id,created_at,updated_at)
                VALUES
                  ('00000000-0000-0000-0000-000000005101','00000000-0000-0000-0000-000000005001',
                   'APPROVED','ACTIVE','H0000001',now(),now()),
                  ('00000000-0000-0000-0000-000000005102','00000000-0000-0000-0000-000000005002',
                   'APPROVED','ACTIVE','H0000002',now(),now());
                """);
    }

    private void migrateTo(MigrationVersion target) {
        var config = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .outOfOrder(true);
        if (target != null) config.target(target);
        config.load().migrate();
    }

    private void execute(String sql) throws Exception {
        try (Connection connection = connection(); var statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private String scalar(String sql) throws Exception {
        try (Connection connection = connection(); var statement = connection.createStatement();
             var result = statement.executeQuery(sql)) {
            result.next();
            return result.getString(1);
        }
    }

    private Connection connection() throws Exception {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }
}
