package com.carebridge.backend.systemconfiguration.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "system_configurations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "system_configuration_id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "api_rate_limit", nullable = false)
    private int apiRateLimit;

    @Column(name = "connection_timeout_ms", nullable = false)
    private int connectionTimeoutMs;

    @Column(name = "max_upload_size_mb", nullable = false)
    private int maxUploadSizeMb;

    @Column(name = "administrator_email", nullable = false, length = 254)
    private String administratorEmail;

    @Column(name = "email_alerts", nullable = false)
    private boolean emailAlerts;

    @Column(name = "sms_alerts", nullable = false)
    private boolean smsAlerts;

    @Column(name = "webhook_alerts", nullable = false)
    private boolean webhookAlerts;

    @Column(name = "ai_moderation_enabled", nullable = false)
    private boolean aiModerationEnabled;

    @Column(name = "maintenance_mode_enabled", nullable = false)
    private boolean maintenanceModeEnabled;

    @Column(name = "updated_by", nullable = false)
    private UUID updatedBy;

    @Version
    @Column(name = "row_version", nullable = false)
    private long rowVersion;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
