package com.carebridge.backend.directchat.dto.response;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ZegoJoinCredentialsResponse {
    private final long appId;
    private final String roomId;
    private final String userId;
    private final String displayName;
    private final String token;
    private final Instant expiresAt;
}
