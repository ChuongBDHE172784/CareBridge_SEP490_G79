package com.carebridge.backend.recommendation;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.content.entity.ContentItem;
import com.carebridge.backend.content.repository.ContentRepository;
import com.carebridge.backend.testsupport.AbstractEmbeddedPostgresIntegrationTest;
import jakarta.persistence.EntityManager;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** Regression coverage for the native PostgreSQL recommendation pool queries. */
@EnabledOnOs(OS.WINDOWS)
@Transactional
class RecommendationContentRepositoryEmbeddedPostgresTest extends AbstractEmbeddedPostgresIntegrationTest {

    private static final Instant PUBLISHED_NEW = Instant.parse("2026-01-02T00:00:00Z");
    private static final Instant PUBLISHED_OLD = Instant.parse("2026-01-01T00:00:00Z");

    @Autowired
    private ContentRepository contentRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void isolateFromReferenceContent() {
        // The baseline migration may contain approved articles.  Archive them inside
        // the test-managed transaction so each test starts with an empty recommendation pool
        // and the rollback restores the user's database state.
        jdbcTemplate.update("""
                UPDATE content_items
                   SET status = 'ARCHIVED'
                 WHERE content_type = 'ARTICLE'
                   AND status = 'APPROVED'
                """);
        entityManager.clear();
    }

    @Test
    void fallbackQueryReturnsEmptyThenOnlyEligibleUnsignaledArticles() {
        assertThat(fallback("PRE_PREGNANCY", null, 20)).isEmpty();

        UUID eligible = article("eligible-fallback", "PRE_PREGNANCY", "APPROVED", null, null, 5, PUBLISHED_NEW);
        UUID tagged = article("tagged-fallback", "PRE_PREGNANCY", "APPROVED", null, null, 5, PUBLISHED_NEW);
        UUID draft = article("draft-fallback", "PRE_PREGNANCY", "DRAFT", null, null, 5, PUBLISHED_NEW);
        UUID faq = content("faq-fallback", "PRE_PREGNANCY", "FAQ", "APPROVED", null, null, 0, PUBLISHED_NEW);
        article("wrong-stage", "POSTPARTUM", "APPROVED", null, null, 5, PUBLISHED_NEW);

        UUID recTag = topic("rec-test-fallback", false);
        link(tagged, recTag);
        entityManager.clear();

        List<ContentItem> result = fallback("PRE_PREGNANCY", null, 20);
        assertThat(ids(result)).containsExactly(eligible);
        assertThat(ids(result)).doesNotContainAnyElementsOf(List.of(draft, faq));
    }

    @Test
    void fallbackQueryUsesInclusivePregnancyWeekBoundaries() {
        UUID universal = article("pregnancy-universal", "PREGNANCY", "APPROVED", null, null, 1, PUBLISHED_OLD);
        UUID weekZero = article("pregnancy-week-zero", "PREGNANCY", "APPROVED", 0, 0, 1, PUBLISHED_OLD);
        UUID weekFour = article("pregnancy-week-four", "PREGNANCY", "APPROVED", 4, 4, 1, PUBLISHED_OLD);
        UUID weekFortyTwo = article("pregnancy-week-forty-two", "PREGNANCY", "APPROVED", 42, 42, 1, PUBLISHED_OLD);
        UUID beforeWindow = article("pregnancy-before-window", "PREGNANCY", "APPROVED", 1, 3, 1, PUBLISHED_OLD);
        UUID afterWindow = article("pregnancy-after-window", "PREGNANCY", "APPROVED", 5, 6, 1, PUBLISHED_OLD);
        entityManager.clear();

        assertThat(ids(fallback("PREGNANCY", null, 20))).containsExactly(universal);
        assertThat(ids(fallback("PREGNANCY", 0, 20))).containsExactly(weekZero, universal);
        assertThat(ids(fallback("PREGNANCY", 4, 20))).containsExactly(weekFour, universal);
        assertThat(ids(fallback("PREGNANCY", 42, 20))).containsExactly(weekFortyTwo, universal);
        assertThat(ids(fallback("PREGNANCY", 4, 20))).doesNotContainAnyElementsOf(List.of(beforeWindow, afterWindow));
    }

