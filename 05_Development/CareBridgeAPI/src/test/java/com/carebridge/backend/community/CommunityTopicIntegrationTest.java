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
    @Test
    void hierarchyInversion_preservesFollowRowsAndExistingTopicIds() throws Exception {
        String database = "community_topic_v2_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection admin = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             Statement statement = admin.createStatement()) {
            statement.execute("CREATE DATABASE " + database);
        }
        String url = POSTGRES.getJdbcUrl().replace("/test?", "/" + database + "?");
        Flyway.configure()
                .dataSource(url, POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .target(MigrationVersion.fromVersion("20260721204919"))
                .outOfOrder(true)
                .load()
                .migrate();

        List<String> beforeTopicIds;
        try (Connection connection = DriverManager.getConnection(url, POSTGRES.getUsername(), POSTGRES.getPassword());
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    INSERT INTO users(user_id, email, password_hash, full_name, created_at, updated_at)
                    VALUES ('00000000-0000-0000-0000-000000009999', 'follow-migration@test.local', 'x', 'Follow Test', now(), now())
                    """);
            statement.execute("""
                    INSERT INTO user_topic_follows(user_id, topic_id) VALUES
                    ('00000000-0000-0000-0000-000000009999', 'a1b2c3d4-e5f6-7890-abcd-ef1234567801'),
                    ('00000000-0000-0000-0000-000000009999', 'a1b2c3d4-e5f6-7890-abcd-ef1234567807')
                    """);
            beforeTopicIds = queryStrings(statement,
                    "SELECT topic_id::text FROM user_topic_follows ORDER BY topic_id");
        }

        Flyway.configure()
                .dataSource(url, POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
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
