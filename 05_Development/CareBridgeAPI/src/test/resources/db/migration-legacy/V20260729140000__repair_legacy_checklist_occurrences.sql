-- CHK-022 roll-forward repair. V20260729070000 is intentionally immutable.
-- Rebuild legacy V2 projections from the retained source rows using a real
-- occurrence token, preserve progressed task state, and quarantine only genuine
-- same-item/same-occurrence collisions.

CREATE TEMP TABLE checklist_occurrence_repair_bootstrap (marker boolean) ON COMMIT DROP;

CREATE OR REPLACE FUNCTION pg_temp.checklist_v1_key(VARIADIC tokens text[])
RETURNS text
LANGUAGE sql
IMMUTABLE
STRICT
AS $$
    SELECT encode(sha256(convert_to(
        'v1' || COALESCE(string_agg(
            octet_length(convert_to(token, 'UTF8'))::text || ':' || token,
            '' ORDER BY ordinal), ''),
        'UTF8')), 'hex')
    FROM unnest(tokens) WITH ORDINALITY AS ordered_tokens(token, ordinal)
$$;

DO $$
BEGIN
    IF pg_temp.checklist_v1_key(
            '11111111-1111-1111-1111-111111111111',
            '22222222-2222-2222-2222-222222222222',
            'MOTHER',
            '33333333-3333-3333-3333-333333333333',
            'JOURNEY',
            '44444444-4444-4444-4444-444444444444',
            'NONE',
            'NONE') <>
            'fad7bba6cefeb717acaf887b59410cef7184b88706e67cdf828be0240678369d' THEN
        RAISE EXCEPTION 'CHECKLIST_V1_KEY_GOLDEN_VECTOR_MISMATCH';
    END IF;
END $$;

CREATE TEMP TABLE checklist_occurrence_source_stage ON COMMIT DROP AS
SELECT
    legacy.checklist_item_id AS source_id,
    legacy.owner_user_id,
    legacy.title,
    COALESCE(legacy.display_order, 0) AS source_display_order,
    legacy.status AS legacy_status,
    legacy.due_at,
    legacy.completed_at,
    legacy.created_at,
    legacy.updated_at,
    legacy.template_entry_id,
    CASE
        WHEN legacy.baby_id IS NOT NULL AND legacy.mother_journey_id IS NULL THEN 'BABY'
        WHEN legacy.mother_journey_id IS NOT NULL AND legacy.baby_id IS NULL THEN 'JOURNEY'
    END AS care_context_type,
    CASE
        WHEN legacy.baby_id IS NOT NULL AND legacy.mother_journey_id IS NULL THEN legacy.baby_id
        WHEN legacy.mother_journey_id IS NOT NULL AND legacy.baby_id IS NULL THEN legacy.mother_journey_id
    END AS care_context_id,
    context_match.care_group_id,
    COALESCE(context_match.match_count, 0) AS context_match_count,
    root.template_lineage_id,
    root.template_version_id,
    item.template_id AS template_item_version_id,
    COALESCE(item.target_subject,
        CASE WHEN legacy.baby_id IS NOT NULL THEN 'BABY' ELSE 'MOTHER' END) AS target_subject,
    CASE WHEN legacy.template_entry_id IS NULL THEN 'USER_CREATED' ELSE 'SYSTEM_TEMPLATE' END AS origin,
    CASE
        WHEN legacy.template_entry_id IS NOT NULL THEN
            (legacy.created_at
                AT TIME ZONE 'Asia/Ho_Chi_Minh')::date
    END AS occurrence_date,
    CASE
        WHEN legacy.baby_id IS NOT NULL AND legacy.mother_journey_id IS NOT NULL
            THEN 'AMBIGUOUS_LEGACY_CONTEXT'
        WHEN legacy.baby_id IS NULL AND legacy.mother_journey_id IS NULL
            THEN 'UNKNOWN_LEGACY_CONTEXT'
        WHEN COALESCE(context_match.match_count, 0) = 0
            THEN 'CONTEXT_OWNER_MISMATCH'
        WHEN context_match.match_count > 1
            THEN 'MULTIPLE_CONTEXT_BINDINGS'
        WHEN legacy.template_entry_id IS NOT NULL AND
             (item.template_id IS NULL OR root.template_version_id IS NULL
              OR root.template_lineage_id IS NULL)
            THEN 'UNKNOWN_TEMPLATE_ROOT'
        WHEN legacy.title IS NULL OR btrim(legacy.title) = ''
            THEN 'INVALID_LEGACY_TITLE'
        WHEN legacy.status IS NULL OR upper(legacy.status) NOT IN
             ('OPEN','PENDING','IN_PROGRESS','COMPLETED','DONE','SKIPPED','CANCELLED')
            THEN 'UNKNOWN_LEGACY_STATUS'
        WHEN legacy.completed_at IS NOT NULL
             AND upper(legacy.status) IN ('OPEN','PENDING','IN_PROGRESS')
            THEN 'CONTRADICTORY_LEGACY_TIMESTAMPS'
    END AS validation_reason
