package com.carebridge.backend.emergency.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "location_snapshots")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "location_snapshot_id")
    private UUID locationSnapshotId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "context_type", length = 30)
    private String contextType;

    @Column(name = "context_id")
    private UUID contextId;

    @Column(name = "latitude")
    private BigDecimal latitude;

    @Column(name = "longitude")
    private BigDecimal longitude;

    @Column(name = "accuracy_meters")
    private BigDecimal accuracyMeters;

    @Column(name = "captured_at")
    private Instant capturedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "consent_status", length = 20)
    private String consentStatus;
}
