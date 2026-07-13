package com.carebridge.backend.emergency.service;

import com.carebridge.backend.emergency.dto.request.EmergencyContactRequest;
import com.carebridge.backend.emergency.dto.response.EmergencyContactResponse;

import java.util.UUID;

public interface IEmergencyContactService {
    EmergencyContactResponse getContact(UUID userId);
    EmergencyContactResponse upsertContact(UUID userId, EmergencyContactRequest request);
}
