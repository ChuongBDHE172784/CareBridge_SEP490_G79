package com.carebridge.backend.exercise.service;

import com.carebridge.backend.common.response.PaginatedResponse;
import com.carebridge.backend.exercise.dto.ExerciseSessionHistorySummary;
import com.carebridge.backend.exercise.entity.TrimesterScope;
import java.time.OffsetDateTime;
import java.util.UUID;

public interface IExerciseSessionHistoryService {

    PaginatedResponse<ExerciseSessionHistorySummary> getSessionHistory(
            UUID userId,
            TrimesterScope trimesterScope,
            OffsetDateTime from,
            OffsetDateTime to,
            int page,
            int size);
}
