package com.carebridge.backend.audit.repository;

import com.carebridge.backend.audit.entity.SecurityEvent;
import com.carebridge.backend.audit.entity.SecurityEventType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SecurityEventRepository extends JpaRepository<SecurityEvent, Long> {

    @Query("""
        SELECT se FROM SecurityEvent se
        WHERE (:userId IS NULL OR se.userId = :userId)
          AND (:eventType IS NULL OR se.eventType = :eventType)
          AND (:severity IS NULL OR se.severity = :severity)
          AND (:status IS NULL OR se.status = :status)
          AND (:ipAddress IS NULL OR se.ipAddress = :ipAddress)
          AND (:from IS NULL OR se.occurredAt >= :from)
          AND (:to IS NULL OR se.occurredAt <= :to)
        """)
    Page<SecurityEvent> search(
            @Param("userId") UUID userId,
            @Param("eventType") SecurityEventType eventType,
            @Param("severity") String severity,
            @Param("status") String status,
            @Param("ipAddress") String ipAddress,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);

    List<SecurityEvent> findByCorrelationIdOrderByOccurredAtAsc(UUID correlationId);

    @Modifying
    @Query("""
        UPDATE SecurityEvent se
        SET se.status = :status, se.reviewedBy = :reviewedBy, se.reviewedAt = :reviewedAt
        WHERE se.id = :id
        """)
    int updateStatus(@Param("id") Long id,
                     @Param("status") String status,
                     @Param("reviewedBy") UUID reviewedBy,
                     @Param("reviewedAt") Instant reviewedAt);
}
