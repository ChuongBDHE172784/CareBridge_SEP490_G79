package com.carebridge.backend.motherbaby;

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
class Wave2MotherBabyCleanupMigrationIntegrationTest {
    private static final MigrationVersion PRE_WAVE = MigrationVersion.fromVersion("20260722231100");
    private static final MigrationVersion WAVE = MigrationVersion.fromVersion("20260722231200");
    private static final String[] REMOVED = {
        "mother_baseline_contexts", "mother_journey_transitions", "pregnancy_outcome_evidence",
        "maternal_health_metrics", "postpartum_logs", "exercise_safety_checks", "exercise_sessions",
        "posture_feedback_events", "posture_analysis_configs", "pregnancy_exercises", "baby_daily_logs",
        "baby_link_submissions", "vaccination_reference_schedules"
    };
    private static final String[] TARGET = {
        "persons","care_subjects","users","user_identities","auth_sessions","auth_revocations",
        "auth_challenges","account_deletion_requests","mother_journeys","mother_journey_events",
        "maternal_observations","maternal_exercise_sessions","care_logs","growth_measurements",
        "development_milestones","vaccination_records","vaccination_schedules","community_profiles",
        "community_topics","community_content","community_interactions","professional_profiles","specialties",
        "professional_specialties","expert_credentials","expert_availability","expert_location_shares",
        "expert_contribution_events","triage_sessions","triage_session_evidence","red_flag_rules",
        "health_context_memories","knowledge_sources","knowledge_source_reviews","health_records","attachments",
        "health_record_attachments","device_connections","health_observations","care_groups","care_group_members",
        "scheduled_care_items","family_tasks","preparation_checklist_items","care_item_templates","content_items",
        "content_item_topics","content_item_sources","moderation_cases","moderation_events","notification_records",
        "device_tokens","safety_configs","safety_monitoring_sessions","safety_events","safety_event_actions",
        "emergency_contacts","administrative_areas","care_facilities","nearby_support_requests",
        "nearby_support_responses","audit_events","security_events","data_permissions","system_configurations",
        "expense_entries","archived_consultation_records","archived_realtime_records","archived_partner_records",
        "flyway_schema_history"
    };

    @Container
    final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    @Test
    void cleanBootstrapRemovesWave2LegacyAndKeepsAllCanonicalNames() throws Exception {
        migrate(PRE_WAVE);
        long tableCountBefore = tableCount();
        long transitionalTableCountBefore = nonTargetTableCount();
        migrate(WAVE);

        for (String table : REMOVED) assertThat(exists(table)).as(table).isFalse();
        for (String table : TARGET) assertThat(exists(table)).as(table).isTrue();
        assertThat(tableCount()).isEqualTo(tableCountBefore - REMOVED.length);
        assertThat(nonTargetTableCount())
                .isEqualTo(transitionalTableCountBefore - REMOVED.length);
        assertThat(number("SELECT count(*) FROM pg_constraint WHERE contype='f' AND NOT convalidated"))
                .isZero();
    }

