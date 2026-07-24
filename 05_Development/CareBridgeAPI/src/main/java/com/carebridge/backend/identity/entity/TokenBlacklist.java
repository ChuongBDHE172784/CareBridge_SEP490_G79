package com.carebridge.backend.identity.entity;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "auth_revocations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenBlacklist {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "revocation_id")
    private UUID id;

    @Column(name = "token_hash", nullable = false, unique = true, length = 255)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at", nullable = false)
    private Instant revokedAt;

    @Column(name = "reason", length = 100)
    private String reason; // "session_revoke", "logout", "admin_action"

    @jakarta.persistence.PrePersist
    void canonicalDefaults() {
        if (revokedAt == null) revokedAt = Instant.now();
        if (reason == null || reason.isBlank()) reason = "TOKEN_REVOKED";
    }
}
