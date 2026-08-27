-- CareBridge database consolidation — persistence contract (queue + expand sources)
-- Spec: 08_References/Database_Table_Audit_And_Consolidation V3.md §3.6-§3.11, §5 waves 5/7
-- Code: Database_Consolidation_Source_Code_Refactor_Plan.md §6 (R13), §7.5
--
-- ============================================================================
-- THIS IS DELIBERATELY *NOT* IN src/main/resources/db/migration.
--
-- Flyway has no partial mode: the moment this file becomes a versioned migration,
-- the next service start drops these tables. They must not be dropped until
-- 05_r11b_observation.sql reports OBSERVATION COMPLETE. Promote this file to
-- V2026…__contract_drop_consolidated_source_tables.sql only then.
-- ============================================================================
--
-- Preconditions, all of which the gates below re-check rather than trust:
--   * R12 shipped — no application code maps or queries these tables. Verified by
--     the static scan: 11/11 retired symbols at zero references.
--   * 05_r11b_observation.sql reports OBSERVATION COMPLETE.
--   * The deployed build is the post-R12 build, not an older one.
--
-- After this commits, rollback is forward-fix or restore only. On this database
-- the Release Owner has waived PITR/backups, so there is no restore — the data in
-- these tables is gone for good.

-- ---------------------------------------------------------------------------
-- 1. Gate — the application must be off these tables already
-- ---------------------------------------------------------------------------
-- A row that appeared after the cutover means something still writes here, which
-- would mean dropping the table loses live data.
DO $$
DECLARE
    v_schedule_jobs bigint;
    v_appointment_jobs bigint;
    v_cutover timestamptz;
BEGIN
    SELECT max(installed_on) INTO v_cutover
      FROM public.flyway_schema_history
     WHERE version = '20260806160000';

    IF v_cutover IS NULL THEN
        RAISE EXCEPTION 'CONTRACT_GATE_FAILED: the notification_jobs expand migration has not run here';
    END IF;

    SELECT count(*) INTO v_schedule_jobs
      FROM public.reminder_schedule_jobs WHERE created_at > v_cutover OR updated_at > v_cutover;
    SELECT count(*) INTO v_appointment_jobs
      FROM public.appointment_notification_jobs WHERE created_at > v_cutover OR updated_at > v_cutover;

    IF v_schedule_jobs > 0 OR v_appointment_jobs > 0 THEN
        RAISE EXCEPTION
            'CONTRACT_GATE_FAILED: source queues changed after cutover (% schedule, % appointment); something still writes to them',
            v_schedule_jobs, v_appointment_jobs;
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

-- preparation_checklist_items is deliberately NOT dropped here. Its entity,
-- repository and service still exist in the codebase (R12 stopped short of them),
-- and Hibernate would fail schema validation at startup against a missing table.
-- Drop it in the release that removes those classes.

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
