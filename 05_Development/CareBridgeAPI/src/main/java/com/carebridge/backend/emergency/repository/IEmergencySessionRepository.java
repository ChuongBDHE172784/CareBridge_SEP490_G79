package com.carebridge.backend.emergency.repository;

import com.carebridge.backend.emergency.entity.EmergencySession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.UUID;

public interface IEmergencySessionRepository extends JpaRepository<EmergencySession, UUID> {

    @Query("SELECT e FROM EmergencySession e WHERE e.userId = :userId AND e.status = com.carebridge.backend.emergency.EmergencyStatus.ACTIVE")
    Optional<EmergencySession> findActiveByUserId(@Param("userId") UUID userId);
}
