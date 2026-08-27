-- Backfill legacy preparation checklist rows into recipient-owned V2 projections.
-- A missing due_at remains active with due_at NULL and is projected as UNSCHEDULED.
-- Invalid, ambiguous, or conflicting rows are quarantined with redacted markers and
-- one typed audit event per newly persisted quarantine result.

-- This session-local helper is byte-for-byte compatible with
-- ChecklistDistributionKeyFactory v1: "v1" plus UTF-8 byte-length-prefixed tokens,
-- hashed once with SHA-256 and rendered as lowercase hexadecimal.
CREATE TEMP TABLE checklist_legacy_key_bootstrap (marker boolean) ON COMMIT DROP;

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

-- Approved runtime v1 golden vector. Abort before touching data if the SQL
-- implementation ever drifts from the Java implementation.
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

CREATE TEMP TABLE checklist_legacy_backfill_stage ON COMMIT DROP AS
SELECT
    legacy.checklist_item_id AS source_id,
    legacy.owner_user_id,
    legacy.title,
    COALESCE(legacy.display_order, 0) AS display_order,
    legacy.status AS legacy_status,
    legacy.due_at,
    legacy.completed_at,
    legacy.created_at,
    legacy.updated_at,
    legacy.template_entry_id,
    CASE
        WHEN legacy.baby_id IS NOT NULL AND legacy.mother_journey_id IS NULL THEN 'BABY'
        WHEN legacy.mother_journey_id IS NOT NULL AND legacy.baby_id IS NULL THEN 'JOURNEY'
        ELSE NULL
    END AS care_context_type,
    CASE
        WHEN legacy.baby_id IS NOT NULL AND legacy.mother_journey_id IS NULL THEN legacy.baby_id
        WHEN legacy.mother_journey_id IS NOT NULL AND legacy.baby_id IS NULL THEN legacy.mother_journey_id
        ELSE NULL
    END AS care_context_id,
    context_match.care_group_id,
    COALESCE(context_match.match_count, 0) AS context_match_count,
    root.template_lineage_id,
    root.template_version_id,
    item.template_id AS template_item_version_id,
    COALESCE(item.target_subject,
        CASE WHEN legacy.baby_id IS NOT NULL THEN 'BABY' ELSE 'MOTHER' END) AS target_subject,
    CASE WHEN legacy.template_entry_id IS NULL THEN 'USER_CREATED' ELSE 'SYSTEM_TEMPLATE' END AS origin,
    CASE WHEN legacy.template_entry_id IS NULL THEN '<ABSENT>' ELSE 'NONE' END AS occurrence_start_token,
    CASE WHEN legacy.template_entry_id IS NULL THEN '<ABSENT>' ELSE 'NONE' END AS occurrence_end_token,
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
             (item.template_id IS NULL OR root.template_version_id IS NULL OR root.template_lineage_id IS NULL)
            THEN 'UNKNOWN_TEMPLATE_ROOT'
        WHEN legacy.title IS NULL OR btrim(legacy.title) = ''
            THEN 'INVALID_LEGACY_TITLE'
        WHEN legacy.status IS NULL OR upper(legacy.status) NOT IN
             ('OPEN','PENDING','IN_PROGRESS','COMPLETED','DONE','SKIPPED','CANCELLED')
            THEN 'UNKNOWN_LEGACY_STATUS'
        WHEN legacy.completed_at IS NOT NULL AND upper(legacy.status) IN ('OPEN','PENDING','IN_PROGRESS')
            THEN 'CONTRADICTORY_LEGACY_TIMESTAMPS'
        ELSE NULL
    END AS reason_code
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
      AND candidate.review_status <> 'BLOCKED'
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

CREATE TEMP TABLE checklist_legacy_quarantine_results (
    source_id uuid NOT NULL,
    reason_code varchar(80) NOT NULL,
    correlation_id uuid NOT NULL DEFAULT gen_random_uuid(),
    redacted_payload bytea NOT NULL DEFAULT sha256(convert_to(
        gen_random_uuid()::text || clock_timestamp()::text || random()::text,
        'UTF8')),
    payload_hash char(64),
    CONSTRAINT checklist_legacy_quarantine_results_uk UNIQUE (source_id, reason_code)
) ON COMMIT DROP;

INSERT INTO checklist_legacy_quarantine_results (source_id, reason_code)
SELECT stage.source_id, stage.reason_code
FROM checklist_legacy_backfill_stage stage
WHERE stage.reason_code IS NOT NULL;