FROM public.preparation_checklist_items legacy
LEFT JOIN public.care_item_templates item
  ON item.template_id = legacy.template_entry_id
 AND item.entry_type = 'CHECKLIST_ENTRY'
LEFT JOIN public.care_item_templates root
  ON root.template_id = item.parent_template_id
 AND root.entry_type = 'TEMPLATE_ROOT'
LEFT JOIN LATERAL (
    SELECT candidate.care_group_id, count(*) OVER () AS match_count
    FROM public.checklist_care_group_contexts candidate
    WHERE candidate.owner_user_id = legacy.owner_user_id
      AND candidate.review_status = 'REVIEWED'
      AND candidate.distribution_blocked = false
      AND (
          (legacy.baby_id IS NOT NULL AND legacy.mother_journey_id IS NULL
              AND candidate.care_context_type = 'BABY'
              AND candidate.care_context_id = legacy.baby_id)
          OR
          (legacy.mother_journey_id IS NOT NULL AND legacy.baby_id IS NULL
              AND candidate.care_context_type = 'JOURNEY'
              AND candidate.care_context_id = legacy.mother_journey_id)
      )
    ORDER BY candidate.care_group_id
    LIMIT 1
) context_match ON true;

CREATE TEMP TABLE checklist_occurrence_repair_quarantine (
    source_id uuid NOT NULL,
    reason_code varchar(80) NOT NULL,
    correlation_id uuid NOT NULL DEFAULT gen_random_uuid(),
    redacted_payload bytea NOT NULL DEFAULT sha256(convert_to(
        gen_random_uuid()::text || clock_timestamp()::text || random()::text,
        'UTF8')),
    CONSTRAINT checklist_occurrence_repair_quarantine_uk UNIQUE (source_id, reason_code)
) ON COMMIT DROP;

INSERT INTO checklist_occurrence_repair_quarantine (source_id, reason_code)
SELECT source.source_id, source.validation_reason
FROM checklist_occurrence_source_stage source
WHERE source.validation_reason IS NOT NULL;

-- A collision is scoped to one recipient/context/version/date and one versioned
-- item. Different dates are different occurrences and must never collide.
INSERT INTO checklist_occurrence_repair_quarantine (source_id, reason_code)
SELECT source.source_id, 'LEGACY_OCCURRENCE_COLLISION'
FROM checklist_occurrence_source_stage source
JOIN (
    SELECT owner_user_id, care_group_id, care_context_type, care_context_id,
           template_version_id, occurrence_date, template_item_version_id
    FROM checklist_occurrence_source_stage
    WHERE validation_reason IS NULL
      AND origin = 'SYSTEM_TEMPLATE'
    GROUP BY owner_user_id, care_group_id, care_context_type, care_context_id,
             template_version_id, occurrence_date, template_item_version_id
    HAVING count(*) > 1
) collision
  ON collision.owner_user_id = source.owner_user_id
 AND collision.care_group_id = source.care_group_id
 AND collision.care_context_type = source.care_context_type
 AND collision.care_context_id = source.care_context_id
 AND collision.template_version_id = source.template_version_id
 AND collision.occurrence_date = source.occurrence_date
 AND collision.template_item_version_id = source.template_item_version_id;

