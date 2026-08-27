package com.carebridge.backend.exercise.service;

import com.carebridge.backend.exercise.dto.SessionResultResponse;
import com.carebridge.backend.exercise.dto.SessionStateResponse;
import com.carebridge.backend.exercise.dto.StartSessionRequest;
import com.carebridge.backend.exercise.dto.StartSessionResponse;
import java.util.UUID;

public interface IExerciseSessionService {

    StartSessionResponse startSession(UUID exerciseId, StartSessionRequest request, UUID userId);

    SessionStateResponse pauseSession(UUID sessionId, UUID userId);

    SessionStateResponse resumeSession(UUID sessionId, UUID userId);

    SessionResultResponse completeSession(UUID sessionId, UUID userId);
}
