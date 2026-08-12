package com.carebridge.backend.security.service.impl;

import com.carebridge.backend.security.dto.request.FederatedAuthRequest;
import com.carebridge.backend.security.dto.request.LinkGoogleIdentityRequest;
import com.carebridge.backend.security.dto.request.PhoneLoginRequest;
import com.carebridge.backend.security.dto.request.PhoneRegisterRequest;
import com.carebridge.backend.security.dto.response.FederatedAuthResponse;
import com.carebridge.backend.security.dto.response.LinkedGoogleIdentityResponse;
import com.carebridge.backend.security.service.FederatedAuthService;
import java.util.UUID;

public final class FederatedAuthServiceStub implements FederatedAuthService {

    @Override
    public FederatedAuthResponse authenticate(FederatedAuthRequest request) {
        throw new UnsupportedOperationException("Federated authentication is not implemented");
    }

    @Override
    public FederatedAuthResponse registerPhone(PhoneRegisterRequest request) {
        throw new UnsupportedOperationException("Phone authentication is not implemented");
    }

    @Override
    public FederatedAuthResponse loginPhone(PhoneLoginRequest request) {
        throw new UnsupportedOperationException("Phone authentication is not implemented");
    }

    @Override
    public LinkedGoogleIdentityResponse getGoogleIdentity(UUID userId) {
        throw new UnsupportedOperationException("Federated identity linking is not implemented");
    }

    @Override
    public LinkedGoogleIdentityResponse linkGoogleIdentity(UUID userId, LinkGoogleIdentityRequest request) {
        throw new UnsupportedOperationException("Federated identity linking is not implemented");
    }
}
