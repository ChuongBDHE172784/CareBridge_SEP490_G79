package com.carebridge.backend.content.repository;

import com.carebridge.backend.content.entity.ContentReport;
import com.carebridge.backend.content.entity.ReportStatus;
import com.carebridge.backend.content.entity.ReportTargetType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ContentReportRepository extends JpaRepository<ContentReport, UUID> {

    Page<ContentReport> findByStatus(ReportStatus status, Pageable pageable);

    Page<ContentReport> findByStatusAndTargetType(
            ReportStatus status, ReportTargetType targetType, Pageable pageable);

    long countByTargetIdAndStatus(UUID targetId, ReportStatus status);

    // UC-111: dashboard aggregation — report count grouped by status
    @Query("SELECT r.status, COUNT(r) FROM ContentReport r GROUP BY r.status")
    List<Object[]> countGroupByStatus();

    // UC-111: dashboard aggregation — handling-time avg computed in Java (Instant deltas), not SQL
    // EXTRACT(EPOCH FROM ...) is Postgres-specific and not portable to this project's H2 test datasource
    // (src/test/resources/application.yaml has no Testcontainers/real-Postgres harness — verified project-wide)
    @Query("SELECT r.createdAt, r.resolvedAt FROM ContentReport r WHERE r.resolvedAt IS NOT NULL")
    List<Object[]> findResolvedTimestamps();
}
