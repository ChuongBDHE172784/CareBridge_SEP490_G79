-- AI moderation policies apply only to community questions and answers.
-- Keep ReportTargetType.CONTENT for the separate editorial moderation workflow.

WITH normalized_targets AS (
    SELECT p.policy_id,
           array_to_string(
               ARRAY(
                   SELECT target_value
                   FROM unnest(regexp_split_to_array(p.applicable_target_types, '\s*,\s*'))
                        WITH ORDINALITY AS parsed(target_value, position)
                   WHERE target_value IN ('QUESTION', 'ANSWER')
                   GROUP BY target_value
                   ORDER BY min(position)
               ),
               ','
           ) AS retained_targets
    FROM public.ai_moderation_policies p
)
UPDATE public.ai_moderation_policies AS policy
SET applicable_target_types = CASE
        WHEN normalized.retained_targets = '' THEN 'QUESTION'
        ELSE normalized.retained_targets
    END,
    active = CASE
        WHEN normalized.retained_targets = '' THEN FALSE
        ELSE policy.active
    END,
    version = policy.version + 1,
    updated_at = clock_timestamp()
FROM normalized_targets AS normalized
WHERE policy.policy_id = normalized.policy_id
  AND policy.applicable_target_types IS DISTINCT FROM CASE
      WHEN normalized.retained_targets = '' THEN 'QUESTION'
      ELSE normalized.retained_targets
  END;

ALTER TABLE public.ai_moderation_policies
ADD CONSTRAINT chk_ai_policy_target_types
CHECK (applicable_target_types IN ('QUESTION', 'ANSWER', 'QUESTION,ANSWER', 'ANSWER,QUESTION'));
