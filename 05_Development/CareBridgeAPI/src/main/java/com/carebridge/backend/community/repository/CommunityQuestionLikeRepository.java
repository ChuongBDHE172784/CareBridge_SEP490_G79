package com.carebridge.backend.community.repository;

import com.carebridge.backend.community.entity.CommunityQuestionLike;
import java.util.Collection;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CommunityQuestionLikeRepository extends JpaRepository<CommunityQuestionLike, UUID> {

    boolean existsByUserIdAndQuestionId(UUID userId, UUID questionId);

    Optional<CommunityQuestionLike> findByUserIdAndQuestionId(UUID userId, UUID questionId);

    // Batch like-state check to avoid N+1 when hydrating feed/bookmarks/detail
    @Query("SELECT l.questionId FROM CommunityQuestionLike l WHERE l.userId = :userId AND l.questionId IN :questionIds")
    Set<UUID> findLikedQuestionIds(@Param("userId") UUID userId, @Param("questionIds") Collection<UUID> questionIds);
}
