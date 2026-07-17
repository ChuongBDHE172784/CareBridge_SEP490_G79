package com.carebridge.backend.content.repository;

import com.carebridge.backend.content.entity.ContentReport;
import com.carebridge.backend.content.entity.ReportStatus;
import com.carebridge.backend.content.entity.ReportTargetType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ContentReportRepository extends JpaRepository<ContentReport, UUID> {

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

    // Dev seed idempotency (DevDataSeeder) — identifies a previously-seeded report by reporter+target+category
    Optional<ContentReport> findByReporterUserIdAndTargetIdAndCategory(UUID reporterUserId, UUID targetId, String category);
}
