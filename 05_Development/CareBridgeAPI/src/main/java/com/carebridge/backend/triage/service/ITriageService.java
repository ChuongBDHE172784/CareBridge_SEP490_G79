package com.carebridge.backend.triage.service;

import com.carebridge.backend.triage.dto.request.RunIntakeRequest;
import com.carebridge.backend.triage.dto.request.StartIntakeConversationRequest;
import com.carebridge.backend.triage.dto.request.ContinueIntakeConversationRequest;
import com.carebridge.backend.triage.dto.response.IntakeConversationResponse;
import com.carebridge.backend.triage.dto.response.IntakeSessionResponse;
import com.carebridge.backend.triage.dto.response.TriageResultResponse;
import java.util.List;
import java.util.UUID;

public interface ITriageService {
    IntakeSessionResponse runIntake(RunIntakeRequest request, UUID userId);
    TriageResultResponse getResult(UUID sessionId, UUID userId);
    List<IntakeSessionResponse> listSessions(UUID userId);
    IntakeConversationResponse startConversation(StartIntakeConversationRequest request, UUID userId);
    IntakeConversationResponse continueConversation(ContinueIntakeConversationRequest request, UUID userId);
}
