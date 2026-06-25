package com.carebridge.backend.security.jwt;

import com.carebridge.backend.common.constants.SecurityConstants;
import com.carebridge.backend.common.response.ErrorResponse;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    // Static mapper is thread-safe after initial configuration and avoids a
    // constructor dependency that breaks @WebMvcTest slices (ObjectMapper isn't
    // always resolvable in the filter construction phase of the test context).
    private static final ObjectMapper MAPPER =
            new ObjectMapper().registerModule(new JavaTimeModule());

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

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

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                subject, null, jwtTokenProvider.getAuthorities(token));
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(SecurityConstants.AUTHORIZATION_HEADER);
        if (header != null && header.startsWith(SecurityConstants.BEARER_PREFIX)) {
            return header.substring(SecurityConstants.BEARER_PREFIX.length());
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
