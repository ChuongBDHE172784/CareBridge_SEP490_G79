package com.carebridge.backend.expert.entity;

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
@Table(name = "expert_location_shares")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpertLocationShare {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "location_share_id")
    private UUID locationShareId;

    @Column(name = "expert_profile_id")
    private UUID expertProfileId;

    @Column(name = "latitude")
    private BigDecimal latitude;

    @Column(name = "longitude")
    private BigDecimal longitude;

    @Column(name = "accuracy_meters")
    private BigDecimal accuracyMeters;

    @Column(name = "availability_status", length = 20)
    private String availabilityStatus;

    @Column(name = "shared_at")
    private Instant sharedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "consent_reference")
    private UUID consentReference;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
