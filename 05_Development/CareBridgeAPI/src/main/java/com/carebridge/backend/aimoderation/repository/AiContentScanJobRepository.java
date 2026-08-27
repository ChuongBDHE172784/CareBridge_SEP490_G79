package com.carebridge.backend.aimoderation.repository;

import com.carebridge.backend.aimoderation.entity.AiContentScanJob;
import com.carebridge.backend.aimoderation.entity.AiScanJobStatus;
import com.carebridge.backend.content.entity.ReportTargetType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AiContentScanJobRepository extends JpaRepository<AiContentScanJob, UUID> {

    boolean existsByTargetTypeAndTargetIdAndContentHashAndStatusIn(
            ReportTargetType targetType, UUID targetId, String contentHash, Collection<AiScanJobStatus> statuses);

    @Query("select j.id from AiContentScanJob j where j.status = :status and j.nextAttemptAt <= :now order by j.createdAt asc")
    List<UUID> findClaimableIds(@Param("status") AiScanJobStatus status, @Param("now") Instant now, Pageable pageable);

    @Query("""
            select j.id from AiContentScanJob j
            where j.status = :status
              and j.targetType in :targetTypes
              and j.nextAttemptAt <= :now
            order by j.createdAt asc
            """)
    List<UUID> findClaimableIdsByTargetTypeIn(
            @Param("status") AiScanJobStatus status,
            @Param("targetTypes") Collection<ReportTargetType> targetTypes,
            @Param("now") Instant now,
            Pageable pageable);

    /**
     * Atomic claim: exactly one caller wins because the UPDATE is guarded by the current
     * status. Returns 0 when another worker already claimed the job. attempt_count is
     * incremented here so every processing attempt is counted exactly once.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update AiContentScanJob j
               set j.status = :processing,
                   j.attemptCount = j.attemptCount + 1,
                   j.lockedBy = :workerId,
                   j.lockedAt = :now,
                   j.updatedAt = :now
             where j.id = :jobId
               and j.status = :queued
               and j.nextAttemptAt <= :now
            """)
    int claim(@Param("jobId") UUID jobId,
              @Param("workerId") String workerId,
              @Param("now") Instant now,
              @Param("queued") AiScanJobStatus queued,
              @Param("processing") AiScanJobStatus processing);

    /** Requeues PROCESSING jobs whose lock is older than the cutoff (crashed worker). */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update AiContentScanJob j
               set j.status = :queued,
                   j.lockedBy = null,
                   j.lockedAt = null,
                   j.updatedAt = :now
             where j.status = :processing
               and j.lockedAt < :cutoff
            """)
    int requeueStale(@Param("cutoff") Instant cutoff,
                     @Param("now") Instant now,
                     @Param("queued") AiScanJobStatus queued,
                     @Param("processing") AiScanJobStatus processing);

    long countByStatus(AiScanJobStatus status);

    long countByStatusAndUpdatedAtAfter(AiScanJobStatus status, Instant after);
}
