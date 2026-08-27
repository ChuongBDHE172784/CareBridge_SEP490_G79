package com.carebridge.backend.emergency.service;

import java.util.UUID;

public record AlertRecipientEndpoint(UUID userId, UUID deviceTokenId, UUID careGroupId, String token) {

    public static AlertRecipientEndpoint inAppOnly(UUID userId, UUID careGroupId) {
        return new AlertRecipientEndpoint(userId, null, careGroupId, null);
    }

    public boolean hasPushEndpoint() {
        return deviceTokenId != null && token != null && !token.isBlank();
    }
}
