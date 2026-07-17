package com.carebridge.backend.integration.zegocloud;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Base64;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ZegoToken04GeneratorTest {

    private final ZegoToken04Generator generator = new ZegoToken04Generator();
    private static final String VALID_SECRET = "12345678901234567890123456789012"; // 32 chars

    @Test
    @DisplayName("ZEGO-TC-001: token starts with version flag '04' and is base64 after that")
    void generate_startsWithVersionFlag() {
        String token = generator.generate(123456L, "user-1", VALID_SECRET, 3600);
        assertThat(token).startsWith("04");
        assertThat(Base64.getDecoder().decode(token.substring(2))).isNotEmpty();
    }

    @Test
    @DisplayName("ZEGO-TC-002: two calls produce different tokens (random IV/nonce each time, never cached/reused)")
    void generate_producesDifferentTokensEachCall() {
        String token1 = generator.generate(123456L, "user-1", VALID_SECRET, 3600);
        String token2 = generator.generate(123456L, "user-1", VALID_SECRET, 3600);
        assertThat(token1).isNotEqualTo(token2);
    }

    @Test
    @DisplayName("ZEGO-TC-005a: illegal appId (<= 0) rejected")
    void generate_illegalAppId_rejected() {
        assertThatThrownBy(() -> generator.generate(0, "user-1", VALID_SECRET, 3600))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("ZEGO-TC-005b: blank userId rejected")
    void generate_blankUserId_rejected() {
        assertThatThrownBy(() -> generator.generate(1, " ", VALID_SECRET, 3600))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("ZEGO-TC-005c: userId over 64 chars rejected")
    void generate_userIdTooLong_rejected() {
        String longId = "a".repeat(65);
        assertThatThrownBy(() -> generator.generate(1, longId, VALID_SECRET, 3600))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("ZEGO-TC-005d: serverSecret not exactly 32 chars rejected")
    void generate_illegalSecretLength_rejected() {
        assertThatThrownBy(() -> generator.generate(1, "user-1", "too-short", 3600))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("ZEGO-TC-005e: non-positive effectiveSeconds rejected")
    void generate_illegalEffectiveTime_rejected() {
        assertThatThrownBy(() -> generator.generate(1, "user-1", VALID_SECRET, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
