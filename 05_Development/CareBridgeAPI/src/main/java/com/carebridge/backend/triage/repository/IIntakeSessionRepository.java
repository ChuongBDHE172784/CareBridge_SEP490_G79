package com.carebridge.backend.triage.repository;

import com.carebridge.backend.triage.entity.IntakeSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IIntakeSessionRepository extends JpaRepository<IntakeSession, UUID> {
    Optional<IntakeSession> findByIdAndUserId(UUID id, UUID userId);
    Optional<IntakeSession> findByUserIdAndClientRequestId(UUID userId, String clientRequestId);
    Optional<IntakeSession> findByUserIdAndContinuationToken(UUID userId, UUID continuationToken);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select session from IntakeSession session where session.userId = :userId and session.continuationToken = :token")
    Optional<IntakeSession> findForUpdateByUserIdAndContinuationToken(
            @Param("userId") UUID userId, @Param("token") UUID token);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select session from IntakeSession session where session.id = :id and session.userId = :userId")
    Optional<IntakeSession> findForUpdateByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);
    List<IntakeSession> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
