package com.carebridge.backend.exercise.mapper;

import com.carebridge.backend.exercise.dto.ExerciseSessionHistorySummary;
import com.carebridge.backend.exercise.dto.SessionResultResponse;
import com.carebridge.backend.exercise.dto.SessionStateResponse;
import com.carebridge.backend.exercise.dto.StartSessionResponse;
import com.carebridge.backend.exercise.entity.ExerciseSession;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Component;

@Component
public class ExerciseSessionMapper {

    public StartSessionResponse toStartResponse(
            ExerciseSession session, boolean supportsPostureAnalysis) {
        return StartSessionResponse.builder()
                .exerciseSessionId(session.getExerciseSessionId())
                .exerciseId(session.getExerciseId())
                .userId(session.getUserId())
                .safetyCheckId(session.getSafetyCheckId())
                .journeyId(session.getJourneyId())
                .sessionStatus(session.getSessionStatus() != null
                        ? session.getSessionStatus().name() : null)
                .startedAt(session.getStartedAt())
                .supportsPostureAnalysis(supportsPostureAnalysis)
                .build();
    }

    public SessionStateResponse toStateResponse(ExerciseSession session) {
        return SessionStateResponse.builder()
                .exerciseSessionId(session.getExerciseSessionId())
                .sessionStatus(session.getSessionStatus() != null
                        ? session.getSessionStatus().name() : null)
                .pausedSeconds(session.getPausedSeconds())
                .warningCount(session.getWarningCount())
                .updatedAt(session.getUpdatedAt())
                .build();
    }

    public SessionResultResponse toResultResponse(ExerciseSession session, String exerciseTitle) {
        return SessionResultResponse.builder()
                .exerciseSessionId(session.getExerciseSessionId())
                .exerciseId(session.getExerciseId())
                .exerciseTitle(exerciseTitle)
                .sessionStatus(session.getSessionStatus() != null
                        ? session.getSessionStatus().name() : null)
                .startedAt(session.getStartedAt())
                .endedAt(session.getEndedAt())
                .actualDurationSeconds(computeActualDurationSeconds(session))
                .completionPercent(session.getCompletionPercent())
                .postureScore(session.getPostureScore())
                .warningCount(session.getWarningCount())
                .summaryJson(session.getSummaryJson())
                .build();
    }

    public ExerciseSessionHistorySummary toHistorySummary(
            ExerciseSession session, String exerciseTitle) {
        return ExerciseSessionHistorySummary.builder()
                .exerciseSessionId(session.getExerciseSessionId())
                .exerciseId(session.getExerciseId())
                .exerciseTitle(exerciseTitle)
                .sessionStatus(session.getSessionStatus() != null
                        ? session.getSessionStatus().name() : null)
                .startedAt(session.getStartedAt())
                .endedAt(session.getEndedAt())
                .actualDurationSeconds(computeActualDurationSeconds(session))
                .completionPercent(session.getCompletionPercent())
                .postureScore(session.getPostureScore())
                .warningCount(session.getWarningCount())
                .build();
    }

    private Long computeActualDurationSeconds(ExerciseSession session) {
        if (session.getStartedAt() == null || session.getEndedAt() == null) {
            return null;
        }
        long elapsed = ChronoUnit.SECONDS.between(session.getStartedAt(), session.getEndedAt());
        int paused = session.getPausedSeconds() != null ? session.getPausedSeconds() : 0;
        long actual = elapsed - paused;
        return Math.max(actual, 0L);
    }
}
