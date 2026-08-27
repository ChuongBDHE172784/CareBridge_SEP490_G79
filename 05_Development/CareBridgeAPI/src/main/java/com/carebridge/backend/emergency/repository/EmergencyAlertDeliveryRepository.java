package com.carebridge.backend.emergency.repository;

import com.carebridge.backend.emergency.service.EmergencyAlertClaim;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Insert/read-only access to immutable DELIVERY intent/result snapshots. */
@Repository
@RequiredArgsConstructor
public class EmergencyAlertDeliveryRepository {

    private final JdbcTemplate jdbcTemplate;

    public record DeliveryAction(UUID actionId, UUID notificationRecordId, int attempts) {
    }

    public Optional<DeliveryAction> findSuccessful(UUID sessionId, UUID deviceTokenId) {
        return queryOne("""
                SELECT action.safety_event_id AS action_id, action.notification_record_id,
                       action.attempt_number
                  FROM safety_events action
                 WHERE action.action_type = 'DELIVERY'
                   AND action.action_phase = 'RESULT'
                   AND action.parent_event_id = ?
                   AND action.device_token_id = ?
                   AND action.delivery_status IN ('SENT','DELIVERED')
                 ORDER BY action.alert_generation DESC, action.created_at DESC
                 LIMIT 1
                """, sessionId, deviceTokenId);
    }

    public DeliveryAction insertIntent(
            EmergencyAlertClaim claim,
            UUID recipientUserId,
            UUID deviceTokenId,
            UUID notificationRecordId) {
        Optional<DeliveryAction> inserted = queryOne("""
                INSERT INTO safety_events (
                    safety_event_id, parent_event_id, record_type, event_type,
                    action_type, user_id, recipient_user_id, device_token_id,
                    device_identifier, notification_record_id, attempt_number,
                    idempotency_key, delivery_status, detected_at, created_at,
                    action_phase, alert_generation, fence_token
                )
                SELECT md5('delivery:' || event.safety_event_id::text || ':'
                           || event.alert_generation::text || ':'
                           || CAST(? AS text) || ':intent')::uuid,
                       event.safety_event_id, 'SAFETY_ACTION', 'ACTION', 'DELIVERY',
                       event.user_id, ?, ?, CAST(? AS text), ?,
                       least(event.alert_generation, 2147483647)::integer,
                       'delivery:' || event.safety_event_id::text || ':'
                           || event.alert_generation::text || ':'
                           || CAST(? AS text) || ':intent',
                       'PENDING', now(), now(), 'INTENT', event.alert_generation,
                       event.alert_claim_token
                  FROM safety_events event
                 WHERE event.safety_event_id = ?
                   AND event.record_type = 'EMERGENCY_SESSION'
                   AND event.status = 'ACTIVE'
                   AND event.alert_status = 'PROCESSING'
                   AND event.alert_generation = ?
                   AND event.alert_claim_token = ?
                   AND event.alert_lease_expires_at > now()
                ON CONFLICT (safety_event_id) DO NOTHING
                RETURNING safety_event_id AS action_id, notification_record_id,
                          attempt_number
                """, deviceTokenId, recipientUserId, deviceTokenId, deviceTokenId,
                notificationRecordId, deviceTokenId, claim.emergencySessionId(),
                claim.generation(), claim.fenceToken());
        if (inserted.isPresent()) {
            return inserted.get();
        }
        return queryOne("""
                SELECT action.safety_event_id AS action_id, action.notification_record_id,
                       action.attempt_number
                  FROM safety_events action
                 WHERE action.action_type = 'DELIVERY'
                   AND action.action_phase = 'INTENT'
                   AND action.parent_event_id = ?
                   AND action.alert_generation = ?
                   AND action.device_token_id = ?
                 LIMIT 1
                """, claim.emergencySessionId(), claim.generation(), deviceTokenId)
                .orElseThrow(() -> new IllegalStateException(
                        "Delivery intent could not be appended"));
    }

    public boolean appendResult(
            UUID intentActionId,
            EmergencyAlertClaim claim,
            boolean successful,
            int attempts,
            String messageId,
            String failureCode) {
        return jdbcTemplate.update("""
                INSERT INTO safety_events (
                    safety_event_id, parent_event_id, record_type, event_type,
                    action_type, user_id, recipient_user_id, device_token_id,
                    device_identifier, notification_record_id, attempt_number,
                    idempotency_key, delivery_status, fcm_message_id, failure_code,
                    detected_at, created_at, delivered_at, action_phase,
                    alert_generation, fence_token, related_action_id
                )
                SELECT md5('delivery:' || intent.parent_event_id::text || ':'
                           || event.alert_generation::text || ':'
                           || intent.device_token_id::text || ':result')::uuid,
                       intent.parent_event_id, 'SAFETY_ACTION', 'ACTION', 'DELIVERY',
                       intent.user_id, intent.recipient_user_id,
                       intent.device_token_id, intent.device_identifier,
                       intent.notification_record_id, greatest(?, 1),
                       'delivery:' || intent.parent_event_id::text || ':'
                           || event.alert_generation::text || ':'
                           || intent.device_token_id::text || ':result',
                       ?, ?, ?, now(), now(), CASE WHEN ? THEN now() ELSE NULL END,
                       'RESULT', event.alert_generation, event.alert_claim_token,
                       intent.safety_event_id
                  FROM safety_events intent
                  JOIN safety_events event
                    ON event.safety_event_id = intent.parent_event_id
                 WHERE intent.safety_event_id = ?
                   AND intent.action_type = 'DELIVERY'
                   AND intent.action_phase = 'INTENT'
                   AND event.record_type = 'EMERGENCY_SESSION'
                   AND event.status = 'ACTIVE'
                   AND event.alert_status = 'PROCESSING'
                   AND event.alert_generation = ?
                   AND event.alert_claim_token = ?
                ON CONFLICT (safety_event_id) DO NOTHING
                """, Math.max(attempts, 1), successful ? "SENT" : "FAILED",
                messageId, failureCode, successful, intentActionId,
                claim.generation(), claim.fenceToken()) == 1;
    }

    private Optional<DeliveryAction> queryOne(String sql, Object... parameters) {
        return jdbcTemplate.query(sql, resultSet -> resultSet.next()
                        ? Optional.of(new DeliveryAction(
                                resultSet.getObject("action_id", UUID.class),
                                resultSet.getObject("notification_record_id", UUID.class),
                                resultSet.getInt("attempt_number")))
                        : Optional.empty(),
                parameters);
    }
}
