package com.carebridge.backend.journey.repository;

import com.carebridge.backend.journey.entity.MotherBaselineContext;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MotherBaselineContextRepository
        extends JpaRepository<MotherBaselineContext, UUID> {

    @Query(value = """
            SELECT 1
            FROM pg_advisory_xact_lock(
                hashtextextended(CAST(:ownerUserId AS text), 0))
            """, nativeQuery = true)
    Integer acquireOwnerLock(@Param("ownerUserId") UUID ownerUserId);

    Optional<MotherBaselineContext> findByOwnerUserIdAndSubmissionId(
            UUID ownerUserId, UUID submissionId);

    Optional<MotherBaselineContext> findTopByOwnerUserIdOrderByRevisionDesc(UUID ownerUserId);
}
