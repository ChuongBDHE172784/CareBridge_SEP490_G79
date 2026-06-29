package com.carebridge.backend.expert.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Expert location share entity.
 * Stores location sharing information for experts during consultations.
 *
 * Used for emergency services and real-time location tracking.
 */
@Entity
@Table(name = "expert_location_shares")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpertLocationShare {

    /**
     * Primary key - auto-generated location share ID.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "location_share_id")
    private Long locationShareId;

    /**
     * Foreign key to expert_profiles table.
     * ERD relationship: expert_profiles ||--o{ expert_location_shares : "shares"
     */
    @Column(name = "expert_profile_id", nullable = false)
    private Long expertProfileId;

    /**
     * Latitude coordinate in decimal degrees (WGS84).
     * Range: -90 to 90
     */
    @Column(name = "latitude", nullable = false, precision = 10, scale = 8)
    private Double latitude;

    /**
     * Longitude coordinate in decimal degrees (WGS84).
     * Range: -180 to 180
     */
    @Column(name = "longitude", nullable = false, precision = 11, scale = 8)
    private Double longitude;

    /**
     * Accuracy of the location in meters.
     * Lower values indicate more precise location.
     */
    @Column(name = "accuracy_meters")
    private Integer accuracyMeters;

    /**
     * Current availability status of the expert.
     * Example values: AVAILABLE, BUSY, OFFLINE, EMERGENCY_ONLY
     */
    @Column(name = "availability_status", length = 50)
    private String availabilityStatus;

    /**
     * Timestamp when the location was shared.
     */
    @Column(name = "shared_at")
    private Instant sharedAt;

    /**
     * Timestamp when the location share expires.
     * Location sharing automatically stops after this time.
     */
    @Column(name = "expires_at")
    private Instant expiresAt;

    /**
     * Reference to consent record (consent_id from data_permissions).
     * Tracks user consent for location sharing.
     */
    @Column(name = "consent_reference")
    private Long consentReference;

    /**
     * Creation timestamp.
     * Automatically set when the record is first inserted.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Last update timestamp.
     * Automatically updated on any modification.
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
