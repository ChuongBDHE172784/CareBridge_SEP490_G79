-- CareBridge database consolidation — persistence contract (queue + expand sources)
-- Spec: 08_References/Database_Table_Audit_And_Consolidation V3.md §3.6-§3.11, §5 waves 5/7
-- Code: Database_Consolidation_Source_Code_Refactor_Plan.md §6 (R13), §7.5
--
-- Promoted from 06_contract_drop_source_tables.sql once the observation window
-- closed. Measured on the linked project on 2026-08-07, all eleven gates of
-- 05_r11b_observation.sql reported PASS:
--
--   216 jobs planned across 3 distinct planner cycles since the cutover
--   1 retry path and 1 stale-lock requeue exercised
--   source queues still frozen at their cutover counts (221 / 10)
--   no duplicate job identity and no duplicate notification record
--
-- R12 had already removed every mapping and query against these tables; the static
-- scan reported 11/11 retired symbols at zero references.
--
-- After this commits, rollback is forward-fix or restore only. On this database the
-- Release Owner waived PITR/backups, so there is no restore: the data in these
-- tables is gone for good. The gates below therefore re-check every precondition
-- rather than trusting that record.

-- ---------------------------------------------------------------------------
-- 1. Gate — the application must be off these tables already
-- ---------------------------------------------------------------------------
-- This gate originally required that nothing had touched the source queues since
-- the notification_jobs cutover. On 2026-08-07 it fired for real: a second backend
-- instance, still running a pre-R12 build, worked reminder_schedule_jobs job
-- 16ab73b4-4254-4437-aee8-99ab0a2c6448 from attempt 3 to attempt 4 at 04:54:17,
-- seven seconds before this build worked the same job in notification_jobs. The
-- refusal was correct and nothing was dropped.
--
-- The Release Owner has since stopped that instance, but "changed since cutover"
-- cannot be satisfied any more: the stale instance's write is a permanent fact of
-- the row's updated_at. So the reference point moves from the cutover to a rolling
-- quiet window — the queues must have been untouched for long enough that a live
-- worker would have shown itself. The retired instance polled roughly every two
-- minutes, so the window below is several times its cadence.
--
-- Be clear about what this can and cannot prove. A quiet window detects a *running*
-- writer; it cannot distinguish one that is merely idle because no work is due
-- (203 rows here sit at attempt_count 0, untouched since 2026-08-05, simply because
-- they are not yet due). What makes the drop safe is the combination of this
-- window, the operator's confirmation that the stale instance is stopped, and the
-- parity checks below — which guarantee that even if a writer did reappear, no
-- scheduled notification exists only in a table this migration removes.
DO $$
DECLARE
    v_required interval := interval '10 minutes';
    v_newest timestamptz;
    v_quiet interval;
BEGIN
    IF NOT EXISTS (SELECT 1 FROM public.flyway_schema_history WHERE version = '20260806160000') THEN
        RAISE EXCEPTION 'CONTRACT_GATE_FAILED: the notification_jobs expand migration has not run here';
    END IF;

    SELECT max(newest) INTO v_newest FROM (
        SELECT max(greatest(created_at, updated_at)) AS newest FROM public.reminder_schedule_jobs
        UNION ALL
        SELECT max(greatest(created_at, updated_at)) FROM public.appointment_notification_jobs
    ) AS writes;

    -- Empty queues are trivially quiescent.
    IF v_newest IS NOT NULL THEN
        v_quiet := now() - v_newest;
        IF v_quiet < v_required THEN
            RAISE EXCEPTION
                'CONTRACT_GATE_FAILED: source queues were last written % ago (need % of quiet); something may still write to them',
                v_quiet, v_required;
        END IF;
    END IF;
END
$$;

-- Every source job must have a counterpart in the consolidated queue, or dropping
-- the table loses a scheduled notification.
DO $$
DECLARE
    v_missing bigint;
BEGIN
    SELECT count(*) INTO v_missing
      FROM public.reminder_schedule_jobs s
     WHERE NOT EXISTS (SELECT 1 FROM public.notification_jobs t
                        WHERE t.job_id = s.job_id AND t.job_type = 'REMINDER_SCHEDULE');
    IF v_missing > 0 THEN
        RAISE EXCEPTION 'CONTRACT_GATE_FAILED: % reminder-schedule job(s) absent from notification_jobs', v_missing;
    END IF;

    SELECT count(*) INTO v_missing
      FROM public.appointment_notification_jobs a
     WHERE NOT EXISTS (SELECT 1 FROM public.notification_jobs t
                        WHERE t.job_id = a.job_id AND t.job_type = 'APPOINTMENT');
    IF v_missing > 0 THEN
        RAISE EXCEPTION 'CONTRACT_GATE_FAILED: % appointment job(s) absent from notification_jobs', v_missing;
    END IF;
END
$$;

-- The expand sources must likewise be fully represented in their targets.
DO $$
DECLARE
    v_bad bigint;
