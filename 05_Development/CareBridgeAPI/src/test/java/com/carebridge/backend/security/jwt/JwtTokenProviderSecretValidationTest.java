package com.carebridge.backend.security.jwt;

import org.junit.jupiter.api.Test;

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
}
