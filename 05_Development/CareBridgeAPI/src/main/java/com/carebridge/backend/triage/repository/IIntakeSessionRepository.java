package com.carebridge.backend.triage.repository;

import com.carebridge.backend.triage.entity.IntakeSession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IIntakeSessionRepository extends JpaRepository<IntakeSession, UUID> {
    Optional<IntakeSession> findByIdAndUserId(UUID id, UUID userId);
    List<IntakeSession> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
