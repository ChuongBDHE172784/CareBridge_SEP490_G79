package com.carebridge.backend.emergency.repository;

import com.carebridge.backend.emergency.entity.FamilyAlertLog;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Read-only projection of the latest immutable FAMILY_ALERT snapshot. */
@Repository
@RequiredArgsConstructor
public class IFamilyAlertLogRepository {

    private final JdbcTemplate jdbcTemplate;

    public boolean existsBySessionId(UUID sessionId) {
        return findBySessionId(sessionId).isPresent();
    }

    public Optional<FamilyAlertLog> findBySessionId(UUID sessionId) {
        return jdbcTemplate.query("""
                SELECT safety_event_action_id, safety_event_id, created_at,
                       recipient_count, location_included, created_by_text,
                       action_type, idempotency_key
                  FROM safety_event_actions
                 WHERE action_type = 'FAMILY_ALERT'
                   AND safety_event_id = ?
                 ORDER BY alert_generation DESC, created_at DESC,
                          safety_event_action_id DESC
                 LIMIT 1
                """, resultSet -> {
                    if (!resultSet.next()) {
                        return Optional.empty();
                    }
                    return Optional.of(FamilyAlertLog.builder()
                            .id(resultSet.getObject("safety_event_action_id", UUID.class))
                            .sessionId(resultSet.getObject("safety_event_id", UUID.class))
                            .sentAt(resultSet.getTimestamp("created_at").toInstant())
                            .recipientCount(resultSet.getInt("recipient_count"))
                            .locationIncluded(resultSet.getBoolean("location_included"))
                            .createdBy(resultSet.getString("created_by_text"))
                            .actionType(resultSet.getString("action_type"))
                            .idempotencyKey(resultSet.getString("idempotency_key"))
                            .build());
                }, sessionId);
    }
}