-- Calculate the exact runtime parent key first, then group all legacy children by
-- owner/context/version/occurrence instead of creating one parent per legacy row.
CREATE TEMP TABLE checklist_legacy_backfill_rows ON COMMIT DROP AS
WITH keyed AS (
    SELECT
        stage.*,
        CASE
            WHEN stage.origin = 'SYSTEM_TEMPLATE' THEN pg_temp.checklist_v1_key(
                stage.template_version_id::text,
                stage.owner_user_id::text,
                'MOTHER',
                stage.care_group_id::text,
                stage.care_context_type,
                stage.care_context_id::text,
                stage.occurrence_start_token,
                stage.occurrence_end_token)
            ELSE pg_temp.checklist_v1_key(
                stage.owner_user_id::text,
                'MOTHER',
                stage.care_group_id::text,
                stage.care_context_type,
                stage.care_context_id::text,
                stage.occurrence_start_token,
                stage.occurrence_end_token)
        END AS legacy_parent_group_key
    FROM checklist_legacy_backfill_stage stage
    WHERE stage.reason_code IS NULL
), identified AS (
    SELECT
        keyed.*,
        pg_temp.checklist_v1_key('LEGACY_PARENT_ID', keyed.legacy_parent_group_key) AS parent_identity_hash
    FROM keyed
)
SELECT
    identified.*,
    (substr(parent_identity_hash, 1, 8) || '-' ||
     substr(parent_identity_hash, 9, 4) || '-' ||
     substr(parent_identity_hash, 13, 4) || '-' ||
     substr(parent_identity_hash, 17, 4) || '-' ||
     substr(parent_identity_hash, 21, 12))::uuid AS parent_instance_id,
    row_number() OVER (
        PARTITION BY legacy_parent_group_key
        ORDER BY display_order, source_id) AS legacy_child_order
FROM identified;

CREATE TEMP TABLE checklist_legacy_parent_stage ON COMMIT DROP AS
SELECT
    legacy_parent_group_key,
    parent_instance_id,
    template_lineage_id,
    template_version_id,
    owner_user_id,
    care_group_id,
    care_context_type,
    care_context_id,
    origin,
    CASE
        WHEN bool_and(upper(legacy_status) = 'CANCELLED') THEN 'CANCELLED'
        WHEN bool_and(upper(legacy_status) IN ('COMPLETED','DONE','SKIPPED')) THEN 'COMPLETED'
        WHEN bool_or(upper(legacy_status) IN ('IN_PROGRESS','COMPLETED','DONE','SKIPPED')) THEN 'IN_PROGRESS'
        ELSE 'PENDING'
    END AS status,
    CASE
        WHEN bool_and(upper(legacy_status) IN ('COMPLETED','DONE','SKIPPED'))
            THEN max(COALESCE(completed_at, updated_at, created_at))
    END AS completed_at,
    CASE
        WHEN bool_and(upper(legacy_status) = 'CANCELLED')
            THEN max(COALESCE(updated_at, created_at))
    END AS cancelled_at,
    min(created_at) AS created_at,
    max(updated_at) AS updated_at
FROM checklist_legacy_backfill_rows
GROUP BY
    legacy_parent_group_key,
    parent_instance_id,
    template_lineage_id,
    template_version_id,
    owner_user_id,
    care_group_id,
    care_context_type,
    care_context_id,
    origin;

-- A deterministic parent id that already names a different payload is drift.
INSERT INTO checklist_legacy_quarantine_results (source_id, reason_code)
SELECT row.source_id, 'LEGACY_PARENT_PAYLOAD_DRIFT'
FROM checklist_legacy_backfill_rows row
JOIN checklist_legacy_parent_stage proposed
  ON proposed.legacy_parent_group_key = row.legacy_parent_group_key
JOIN public.checklist_instances existing
  ON existing.checklist_instance_id = proposed.parent_instance_id
WHERE existing.distribution_key IS DISTINCT FROM proposed.legacy_parent_group_key
   OR existing.template_lineage_id IS DISTINCT FROM proposed.template_lineage_id
   OR existing.template_version_id IS DISTINCT FROM proposed.template_version_id
   OR existing.recipient_user_id IS DISTINCT FROM proposed.owner_user_id
   OR existing.recipient_role IS DISTINCT FROM 'MOTHER'
   OR existing.care_group_id IS DISTINCT FROM proposed.care_group_id
   OR existing.care_context_type IS DISTINCT FROM proposed.care_context_type
   OR existing.care_context_id IS DISTINCT FROM proposed.care_context_id
   OR existing.context_owner_user_id IS DISTINCT FROM proposed.owner_user_id
   OR existing.origin IS DISTINCT FROM proposed.origin;

