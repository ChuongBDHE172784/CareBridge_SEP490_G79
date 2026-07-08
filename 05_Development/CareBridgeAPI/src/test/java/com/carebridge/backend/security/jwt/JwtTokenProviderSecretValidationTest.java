package com.carebridge.backend.security.jwt;

import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.rbac.Role;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderSecretValidationTest {

    @Test
    void init_WhenSecretMissing_ThrowsIllegalStateException() {
        JwtTokenProvider provider = new JwtTokenProvider();
        try {
            var field = JwtTokenProvider.class.getDeclaredField("configuredSecret");
            field.setAccessible(true);
            field.set(provider, null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        assertThatThrownBy(provider::init)
                .isInstanceOf(IllegalStateException.class)
                .message().contains("JWT_SECRET is not configured");
    }

    @Test
    void init_WhenSecretBlank_ThrowsIllegalStateException() {
        JwtTokenProvider provider = new JwtTokenProvider();
        try {
            var field = JwtTokenProvider.class.getDeclaredField("configuredSecret");
            field.setAccessible(true);
            field.set(provider, "   ");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        assertThatThrownBy(provider::init)
                .isInstanceOf(IllegalStateException.class)
                .message().contains("JWT_SECRET is not configured");
    }

    @Test
    void init_WhenSecretTooShort_ThrowsIllegalStateException() {
        JwtTokenProvider provider = new JwtTokenProvider();
        try {
            var field = JwtTokenProvider.class.getDeclaredField("configuredSecret");
            field.setAccessible(true);
            field.set(provider, "short");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        assertThatThrownBy(provider::init)
                .isInstanceOf(IllegalStateException.class)
                .message().contains("too weak");
    }

    @Test
    void init_WhenSecretValid_DoesNotThrow() {
        JwtTokenProvider provider = new JwtTokenProvider();
        try {
            var field = JwtTokenProvider.class.getDeclaredField("configuredSecret");
            field.setAccessible(true);
            field.set(provider, "this-is-a-very-long-secret-key-that-is-at-least-32-chars");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        // Should not throw
        provider.init();
    }

    @Test
    @DisplayName("LOGIN-TC-007: Access token TTL is exactly 900 seconds (exp - iat)")
    void generateAccessToken_hasExactly900SecondTtl() {
        JwtTokenProvider provider = new JwtTokenProvider();
        ReflectionTestUtils.setField(provider, "configuredSecret",
                "this-is-a-very-long-secret-key-that-is-at-least-32-chars");
        ReflectionTestUtils.setField(provider, "accessTokenExpirationMs", 900_000L);
        provider.init();

        User user = User.builder()
                .id(UUID.randomUUID())
                .phone("+84912345678")
                .role(Role.MOTHER)
                .build();

        String token = provider.generateAccessToken(user);

        String[] parts = token.split("\\.");
        assertThat(parts).hasSize(3);
        String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);

        long iat = extractNumericClaim(payloadJson, "iat");
        long exp = extractNumericClaim(payloadJson, "exp");

        assertThat(exp - iat).isEqualTo(900L);
        assertThat(payloadJson).contains("\"sub\":\"" + user.getId());
        assertThat(payloadJson).contains("\"role\":\"" + Role.MOTHER.getAuthority() + "\"");
    }

    private static long extractNumericClaim(String json, String key) {
        String marker = "\"" + key + "\":";
        int start = json.indexOf(marker) + marker.length();
        int end = start;
        while (end < json.length() && Character.isDigit(json.charAt(end))) {
            end++;
        }
        return Long.parseLong(json.substring(start, end));
    }
}
