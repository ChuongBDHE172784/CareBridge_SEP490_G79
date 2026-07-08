package com.carebridge.backend.security.jwt;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import java.util.Collection;
import java.util.UUID;

/**
 * Authentication token that includes the session ID (sid) from JWT.
 * This provides a typed way to access the session ID throughout the application.
 */
public class JwtAuthenticationToken extends UsernamePasswordAuthenticationToken {

    private final UUID sessionId;

    public JwtAuthenticationToken(Object principal, Object credentials, UUID sessionId) {
        super(principal, credentials);
        this.sessionId = sessionId;
    }

    public JwtAuthenticationToken(Object principal, Object credentials, Collection<? extends GrantedAuthority> authorities, UUID sessionId) {
        super(principal, credentials, authorities);
        this.sessionId = sessionId;
    }

    /**
     * Get the session ID (sid) from the JWT claims.
     * @return the session ID, or null if not present
     */
    public UUID getSessionId() {
        return sessionId;
    }
}
