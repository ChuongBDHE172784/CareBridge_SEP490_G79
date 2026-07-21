package com.carebridge.backend.health.repository;

import com.carebridge.backend.health.entity.PostpartumLog;
import com.carebridge.backend.health.entity.PostpartumLogStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;

public interface PostpartumLogRepository extends JpaRepository<PostpartumLog, UUID> {

    @Query(value = "SELECT 1 FROM pg_advisory_xact_lock(hashtextextended(CAST(:journeyId AS text), 1))",
            nativeQuery = true)
    Integer acquireJourneyMutationLock(@Param("journeyId") UUID journeyId);

    Optional<PostpartumLog> findByIdAndStatus(UUID id, PostpartumLogStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PostpartumLog p where p.id = :id and p.status = :status")
    Optional<PostpartumLog> findByIdAndStatusForUpdate(
            @Param("id") UUID id,
            @Param("status") PostpartumLogStatus status);

    Page<PostpartumLog> findByJourneyIdAndStatus(
            UUID journeyId, PostpartumLogStatus status, Pageable pageable);

    Optional<PostpartumLog> findByJourneyIdAndSubmissionId(
            UUID journeyId, UUID submissionId);
}
