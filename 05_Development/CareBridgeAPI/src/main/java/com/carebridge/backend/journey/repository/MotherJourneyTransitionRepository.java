package com.carebridge.backend.journey.repository;

import com.carebridge.backend.journey.entity.MotherJourneyTransition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;
import java.util.Optional;

public interface MotherJourneyTransitionRepository
        extends Repository<MotherJourneyTransition, UUID> {

    <S extends MotherJourneyTransition> S saveAndFlush(S transition);

    Page<MotherJourneyTransition> findByJourneyIdOrderByRecordedAtDesc(
            UUID journeyId, Pageable pageable);

    long countByJourneyId(UUID journeyId);

    @Query(value = """
            SELECT * FROM audit_events
             WHERE event_category='MOTHER_JOURNEY_TRANSITION'
               AND subject_reference_id=:journeyId
               AND CASE WHEN payload->>'journeyVersion' ~ '^[0-9]+$'
                        AND (payload->>'journeyVersion')::numeric <= 9223372036854775807
                        THEN (payload->>'journeyVersion')::bigint END=:journeyVersion
             ORDER BY created_at DESC LIMIT 1
            """, nativeQuery = true)
    Optional<MotherJourneyTransition> findFirstByJourneyIdAndJourneyVersionOrderByRecordedAtDesc(
            @Param("journeyId") UUID journeyId, @Param("journeyVersion") long journeyVersion);

    @Query(value = """
            SELECT CASE WHEN payload->>'journeyVersion' ~ '^[0-9]+$'
                        AND (payload->>'journeyVersion')::numeric <= 9223372036854775807
                        THEN (payload->>'journeyVersion')::bigint END
              FROM audit_events
             WHERE event_category='MOTHER_JOURNEY_TRANSITION'
               AND subject_reference_id=:journeyId
               AND ((payload->>'eventType'='PREGNANCY_EPOCH_STARTED'
                     AND payload->>'toStage'='PREGNANCY')
                    OR (payload->>'eventType'='STAGE_CHANGED'
                        AND payload->>'fromStage'='POSTPARTUM'
                        AND payload->>'toStage'='PREGNANCY'))
               AND CASE WHEN payload->>'journeyVersion' ~ '^[0-9]+$'
                        AND (payload->>'journeyVersion')::numeric <= 9223372036854775807
                        THEN (payload->>'journeyVersion')::bigint END IS NOT NULL
             ORDER BY CASE WHEN payload->>'journeyVersion' ~ '^[0-9]+$'
                           AND (payload->>'journeyVersion')::numeric <= 9223372036854775807
                           THEN (payload->>'journeyVersion')::bigint END DESC
             LIMIT 1
            """, nativeQuery = true)
    Optional<Long> findLatestPostpartumToPregnancyEpochVersion(
            @Param("journeyId") UUID journeyId);

    /**
     * Dating revisions are reconstructed from the immutable transition spine,
     * not from the nullable current Journey row.  The native cast is guarded so
     * migrated legacy events without a revision are ignored.
     */
    @Query(value = """
            SELECT max(CASE
                         WHEN payload->>'gestationalDatingRevision' ~ '^[0-9]+$'
                              AND (payload->>'gestationalDatingRevision')::numeric
                                  <= 9223372036854775807
                         THEN (payload->>'gestationalDatingRevision')::bigint
                       END)
              FROM audit_events
             WHERE event_category='MOTHER_JOURNEY_TRANSITION'
               AND subject_reference_id=:journeyId
            """, nativeQuery = true)
    Long findMaxGestationalDatingRevision(@Param("journeyId") UUID journeyId);

    @Query(value = """
            SELECT EXISTS (
                SELECT 1
                  FROM audit_events
                 WHERE event_category='MOTHER_JOURNEY_TRANSITION'
                   AND subject_reference_id=:journeyId
                   AND (payload->>'fromStage'='POSTPARTUM'
                        OR payload->>'toStage'='POSTPARTUM')
            )
            """, nativeQuery = true)
    boolean hasPostpartumHistory(@Param("journeyId") UUID journeyId);

    long count();
}
