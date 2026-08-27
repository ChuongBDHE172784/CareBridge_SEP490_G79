package com.carebridge.backend.security.config;

import com.carebridge.backend.security.jwt.JwtAuthenticationFilter;
import com.carebridge.backend.systemconfiguration.security.MaintenanceModeFilter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final MaintenanceModeFilter maintenanceModeFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            CorsConfigurationSource corsConfigurationSource) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Only DB-backed readiness is anonymous for the container
                        // orchestrator. Root health, liveness, and every other
                        // actuator operation remain denied.
                        .requestMatchers(HttpMethod.GET, "/actuator/health/readiness").permitAll()
                        .requestMatchers(EndpointRequest.toAnyEndpoint()).denyAll()
                        // Auth endpoints
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/auth/register",
                                "/api/v1/auth/login",
                                "/api/v1/auth/federated",
                                "/api/v1/auth/phone/register",
                                "/api/v1/auth/phone/login",
                                "/api/v1/auth/verify-otp",
                                "/api/v1/auth/resend-otp",
                                "/api/v1/auth/refresh",
                                "/api/v1/auth/forgot-password",
                                "/api/v1/auth/reset-password",
                                "/api/v1/auth/lock-appeals")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/profile").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/v1/auth/profile").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/logout").authenticated()
                        // Master data endpoints (public)
                        .requestMatchers(HttpMethod.GET, "/api/v1/master-data/**").permitAll()
                        // All community and content read endpoints require a valid JWT.
                        // Unauthenticated requests → 401 (per CNT82-TC-SEC-001, ADR-COM-*)
                        // Admin / privileged write endpoints
                        .requestMatchers("/api/v1/consent/grants/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/admin/audit-logs")
                        .hasAnyRole("SYSTEM_ADMIN", "OPERATIONS")
                        .requestMatchers(HttpMethod.GET, "/api/v1/moderator/community/dashboard").hasRole("MODERATOR")
                        .requestMatchers(HttpMethod.GET, "/api/v1/admin/moderation/queue").hasRole("MODERATOR")
                        // CB-MOD-IMP-004: added retroactively — @PreAuthorize + the /api/v1/** fallback
                        // below already enforced MODERATOR-only, so this was a convention gap, not a
                        // hole.
                        .requestMatchers(HttpMethod.GET, "/api/v1/admin/moderation/pending-content")
                        .hasRole("MODERATOR")
                        .requestMatchers(HttpMethod.GET, "/api/v1/admin/moderation/history").hasRole("MODERATOR")
                        .requestMatchers(HttpMethod.POST, "/api/v1/admin/moderation/actions").hasRole("MODERATOR")
                        .requestMatchers(HttpMethod.POST, "/api/v1/admin/moderation/reports/*/resolve")
                        .hasRole("MODERATOR")
                        .requestMatchers(HttpMethod.POST, "/api/v1/admin/moderation/account-actions")
                        .hasRole("MODERATOR")
                        .requestMatchers(HttpMethod.POST, "/api/v1/admin/content").hasRole("CONTENT_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/admin/content/*").hasRole("CONTENT_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/admin/content/*/archive").hasRole("CONTENT_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/admin/content/*/unpublish").hasRole("CONTENT_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/admin/content/*/decision").hasAnyRole("SYSTEM_ADMIN", "EXPERT")
                        .requestMatchers(HttpMethod.POST, "/api/v1/admin/content/*/reassign").hasRole("SYSTEM_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/admin/checklist-templates/*/decision").hasAnyRole("SYSTEM_ADMIN", "EXPERT")
                        .requestMatchers(HttpMethod.POST, "/api/v1/admin/checklist-templates/*/reassign").hasRole("SYSTEM_ADMIN")
                        .requestMatchers("/api/v1/expert/content-approval/**").hasRole("EXPERT")
                        .requestMatchers(HttpMethod.POST, "/api/v1/community/topics")
                        .hasAnyRole("MODERATOR", "CONTENT_ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/community/topics/**")
                        .hasAnyRole("MODERATOR", "CONTENT_ADMIN")
                        .requestMatchers("/api/v1/**").authenticated()
                        .anyRequest().permitAll())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(maintenanceModeFilter, JwtAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public FilterRegistrationBean<MaintenanceModeFilter> maintenanceModeRegistration() {
        FilterRegistrationBean<MaintenanceModeFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(maintenanceModeFilter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("#{'${carebridge.cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173,http://localhost:5000,http://127.0.0.1:5000,http://localhost:3000,http://127.0.0.1:3000}'.split(',')}") List<String> allowedOrigins) {
        if (allowedOrigins == null) {
            throw new IllegalStateException("At least one exact CORS origin is required");
        }
        List<String> validatedOrigins = allowedOrigins.stream()
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .map(SecurityConfig::validateExactCorsOrigin)
                .distinct()
                .toList();
        if (validatedOrigins.isEmpty()) {
            throw new IllegalStateException("At least one exact CORS origin is required");
        }

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(validatedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/v1/**", configuration);
        // The transition-only internal triage alias lives outside /api/v1, so without this it carried no
        // Access-Control-Allow-Origin at all and every browser call failed as an opaque network
        // error — the client could not even see that it had been given a 403. This grants no
        // authorization: the same exact-origin allowlist applies, and @PreAuthorize still guards
        // the endpoints.
        source.registerCorsConfiguration("/api/internal/**", configuration);
        return source;
    }

    private static String validateExactCorsOrigin(String origin) {
        if (origin.contains("*")) {
            throw new IllegalArgumentException("CORS origins must not contain wildcards: " + origin);
        }

        final URI uri;
        try {
            uri = new URI(origin);
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException("Invalid CORS origin: " + origin, exception);
        }

        String scheme = uri.getScheme();
        boolean validScheme = "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
        boolean hasPath = uri.getRawPath() != null && !uri.getRawPath().isEmpty();
        if (!validScheme
                || uri.getHost() == null
                || uri.getRawUserInfo() != null
                || hasPath
                || uri.getRawQuery() != null
                || uri.getRawFragment() != null) {
            throw new IllegalArgumentException(
                    "CORS origin must be an exact HTTP(S) origin without credentials, path, query, or fragment: "
                            + origin);
        }
        return origin;
    }
}
