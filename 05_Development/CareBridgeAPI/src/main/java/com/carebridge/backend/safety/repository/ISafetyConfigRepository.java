package com.carebridge.backend.safety.repository;

import com.carebridge.backend.safety.entity.SafetyMonitoringConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface ISafetyConfigRepository extends JpaRepository<SafetyMonitoringConfig, UUID> {
    Optional<SafetyMonitoringConfig> findByUserId(UUID userId);
}
