package com.carebridge.backend.triage.service;

import com.carebridge.backend.triage.dto.request.TriageSessionContinueRequest;
import com.carebridge.backend.triage.dto.request.TriageSessionStartRequest;
import com.carebridge.backend.triage.dto.response.TriageSessionResponse;

import java.util.UUID;

public interface ITriageSessionService {
    TriageSessionResponse start(TriageSessionStartRequest request, UUID userId);
    TriageSessionResponse continueSession(TriageSessionContinueRequest request, UUID userId);
    TriageSessionResponse get(UUID sessionId, UUID userId);
    TriageSessionResponse cancel(UUID sessionId, int expectedStateVersion, UUID userId);
}
