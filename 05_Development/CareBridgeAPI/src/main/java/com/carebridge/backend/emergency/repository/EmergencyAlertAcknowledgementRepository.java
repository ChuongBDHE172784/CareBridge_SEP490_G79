package com.carebridge.backend.emergency.repository;

import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class EmergencyAlertAcknowledgementRepository {

    private final JdbcTemplate jdbcTemplate;

    public AcknowledgementState find(UUID sessionId, UUID recipientUserId) {
        return jdbcTemplate.queryForObject("""
                WITH latest AS (
                    SELECT is_read, read_at, created_at
                      FROM notification_records
                     WHERE user_id = ?
                       AND reference_id = ?
                       AND type = 'EMERGENCY'
                       AND reference_type = 'EMERGENCY_SESSION'
                     ORDER BY created_at DESC
                     LIMIT 1
                )
                SELECT count(*) > 0 AS notification_exists,
                       coalesce(bool_or(is_read), false) AS acknowledged,
                       max(read_at) AS acknowledged_at,
                       max(created_at) AS notified_at
                  FROM latest
                """, (resultSet, rowNum) -> new AcknowledgementState(
                        resultSet.getBoolean("notification_exists"),
                        resultSet.getBoolean("acknowledged"),
                        resultSet.getTimestamp("acknowledged_at") == null
                                ? null
                                : resultSet.getTimestamp("acknowledged_at").toInstant(),
                        resultSet.getTimestamp("notified_at") == null
                                ? null
                                : resultSet.getTimestamp("notified_at").toInstant()),
                recipientUserId, sessionId);
    }

    public int acknowledge(UUID sessionId, UUID recipientUserId, Instant acknowledgedAt) {
        return jdbcTemplate.update("""
                UPDATE notification_records
                   SET is_read = true,
                       read_at = ?,
                       updated_at = ?
                 WHERE user_id = ?
                   AND reference_id = ?
                   AND type = 'EMERGENCY'
                   AND reference_type = 'EMERGENCY_SESSION'
                   AND is_read = false
                """, acknowledgedAt, acknowledgedAt, recipientUserId, sessionId);
    }

    public record AcknowledgementState(
            boolean notificationExists,
            boolean acknowledged,
            Instant acknowledgedAt,
            Instant notifiedAt) {
    }
}
