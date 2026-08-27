package com.carebridge.backend.systemconfiguration.service;

import com.carebridge.backend.systemconfiguration.dto.request.UpdateSystemConfigurationRequest;
import com.carebridge.backend.systemconfiguration.dto.response.SystemConfigurationResponse;
import java.util.UUID;

public interface SystemConfigurationService {
    SystemConfigurationResponse get(UUID actorId);
    SystemConfigurationResponse update(UpdateSystemConfigurationRequest request, UUID actorId);
}
