package com.carebridge.backend.systemconfiguration.repository;

import com.carebridge.backend.systemconfiguration.entity.SystemConfiguration;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemConfigurationRepository extends JpaRepository<SystemConfiguration, UUID> {
    Optional<SystemConfiguration> findFirstByOrderByCreatedAtAsc();
}
