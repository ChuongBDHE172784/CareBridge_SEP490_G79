package com.carebridge.backend.exercise.repository;

import com.carebridge.backend.exercise.entity.ExerciseSafetyCheck;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ExerciseSafetyCheckRepository extends JpaRepository<ExerciseSafetyCheck, UUID> {

    @Query(value = """
            SELECT * FROM health_observations
             WHERE observation_type='EXERCISE_SAFETY_RESULT'
               AND raw_payload_jsonb->>'exerciseTemplateId'=:#{#exerciseId.toString()}
               AND raw_payload_jsonb->>'ownerUserId'=:#{#userId.toString()}
             ORDER BY created_at DESC LIMIT 1
            """, nativeQuery = true)
    Optional<ExerciseSafetyCheck> findTopByExerciseIdAndUserIdOrderByCreatedAtDesc(
            @Param("exerciseId") UUID exerciseId, @Param("userId") UUID userId);

    @Modifying(flushAutomatically = true)
    @Query(value = """
        INSERT INTO health_observations (
            health_observation_id, care_subject_id, subject_type, observation_type,
            text_value, observed_at, raw_payload_jsonb, source_type,
            legacy_source, legacy_id, created_at)
        SELECT (md5('safety-answer:' || m.health_observation_id || ':' || answer.key))::uuid,
               m.care_subject_id, 'MOTHER', 'EXERCISE_SAFETY_CHECK',
               m.text_value, m.observed_at,
               jsonb_build_object(
                   'exerciseTemplateId', m.raw_payload_jsonb->>'exerciseTemplateId',
                   'ownerUserId', m.raw_payload_jsonb->>'ownerUserId',
                   'checkCode', answer.key,
                   'responseBoolean', answer.value::boolean,
                   'blockedBoolean', coalesce((m.raw_payload_jsonb->>'blockedBoolean')::boolean,false),
                   'recordStatus', m.raw_payload_jsonb->>'recordStatus'),
               'EXERCISE_SAFETY', 'exercise_safety_checks_answer',
               m.health_observation_id::text || ':' || answer.key, m.created_at
          FROM health_observations m
          CROSS JOIN LATERAL jsonb_each_text(coalesce(m.raw_payload_jsonb->'answer','{}'::jsonb)) answer
         WHERE m.health_observation_id = :observationId
           AND m.observation_type = 'EXERCISE_SAFETY_RESULT'
        ON CONFLICT (legacy_source, legacy_id) DO UPDATE SET
            raw_payload_jsonb = EXCLUDED.raw_payload_jsonb,
            observed_at = EXCLUDED.observed_at,
            updated_at = now()
        """, nativeQuery = true)
    int expandSafetyAnswers(@Param("observationId") UUID observationId);
}
