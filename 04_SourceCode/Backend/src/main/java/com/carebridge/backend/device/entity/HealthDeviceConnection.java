package com.carebridge.backend.device.entity;

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
@Table(name = "health_device_connections")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthDeviceConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "connection_id")
    private UUID connectionId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "provider_name", length = 80)
    private String providerName;

    @Column(name = "device_name", length = 150)
    private String deviceName;

    @Column(name = "scopes_json", columnDefinition = "jsonb")
    private String scopesJson;

    @Column(name = "token_reference", length = 255)
    private String tokenReference;

    @Column(name = "consent_granted_at")
    private Instant consentGrantedAt;

    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
