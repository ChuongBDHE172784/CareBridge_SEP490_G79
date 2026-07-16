package com.carebridge.backend.notification.repository;

import com.carebridge.backend.notification.entity.NotificationRecord;
import com.carebridge.backend.notification.entity.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRecordRepository extends JpaRepository<NotificationRecord, UUID> {

    Page<NotificationRecord> findByUserId(UUID userId, Pageable pageable);

    Page<NotificationRecord> findByUserIdAndType(UUID userId, NotificationType type, Pageable pageable);

    @Query("SELECT r FROM NotificationRecord r WHERE r.userId = :userId "
            + "AND r.status NOT IN ('PENDING', 'PROCESSING')")
    Page<NotificationRecord> findVisibleByUserId(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT r FROM NotificationRecord r WHERE r.userId = :userId AND r.type = :type "
            + "AND r.status NOT IN ('PENDING', 'PROCESSING')")
    Page<NotificationRecord> findVisibleByUserIdAndType(
            @Param("userId") UUID userId,
            @Param("type") NotificationType type,
            Pageable pageable);

    // =========================================================================
    // UC-12: Mark as Read
    // =========================================================================

    /**
     * Find a notification record by its ID and owner — enforces ownership (UC-12, BR-NOTIF-OWN-012).
     */
    Optional<NotificationRecord> findByIdAndUserId(UUID id, UUID userId);

    /**
     * Atomically mark a single notification as read (UC-12, ADR-012-003).
     * Condition {@code AND is_read = false} makes the operation idempotent.
     *
     * @return number of rows affected (0 = already read or not found)
     */
    @Modifying
    @Query("""
            UPDATE NotificationRecord r
            SET r.isRead = true, r.readAt = :readAt
            WHERE r.id = :id
              AND r.userId = :userId
              AND r.isRead = false
            """)
    int markAsReadById(
            @Param("id") UUID id,
            @Param("userId") UUID userId,
            @Param("readAt") Instant readAt);

    /**
     * Atomically mark all unread notifications of a user as read (UC-12).
     *
     * @return number of rows affected
     */
    @Modifying
    @Query("""
            UPDATE NotificationRecord r
            SET r.isRead = true, r.readAt = :readAt
            WHERE r.userId = :userId
              AND r.isRead = false
            """)
    int markAllAsReadByUserId(
            @Param("userId") UUID userId,
            @Param("readAt") Instant readAt);

    /**
     * Count unread notifications for a user.
     */
    @Query("SELECT COUNT(r) FROM NotificationRecord r WHERE r.userId = :userId AND r.isRead = false "
            + "AND r.status NOT IN ('PENDING', 'PROCESSING')")
    long countUnreadByUserId(@Param("userId") UUID userId);

    // ADR-MEDI-004 mục 3 — lookup after NotificationRecordWriter.insertIfAbsent's ON CONFLICT DO
    // NOTHING, to read back the row that already exists for this (recipient, messageId) pair.
    Optional<NotificationRecord> findByUserIdAndReferenceIdAndTypeAndReferenceType(
            UUID userId, UUID referenceId, NotificationType type, String referenceType);
}
