package com.carebridge.backend.emergency.entity;

import com.carebridge.backend.emergency.EmergencyStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "safety_events")
@org.hibernate.annotations.SQLRestriction("record_type = 'EMERGENCY_SESSION'")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmergencySession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "safety_event_id")
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "source_event_id")
    private UUID sourceEventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EmergencyStatus status;

    @Column(name = "event_type", nullable = false, length = 50)
    private String triggerSource;

    @Column(name = "user_latitude", precision = 10, scale = 7)
    private BigDecimal userLatitude;

    @Column(name = "user_longitude", precision = 10, scale = 7)
    private BigDecimal userLongitude;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "created_by_user_id")
    private UUID createdBy;

    @Builder.Default
    @Column(name = "alert_generation", nullable = false)
    private long alertGeneration = 0;

    @Column(name = "alert_status", length = 20)
    private String alertStatus;

    @Column(name = "alert_claim_token")
    private UUID alertClaimToken;

    @Column(name = "alert_claimed_at")
    private Instant alertClaimedAt;

    @Column(name = "alert_lease_expires_at")
    private Instant alertLeaseExpiresAt;

    @Column(name = "alert_completed_at")
    private Instant alertCompletedAt;

    @Builder.Default
    @Column(name = "alert_successful_recipient_count", nullable = false)
    private int alertSuccessfulRecipientCount = 0;

    @Builder.Default
    @Column(name = "alert_failed_recipient_count", nullable = false)
    private int alertFailedRecipientCount = 0;

    @Column(name = "alert_updated_at")
    private Instant alertUpdatedAt;

    @Builder.Default
    @Column(name = "record_type", nullable = false, updatable = false)
    private String recordType = "EMERGENCY_SESSION";

    @PrePersist
    void prepareCanonicalEvent() {
        recordType = "EMERGENCY_SESSION";
    }
}
