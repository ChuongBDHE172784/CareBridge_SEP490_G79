-- CareBridge database consolidation — R11 expand + backfill
-- Spec: 08_References/Database_Table_Audit_And_Consolidation V3.md §3.8
-- Code: Database_Consolidation_Source_Code_Refactor_Plan.md §4.9
--
-- Typed-polymorphic, NOT generic source_type/source_id: PostgreSQL cannot build a
-- foreign key that points at different tables per row, so a generic discriminator
-- would trade a real referential guarantee for a string. Each branch keeps its own
-- typed FK and its own identity; a CHECK on the discriminator makes the two branch
-- column sets mutually exclusive.
--
-- Identity design rests on ReminderOccurrenceIdGenerationContractTest: occurrence-ID
-- v2 folds occurrence_generation into occurrence_id, so the generation is kept as a
-- snapshot but stays out of the unique identity (V3 §3.8). If that test ever fails,
-- this index is wrong and must gain occurrence_generation.
--
-- Both source tables stay and keep receiving writes through the observation window.

-- ---------------------------------------------------------------------------
-- 1. Gate — job_id collision between the two sources
-- ---------------------------------------------------------------------------
-- V3 §3.8 forbids ON CONFLICT DO NOTHING here: it would silently drop a job.
-- A collision instead demands an explicit ID remapping, so fail loudly.
DO $$
DECLARE
    v_collisions bigint;
BEGIN
    SELECT count(*) INTO v_collisions
    FROM (
        SELECT job_id FROM public.reminder_schedule_jobs
        INTERSECT
        SELECT job_id FROM public.appointment_notification_jobs
    ) collided;

    IF v_collisions > 0 THEN
        RAISE EXCEPTION
            'R11_JOB_ID_COLLISION: % job_id(s) exist in both source queues; an explicit id remapping is required',
            v_collisions;
    END IF;
END
$$;

-- ---------------------------------------------------------------------------
-- 2. Target table
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.notification_jobs (
    -- Common columns: the state machine is identical for both job types.
    job_id uuid NOT NULL DEFAULT gen_random_uuid(),
    job_type varchar(20) NOT NULL,
    due_at timestamptz NOT NULL,
    status varchar(20) NOT NULL DEFAULT 'PENDING',
    attempt_count integer NOT NULL DEFAULT 0,
    next_attempt_at timestamptz NOT NULL,
    locked_by varchar(120),
    locked_at timestamptz,
    notification_record_id uuid,
    last_error_code varchar(80),
    created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
    updated_at timestamptz NOT NULL DEFAULT clock_timestamp(),

    -- REMINDER_SCHEDULE branch.
    schedule_id uuid,
    schedule_revision bigint,
    occurrence_date date,
    local_time time,
    time_zone varchar(80),

    -- APPOINTMENT branch.
    reminder_id uuid,
    occurrence_id uuid,
    occurrence_generation bigint,
    occurrence_scheduled_at timestamptz,
    config_revision bigint,
    offset_minutes integer,

    CONSTRAINT notification_jobs_pkey PRIMARY KEY (job_id),

    -- Typed foreign keys survive the merge, one per branch.
    CONSTRAINT notification_jobs_schedule_fk FOREIGN KEY (schedule_id)
        REFERENCES public.reminder_schedules(schedule_id) ON DELETE CASCADE,
    CONSTRAINT notification_jobs_reminder_fk FOREIGN KEY (reminder_id)
        REFERENCES public.care_tasks(task_id) ON DELETE CASCADE,
    -- SET NULL, not CASCADE: losing a notification record must not delete the job
    -- that records the attempt.
    CONSTRAINT notification_jobs_record_fk FOREIGN KEY (notification_record_id)
        REFERENCES public.notification_records(id) ON DELETE SET NULL,

    CONSTRAINT notification_jobs_type_ck
        CHECK (job_type IN ('REMINDER_SCHEDULE', 'APPOINTMENT')),

    -- The discriminator decides which column set is populated. Without this a row
    -- could carry both branches at once and no worker could tell what it is.
    CONSTRAINT notification_jobs_branch_ck CHECK (
        (job_type = 'REMINDER_SCHEDULE'
            AND schedule_id IS NOT NULL
            AND schedule_revision IS NOT NULL
            AND occurrence_date IS NOT NULL
            AND local_time IS NOT NULL
            AND time_zone IS NOT NULL
            AND reminder_id IS NULL
            AND occurrence_id IS NULL
            AND occurrence_generation IS NULL
            AND occurrence_scheduled_at IS NULL
            AND config_revision IS NULL
            AND offset_minutes IS NULL)
        OR
        (job_type = 'APPOINTMENT'
            AND reminder_id IS NOT NULL
            AND occurrence_id IS NOT NULL
            AND occurrence_generation IS NOT NULL
            AND occurrence_scheduled_at IS NOT NULL
            AND config_revision IS NOT NULL
            AND offset_minutes IS NOT NULL
            AND schedule_id IS NULL
            AND schedule_revision IS NULL
            AND occurrence_date IS NULL
            AND local_time IS NULL
            AND time_zone IS NULL)
    ),

    -- Every constraint the two source tables carried, preserved verbatim.
    CONSTRAINT notification_jobs_status_ck CHECK (status IN
        ('PENDING', 'PROCESSING', 'SENT', 'FAILED', 'SUPPRESSED', 'CANCELLED')),
    CONSTRAINT notification_jobs_attempt_ck CHECK (attempt_count >= 0),
    CONSTRAINT notification_jobs_lock_ck CHECK (
        (status = 'PROCESSING' AND locked_by IS NOT NULL AND locked_at IS NOT NULL)
        OR status <> 'PROCESSING'
    ),
    CONSTRAINT notification_jobs_schedule_revision_ck
        CHECK (schedule_revision IS NULL OR schedule_revision > 0),
    CONSTRAINT notification_jobs_config_revision_ck
        CHECK (config_revision IS NULL OR config_revision > 0),
    CONSTRAINT notification_jobs_generation_ck
        CHECK (occurrence_generation IS NULL OR occurrence_generation >= 0),
    CONSTRAINT notification_jobs_offset_ck
        CHECK (offset_minutes IS NULL OR offset_minutes BETWEEN -43200 AND 10080)
);

