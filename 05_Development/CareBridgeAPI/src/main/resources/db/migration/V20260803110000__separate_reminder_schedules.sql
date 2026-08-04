-- Separate alarm-like reminder schedules from completable care tasks.
-- This migration is additive and intentionally leaves legacy care_tasks readable.
CREATE TABLE IF NOT EXISTS public.reminder_schedules (
    schedule_id uuid NOT NULL DEFAULT gen_random_uuid(),
    owner_user_id uuid NOT NULL,
    title varchar(255) NOT NULL,
    time_zone varchar(80) NOT NULL,
    recurrence varchar(20) NOT NULL DEFAULT 'NONE',
    start_date date NOT NULL,
    end_date date,
    active boolean NOT NULL DEFAULT true,
    revision bigint NOT NULL DEFAULT 1,
    lock_version bigint NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
    updated_at timestamptz NOT NULL DEFAULT clock_timestamp(),
    CONSTRAINT reminder_schedules_pkey PRIMARY KEY (schedule_id),
    CONSTRAINT reminder_schedules_owner_fk FOREIGN KEY (owner_user_id)
        REFERENCES public.users(user_id) ON DELETE CASCADE,
    CONSTRAINT reminder_schedules_title_ck CHECK (length(trim(title)) > 0),
    CONSTRAINT reminder_schedules_timezone_ck CHECK (length(trim(time_zone)) > 0),
    CONSTRAINT reminder_schedules_recurrence_ck CHECK (recurrence IN ('NONE', 'DAILY')),
    CONSTRAINT reminder_schedules_revision_ck CHECK (revision > 0),
    CONSTRAINT reminder_schedules_lock_version_ck CHECK (lock_version >= 0),
    CONSTRAINT reminder_schedules_dates_ck CHECK (end_date IS NULL OR end_date >= start_date)
);

CREATE TABLE IF NOT EXISTS public.reminder_schedule_times (
    time_id uuid NOT NULL DEFAULT gen_random_uuid(),
    schedule_id uuid NOT NULL,
    local_time time NOT NULL,
    sort_order integer NOT NULL,
    CONSTRAINT reminder_schedule_times_pkey PRIMARY KEY (time_id),
    CONSTRAINT reminder_schedule_times_schedule_fk FOREIGN KEY (schedule_id)
        REFERENCES public.reminder_schedules(schedule_id) ON DELETE CASCADE,
    CONSTRAINT reminder_schedule_times_unique UNIQUE (schedule_id, local_time),
    CONSTRAINT reminder_schedule_times_order_unique UNIQUE (schedule_id, sort_order),
    CONSTRAINT reminder_schedule_times_sort_ck CHECK (sort_order >= 0)
);

CREATE TABLE IF NOT EXISTS public.reminder_schedule_jobs (
    job_id uuid NOT NULL DEFAULT gen_random_uuid(),
    schedule_id uuid NOT NULL,
    schedule_revision bigint NOT NULL,
    occurrence_date date NOT NULL,
    local_time time NOT NULL,
    time_zone varchar(80) NOT NULL,
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
    CONSTRAINT reminder_schedule_jobs_pkey PRIMARY KEY (job_id),
    CONSTRAINT reminder_schedule_jobs_schedule_fk FOREIGN KEY (schedule_id)
        REFERENCES public.reminder_schedules(schedule_id) ON DELETE CASCADE,
    CONSTRAINT reminder_schedule_jobs_record_fk FOREIGN KEY (notification_record_id)
        REFERENCES public.notification_records(id) ON DELETE SET NULL,
    CONSTRAINT reminder_schedule_jobs_identity_uk UNIQUE
        (schedule_id, schedule_revision, occurrence_date, local_time),
    CONSTRAINT reminder_schedule_jobs_revision_ck CHECK (schedule_revision > 0),
    CONSTRAINT reminder_schedule_jobs_attempt_ck CHECK (attempt_count >= 0),
    CONSTRAINT reminder_schedule_jobs_status_ck CHECK (status IN
        ('PENDING', 'PROCESSING', 'SENT', 'FAILED', 'SUPPRESSED', 'CANCELLED')),
    CONSTRAINT reminder_schedule_jobs_lock_ck CHECK (
        (status = 'PROCESSING' AND locked_by IS NOT NULL AND locked_at IS NOT NULL)
        OR status <> 'PROCESSING'
    )
);

CREATE INDEX IF NOT EXISTS reminder_schedules_owner_active_ix
    ON public.reminder_schedules(owner_user_id, active, start_date);
CREATE INDEX IF NOT EXISTS reminder_schedule_jobs_due_ix
    ON public.reminder_schedule_jobs(status, next_attempt_at, due_at);
CREATE INDEX IF NOT EXISTS reminder_schedule_jobs_schedule_revision_ix
    ON public.reminder_schedule_jobs(schedule_id, schedule_revision, status);

CREATE UNIQUE INDEX IF NOT EXISTS uq_notification_records_reminder_schedule_job
    ON public.notification_records(user_id, reference_id, ((metadata ->> 'scheduleJobId')))
    WHERE type = 'REMINDER'
      AND reference_type = 'REMINDER_SCHEDULE'
      AND metadata ? 'scheduleJobId';
