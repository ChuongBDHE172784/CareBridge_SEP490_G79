package com.carebridge.backend.triage.repository;

import com.carebridge.backend.triage.entity.HealthMemoryEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HealthMemoryEntryRepository extends JpaRepository<HealthMemoryEntry, UUID> {
    @Query("""
            select entry from HealthMemoryEntry entry
            where entry.userId = :userId and entry.motherProfileId = :profileId
              and entry.relatedStage = :stage and entry.deletedAt is null
              and (entry.expiresAt is null or entry.expiresAt > :now)
            order by entry.createdAt desc
            """)
    List<HealthMemoryEntry> findActiveMaternal(@Param("userId") UUID userId, @Param("profileId") UUID profileId,
            @Param("stage") com.carebridge.backend.triage.TriageStage stage, @Param("now") Instant now);

    @Query("""
            select entry from HealthMemoryEntry entry
            where entry.userId = :userId and entry.babyProfileId = :profileId
              and entry.relatedStage = :stage and entry.deletedAt is null
              and (entry.expiresAt is null or entry.expiresAt > :now)
            order by entry.createdAt desc
            """)
    List<HealthMemoryEntry> findActivePediatric(@Param("userId") UUID userId, @Param("profileId") UUID profileId,
            @Param("stage") com.carebridge.backend.triage.TriageStage stage, @Param("now") Instant now);
    Optional<HealthMemoryEntry> findByIdAndUserIdAndDeletedAtIsNull(UUID id, UUID userId);
}
