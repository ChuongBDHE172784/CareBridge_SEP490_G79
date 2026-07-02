package com.carebridge.backend.community.repository;

import com.carebridge.backend.community.entity.CommunityBookmark;
import java.util.Collection;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CommunityBookmarkRepository extends JpaRepository<CommunityBookmark, UUID> {

    boolean existsByUserIdAndQuestionId(UUID userId, UUID questionId);

    Optional<CommunityBookmark> findByUserIdAndQuestionId(UUID userId, UUID questionId);

    Page<CommunityBookmark> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    // Batch bookmark-state check to avoid N+1 when hydrating a feed/search page (UC-58 hydration fix)
    @Query("SELECT b.questionId FROM CommunityBookmark b WHERE b.userId = :userId AND b.questionId IN :questionIds")
    Set<UUID> findBookmarkedQuestionIds(@Param("userId") UUID userId, @Param("questionIds") Collection<UUID> questionIds);
}
