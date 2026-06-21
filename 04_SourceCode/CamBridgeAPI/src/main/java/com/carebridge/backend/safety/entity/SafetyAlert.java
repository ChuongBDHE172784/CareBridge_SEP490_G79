package com.carebridge.backend.safety.entity;

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
@Table(name = "safety_alerts")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SafetyAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "safety_alert_id")
    private UUID safetyAlertId;

    @Column(name = "safety_event_id")
    private UUID safetyEventId;

    @Column(name = "recipient_user_id")
    private UUID recipientUserId;

    @Column(name = "location_snapshot_id")
    private UUID locationSnapshotId;

    @Column(name = "alert_reason", length = 30)
    private String alertReason;

    @Column(name = "payload_json", columnDefinition = "jsonb")
    private String payloadJson;

    @Column(name = "delivery_status", length = 20)
    private String deliveryStatus;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