CREATE TEMP TABLE checklist_occurrence_repair_rows ON COMMIT DROP AS
WITH eligible AS (
    SELECT source.*
    FROM checklist_occurrence_source_stage source
    WHERE source.validation_reason IS NULL
      AND NOT EXISTS (
          SELECT 1
          FROM checklist_occurrence_repair_quarantine quarantined
          WHERE quarantined.source_id = source.source_id
      )
), keyed AS (
    SELECT
        eligible.*,
        CASE
            WHEN eligible.origin = 'SYSTEM_TEMPLATE' THEN pg_temp.checklist_v1_key(
                eligible.template_version_id::text,
                eligible.owner_user_id::text,
                'MOTHER',
                eligible.care_group_id::text,
                eligible.care_context_type,
                eligible.care_context_id::text,
                eligible.occurrence_date::text,
                eligible.occurrence_date::text)
            ELSE pg_temp.checklist_v1_key(
                'LEGACY_USER_CREATED_PARENT',
                eligible.owner_user_id::text,
                'MOTHER',
                eligible.care_group_id::text,
                eligible.care_context_type,
                eligible.care_context_id::text,
                eligible.source_id::text)
        END AS repaired_parent_key,
        CASE
            WHEN eligible.origin = 'SYSTEM_TEMPLATE' THEN pg_temp.checklist_v1_key(
                eligible.template_version_id::text,
                eligible.owner_user_id::text,
                'MOTHER',
                eligible.care_group_id::text,
                eligible.care_context_type,
                eligible.care_context_id::text,
                'NONE',
                'NONE')
            ELSE pg_temp.checklist_v1_key(
                eligible.owner_user_id::text,
                'MOTHER',
                eligible.care_group_id::text,
                eligible.care_context_type,
                eligible.care_context_id::text,
                '<ABSENT>',
                '<ABSENT>')
        END AS old_parent_key
    FROM eligible
), identified AS (
    SELECT
        keyed.*,
        pg_temp.checklist_v1_key('LEGACY_PARENT_ID', repaired_parent_key)
            AS repaired_parent_identity_hash,
        pg_temp.checklist_v1_key('LEGACY_PARENT_ID', old_parent_key)
            AS old_parent_identity_hash
    FROM keyed
), with_ids AS (
    SELECT
        identified.*,
        (substr(repaired_parent_identity_hash, 1, 8) || '-' ||
         substr(repaired_parent_identity_hash, 9, 4) || '-' ||
         substr(repaired_parent_identity_hash, 13, 4) || '-' ||
         substr(repaired_parent_identity_hash, 17, 4) || '-' ||
         substr(repaired_parent_identity_hash, 21, 12))::uuid AS repaired_parent_id,
        (substr(old_parent_identity_hash, 1, 8) || '-' ||
         substr(old_parent_identity_hash, 9, 4) || '-' ||
         substr(old_parent_identity_hash, 13, 4) || '-' ||
         substr(old_parent_identity_hash, 17, 4) || '-' ||
         substr(old_parent_identity_hash, 21, 12))::uuid AS old_parent_id
    FROM identified
)
SELECT
    with_ids.*,
    row_number() OVER (
        PARTITION BY repaired_parent_id
        ORDER BY source_display_order, source_id) - 1 AS repaired_display_order
FROM with_ids;

CREATE UNIQUE INDEX checklist_occurrence_repair_rows_source_ix
    ON checklist_occurrence_repair_rows(source_id);
CREATE INDEX checklist_occurrence_repair_rows_parent_ix
    ON checklist_occurrence_repair_rows(repaired_parent_id);
CREATE INDEX checklist_occurrence_repair_rows_old_parent_source_ix
    ON checklist_occurrence_repair_rows(old_parent_id, source_id);
ANALYZE checklist_occurrence_repair_rows;

CREATE TEMP TABLE checklist_occurrence_repair_parents ON COMMIT DROP AS
SELECT
    repaired_parent_id,
    repaired_parent_key,
    template_lineage_id,
    template_version_id,
    owner_user_id,
    care_group_id,
    care_context_type,
    care_context_id,
    origin,
    occurrence_date,
    CASE
        WHEN bool_and(upper(legacy_status) = 'CANCELLED') THEN 'CANCELLED'
        WHEN bool_and(upper(legacy_status) IN
                ('COMPLETED','DONE','SKIPPED','CANCELLED')) THEN 'COMPLETED'
        WHEN bool_or(upper(legacy_status) IN
                ('IN_PROGRESS','COMPLETED','DONE','SKIPPED','CANCELLED'))
            THEN 'IN_PROGRESS'
        ELSE 'PENDING'
    END AS status,
    CASE
        WHEN NOT bool_and(upper(legacy_status) = 'CANCELLED')
             AND bool_and(upper(legacy_status) IN
                 ('COMPLETED','DONE','SKIPPED','CANCELLED'))
            THEN max(COALESCE(completed_at, updated_at, created_at))
    END AS completed_at,
    CASE
        WHEN bool_and(upper(legacy_status) = 'CANCELLED')
            THEN max(COALESCE(updated_at, created_at))
    END AS cancelled_at,
    min(created_at) AS created_at,
    max(updated_at) AS updated_at
FROM checklist_occurrence_repair_rows
GROUP BY repaired_parent_id, repaired_parent_key, template_lineage_id,
         template_version_id, owner_user_id, care_group_id, care_context_type,
         care_context_id, origin, occurrence_date;

