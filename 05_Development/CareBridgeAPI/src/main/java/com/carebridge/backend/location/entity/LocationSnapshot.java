package com.carebridge.backend.location.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "safety_events")
@org.hibernate.annotations.SQLRestriction("action_type = 'LOCATION_SNAPSHOT'")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "safety_event_id", updatable = false, nullable = false)
    private UUID locationSnapshotId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "context_type", length = 50)
    private String contextType;

    @Column(name = "context_id")
    private UUID contextId;

    @Column(nullable = false, precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 11, scale = 8)
    private BigDecimal longitude;

    @Column(name = "accuracy_meters", precision = 6, scale = 2)
    private BigDecimal accuracyMeters;

    @Column(name = "captured_at", nullable = false)
    @CreationTimestamp
    private LocalDateTime capturedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "consent_status", length = 20)
    private String consentStatus;

    @Builder.Default
    @Column(name = "action_type", nullable = false, updatable = false)
    private String actionType = "LOCATION_SNAPSHOT";

    @Column(name = "idempotency_key", nullable = false, updatable = false)
    private String idempotencyKey;

    @Builder.Default
    @Column(name = "event_type", nullable = false, updatable = false, length = 50)
    private String eventType = "ACTION";

    @Column(name = "detected_at", nullable = false, updatable = false)
    private LocalDateTime detectedAt;

    @PrePersist
    void prepareCanonicalAction() {
        actionType = "LOCATION_SNAPSHOT";
        if (idempotencyKey == null) idempotencyKey = "location:" + UUID.randomUUID();
        if (detectedAt == null) detectedAt = capturedAt == null ? LocalDateTime.now() : capturedAt;
    }
}
