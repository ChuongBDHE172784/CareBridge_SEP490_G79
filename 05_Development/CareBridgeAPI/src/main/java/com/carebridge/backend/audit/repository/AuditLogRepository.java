package com.carebridge.backend.audit.repository;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.entity.AuditLog;
import java.time.Instant;
import java.util.List;
import java.util.Collection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, java.util.UUID> {

    // Admin Governance cluster (UC114/UC115/UC116) integration-test verification helper.
    // Both entityId and action are non-null columns, so a derived query is safe here
    // (no null-bind-parameter type-inference issue).
    List<AuditLog> findByEntityIdAndAction(java.util.UUID entityId, AuditAction action);

    List<AuditLog> findByEntityIdAndEntityTypeAndActionInOrderByCreatedAtDesc(
            java.util.UUID entityId, String entityType, Collection<AuditAction> actions);

    // CASTs keep PostgreSQL from failing with "could not determine data type of
    // parameter" when the optional timestamp filters are bound as null.
    @Query("""
            select a
            from AuditLog a
            where (:userId is null or a.actorUserId = :userId)
              and (:action is null or a.action = :action)
              and (cast(:fromDate as instant) is null or a.createdAt >= :fromDate)
              and (cast(:toDate as instant) is null or a.createdAt <= :toDate)
            order by a.createdAt desc
            """)
    Page<AuditLog> search(
            @Param("userId") java.util.UUID userId,
            @Param("action") AuditAction action,
            @Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate,
            Pageable pageable);
}
