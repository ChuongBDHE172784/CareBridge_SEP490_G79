package com.carebridge.backend.security.dto.response;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpSendResponse {

    private String message;
    private long expiresIn;
    private UUID userId;
    private Instant otpExpiresAt;

    /** Non-null when the identifier is already verified — client can use these tokens directly, skipping OTP. */
    private AuthResponse auth;
}
