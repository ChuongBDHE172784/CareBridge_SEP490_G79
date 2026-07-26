package com.carebridge.backend.checklist;

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
class CanonicalChecklistSettingsIntegrityMigrationIntegrationTest {

    private static final MigrationVersion BEFORE_REMEDIATION =
            MigrationVersion.fromVersion("20260723230000");
    private static final MigrationVersion REMEDIATION =
            MigrationVersion.fromVersion("20260724212000");

    @Container
    final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Test
    void upgradeCanonicalizesBabyScopeAndRetainsIndependentJourneyScopes() throws Exception {
        migrate(BEFORE_REMEDIATION);
        seedHistoricalChecklistImports();

        migrate(REMEDIATION);

        assertThat(number("""
                select count(*)
                  from preparation_checklist_items
                 where owner_user_id = '72000000-0000-0000-0000-000000000001'
                   and baby_id = '72000000-0000-0000-0000-000000000021'
                   and template_entry_id = '72000000-0000-0000-0000-000000000031'
                """)).isOne();
        assertThat(number("""
                select count(*)
                  from preparation_checklist_items
                 where checklist_item_id = '72000000-0000-0000-0000-000000000042'
                   and mother_journey_id is null
                   and status = 'COMPLETED'
                """)).isOne();
        assertThat(number("""
                select count(*)
                  from preparation_checklist_items
                 where owner_user_id = '72000000-0000-0000-0000-000000000001'
                   and baby_id is null
                   and template_entry_id = '72000000-0000-0000-0000-000000000031'
                """)).isEqualTo(2);

        assertThat(number("""
                select count(*) from pg_indexes
                 where schemaname = 'public'
                   and indexname in (
                       'uq_preparation_checklist_baby_import_scope',
                       'uq_preparation_checklist_journey_import_scope')
                """)).isEqualTo(2);
        assertThat(number("""
                select count(*) from pg_indexes
                 where schemaname = 'public'
                   and indexname = 'uq_preparation_checklist_import_scope'
                """)).isZero();
        assertThat(number("""
                select character_maximum_length
                  from information_schema.columns
                 where table_schema = 'public'
                   and table_name = 'care_item_templates'
                   and column_name = 'title'
                """)).isEqualTo(500);
        assertThat(number("""
                select count(*)
                  from information_schema.columns
                 where table_schema = 'public'
                   and table_name = 'notification_records'
                   and column_name = 'claim_token'
                """)).isOne();
        assertThat(number("""
                select count(*)
                  from pg_constraint
                 where conname = 'users_settings_jsonb_object_ck'
                   and convalidated
                """)).isOne();
    }

    private void seedHistoricalChecklistImports() throws Exception {
        execute("""
                insert into persons(person_id,display_name,created_at,updated_at) values
                  ('72000000-0000-0000-0000-000000000001','Owner',now(),now()),
                  ('72000000-0000-0000-0000-000000000011','Journey one',now(),now()),
                  ('72000000-0000-0000-0000-000000000012','Journey two',now(),now()),
                  ('72000000-0000-0000-0000-000000000013','Baby',now(),now());
                insert into users(
                    user_id,person_id,email,role,account_status,enabled,locked,
                    email_verified,phone_verified,settings_jsonb,created_at,updated_at)
                values (
                    '72000000-0000-0000-0000-000000000001',
                    '72000000-0000-0000-0000-000000000001',
                    'checklist.migration@test','MOTHER','ACTIVE',true,false,true,false,
                    '{"unrelated":"keep"}'::jsonb,now(),now());
                insert into care_subjects(
                    care_subject_id,person_id,owner_user_id,subject_type,nickname,status,
                    created_at,updated_at) values
                  ('72000000-0000-0000-0000-000000000011',
                   '72000000-0000-0000-0000-000000000011',
                   '72000000-0000-0000-0000-000000000001','MOTHER','Journey one','ACTIVE',now(),now()),
                  ('72000000-0000-0000-0000-000000000012',
                   '72000000-0000-0000-0000-000000000012',
                   '72000000-0000-0000-0000-000000000001','MOTHER','Journey two','ACTIVE',now(),now()),
                  ('72000000-0000-0000-0000-000000000021',
                   '72000000-0000-0000-0000-000000000013',
                   '72000000-0000-0000-0000-000000000001','BABY','Baby','ACTIVE',now(),now());
                insert into mother_journeys(
                    journey_id,care_subject_id,owner_user_id,journey_type,status,
                    created_at,updated_at) values
                  ('72000000-0000-0000-0000-000000000011',
                   '72000000-0000-0000-0000-000000000011',
                   '72000000-0000-0000-0000-000000000001','PREGNANCY','COMPLETED',now(),now()),
                  ('72000000-0000-0000-0000-000000000012',
                   '72000000-0000-0000-0000-000000000012',
                   '72000000-0000-0000-0000-000000000001','POSTPARTUM','ACTIVE',now(),now());
                insert into care_item_templates(
                    template_id,entry_type,title,display_order,stage,is_active,
                    content_status,version,created_at,updated_at) values
                  ('72000000-0000-0000-0000-000000000030','TEMPLATE_ROOT',
                   'Template',0,'BABY_CARE',true,'APPROVED',1,now(),now()),
                  ('72000000-0000-0000-0000-000000000031','CHECKLIST_ENTRY',
                   'Entry','1','BABY_CARE',true,'APPROVED',1,now(),now());
                update care_item_templates
                   set parent_template_id = '72000000-0000-0000-0000-000000000030'
                 where template_id = '72000000-0000-0000-0000-000000000031';
                insert into preparation_checklist_items(
                    checklist_item_id,owner_user_id,mother_journey_id,baby_id,
                    template_entry_id,title,category,status,display_order,
                    created_at,updated_at) values
                  ('72000000-0000-0000-0000-000000000041',
                   '72000000-0000-0000-0000-000000000001',
                   '72000000-0000-0000-0000-000000000011',
                   '72000000-0000-0000-0000-000000000021',
                   '72000000-0000-0000-0000-000000000031','Baby old','GENERAL','OPEN',1,
                   now()-interval '1 day',now()-interval '1 day'),
                  ('72000000-0000-0000-0000-000000000042',
                   '72000000-0000-0000-0000-000000000001',
                   '72000000-0000-0000-0000-000000000012',
                   '72000000-0000-0000-0000-000000000021',
                   '72000000-0000-0000-0000-000000000031','Baby completed','GENERAL','COMPLETED',1,
                   now(),now()),
                  ('72000000-0000-0000-0000-000000000043',
                   '72000000-0000-0000-0000-000000000001',
                   '72000000-0000-0000-0000-000000000011',null,
                   '72000000-0000-0000-0000-000000000031','Journey one','GENERAL','OPEN',1,now(),now()),
                  ('72000000-0000-0000-0000-000000000044',
                   '72000000-0000-0000-0000-000000000001',
                   '72000000-0000-0000-0000-000000000012',null,
                   '72000000-0000-0000-0000-000000000031','Journey two','GENERAL','OPEN',1,now(),now());
                """);
    }

    private void migrate(MigrationVersion target) {
        Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .target(target)
                .load()
                .migrate();
    }

    private long number(String sql) throws Exception {
        try (Connection connection = connection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return result.getLong(1);
        }
    }

    private void execute(String sql) throws Exception {
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private Connection connection() throws Exception {
        return DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    }
}
