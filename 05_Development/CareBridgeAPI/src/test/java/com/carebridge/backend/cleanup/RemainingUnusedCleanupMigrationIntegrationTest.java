package com.carebridge.backend.cleanup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers(disabledWithoutDocker = true)
class RemainingUnusedCleanupMigrationIntegrationTest {

    private static final MigrationVersion PRE_CLEANUP =
            MigrationVersion.fromVersion("20260722020900");
    private static final MigrationVersion CLEANUP =
            MigrationVersion.fromVersion("20260722021000");
    private static final String[] REMOVED = {
            "contribution_attachments",
            "expert_identity_verifications",
            "expert_verification_documents",
            "impact_assessment_ratings",
            "medical_contributions"
    };

    @Container
    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:16-alpine");

    @BeforeEach
    void resetSchema() throws Exception {
        execute("DROP SCHEMA public CASCADE; CREATE SCHEMA public");
    }

    @Test
    void cleanBootstrapAcceptsOnlyKnownLiveOnlyAbsences() throws Exception {
        migrateTo(PRE_CLEANUP);
        migrateTo(CLEANUP);

        for (String table : REMOVED) assertThat(exists(table)).as(table).isFalse();
        assertThat(exists("expert_credentials")).isTrue();
        assertThat(exists("contribution_points")).isTrue();
        assertThat(Integer.parseInt(scalar("""
                SELECT count(*)
                  FROM information_schema.tables
                 WHERE table_schema = 'public'
                   AND table_type = 'BASE TABLE'
                """)))
                .as("clean-bootstrap public base-table count")
                .isEqualTo(99);
    }

    @Test
    void auditedLegacyShapeDropsKnownLiveOnlyTables() throws Exception {
        migrateTo(PRE_CLEANUP);
        installLiveOnlyEmptyFixture();
        migrateTo(CLEANUP);

        for (String table : REMOVED) assertThat(exists(table)).as(table).isFalse();
    }

    @Test
    void nonzeroCandidateRollsBackCompleteWave() throws Exception {
        migrateTo(PRE_CLEANUP);
        execute("""
                SET session_replication_role = replica;
                INSERT INTO impact_assessment_ratings
                    (rating_id, user_id, content_id, rating_value, created_at, updated_at)
                VALUES (gen_random_uuid(), gen_random_uuid(), gen_random_uuid(), 1, now(), now());
                SET session_replication_role = origin
                """);

        assertThatThrownBy(() -> migrateTo(CLEANUP))
                .rootCause()
                .hasMessageContaining("BLOCKED_FINAL_CLEANUP")
                .hasMessageContaining("impact_assessment_ratings");
        assertThat(exists("impact_assessment_ratings")).isTrue();
        assertThat(exists("expert_verification_documents")).isTrue();
    }

    @Test
    void dependentViewRollsBackCompleteWave() throws Exception {
        migrateTo(PRE_CLEANUP);
        execute("CREATE VIEW retained_impact_ratings AS SELECT * FROM impact_assessment_ratings");

        assertThatThrownBy(() -> migrateTo(CLEANUP))
                .rootCause()
                .hasMessageContaining("BLOCKED_FINAL_CLEANUP")
                .hasMessageContaining("impact_assessment_ratings");
        assertThat(exists("retained_impact_ratings")).isTrue();
        assertThat(exists("expert_verification_documents")).isTrue();
    }

    @Test
    void unexpectedCandidateShapeRollsBackCompleteWave() throws Exception {
        migrateTo(PRE_CLEANUP);
        execute("ALTER TABLE impact_assessment_ratings ADD COLUMN unreviewed_payload jsonb");

        assertThatThrownBy(() -> migrateTo(CLEANUP))
                .rootCause()
                .hasMessageContaining("BLOCKED_FINAL_CLEANUP")
                .hasMessageContaining("unapproved catalog shape");
        assertThat(exists("impact_assessment_ratings")).isTrue();
        assertThat(exists("expert_verification_documents")).isTrue();
    }

    @Test
    void publicGrantRollsBackCompleteWave() throws Exception {
        migrateTo(PRE_CLEANUP);
        execute("GRANT SELECT ON expert_verification_documents TO PUBLIC");

        assertThatThrownBy(() -> migrateTo(CLEANUP))
                .rootCause()
                .hasMessageContaining("BLOCKED_FINAL_CLEANUP")
                .hasMessageContaining("external table or column grants");
        assertThat(exists("impact_assessment_ratings")).isTrue();
        assertThat(exists("expert_verification_documents")).isTrue();
    }

