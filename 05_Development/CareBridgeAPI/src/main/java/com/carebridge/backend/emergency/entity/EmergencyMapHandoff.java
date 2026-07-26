package com.carebridge.backend.emergency.entity;

import com.carebridge.backend.emergency.handoffstatus.HandoffStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "safety_events")
@org.hibernate.annotations.SQLRestriction("action_type = 'MAP_HANDOFF'")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmergencyMapHandoff {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "safety_event_id", updatable = false, nullable = false)
    private UUID handoffId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "safety_event_id")
    private UUID safetyEventId;

    @Column(name = "triage_handoff_id")
    private UUID triageHandoffId;

    @Column(name = "risk_level", length = 20)
    private String riskLevel;

    @Column(name = "latitude", precision = 10, scale = 8)
    private BigDecimal userLatitude;

    @Column(name = "longitude", precision = 11, scale = 8)
    private BigDecimal userLongitude;

    @Column(name = "care_facility_id")
    private UUID selectedFacilityId;

    @Column(columnDefinition = "text")
    private String summary;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_status", nullable = false, length = 20)
    private HandoffStatus status = HandoffStatus.OPEN;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Builder.Default
    @Column(name = "action_type", nullable = false, updatable = false)
    private String actionType = "MAP_HANDOFF";
    @Column(name = "idempotency_key", nullable = false, updatable = false)
    private String idempotencyKey;
    @Builder.Default
    @Column(name = "event_type", nullable = false, updatable = false)
    private String eventType = "ACTION";
    @Column(name = "detected_at", nullable = false, updatable = false)
    private Instant detectedAt;

    @PrePersist
    void prepareCanonicalAction() {
        actionType = "MAP_HANDOFF";
        if (idempotencyKey == null) idempotencyKey = "map-handoff:" + UUID.randomUUID();
        if (detectedAt == null) detectedAt = createdAt == null ? Instant.now() : createdAt;
    }
}
