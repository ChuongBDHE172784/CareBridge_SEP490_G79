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
import java.util.List;
import java.util.UUID;
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
        CommunityTopic topic = topicRepository.save(CommunityTopic.builder()
                .name("INT Test Topic " + UUID.randomUUID())
                .type(TopicType.TOPIC)
                .slug("int-test-topic-" + UUID.randomUUID())
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

    // COM-TC-INT-002: DB CHECK constraint blocks a TOPIC with a parent, even bypassing the service
    @Test
    void insertTopicTypeWithParentId_bypassingService_isRejectedByCheckConstraint() {
        CommunityTopic parentCandidate = topicRepository.save(CommunityTopic.builder()
                .name("INT Parent Candidate " + UUID.randomUUID())
                .type(TopicType.TOPIC)
                .slug("int-parent-candidate-" + UUID.randomUUID())
                .isHidden(false)
                .sortOrder(1)
                .build());

        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO community_topics (id, name, type, slug, parent_id, is_hidden, sort_order, created_at) "
                        + "VALUES (gen_random_uuid(), 'Bad Topic', 'TOPIC', 'bad-topic-' || gen_random_uuid()::text, ?, false, 0, now())",
                parentCandidate.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // COM-TC-INT-002 sanity check: a CATEGORY with no parent is NOT rejected (ADR-COM-016 revised —
    // parentId is optional for CATEGORY/TAG so ContentCategoryController keeps working).
    @Test
    void insertCategoryTypeWithNullParentId_bypassingService_isAccepted() {
        jdbcTemplate.update(
                "INSERT INTO community_topics (id, name, type, slug, parent_id, is_hidden, sort_order, created_at) "
                        + "VALUES (gen_random_uuid(), 'Standalone Category', 'CATEGORY', 'standalone-category-' || gen_random_uuid()::text, NULL, false, 0, now())");

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM community_topics WHERE name = 'Standalone Category'", Integer.class);
        assertThat(count).isEqualTo(1);
    }

    private CommunityQuestion makeQuestion(UUID topicId, QuestionStatus status) {
        return CommunityQuestion.builder()
                .topicId(topicId)
                .authorId(UUID.randomUUID())
                .title("INT question")
                .body("INT body")
                .stage(com.carebridge.backend.community.entity.PregnancyStage.PREGNANCY)
                .urgency(com.carebridge.backend.community.entity.UrgencyLevel.NORMAL)
                .status(status)
                .build();
    }
}
