package com.carebridge.backend.security.federation;

public record VerifiedFederatedIdentity(
        FederatedProvider provider,
        String subject,
        String email,
        String phoneNumber,
        String displayName,
        boolean emailVerified,
        boolean phoneVerified) {
}
