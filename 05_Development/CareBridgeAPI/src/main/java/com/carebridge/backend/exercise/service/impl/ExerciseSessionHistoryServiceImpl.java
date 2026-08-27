package com.carebridge.backend.exercise.service.impl;

import com.carebridge.backend.common.response.PaginatedResponse;
import com.carebridge.backend.exercise.dto.ExerciseSessionHistorySummary;
import com.carebridge.backend.exercise.entity.ExerciseSession;
import com.carebridge.backend.exercise.entity.PregnancyExercise;
import com.carebridge.backend.exercise.entity.TrimesterScope;
import com.carebridge.backend.exercise.mapper.ExerciseSessionMapper;
import com.carebridge.backend.exercise.repository.ExerciseRepository;
import com.carebridge.backend.exercise.repository.ExerciseSessionRepository;
import com.carebridge.backend.exercise.service.IExerciseSessionHistoryService;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ExerciseSessionHistoryServiceImpl implements IExerciseSessionHistoryService {

    private static final int MIN_SIZE = 1;
    private static final int MAX_SIZE = 50;

    private final ExerciseSessionRepository sessionRepository;
    private final ExerciseRepository exerciseRepository;
    private final ExerciseSessionMapper sessionMapper;

    @Override
    public PaginatedResponse<ExerciseSessionHistorySummary> getSessionHistory(
            UUID userId,
            TrimesterScope trimesterScope,
            OffsetDateTime from,
            OffsetDateTime to,
            int page,
            int size) {

        int clampedSize = Math.max(MIN_SIZE, Math.min(size, MAX_SIZE));
        int clampedPage = Math.max(0, page);
        Pageable pageable = PageRequest.of(clampedPage, clampedSize);

        Page<ExerciseSession> sessions = sessionRepository.findCompletedByUserIdAndFilters(
                userId, trimesterScope, from, to, pageable);

        Map<UUID, String> titleCache = new HashMap<>();
        Page<ExerciseSessionHistorySummary> mapped = sessions.map(session -> {
            String title = titleCache.computeIfAbsent(
                    session.getExerciseId(),
                    id -> exerciseRepository.findById(id)
                            .map(PregnancyExercise::getTitle)
                            .orElse("Unknown Exercise"));
            return sessionMapper.toHistorySummary(session, title);
        });

        return PaginatedResponse.of(mapped);
    }
}
