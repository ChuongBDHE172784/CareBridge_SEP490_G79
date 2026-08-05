package com.carebridge.backend.exercise.repository;

import com.carebridge.backend.exercise.entity.ExerciseSession;
import com.carebridge.backend.exercise.entity.SessionStatus;
import com.carebridge.backend.exercise.entity.TrimesterScope;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ExerciseSessionRepository extends JpaRepository<ExerciseSession, UUID> {

    Optional<ExerciseSession>
            findFirstByExerciseIdAndUserIdAndSessionStatusInAndStartedAtGreaterThanEqualAndStartedAtLessThanOrderByStartedAtAscExerciseSessionIdAsc(
                    UUID exerciseId,
                    UUID userId,
                    List<SessionStatus> statuses,
                    OffsetDateTime dayStart,
                    OffsetDateTime dayEnd);

    List<ExerciseSession> findByUserIdAndSessionStatusOrderByStartedAtDesc(
            UUID userId, SessionStatus status, Pageable pageable);

    @Query("""
        SELECT s FROM ExerciseSession s
        WHERE s.userId = :userId
          AND s.sessionStatus = 'COMPLETED'
          AND (:trimesterScope IS NULL OR EXISTS (
              SELECT 1 FROM PregnancyExercise e WHERE e.exerciseId = s.exerciseId
              AND e.trimesterScope = :trimesterScope))
          AND (:from IS NULL OR s.startedAt >= :from)
          AND (:to IS NULL OR s.startedAt <= :to)
        ORDER BY s.startedAt DESC
    """)
    Page<ExerciseSession> findCompletedByUserIdAndFilters(
            @Param("userId") UUID userId,
            @Param("trimesterScope") TrimesterScope trimesterScope,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to,
            Pageable pageable);
}
