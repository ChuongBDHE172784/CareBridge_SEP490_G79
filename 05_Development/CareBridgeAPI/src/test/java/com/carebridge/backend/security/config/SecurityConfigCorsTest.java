package com.carebridge.backend.security.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

class SecurityConfigCorsTest {

    @Test
    void corsConfigurationSource_UsesOnlyConfiguredExactOrigins() {
        SecurityConfig config = new SecurityConfig(null, null);
        CorsConfigurationSource source = config.corsConfigurationSource(List.of(
                " https://portal.dev.carebridge.example ",
                "https://portal.carebridge.example"));

        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/v1/auth/login");
        CorsConfiguration cors = source.getCorsConfiguration(request);

        assertThat(cors).isNotNull();
        assertThat(cors.getAllowedOrigins()).containsExactly(
                "https://portal.dev.carebridge.example",
                "https://portal.carebridge.example");
        assertThat(cors.getAllowedOriginPatterns()).isNullOrEmpty();
        assertThat(cors.getAllowCredentials()).isTrue();
        assertThat(cors.checkOrigin("https://portal.dev.carebridge.example"))
                .isEqualTo("https://portal.dev.carebridge.example");
        assertThat(cors.checkOrigin("https://untrusted.example")).isNull();
    }

    @Test
    void corsConfigurationSource_RejectsEmptyOrigins() {
        SecurityConfig config = new SecurityConfig(null, null);

        assertThatThrownBy(() -> config.corsConfigurationSource(List.of(" ", "")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("At least one exact CORS origin");
    }

    @Test
    void corsConfigurationSource_RejectsWildcardOrigins() {
        SecurityConfig config = new SecurityConfig(null, null);

        assertThatThrownBy(() -> config.corsConfigurationSource(List.of("https://*.example.com")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not contain wildcards");
    }

    @Test
    void corsConfigurationSource_RejectsMalformedOrNonOriginValues() {
        SecurityConfig config = new SecurityConfig(null, null);

        assertThatThrownBy(() -> config.corsConfigurationSource(List.of("ftp://portal.example.com")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> config.corsConfigurationSource(List.of("https://portal.example.com/path")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> config.corsConfigurationSource(List.of("https://portal.example.com?debug=true")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> config.corsConfigurationSource(List.of("not an origin")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