    @Test
    void targetedQueryRanksMatchedSignalsAndReturnsMultiTaggedArticleOnce() {
        UUID firstTag = topic("rec-test-target-a", false);
        UUID secondTag = topic("rec-test-target-b", false);
        UUID hiddenTag = topic("rec-test-target-hidden", true);
        UUID unselectedRecTag = topic("rec-test-target-unselected", false);
        UUID unrelatedTag = topic("topic-test-unrelated", false);

        UUID multiTagged = article("multi-tagged", "PREGNANCY", "APPROVED", null, null, 1, PUBLISHED_OLD);
        UUID singleTagged = article("single-tagged", "PREGNANCY", "APPROVED", null, null, 1, PUBLISHED_OLD);
        UUID hiddenOnly = article("hidden-tagged", "PREGNANCY", "APPROVED", null, null, 1, PUBLISHED_OLD);
        UUID unselectedRecOnly = article("unselected-rec-tagged", "PREGNANCY", "APPROVED", null, null, 1, PUBLISHED_OLD);
        UUID unrelatedOnly = article("unrelated-tagged", "PREGNANCY", "APPROVED", null, null, 1, PUBLISHED_OLD);
        link(multiTagged, firstTag);
        link(multiTagged, secondTag);
        link(singleTagged, firstTag);
        link(hiddenOnly, hiddenTag);
        link(unselectedRecOnly, unselectedRecTag);
        link(unrelatedOnly, unrelatedTag);
        entityManager.clear();

        List<ContentItem> result = contentRepository.findApprovedTargetedArticlesForRecommendation(
                "PREGNANCY", 4, List.of("rec-test-target-a", "rec-test-target-b", "rec-test-target-hidden"),
                PageRequest.of(0, 20));

        assertThat(ids(result)).containsExactly(multiTagged, singleTagged);
        assertThat(ids(result)).doesNotHaveDuplicates();
        assertThat(ids(result)).doesNotContainAnyElementsOf(List.of(hiddenOnly, unselectedRecOnly, unrelatedOnly));
    }

    @Test
    void targetedQueryPreservesPrioritySpecificityWidthPublicationAndUuidOrder() {
        UUID tag = topic("rec-test-order", false);
        UUID highPriority = content("60000000-0000-0000-0000-000000000001", "PREGNANCY", "ARTICLE", "APPROVED", null, null, 10, PUBLISHED_OLD);
        UUID exactWeek = content("60000000-0000-0000-0000-000000000002", "PREGNANCY", "ARTICLE", "APPROVED", 4, 4, 5, PUBLISHED_OLD);
        UUID narrowWindow = content("60000000-0000-0000-0000-000000000003", "PREGNANCY", "ARTICLE", "APPROVED", 3, 5, 5, PUBLISHED_OLD);
        UUID broadWindow = content("60000000-0000-0000-0000-000000000004", "PREGNANCY", "ARTICLE", "APPROVED", 1, 10, 5, PUBLISHED_OLD);
        UUID publishedNew = content("60000000-0000-0000-0000-000000000005", "PREGNANCY", "ARTICLE", "APPROVED", null, null, 5, PUBLISHED_NEW);
        UUID publishedOldLow = content("60000000-0000-0000-0000-000000000006", "PREGNANCY", "ARTICLE", "APPROVED", null, null, 5, PUBLISHED_OLD);
        UUID publishedOldHigh = content("60000000-0000-0000-0000-000000000007", "PREGNANCY", "ARTICLE", "APPROVED", null, null, 5, PUBLISHED_OLD);
        UUID unpublished = content("60000000-0000-0000-0000-000000000008", "PREGNANCY", "ARTICLE", "APPROVED", null, null, 5, null);
        for (UUID id : List.of(highPriority, exactWeek, narrowWindow, broadWindow, publishedNew,
                publishedOldLow, publishedOldHigh, unpublished)) {
            link(id, tag);
        }
        entityManager.clear();

        List<UUID> expected = List.of(highPriority, exactWeek, narrowWindow, broadWindow,
                publishedNew, publishedOldLow, publishedOldHigh, unpublished);
        List<UUID> first = ids(contentRepository.findApprovedTargetedArticlesForRecommendation(
                "PREGNANCY", 4, List.of("rec-test-order"), PageRequest.of(0, 20)));
        List<UUID> repeated = ids(contentRepository.findApprovedTargetedArticlesForRecommendation(
                "PREGNANCY", 4, List.of("rec-test-order"), PageRequest.of(0, 20)));
        assertThat(first).containsExactlyElementsOf(expected);
        assertThat(repeated).containsExactlyElementsOf(expected);
        assertThat(first).doesNotHaveDuplicates();

        List<UUID> firstPage = ids(contentRepository.findApprovedTargetedArticlesForRecommendation(
                "PREGNANCY", 4, List.of("rec-test-order"), PageRequest.of(0, 4)));
        List<UUID> secondPage = ids(contentRepository.findApprovedTargetedArticlesForRecommendation(
                "PREGNANCY", 4, List.of("rec-test-order"), PageRequest.of(1, 4)));
        assertThat(firstPage).containsExactlyElementsOf(expected.subList(0, 4));
        assertThat(secondPage).containsExactlyElementsOf(expected.subList(4, 8));
        assertThat(firstPage).doesNotContainAnyElementsOf(secondPage);
    }

