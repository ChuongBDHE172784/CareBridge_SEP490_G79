-- CareBridge database consolidation — R11b observation (Step 4)
-- Spec: Database_Consolidation_Source_Code_Refactor_Plan.md §7.5 (job gates)
--
-- Read-only. Run repeatedly during the observation window; each run prints one
-- PASS/FAIL/INFO row per gate plus a verdict, the same shape as 04_readiness_check.
--
-- §7.5 requires, before R12 and the queue contract migration:
--   * at least two planner cycles
--   * at least one retry path exercised
--   * at least one stale-lock requeue
--   * no duplicate notification and no duplicate job identity
--   * source queues frozen — the new code must never write to them again
--
-- Timing note: the planners are cron'd at 02:15 (appointment) and 02:20 (reminder
-- schedule) in the server's local zone, so "two planner cycles" is two nights
-- unless APPOINTMENT_NOTIFICATION_PLANNER_CRON / REMINDER_SCHEDULE_PLANNER_CRON are
-- shortened for the window. The workers run every 15s regardless, but only claim
-- jobs whose due_at and next_attempt_at have passed.

DROP TABLE IF EXISTS pg_temp.r11b_observation;
CREATE TEMP TABLE r11b_observation (
    seq integer, gate text, expected text, actual text, result text
);

DO $outer$
DECLARE
    v_n bigint;
    v_txt text;
    v_baseline_schedule bigint := 221;   -- measured at cutover, 2026-08-07
    v_baseline_appointment bigint := 10;
