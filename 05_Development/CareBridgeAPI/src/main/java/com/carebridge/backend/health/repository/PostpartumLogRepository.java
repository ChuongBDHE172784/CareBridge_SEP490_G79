package com.carebridge.backend.health.repository;

import com.carebridge.backend.health.entity.PostpartumLog;
import com.carebridge.backend.health.entity.PostpartumLogStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PostpartumLogRepository extends JpaRepository<PostpartumLog, UUID> {

    Optional<PostpartumLog> findByIdAndStatus(UUID id, PostpartumLogStatus status);

    List<PostpartumLog> findByJourneyIdAndStatusOrderByLogDateDescCreatedAtDesc(
            UUID journeyId, PostpartumLogStatus status);
}
