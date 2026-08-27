package com.carebridge.backend.integration.zegocloud;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ZegoTokenDto {
    private final String roomId;
    private final String token;
    private final long appId;
    private final Instant expiresAt;
}
