package com.carebridge.backend.content.repository;

import com.carebridge.backend.content.entity.ContentReport;
import com.carebridge.backend.content.entity.ReportStatus;
import com.carebridge.backend.content.entity.ReportTargetType;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ContentReportRepository
        extends JpaRepository<ContentReport, UUID>, JpaSpecificationExecutor<ContentReport> {

    Optional<ContentReport> findByTargetIdAndCategory(UUID targetId, String category);

    Page<ContentReport> findByStatus(ReportStatus status, Pageable pageable);

    Page<ContentReport> findByStatusAndTargetType(
            ReportStatus status, ReportTargetType targetType, Pageable pageable);

    long countByTargetIdAndStatus(UUID targetId, ReportStatus status);

    Page<ContentReport> findByTargetIdAndTargetTypeOrderByCreatedAtDesc(
            UUID targetId, ReportTargetType targetType, Pageable pageable);

    // UC-111: dashboard aggregation — report count grouped by status
    @Query("SELECT r.status, COUNT(r) FROM ContentReport r GROUP BY r.status")
    List<Object[]> countGroupByStatus();

    // UC-111: dashboard aggregation — handling-time avg computed in Java (Instant deltas), not SQL
    // EXTRACT(EPOCH FROM ...) is Postgres-specific and not portable to this project's H2 test datasource
    // (src/test/resources/application.yaml has no Testcontainers/real-Postgres harness — verified project-wide)
    @Query("SELECT r.createdAt, r.resolvedAt FROM ContentReport r WHERE r.resolvedAt IS NOT NULL")
    List<Object[]> findResolvedTimestamps();

    // UC-14: rate limit check — count this reporter's reports on this target within the
    // rolling 24h window (idx_content_reports_rate_limit, added in V2__spec_sync_from_tds.sql).
    int countByReporterUserIdAndTargetIdAndCreatedAtAfter(UUID reporterUserId, UUID targetId, Instant since);

    // UC-14: duplicate check — reporter already has a PENDING report on this target
    // (idx_content_reports_duplicate, added in V2__spec_sync_from_tds.sql).
    boolean existsByReporterUserIdAndTargetIdAndStatus(UUID reporterUserId, UUID targetId, ReportStatus status);

    // CB-MOD-IMP-016: duplicate guard must also cover claimed (IN_REVIEW) reports
    boolean existsByReporterUserIdAndTargetIdAndStatusIn(
            UUID reporterUserId, UUID targetId, Collection<ReportStatus> statuses);

    /**
     * CB-MOD-IMP-016 ADR-005: open-case lookup for AI attach-first dedup. Pessimistic lock
     * serializes concurrent workers deciding attach-vs-create for the same target.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select r from ContentReport r
             where r.targetId = :targetId
               and r.targetType = :targetType
               and r.status in :statuses
             order by r.createdAt asc
            """)
    List<ContentReport> findOpenCasesForUpdate(@Param("targetId") UUID targetId,
                                               @Param("targetType") ReportTargetType targetType,
                                               @Param("statuses") Collection<ReportStatus> statuses);

    /**
     * CB-MOD-IMP-016 ADR-006: atomic claim — exactly one moderator wins because the UPDATE is
     * guarded by status = PENDING. Returns 0 when the report was already claimed/resolved.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update ContentReport r
               set r.status = :inReview,
                   r.assignedModeratorId = :moderatorId,
                   r.claimedAt = :now,
                   r.updatedAt = :now
             where r.id = :reportId
               and r.status = :pending
            """)
    int claimReport(@Param("reportId") UUID reportId,
                    @Param("moderatorId") UUID moderatorId,
                    @Param("now") Instant now,
                    @Param("pending") ReportStatus pending,
                    @Param("inReview") ReportStatus inReview);

    /** Atomic release back to PENDING — only the claiming moderator may release. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update ContentReport r
               set r.status = :pending,
                   r.assignedModeratorId = null,
                   r.claimedAt = null,
                   r.updatedAt = :now
             where r.id = :reportId
               and r.status = :inReview
               and r.assignedModeratorId = :moderatorId
            """)
    int releaseReport(@Param("reportId") UUID reportId,
                      @Param("moderatorId") UUID moderatorId,
                      @Param("now") Instant now,
                      @Param("pending") ReportStatus pending,
                      @Param("inReview") ReportStatus inReview);
}
