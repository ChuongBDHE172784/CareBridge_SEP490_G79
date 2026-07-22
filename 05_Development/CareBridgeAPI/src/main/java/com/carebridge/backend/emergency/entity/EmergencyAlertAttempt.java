package com.carebridge.backend.emergency.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "emergency_alert_attempts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmergencyAlertAttempt {
    @Id
    @Column(name = "emergency_session_id", nullable = false)
    private UUID emergencySessionId;
    @Column(name = "status", nullable = false, length = 20)
    private String status;
    @Column(name = "started_at", nullable = false)
    private Instant startedAt;
    @Column(name = "completed_at")
    private Instant completedAt;
    @Column(name = "lease_expires_at", nullable = false)
    private Instant leaseExpiresAt;
    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;
    @Column(name = "successful_recipient_count", nullable = false)
    private int successfulRecipientCount;
    @Column(name = "failed_recipient_count", nullable = false)
    private int failedRecipientCount;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
