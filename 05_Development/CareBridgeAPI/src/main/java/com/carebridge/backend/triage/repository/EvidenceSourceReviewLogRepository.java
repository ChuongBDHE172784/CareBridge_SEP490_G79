package com.carebridge.backend.triage.repository;

import com.carebridge.backend.triage.entity.EvidenceSourceReviewLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EvidenceSourceReviewLogRepository extends JpaRepository<EvidenceSourceReviewLog, UUID> {
    List<EvidenceSourceReviewLog> findByEvidenceSourceIdOrderByChangedAtDesc(UUID evidenceSourceId);
}