-- A runtime key already owned by another parent id is a collision. Never silently
-- reuse it because child identity is derived from the parent id.
INSERT INTO checklist_legacy_quarantine_results (source_id, reason_code)
SELECT row.source_id, 'LEGACY_DISTRIBUTION_KEY_COLLISION'
FROM checklist_legacy_backfill_rows row
JOIN checklist_legacy_parent_stage proposed
  ON proposed.legacy_parent_group_key = row.legacy_parent_group_key
JOIN public.checklist_instances existing
  ON existing.distribution_key = proposed.legacy_parent_group_key
WHERE existing.checklist_instance_id <> proposed.parent_instance_id
  AND NOT EXISTS (
      SELECT 1
      FROM checklist_legacy_quarantine_results quarantined
      WHERE quarantined.source_id = row.source_id
        AND quarantined.reason_code = 'LEGACY_DISTRIBUTION_KEY_COLLISION');

INSERT INTO public.checklist_instances
    (checklist_instance_id, distribution_key, key_version,
     template_lineage_id, template_version_id,
     recipient_user_id, recipient_role, care_group_id,
     care_context_type, care_context_id, context_owner_user_id,
     origin, window_start, window_end, status,
     completed_at, cancelled_at, cancellation_reason_code,
     created_at, updated_at)
SELECT
    proposed.parent_instance_id,
    proposed.legacy_parent_group_key,
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
    NULL,
    NULL,
    proposed.status,
    proposed.completed_at,
    proposed.cancelled_at,
    CASE WHEN proposed.status = 'CANCELLED' THEN 'LEGACY_CANCELLED' END,
    proposed.created_at,
    proposed.updated_at
FROM checklist_legacy_parent_stage proposed
WHERE NOT EXISTS (
        SELECT 1
        FROM checklist_legacy_backfill_rows row
        JOIN checklist_legacy_quarantine_results quarantined
          ON quarantined.source_id = row.source_id
        WHERE row.legacy_parent_group_key = proposed.legacy_parent_group_key
    )
  AND NOT EXISTS (
        SELECT 1
        FROM public.checklist_instances existing
        WHERE existing.checklist_instance_id = proposed.parent_instance_id
    )
  AND NOT EXISTS (
        SELECT 1
        FROM public.checklist_instances existing
        WHERE existing.distribution_key = proposed.legacy_parent_group_key
    );

CREATE TEMP TABLE checklist_legacy_child_stage ON COMMIT DROP AS
SELECT
    row.*,
    CASE
        WHEN row.origin = 'SYSTEM_TEMPLATE' THEN pg_temp.checklist_v1_key(
            row.parent_instance_id::text,
            row.template_item_version_id::text)
        ELSE pg_temp.checklist_v1_key(
            row.parent_instance_id::text,
            'USER_CREATED',
            row.source_id::text)
    END AS legacy_task_key
FROM checklist_legacy_backfill_rows row
WHERE NOT EXISTS (
    SELECT 1
    FROM checklist_legacy_quarantine_results quarantined
    WHERE quarantined.source_id = row.source_id
);

-- Duplicate runtime child identities inside one parent are key collisions, even
-- when the source primary keys differ.
INSERT INTO checklist_legacy_quarantine_results (source_id, reason_code)
SELECT child.source_id, 'LEGACY_TASK_KEY_COLLISION'
FROM checklist_legacy_child_stage child
JOIN (
    SELECT legacy_task_key
    FROM checklist_legacy_child_stage
    GROUP BY legacy_task_key
    HAVING count(*) > 1
) duplicate_key ON duplicate_key.legacy_task_key = child.legacy_task_key;

INSERT INTO checklist_legacy_quarantine_results (source_id, reason_code)
SELECT child.source_id, 'LEGACY_TASK_PAYLOAD_DRIFT'
FROM checklist_legacy_child_stage child
JOIN public.checklist_task_instances existing
  ON existing.checklist_task_instance_id = child.source_id
WHERE existing.checklist_instance_id IS DISTINCT FROM child.parent_instance_id
   OR existing.template_version_id IS DISTINCT FROM child.template_version_id
   OR existing.template_item_version_id IS DISTINCT FROM child.template_item_version_id
   OR existing.task_key IS DISTINCT FROM child.legacy_task_key
   OR existing.title_snapshot IS DISTINCT FROM child.title
   OR existing.display_order IS DISTINCT FROM (child.legacy_child_order - 1)::integer
   OR existing.target_subject IS DISTINCT FROM child.target_subject
   OR existing.due_at IS DISTINCT FROM child.due_at;

