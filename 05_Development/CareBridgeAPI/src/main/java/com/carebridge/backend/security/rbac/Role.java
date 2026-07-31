package com.carebridge.backend.security.rbac;

public enum Role {
    MOTHER,
    FAMILY,
    EXPERT,
    MODERATOR,
    CONTENT_ADMIN,
    SYSTEM_ADMIN,
    OPERATIONS,
    PARTNER;

    public String getAuthority() {
        return "ROLE_" + name();
    }
}
