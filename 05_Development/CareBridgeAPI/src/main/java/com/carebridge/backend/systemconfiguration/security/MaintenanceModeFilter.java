package com.carebridge.backend.systemconfiguration.security;

import com.carebridge.backend.common.response.ErrorResponse;
import com.carebridge.backend.systemconfiguration.service.SystemMaintenanceModeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Blocks normal API traffic while preserving recovery and System Admin control paths. */
@Component
@RequiredArgsConstructor
public class MaintenanceModeFilter extends OncePerRequestFilter {

    private static final Set<String> RECOVERY_REQUESTS = Set.of(
            "POST /api/v1/auth/login",
            "POST /api/v1/auth/login-direct",
            "POST /api/v1/auth/federated",
            "POST /api/v1/auth/verify-otp",
            "POST /api/v1/auth/resend-otp",
            "POST /api/v1/auth/refresh",
            "POST /api/v1/auth/logout",
            "POST /api/v1/auth/forgot-password",
            "POST /api/v1/auth/reset-password",
            "GET /api/v1/auth/profile",
            "GET /api/v1/admin/system-configuration",
            "PUT /api/v1/admin/system-configuration",
            "GET /actuator/health/readiness");

    private final SystemMaintenanceModeService maintenanceModeService;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = normalizedPath(request);
        return HttpMethod.OPTIONS.matches(request.getMethod())
                || RECOVERY_REQUESTS.contains(request.getMethod() + " " + path)
                || !path.startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        if (isSystemAdmin() || !maintenanceModeService.isMaintenanceEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Retry-After", "2");
        response.setHeader("Cache-Control", "no-store");
        objectMapper.writeValue(response.getWriter(), ErrorResponse.of(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                "SYSTEM_MAINTENANCE",
                "CareBridge is temporarily unavailable for scheduled maintenance.",
                request.getRequestURI()));
    }

    private String normalizedPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (path == null || path.isBlank()) {
            path = "/";
        } else if (contextPath != null && !contextPath.isBlank() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        while (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    private boolean isSystemAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_SYSTEM_ADMIN"::equals);
    }
}
