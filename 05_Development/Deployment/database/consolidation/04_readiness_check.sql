-- CareBridge database consolidation — R9 + R11b readiness check
-- Spec: Database_Table_Audit_And_Consolidation V3.md §3.8, §3.10
--       Database_Consolidation_Source_Code_Refactor_Plan.md §4.9, §4.11, §7.5
--
-- Read-only against user data. Prints one PASS/FAIL/INFO row per gate, so the
-- whole outstanding blocker list is a single command.
--
--   psql:      \i 04_readiness_check.sql
--   Supabase:  paste into the SQL editor and run
--
-- A check whose tables no longer exist reports SKIPPED rather than erroring, so
-- the script keeps working after the contract migrations have run.
--
-- IMPORTANT — the queue-stability gate cannot be answered by one run. Run this
-- script twice, at least 5 minutes apart, with the planners already disabled, and
-- compare the two INFO rows labelled 'queue depth snapshot'. Identical counts plus
-- PROCESSING = 0 is what plan §7.5 means by "queue quiesced".

DROP TABLE IF EXISTS pg_temp.consolidation_readiness;
CREATE TEMP TABLE consolidation_readiness (
    seq          integer,
    wave         text,
    gate         text,
    expected     text,
    actual       text,
    result       text
);

DO $outer$
DECLARE
    v_actual   bigint;
    v_present  boolean;
    v_detail   text;
