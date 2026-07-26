package com.carebridge.backend.emergency.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.PrePersist;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "safety_events")
@org.hibernate.annotations.SQLRestriction("action_type = 'DELIVERY'")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmergencyAlertDelivery {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "safety_event_id")
    private UUID id;
    @Column(name = "parent_event_id", nullable = false) private UUID emergencySessionId;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(name = "recipient_user_id", nullable = false) private UUID recipientUserId;
    @Column(name = "device_token_id", nullable = false) private UUID deviceTokenId;
    @Column(name = "notification_record_id", nullable = false) private UUID notificationRecordId;
    @Column(name = "delivery_status", nullable = false, length = 20) private String deliveryStatus;
    @Column(name = "attempt_number", nullable = false) private int attemptCount;
    @Column(name = "fcm_message_id", length = 255) private String fcmMessageId;
    @Column(name = "failure_code", length = 120) private String failureCode;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "delivered_at") private Instant deliveredAt;

    @Builder.Default
    @Column(name = "action_type", nullable = false, updatable = false)
    private String actionType = "DELIVERY";
    @Column(name = "idempotency_key", nullable = false, updatable = false)
    private String idempotencyKey;
    @Builder.Default
    @Column(name = "event_type", nullable = false, updatable = false)
    private String eventType = "ACTION";
    @Column(name = "detected_at", nullable = false, updatable = false)
    private Instant detectedAt;

    @PrePersist
    void prepareCanonicalAction() {
        actionType = "DELIVERY";
        if (idempotencyKey == null) idempotencyKey = "delivery:" + UUID.randomUUID();
        if (userId == null) {
            throw new IllegalStateException("Emergency delivery requires the emergency owner user id");
        }
        if (detectedAt == null) detectedAt = createdAt == null ? Instant.now() : createdAt;
    }
}