CREATE UNIQUE INDEX checklist_occurrence_repair_parents_id_ix
    ON checklist_occurrence_repair_parents(repaired_parent_id);
CREATE UNIQUE INDEX checklist_occurrence_repair_parents_key_ix
    ON checklist_occurrence_repair_parents(repaired_parent_key);
ANALYZE checklist_occurrence_repair_parents;

INSERT INTO checklist_occurrence_repair_quarantine (source_id, reason_code)
SELECT row.source_id, 'LEGACY_PARENT_PAYLOAD_DRIFT'
FROM checklist_occurrence_repair_rows row
JOIN checklist_occurrence_repair_parents proposed
  ON proposed.repaired_parent_id = row.repaired_parent_id
JOIN public.checklist_instances existing
  ON existing.checklist_instance_id = proposed.repaired_parent_id
WHERE existing.distribution_key IS DISTINCT FROM proposed.repaired_parent_key
   OR existing.key_version IS DISTINCT FROM 'v1'
   OR existing.template_lineage_id IS DISTINCT FROM proposed.template_lineage_id
   OR existing.template_version_id IS DISTINCT FROM proposed.template_version_id
   OR existing.recipient_user_id IS DISTINCT FROM proposed.owner_user_id
   OR existing.recipient_role IS DISTINCT FROM 'MOTHER'
   OR existing.care_group_id IS DISTINCT FROM proposed.care_group_id
   OR existing.care_context_type IS DISTINCT FROM proposed.care_context_type
   OR existing.care_context_id IS DISTINCT FROM proposed.care_context_id
   OR existing.context_owner_user_id IS DISTINCT FROM proposed.owner_user_id
   OR existing.origin IS DISTINCT FROM proposed.origin
   OR existing.window_start IS DISTINCT FROM CASE
       WHEN proposed.origin = 'SYSTEM_TEMPLATE' THEN proposed.occurrence_date END
   OR existing.window_end IS DISTINCT FROM CASE
       WHEN proposed.origin = 'SYSTEM_TEMPLATE' THEN proposed.occurrence_date END
ON CONFLICT DO NOTHING;

INSERT INTO checklist_occurrence_repair_quarantine (source_id, reason_code)
SELECT row.source_id, 'LEGACY_DISTRIBUTION_KEY_COLLISION'
FROM checklist_occurrence_repair_rows row
JOIN public.checklist_instances existing
  ON existing.distribution_key = row.repaired_parent_key
WHERE existing.checklist_instance_id <> row.repaired_parent_id
ON CONFLICT DO NOTHING;

INSERT INTO public.checklist_instances
    (checklist_instance_id, distribution_key, key_version,
     template_lineage_id, template_version_id,
     recipient_user_id, recipient_role, care_group_id,
     care_context_type, care_context_id, context_owner_user_id,
     origin, window_start, window_end, status,
     completed_at, cancelled_at, cancellation_reason_code,
     created_at, updated_at)
SELECT
    proposed.repaired_parent_id,
    proposed.repaired_parent_key,
    'v1',
    proposed.template_lineage_id,
    proposed.template_version_id,
    proposed.owner_user_id,
    'MOTHER',
    proposed.care_group_id,
    proposed.care_context_type,
    proposed.care_context_id,
    proposed.owner_user_id,
    proposed.origin,
    CASE WHEN proposed.origin = 'SYSTEM_TEMPLATE' THEN proposed.occurrence_date END,
    CASE WHEN proposed.origin = 'SYSTEM_TEMPLATE' THEN proposed.occurrence_date END,
    proposed.status,
    proposed.completed_at,
    proposed.cancelled_at,
    CASE WHEN proposed.status = 'CANCELLED' THEN 'LEGACY_CANCELLED' END,
    proposed.created_at,
    proposed.updated_at
FROM checklist_occurrence_repair_parents proposed
WHERE NOT EXISTS (
        SELECT 1
        FROM checklist_occurrence_repair_rows row
        JOIN checklist_occurrence_repair_quarantine quarantined
          ON quarantined.source_id = row.source_id
        WHERE row.repaired_parent_id = proposed.repaired_parent_id
    )
  AND NOT EXISTS (
        SELECT 1 FROM public.checklist_instances existing
        WHERE existing.checklist_instance_id = proposed.repaired_parent_id
    )
  AND NOT EXISTS (
        SELECT 1 FROM public.checklist_instances existing
        WHERE existing.distribution_key = proposed.repaired_parent_key
    );
