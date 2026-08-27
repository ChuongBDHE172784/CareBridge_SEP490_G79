package com.carebridge.backend.community.repository;

import com.carebridge.backend.community.entity.CommunityTopic;
import com.carebridge.backend.community.entity.TopicType;
import java.util.List;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommunityTopicRepository extends JpaRepository<CommunityTopic, UUID> {

    List<CommunityTopic> findAllByOrderBySortOrderAsc();

    List<CommunityTopic> findAllByIsHiddenFalseOrderBySortOrderAsc();

    // ADR-COM-017: mobile passes type=TOPIC; web (management) omits type to get all 3 kinds.
    List<CommunityTopic> findAllByTypeOrderBySortOrderAsc(TopicType type);

    List<CommunityTopic> findAllByIsHiddenFalseAndTypeOrderBySortOrderAsc(TopicType type);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, UUID id);

    List<CommunityTopic> findAllBySlugIn(Collection<String> slugs);

    /** Public projection/privacy helper: discover retired or malformed rec-* rows in one query. */
    List<CommunityTopic> findAllBySlugStartingWith(String slugPrefix);

    boolean existsByParentId(UUID parentId);

    Optional<CommunityTopic> findByIdAndIsHiddenFalse(UUID id);

    List<CommunityTopic> findAllByIdInAndTypeAndIsHiddenFalse(Collection<UUID> ids, TopicType type);

    // ADR-COM-020: typed, visible lookup used for CATEGORY parent and question-target validation.
    Optional<CommunityTopic> findByIdAndTypeAndIsHiddenFalse(UUID id, TopicType type);

    @Query("SELECT t FROM CommunityTopic t WHERE t.isHidden = false AND LOWER(t.name) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY t.sortOrder ASC")
    List<CommunityTopic> searchByKeyword(@Param("keyword") String keyword);

    @Query("SELECT t FROM CommunityTopic t WHERE LOWER(t.name) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY t.sortOrder ASC")
    List<CommunityTopic> searchByKeywordIncludingHidden(@Param("keyword") String keyword);
}
