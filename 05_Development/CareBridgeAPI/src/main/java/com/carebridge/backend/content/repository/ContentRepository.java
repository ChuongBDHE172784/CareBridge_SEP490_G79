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

    @Query("SELECT c FROM ContentItem c WHERE " +
           "LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(c.body) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<ContentItem> searchStaffByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT c FROM ContentItem c WHERE c.type = :type AND " +
           "(LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(c.body) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<ContentItem> searchStaffByKeywordAndType(
            @Param("keyword") String keyword, @Param("type") ContentType type, Pageable pageable);

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
