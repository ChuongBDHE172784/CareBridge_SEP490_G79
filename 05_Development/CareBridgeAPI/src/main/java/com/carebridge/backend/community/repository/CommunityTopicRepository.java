package com.carebridge.backend.community.repository;

import com.carebridge.backend.community.entity.CommunityTopic;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommunityTopicRepository extends JpaRepository<CommunityTopic, UUID> {

    List<CommunityTopic> findAllByOrderBySortOrderAsc();

    List<CommunityTopic> findAllByIsHiddenFalseOrderBySortOrderAsc();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);

    Optional<CommunityTopic> findByIdAndIsHiddenFalse(UUID id);

    @Query("SELECT t FROM CommunityTopic t WHERE t.isHidden = false AND LOWER(t.name) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY t.sortOrder ASC")
    List<CommunityTopic> searchByKeyword(@Param("keyword") String keyword);

    @Query("SELECT t FROM CommunityTopic t WHERE LOWER(t.name) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY t.sortOrder ASC")
    List<CommunityTopic> searchByKeywordIncludingHidden(@Param("keyword") String keyword);
}
