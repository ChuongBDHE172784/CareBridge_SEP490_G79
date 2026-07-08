package com.carebridge.backend.exercise.repository;

import com.carebridge.backend.exercise.entity.PostureAnalysisConfig;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PostureAnalysisConfigRepository
        extends JpaRepository<PostureAnalysisConfig, UUID> {

    // --- EXISTING (unchanged, used by getActiveConfig() / Mother-facing UC180) ---
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

    // --- NEW (UC186 admin write side) ---

    /** Finds the current ACTIVE row for an exercise, regardless of effectiveTo window — used for supersede. */
    Optional<PostureAnalysisConfig> findByExerciseIdAndStatus(UUID exerciseId, String status);

    /** Full version history for the admin list screen, newest first. */
    List<PostureAnalysisConfig> findAllByExerciseIdOrderByEffectiveFromDesc(UUID exerciseId);

    /** Guards createConfig() — PAC-006 "already exists, use new-version endpoint" check. */
    boolean existsByExerciseId(UUID exerciseId);

    // findById(UUID) inherited from JpaRepository — used by activateVersion()
    // Note: no delete() method added — append-only, no hard delete (ADR-PAC-002)
}
