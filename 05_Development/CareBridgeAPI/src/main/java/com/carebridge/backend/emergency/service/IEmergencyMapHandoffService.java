package com.carebridge.backend.emergency.service;

import com.carebridge.backend.emergency.dto.request.CreateEmergencyHandoffRequest;
import com.carebridge.backend.emergency.dto.response.EmergencyHandoffResponse;
import java.util.List;
import java.util.UUID;

public interface IEmergencyMapHandoffService {

    EmergencyHandoffResponse createHandoff(UUID userId, CreateEmergencyHandoffRequest request);

    EmergencyHandoffResponse getHandoff(UUID handoffId, UUID callerId, boolean systemAdmin);

    List<EmergencyHandoffResponse> getMyHandoffs(UUID userId);
}