-- Two identities, one per branch — partial so neither constrains the other.
CREATE UNIQUE INDEX IF NOT EXISTS notification_jobs_schedule_identity_uk
    ON public.notification_jobs (schedule_id, schedule_revision, occurrence_date, local_time)
    WHERE job_type = 'REMINDER_SCHEDULE';

CREATE UNIQUE INDEX IF NOT EXISTS notification_jobs_appointment_identity_uk
    ON public.notification_jobs (reminder_id, occurrence_id, config_revision, offset_minutes)
    WHERE job_type = 'APPOINTMENT';

-- Claim query support: workers poll by type, status and due time.
CREATE INDEX IF NOT EXISTS notification_jobs_claim_ix
    ON public.notification_jobs (job_type, status, next_attempt_at);

-- ---------------------------------------------------------------------------
-- 3. Backfill — job_id and status preserved exactly
-- ---------------------------------------------------------------------------
INSERT INTO public.notification_jobs (
    job_id, job_type, due_at, status, attempt_count, next_attempt_at,
    locked_by, locked_at, notification_record_id, last_error_code,
    created_at, updated_at,
    schedule_id, schedule_revision, occurrence_date, local_time, time_zone)
SELECT j.job_id, 'REMINDER_SCHEDULE', j.due_at, j.status, j.attempt_count, j.next_attempt_at,
       j.locked_by, j.locked_at, j.notification_record_id, j.last_error_code,
       j.created_at, j.updated_at,
       j.schedule_id, j.schedule_revision, j.occurrence_date, j.local_time, j.time_zone
FROM public.reminder_schedule_jobs j
WHERE NOT EXISTS (
    SELECT 1 FROM public.notification_jobs t WHERE t.job_id = j.job_id);

INSERT INTO public.notification_jobs (
    job_id, job_type, due_at, status, attempt_count, next_attempt_at,
    locked_by, locked_at, notification_record_id, last_error_code,
    created_at, updated_at,
    reminder_id, occurrence_id, occurrence_generation, occurrence_scheduled_at,
    config_revision, offset_minutes)
SELECT j.job_id, 'APPOINTMENT', j.due_at, j.status, j.attempt_count, j.next_attempt_at,
       j.locked_by, j.locked_at, j.notification_record_id, j.last_error_code,
       j.created_at, j.updated_at,
       j.reminder_id, j.occurrence_id, j.occurrence_generation, j.occurrence_scheduled_at,
       j.config_revision, j.offset_minutes
FROM public.appointment_notification_jobs j
WHERE NOT EXISTS (
    SELECT 1 FROM public.notification_jobs t WHERE t.job_id = j.job_id);

-- ---------------------------------------------------------------------------
-- 4. Reconciliation gate — count by type and by status
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    v_source_schedule bigint;
    v_source_appointment bigint;
    v_target_schedule bigint;
    v_target_appointment bigint;
    v_status_drift bigint;
BEGIN
    SELECT count(*) INTO v_source_schedule FROM public.reminder_schedule_jobs;
    SELECT count(*) INTO v_source_appointment FROM public.appointment_notification_jobs;
    SELECT count(*) INTO v_target_schedule
      FROM public.notification_jobs WHERE job_type = 'REMINDER_SCHEDULE';
    SELECT count(*) INTO v_target_appointment
      FROM public.notification_jobs WHERE job_type = 'APPOINTMENT';

    IF v_source_schedule <> v_target_schedule
       OR v_source_appointment <> v_target_appointment THEN
        RAISE EXCEPTION
            'R11_BACKFILL_COUNT_MISMATCH: schedule %/%s, appointment %/%s (source/target)',
            v_source_schedule, v_target_schedule, v_source_appointment, v_target_appointment;
    END IF;

    -- A job that changed status in transit would mean a lost or replayed send.
    SELECT count(*) INTO v_status_drift
    FROM public.notification_jobs t
    JOIN public.reminder_schedule_jobs s ON s.job_id = t.job_id
    WHERE t.job_type = 'REMINDER_SCHEDULE'
      AND (t.status, t.attempt_count, t.due_at, t.notification_record_id)
       IS DISTINCT FROM (s.status, s.attempt_count, s.due_at, s.notification_record_id);

    IF v_status_drift > 0 THEN
        RAISE EXCEPTION 'R11_STATUS_DRIFT: % reminder-schedule job(s) changed state during backfill',
            v_status_drift;
    END IF;

    SELECT count(*) INTO v_status_drift
    FROM public.notification_jobs t
    JOIN public.appointment_notification_jobs a ON a.job_id = t.job_id
    WHERE t.job_type = 'APPOINTMENT'
      AND (t.status, t.attempt_count, t.due_at, t.notification_record_id)
       IS DISTINCT FROM (a.status, a.attempt_count, a.due_at, a.notification_record_id);

    IF v_status_drift > 0 THEN
        RAISE EXCEPTION 'R11_STATUS_DRIFT: % appointment job(s) changed state during backfill',
            v_status_drift;
    END IF;
END
$$;

COMMENT ON TABLE public.notification_jobs IS
    'R11: typed-polymorphic notification queue. job_type discriminates; each branch keeps its own typed FK and its own partial unique identity (V3 §3.8).';
