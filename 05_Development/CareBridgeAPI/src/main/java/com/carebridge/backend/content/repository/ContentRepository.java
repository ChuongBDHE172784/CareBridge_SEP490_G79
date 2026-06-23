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
}
