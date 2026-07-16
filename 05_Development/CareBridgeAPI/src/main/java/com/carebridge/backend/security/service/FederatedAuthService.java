package com.carebridge.backend.security.service;

import com.carebridge.backend.security.dto.request.FederatedAuthRequest;
import com.carebridge.backend.security.dto.response.FederatedAuthResponse;

public interface FederatedAuthService {

    FederatedAuthResponse authenticate(FederatedAuthRequest request);
}
