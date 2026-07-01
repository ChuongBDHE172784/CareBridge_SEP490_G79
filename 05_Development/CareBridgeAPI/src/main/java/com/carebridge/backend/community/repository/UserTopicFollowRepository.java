package com.carebridge.backend.community.repository;

import com.carebridge.backend.community.entity.UserTopicFollow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserTopicFollowRepository extends JpaRepository<UserTopicFollow, UUID> {

    Optional<UserTopicFollow> findByUserIdAndTopicId(UUID userId, UUID topicId);
}
