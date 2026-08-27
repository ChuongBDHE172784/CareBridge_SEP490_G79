package com.carebridge.backend.community.repository;

import com.carebridge.backend.community.entity.UserTopicFollow;
import java.util.Collection;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserTopicFollowRepository extends JpaRepository<UserTopicFollow, UUID> {

    Optional<UserTopicFollow> findByUserIdAndTopicId(UUID userId, UUID topicId);

    boolean existsByTopicId(UUID topicId);

    // Batch follow-state check to avoid N+1 when hydrating a topic list (UC-171 hydration fix)
    @Query("SELECT f.topicId FROM UserTopicFollow f WHERE f.userId = :userId AND f.topicId IN :topicIds")
    Set<UUID> findFollowedTopicIds(@Param("userId") UUID userId, @Param("topicIds") Collection<UUID> topicIds);
}
