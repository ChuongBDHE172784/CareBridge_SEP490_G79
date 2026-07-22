package com.carebridge.backend.emergency.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "emergency_alert_deliveries")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmergencyAlertDelivery {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "emergency_session_id", nullable = false) private UUID emergencySessionId;
    @Column(name = "recipient_user_id", nullable = false) private UUID recipientUserId;
    @Column(name = "device_token_id", nullable = false) private UUID deviceTokenId;
    @Column(name = "notification_record_id", nullable = false) private UUID notificationRecordId;
    @Column(name = "delivery_status", nullable = false, length = 20) private String deliveryStatus;
    @Column(name = "attempt_count", nullable = false) private int attemptCount;
    @Column(name = "fcm_message_id", length = 255) private String fcmMessageId;
    @Column(name = "failure_code", length = 120) private String failureCode;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "delivered_at") private Instant deliveredAt;
}
