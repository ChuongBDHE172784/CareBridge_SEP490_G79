package com.carebridge.backend.security.dto.response;

import lombok.Builder;

@Builder
public record FederatedAuthResponse(
        String accessToken,
        String refreshToken,
        UserProfileResponse user,
        boolean newUser,
        boolean profileCompleted) {
}
