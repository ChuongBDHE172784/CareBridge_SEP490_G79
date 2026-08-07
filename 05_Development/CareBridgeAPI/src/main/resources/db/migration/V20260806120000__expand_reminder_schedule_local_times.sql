-- CareBridge database consolidation — R6 expand + backfill
-- Spec: 08_References/Database_Table_Audit_And_Consolidation V3.md §3.6
-- Code: Database_Consolidation_Source_Code_Refactor_Plan.md §4.7
--
-- Additive only. reminder_schedule_times keeps existing and keeps receiving writes
-- through the observation window; it is the rollback path and is dropped by a
-- separate contract migration once the array has been observed in production.

-- ---------------------------------------------------------------------------
-- 1. Validator
-- ---------------------------------------------------------------------------
-- PostgreSQL cannot put a subquery inside CHECK, so the collection rules live in
-- an IMMUTABLE function that the constraint calls. Marked STRICT: a NULL array is
-- judged by the separate active/cardinality constraint below, not here.
CREATE OR REPLACE FUNCTION public.carebridge_validate_reminder_local_times(p_times time[])
RETURNS boolean
LANGUAGE sql
IMMUTABLE
STRICT
PARALLEL SAFE
AS $$
    SELECT
        -- No NULL element: a reminder with an unknown time cannot be scheduled.
        array_position(p_times, NULL) IS NULL
        -- No duplicate: the source table enforced this with a UNIQUE constraint,
        -- and a duplicate would materialise two identical jobs per occurrence.
        AND cardinality(p_times) = (
            SELECT count(DISTINCT t) FROM unnest(p_times) AS t
        )
        -- Same ceiling the application applies (MAX_TIMES).
        AND cardinality(p_times) <= 96
$$;

COMMENT ON FUNCTION public.carebridge_validate_reminder_local_times(time[]) IS
    'R6: element/duplicate/size rules for reminder_schedules.local_times. Array order is display and execution order and is deliberately not constrained.';

-- ---------------------------------------------------------------------------
-- 2. Expand
-- ---------------------------------------------------------------------------
ALTER TABLE public.reminder_schedules
    ADD COLUMN IF NOT EXISTS local_times time[] NOT NULL DEFAULT '{}'::time[];

-- ---------------------------------------------------------------------------
-- 3. Backfill
-- ---------------------------------------------------------------------------
-- Ordering is (sort_order, local_time) exactly as the read path did, so the array
-- preserves the display order users already see. Idempotent: only empty arrays
-- are filled, so a rerun cannot clobber values written by the new code.
UPDATE public.reminder_schedules s
   SET local_times = ordered.times
  FROM (
        SELECT schedule_id,
               array_agg(local_time ORDER BY sort_order, local_time) AS times
          FROM public.reminder_schedule_times
         GROUP BY schedule_id
       ) AS ordered
 WHERE s.schedule_id = ordered.schedule_id
   AND cardinality(s.local_times) = 0;

-- ---------------------------------------------------------------------------
-- 4. Reconciliation gate
-- ---------------------------------------------------------------------------
-- Expected counts per V3 §7.2: every schedule that had child rows now holds the
-- same number of times, in the same order.
DO $$
DECLARE
    v_mismatched bigint;
BEGIN
    SELECT count(*) INTO v_mismatched
    FROM (
        SELECT schedule_id,
               array_agg(local_time ORDER BY sort_order, local_time) AS source_times
          FROM public.reminder_schedule_times
         GROUP BY schedule_id
    ) src
    JOIN public.reminder_schedules s ON s.schedule_id = src.schedule_id
    WHERE s.local_times IS DISTINCT FROM src.source_times;

    IF v_mismatched > 0 THEN
        RAISE EXCEPTION
            'R6_BACKFILL_MISMATCH: % schedule(s) whose local_times do not match reminder_schedule_times',
            v_mismatched;
    END IF;
END
$$;

-- ---------------------------------------------------------------------------
-- 5. Constraints
-- ---------------------------------------------------------------------------
-- Added after the backfill so existing rows are already valid.
ALTER TABLE public.reminder_schedules
    DROP CONSTRAINT IF EXISTS reminder_schedules_local_times_ck;
ALTER TABLE public.reminder_schedules
    ADD CONSTRAINT reminder_schedules_local_times_ck
    CHECK (public.carebridge_validate_reminder_local_times(local_times));

-- An active schedule with no time would never fire; an inactive one is allowed to
-- be empty so deactivating never has to destroy the configured times.
ALTER TABLE public.reminder_schedules
    DROP CONSTRAINT IF EXISTS reminder_schedules_active_times_ck;
ALTER TABLE public.reminder_schedules
    ADD CONSTRAINT reminder_schedules_active_times_ck
    CHECK (active = false OR cardinality(local_times) > 0);
