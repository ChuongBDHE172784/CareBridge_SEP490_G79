package com.carebridge.backend.security.dto.response;

import com.carebridge.backend.security.federation.FederatedProvider;
import java.time.Instant;

public record LinkedGoogleIdentityResponse(
        FederatedProvider provider,
        boolean linked,
        String email,
        Instant linkedAt) {
}