CREATE TEMP TABLE checklist_occurrence_repair_tasks ON COMMIT DROP AS
SELECT
    row.*,
    CASE
        WHEN row.origin = 'SYSTEM_TEMPLATE' THEN pg_temp.checklist_v1_key(
            row.repaired_parent_id::text,
            row.template_item_version_id::text)
        ELSE pg_temp.checklist_v1_key(
            row.repaired_parent_id::text,
            'USER_CREATED',
            row.source_id::text)
    END AS repaired_task_key
FROM checklist_occurrence_repair_rows row
WHERE NOT EXISTS (
    SELECT 1
    FROM checklist_occurrence_repair_quarantine quarantined
    WHERE quarantined.source_id = row.source_id
);

CREATE UNIQUE INDEX checklist_occurrence_repair_tasks_source_ix
    ON checklist_occurrence_repair_tasks(source_id);
CREATE INDEX checklist_occurrence_repair_tasks_parent_ix
    ON checklist_occurrence_repair_tasks(repaired_parent_id);
ANALYZE checklist_occurrence_repair_tasks;

INSERT INTO checklist_occurrence_repair_quarantine (source_id, reason_code)
SELECT proposed.source_id, 'LEGACY_OCCURRENCE_COLLISION'
FROM checklist_occurrence_repair_tasks proposed
JOIN public.checklist_task_instances existing
  ON existing.task_key = proposed.repaired_task_key
WHERE existing.checklist_task_instance_id <> proposed.source_id
ON CONFLICT DO NOTHING;

INSERT INTO checklist_occurrence_repair_quarantine (source_id, reason_code)
SELECT proposed.source_id, 'LEGACY_TASK_PAYLOAD_DRIFT'
FROM checklist_occurrence_repair_tasks proposed
JOIN public.checklist_task_instances existing
  ON existing.checklist_task_instance_id = proposed.source_id
WHERE existing.template_version_id IS DISTINCT FROM proposed.template_version_id
   OR existing.template_item_version_id IS DISTINCT FROM proposed.template_item_version_id
   OR existing.key_version IS DISTINCT FROM 'v1'
   OR existing.title_snapshot IS DISTINCT FROM proposed.title
   OR existing.target_subject IS DISTINCT FROM proposed.target_subject
   OR existing.due_at IS DISTINCT FROM proposed.due_at
   OR existing.is_required IS DISTINCT FROM false
ON CONFLICT DO NOTHING;

-- Re-parent existing V70000 projections without touching status or terminal/action
-- timestamps. These rows may have progressed after the original backfill.
UPDATE public.checklist_task_instances existing
SET checklist_instance_id = proposed.repaired_parent_id,
    template_version_id = proposed.template_version_id,
    template_item_version_id = proposed.template_item_version_id,
    task_key = proposed.repaired_task_key,
    display_order = proposed.repaired_display_order::integer
FROM checklist_occurrence_repair_tasks proposed
WHERE existing.checklist_task_instance_id = proposed.source_id
  AND EXISTS (
      SELECT 1 FROM public.checklist_instances parent
      WHERE parent.checklist_instance_id = proposed.repaired_parent_id
  )
  AND NOT EXISTS (
      SELECT 1
      FROM checklist_occurrence_repair_quarantine quarantined
      WHERE quarantined.source_id = proposed.source_id
  );

INSERT INTO public.checklist_task_instances
    (checklist_task_instance_id, checklist_instance_id,
     template_version_id, template_item_version_id,
     task_key, key_version, title_snapshot, display_order,
     is_required, target_subject, due_at, status,
     completed_at, skipped_at, cancelled_at, action_reason_code,
     created_at, updated_at)
SELECT
    proposed.source_id,
    proposed.repaired_parent_id,
    proposed.template_version_id,
    proposed.template_item_version_id,
    proposed.repaired_task_key,
    'v1',
    proposed.title,
    proposed.repaired_display_order::integer,
    false,
    proposed.target_subject,
    proposed.due_at,
    CASE
        WHEN upper(proposed.legacy_status) IN ('COMPLETED','DONE') THEN 'COMPLETED'
        WHEN upper(proposed.legacy_status) = 'SKIPPED' THEN 'SKIPPED'
        WHEN upper(proposed.legacy_status) = 'CANCELLED' THEN 'CANCELLED'
        WHEN upper(proposed.legacy_status) = 'IN_PROGRESS' THEN 'IN_PROGRESS'
        ELSE 'PENDING'
    END,
    CASE WHEN upper(proposed.legacy_status) IN ('COMPLETED','DONE')
        THEN COALESCE(proposed.completed_at, proposed.updated_at, proposed.created_at) END,
    CASE WHEN upper(proposed.legacy_status) = 'SKIPPED'
        THEN COALESCE(proposed.completed_at, proposed.updated_at, proposed.created_at) END,
    CASE WHEN upper(proposed.legacy_status) = 'CANCELLED'
        THEN COALESCE(proposed.updated_at, proposed.created_at) END,
    CASE
        WHEN upper(proposed.legacy_status) = 'SKIPPED' THEN 'LEGACY_SKIPPED'
        WHEN upper(proposed.legacy_status) = 'CANCELLED' THEN 'LEGACY_CANCELLED'
    END,
    proposed.created_at,
    proposed.updated_at
