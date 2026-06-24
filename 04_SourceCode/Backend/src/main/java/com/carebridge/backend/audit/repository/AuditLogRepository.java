package com.carebridge.backend.audit.repository;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.entity.AuditLog;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("""
            select a
            from AuditLog a
            where (:userId is null or a.userId = :userId)
              and (:action is null or a.action = :action)
              and (:fromDate is null or a.timestamp >= :fromDate)
              and (:toDate is null or a.timestamp <= :toDate)
            order by a.timestamp desc
            """)
    Page<AuditLog> search(
            @Param("userId") Long userId,
            @Param("action") AuditAction action,
            @Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate,
            Pageable pageable);
}
