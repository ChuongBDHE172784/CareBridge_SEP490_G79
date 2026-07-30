package com.carebridge.backend.identity.admin.dto.response;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Privacy-minimized session projection for SYSTEM_ADMIN monitoring. */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserSessionResponse {
    private UUID id;
    private String deviceName;
    private String status;
    private Instant issuedAt;
    private Instant lastActivityAt;
    private Instant expiresAt;
    private Instant revokedAt;
}
