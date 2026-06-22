package com.carebridge.backend.safety.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "safety_monitoring_settings")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SafetyMonitoringSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "setting_id")
    private UUID settingId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "is_enabled")
    private Boolean isEnabled;

    @Column(name = "countdown_seconds")
    private Integer countdownSeconds;

    @Column(name = "location_sharing_enabled")
    private Boolean locationSharingEnabled;

    @Column(name = "emergency_contact_user_id")
    private UUID emergencyContactUserId;

    @Column(name = "monitoring_schedule_json", columnDefinition = "jsonb")
    private String monitoringScheduleJson;

    @Column(name = "sensor_consent_at")
    private Instant sensorConsentAt;

    @Column(name = "location_consent_at")
    private Instant locationConsentAt;

    @Column(name = "disclaimer_version", length = 50)
    private String disclaimerVersion;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
