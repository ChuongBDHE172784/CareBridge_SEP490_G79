package com.carebridge.backend.notification.service.impl;

import com.carebridge.backend.notification.entity.NotificationRecord;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
class ConsultationRequestNotificationWriter {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    ConsultationRequestNotificationWriter(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    boolean insertIfAbsent(NotificationRecord candidate) {
        return jdbcTemplate.update("""
                INSERT INTO notification_records
                    (id, user_id, type, title, body, reference_id, reference_type,
                     status, attempt_count, created_at, metadata)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb)
                ON CONFLICT (user_id, reference_id, ((metadata ->> 'eventType')))
                    WHERE type = 'CONSULTATION' AND reference_type = 'CONSULTATION_REQUEST'
                DO NOTHING
                """,
                candidate.getId(),
                candidate.getUserId(),
                candidate.getType().name(),
                candidate.getTitle(),
                candidate.getBody(),
                candidate.getReferenceId(),
                candidate.getReferenceType(),
                candidate.getStatus().name(),
                candidate.getAttemptCount(),
                Timestamp.from(candidate.getCreatedAt()),
                toJson(candidate.getMetadata())) == 1;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    boolean claim(UUID id) {
        return jdbcTemplate.update("""
                UPDATE notification_records
                   SET status = 'PROCESSING', processing_started_at = now(), updated_at = now()
                 WHERE id = ?
                   AND (status = 'PENDING'
                        OR (status = 'PROCESSING'
                            AND processing_started_at < now() - interval '1 minute'))
                """, id) == 1;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void complete(NotificationRecord record) {
        jdbcTemplate.update("""
                UPDATE notification_records
                   SET status = ?, attempt_count = ?, fcm_message_id = ?,
                       sent_at = ?, failed_at = ?, processing_started_at = NULL,
                       updated_at = now()
                 WHERE id = ?
                """,
                record.getStatus().name(),
                record.getAttemptCount(),
                record.getFcmMessageId(),
                record.getSentAt() == null ? null : Timestamp.from(record.getSentAt()),
                record.getFailedAt() == null ? null : Timestamp.from(record.getFailedAt()),
                record.getId());
    }

    List<UUID> findPendingIds(int limit) {
        return jdbcTemplate.query("""
                SELECT id
                  FROM notification_records
                 WHERE type = 'CONSULTATION'
                   AND reference_type = 'CONSULTATION_REQUEST'
                   AND (status = 'PENDING'
                        OR (status = 'PROCESSING'
                            AND processing_started_at < now() - interval '1 minute'))
                 ORDER BY created_at, id
                 LIMIT ?
                """, (rs, rowNum) -> rs.getObject(1, UUID.class), limit);
    }

    private String toJson(Map<String, String> metadata) {
        try {
            return objectMapper.writeValueAsString(metadata == null ? Map.of() : metadata);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize consultation request metadata", e);
        }
    }
}
