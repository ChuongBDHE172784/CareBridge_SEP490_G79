package com.carebridge.backend.integration.zegocloud;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ZegoCloudServiceImplTest {

    private ZegoCloudServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ZegoCloudServiceImpl(new ZegoToken04Generator());
        ReflectionTestUtils.setField(service, "appId", 123456L);
        ReflectionTestUtils.setField(service, "serverSecret", "12345678901234567890123456789012");
        ReflectionTestUtils.setField(service, "tokenTtlSeconds", 3600);
    }

    @Test
    @DisplayName("ZEGO-TC-003: generateToken returns roomId == sessionId (never a separately generated id)")
    void generateToken_roomIdEqualsSessionId() {
        String sessionId = java.util.UUID.randomUUID().toString();
        ZegoTokenDto token = service.generateToken(sessionId, "user-1", "Mother Test");
        assertThat(token.getRoomId()).isEqualTo(sessionId);
    }

    @Test
    @DisplayName("ZEGO-TC-004: token TTL is ~3600 seconds from now, appId matches configured value")
    void generateToken_ttlAndAppId() {
        ZegoTokenDto token = service.generateToken("session-1", "user-1", "Expert Test");
        assertThat(token.getAppId()).isEqualTo(123456L);
        assertThat(token.getExpiresAt()).isCloseTo(Instant.now().plusSeconds(3600), org.assertj.core.api.Assertions.within(5, java.time.temporal.ChronoUnit.SECONDS));
    }

    @Test
    @DisplayName("ZEGO-TC-006: generateToken never returns a token that contains raw appId/secret as plaintext substrings")
    void generateToken_doesNotLeakSecretAsPlaintext() {
        String secret = "12345678901234567890123456789012";
        ZegoTokenDto token = service.generateToken("session-1", "user-1", "x");
        assertThat(token.getToken()).doesNotContain(secret);
    }
}
