package com.carebridge.backend.content.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.DriverManager;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers(disabledWithoutDocker = true)
class ContentStageConsolidationMigrationTest {

    private static final UUID POSTPARTUM_CATEGORY_ID =
            UUID.fromString("b1b2c3d4-e5f6-7890-abcd-ef1234567803");
    private static final UUID BABY_CARE_CATEGORY_ID =
            UUID.fromString("b1b2c3d4-e5f6-7890-abcd-ef1234567804");

    @Container
    final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Test
    void v4ConsolidatesStagesTopicsContentAndDuplicateInteractions() throws Exception {
        flyway(MigrationVersion.fromVersion("3")).migrate();

        UUID userWithDuplicateFollows = UUID.randomUUID();
        UUID userFollowingOnlyBabyCare = UUID.randomUUID();
        UUID childTopicId = UUID.randomUUID();
        UUID contentItemId = UUID.randomUUID();
        UUID communityContentId = UUID.randomUUID();

        try (var connection = connection();
             var statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO public.users (
                        user_id, person_id, interest_stage, created_at, updated_at
                    ) VALUES
                    ('%s', '%s', 'BABY_CARE', now(), now()),
                    ('%s', '%s', null, now(), now())
                    """.formatted(
                    userWithDuplicateFollows, UUID.randomUUID(),
                    userFollowingOnlyBabyCare, UUID.randomUUID()));
            statement.executeUpdate("""
                    INSERT INTO public.content_items (
                        content_item_id, topic_id, status, stage, created_at, updated_at
                    ) VALUES ('%s', '%s', 'DRAFT', 'BABY_CARE', now(), now())
                    """.formatted(contentItemId, BABY_CARE_CATEGORY_ID));
            statement.executeUpdate("""
                    INSERT INTO public.care_item_templates (entry_type, title, stage)
                    VALUES ('CHECKLIST', 'Legacy baby-care template', 'BABY_CARE')
                    """);
            statement.executeUpdate("""
                    INSERT INTO public.community_topics (
                        id, created_at, updated_at, name, slug, type, parent_id
                    ) VALUES (
                        '%s', now(), now(), 'Legacy baby-care child', '%s', 'TOPIC', '%s'
                    )
                    """.formatted(
                    childTopicId,
                    "legacy-baby-care-child-" + childTopicId,
                    BABY_CARE_CATEGORY_ID));
            statement.executeUpdate("""
                    INSERT INTO public.community_content (
                        content_id, topic_id, author_user_id, content_type, body, stage
                    ) VALUES ('%s', '%s', '%s', 'QUESTION', 'Legacy question', 'BABY_CARE')
                    """.formatted(
                    communityContentId,
                    BABY_CARE_CATEGORY_ID,
                    userWithDuplicateFollows));
            statement.executeUpdate("""
                    INSERT INTO public.community_interactions (
                        interaction_id, actor_user_id, interaction_type, topic_id
                    ) VALUES
                    (gen_random_uuid(), '%s', 'FOLLOW', '%s'),
                    (gen_random_uuid(), '%s', 'FOLLOW', '%s'),
                    (gen_random_uuid(), '%s', 'FOLLOW', '%s')
                    """.formatted(
                    userWithDuplicateFollows, POSTPARTUM_CATEGORY_ID,
                    userWithDuplicateFollows, BABY_CARE_CATEGORY_ID,
                    userFollowingOnlyBabyCare, BABY_CARE_CATEGORY_ID));
        }

        var migrationResult = flyway(null).migrate();
        assertThat(migrationResult.success).isTrue();
        assertThat(migrationResult.migrationsExecuted).isEqualTo(1);

        try (var connection = connection();
             var statement = connection.createStatement()) {
            assertThat(singleLong(statement, """
                    SELECT count(*) FROM (
                        SELECT 1 FROM public.content_items WHERE stage = 'BABY_CARE'
                        UNION ALL
                        SELECT 1 FROM public.care_item_templates WHERE stage = 'BABY_CARE'
                        UNION ALL
                        SELECT 1 FROM public.community_content WHERE stage = 'BABY_CARE'
                        UNION ALL
                        SELECT 1 FROM public.users WHERE interest_stage = 'BABY_CARE'
                    ) legacy_stages
                    """)).isZero();
            assertThat(singleLong(statement, """
                    SELECT count(*) FROM public.content_items WHERE stage = 'POSTPARTUM'
                    """)).isPositive();
            assertThat(singleUuid(statement, """
                    SELECT topic_id FROM public.content_items WHERE content_item_id = '%s'
                    """.formatted(contentItemId))).isEqualTo(POSTPARTUM_CATEGORY_ID);
            assertThat(singleLong(statement, """
                    SELECT count(*) FROM public.care_item_templates WHERE stage = 'POSTPARTUM'
                    """)).isPositive();
            assertThat(singleLong(statement, """
                    SELECT count(*) FROM public.users
                    WHERE user_id = '%s' AND interest_stage = 'POSTPARTUM'
                    """.formatted(userWithDuplicateFollows))).isEqualTo(1);

            try (var result = statement.executeQuery("""
                    SELECT name, description
                    FROM public.community_topics
                    WHERE id = '%s'
                    """.formatted(POSTPARTUM_CATEGORY_ID))) {
                assertThat(result.next()).isTrue();
                assertThat(result.getString("name")).isEqualTo("Hậu sản & Chăm bé");
                assertThat(result.getString("description"))
                        .isEqualTo("Hồi phục sau sinh và chăm sóc bé");
            }

            assertThat(singleLong(statement, """
                    SELECT count(*) FROM public.community_topics WHERE id = '%s'
                    """.formatted(BABY_CARE_CATEGORY_ID))).isZero();
            assertThat(singleLong(statement, """
                    SELECT count(*) FROM public.content_items WHERE topic_id = '%s'
                    """.formatted(BABY_CARE_CATEGORY_ID))).isZero();
            assertThat(singleUuid(statement, """
                    SELECT parent_id FROM public.community_topics WHERE id = '%s'
                    """.formatted(childTopicId))).isEqualTo(POSTPARTUM_CATEGORY_ID);

            try (var result = statement.executeQuery("""
                    SELECT topic_id, stage
                    FROM public.community_content
                    WHERE content_id = '%s'
                    """.formatted(communityContentId))) {
                assertThat(result.next()).isTrue();
                assertThat(result.getObject("topic_id", UUID.class))
                        .isEqualTo(POSTPARTUM_CATEGORY_ID);
                assertThat(result.getString("stage")).isEqualTo("POSTPARTUM");
            }

            assertThat(singleLong(statement, """
                    SELECT count(*) FROM public.community_interactions
                    WHERE topic_id = '%s'
                    """.formatted(BABY_CARE_CATEGORY_ID))).isZero();
            assertThat(singleLong(statement, """
                    SELECT count(*) FROM public.community_interactions
                    WHERE actor_user_id = '%s'
                      AND interaction_type = 'FOLLOW'
                      AND topic_id = '%s'
                    """.formatted(userWithDuplicateFollows, POSTPARTUM_CATEGORY_ID)))
                    .isEqualTo(1);
            assertThat(singleLong(statement, """
                    SELECT count(*) FROM public.community_interactions
                    WHERE actor_user_id = '%s'
                      AND interaction_type = 'FOLLOW'
                      AND topic_id = '%s'
                    """.formatted(userFollowingOnlyBabyCare, POSTPARTUM_CATEGORY_ID)))
                    .isEqualTo(1);
        }
    }

    private long singleLong(java.sql.Statement statement, String sql) throws Exception {
        try (var result = statement.executeQuery(sql)) {
            assertThat(result.next()).isTrue();
            return result.getLong(1);
        }
    }

    private UUID singleUuid(java.sql.Statement statement, String sql) throws Exception {
        try (var result = statement.executeQuery(sql)) {
            assertThat(result.next()).isTrue();
            return result.getObject(1, UUID.class);
        }
    }

    private java.sql.Connection connection() throws Exception {
        return DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    }

    private Flyway flyway(MigrationVersion target) {
        var configuration = Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration");
        if (target != null) {
            configuration.target(target);
        }
        return configuration.load();
    }
}
