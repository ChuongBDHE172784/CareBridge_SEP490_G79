package com.carebridge.backend.triage.repository;

import com.carebridge.backend.triage.entity.EvidenceSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EvidenceSourceRepository extends JpaRepository<EvidenceSource, UUID> {
    @Query("select source from EvidenceSource source "
            + "where lower(trim(source.domain)) in :domains order by source.id")
    List<EvidenceSource> findAllByNormalizedDomains(
            @Param("domains") Collection<String> domains);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select source from EvidenceSource source "
            + "where lower(trim(source.domain)) in :domains order by source.id")
    List<EvidenceSource> findAllByNormalizedDomainsForUpdate(
            @Param("domains") Collection<String> domains);

    Optional<EvidenceSource> findByDomainIgnoreCase(String domain);
    List<EvidenceSource> findByStatusOrderByUpdatedAtDesc(String status);
    List<EvidenceSource> findByStatus(String status);
}
