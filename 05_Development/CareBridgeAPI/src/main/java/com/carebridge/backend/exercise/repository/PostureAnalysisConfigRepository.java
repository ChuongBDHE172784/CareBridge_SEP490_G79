package com.carebridge.backend.exercise.repository;

import com.carebridge.backend.exercise.entity.PostureAnalysisConfig;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PostureAnalysisConfigRepository
        extends JpaRepository<PostureAnalysisConfig, UUID> {

    @Query("""
        SELECT c FROM PostureAnalysisConfig c
        WHERE c.exerciseId = :exerciseId
          AND c.status = 'ACTIVE'
          AND (c.effectiveTo IS NULL OR c.effectiveTo > :now)
        ORDER BY c.effectiveFrom DESC
    """)
    Optional<PostureAnalysisConfig> findActiveConfigByExerciseId(
            @Param("exerciseId") UUID exerciseId,
            @Param("now") OffsetDateTime now);
}
