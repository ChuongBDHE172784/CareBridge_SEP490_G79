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

    Optional<ExerciseSafetyCheck> findTopByExerciseIdAndUserIdOrderByCreatedAtDesc(
            UUID exerciseId, UUID userId);

    @Modifying(flushAutomatically = true)
    @Query(value = """
        INSERT INTO maternal_observations (
            observation_id, observation_type, mother_journey_id, exercise_template_id,
            owner_user_id, check_code, response_boolean, blocked_boolean, observed_at,
            payload_jsonb, schema_version, source_type, legacy_source, legacy_id, created_at)
        SELECT (substr(md5('safety-answer:' || m.observation_id || ':' || answer.key),1,8) || '-' ||
                substr(md5('safety-answer:' || m.observation_id || ':' || answer.key),9,4) || '-' ||
                substr(md5('safety-answer:' || m.observation_id || ':' || answer.key),13,4) || '-' ||
                substr(md5('safety-answer:' || m.observation_id || ':' || answer.key),17,4) || '-' ||
                substr(md5('safety-answer:' || m.observation_id || ':' || answer.key),21,12))::uuid,
               'EXERCISE_SAFETY_CHECK', m.mother_journey_id, m.exercise_template_id,
               m.owner_user_id, answer.key, answer.value::boolean, m.blocked_boolean,
               m.observed_at, '{}'::jsonb, '1', 'EXERCISE_SAFETY',
               'EXERCISE_SAFETY_ANSWER', m.observation_id::text || ':' || answer.key,
               m.created_at
          FROM maternal_observations m
          CROSS JOIN LATERAL jsonb_each_text(m.payload_jsonb) answer
         WHERE m.observation_id = :observationId
           AND m.legacy_source = 'EXERCISE_SAFETY'
        ON CONFLICT (legacy_source, legacy_id) DO UPDATE SET
            response_boolean = EXCLUDED.response_boolean,
            blocked_boolean = EXCLUDED.blocked_boolean,
            observed_at = EXCLUDED.observed_at
        """, nativeQuery = true)
    int expandSafetyAnswers(@Param("observationId") UUID observationId);
}
