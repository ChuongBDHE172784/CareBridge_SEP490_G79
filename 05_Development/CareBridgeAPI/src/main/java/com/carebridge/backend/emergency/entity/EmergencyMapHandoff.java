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
@Table(name = "emergency_map_handoffs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmergencyMapHandoff {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "handoff_id", updatable = false, nullable = false)
    private UUID handoffId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "triage_handoff_id")
    private UUID triageHandoffId;

    @Column(name = "risk_level", length = 20)
    private String riskLevel;

    @Column(name = "user_latitude", precision = 10, scale = 8)
    private BigDecimal userLatitude;

    @Column(name = "user_longitude", precision = 11, scale = 8)
    private BigDecimal userLongitude;

    @Column(name = "selected_facility_id")
    private UUID selectedFacilityId;

    @Column(columnDefinition = "text")
    private String summary;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private HandoffStatus status = HandoffStatus.OPEN;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
