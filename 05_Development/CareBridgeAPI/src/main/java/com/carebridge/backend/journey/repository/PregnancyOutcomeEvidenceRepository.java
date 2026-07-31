package com.carebridge.backend.journey.repository;

import com.carebridge.backend.journey.entity.PregnancyOutcomeEvidence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PregnancyOutcomeEvidenceRepository
        extends JpaRepository<PregnancyOutcomeEvidence, UUID> {

    @Query(value = """
            SELECT * FROM audit_events
             WHERE event_category='PREGNANCY_OUTCOME_EVIDENCE'
               AND subject_reference_id=:journeyId
               AND payload->>'submissionId'=CAST(:submissionId AS text)
             ORDER BY occurred_at DESC LIMIT 1
            """, nativeQuery = true)
    Optional<PregnancyOutcomeEvidence> findByJourneyIdAndSubmissionId(
            @Param("journeyId") UUID journeyId, @Param("submissionId") UUID submissionId);

    @Query(value = """
            SELECT * FROM audit_events
             WHERE event_category='PREGNANCY_OUTCOME_EVIDENCE'
               AND subject_reference_id=:journeyId
               AND payload->>'submissionId'=CAST(:submissionId AS text)
               AND CAST(COALESCE(payload->>'journeyVersion', '0') AS bigint) > :epochVersion
             ORDER BY occurred_at DESC LIMIT 1
            """, nativeQuery = true)
    Optional<PregnancyOutcomeEvidence> findByJourneyIdAndSubmissionIdAfterEpochVersion(
            @Param("journeyId") UUID journeyId,
            @Param("submissionId") UUID submissionId,
            @Param("epochVersion") long epochVersion);

    @Query(value = """
            SELECT * FROM audit_events
             WHERE event_category='PREGNANCY_OUTCOME_EVIDENCE'
               AND subject_reference_id=:journeyId
             ORDER BY CAST(NULLIF(payload->>'revisionNumber', '') AS integer) DESC NULLS LAST,
                      created_at DESC,
                      audit_event_id DESC
             LIMIT 1
            """, nativeQuery = true)
    Optional<PregnancyOutcomeEvidence> findFirstByJourneyIdOrderByRevisionNumberDesc(
            @Param("journeyId") UUID journeyId);

    @Query(value = """
            SELECT * FROM audit_events
             WHERE event_category='PREGNANCY_OUTCOME_EVIDENCE'
               AND subject_reference_id=:journeyId
               AND CAST(COALESCE(payload->>'journeyVersion', '0') AS bigint) > :epochVersion
             ORDER BY CAST(NULLIF(payload->>'revisionNumber', '') AS integer) DESC NULLS LAST,
                      created_at DESC,
                      audit_event_id DESC
             LIMIT 1
            """, nativeQuery = true)
    Optional<PregnancyOutcomeEvidence> findFirstByJourneyIdAfterEpochVersionOrderByRevisionNumberDesc(
            @Param("journeyId") UUID journeyId, @Param("epochVersion") long epochVersion);
}
