package com.carebridge.backend.community.repository;

import com.carebridge.backend.community.entity.CommunityAnswerLike;
import java.util.Collection;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CommunityAnswerLikeRepository extends JpaRepository<CommunityAnswerLike, UUID> {

    boolean existsByUserIdAndAnswerId(UUID userId, UUID answerId);

    Optional<CommunityAnswerLike> findByUserIdAndAnswerId(UUID userId, UUID answerId);

    // Batch like-state check to avoid N+1 when hydrating a question's answer list (UC-59 hydration fix)
    @Query("SELECT l.answerId FROM CommunityAnswerLike l WHERE l.userId = :userId AND l.answerId IN :answerIds")
    Set<UUID> findLikedAnswerIds(@Param("userId") UUID userId, @Param("answerIds") Collection<UUID> answerIds);
}