FROM checklist_occurrence_repair_tasks proposed
WHERE EXISTS (
        SELECT 1 FROM public.checklist_instances parent
        WHERE parent.checklist_instance_id = proposed.repaired_parent_id
    )
  AND NOT EXISTS (
        SELECT 1
        FROM checklist_occurrence_repair_quarantine quarantined
        WHERE quarantined.source_id = proposed.source_id
    )
  AND NOT EXISTS (
        SELECT 1 FROM public.checklist_task_instances existing
        WHERE existing.checklist_task_instance_id = proposed.source_id
    )
  AND NOT EXISTS (
        SELECT 1 FROM public.checklist_task_instances existing
        WHERE existing.task_key = proposed.repaired_task_key
    );

-- Parent state follows the repaired children, not stale source status. This keeps
-- completions/actions that happened after V70000 while fixing the grouping only.
WITH aggregate_state AS (
    SELECT
        parent.checklist_instance_id,
        CASE
            WHEN bool_and(task.status = 'CANCELLED') THEN 'CANCELLED'
            WHEN bool_and(task.status IN ('COMPLETED','SKIPPED','CANCELLED'))
                THEN 'COMPLETED'
            WHEN bool_or(task.status IN
                    ('IN_PROGRESS','COMPLETED','SKIPPED','CANCELLED'))
                THEN 'IN_PROGRESS'
            ELSE 'PENDING'
        END AS status,
        CASE WHEN NOT bool_and(task.status = 'CANCELLED')
                  AND bool_and(task.status IN ('COMPLETED','SKIPPED','CANCELLED'))
            THEN max(COALESCE(
                task.completed_at, task.skipped_at, task.cancelled_at, task.updated_at))
        END AS completed_at,
        CASE WHEN bool_and(task.status = 'CANCELLED')
            THEN max(COALESCE(task.cancelled_at, task.updated_at)) END AS cancelled_at
    FROM checklist_occurrence_repair_parents proposed
    JOIN public.checklist_instances parent
      ON parent.checklist_instance_id = proposed.repaired_parent_id
     AND parent.distribution_key = proposed.repaired_parent_key
     AND parent.key_version = 'v1'
     AND parent.template_lineage_id IS NOT DISTINCT FROM proposed.template_lineage_id
     AND parent.template_version_id IS NOT DISTINCT FROM proposed.template_version_id
     AND parent.recipient_user_id = proposed.owner_user_id
     AND parent.recipient_role = 'MOTHER'
     AND parent.care_group_id = proposed.care_group_id
     AND parent.care_context_type = proposed.care_context_type
     AND parent.care_context_id = proposed.care_context_id
     AND parent.context_owner_user_id = proposed.owner_user_id
     AND parent.origin = proposed.origin
     AND parent.window_start IS NOT DISTINCT FROM CASE
         WHEN proposed.origin = 'SYSTEM_TEMPLATE' THEN proposed.occurrence_date END
     AND parent.window_end IS NOT DISTINCT FROM CASE
         WHEN proposed.origin = 'SYSTEM_TEMPLATE' THEN proposed.occurrence_date END
    JOIN public.checklist_task_instances task
      ON task.checklist_instance_id = parent.checklist_instance_id
    WHERE NOT EXISTS (
        SELECT 1
        FROM checklist_occurrence_repair_rows row
        JOIN checklist_occurrence_repair_quarantine quarantined
          ON quarantined.source_id = row.source_id
        WHERE row.repaired_parent_id = proposed.repaired_parent_id
    )
    GROUP BY parent.checklist_instance_id
)
UPDATE public.checklist_instances parent
SET status = aggregate.status,
    completed_at = aggregate.completed_at,
    cancelled_at = aggregate.cancelled_at,
    cancellation_reason_code = CASE
        WHEN aggregate.status = 'CANCELLED' THEN 'LEGACY_CANCELLED'
    END
