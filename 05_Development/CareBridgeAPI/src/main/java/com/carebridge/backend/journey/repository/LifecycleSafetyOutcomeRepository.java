package com.carebridge.backend.journey.repository;

import com.carebridge.backend.journey.entity.LifecycleSafetyOutcome;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.Repository;

public interface LifecycleSafetyOutcomeRepository
        extends Repository<LifecycleSafetyOutcome, UUID> {
    Optional<LifecycleSafetyOutcome> findByIntakeSessionId(UUID intakeSessionId);
    long countByIntakeSessionId(UUID intakeSessionId);
}