BEGIN
    ------------------------------------------------------------------
    -- Source queues must be frozen. The new code reads and writes only
    -- notification_jobs; any movement here means something still targets the
    -- retired tables, which would split the queue in two.
    ------------------------------------------------------------------
    SELECT count(*) INTO v_n FROM public.reminder_schedule_jobs;
    INSERT INTO r11b_observation VALUES (1, 'reminder_schedule_jobs frozen at cutover count',
        v_baseline_schedule::text, v_n::text,
        CASE WHEN v_n = v_baseline_schedule THEN 'PASS' ELSE 'FAIL' END);

    SELECT count(*) INTO v_n FROM public.appointment_notification_jobs;
    INSERT INTO r11b_observation VALUES (2, 'appointment_notification_jobs frozen at cutover count',
        v_baseline_appointment::text, v_n::text,
        CASE WHEN v_n = v_baseline_appointment THEN 'PASS' ELSE 'FAIL' END);

    ------------------------------------------------------------------
    -- Duplicate identity would mean the same occurrence notified twice.
    ------------------------------------------------------------------
    SELECT count(*) INTO v_n FROM (
        SELECT schedule_id, schedule_revision, occurrence_date, local_time
          FROM public.notification_jobs WHERE job_type = 'REMINDER_SCHEDULE'
         GROUP BY 1,2,3,4 HAVING count(*) > 1) d;
    INSERT INTO r11b_observation VALUES (3, 'duplicate reminder-schedule identity',
        '0', v_n::text, CASE WHEN v_n = 0 THEN 'PASS' ELSE 'FAIL' END);

    SELECT count(*) INTO v_n FROM (
        SELECT reminder_id, occurrence_id, config_revision, offset_minutes
          FROM public.notification_jobs WHERE job_type = 'APPOINTMENT'
         GROUP BY 1,2,3,4 HAVING count(*) > 1) d;
    INSERT INTO r11b_observation VALUES (4, 'duplicate appointment identity',
        '0', v_n::text, CASE WHEN v_n = 0 THEN 'PASS' ELSE 'FAIL' END);

    -- One notification record must not be claimed by two jobs.
    SELECT count(*) INTO v_n FROM (
        SELECT notification_record_id FROM public.notification_jobs
         WHERE notification_record_id IS NOT NULL
         GROUP BY 1 HAVING count(*) > 1) d;
    INSERT INTO r11b_observation VALUES (5, 'duplicate notification record across jobs',
        '0', v_n::text, CASE WHEN v_n = 0 THEN 'PASS' ELSE 'FAIL' END);

    ------------------------------------------------------------------
    -- Evidence that a planner has run since the cutover: jobs created after the
    -- last migration. Until this is > 0, no planner cycle has been observed.
    ------------------------------------------------------------------
    SELECT count(*) INTO v_n
      FROM public.notification_jobs
     WHERE created_at > (SELECT max(installed_on) FROM public.flyway_schema_history);
    INSERT INTO r11b_observation VALUES (6, 'jobs planned since cutover',
        '> 0', v_n::text,
        CASE WHEN v_n > 0 THEN 'PASS' ELSE 'INFO' END);

    -- §7.5 asks for two planner CYCLES, not two days; an earlier draft of this
    -- script demanded two distinct days, which is stricter than the spec.
    -- A cycle materialises its jobs in one burst, so distinct creation minutes is
    -- the closest proxy the database holds.
    --
    -- Caveat worth knowing before waiting on this: the planner is idempotent. If
    -- the 35-day horizon is already fully materialised, a perfectly healthy cycle
    -- inserts nothing and is invisible here. Evidence then has to come from the
    -- application log, or from a schedule being created/updated during the window.
    SELECT count(DISTINCT date_trunc('minute', created_at)) INTO v_n
      FROM public.notification_jobs
     WHERE created_at > (SELECT max(installed_on) FROM public.flyway_schema_history);
    INSERT INTO r11b_observation VALUES (7, 'distinct planner cycles since cutover',
        '>= 2', v_n::text, CASE WHEN v_n >= 2 THEN 'PASS' ELSE 'INFO' END);

    ------------------------------------------------------------------
    -- Retry and stale-lock paths.
    ------------------------------------------------------------------
    SELECT count(*) INTO v_n
      FROM public.notification_jobs
     WHERE attempt_count > 1
       AND updated_at > (SELECT max(installed_on) FROM public.flyway_schema_history);
    INSERT INTO r11b_observation VALUES (8, 'retry path exercised since cutover',
        '>= 1', v_n::text, CASE WHEN v_n >= 1 THEN 'PASS' ELSE 'INFO' END);

    -- A requeued stale lock lands back in PENDING with the lock cleared while the
    -- attempt count already advanced.
    SELECT count(*) INTO v_n
      FROM public.notification_jobs
     WHERE status = 'PENDING' AND attempt_count > 0 AND locked_by IS NULL
       AND updated_at > (SELECT max(installed_on) FROM public.flyway_schema_history);
    INSERT INTO r11b_observation VALUES (9, 'stale-lock requeue observed since cutover',
        '>= 1', v_n::text, CASE WHEN v_n >= 1 THEN 'PASS' ELSE 'INFO' END);

    ------------------------------------------------------------------
    -- Lock hygiene: a PROCESSING row must always carry its owner and time.
    ------------------------------------------------------------------
    SELECT count(*) INTO v_n FROM public.notification_jobs
     WHERE status = 'PROCESSING' AND (locked_by IS NULL OR locked_at IS NULL);
    INSERT INTO r11b_observation VALUES (10, 'PROCESSING rows with a broken lock',
        '0', v_n::text, CASE WHEN v_n = 0 THEN 'PASS' ELSE 'FAIL' END);

    SELECT count(*) INTO v_n FROM public.notification_jobs
     WHERE status = 'PROCESSING' AND locked_at < now() - interval '30 minutes';
    INSERT INTO r11b_observation VALUES (11, 'locks held longer than 30 minutes',
        '0', v_n::text, CASE WHEN v_n = 0 THEN 'PASS' ELSE 'FAIL' END);

    ------------------------------------------------------------------
    -- Current shape, for the log.
    ------------------------------------------------------------------
    SELECT string_agg(job_type || '/' || status || '=' || n, ', ' ORDER BY job_type, status)
      INTO v_txt
      FROM (SELECT job_type, status, count(*) AS n
              FROM public.notification_jobs GROUP BY 1,2) s;
    INSERT INTO r11b_observation VALUES (12, 'queue shape', 'informational', v_txt, 'INFO');

    SELECT to_char(min(due_at) AT TIME ZONE 'UTC', 'YYYY-MM-DD HH24:MI') INTO v_txt
      FROM public.notification_jobs WHERE status = 'PENDING';
    INSERT INTO r11b_observation VALUES (13, 'earliest pending due_at (UTC)',
        'informational', coalesce(v_txt, 'none pending'), 'INFO');
END
$outer$;

SELECT seq, gate, expected, actual, result FROM r11b_observation ORDER BY seq;

SELECT CASE
         WHEN count(*) FILTER (WHERE result = 'FAIL') > 0
              THEN 'ROLL BACK — ' || count(*) FILTER (WHERE result = 'FAIL')::text
                   || ' gate(s) failing; §7.5 treats these as unconditional rollback triggers'
         WHEN count(*) FILTER (WHERE result = 'INFO' AND seq IN (6,7,8,9)) > 0
              THEN 'OBSERVING — no failures, but the planner/retry/stale-lock evidence is not complete yet'
         ELSE 'OBSERVATION COMPLETE — R12 and the queue contract migration may proceed'
       END AS verdict
  FROM r11b_observation;
