package com.carebridge.backend.integration.zegocloud;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ZegoCloudServiceImpl implements IZegoCloudService {

    private final ZegoToken04Generator tokenGenerator;

    @Value("${carebridge.zego.app-id}")
    private long appId;

    @Value("${carebridge.zego.server-secret}")
    private String serverSecret;

    @Value("${carebridge.zego.token-ttl-seconds:3600}")
    private int tokenTtlSeconds;

    @Override
    public ZegoTokenDto generateToken(String sessionId, String userId, String userName) {
        String token = tokenGenerator.generate(appId, userId, serverSecret, tokenTtlSeconds);
        return new ZegoTokenDto(sessionId, token, appId, Instant.now().plusSeconds(tokenTtlSeconds));
    }
}
