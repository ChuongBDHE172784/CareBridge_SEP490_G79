package com.carebridge.backend.community.repository;

import com.carebridge.backend.community.entity.CommunityAnswerLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CommunityAnswerLikeRepository extends JpaRepository<CommunityAnswerLike, UUID> {

    boolean existsByUserIdAndAnswerId(UUID userId, UUID answerId);

    Optional<CommunityAnswerLike> findByUserIdAndAnswerId(UUID userId, UUID answerId);
}