INSERT INTO checklist_legacy_quarantine_results (source_id, reason_code)
SELECT child.source_id, 'LEGACY_TASK_KEY_COLLISION'
FROM checklist_legacy_child_stage child
JOIN public.checklist_task_instances existing
  ON existing.task_key = child.legacy_task_key
WHERE existing.checklist_task_instance_id <> child.source_id
  AND NOT EXISTS (
      SELECT 1
      FROM checklist_legacy_quarantine_results quarantined
      WHERE quarantined.source_id = child.source_id
        AND quarantined.reason_code = 'LEGACY_TASK_KEY_COLLISION');

INSERT INTO public.checklist_task_instances
    (checklist_task_instance_id, checklist_instance_id,
     template_version_id, template_item_version_id,
     task_key, key_version, title_snapshot, display_order,
     is_required, target_subject, due_at, status,
     completed_at, skipped_at, cancelled_at, action_reason_code,
     created_at, updated_at)
SELECT
    child.source_id,
    child.parent_instance_id,
    child.template_version_id,
    child.template_item_version_id,
    child.legacy_task_key,
    'v1',
    child.title,
    (child.legacy_child_order - 1)::integer,
    false,
    child.target_subject,
    child.due_at,
    CASE
        WHEN upper(child.legacy_status) IN ('COMPLETED','DONE') THEN 'COMPLETED'
        WHEN upper(child.legacy_status) = 'SKIPPED' THEN 'SKIPPED'
        WHEN upper(child.legacy_status) = 'CANCELLED' THEN 'CANCELLED'
        WHEN upper(child.legacy_status) = 'IN_PROGRESS' THEN 'IN_PROGRESS'
        ELSE 'PENDING'
    END,
    CASE WHEN upper(child.legacy_status) IN ('COMPLETED','DONE')
        THEN COALESCE(child.completed_at, child.updated_at, child.created_at) END,
    CASE WHEN upper(child.legacy_status) = 'SKIPPED'
        THEN COALESCE(child.completed_at, child.updated_at, child.created_at) END,
    CASE WHEN upper(child.legacy_status) = 'CANCELLED'
        THEN COALESCE(child.updated_at, child.created_at) END,
    CASE
        WHEN upper(child.legacy_status) = 'SKIPPED' THEN 'LEGACY_SKIPPED'
        WHEN upper(child.legacy_status) = 'CANCELLED' THEN 'LEGACY_CANCELLED'
    END,
    child.created_at,
    child.updated_at
FROM checklist_legacy_child_stage child
WHERE NOT EXISTS (
        SELECT 1
        FROM checklist_legacy_quarantine_results quarantined
        WHERE quarantined.source_id = child.source_id
    )
  AND NOT EXISTS (
        SELECT 1
        FROM public.checklist_task_instances existing
        WHERE existing.checklist_task_instance_id = child.source_id
    )
  AND NOT EXISTS (
        SELECT 1
        FROM public.checklist_task_instances existing
        WHERE existing.task_key = child.legacy_task_key
    );

UPDATE checklist_legacy_quarantine_results
SET payload_hash = encode(sha256(redacted_payload), 'hex')
WHERE payload_hash IS NULL;

-- Persist quarantine and its typed audit record in one data-modifying statement so
-- the exact generated correlation id is shared by both rows. The audit payload is
-- controlled metadata only; legacy title/body text is never copied.
WITH inserted_quarantine AS (
    INSERT INTO public.checklist_migration_quarantine
        (source_table, source_id, reason_code, payload_ciphertext, payload_hash,
         encryption_key_version, correlation_id, retain_until)
    SELECT
        'preparation_checklist_items',
        result.source_id,
        result.reason_code,
        result.redacted_payload,
        result.payload_hash,
        'REDACTED_NO_PAYLOAD_V1',
        result.correlation_id,
        now() + interval '7 years'
    FROM checklist_legacy_quarantine_results result
    WHERE NOT EXISTS (
        SELECT 1
        FROM public.checklist_migration_quarantine existing
        WHERE existing.source_table = 'preparation_checklist_items'
          AND existing.source_id = result.source_id
          AND existing.reason_code = result.reason_code
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
    'LEGACY_CHECKLIST_V2_BACKFILL',
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
    'CHECKLIST_LEGACY_BACKFILL',
    inserted.reason_code
FROM inserted_quarantine inserted;
