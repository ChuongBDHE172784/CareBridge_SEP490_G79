package com.carebridge.backend.consultation.repository;

import com.carebridge.backend.consultation.entity.ConsultationSession;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsultationSessionRepository extends JpaRepository<ConsultationSession, UUID> {

    // UC-113: "completed" oracle is ended_at IS NOT NULL — the exact session_status "completed" enum
    // value is unconfirmed (TDS ADR-001 Open item), so this repo does not filter on session_status.
    long countByEndedAtIsNotNull();
}
