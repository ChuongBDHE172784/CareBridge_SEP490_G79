package com.carebridge.backend.security.config;

import com.carebridge.backend.security.jwt.JwtAuthenticationFilter;
import com.carebridge.backend.baby.security.BabyLinkBoundaryAuditFilter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
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
    private final ObjectProvider<BabyLinkBoundaryAuditFilter> babyLinkBoundaryAuditFilterProvider;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Auth endpoints
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/auth/register",
                                "/api/v1/auth/login",
                                "/api/v1/auth/login-direct",
                                "/api/v1/auth/federated",
                                "/api/v1/auth/verify-otp",
                                "/api/v1/auth/resend-otp",
                                "/api/v1/auth/refresh",
                                "/api/v1/auth/forgot-password",
                                "/api/v1/auth/reset-password").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/profile").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/v1/auth/profile").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/logout").authenticated()
                        // All community and content read endpoints require a valid JWT.
                        // Unauthenticated requests → 401 (per CNT82-TC-SEC-001, ADR-COM-*)
                        // Admin / privileged write endpoints
                        .requestMatchers("/api/v1/consent/grants/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/admin/audit-logs").hasRole("SYSTEM_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/admin/community/dashboard").hasRole("SYSTEM_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/admin/moderation/queue").hasRole("MODERATOR")
                        // CB-MOD-IMP-004: added retroactively — @PreAuthorize + the /api/v1/** fallback
                        // below already enforced MODERATOR-only, so this was a convention gap, not a hole.
                        .requestMatchers(HttpMethod.GET, "/api/v1/admin/moderation/pending-content").hasRole("MODERATOR")
                        .requestMatchers(HttpMethod.GET, "/api/v1/admin/moderation/history").hasRole("MODERATOR")
                        .requestMatchers(HttpMethod.POST, "/api/v1/admin/moderation/actions").hasRole("MODERATOR")
                        .requestMatchers(HttpMethod.POST, "/api/v1/admin/moderation/reports/*/resolve").hasRole("MODERATOR")
                        .requestMatchers(HttpMethod.POST, "/api/v1/admin/moderation/account-actions").hasRole("MODERATOR")
                        .requestMatchers(HttpMethod.POST, "/api/v1/admin/content").hasRole("CONTENT_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/admin/content/*").hasRole("CONTENT_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/admin/content/*/archive").hasRole("CONTENT_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/admin/content/*/unpublish").hasRole("CONTENT_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/admin/content/*/decision").hasRole("SYSTEM_ADMIN")
                        .requestMatchers("/api/v1/admin/content/categories/**").hasRole("CONTENT_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/community/topics").hasAnyRole("MODERATOR", "CONTENT_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/partner/profile").hasRole("PARTNER")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/partner/profile").hasRole("PARTNER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/admin/partners/*/decision").hasRole("SYSTEM_ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/community/topics/**").hasAnyRole("MODERATOR", "CONTENT_ADMIN")
                        .requestMatchers("/api/v1/**").authenticated()
                        .anyRequest().permitAll())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        BabyLinkBoundaryAuditFilter boundaryAuditFilter = babyLinkBoundaryAuditFilterProvider.getIfAvailable();
        if (boundaryAuditFilter != null) {
            http.addFilterAfter(boundaryAuditFilter, JwtAuthenticationFilter.class);
        }
        return http.build();
    }

    @Bean
    public FilterRegistrationBean<BabyLinkBoundaryAuditFilter> babyLinkBoundaryAuditRegistration() {
        FilterRegistrationBean<BabyLinkBoundaryAuditFilter> registration = new FilterRegistrationBean<>();
        BabyLinkBoundaryAuditFilter boundaryAuditFilter = babyLinkBoundaryAuditFilterProvider.getIfAvailable();
        if (boundaryAuditFilter != null) {
            registration.setFilter(boundaryAuditFilter);
        }
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // allowedOriginPatterns supports wildcards and is compatible with allowCredentials=true
        configuration.setAllowedOriginPatterns(List.of("http://localhost:*", "http://192.168.*.*:*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/v1/**", configuration);
        return source;
    }
}
