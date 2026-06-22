package com.carebridge.backend.audit.repository;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.entity.AuditLog;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    @Query("""
            select a
            from AuditLog a
            where (:userId is null or a.actorUserId = :userId)
              and (:action is null or a.action = :action)
              and (:fromDate is null or a.createdAt >= :fromDate)
              and (:toDate is null or a.createdAt <= :toDate)
            order by a.createdAt desc
            """)
    Page<AuditLog> search(
            @Param("userId") UUID userId,
            @Param("action") AuditAction action,
            @Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate,
            Pageable pageable);
}
