package com.carebridge.backend.content.repository;

import com.carebridge.backend.content.entity.ContentItem;
import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.entity.ContentStatus;
import com.carebridge.backend.content.entity.ContentType;
import java.util.Optional;
import java.util.List;
import java.util.Collection;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ContentRepository extends JpaRepository<ContentItem, UUID> {

    /** Targeted pool prefilter reusing the existing content/topic join table. */
    @Query(value = """
            SELECT DISTINCT c.*
              FROM content_items c
             WHERE c.stage = :stage
               AND c.content_type = 'ARTICLE'
               AND c.status = 'APPROVED'
               AND (
                    (:pregnancyWeek IS NULL
                     AND c.eligible_from_week IS NULL
                     AND c.eligible_to_week IS NULL)
                    OR (:pregnancyWeek IS NOT NULL AND
                        ((c.eligible_from_week IS NULL AND c.eligible_to_week IS NULL)
                         OR (c.eligible_from_week <= :pregnancyWeek
                             AND c.eligible_to_week >= :pregnancyWeek)))
               )
               AND EXISTS (
                   SELECT 1
                     FROM content_item_topics cit
                     JOIN community_topics ct ON ct.id = cit.topic_id
                    WHERE cit.content_item_id = c.content_item_id
                      AND ct.type = 'TAG'
                      AND ct.is_hidden = false
                      AND ct.slug IN (:tagSlugs)
               )
             ORDER BY c.recommendation_priority DESC,
                      (SELECT COUNT(DISTINCT matched_cit.topic_id)
                         FROM content_item_topics matched_cit
                         JOIN community_topics matched_ct ON matched_ct.id = matched_cit.topic_id
                        WHERE matched_cit.content_item_id = c.content_item_id
                          AND matched_ct.type = 'TAG'
                          AND matched_ct.is_hidden = false
                          AND matched_ct.slug IN (:tagSlugs)) DESC,
                      CASE WHEN c.eligible_from_week IS NULL AND c.eligible_to_week IS NULL THEN 1 ELSE 0 END ASC,
                      COALESCE(c.eligible_to_week - c.eligible_from_week + 1, 43) ASC,
                      c.published_at DESC NULLS LAST,
                      c.content_item_id ASC
            """, nativeQuery = true)
    List<ContentItem> findApprovedTargetedArticlesForRecommendation(
            @Param("stage") String stage,
            @Param("pregnancyWeek") Integer pregnancyWeek,
            @Param("tagSlugs") Collection<String> tagSlugs,
            Pageable pageable);

    /** Fallback pool prefilter excluding every rec-* association. */
    @Query(value = """
            SELECT DISTINCT c.*
              FROM content_items c
             WHERE c.stage = :stage
               AND c.content_type = 'ARTICLE'
               AND c.status = 'APPROVED'
               AND (
                    (:pregnancyWeek IS NULL
                     AND c.eligible_from_week IS NULL
                     AND c.eligible_to_week IS NULL)
                    OR (:pregnancyWeek IS NOT NULL AND
                        ((c.eligible_from_week IS NULL AND c.eligible_to_week IS NULL)
                         OR (c.eligible_from_week <= :pregnancyWeek
                             AND c.eligible_to_week >= :pregnancyWeek)))
               )
               AND NOT EXISTS (
                   SELECT 1
                     FROM content_item_topics cit
                     JOIN community_topics ct ON ct.id = cit.topic_id
                    WHERE cit.content_item_id = c.content_item_id
                      AND ct.type = 'TAG'
                      AND ct.slug LIKE 'rec-%'
               )
             ORDER BY c.recommendation_priority DESC,
                      CASE WHEN c.eligible_from_week IS NULL AND c.eligible_to_week IS NULL THEN 1 ELSE 0 END ASC,
                      COALESCE(c.eligible_to_week - c.eligible_from_week + 1, 43) ASC,
                      c.published_at DESC NULLS LAST,
                      c.content_item_id ASC
            """, nativeQuery = true)
    List<ContentItem> findApprovedFallbackArticlesForRecommendation(
            @Param("stage") String stage,
            @Param("pregnancyWeek") Integer pregnancyWeek,
            Pageable pageable);

    /**
     * Reads recommendation tag links in one JPQL join instead of triggering a
     * lazy ElementCollection query for every candidate entity.
     */
    @Query("select c.id, tag from ContentItem c join c.tagIds tag where c.id in :contentItemIds")
    List<Object[]> findRecommendationTagRows(@Param("contentItemIds") Collection<UUID> contentItemIds);

    Optional<ContentItem> findByTitleIgnoreCaseAndStageAndType(
            String title, ContentStage stage, ContentType type);

    @Query("SELECT c FROM ContentItem c WHERE " +
           "(:type IS NULL OR c.type = :type) AND " +
           "(:stage IS NULL OR c.stage = :stage) AND " +
           "(:topicId IS NULL OR c.topicId = :topicId) AND " +
           "c.status = :status " +
           "ORDER BY c.publishedAt DESC NULLS LAST, c.id DESC")
    Page<ContentItem> findByFilters(
            @Param("type") ContentType type,
            @Param("stage") ContentStage stage,
            @Param("topicId") UUID topicId,
            @Param("status") ContentStatus status,
            Pageable pageable);

    Optional<ContentItem> findByIdAndStatus(UUID id, ContentStatus status);

    Optional<ContentItem> findByIdAndStageAndStatus(UUID id, ContentStage stage, ContentStatus status);

    Page<ContentItem> findByStatus(ContentStatus status, Pageable pageable);

    Page<ContentItem> findByType(ContentType type, Pageable pageable);

    // Admin workspace filter: every param optional and ANDed together (type+stage+status+keyword
    // used to be handled by separate findByStatus/findByType/searchStaffByKeyword* methods that
    // branched on which param was present instead of combining them, so passing e.g. type=FAQ
    // together with status=DRAFT silently ignored the type and returned drafts of every type.
    // keyword is CAST to string: an untyped null bind parameter used only inside lower(...) makes
    // pgjdbc guess its type as bytea (no other context to infer from), and lower(bytea) doesn't exist.
    @Query("SELECT c FROM ContentItem c WHERE " +
           "(:type IS NULL OR c.type = :type) AND " +
           "(:stage IS NULL OR c.stage = :stage) AND " +
           "(:status IS NULL OR c.status = :status) AND " +
           "(:keyword IS NULL OR LOWER(c.title) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) " +
           "   OR LOWER(c.body) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))")
    Page<ContentItem> findByAdminFilters(
            @Param("type") ContentType type,
            @Param("stage") ContentStage stage,
            @Param("status") ContentStatus status,
            @Param("keyword") String keyword,
            Pageable pageable);

    // UC-113: impact report — published content reach (count only, no view/impression column exists)
    long countByPublishedAtIsNotNull();

    // ContentImageOrphanCleanup_TDS.md ADR-CLEAN-001: deliberately NOT filtered by status — a
    // PUBLIC content image referenced by ARCHIVED content must still count as "referenced" (the
    // row is never hard-deleted, so its images must not be purged either — ADR-RTE-007 addendum,
    // ContentRichTextEditor_TDS.md).
    @Query("SELECT COUNT(c) > 0 FROM ContentItem c WHERE c.body LIKE CONCAT('%', :publicId, '%')")
    boolean existsByBodyContaining(@Param("publicId") String publicId);

    @Query("SELECT c FROM ContentItem c WHERE " +
           "c.status = :status AND " +
           "(:keyword IS NULL OR LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "   OR LOWER(c.body) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:type IS NULL OR c.type = :type) AND " +
           "(:stage IS NULL OR c.stage = :stage) AND " +
           "(:topicId IS NULL OR c.topicId = :topicId) " +
           "ORDER BY c.publishedAt DESC NULLS LAST")
    Page<ContentItem> searchByFilters(
            @Param("keyword") String keyword,
            @Param("type") ContentType type,
            @Param("stage") ContentStage stage,
            @Param("topicId") UUID topicId,
            @Param("status") ContentStatus status,
            Pageable pageable);
}
