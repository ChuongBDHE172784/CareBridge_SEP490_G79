package com.carebridge.backend.identity.dto;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionInfo {
    private UUID sessionId;
    private String deviceName;
    private String browser;
    private String ipAddress;
    private String location;
    private Instant lastActivityAt;
    private boolean isCurrent;
    private String status; // "active" or "inactive" or "revoked"
}
