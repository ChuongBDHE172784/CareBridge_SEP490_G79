package com.carebridge.backend.security.jwt;

import com.carebridge.backend.common.constants.SecurityConstants;
import com.carebridge.backend.common.response.ErrorResponse;
import com.carebridge.backend.identity.entity.UserSession;
import com.carebridge.backend.identity.repository.UserSessionRepository;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final ObjectMapper MAPPER =
            new ObjectMapper().registerModule(new JavaTimeModule());

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final ObjectProvider<UserSessionRepository> userSessionRepositoryProvider;

    /** Skip account-state check for public auth endpoints that never require a session. */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        return "POST".equals(method) && (
                path.equals("/api/v1/auth/register") ||
                path.equals("/api/v1/auth/login") ||
                path.equals("/api/v1/auth/verify-otp") ||
                path.equals("/api/v1/auth/refresh")
        );
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String token = resolveToken(request);
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        String subject = jwtTokenProvider.getSubject(token);
        UUID userId;
        try {
            userId = UUID.fromString(subject);
        } catch (IllegalArgumentException e) {
            writeError(request, response, 401, "AUTHENTICATION_FAILED", "Invalid token subject");
            return;
        }

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            writeError(request, response, 401, "AUTHENTICATION_FAILED", "User not found");
            return;
        }

        User user = userOpt.get();
        if (!user.isEnabled()) {
            writeError(request, response, 403, "ACCOUNT_DISABLED", "This account has been disabled");
            return;
        }
        if (user.isLocked()) {
            writeError(request, response, 403, "ACCOUNT_LOCKED", "This account has been locked");
            return;
        }
        // UC-102 ADR-003 touchpoint #1: lazy, read-only expiry check — no write-back of
        // suspendedUntil=null in this hot path (C8), even after the suspension has lapsed.
        if (user.getSuspendedUntil() != null && Instant.now().isBefore(user.getSuspendedUntil())) {
            writeError(request, response, 403, "ACCOUNT_SUSPENDED",
                    "This account is suspended until " + user.getSuspendedUntil());
            return;
        }

        UUID sessionId = jwtTokenProvider.getSessionId(token);
        UserSessionRepository sessionRepository = userSessionRepositoryProvider.getIfAvailable();
        if (sessionId != null && sessionRepository != null) {
            Optional<UserSession> sessionOpt = sessionRepository.findById(sessionId);
            if (sessionOpt.isEmpty() || !isActiveSession(sessionOpt.get(), userId)) {
                writeError(request, response, 401, "SESSION_REVOKED",
                        "This session is no longer active");
                return;
            }
        }

        JwtAuthenticationToken authentication = new JwtAuthenticationToken(
                subject,
                null,
                jwtTokenProvider.getAuthorities(token),
                sessionId);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        org.springframework.security.core.context.SecurityContext securityContext =
                SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);

        filterChain.doFilter(request, response);
    }

    private boolean isActiveSession(UserSession session, UUID userId) {
        return userId.equals(session.getUserId())
                && !session.isRevoked()
                && "active".equals(session.getStatus())
                && session.getExpiresAt() != null
                && session.getExpiresAt().isAfter(Instant.now());
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(SecurityConstants.AUTHORIZATION_HEADER);
        if (bearerToken != null && bearerToken.startsWith(SecurityConstants.BEARER_PREFIX)) {
            return bearerToken.substring(SecurityConstants.BEARER_PREFIX.length());
        }
        return null;
    }

    private void writeError(HttpServletRequest request, HttpServletResponse response,
                            int status, String error, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        MAPPER.writeValue(response.getWriter(),
                ErrorResponse.of(status, error, message, request.getRequestURI()));
    }
}
