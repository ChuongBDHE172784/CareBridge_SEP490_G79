package com.carebridge.backend.emergency.repository;

import com.carebridge.backend.emergency.entity.FamilyAlertLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface IFamilyAlertLogRepository extends JpaRepository<FamilyAlertLog, UUID> {
    boolean existsBySessionId(UUID sessionId);

    Optional<FamilyAlertLog> findBySessionId(UUID sessionId);
}
