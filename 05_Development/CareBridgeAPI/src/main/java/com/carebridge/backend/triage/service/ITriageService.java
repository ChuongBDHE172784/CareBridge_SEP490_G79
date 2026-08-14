package com.carebridge.backend.triage.service;

import com.carebridge.backend.triage.dto.response.IntakeSessionResponse;
import com.carebridge.backend.triage.dto.response.TriageResultResponse;
import java.util.List;
import java.util.UUID;

public interface ITriageService {
    TriageResultResponse getResult(UUID sessionId, UUID userId);
    List<IntakeSessionResponse> listSessions(UUID userId);
}
