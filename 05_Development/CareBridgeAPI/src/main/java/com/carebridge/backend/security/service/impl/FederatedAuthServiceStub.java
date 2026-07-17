package com.carebridge.backend.security.service.impl;

import com.carebridge.backend.security.dto.request.FederatedAuthRequest;
import com.carebridge.backend.security.dto.response.FederatedAuthResponse;
import com.carebridge.backend.security.service.FederatedAuthService;

public final class FederatedAuthServiceStub implements FederatedAuthService {

    @Override
    public FederatedAuthResponse authenticate(FederatedAuthRequest request) {
        throw new UnsupportedOperationException("Federated authentication is not implemented");
    }
}
