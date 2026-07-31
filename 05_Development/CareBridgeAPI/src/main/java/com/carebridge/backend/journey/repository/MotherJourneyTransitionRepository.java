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
               AND CAST(payload->>'journeyVersion' AS bigint)=:journeyVersion
             ORDER BY created_at DESC LIMIT 1
            """, nativeQuery = true)
    Optional<MotherJourneyTransition> findFirstByJourneyIdAndJourneyVersionOrderByRecordedAtDesc(
            @Param("journeyId") UUID journeyId, @Param("journeyVersion") long journeyVersion);

    @Query(value = """
            SELECT CAST(payload->>'journeyVersion' AS bigint)
              FROM audit_events
             WHERE event_category='MOTHER_JOURNEY_TRANSITION'
               AND subject_reference_id=:journeyId
               AND payload->>'eventType'='STAGE_CHANGED'
               AND payload->>'fromStage'='POSTPARTUM'
               AND payload->>'toStage'='PREGNANCY'
             ORDER BY CAST(payload->>'journeyVersion' AS bigint) DESC
             LIMIT 1
            """, nativeQuery = true)
    Optional<Long> findLatestPostpartumToPregnancyEpochVersion(
            @Param("journeyId") UUID journeyId);

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
