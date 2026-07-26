package com.carebridge.backend.identity.entity;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenBlacklist {

    private UUID id;

    private String tokenHash;

    private Instant expiresAt;

    private Instant revokedAt;

    private String reason; // "session_revoke", "logout", "admin_action"

    private UUID tokenFamilyId;

    private String deviceIdentifier;

    private Instant issuedAt;

    private String status;

    @Builder.Default
    private boolean detectedReuse = false;

    public void canonicalDefaults() {
        if (revokedAt == null) revokedAt = Instant.now();
        if (reason == null || reason.isBlank()) reason = "TOKEN_REVOKED";
        status = "REVOKED";
    }
}
