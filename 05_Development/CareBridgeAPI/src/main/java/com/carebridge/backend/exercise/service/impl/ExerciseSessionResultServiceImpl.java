package com.carebridge.backend.exercise.service.impl;

import com.carebridge.backend.common.exception.SessionNotFoundException;
import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.exercise.dto.SessionResultResponse;
import com.carebridge.backend.exercise.entity.ExerciseSession;
import com.carebridge.backend.exercise.entity.PregnancyExercise;
import com.carebridge.backend.exercise.entity.SessionStatus;
import com.carebridge.backend.exercise.exception.SessionNotCompletedException;
import com.carebridge.backend.exercise.exception.SessionOwnershipException;
import com.carebridge.backend.exercise.mapper.ExerciseSessionMapper;
import com.carebridge.backend.exercise.repository.ExerciseRepository;
import com.carebridge.backend.exercise.repository.ExerciseSessionRepository;
import com.carebridge.backend.exercise.service.IExerciseSessionResultService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ExerciseSessionResultServiceImpl implements IExerciseSessionResultService {

    private final ExerciseSessionRepository sessionRepository;
    private final ExerciseRepository exerciseRepository;
    private final ExerciseSessionMapper sessionMapper;

    @Override
    public ApiResponse<SessionResultResponse> getSessionResult(UUID sessionId, UUID userId) {
        ExerciseSession session = sessionRepository
                .findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundException("Exercise session not found"));

        if (!userId.equals(session.getUserId())) {
            throw new SessionOwnershipException();
        }

        if (session.getSessionStatus() != SessionStatus.COMPLETED) {
            throw new SessionNotCompletedException();
        }

        String title = exerciseRepository
                .findById(session.getExerciseId())
                .map(PregnancyExercise::getTitle)
                .orElse("Unknown Exercise");

        return ApiResponse.success(sessionMapper.toResultResponse(session, title));
    }
}
