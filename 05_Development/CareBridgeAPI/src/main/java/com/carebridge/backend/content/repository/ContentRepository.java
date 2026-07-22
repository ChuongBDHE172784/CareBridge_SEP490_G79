package com.carebridge.backend.content.repository;

import com.carebridge.backend.content.entity.ContentItem;
import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.entity.ContentStatus;
import com.carebridge.backend.content.entity.ContentType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ContentRepository extends JpaRepository<ContentItem, UUID> {

    Optional<ContentItem> findByTitleIgnoreCaseAndStageAndType(
            String title, ContentStage stage, ContentType type);

    @Query("SELECT c FROM ContentItem c WHERE " +
           "(:type IS NULL OR c.type = :type) AND " +
           "(:stage IS NULL OR c.stage = :stage) AND " +
           "(:topicId IS NULL OR c.topicId = :topicId) AND " +
           "c.status = :status")
    Page<ContentItem> findByFilters(
            @Param("type") ContentType type,
            @Param("stage") ContentStage stage,
            @Param("topicId") UUID topicId,
            @Param("status") ContentStatus status,
            Pageable pageable);

    Optional<ContentItem> findByIdAndStatus(UUID id, ContentStatus status);

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
