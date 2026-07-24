package com.carebridge.backend.emergency.repository;

import com.carebridge.backend.emergency.entity.TriageEmergencyEscalation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TriageEmergencyEscalationRepository
        extends JpaRepository<TriageEmergencyEscalation, UUID> {

    Optional<TriageEmergencyEscalation> findByIntakeSessionId(UUID intakeSessionId);
}
