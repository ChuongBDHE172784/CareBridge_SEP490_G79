package com.carebridge.backend.emergency.service;

import com.carebridge.backend.emergency.dto.request.OpenEmergencyRequest;
import com.carebridge.backend.emergency.dto.response.EmergencySessionResponse;
import java.util.UUID;

public interface IEmergencyService {

    EmergencySessionResponse openFlow(OpenEmergencyRequest request, UUID userId);

    EmergencySessionResponse getActiveSession(UUID userId);

    EmergencySessionResponse resolveSession(UUID sessionId, UUID userId);
}
