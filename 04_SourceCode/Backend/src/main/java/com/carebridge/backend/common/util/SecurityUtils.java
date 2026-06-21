package com.carebridge.backend.common.util;

import com.carebridge.backend.common.exception.AuthenticationException;
import java.security.Principal;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static UUID requireCurrentUserId(Principal principal) {
        if (principal == null || principal.getName() == null) {
            throw new AuthenticationException("Authenticated user is required");
        }
        try {
            return UUID.fromString(principal.getName());
        } catch (IllegalArgumentException ex) {
            throw new AuthenticationException("Invalid authenticated user");
        }
    }

    public static boolean hasRole(String role) {
        return currentAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.equals(role) || authority.equals("ROLE_" + role));
    }

    public static Collection<? extends GrantedAuthority> currentAuthorities() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return List.of();
        }
        return authentication.getAuthorities();
    }
}
