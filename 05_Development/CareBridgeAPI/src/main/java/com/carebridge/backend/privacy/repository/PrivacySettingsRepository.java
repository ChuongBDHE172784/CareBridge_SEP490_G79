package com.carebridge.backend.privacy.repository;

import com.carebridge.backend.privacy.entity.PrivacySettings;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PrivacySettingsRepository extends JpaRepository<PrivacySettings, UUID> {

    Optional<PrivacySettings> findByUserId(UUID userId);
}