    @Test
    void populatedUpgradeReconcilesEveryWave2SourceWithoutOrphans() throws Exception {
        migrate(PRE_WAVE);
        execute("""
            INSERT INTO persons(person_id,display_name) VALUES
              ('20000000-0000-0000-0000-000000000001','Wave Two Mother'),
              ('20000000-0000-0000-0000-000000000002','Wave Two Baby');
            INSERT INTO users(user_id,person_id,email,full_name,password_hash,phone,role,enabled,locked,created_at,updated_at)
            VALUES ('20000000-0000-0000-0000-000000000001','20000000-0000-0000-0000-000000000001',
              'wave2@example.test','Wave Two Mother','hash','+84900000002','MOTHER',true,false,now(),now());
            INSERT INTO care_subjects(care_subject_id,person_id,owner_user_id,subject_type,nickname,status,created_at,updated_at)
            VALUES
              ('21000000-0000-0000-0000-000000000001','20000000-0000-0000-0000-000000000001',
               '20000000-0000-0000-0000-000000000001','MOTHER','Mother','ACTIVE',now(),now()),
              ('22000000-0000-0000-0000-000000000001','20000000-0000-0000-0000-000000000002',
               '20000000-0000-0000-0000-000000000001','BABY','Baby','ACTIVE',now(),now());
            INSERT INTO mother_journeys(journey_id,care_subject_id,owner_user_id,journey_type,start_date,status,version,created_at,updated_at)
            VALUES ('21000000-0000-0000-0000-000000000001','21000000-0000-0000-0000-000000000001',
              '20000000-0000-0000-0000-000000000001','PREGNANCY',current_date,'ACTIVE',7,now(),now());
            UPDATE care_subjects SET mother_journey_id='21000000-0000-0000-0000-000000000001'
             WHERE care_subject_id IN ('21000000-0000-0000-0000-000000000001','22000000-0000-0000-0000-000000000001');

            INSERT INTO mother_baseline_contexts
              (baseline_id,owner_user_id,submission_id,revision,schema_version,source,lifecycle_goal,locale,time_zone,preferences,recorded_at)
            VALUES ('21100000-0000-0000-0000-000000000001','20000000-0000-0000-0000-000000000001',
              '21100000-0000-0000-0000-000000000002',1,'MOTHER_BASELINE_V1','SELF_REPORTED',
              'CURRENTLY_PREGNANT','vi','Asia/Ho_Chi_Minh','NUTRITION,SLEEP',now());
            INSERT INTO mother_journey_transitions
              (transition_id,journey_id,event_type,from_stage,to_stage,changes_json,source,confidence,reason,
               actor_user_id,effective_at,recorded_at,journey_version)
            VALUES ('21200000-0000-0000-0000-000000000001','21000000-0000-0000-0000-000000000001',
              'DATES_CHANGED','PREGNANCY','PREGNANCY','{\"estimatedDueDate\":\"changed\"}',
              'SELF_REPORTED','ESTIMATED','fixture','20000000-0000-0000-0000-000000000001',now(),now(),8);
            INSERT INTO pregnancy_outcome_evidence
              (evidence_id,journey_id,owner_user_id,submission_id,outcome_type,outcome_date,source,actor_user_id,
               reason,effective_at,revision_number,journey_version,semantic_hash,correction)
            VALUES ('21300000-0000-0000-0000-000000000001','21000000-0000-0000-0000-000000000001',
              '20000000-0000-0000-0000-000000000001','21300000-0000-0000-0000-000000000002',
              'LIVE_BIRTH',current_date,'SELF_REPORTED','20000000-0000-0000-0000-000000000001',
              'fixture outcome',now(),1,9,'fixture-hash',false);
            INSERT INTO maternal_health_metrics
              (metric_id,journey_id,metric_type,value_numeric,value_secondary,unit,measured_at,source_type,status,created_at,updated_at)
            VALUES ('21400000-0000-0000-0000-000000000001','21000000-0000-0000-0000-000000000001',
              'BLOOD_PRESSURE_SYSTOLIC',120,80,'mmHg',now(),'MANUAL','ACTIVE',now(),now());
            INSERT INTO postpartum_logs
              (postpartum_log_id,journey_id,submission_id,log_date,pain_level,bleeding_level,mood_level,
               sleep_hours,breastfeeding_note,symptom_note,status,created_at,updated_at)
            VALUES ('21500000-0000-0000-0000-000000000001','21000000-0000-0000-0000-000000000001',
              '21500000-0000-0000-0000-000000000002',current_date,2,'LIGHT',4,6.5,
              'feeding well','stable','ACTIVE',now(),now());

            INSERT INTO pregnancy_exercises
              (exercise_id,created_by,title,trimester_scope,difficulty_level,duration_minutes,
               instruction_content,safety_warning,supports_posture_analysis,status,version_no,created_at,updated_at)
            VALUES ('23000000-0000-0000-0000-000000000001','20000000-0000-0000-0000-000000000001',
              'Safe stretch','SECOND','EASY',15,'steps','stop on pain',true,'PUBLISHED',3,now()-interval '1 day',now());
            INSERT INTO posture_analysis_configs
              (posture_config_id,exercise_id,configured_by,analysis_mode,rule_or_model_version,
               confidence_threshold,feedback_level,config_json,effective_from,status,created_at,updated_at)
            VALUES ('23000000-0000-0000-0000-000000000002','23000000-0000-0000-0000-000000000001',
              '20000000-0000-0000-0000-000000000001','RULE_BASED','rules-v3',0.80,'DETAILED',
              '{\"angle\":15}',now()-interval '1 hour','ACTIVE',now()-interval '1 hour',now());
            INSERT INTO exercise_safety_checks
              (safety_check_id,exercise_id,journey_id,user_id,answer_json,red_flag_detected,
               result_status,completed_at,created_at)
            VALUES ('23000000-0000-0000-0000-000000000003','23000000-0000-0000-0000-000000000001',
              '21000000-0000-0000-0000-000000000001','20000000-0000-0000-0000-000000000001',
              '{\"Q1_NO_DIZZINESS\":true,\"Q2_NO_CONTRACTIONS\":true}',false,'CLEARED',now(),now());
            INSERT INTO exercise_sessions
              (exercise_session_id,exercise_id,journey_id,user_id,safety_check_id,started_at,ended_at,
               completion_percent,session_status,summary_json,created_at,updated_at)
            VALUES ('23000000-0000-0000-0000-000000000004','23000000-0000-0000-0000-000000000001',
              '21000000-0000-0000-0000-000000000001','20000000-0000-0000-0000-000000000001',
              '23000000-0000-0000-0000-000000000003',now()-interval '10 min',now(),100,'COMPLETED','{}',now(),now());
            INSERT INTO posture_feedback_events
              (feedback_event_id,exercise_session_id,posture_config_id,event_time_ms,posture_code,
               confidence_score,severity,feedback_text,keypoint_summary_json,created_at)
            VALUES ('23000000-0000-0000-0000-000000000005','23000000-0000-0000-0000-000000000004',
              '23000000-0000-0000-0000-000000000002',5000,'BACK_ANGLE',0.91,'INFO','good','{\"angle\":10}',now());

            INSERT INTO baby_daily_logs
              (baby_log_id,baby_id,log_type,started_at,ended_at,quantity,unit,note,recorded_by,status,created_at,updated_at)
            VALUES ('24000000-0000-0000-0000-000000000001','22000000-0000-0000-0000-000000000001',
              'FEEDING',now()-interval '20 min',now(),90,'ml','fixture','20000000-0000-0000-0000-000000000001',
              'ACTIVE',now(),now());
            INSERT INTO baby_link_submissions
              (link_submission_id,owner_user_id,operation_type,submission_id,semantic_intent,baby_id,journey_id,created_at)
            VALUES ('24100000-0000-0000-0000-000000000001','20000000-0000-0000-0000-000000000001',
              'LINK_EXISTING','24100000-0000-0000-0000-000000000002','fixture link',
              '22000000-0000-0000-0000-000000000001','21000000-0000-0000-0000-000000000001',now());
            INSERT INTO vaccination_reference_schedules(ref_id,vaccine_name,dose_number,offset_days,description,created_at)
            VALUES ('24200000-0000-0000-0000-000000000001','BCG',1,0,'birth dose',now());
            """);

        migrate(WAVE);

        assertThat(number("SELECT count(*) FROM mother_journey_events WHERE legacy_source IN "
                + "('MOTHER_BASELINE','JOURNEY_TRANSITION','PREGNANCY_OUTCOME','BABY_LINK')")).isEqualTo(4);
        assertThat(number("SELECT count(*) FROM maternal_observations WHERE legacy_source IN "
                + "('MATERNAL_METRIC','POSTPARTUM_LOG','EXERCISE_SAFETY','POSTURE_FEEDBACK')")).isEqualTo(4);
        assertThat(number("SELECT count(*) FROM maternal_observations WHERE legacy_source='EXERCISE_SAFETY_ANSWER'")).isEqualTo(2);
        assertThat(number("SELECT count(*) FROM maternal_exercise_sessions WHERE exercise_session_id='23000000-0000-0000-0000-000000000004' "
                + "AND posture_config_id='23000000-0000-0000-0000-000000000002' "
                + "AND safety_observation_id='23000000-0000-0000-0000-000000000003'")).isOne();
        assertThat(number("SELECT count(*) FROM care_item_templates WHERE template_id IN "
                + "('23000000-0000-0000-0000-000000000001','23000000-0000-0000-0000-000000000002')")).isEqualTo(2);
        assertThat(number("SELECT count(*) FROM care_logs WHERE care_log_id='24000000-0000-0000-0000-000000000001' AND quantity=90")).isOne();
        assertThat(number("SELECT count(*) FROM vaccination_schedules WHERE vaccination_schedule_id='24200000-0000-0000-0000-000000000001'")).isOne();
        assertThat(number("SELECT count(*) FROM mother_journeys WHERE journey_id='21000000-0000-0000-0000-000000000001' "
                + "AND baseline_revision=1 AND baseline_locale='vi'")).isOne();
        assertThat(number("SELECT count(*) FROM maternal_observations o LEFT JOIN mother_journeys j ON j.journey_id=o.mother_journey_id "
                + "WHERE o.mother_journey_id IS NOT NULL AND j.journey_id IS NULL")).isZero();
        assertThat(number("SELECT count(*) FROM maternal_exercise_sessions s LEFT JOIN care_item_templates t "
                + "ON t.template_id=s.exercise_template_id WHERE t.template_id IS NULL")).isZero();
        for (String table : REMOVED) assertThat(exists(table)).as(table).isFalse();
    }

    private void migrate(MigrationVersion target) {
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration").target(target).load().migrate();
    }

    private boolean exists(String table) throws Exception {
        try (Connection c=connection(); Statement s=c.createStatement(); ResultSet r=s.executeQuery(
                "SELECT to_regclass('public."+table+"') IS NOT NULL")) { r.next(); return r.getBoolean(1); }
    }

    private long tableCount() throws Exception {
        return number("SELECT count(*) FROM information_schema.tables "
                + "WHERE table_schema='public' AND table_type='BASE TABLE'");
    }

    private long nonTargetTableCount() throws Exception {
        return number("SELECT count(*) FROM information_schema.tables WHERE table_schema='public' "
                + "AND table_type='BASE TABLE' AND table_name <> ALL(ARRAY['"
                + String.join("','", TARGET) + "'])");
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