BEGIN
    SELECT count(*) INTO v_bad
      FROM (SELECT schedule_id, array_agg(local_time ORDER BY sort_order, local_time) AS times
              FROM public.reminder_schedule_times GROUP BY schedule_id) src
      JOIN public.reminder_schedules s ON s.schedule_id = src.schedule_id
     WHERE s.local_times IS DISTINCT FROM src.times;
    IF v_bad > 0 THEN
        RAISE EXCEPTION 'CONTRACT_GATE_FAILED: % schedule(s) whose local_times diverge from the child table', v_bad;
    END IF;

    SELECT count(*) INTO v_bad
      FROM (SELECT reminder_id,
                   jsonb_agg(jsonb_build_object('offsetMinutes', offset_minutes)
                             ORDER BY sort_order, offset_minutes) AS rules
              FROM public.appointment_notification_rules GROUP BY reminder_id) src
      JOIN public.appointment_notification_configs c ON c.reminder_id = src.reminder_id
     WHERE c.rules_jsonb IS DISTINCT FROM src.rules;
    IF v_bad > 0 THEN
        RAISE EXCEPTION 'CONTRACT_GATE_FAILED: % config(s) whose rules_jsonb diverges from the rule table', v_bad;
    END IF;

    SELECT count(*) INTO v_bad
      FROM public.safety_configs sc
      JOIN public.users u ON u.user_id = sc.user_id
     WHERE (u.fall_detection_enabled, u.fall_detection_sensitivity_level, u.emergency_auto_alert,
            u.emergency_countdown_seconds, u.sensor_permission_granted)
        IS DISTINCT FROM
           (sc.fall_detection_enabled, sc.sensitivity_level, sc.emergency_auto_alert,
            sc.countdown_seconds, sc.sensor_permission_granted);
    IF v_bad > 0 THEN
        RAISE EXCEPTION 'CONTRACT_GATE_FAILED: % user(s) whose safety columns diverge from safety_configs', v_bad;
    END IF;

    SELECT count(*) INTO v_bad
      FROM public.consultation_sessions s
     WHERE NOT EXISTS (SELECT 1 FROM public.consultation_bookings b
                        WHERE b.legacy_session_id = s.session_id);
    IF v_bad > 0 THEN
        RAISE EXCEPTION 'CONTRACT_GATE_FAILED: % consultation session(s) not migrated onto a booking', v_bad;
    END IF;
END
$$;

-- ---------------------------------------------------------------------------
-- 2. Drop, children before parents, no CASCADE anywhere
-- ---------------------------------------------------------------------------
-- Named constraints go first so an unexpected dependency aborts the migration
-- instead of being silently swept away.

ALTER TABLE public.reminder_schedule_jobs
    DROP CONSTRAINT IF EXISTS reminder_schedule_jobs_schedule_fk,
    DROP CONSTRAINT IF EXISTS reminder_schedule_jobs_record_fk;
DROP TABLE IF EXISTS public.reminder_schedule_jobs;

ALTER TABLE public.appointment_notification_jobs
    DROP CONSTRAINT IF EXISTS appointment_notification_jobs_reminder_fk,
    DROP CONSTRAINT IF EXISTS appointment_notification_jobs_record_fk;
DROP TABLE IF EXISTS public.appointment_notification_jobs;

ALTER TABLE public.reminder_schedule_times
    DROP CONSTRAINT IF EXISTS reminder_schedule_times_schedule_fk;
DROP TABLE IF EXISTS public.reminder_schedule_times;

ALTER TABLE public.appointment_notification_rules
    DROP CONSTRAINT IF EXISTS appointment_notification_rules_config_fk;
DROP TABLE IF EXISTS public.appointment_notification_rules;

ALTER TABLE public.safety_configs
    DROP CONSTRAINT IF EXISTS safety_configs_user_id_fkey,
    DROP CONSTRAINT IF EXISTS safety_configs_updated_by_fkey;
DROP TABLE IF EXISTS public.safety_configs;

DROP TABLE IF EXISTS public.consultation_sessions;

-- The legacy checklist table goes too: R12 removed UserChecklistItem, its
-- repository, IUserChecklistItemService and the impl, so nothing maps it any more.
-- The gate below is what makes that safe — V20260806176000 already proved every
-- legacy row had a v2 counterpart, and this re-derives it rather than trusting it.
DO $$
DECLARE
    v_unmapped bigint;
BEGIN
    SELECT count(*) INTO v_unmapped
      FROM public.preparation_checklist_items legacy
     WHERE NOT EXISTS (SELECT 1 FROM public.checklist_task_instances task
                        WHERE task.checklist_task_instance_id = legacy.checklist_item_id);
    IF v_unmapped > 0 THEN
        RAISE EXCEPTION
            'CONTRACT_GATE_FAILED: % legacy checklist row(s) have no v2 task instance', v_unmapped;
    END IF;
END
$$;

DROP TABLE IF EXISTS public.preparation_checklist_items;

-- ---------------------------------------------------------------------------
-- 3. Negative-impact check (plan §4.14)
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    v_missing text;
BEGIN
    SELECT string_agg(expected, ', ' ORDER BY expected) INTO v_missing
    FROM unnest(ARRAY[
        'notification_jobs', 'reminder_schedules', 'appointment_notification_configs',
        'consultation_bookings', 'users', 'device_tokens',
        'reminder_occurrence_aliases', 'direct_conversation_read_cursors',
        'growth_measurements', 'safety_events', 'audit_events'
    ]) AS expected
    WHERE to_regclass('public.' || expected) IS NULL;

    IF v_missing IS NOT NULL THEN
        RAISE EXCEPTION 'CONSOLIDATION_REGRESSION: retained object(s) missing: %', v_missing;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'users' AND column_name = 'settings_jsonb') THEN
        RAISE EXCEPTION 'CONSOLIDATION_REGRESSION: users.settings_jsonb was removed';
    END IF;
END
$$;
