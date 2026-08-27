package com.carebridge.backend.safety.service;

import com.carebridge.backend.safety.dto.request.SafetyConfigRequest;
import com.carebridge.backend.safety.dto.response.SafetyConfigResponse;
import java.util.UUID;

public interface ISafetyConfigService {
    SafetyConfigResponse configure(SafetyConfigRequest request, UUID userId);
    SafetyConfigResponse getConfig(UUID userId);
}
