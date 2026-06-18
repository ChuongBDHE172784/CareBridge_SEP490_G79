package com.carebridge.backend.common.constants;

public final class SecurityConstants {

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String CLAIM_ROLE = "role";
    public static final String CLAIM_PHONE = "phone";
    public static final String CLAIM_TOKEN_TYPE = "tokenType";
    public static final String ACCESS_TOKEN_TYPE = "ACCESS";
    public static final String REFRESH_TOKEN_TYPE = "REFRESH";

    private SecurityConstants() {
    }
}
