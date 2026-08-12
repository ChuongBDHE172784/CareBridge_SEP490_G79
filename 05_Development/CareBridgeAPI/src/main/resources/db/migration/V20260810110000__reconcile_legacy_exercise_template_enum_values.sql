-- V2 seeded one exercise template with generic lifecycle values that the
-- exercise JPA enums cannot deserialize. Keep the applied seed immutable and
-- reconcile only that exact legacy row and value pair in this forward change.

UPDATE public.care_item_templates
   SET stage = 'ALL'
 WHERE template_id = '60000000-0000-0000-0000-000000000003'
   AND entry_type = 'EXERCISE_TEMPLATE'
   AND stage = 'PREGNANCY';

UPDATE public.care_item_templates
   SET template_status = 'PUBLISHED'
 WHERE template_id = '60000000-0000-0000-0000-000000000003'
   AND entry_type = 'EXERCISE_TEMPLATE'
   AND template_status = 'ACTIVE';

-- Fail the deployment rather than letting any unreadable exercise template
-- turn title hydration into a runtime 500. Other care-item entry types have
-- separate lifecycle vocabularies and are deliberately outside this gate.
DO $$
DECLARE
    v_bad text;
BEGIN
    SELECT string_agg(detail, '; ' ORDER BY detail)
      INTO v_bad
      FROM (
          SELECT 'stage=' || stage AS detail
            FROM public.care_item_templates
           WHERE entry_type = 'EXERCISE_TEMPLATE'
             AND stage IS NOT NULL
             AND stage NOT IN ('FIRST', 'SECOND', 'THIRD', 'ALL')
           GROUP BY stage
          UNION ALL
          SELECT 'difficulty_level=' || difficulty_level
            FROM public.care_item_templates
           WHERE entry_type = 'EXERCISE_TEMPLATE'
             AND difficulty_level IS NOT NULL
             AND difficulty_level NOT IN ('EASY', 'MEDIUM', 'HARD')
           GROUP BY difficulty_level
          UNION ALL
          SELECT 'template_status=' || template_status
            FROM public.care_item_templates
           WHERE entry_type = 'EXERCISE_TEMPLATE'
             AND template_status NOT IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')
           GROUP BY template_status
      ) AS offenders;

    IF v_bad IS NOT NULL THEN
        RAISE EXCEPTION
            'EXERCISE_TEMPLATE_ENUM_RECONCILE_FAILED: unreadable values remain: %',
            v_bad;
    END IF;
END
$$;
