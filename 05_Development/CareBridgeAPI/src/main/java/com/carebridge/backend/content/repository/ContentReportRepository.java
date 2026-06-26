package com.carebridge.backend.content.repository;

import com.carebridge.backend.content.entity.ContentReport;
import com.carebridge.backend.content.entity.ReportStatus;
import com.carebridge.backend.content.entity.ReportTargetType;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContentReportRepository extends JpaRepository<ContentReport, UUID> {

    Page<ContentReport> findByStatus(ReportStatus status, Pageable pageable);

    Page<ContentReport> findByStatusAndTargetType(
            ReportStatus status, ReportTargetType targetType, Pageable pageable);

    long countByTargetIdAndStatus(UUID targetId, ReportStatus status);
}
