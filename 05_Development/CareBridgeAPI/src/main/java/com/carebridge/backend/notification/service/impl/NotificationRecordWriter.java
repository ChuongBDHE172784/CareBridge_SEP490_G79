package com.carebridge.backend.notification.service.impl;

import com.carebridge.backend.notification.entity.NotificationRecord;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * ADR-MEDI-004 mục 3 — DB-enforced idempotency, mirrors DirectMessageWriter.insertIfAbsent
 * exactly: same ON CONFLICT ... DO NOTHING pattern, this time against
 * uq_notification_records_direct_message (user_id, reference_id) WHERE type='MESSAGE' AND
 * reference_type='DIRECT_MESSAGE'. Independent of clientMessageId's early-return in
 * sendMessage() — protects against DirectMessageNotificationListener itself running twice.
 */
@Component
class NotificationRecordWriter {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    NotificationRecordWriter(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    /** Returns true if this call actually inserted the row (i.e. it is genuinely new). */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    boolean insertIfAbsent(NotificationRecord candidate) {
        String metadataJson = toJson(candidate.getMetadata() == null ? Map.of() : candidate.getMetadata());
        return jdbcTemplate.update("""
                INSERT INTO notification_records
                    (id, user_id, type, title, body, reference_id, reference_type, status, attempt_count, created_at, metadata)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb)
                ON CONFLICT (user_id, reference_id) WHERE type = 'MESSAGE' AND reference_type = 'DIRECT_MESSAGE'
                DO NOTHING
                """,
                candidate.getId(), candidate.getUserId(), candidate.getType().name(), candidate.getTitle(),
                candidate.getBody(), candidate.getReferenceId(), candidate.getReferenceType(),
                candidate.getStatus().name(), candidate.getAttemptCount(), Timestamp.from(candidate.getCreatedAt()),
                metadataJson) == 1;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    boolean claim(UUID id) {
        return jdbcTemplate.update("""
                UPDATE notification_records
                SET status = 'PROCESSING', processing_started_at = now()
                WHERE id = ? AND (status = 'PENDING'
                    OR (status = 'PROCESSING'
                        AND processing_started_at < now() - interval '1 minute'))
                """, id) == 1;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void complete(NotificationRecord record) {
        jdbcTemplate.update("""
                UPDATE notification_records
                SET status = ?, attempt_count = ?, fcm_message_id = ?, sent_at = ?, failed_at = ?,
                    processing_started_at = NULL
                WHERE id = ?
                """, record.getStatus().name(), record.getAttemptCount(), record.getFcmMessageId(),
                record.getSentAt() == null ? null : Timestamp.from(record.getSentAt()),
                record.getFailedAt() == null ? null : Timestamp.from(record.getFailedAt()), record.getId());
    }

    java.util.List<UUID> findPendingIds(int limit) {
        return jdbcTemplate.query("""
                SELECT id FROM notification_records
                WHERE type = 'MESSAGE' AND reference_type = 'DIRECT_MESSAGE'
                  AND (status = 'PENDING'
                       OR (status = 'PROCESSING'
                           AND processing_started_at < now() - interval '1 minute'))
                ORDER BY created_at, id LIMIT ?
                """, (rs, rowNum) -> UUID.fromString(rs.getString(1)), limit);
    }

    private String toJson(Map<String, String> metadata) {
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize NotificationRecord metadata", e);
        }
    }
}
