package com.carebridge.backend.exercise.repository;

import com.carebridge.backend.exercise.entity.ExerciseSafetyCheck;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExerciseSafetyCheckRepository extends JpaRepository<ExerciseSafetyCheck, UUID> {

    Optional<ExerciseSafetyCheck> findTopByExerciseIdAndUserIdOrderByCreatedAtDesc(
            UUID exerciseId, UUID userId);
}
