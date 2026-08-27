package com.carebridge.backend.identity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;

import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "auth_sessions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSession {

    @Id
    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "refresh_token_hash", nullable = false, length = 255)
    private String refreshTokenHash;

    @Column(name = "device_name", length = 150)
    private String deviceName;

    @Transient
    private String ipAddress;

    @Transient
    private String browser;

    @Transient
    private String location;

    @Column(name = "last_used_at")
    private Instant lastActivityAt;

    @Transient
    @Builder.Default
    private boolean isCurrent = false;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Transient
    private boolean revoked;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "issued_at", nullable = false)
    private Instant createdAt;

    @Transient
    private Instant updatedAt;

    @Column(name = "token_family_id", nullable = false)
    private UUID tokenFamilyId;

    @Column(name = "device_identifier", nullable = false, length = 255)
    private String deviceIdentifier;

    public boolean isRevoked() { return revoked || revokedAt != null || "REVOKED".equalsIgnoreCase(status); }
    public void setRevoked(boolean revoked) {
        this.revoked = revoked;
        revokedAt = revoked ? Instant.now() : null;
        if (revoked) status = "REVOKED";
    }

    @PrePersist
    void canonicalDefaults() {
        if (sessionId == null) sessionId = UUID.randomUUID();
        if (tokenFamilyId == null) tokenFamilyId = sessionId;
        if (deviceIdentifier == null || deviceIdentifier.isBlank()) {
            deviceIdentifier = deviceName == null || deviceName.isBlank() ? sessionId.toString() : deviceName;
        }
        if (createdAt == null) createdAt = Instant.now();
        if (status == null) status = "ACTIVE"; else status = status.toUpperCase();
    }
}
