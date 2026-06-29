package com.carebridge.backend.exercise.repository;

import com.carebridge.backend.exercise.entity.DifficultyLevel;
import com.carebridge.backend.exercise.entity.ExerciseStatus;
import com.carebridge.backend.exercise.entity.PregnancyExercise;
import com.carebridge.backend.exercise.entity.TrimesterScope;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ExerciseRepository extends JpaRepository<PregnancyExercise, UUID> {

    Optional<PregnancyExercise> findByExerciseIdAndStatus(UUID exerciseId, ExerciseStatus status);

    @Query("SELECT e FROM PregnancyExercise e WHERE e.status = :status "
         + "AND (:trimester IS NULL OR e.trimesterScope = :trimester) "
         + "AND (:difficulty IS NULL OR e.difficultyLevel = :difficulty) "
         + "ORDER BY e.createdAt DESC")
    Page<PregnancyExercise> findPublishedByFilters(
            @Param("status") ExerciseStatus status,
            @Param("trimester") TrimesterScope trimester,
            @Param("difficulty") DifficultyLevel difficulty,
            Pageable pageable);
}
