package com.carebridge.backend.family;

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
class Wave6FamilyCarePlanCleanupMigrationIntegrationTest {

    private static final MigrationVersion PRE = MigrationVersion.fromVersion("20260722231500");
    private static final MigrationVersion WAVE = MigrationVersion.fromVersion("20260722231600");
    private static final String[] REMOVED = {
        "reminders", "care_tasks", "user_checklist_items", "checklist_templates", "checklist_items"
    };

    @Container
    final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Test
    void cleanBootstrapRemovesFiveWave6Tables() throws Exception {
        migrate(PRE);
        long tableCountBefore = tableCount();
        migrate(WAVE);
        assertThat(tableCount()).isEqualTo(tableCountBefore - REMOVED.length);
        for (String table : REMOVED) assertThat(exists(table)).as(table).isFalse();
        assertThat(number("SELECT count(*) FROM pg_constraint WHERE contype='f' AND NOT convalidated"))
                .isZero();
    }

    @Test
    void populatedUpgradePreservesRecurrenceAssigneeCompletionSnoozeAndTemplateHierarchy() throws Exception {
        migrate(PRE);
        execute("""
            INSERT INTO persons(person_id,display_name) VALUES
              ('61000000-0000-0000-0000-000000000001','Wave 6 Mother'),
              ('61000000-0000-0000-0000-000000000002','Wave 6 Family');
            INSERT INTO users(user_id,person_id,email,role,enabled,locked,created_at,updated_at) VALUES
              ('61000000-0000-0000-0000-000000000001','61000000-0000-0000-0000-000000000001','mother.wave6@test','MOTHER',true,false,now(),now()),
              ('61000000-0000-0000-0000-000000000002','61000000-0000-0000-0000-000000000002','family.wave6@test','FAMILY',true,false,now(),now());
            INSERT INTO care_groups
              (care_group_id,owner_user_id,group_name,status,created_at,updated_at)
            VALUES ('61100000-0000-0000-0000-000000000001','61000000-0000-0000-0000-000000000001',
              'Wave 6 group','ACTIVE',now(),now());
            INSERT INTO reminders
              (reminder_id,owner_user_id,reminder_type,title,scheduled_at,recurrence_rule,
               recurrence_type,recurrence_end_date,fcm_job_id,status,snoozed_until,created_at,updated_at)
            VALUES ('61200000-0000-0000-0000-000000000001','61000000-0000-0000-0000-000000000001',
              'MEDICATION','Take vitamin',now()+interval '1 day','FREQ=DAILY','DAILY',
              now()+interval '30 days','wave6-job','SNOOZED',now()+interval '2 hours',now(),now());
            INSERT INTO care_tasks
              (care_task_id,care_group_id,assigned_by,assigned_to,title,description,due_at,
               status,completed_at,created_at,updated_at)
            VALUES ('61300000-0000-0000-0000-000000000001','61100000-0000-0000-0000-000000000001',
              '61000000-0000-0000-0000-000000000001','61000000-0000-0000-0000-000000000002',
              'Prepare bag','Hospital bag',now()+interval '3 days','COMPLETED',now(),now(),now());
            INSERT INTO checklist_templates
              (checklist_template_id,name,stage,status,description,version_no,created_at,updated_at)
            VALUES ('61400000-0000-0000-0000-000000000001','Birth preparation','PREGNANCY',
              'PUBLISHED','Wave 6 template',2,now(),now());
            INSERT INTO checklist_items
              (checklist_item_id,checklist_template_id,item_text,item_order,is_required,note,created_at,updated_at)
            VALUES ('61500000-0000-0000-0000-000000000001','61400000-0000-0000-0000-000000000001',
              'Bring documents',1,true,'Required document',now(),now());
            INSERT INTO user_checklist_items
              (user_checklist_item_id,owner_user_id,template_item_id,item_text,category,
               is_completed,completed_at,item_order,created_at,updated_at)
            VALUES ('61600000-0000-0000-0000-000000000001','61000000-0000-0000-0000-000000000001',
              '61500000-0000-0000-0000-000000000001','Bring documents','PAPERWORK',true,now(),1,now(),now());
            """);

        migrate(WAVE);

        assertThat(number("SELECT count(*) FROM scheduled_care_items WHERE care_item_id="
                + "'61200000-0000-0000-0000-000000000001' AND recurrence_type='DAILY' "
                + "AND recurrence_rule='FREQ=DAILY' AND status='SNOOZED' AND snoozed_until IS NOT NULL"))
                .isOne();
        assertThat(number("SELECT count(*) FROM family_tasks WHERE task_id="
                + "'61300000-0000-0000-0000-000000000001' AND assignee_user_id="
                + "'61000000-0000-0000-0000-000000000002' AND status='COMPLETED' AND completed_at IS NOT NULL"))
                .isOne();
        assertThat(number("SELECT count(*) FROM care_item_templates child JOIN care_item_templates root "
                + "ON root.template_id=child.parent_template_id WHERE root.template_id="
                + "'61400000-0000-0000-0000-000000000001' AND root.entry_type='TEMPLATE_ROOT' "
                + "AND root.content_status='PUBLISHED' AND child.template_id="
                + "'61500000-0000-0000-0000-000000000001' AND child.entry_type='CHECKLIST_ENTRY' "
                + "AND child.is_required"))
                .isOne();
        assertThat(number("SELECT count(*) FROM preparation_checklist_items WHERE checklist_item_id="
                + "'61600000-0000-0000-0000-000000000001' AND status='COMPLETED' "
                + "AND category='PAPERWORK' AND completed_at IS NOT NULL"))
                .isOne();
        assertThat(number("SELECT count(*) FROM preparation_checklist_items p LEFT JOIN care_item_templates t "
                + "ON t.template_id=p.template_entry_id "
                + "WHERE p.template_entry_id IS NOT NULL AND t.template_id IS NULL"))
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