    private void installLiveOnlyEmptyFixture() throws Exception {
        execute("""
                CREATE TABLE medical_contributions (
                    contribution_id uuid PRIMARY KEY,
                    content text NOT NULL,
                    created_at timestamptz NOT NULL,
                    expert_user_id uuid NOT NULL,
                    hospital_id varchar,
                    rejection_reason varchar,
                    specialty_id varchar,
                    status varchar NOT NULL,
                    title varchar NOT NULL,
                    updated_at timestamptz NOT NULL,
                    version integer NOT NULL,
                    CONSTRAINT medical_contributions_status_check
                        CHECK (status IN ('DRAFT', 'SUBMITTED', 'APPROVED', 'REJECTED'))
                );
                CREATE INDEX idx_medical_contributions_expert_user_id
                    ON medical_contributions (expert_user_id);
                CREATE INDEX idx_medical_contributions_hospital_id
                    ON medical_contributions (hospital_id);
                CREATE INDEX idx_medical_contributions_specialty_id
                    ON medical_contributions (specialty_id);
                CREATE INDEX idx_medical_contributions_status
                    ON medical_contributions (status);
                CREATE TABLE contribution_attachments (
                    attachment_id uuid PRIMARY KEY,
                    access_mode varchar NOT NULL,
                    contribution_id uuid NOT NULL,
                    created_at timestamptz NOT NULL,
                    display_order integer NOT NULL,
                    file_id uuid NOT NULL,
                    kind varchar NOT NULL,
                    owner_user_id uuid NOT NULL,
                    purpose varchar NOT NULL,
                    CONSTRAINT contribution_attachments_access_mode_check
                        CHECK (access_mode IN ('PRIVATE', 'AUTHENTICATED', 'PUBLIC')),
                    CONSTRAINT contribution_attachments_kind_check
                        CHECK (kind IN ('IMAGE', 'DOCUMENT')),
                    CONSTRAINT contribution_attachments_purpose_check CHECK (purpose IN (
                        'EXPERT_IDENTITY_SELFIE', 'EXPERT_IDENTITY_CCCD_FRONT',
                        'EXPERT_IDENTITY_CCCD_BACK', 'EXPERT_IDENTITY_SELFIE_CROP',
                        'EXPERT_IDENTITY_CCCD_FRONT_CROP', 'EXPERT_CREDENTIAL',
                        'COMMUNITY_ANSWER_IMAGE', 'MEDICAL_CONTRIBUTION_IMAGE',
                        'MEDICAL_CONTRIBUTION_DOCUMENT', 'PUBLIC_CONTENT_IMAGE'))
                );
                CREATE INDEX idx_contrib_attachments_contribution_id
                    ON contribution_attachments (contribution_id);
                CREATE INDEX idx_contrib_attachments_file_id
                    ON contribution_attachments (file_id);
                CREATE TABLE expert_identity_verifications (
                    identity_verification_id uuid PRIMARY KEY,
                    created_at timestamptz NOT NULL,
                    expert_profile_id uuid NOT NULL,
                    face_provider varchar NOT NULL,
                    face_similarity numeric,
                    face_status varchar NOT NULL,
                    face_threshold numeric,
                    identity_back_file_id uuid NOT NULL,
                    identity_front_file_id uuid NOT NULL,
                    provider_error_code varchar,
                    review_reason text,
                    review_status varchar NOT NULL,
                    reviewed_at timestamptz,
                    reviewed_by uuid,
                    selfie_file_id uuid NOT NULL,
                    updated_at timestamptz NOT NULL,
                    detection_id_card_status varchar,
                    detection_selfie_status varchar,
                    id_card_crop_file_id uuid,
                    pipeline_error_code varchar,
                    pipeline_status varchar,
                    processed_at timestamptz,
                    selfie_crop_file_id uuid,
                    CONSTRAINT expert_identity_verifications_face_status_check CHECK (face_status IN (
                        'DISABLED', 'MATCHED', 'NOT_MATCHED', 'RETRYABLE_ERROR',
                        'NO_FACE', 'MULTIPLE_FACES')),
                    CONSTRAINT expert_identity_verifications_review_status_check CHECK (review_status IN (
                        'PENDING_REVIEW', 'MANUAL_REVIEW_REQUIRED', 'APPROVED', 'REJECTED')),
                    CONSTRAINT fk_expert_identity_id_card_crop_file
                        FOREIGN KEY (id_card_crop_file_id) REFERENCES uploaded_files(file_id)
                        ON DELETE SET NULL,
                    CONSTRAINT fk_expert_identity_selfie_crop_file
                        FOREIGN KEY (selfie_crop_file_id) REFERENCES uploaded_files(file_id)
                        ON DELETE SET NULL
                );
                CREATE INDEX idx_expert_identity_pipeline_status
                    ON expert_identity_verifications (pipeline_status, created_at);
                CREATE INDEX idx_expert_identity_profile_created
                    ON expert_identity_verifications (expert_profile_id, created_at DESC);
                CREATE INDEX idx_expert_identity_review_status
                    ON expert_identity_verifications (review_status, created_at)
                """);
    }

    private void migrateTo(MigrationVersion target) {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .outOfOrder(true)
                .target(target)
                .load()
                .migrate();
    }

    private void execute(String sql) throws Exception {
        try (Connection connection = connection(); var statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private boolean exists(String relation) throws Exception {
        return "t".equals(scalar("SELECT to_regclass('public." + relation + "') IS NOT NULL"));
    }

    private String scalar(String sql) throws Exception {
        try (Connection connection = connection(); var statement = connection.createStatement();
             var result = statement.executeQuery(sql)) {
            result.next();
            return result.getString(1);
        }
    }

    private Connection connection() throws Exception {
        return DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }
}
