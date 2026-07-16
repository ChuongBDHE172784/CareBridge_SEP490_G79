package com.carebridge.backend.triage.repository;

import com.carebridge.backend.triage.entity.EvidenceSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EvidenceSourceRepository extends JpaRepository<EvidenceSource, UUID> {
    Optional<EvidenceSource> findByDomainIgnoreCase(String domain);
    List<EvidenceSource> findByStatusOrderByUpdatedAtDesc(String status);
    List<EvidenceSource> findByStatus(String status);
}
