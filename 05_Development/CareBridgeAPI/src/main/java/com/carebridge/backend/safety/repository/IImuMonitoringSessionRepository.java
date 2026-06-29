package com.carebridge.backend.safety.repository;

import com.carebridge.backend.safety.entity.ImuMonitoringSession;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.UUID;

public interface IImuMonitoringSessionRepository extends JpaRepository<ImuMonitoringSession, UUID> {

    @Query("SELECT s FROM ImuMonitoringSession s WHERE s.userId = :userId AND s.status = com.carebridge.backend.safety.ImuSessionStatus.ACTIVE")
    Optional<ImuMonitoringSession> findActiveByUserId(@Param("userId") UUID userId);
}
