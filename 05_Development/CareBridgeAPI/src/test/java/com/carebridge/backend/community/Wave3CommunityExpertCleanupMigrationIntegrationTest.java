package com.carebridge.backend.community;

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
class Wave3CommunityExpertCleanupMigrationIntegrationTest {

    private static final MigrationVersion PRE = MigrationVersion.fromVersion("20260722231200");
    private static final MigrationVersion WAVE = MigrationVersion.fromVersion("20260722231300");
    private static final String[] REMOVED = {
        "community_questions", "community_answers", "community_question_likes",
        "community_answer_likes", "community_bookmarks", "user_topic_follows",
        "question_notification_mutes", "expert_profiles", "contribution_points"
    };

    @Container
    final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Test
    void cleanBootstrapRemovesNineWave3Tables() throws Exception {
        migrate(PRE);
        assertThat(tableCount()).isEqualTo(120);
        migrate(WAVE);
        assertThat(tableCount()).isEqualTo(111);
        for (String table : REMOVED) {
            assertThat(exists(table)).as(table).isFalse();
        }
        assertThat(number("SELECT count(*) FROM pg_constraint WHERE contype='f' AND NOT convalidated"))
                .isZero();
    }

    @Test
    void populatedUpgradePreservesAllCommunityAndExpertSubtypes() throws Exception {
        migrate(PRE);
        execute("""
            INSERT INTO persons(person_id,display_name) VALUES
              ('31000000-0000-0000-0000-000000000001','Community Mother'),
              ('31000000-0000-0000-0000-000000000002','Community Expert');
            INSERT INTO users(user_id,person_id,email,role,enabled,locked,created_at,updated_at) VALUES
              ('31000000-0000-0000-0000-000000000001','31000000-0000-0000-0000-000000000001','mother.wave3@test','MOTHER',true,false,now(),now()),
              ('31000000-0000-0000-0000-000000000002','31000000-0000-0000-0000-000000000002','expert.wave3@test','EXPERT',true,false,now(),now());
            INSERT INTO community_topics(id,name,slug,description,created_at,updated_at,is_hidden,sort_order)
            VALUES ('31100000-0000-0000-0000-000000000001','Wave 3 Topic','wave-3-topic','fixture',now(),now(),false,1);
            INSERT INTO community_questions
              (id,topic_id,author_id,title,body,stage,pregnancy_week,urgency,is_anonymous,status,like_count,answer_count,created_at,updated_at)
            VALUES ('31200000-0000-0000-0000-000000000001','31100000-0000-0000-0000-000000000001',
              '31000000-0000-0000-0000-000000000001','Question','Body','PREGNANCY',20,'NORMAL',false,'APPROVED',1,1,now(),now());
            INSERT INTO community_answers
              (id,question_id,author_id,body,is_expert_labeled,is_personal_experience,status,like_count,created_at,updated_at)
            VALUES ('31300000-0000-0000-0000-000000000001','31200000-0000-0000-0000-000000000001',
              '31000000-0000-0000-0000-000000000002','Answer',true,false,'APPROVED',1,now(),now());
            INSERT INTO community_question_likes(id,user_id,question_id,created_at)
            VALUES ('31400000-0000-0000-0000-000000000001','31000000-0000-0000-0000-000000000002','31200000-0000-0000-0000-000000000001',now());
            INSERT INTO community_answer_likes(id,user_id,answer_id,created_at)
            VALUES ('31400000-0000-0000-0000-000000000002','31000000-0000-0000-0000-000000000001','31300000-0000-0000-0000-000000000001',now());
            INSERT INTO community_bookmarks(id,user_id,question_id,created_at)
            VALUES ('31400000-0000-0000-0000-000000000003','31000000-0000-0000-0000-000000000001','31200000-0000-0000-0000-000000000001',now());
            INSERT INTO user_topic_follows(id,user_id,topic_id,created_at)
            VALUES ('31400000-0000-0000-0000-000000000004','31000000-0000-0000-0000-000000000001','31100000-0000-0000-0000-000000000001',now());
            INSERT INTO question_notification_mutes(id,user_id,question_id,created_at)
            VALUES ('31400000-0000-0000-0000-000000000005','31000000-0000-0000-0000-000000000002','31200000-0000-0000-0000-000000000001',now());
            INSERT INTO expert_profiles
              (expert_profile_id,user_id,specialty,professional_title,experience_years,workplace,
               verification_status,trust_status,rating_avg,created_at,updated_at)
            VALUES ('31500000-0000-0000-0000-000000000001','31000000-0000-0000-0000-000000000002',
              'Sản khoa','Bác sĩ',10,'CareBridge','APPROVED','ACTIVE',4.8,now(),now());
            INSERT INTO contribution_points(point_record_id,points,reason,recorded_at,source_id,source_type,user_id)
            VALUES ('31600000-0000-0000-0000-000000000001',10,'ANSWER_APPROVED',now(),
              '31300000-0000-0000-0000-000000000001','COMMUNITY_ANSWER','31000000-0000-0000-0000-000000000002');
            """);

        migrate(WAVE);

        assertThat(number("SELECT count(*) FROM community_content WHERE content_id IN "
                + "('31200000-0000-0000-0000-000000000001','31300000-0000-0000-0000-000000000001')"))
                .isEqualTo(2);
        assertThat(number("SELECT count(*) FROM community_content WHERE content_type='ANSWER' "
                + "AND parent_content_id='31200000-0000-0000-0000-000000000001' AND is_expert_labeled"))
                .isOne();
        assertThat(number("SELECT count(*) FROM community_interactions WHERE interaction_id::text LIKE '31400000-%'"))
                .isEqualTo(5);
        assertThat(number("SELECT count(*) FROM professional_profiles WHERE professional_profile_id="
                + "'31500000-0000-0000-0000-000000000001' AND trust_status='ACTIVE'"))
                .isOne();
        assertThat(number("SELECT count(*) FROM professional_specialties ps JOIN specialties s USING(specialty_id) "
                + "WHERE ps.professional_profile_id='31500000-0000-0000-0000-000000000001'"))
                .isOne();
        assertThat(number("SELECT count(*) FROM expert_contribution_events WHERE contribution_event_id="
                + "'31600000-0000-0000-0000-000000000001' AND professional_profile_id="
                + "'31500000-0000-0000-0000-000000000001'"))
                .isOne();
        assertThat(number("SELECT count(*) FROM community_content a LEFT JOIN community_content q "
                + "ON q.content_id=a.parent_content_id WHERE a.content_type='ANSWER' AND q.content_id IS NULL"))
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
