package com.carebridge.backend.security.service;

import com.carebridge.backend.security.dto.request.FederatedAuthRequest;
import com.carebridge.backend.security.dto.request.LinkGoogleIdentityRequest;
import com.carebridge.backend.security.dto.response.FederatedAuthResponse;
import com.carebridge.backend.security.dto.response.LinkedGoogleIdentityResponse;
import java.util.UUID;

public interface FederatedAuthService {

    FederatedAuthResponse authenticate(FederatedAuthRequest request);

    LinkedGoogleIdentityResponse getGoogleIdentity(UUID userId);

    LinkedGoogleIdentityResponse linkGoogleIdentity(UUID userId, LinkGoogleIdentityRequest request);
}
