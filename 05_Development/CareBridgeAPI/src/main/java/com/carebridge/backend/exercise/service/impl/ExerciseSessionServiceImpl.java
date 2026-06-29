package com.carebridge.backend.exercise.service.impl;

import com.carebridge.backend.exercise.service.IExerciseSessionService;
import com.carebridge.backend.common.exception.SessionNotFoundException;
import com.carebridge.backend.exercise.dto.SessionResultResponse;
import com.carebridge.backend.exercise.dto.SessionStateResponse;
import com.carebridge.backend.exercise.dto.StartSessionRequest;
import com.carebridge.backend.exercise.dto.StartSessionResponse;
import com.carebridge.backend.exercise.entity.ExerciseSafetyCheck;
import com.carebridge.backend.exercise.entity.ExerciseSession;
import com.carebridge.backend.exercise.entity.ExerciseStatus;
import com.carebridge.backend.exercise.entity.PregnancyExercise;
import com.carebridge.backend.exercise.entity.SafetyCheckStatus;
import com.carebridge.backend.exercise.entity.SessionStatus;
import com.carebridge.backend.exercise.exception.DuplicateSessionException;
import com.carebridge.backend.exercise.exception.ExerciseNotFoundException;
import com.carebridge.backend.exercise.exception.InvalidSessionStateException;
import com.carebridge.backend.exercise.exception.SafetyCheckNotClearedException;
import com.carebridge.backend.exercise.exception.SessionOwnershipException;
import com.carebridge.backend.exercise.mapper.ExerciseSessionMapper;
import com.carebridge.backend.exercise.repository.ExerciseRepository;
import com.carebridge.backend.exercise.repository.ExerciseSafetyCheckRepository;
import com.carebridge.backend.exercise.repository.ExerciseSessionRepository;
import com.carebridge.backend.exercise.repository.PostureFeedbackEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExerciseSessionServiceImpl implements IExerciseSessionService {

    private static final List<SessionStatus> ACTIVE_STATUSES =
            List.of(SessionStatus.IN_PROGRESS, SessionStatus.PAUSED);

    private final ExerciseSessionRepository sessionRepository;
    private final ExerciseRepository exerciseRepository;
    private final ExerciseSafetyCheckRepository safetyCheckRepository;
    private final PostureFeedbackEventRepository postureFeedbackEventRepository;
    private final ExerciseSessionMapper sessionMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public StartSessionResponse startSession(
            UUID exerciseId, StartSessionRequest request, UUID userId) {
        // 1. Exercise must exist and be PUBLISHED.
        PregnancyExercise exercise = exerciseRepository
                .findByExerciseIdAndStatus(exerciseId, ExerciseStatus.PUBLISHED)
                .orElseThrow(ExerciseNotFoundException::notFound);

        // 2. Safety check must belong to user, match exercise, and be CLEARED.
        ExerciseSafetyCheck safetyCheck = safetyCheckRepository
                .findById(request.getSafetyCheckId())
                .orElseThrow(SafetyCheckNotClearedException::new);
        if (!userId.equals(safetyCheck.getUserId())
                || !exerciseId.equals(safetyCheck.getExerciseId())
                || safetyCheck.getResultStatus() != SafetyCheckStatus.CLEARED) {
            throw new SafetyCheckNotClearedException();
        }

        // 3. No duplicate active session today.
        OffsetDateTime dayStart = OffsetDateTime.now(ZoneOffset.UTC)
                .truncatedTo(ChronoUnit.DAYS);
        sessionRepository
                .findActiveSessionToday(exerciseId, userId, ACTIVE_STATUSES, dayStart)
                .ifPresent(s -> {
                    throw new DuplicateSessionException();
                });

        // 4. Build the new IN_PROGRESS session.
        OffsetDateTime now = OffsetDateTime.now();
        ExerciseSession session = ExerciseSession.builder()
                .exerciseSessionId(UUID.randomUUID())
                .exerciseId(exerciseId)
                .userId(userId)
                .journeyId(request.getJourneyId())
                .safetyCheckId(request.getSafetyCheckId())
                .startedAt(now)
                .pausedSeconds(0)
                .warningCount(0)
                .sessionStatus(SessionStatus.IN_PROGRESS)
                .createdAt(now)
                .updatedAt(now)
                .build();

        ExerciseSession saved = sessionRepository.save(session);
        boolean supportsPosture = Boolean.TRUE.equals(exercise.getSupportsPostureAnalysis());
        return sessionMapper.toStartResponse(saved, supportsPosture);
    }

    @Override
    @Transactional
    public SessionStateResponse pauseSession(UUID sessionId, UUID userId) {
        ExerciseSession session = loadOwnedSession(sessionId, userId);
        if (session.getSessionStatus() != SessionStatus.IN_PROGRESS) {
            throw InvalidSessionStateException.alreadyPaused();
        }
        session.setSessionStatus(SessionStatus.PAUSED);
        session.setWarningCount(nullSafe(session.getWarningCount()) + 1);
        // updatedAt marks the pause-start timestamp used when resuming.
        session.setUpdatedAt(OffsetDateTime.now());
        ExerciseSession saved = sessionRepository.save(session);
        return sessionMapper.toStateResponse(saved);
    }

    @Override
    @Transactional
    public SessionStateResponse resumeSession(UUID sessionId, UUID userId) {
        ExerciseSession session = loadOwnedSession(sessionId, userId);
        if (session.getSessionStatus() != SessionStatus.PAUSED) {
            throw InvalidSessionStateException.notPaused();
        }
        long elapsedPauseSeconds =
                ChronoUnit.SECONDS.between(session.getUpdatedAt(), OffsetDateTime.now());
        if (elapsedPauseSeconds < 0) {
            elapsedPauseSeconds = 0;
        }
        session.setPausedSeconds(nullSafe(session.getPausedSeconds()) + (int) elapsedPauseSeconds);
        session.setSessionStatus(SessionStatus.IN_PROGRESS);
        session.setUpdatedAt(OffsetDateTime.now());
        ExerciseSession saved = sessionRepository.save(session);
        return sessionMapper.toStateResponse(saved);
    }

    @Override
    @Transactional
    public SessionResultResponse completeSession(UUID sessionId, UUID userId) {
        ExerciseSession session = loadOwnedSession(sessionId, userId);
        if (session.getSessionStatus() != SessionStatus.IN_PROGRESS
                && session.getSessionStatus() != SessionStatus.PAUSED) {
            throw InvalidSessionStateException.cannotComplete();
        }

        PregnancyExercise exercise = exerciseRepository
                .findById(session.getExerciseId())
                .orElseThrow(ExerciseNotFoundException::notFound);

        OffsetDateTime endedAt = OffsetDateTime.now();
        long elapsed = ChronoUnit.SECONDS.between(session.getStartedAt(), endedAt);
        long actualDurationSeconds = Math.max(elapsed - nullSafe(session.getPausedSeconds()), 0L);

        BigDecimal completionPercent =
                computeCompletionPercent(actualDurationSeconds, exercise.getDurationMinutes());
        BigDecimal postureScore = computePostureScore(sessionId);
        String summaryJson = buildSummaryJson(sessionId);

        session.setEndedAt(endedAt);
        session.setSessionStatus(SessionStatus.COMPLETED);
        session.setCompletionPercent(completionPercent);
        session.setPostureScore(postureScore);
        session.setSummaryJson(summaryJson);
        session.setUpdatedAt(OffsetDateTime.now());

        ExerciseSession saved = sessionRepository.save(session);
        return sessionMapper.toResultResponse(saved, exercise.getTitle());
    }

    private ExerciseSession loadOwnedSession(UUID sessionId, UUID userId) {
        ExerciseSession session = sessionRepository
                .findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundException("Exercise session not found"));
        if (!userId.equals(session.getUserId())) {
            throw new SessionOwnershipException();
        }
        return session;
    }

    private BigDecimal computeCompletionPercent(long actualDurationSeconds, Short durationMinutes) {
        if (durationMinutes == null || durationMinutes <= 0) {
            return null;
        }
        double plannedSeconds = durationMinutes * 60.0;
        double percent = actualDurationSeconds / plannedSeconds * 100.0;
        BigDecimal value = BigDecimal.valueOf(percent).setScale(2, RoundingMode.HALF_UP);
        BigDecimal hundred = new BigDecimal("100.00");
        return value.compareTo(hundred) > 0 ? hundred : value;
    }

    private BigDecimal computePostureScore(UUID sessionId) {
        List<com.carebridge.backend.exercise.entity.PostureFeedbackEvent> events =
                postureFeedbackEventRepository.findByExerciseSessionId(sessionId);
        if (events.isEmpty()) {
            return null;
        }
        BigDecimal sum = BigDecimal.ZERO;
        int count = 0;
        for (var event : events) {
            if (event.getConfidenceScore() != null) {
                sum = sum.add(event.getConfidenceScore());
                count++;
            }
        }
        if (count == 0) {
            return null;
        }
        return sum.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
    }

    private String buildSummaryJson(UUID sessionId) {
        List<com.carebridge.backend.exercise.entity.PostureFeedbackEvent> events =
                postureFeedbackEventRepository.findByExerciseSessionId(sessionId);
        List<String> issues = new ArrayList<>();
        List<String> highlights = new ArrayList<>();
        for (var event : events) {
            String severity = event.getSeverity();
            String code = event.getPostureCode();
            if (code == null) {
                continue;
            }
            if ("HIGH".equalsIgnoreCase(severity)) {
                issues.add(code);
            } else if ("LOW".equalsIgnoreCase(severity)) {
                highlights.add(code);
            }
        }
        try {
            return objectMapper.writeValueAsString(
                    Map.of("issues", issues, "highlights", highlights));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to build session summary JSON", ex);
        }
    }

    private int nullSafe(Integer value) {
        return value != null ? value : 0;
    }
}