BEGIN
    ------------------------------------------------------------------
    -- R11b — notification queue quiesce
    ------------------------------------------------------------------

    v_present := to_regclass('public.reminder_schedule_jobs') IS NOT NULL;
    IF v_present THEN
        EXECUTE 'SELECT count(*) FROM public.reminder_schedule_jobs WHERE status = ''PROCESSING'''
           INTO v_actual;
        INSERT INTO consolidation_readiness VALUES (
            1, 'R11b', 'reminder_schedule_jobs in PROCESSING', '0', v_actual::text,
            CASE WHEN v_actual = 0 THEN 'PASS' ELSE 'FAIL' END);
    ELSE
        INSERT INTO consolidation_readiness VALUES (
            1, 'R11b', 'reminder_schedule_jobs in PROCESSING', '0', 'table absent', 'SKIPPED');
    END IF;

    v_present := to_regclass('public.appointment_notification_jobs') IS NOT NULL;
    IF v_present THEN
        EXECUTE 'SELECT count(*) FROM public.appointment_notification_jobs WHERE status = ''PROCESSING'''
           INTO v_actual;
        INSERT INTO consolidation_readiness VALUES (
            2, 'R11b', 'appointment_notification_jobs in PROCESSING', '0', v_actual::text,
            CASE WHEN v_actual = 0 THEN 'PASS' ELSE 'FAIL' END);
    ELSE
        INSERT INTO consolidation_readiness VALUES (
            2, 'R11b', 'appointment_notification_jobs in PROCESSING', '0', 'table absent', 'SKIPPED');
    END IF;

    -- A shared job_id would make the final delta ambiguous. V3 §3.8 forbids
    -- resolving it with ON CONFLICT DO NOTHING, because that silently drops a job.
    IF to_regclass('public.reminder_schedule_jobs') IS NOT NULL
       AND to_regclass('public.appointment_notification_jobs') IS NOT NULL THEN
        EXECUTE '
            SELECT count(*) FROM (
                SELECT job_id FROM public.reminder_schedule_jobs
                INTERSECT
                SELECT job_id FROM public.appointment_notification_jobs) collided'
           INTO v_actual;
        INSERT INTO consolidation_readiness VALUES (
            3, 'R11b', 'job_id collision across the two source queues', '0', v_actual::text,
            CASE WHEN v_actual = 0 THEN 'PASS' ELSE 'FAIL' END);
    ELSE
        INSERT INTO consolidation_readiness VALUES (
            3, 'R11b', 'job_id collision across the two source queues', '0', 'table absent', 'SKIPPED');
    END IF;

    -- Backfill parity: every source row must already exist in the target, so the
    -- cutover only has to move the delta accumulated since the expand migration.
    IF to_regclass('public.notification_jobs') IS NOT NULL
       AND to_regclass('public.reminder_schedule_jobs') IS NOT NULL THEN
        EXECUTE '
            SELECT count(*) FROM public.reminder_schedule_jobs s
             WHERE NOT EXISTS (SELECT 1 FROM public.notification_jobs t
                                WHERE t.job_id = s.job_id
                                  AND t.job_type = ''REMINDER_SCHEDULE'')'
           INTO v_actual;
        INSERT INTO consolidation_readiness VALUES (
            4, 'R11b', 'reminder_schedule_jobs rows missing from notification_jobs',
            '0 (delta to replay at cutover)', v_actual::text,
            CASE WHEN v_actual = 0 THEN 'PASS' ELSE 'INFO' END);
    ELSE
        INSERT INTO consolidation_readiness VALUES (
            4, 'R11b', 'reminder_schedule_jobs rows missing from notification_jobs',
            '0', 'table absent', 'SKIPPED');
    END IF;

    IF to_regclass('public.notification_jobs') IS NOT NULL
       AND to_regclass('public.appointment_notification_jobs') IS NOT NULL THEN
        EXECUTE '
            SELECT count(*) FROM public.appointment_notification_jobs a
             WHERE NOT EXISTS (SELECT 1 FROM public.notification_jobs t
                                WHERE t.job_id = a.job_id
                                  AND t.job_type = ''APPOINTMENT'')'
           INTO v_actual;
        INSERT INTO consolidation_readiness VALUES (
            5, 'R11b', 'appointment_notification_jobs rows missing from notification_jobs',
            '0 (delta to replay at cutover)', v_actual::text,
            CASE WHEN v_actual = 0 THEN 'PASS' ELSE 'INFO' END);
    ELSE
        INSERT INTO consolidation_readiness VALUES (
            5, 'R11b', 'appointment_notification_jobs rows missing from notification_jobs',
            '0', 'table absent', 'SKIPPED');
    END IF;

    -- Stability snapshot: meaningless alone, decisive when compared across runs.
    IF to_regclass('public.reminder_schedule_jobs') IS NOT NULL
       AND to_regclass('public.appointment_notification_jobs') IS NOT NULL THEN
        EXECUTE '
            SELECT (SELECT count(*) FROM public.reminder_schedule_jobs)::text
                   || '' schedule / ''
                   || (SELECT count(*) FROM public.appointment_notification_jobs)::text
                   || '' appointment, at '' || now()::text'
           INTO v_detail;
        INSERT INTO consolidation_readiness VALUES (
            6, 'R11b', 'queue depth snapshot (compare two runs >= 5 min apart)',
            'identical across both runs', v_detail, 'INFO');
    END IF;

    ------------------------------------------------------------------
    -- R9 — legacy checklist backfill completeness
    ------------------------------------------------------------------

    IF to_regclass('public.preparation_checklist_items') IS NOT NULL
       AND to_regclass('public.checklist_task_instances') IS NOT NULL THEN
        EXECUTE '
            SELECT count(*) FROM public.preparation_checklist_items legacy
             WHERE NOT EXISTS (
                   SELECT 1 FROM public.checklist_task_instances task
                    WHERE task.checklist_task_instance_id = legacy.checklist_item_id)'
           INTO v_actual;
        INSERT INTO consolidation_readiness VALUES (
            7, 'R9', 'legacy checklist rows with no v2 task instance', '0', v_actual::text,
            CASE WHEN v_actual = 0 THEN 'PASS' ELSE 'FAIL' END);

        -- The ids that block the cutover, so the operator does not have to re-derive them.
        IF v_actual > 0 THEN
            EXECUTE '
                SELECT string_agg(checklist_item_id::text, '', '' ORDER BY checklist_item_id)
                  FROM (SELECT checklist_item_id
                          FROM public.preparation_checklist_items legacy
                         WHERE NOT EXISTS (
                               SELECT 1 FROM public.checklist_task_instances task
                                WHERE task.checklist_task_instance_id = legacy.checklist_item_id)
                         ORDER BY checklist_item_id LIMIT 20) blocking'
               INTO v_detail;
            INSERT INTO consolidation_readiness VALUES (
                8, 'R9', 'blocking legacy checklist_item_id values (first 20)',
                'none', v_detail, 'FAIL');
        END IF;
    ELSE
        INSERT INTO consolidation_readiness VALUES (
            7, 'R9', 'legacy checklist rows with no v2 task instance', '0',
            'table absent', 'SKIPPED');
    END IF;

    -- An open quarantine row is an item a real user may still be looking at.
    -- V3 §3.10: reconcile it; never downgrade it to USER_CREATED.
    IF to_regclass('public.checklist_migration_quarantine') IS NOT NULL THEN
        EXECUTE '
            SELECT count(*) FROM public.checklist_migration_quarantine
             WHERE source_table = ''preparation_checklist_items'' AND resolved_at IS NULL'
           INTO v_actual;
        INSERT INTO consolidation_readiness VALUES (
            9, 'R9', 'unresolved checklist quarantine rows', '0', v_actual::text,
            CASE WHEN v_actual = 0 THEN 'PASS' ELSE 'FAIL' END);

        IF v_actual > 0 THEN
            EXECUTE '
                SELECT string_agg(DISTINCT reason_code, '', '')
                  FROM public.checklist_migration_quarantine
                 WHERE source_table = ''preparation_checklist_items'' AND resolved_at IS NULL'
               INTO v_detail;
            INSERT INTO consolidation_readiness VALUES (
                10, 'R9', 'quarantine reason codes to resolve', 'none', v_detail, 'FAIL');
        END IF;
    ELSE
        INSERT INTO consolidation_readiness VALUES (
            9, 'R9', 'unresolved checklist quarantine rows', '0', 'table absent', 'SKIPPED');
    END IF;

    -- A duplicated deterministic key means two logical tasks share one identity.
    IF to_regclass('public.checklist_task_instances') IS NOT NULL THEN
        EXECUTE '
            SELECT count(*) FROM (
                SELECT checklist_instance_id, task_key
                  FROM public.checklist_task_instances
                 GROUP BY checklist_instance_id, task_key
                HAVING count(*) > 1) collided'
           INTO v_actual;
        INSERT INTO consolidation_readiness VALUES (
            11, 'R9', 'duplicate (checklist_instance_id, task_key) pairs', '0', v_actual::text,
            CASE WHEN v_actual = 0 THEN 'PASS' ELSE 'FAIL' END);
    ELSE
        INSERT INTO consolidation_readiness VALUES (
            11, 'R9', 'duplicate (checklist_instance_id, task_key) pairs', '0',
            'table absent', 'SKIPPED');
    END IF;
END
$outer$;

SELECT seq, wave, gate, expected, actual, result
  FROM consolidation_readiness
 ORDER BY seq;

-- Overall verdict, so a caller can branch on one row.
SELECT CASE
         WHEN count(*) FILTER (WHERE result = 'FAIL') > 0
              THEN 'BLOCKED — ' || count(*) FILTER (WHERE result = 'FAIL')::text || ' gate(s) failing'
         ELSE 'ALL GATES PASS — R9 cutover and R11b final delta may proceed'
       END AS verdict
  FROM consolidation_readiness;
