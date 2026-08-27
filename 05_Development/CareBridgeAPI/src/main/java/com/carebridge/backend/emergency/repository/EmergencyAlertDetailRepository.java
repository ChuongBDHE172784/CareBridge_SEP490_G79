package com.carebridge.backend.emergency.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class EmergencyAlertDetailRepository {

    private final JdbcTemplate jdbcTemplate;

    public Optional<LinkedFallSnapshot> findLatestLinkedFall(UUID sessionId, UUID motherId) {
        return jdbcTemplate.query("""
                SELECT user_latitude, user_longitude, detected_at
                  FROM safety_events
                 WHERE record_type = 'IMU_EVENT'
                   AND emergency_session_id = ?
                   AND user_id = ?
                 ORDER BY detected_at DESC
                 LIMIT 1
                """, resultSet -> {
                    if (!resultSet.next()) return Optional.empty();
                    var detectedAt = resultSet.getTimestamp("detected_at");
                    return Optional.of(new LinkedFallSnapshot(
                            resultSet.getBigDecimal("user_latitude"),
                            resultSet.getBigDecimal("user_longitude"),
                            detectedAt == null ? null : detectedAt.toInstant()));
                }, sessionId, motherId);
    }

    public record LinkedFallSnapshot(
            BigDecimal latitude,
            BigDecimal longitude,
            Instant detectedAt) {
    }
}
