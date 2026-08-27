package com.carebridge.backend.journey.repository;

import com.carebridge.backend.journey.entity.MotherBaselineContext;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MotherBaselineContextRepository
        extends JpaRepository<MotherBaselineContext, UUID> {

    @Query(value = """
            SELECT 1
            FROM pg_advisory_xact_lock(
                hashtextextended(CAST(:ownerUserId AS text), 0))
            """, nativeQuery = true)
    Integer acquireOwnerLock(@Param("ownerUserId") UUID ownerUserId);

    @Query(value = """
            SELECT * FROM audit_events
             WHERE event_category='BASELINE_CONTEXT'
               AND actor_user_id=:ownerUserId
               AND payload->>'submissionId'=CAST(:submissionId AS text)
             ORDER BY occurred_at DESC LIMIT 1
            """, nativeQuery = true)
    Optional<MotherBaselineContext> findByOwnerUserIdAndSubmissionId(
            @Param("ownerUserId") UUID ownerUserId, @Param("submissionId") UUID submissionId);

    @Query(value = """
            SELECT * FROM audit_events
             WHERE event_category='BASELINE_CONTEXT'
               AND actor_user_id=:ownerUserId
             ORDER BY CAST(NULLIF(payload->>'revision', '') AS bigint) DESC NULLS LAST,
                      occurred_at DESC,
                      audit_event_id DESC
             LIMIT 1
            """, nativeQuery = true)
    Optional<MotherBaselineContext> findTopByOwnerUserIdOrderByRevisionDesc(
            @Param("ownerUserId") UUID ownerUserId);

    @Modifying(flushAutomatically = true)
    @Query(value = """
            UPDATE mother_journeys
               SET baseline_revision = :#{#baseline.revision},
                   baseline_schema_version = :#{#baseline.schemaVersion},
                   baseline_source = :#{#baseline.source},
                   baseline_lifecycle_goal = CAST(:#{#baseline.lifecycleGoal?.name()} AS varchar),
                   baseline_locale = :#{#baseline.locale},
                   baseline_time_zone = :#{#baseline.timeZone},
                   baseline_preferences = :#{#baseline.preferences},
                   baseline_submission_id = :#{#baseline.submissionId},
                   baseline_recorded_at = :#{#baseline.recordedAt}
             WHERE journey_id = (
                 SELECT journey_id FROM mother_journeys
                  WHERE owner_user_id = :#{#baseline.ownerUserId}
                  ORDER BY created_at DESC, journey_id
                  LIMIT 1)
            """, nativeQuery = true)
    int updateCurrentJourneySnapshot(@Param("baseline") MotherBaselineContext baseline);
}
