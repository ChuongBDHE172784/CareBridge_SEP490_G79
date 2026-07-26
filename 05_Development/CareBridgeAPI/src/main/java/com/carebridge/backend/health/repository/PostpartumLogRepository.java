package com.carebridge.backend.health.repository;

import com.carebridge.backend.health.entity.PostpartumLog;
import com.carebridge.backend.health.entity.PostpartumLogStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;

public interface PostpartumLogRepository extends JpaRepository<PostpartumLog, UUID> {

    @Query(value = "SELECT 1 FROM pg_advisory_xact_lock(hashtextextended(CAST(:journeyId AS text), 1))",
            nativeQuery = true)
    Integer acquireJourneyMutationLock(@Param("journeyId") UUID journeyId);

    @Query(value = """
            SELECT * FROM health_observations
             WHERE health_observation_id = :id
               AND legacy_source = 'postpartum_logs'
               AND COALESCE(raw_payload_jsonb->>'recordStatus', 'ACTIVE') = :#{#status.name()}
            """, nativeQuery = true)
    Optional<PostpartumLog> findByIdAndStatus(
            @Param("id") UUID id,
            @Param("status") PostpartumLogStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(value = """
            SELECT * FROM health_observations
             WHERE health_observation_id = :id
               AND legacy_source = 'postpartum_logs'
               AND COALESCE(raw_payload_jsonb->>'recordStatus', 'ACTIVE') = :#{#status.name()}
             FOR UPDATE
            """, nativeQuery = true)
    Optional<PostpartumLog> findByIdAndStatusForUpdate(
            @Param("id") UUID id,
            @Param("status") PostpartumLogStatus status);

    @Query(
            value = """
                    SELECT * FROM health_observations
                     WHERE care_subject_id = :journeyId
                       AND legacy_source = 'postpartum_logs'
                       AND COALESCE(raw_payload_jsonb->>'recordStatus', 'ACTIVE') = :#{#status.name()}
                     ORDER BY observed_at DESC, created_at DESC, health_observation_id DESC
                    """,
            countQuery = """
                    SELECT count(*) FROM health_observations
                     WHERE care_subject_id = :journeyId
                       AND legacy_source = 'postpartum_logs'
                       AND COALESCE(raw_payload_jsonb->>'recordStatus', 'ACTIVE') = :#{#status.name()}
                    """,
            nativeQuery = true)
    Page<PostpartumLog> findByJourneyIdAndStatus(
            @Param("journeyId") UUID journeyId,
            @Param("status") PostpartumLogStatus status,
            Pageable pageable);

    @Query(value = """
            SELECT * FROM health_observations
             WHERE care_subject_id = :journeyId
               AND legacy_source = 'postpartum_logs'
               AND raw_payload_jsonb->>'submissionId' = CAST(:submissionId AS text)
             ORDER BY created_at DESC
             LIMIT 1
            """, nativeQuery = true)
    Optional<PostpartumLog> findByJourneyIdAndSubmissionId(
            @Param("journeyId") UUID journeyId,
            @Param("submissionId") UUID submissionId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            UPDATE health_observations
               SET raw_payload_jsonb = jsonb_set(
                       COALESCE(raw_payload_jsonb, '{}'::jsonb),
                       '{recordStatus}',
                       to_jsonb(CAST(:#{#status.name()} AS text)),
                       true),
                   updated_at = now()
             WHERE health_observation_id = :id
               AND legacy_source = 'postpartum_logs'
            """, nativeQuery = true)
    int updateStatus(@Param("id") UUID id, @Param("status") PostpartumLogStatus status);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            UPDATE health_observations
               SET raw_payload_jsonb =
                       ((((COALESCE(raw_payload_jsonb, '{}'::jsonb)
                           - 'submissionId') - 'moodLevel')
                           - 'breastfeedingNote') - 'recordStatus')
                       || jsonb_strip_nulls(jsonb_build_object(
                           'submissionId', CAST(:submissionId AS text),
                           'moodLevel', CAST(:moodLevel AS smallint),
                           'breastfeedingNote', CAST(:breastfeedingNote AS text),
                           'recordStatus', CAST(:#{#status.name()} AS text))),
                   updated_at = now()
             WHERE health_observation_id = :id
               AND legacy_source = 'postpartum_logs'
            """, nativeQuery = true)
    int syncPayload(
            @Param("id") UUID id,
            @Param("submissionId") UUID submissionId,
            @Param("moodLevel") Short moodLevel,
            @Param("breastfeedingNote") String breastfeedingNote,
            @Param("status") PostpartumLogStatus status);
}