FROM aggregate_state aggregate
WHERE parent.checklist_instance_id = aggregate.checklist_instance_id;
-- Tombstone deterministic V70000 parents that became empty after re-parenting.
-- Updating instead of deleting avoids same-transaction FK scans over the old
-- task index versions and retains an explicit, audited migration boundary.
CREATE TEMP TABLE checklist_occurrence_repair_audited_old_parents ON COMMIT DROP AS
SELECT DISTINCT history.old_parent_id
FROM public.audit_events audit
JOIN checklist_occurrence_repair_rows history
  ON history.source_id = audit.checklist_task_instance_id
WHERE audit.checklist_task_instance_id IS NOT NULL
  AND history.old_parent_id IS NOT NULL;
CREATE UNIQUE INDEX checklist_occurrence_repair_audited_old_parents_ix
    ON checklist_occurrence_repair_audited_old_parents(old_parent_id);

WITH distinct_old_parent_keys AS MATERIALIZED (
    SELECT DISTINCT repaired.old_parent_id, repaired.old_parent_key,
           repaired.owner_user_id, repaired.care_group_id,
           repaired.care_context_type, repaired.care_context_id, repaired.origin
    FROM checklist_occurrence_repair_rows repaired
), empty_old_parents AS MATERIALIZED (
    SELECT repaired.*
    FROM distinct_old_parent_keys repaired
    JOIN public.checklist_instances old_parent
      ON old_parent.checklist_instance_id = repaired.old_parent_id
     AND old_parent.distribution_key = repaired.old_parent_key
     AND old_parent.recipient_user_id = repaired.owner_user_id
     AND old_parent.recipient_role = 'MOTHER'
     AND old_parent.care_group_id = repaired.care_group_id
     AND old_parent.care_context_type = repaired.care_context_type
     AND old_parent.care_context_id = repaired.care_context_id
     AND old_parent.origin = repaired.origin
     AND old_parent.status = 'PENDING'
     AND old_parent.completed_at IS NULL
     AND old_parent.cancelled_at IS NULL
    WHERE NOT EXISTS (
        SELECT 1 FROM public.checklist_task_instances child
        WHERE child.checklist_instance_id = old_parent.checklist_instance_id
    )
), tombstoned AS (
    UPDATE public.checklist_instances old_parent
    SET status = 'CANCELLED',
        cancelled_at = clock_timestamp(),
        cancellation_reason_code = 'LEGACY_OCCURRENCE_REPAIRED',
        updated_at = clock_timestamp()
    FROM empty_old_parents repaired
    WHERE old_parent.checklist_instance_id = repaired.old_parent_id
      AND old_parent.distribution_key = repaired.old_parent_key
      AND NOT EXISTS (
          SELECT 1 FROM public.audit_events audit
          WHERE audit.resource_type = 'CHECKLIST_INSTANCE'
            AND audit.resource_id = old_parent.checklist_instance_id
      )
      AND NOT EXISTS (
          SELECT 1
          FROM checklist_occurrence_repair_audited_old_parents audited
          WHERE audited.old_parent_id = old_parent.checklist_instance_id
      )
    RETURNING old_parent.checklist_instance_id, old_parent.recipient_user_id,
              old_parent.care_context_type, old_parent.care_context_id,
              old_parent.template_version_id
)
INSERT INTO public.audit_events
    (actor_user_id, event_category, subject_user_id,
     resource_type, resource_id, purpose, decision,
     occurred_at, created_at, event_origin, payload,
     correlation_id, severity, status,
     actor_type, actor_service, reason_code,
     care_context_type, care_context_id, template_version_id,
     before_payload_jsonb, after_payload_jsonb)
SELECT
    NULL,
    'CHECKLIST_CANCELLED',
    tombstoned.recipient_user_id,
    'CHECKLIST_INSTANCE',
    tombstoned.checklist_instance_id,
    'LEGACY_CHECKLIST_OCCURRENCE_REPAIR',
    'CANCELLED',
    now(),
    now(),
    'CHECKLIST_MIGRATION',
    jsonb_build_object(
        'reasonCode', 'LEGACY_OCCURRENCE_REPAIRED',
        'metadata', 'REDACTED'),
    gen_random_uuid(),
    'MEDIUM',
    'CLOSED',
    'SERVICE',
    'CHECKLIST_LEGACY_OCCURRENCE_REPAIR',
    'LEGACY_OCCURRENCE_REPAIRED',
    tombstoned.care_context_type,
    tombstoned.care_context_id,
    tombstoned.template_version_id,
    jsonb_build_object('status', 'PENDING'),
    jsonb_build_object(
        'status', 'CANCELLED',
        'reasonCode', 'LEGACY_OCCURRENCE_REPAIRED')
FROM tombstoned;