    @Test
    void fallbackQueryKeepsDeterministicOrderAcrossBoundedPages() {
        UUID highPriority = content("10000000-0000-0000-0000-000000000001", "PREGNANCY", "ARTICLE", "APPROVED", null, null, 10, PUBLISHED_OLD);
        UUID exactWeek = content("20000000-0000-0000-0000-000000000001", "PREGNANCY", "ARTICLE", "APPROVED", 4, 4, 5, PUBLISHED_OLD);
        UUID narrowWindow = content("30000000-0000-0000-0000-000000000001", "PREGNANCY", "ARTICLE", "APPROVED", 3, 5, 5, PUBLISHED_OLD);
        UUID broadWindow = content("40000000-0000-0000-0000-000000000001", "PREGNANCY", "ARTICLE", "APPROVED", 1, 10, 5, PUBLISHED_OLD);
        UUID publishedNew = content("50000000-0000-0000-0000-000000000001", "PREGNANCY", "ARTICLE", "APPROVED", null, null, 5, PUBLISHED_NEW);
        UUID publishedOld = content("50000000-0000-0000-0000-000000000002", "PREGNANCY", "ARTICLE", "APPROVED", null, null, 5, PUBLISHED_OLD);
        UUID uuidLow = content("50000000-0000-0000-0000-000000000003", "PREGNANCY", "ARTICLE", "APPROVED", null, null, 5, PUBLISHED_OLD);
        UUID uuidHigh = content("50000000-0000-0000-0000-000000000004", "PREGNANCY", "ARTICLE", "APPROVED", null, null, 5, PUBLISHED_OLD);
        UUID unpublished = content("50000000-0000-0000-0000-000000000005", "PREGNANCY", "ARTICLE", "APPROVED", null, null, 5, null);
        entityManager.clear();

        List<UUID> expected = List.of(highPriority, exactWeek, narrowWindow, broadWindow,
                publishedNew, publishedOld, uuidLow, uuidHigh, unpublished);
        assertThat(ids(fallback("PREGNANCY", 4, 20))).containsExactlyElementsOf(expected);
        assertThat(ids(fallback("PREGNANCY", 4, 20))).containsExactlyElementsOf(expected);

        List<UUID> pageZero = ids(fallback("PREGNANCY", 4, 2));
        List<UUID> pageOne = ids(contentRepository.findApprovedFallbackArticlesForRecommendation(
                "PREGNANCY", 4, PageRequest.of(1, 2)));
        assertThat(pageZero).containsExactly(highPriority, exactWeek);
        assertThat(pageOne).containsExactly(narrowWindow, broadWindow);
        assertThat(pageZero).doesNotContainAnyElementsOf(pageOne);
        assertThat(List.of(pageZero, pageOne).stream().flatMap(Collection::stream).collect(Collectors.toList()))
                .containsExactlyElementsOf(expected.subList(0, 4));
    }

    private List<ContentItem> fallback(String stage, Integer week, int pageSize) {
        entityManager.clear();
        return contentRepository.findApprovedFallbackArticlesForRecommendation(
                stage, week, PageRequest.of(0, pageSize));
    }

    private UUID article(String title, String stage, String status, Integer fromWeek, Integer toWeek,
                         int priority, Instant publishedAt) {
        return content(title, stage, "ARTICLE", status, fromWeek, toWeek, priority, publishedAt);
    }

    private UUID content(String titleOrId, String stage, String type, String status,
                         Integer fromWeek, Integer toWeek, int priority, Instant publishedAt) {
        UUID id;
        try {
            id = UUID.fromString(titleOrId);
        } catch (IllegalArgumentException ignored) {
            id = UUID.nameUUIDFromBytes((getClass().getName() + ":" + titleOrId).getBytes(StandardCharsets.UTF_8));
        }
        jdbcTemplate.update("""
                INSERT INTO content_items (
                    content_item_id, created_at, status, title, body, summary,
                    content_type, stage, eligible_from_week, eligible_to_week,
                    recommendation_priority, published_at, lock_version
                ) VALUES (?, now(), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
                """, id, status, titleOrId, "Test body", "Test summary", type, stage,
                fromWeek, toWeek, (short) priority,
                publishedAt == null ? null : Timestamp.from(publishedAt));
        return id;
    }

    private UUID topic(String slug, boolean hidden) {
        UUID id = UUID.nameUUIDFromBytes((getClass().getName() + ":topic:" + slug).getBytes(StandardCharsets.UTF_8));
        jdbcTemplate.update("""
                INSERT INTO community_topics (
                    id, created_at, name, updated_at, is_hidden, sort_order, type, slug
                ) VALUES (?, now(), ?, now(), ?, 0, 'TAG', ?)
                """, id, slug, hidden, slug);
        return id;
    }

    private void link(UUID contentId, UUID topicId) {
        jdbcTemplate.update("""
                INSERT INTO content_item_topics (content_item_id, topic_id)
                VALUES (?, ?)
                """, contentId, topicId);
    }

    private List<UUID> ids(List<ContentItem> content) {
        return content.stream().map(ContentItem::getId).toList();
    }
}
