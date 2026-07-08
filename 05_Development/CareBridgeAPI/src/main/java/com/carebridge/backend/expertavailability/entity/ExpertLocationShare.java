package com.carebridge.backend.expertavailability.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "expert_location_shares")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpertLocationShare {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "location_share_id", updatable = false, nullable = false)
    private UUID locationShareId;

    @Column(name = "expert_profile_id", nullable = false)
    private UUID expertProfileId;

    @Column(nullable = false, precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 11, scale = 8)
    private BigDecimal longitude;

    @Column(name = "accuracy_meters", precision = 6, scale = 2)
    private BigDecimal accuracyMeters;

    @Column(name = "availability_status", length = 20)
    private String availabilityStatus;

    @Column(name = "shared_at", nullable = false)
    @CreationTimestamp
    private LocalDateTime sharedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "consent_reference")
    private UUID consentReference;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
