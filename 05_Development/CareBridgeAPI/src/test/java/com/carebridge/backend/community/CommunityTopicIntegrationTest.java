package com.carebridge.backend.community;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.carebridge.backend.community.entity.CommunityQuestion;
import com.carebridge.backend.community.entity.CommunityTopic;
import com.carebridge.backend.community.entity.QuestionStatus;
import com.carebridge.backend.community.entity.TopicType;
import com.carebridge.backend.community.repository.CommunityQuestionRepository;
import com.carebridge.backend.community.repository.CommunityTopicRepository;
import com.carebridge.backend.community.repository.TopicQuestionCountProjection;
import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
import com.carebridge.backend.testsupport.CanonicalUserFixture;
import java.util.List;
import java.util.UUID;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * COM-TC-INT-001 / COM-TC-INT-002 — full stack against a real Testcontainers PostgreSQL,
 * verifying the V20260721204919__add_community_topic_taxonomy.sql migration and the
 * APPROVED-only question-count aggregation (ADR-COM-015 / ADR-COM-016).
 */
@Transactional
class CommunityTopicIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private CommunityTopicRepository topicRepository;
    @Autowired private CommunityQuestionRepository questionRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    // COM-TC-INT-001: questionCount aggregation counts only APPROVED, against real DB rows
    @Test
    void countApprovedQuestionsByTopicIds_mixedStatuses_countsOnlyApproved() {
        CommunityTopic category = topicRepository.save(CommunityTopic.builder()
                .name("INT Category " + UUID.randomUUID())
                .type(TopicType.CATEGORY)
                .slug("int-category-" + UUID.randomUUID())
                .isHidden(false)
                .sortOrder(1)
                .build());
        CommunityTopic topic = topicRepository.save(CommunityTopic.builder()
                .name("INT Test Topic " + UUID.randomUUID())
                .type(TopicType.TOPIC)
                .slug("int-test-topic-" + UUID.randomUUID())
                .parentId(category.getId())
                .isHidden(false)
                .sortOrder(1)
                .build());

        questionRepository.save(makeQuestion(topic.getId(), QuestionStatus.APPROVED));
        questionRepository.save(makeQuestion(topic.getId(), QuestionStatus.APPROVED));
        questionRepository.save(makeQuestion(topic.getId(), QuestionStatus.APPROVED));
        questionRepository.save(makeQuestion(topic.getId(), QuestionStatus.PENDING));
        questionRepository.save(makeQuestion(topic.getId(), QuestionStatus.HIDDEN));

        List<TopicQuestionCountProjection> counts =
                questionRepository.countApprovedQuestionsByTopicIds(List.of(topic.getId()));

        assertThat(counts).hasSize(1);
        assertThat(counts.get(0).getTopicId()).isEqualTo(topic.getId());
        assertThat(counts.get(0).getCnt()).isEqualTo(3L);
    }

    // COM-TC-INT-002: DB CHECK accepts TOPIC child and rejects TOPIC root.
    @Test
    void insertTopicTypeWithCategoryParent_bypassingService_isAccepted() {
        UUID categoryId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO community_topics (id, name, type, slug, is_hidden, sort_order, created_at) "
                        + "VALUES (?, 'INT Parent Category', 'CATEGORY', ?, false, 1, now())",
                categoryId, "int-parent-category-" + categoryId);

        int inserted = jdbcTemplate.update(
                "INSERT INTO community_topics (id, name, type, slug, parent_id, is_hidden, sort_order, created_at) "
                        + "VALUES (gen_random_uuid(), 'Valid Child Topic', 'TOPIC', 'valid-topic-' || gen_random_uuid()::text, ?, false, 0, now())",
                categoryId);

        assertThat(inserted).isEqualTo(1);
    }

    @Test
    void insertTopicTypeWithNullParent_bypassingService_isRejectedByCheckConstraint() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO community_topics (id, name, type, slug, parent_id, is_hidden, sort_order, created_at) "
                        + "VALUES (gen_random_uuid(), 'Bad Root Topic', 'TOPIC', 'bad-root-topic-' || gen_random_uuid()::text, NULL, false, 0, now())"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // COM-TC-INT-002: CATEGORY remains a root.
    @Test
    void insertCategoryTypeWithNullParentId_bypassingService_isAccepted() {
        jdbcTemplate.update(
                "INSERT INTO community_topics (id, name, type, slug, parent_id, is_hidden, sort_order, created_at) "
                        + "VALUES (gen_random_uuid(), 'Standalone Category', 'CATEGORY', 'standalone-category-' || gen_random_uuid()::text, NULL, false, 0, now())");

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM community_topics WHERE name = 'Standalone Category'", Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void insertCategoryTypeWithParentId_bypassingService_isRejectedByCheckConstraint() {
        CommunityTopic category = topicRepository.save(CommunityTopic.builder()
                .name("INT Root Category " + UUID.randomUUID())
                .type(TopicType.CATEGORY)
                .slug("int-root-category-" + UUID.randomUUID())
                .isHidden(false)
                .sortOrder(1)
                .build());

        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO community_topics (id, name, type, slug, parent_id, is_hidden, sort_order, created_at) "
                        + "VALUES (gen_random_uuid(), 'Bad Child Category', 'CATEGORY', 'bad-child-category-' || gen_random_uuid()::text, ?, false, 0, now())",
                category.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // COM-TC-026 + COM-TC-038: run the real migration boundary and prove follow ids survive.
    // The historical taxonomy/inversion migrations were consolidated into the single canonical
    // convergence migration, so the pre-migration state is now built as a synthetic legacy
    // database (inverted hierarchy + FOLLOW rows already in community_interactions), and the
    // one canonical migration must preserve both the existing topic ids and the follow rows.
    @Test
    void hierarchyInversion_preservesFollowRowsAndExistingTopicIds() throws Exception {
        String database = "community_topic_v2_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection admin = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             Statement statement = admin.createStatement()) {
            statement.execute("CREATE DATABASE " + database);
        }
        String url = POSTGRES.getJdbcUrl().replace("/test?", "/" + database + "?");

        List<String> beforeTopicIds;
        try (Connection connection = DriverManager.getConnection(url, POSTGRES.getUsername(), POSTGRES.getPassword());
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE users (
                        user_id uuid PRIMARY KEY,
                        person_id uuid NOT NULL,
                        created_at timestamptz NOT NULL,
                        updated_at timestamptz NOT NULL
                    )
                    """);
            statement.execute("""
                    INSERT INTO users(user_id, person_id, created_at, updated_at)
                    VALUES ('00000000-0000-0000-0000-000000009999',
                            '00000000-0000-0000-0000-000000009999', now(), now())
                    """);
            // Same column ORDER as the canonical relation: the convergence migration
            // re-seeds the taxonomy with positional INSERT ... VALUES statements.
            statement.execute("""
                    CREATE TABLE community_topics (
                        id uuid PRIMARY KEY,
                        created_at timestamptz NOT NULL DEFAULT now(),
                        description text,
                        name varchar(100) NOT NULL,
                        updated_at timestamptz DEFAULT now(),
                        is_hidden boolean NOT NULL DEFAULT false,
                        icon varchar(255),
                        sort_order integer NOT NULL DEFAULT 0,
                        created_by uuid,
                        type varchar(20) NOT NULL,
                        slug varchar(140) NOT NULL UNIQUE,
                        parent_id uuid REFERENCES community_topics(id)
                    )
                    """);
            // Existing (already inverted) taxonomy with the production topic ids. The
            // canonical migration re-seeds the same slugs with ON CONFLICT DO NOTHING,
            // so these pre-existing ids must survive unchanged.
            statement.execute("""
                    INSERT INTO community_topics(id, name, type, slug, parent_id, sort_order) VALUES
                    ('b1b2c3d4-e5f6-7890-abcd-ef1234567801', 'Chuẩn bị mang thai', 'CATEGORY', 'chuan-bi-mang-thai', NULL, 1),
                    ('b1b2c3d4-e5f6-7890-abcd-ef1234567802', 'Mang thai', 'CATEGORY', 'mang-thai', NULL, 2),
                    ('b1b2c3d4-e5f6-7890-abcd-ef1234567803', 'Sau sinh', 'CATEGORY', 'sau-sinh', NULL, 3),
                    ('b1b2c3d4-e5f6-7890-abcd-ef1234567804', 'Chăm bé', 'CATEGORY', 'cham-be', NULL, 4),
                    ('b1b2c3d4-e5f6-7890-abcd-ef1234567805', 'Khác', 'CATEGORY', 'khac', NULL, 5),
                    ('a1b2c3d4-e5f6-7890-abcd-ef1234567801', 'Dinh dưỡng thai kỳ', 'TOPIC', 'dinh-duong-thai-ky', 'b1b2c3d4-e5f6-7890-abcd-ef1234567802', 1),
                    ('a1b2c3d4-e5f6-7890-abcd-ef1234567802', 'Sức khỏe thai nhi', 'TOPIC', 'suc-khoe-thai-nhi', 'b1b2c3d4-e5f6-7890-abcd-ef1234567802', 2),
                    ('a1b2c3d4-e5f6-7890-abcd-ef1234567803', 'Chăm sóc sau sinh', 'TOPIC', 'cham-soc-sau-sinh', 'b1b2c3d4-e5f6-7890-abcd-ef1234567803', 3),
                    ('a1b2c3d4-e5f6-7890-abcd-ef1234567804', 'Nuôi con bằng sữa mẹ', 'TOPIC', 'nuoi-con-bang-sua-me', 'b1b2c3d4-e5f6-7890-abcd-ef1234567803', 4),
                    ('a1b2c3d4-e5f6-7890-abcd-ef1234567805', 'Giấc ngủ và thể chất', 'TOPIC', 'giac-ngu-va-the-chat', 'b1b2c3d4-e5f6-7890-abcd-ef1234567802', 5),
                    ('a1b2c3d4-e5f6-7890-abcd-ef1234567806', 'Tâm lý & Cảm xúc', 'TOPIC', 'tam-ly-va-cam-xuc', 'b1b2c3d4-e5f6-7890-abcd-ef1234567805', 6),
                    ('a1b2c3d4-e5f6-7890-abcd-ef1234567807', 'Chăm sóc bé sơ sinh', 'TOPIC', 'cham-soc-be-so-sinh', 'b1b2c3d4-e5f6-7890-abcd-ef1234567804', 7),
                    ('a1b2c3d4-e5f6-7890-abcd-ef1234567808', 'Hỏi đáp chung', 'TOPIC', 'hoi-dap-chung', 'b1b2c3d4-e5f6-7890-abcd-ef1234567805', 8)
                    """);
            statement.execute("""
                    CREATE TABLE community_interactions (
                        actor_user_id uuid NOT NULL,
                        interaction_type varchar(30) NOT NULL,
                        topic_id uuid,
                        created_at timestamptz NOT NULL DEFAULT now()
                    )
                    """);
            statement.execute("""
                    INSERT INTO community_interactions(actor_user_id, interaction_type, topic_id) VALUES
                    ('00000000-0000-0000-0000-000000009999', 'FOLLOW', 'a1b2c3d4-e5f6-7890-abcd-ef1234567801'),
                    ('00000000-0000-0000-0000-000000009999', 'FOLLOW', 'a1b2c3d4-e5f6-7890-abcd-ef1234567807')
                    """);
            beforeTopicIds = queryStrings(statement,
                    "SELECT topic_id::text FROM community_interactions "
                            + "WHERE interaction_type='FOLLOW' ORDER BY topic_id");
        }

        Flyway.configure()
                .dataSource(url, POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                // The synthetic legacy schema pre-dates Flyway history, exactly like
                // the real databases the convergence migration was applied to.
                .baselineOnMigrate(true)
                .baselineVersion(MigrationVersion.fromVersion("0"))
                .target(MigrationVersion.fromVersion("20260727010000"))
                .outOfOrder(true)
                .load()
                .migrate();

        try (Connection connection = DriverManager.getConnection(url, POSTGRES.getUsername(), POSTGRES.getPassword());
             Statement statement = connection.createStatement()) {
            assertThat(queryStrings(statement,
                    "SELECT topic_id::text FROM community_interactions "
                            + "WHERE interaction_type='FOLLOW' ORDER BY topic_id"))
                    .containsExactlyElementsOf(beforeTopicIds);
            assertThat(singleLong(statement,
                    "SELECT COUNT(*) FROM community_topics WHERE type='CATEGORY' AND parent_id IS NULL"))
                    .isEqualTo(5L);
            assertThat(singleLong(statement,
                    "SELECT COUNT(*) FROM community_topics WHERE type='TOPIC' AND parent_id IS NOT NULL"))
                    .isEqualTo(8L);
            assertThat(queryStrings(statement, """
                    SELECT t.id::text || '=' || c.name
                    FROM community_topics t
                    JOIN community_topics c ON c.id = t.parent_id
                    WHERE t.id IN (
                        'a1b2c3d4-e5f6-7890-abcd-ef1234567801',
                        'a1b2c3d4-e5f6-7890-abcd-ef1234567802',
                        'a1b2c3d4-e5f6-7890-abcd-ef1234567803',
                        'a1b2c3d4-e5f6-7890-abcd-ef1234567804',
                        'a1b2c3d4-e5f6-7890-abcd-ef1234567805',
                        'a1b2c3d4-e5f6-7890-abcd-ef1234567806',
                        'a1b2c3d4-e5f6-7890-abcd-ef1234567807',
                        'a1b2c3d4-e5f6-7890-abcd-ef1234567808'
                    )
                    ORDER BY t.id
                    """))
                    .containsExactly(
                            "a1b2c3d4-e5f6-7890-abcd-ef1234567801=Mang thai",
                            "a1b2c3d4-e5f6-7890-abcd-ef1234567802=Mang thai",
                            "a1b2c3d4-e5f6-7890-abcd-ef1234567803=Sau sinh",
                            "a1b2c3d4-e5f6-7890-abcd-ef1234567804=Sau sinh",
                            "a1b2c3d4-e5f6-7890-abcd-ef1234567805=Mang thai",
                            "a1b2c3d4-e5f6-7890-abcd-ef1234567806=Khác",
                            "a1b2c3d4-e5f6-7890-abcd-ef1234567807=Chăm bé",
                            "a1b2c3d4-e5f6-7890-abcd-ef1234567808=Khác");
        }
    }

    private List<String> queryStrings(Statement statement, String sql) throws Exception {
        try (ResultSet resultSet = statement.executeQuery(sql)) {
            java.util.ArrayList<String> values = new java.util.ArrayList<>();
            while (resultSet.next()) {
                values.add(resultSet.getString(1));
            }
            return values;
        }
    }

    private long singleLong(Statement statement, String sql) throws Exception {
        try (ResultSet resultSet = statement.executeQuery(sql)) {
            assertThat(resultSet.next()).isTrue();
            return resultSet.getLong(1);
        }
    }

    private CommunityQuestion makeQuestion(UUID topicId, QuestionStatus status) {
        UUID authorId = UUID.randomUUID();
        CanonicalUserFixture.insertUser(
                jdbcTemplate, authorId, "Community test author", null, "MOTHER");
        return CommunityQuestion.builder()
                .topicId(topicId)
                .authorId(authorId)
                .title("INT question")
                .body("INT body")
                .stage(com.carebridge.backend.community.entity.PregnancyStage.PREGNANCY)
                .urgency(com.carebridge.backend.community.entity.UrgencyLevel.NORMAL)
                .status(status)
                .build();
    }
}