-- A repaired parent can be empty only when every candidate task was quarantined.
WITH empty_repaired_parents AS MATERIALIZED (
    SELECT proposed.repaired_parent_id
    FROM checklist_occurrence_repair_parents proposed
    JOIN public.checklist_instances parent
      ON parent.checklist_instance_id = proposed.repaired_parent_id
     AND parent.distribution_key = proposed.repaired_parent_key
     AND parent.key_version = 'v1'
     AND parent.template_lineage_id IS NOT DISTINCT FROM proposed.template_lineage_id
     AND parent.template_version_id IS NOT DISTINCT FROM proposed.template_version_id
     AND parent.recipient_user_id = proposed.owner_user_id
     AND parent.recipient_role = 'MOTHER'
     AND parent.care_group_id = proposed.care_group_id
     AND parent.care_context_type = proposed.care_context_type
     AND parent.care_context_id = proposed.care_context_id
     AND parent.context_owner_user_id = proposed.owner_user_id
     AND parent.origin = proposed.origin
     AND parent.status = 'PENDING'
     AND parent.status = proposed.status
     AND parent.window_start IS NOT DISTINCT FROM CASE
         WHEN proposed.origin = 'SYSTEM_TEMPLATE' THEN proposed.occurrence_date END
     AND parent.window_end IS NOT DISTINCT FROM CASE
         WHEN proposed.origin = 'SYSTEM_TEMPLATE' THEN proposed.occurrence_date END
     AND parent.completed_at IS NOT DISTINCT FROM proposed.completed_at
     AND parent.cancelled_at IS NOT DISTINCT FROM proposed.cancelled_at
     AND parent.cancellation_reason_code IS NOT DISTINCT FROM CASE
         WHEN proposed.status = 'CANCELLED' THEN 'LEGACY_CANCELLED' END
     AND parent.created_at = proposed.created_at
     AND parent.updated_at = proposed.updated_at
    WHERE NOT EXISTS (
        SELECT 1 FROM public.checklist_task_instances child
        WHERE child.checklist_instance_id = parent.checklist_instance_id
    )
)
DELETE FROM public.checklist_instances parent
USING empty_repaired_parents empty
WHERE parent.checklist_instance_id = empty.repaired_parent_id
  AND NOT EXISTS (
      SELECT 1 FROM public.audit_events audit
      WHERE audit.resource_type = 'CHECKLIST_INSTANCE'
        AND audit.resource_id = parent.checklist_instance_id
  );

WITH prepared AS (
    SELECT
        result.source_id,
        result.reason_code,
        result.correlation_id,
        result.redacted_payload,
        encode(sha256(result.redacted_payload), 'hex') AS payload_hash
    FROM checklist_occurrence_repair_quarantine result
), inserted_quarantine AS (
    INSERT INTO public.checklist_migration_quarantine
        (source_table, source_id, reason_code, payload_ciphertext, payload_hash,
         encryption_key_version, correlation_id, retain_until)
    SELECT
        'preparation_checklist_items',
        prepared.source_id,
        prepared.reason_code,
        prepared.redacted_payload,
        prepared.payload_hash,
        'REDACTED_NO_PAYLOAD_V1',
        prepared.correlation_id,
        now() + interval '7 years'
    FROM prepared
    WHERE NOT EXISTS (
        SELECT 1
        FROM public.checklist_migration_quarantine existing
        WHERE existing.source_table = 'preparation_checklist_items'
          AND existing.source_id = prepared.source_id
          AND existing.reason_code = prepared.reason_code
    )
    RETURNING source_id, reason_code, correlation_id
)
INSERT INTO public.audit_events
    (actor_user_id, event_category, subject_reference_id,
     resource_type, resource_id, purpose, decision,
     occurred_at, created_at, event_origin, payload,
     correlation_id, severity, status,
     actor_type, actor_service, reason_code)
SELECT
    NULL,
    'CHECKLIST_MIGRATION_QUARANTINED',
    inserted.source_id,
    'LEGACY_CHECKLIST_ITEM',
    inserted.source_id,
    'LEGACY_CHECKLIST_OCCURRENCE_REPAIR',
    'QUARANTINED',
    now(),
    now(),
    'CHECKLIST_MIGRATION',
    jsonb_build_object(
        'sourceTable', 'preparation_checklist_items',
        'sourceId', inserted.source_id,
        'reasonCode', inserted.reason_code,
        'metadata', 'REDACTED'),
    inserted.correlation_id,
    'HIGH',
    'OPEN',
    'SERVICE',
    'CHECKLIST_LEGACY_OCCURRENCE_REPAIR',
    inserted.reason_code
FROM inserted_quarantine inserted;
