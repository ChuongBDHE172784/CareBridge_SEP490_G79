package com.carebridge.backend.triage.service;

import com.carebridge.backend.triage.dto.request.TriageV2ContinueRequest;
import com.carebridge.backend.triage.dto.request.TriageV2StartRequest;
import com.carebridge.backend.triage.dto.response.TriageV2SessionResponse;

import java.util.UUID;

public interface ITriageV2SessionService {
    TriageV2SessionResponse start(TriageV2StartRequest request, UUID userId);
    TriageV2SessionResponse continueSession(TriageV2ContinueRequest request, UUID userId);
    TriageV2SessionResponse get(UUID sessionId, UUID userId);
    TriageV2SessionResponse cancel(UUID sessionId, int expectedStateVersion, UUID userId);
}
