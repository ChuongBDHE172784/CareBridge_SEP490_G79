-- CareBridge database consolidation — R7 expand + backfill
-- Spec: 08_References/Database_Table_Audit_And_Consolidation V3.md §3.7
-- Code: Database_Consolidation_Source_Code_Refactor_Plan.md §4.8
--
-- Additive only. appointment_notification_rules keeps existing and keeps receiving
-- writes through the observation window; it is the rollback path and is dropped by
-- a separate contract migration.

-- ---------------------------------------------------------------------------
-- 1. Validator
-- ---------------------------------------------------------------------------
-- Closed schema v1: the array holds objects whose only key is offsetMinutes.
-- Rejecting unknown keys here is what stops a future writer from smuggling
-- semantics into the JSON that the planner does not read.
CREATE OR REPLACE FUNCTION public.carebridge_validate_appointment_rules(p_rules jsonb)
RETURNS boolean
LANGUAGE sql
IMMUTABLE
STRICT
PARALLEL SAFE
AS $$
    SELECT
        jsonb_typeof(p_rules) = 'array'
        -- Same ceiling the application applies (MAX_RULES).
        AND jsonb_array_length(p_rules) <= 10
        AND NOT EXISTS (
            SELECT 1
            FROM jsonb_array_elements(p_rules) AS element
            WHERE jsonb_typeof(element) <> 'object'
               OR (element -> 'offsetMinutes') IS NULL
               -- Closed schema: offsetMinutes and nothing else.
               OR (SELECT count(*) FROM jsonb_object_keys(element)) <> 1
               OR jsonb_typeof(element -> 'offsetMinutes') <> 'number'
               -- Integer, not 30.5: a fractional offset has no meaning in minutes.
               OR (element ->> 'offsetMinutes') !~ '^-?[0-9]+$'
               OR ((element ->> 'offsetMinutes')::bigint < -43200)
               OR ((element ->> 'offsetMinutes')::bigint > 10080)
        )
        -- No duplicate offset: two identical offsets would produce two identical
        -- jobs for one occurrence, which the partial unique index would then reject.
        AND (
            SELECT count(DISTINCT element ->> 'offsetMinutes')
            FROM jsonb_array_elements(p_rules) AS element
        ) = jsonb_array_length(p_rules)
$$;

COMMENT ON FUNCTION public.carebridge_validate_appointment_rules(jsonb) IS
    'R7: closed schema v1 for appointment_notification_configs.rules_jsonb — array of {offsetMinutes:int in [-43200,10080]}, no duplicates, max 10. Array order replaces sort_order.';

-- ---------------------------------------------------------------------------
-- 2. Expand
-- ---------------------------------------------------------------------------
ALTER TABLE public.appointment_notification_configs
    ADD COLUMN IF NOT EXISTS rules_jsonb jsonb NOT NULL DEFAULT '[]'::jsonb;

-- ---------------------------------------------------------------------------
-- 3. Backfill
-- ---------------------------------------------------------------------------
-- Ordering is (sort_order, offset_minutes), matching the read path. Idempotent:
-- only still-empty arrays are filled, so a rerun cannot clobber the new writer.
--
-- config_revision is deliberately NOT bumped here. The effective configuration is
-- unchanged — only its representation moves — while every PENDING job carries the
-- config_revision it was materialised from. Bumping would make those jobs look
-- obsolete to cancelObsoleteRevisions and silently cancel valid notifications.
-- Revision bumps stay where they belong: in the same transaction as a real rules
-- change, on the application path.
UPDATE public.appointment_notification_configs c
   SET rules_jsonb = ordered.rules
  FROM (
        SELECT reminder_id,
               jsonb_agg(jsonb_build_object('offsetMinutes', offset_minutes)
                         ORDER BY sort_order, offset_minutes) AS rules
          FROM public.appointment_notification_rules
         GROUP BY reminder_id
       ) AS ordered
 WHERE c.reminder_id = ordered.reminder_id
   AND jsonb_array_length(c.rules_jsonb) = 0;

-- ---------------------------------------------------------------------------
-- 4. Reconciliation gate
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    v_mismatched bigint;
    v_source_rows bigint;
    v_migrated_offsets bigint;
BEGIN
    SELECT count(*) INTO v_mismatched
    FROM (
        SELECT reminder_id,
               jsonb_agg(jsonb_build_object('offsetMinutes', offset_minutes)
                         ORDER BY sort_order, offset_minutes) AS source_rules
          FROM public.appointment_notification_rules
         GROUP BY reminder_id
    ) src
    JOIN public.appointment_notification_configs c ON c.reminder_id = src.reminder_id
    WHERE c.rules_jsonb IS DISTINCT FROM src.source_rules;

    IF v_mismatched > 0 THEN
        RAISE EXCEPTION
            'R7_BACKFILL_MISMATCH: % config(s) whose rules_jsonb does not match appointment_notification_rules',
            v_mismatched;
    END IF;

    -- Every source row must be represented; a lost offset is a lost notification.
    SELECT count(*) INTO v_source_rows FROM public.appointment_notification_rules;
    SELECT coalesce(sum(jsonb_array_length(rules_jsonb)), 0) INTO v_migrated_offsets
      FROM public.appointment_notification_configs c
     WHERE EXISTS (SELECT 1 FROM public.appointment_notification_rules r
                    WHERE r.reminder_id = c.reminder_id);

    IF v_source_rows <> v_migrated_offsets THEN
        RAISE EXCEPTION
            'R7_BACKFILL_COUNT_MISMATCH: % source rule row(s) but % migrated offset(s)',
            v_source_rows, v_migrated_offsets;
    END IF;
END
$$;

-- ---------------------------------------------------------------------------
-- 5. Constraint
-- ---------------------------------------------------------------------------
ALTER TABLE public.appointment_notification_configs
    DROP CONSTRAINT IF EXISTS appointment_notification_configs_rules_ck;
ALTER TABLE public.appointment_notification_configs
    ADD CONSTRAINT appointment_notification_configs_rules_ck
    CHECK (public.carebridge_validate_appointment_rules(rules_jsonb));
