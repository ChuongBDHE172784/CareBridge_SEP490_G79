package com.carebridge.backend.emergency.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "emergency_notification_outbox")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmergencyNotificationOutbox {

    public static final String PENDING = "PENDING";
    public static final String DELIVERED = "DELIVERED";
    public static final String SUPPRESSED = "SUPPRESSED";

    @Id
    @Column(name = "emergency_session_id", nullable = false)
    private UUID emergencySessionId;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "last_error_code", length = 120)
    private String lastErrorCode;

    @Column(name = "claim_token")
    private UUID claimToken;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "terminal_at")
    private Instant terminalAt;
}
