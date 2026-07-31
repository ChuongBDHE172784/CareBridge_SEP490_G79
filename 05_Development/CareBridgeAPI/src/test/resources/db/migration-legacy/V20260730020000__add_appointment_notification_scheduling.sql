CREATE TABLE public.appointment_notification_configs (
    reminder_id uuid NOT NULL,
    time_zone varchar(80) NOT NULL DEFAULT 'Asia/Ho_Chi_Minh',
    config_revision bigint NOT NULL DEFAULT 1,
    created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
    updated_at timestamptz NOT NULL DEFAULT clock_timestamp(),
    CONSTRAINT appointment_notification_configs_pkey PRIMARY KEY (reminder_id),
    CONSTRAINT appointment_notification_configs_reminder_fk
        FOREIGN KEY (reminder_id) REFERENCES public.care_tasks(task_id) ON DELETE CASCADE,
    CONSTRAINT appointment_notification_configs_revision_ck CHECK (config_revision > 0),
    CONSTRAINT appointment_notification_configs_timezone_ck CHECK (length(trim(time_zone)) > 0)
);

CREATE TABLE public.appointment_notification_rules (
    rule_id uuid NOT NULL,
    reminder_id uuid NOT NULL,
    offset_minutes integer NOT NULL,
    sort_order integer NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
    updated_at timestamptz NOT NULL DEFAULT clock_timestamp(),
    CONSTRAINT appointment_notification_rules_pkey PRIMARY KEY (rule_id),
    CONSTRAINT appointment_notification_rules_config_fk
        FOREIGN KEY (reminder_id) REFERENCES public.appointment_notification_configs(reminder_id) ON DELETE CASCADE,
    CONSTRAINT appointment_notification_rules_reminder_offset_uk UNIQUE (reminder_id, offset_minutes),
    CONSTRAINT appointment_notification_rules_offset_ck
        CHECK (offset_minutes BETWEEN -43200 AND 10080),
    CONSTRAINT appointment_notification_rules_sort_ck CHECK (sort_order >= 0)
);

CREATE TABLE public.appointment_notification_jobs (
    job_id uuid NOT NULL,
    reminder_id uuid NOT NULL,
    occurrence_id uuid NOT NULL,
    occurrence_generation bigint NOT NULL DEFAULT 0,
    occurrence_scheduled_at timestamptz NOT NULL,
    config_revision bigint NOT NULL,
    offset_minutes integer NOT NULL,
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
    CONSTRAINT appointment_notification_jobs_pkey PRIMARY KEY (job_id),
    CONSTRAINT appointment_notification_jobs_reminder_fk
        FOREIGN KEY (reminder_id) REFERENCES public.care_tasks(task_id) ON DELETE CASCADE,
    CONSTRAINT appointment_notification_jobs_record_fk
        FOREIGN KEY (notification_record_id) REFERENCES public.notification_records(id) ON DELETE SET NULL,
    CONSTRAINT appointment_notification_jobs_identity_uk
        UNIQUE (reminder_id, occurrence_id, config_revision, offset_minutes),
    CONSTRAINT appointment_notification_jobs_generation_ck CHECK (occurrence_generation >= 0),
    CONSTRAINT appointment_notification_jobs_revision_ck CHECK (config_revision > 0),
    CONSTRAINT appointment_notification_jobs_offset_ck
        CHECK (offset_minutes BETWEEN -43200 AND 10080),
    CONSTRAINT appointment_notification_jobs_attempt_ck CHECK (attempt_count >= 0),
    CONSTRAINT appointment_notification_jobs_status_ck CHECK (status IN (
        'PENDING', 'PROCESSING', 'SENT', 'FAILED', 'SUPPRESSED', 'CANCELLED'
    )),
    CONSTRAINT appointment_notification_jobs_lock_ck CHECK (
        (status = 'PROCESSING' AND locked_by IS NOT NULL AND locked_at IS NOT NULL)
        OR status <> 'PROCESSING'
    )
);

CREATE INDEX appointment_notification_jobs_due_ix
    ON public.appointment_notification_jobs(status, next_attempt_at, due_at);
CREATE INDEX appointment_notification_jobs_reminder_revision_ix
    ON public.appointment_notification_jobs(reminder_id, config_revision, status);
CREATE INDEX appointment_notification_jobs_occurrence_ix
    ON public.appointment_notification_jobs(occurrence_id, status);

CREATE UNIQUE INDEX uq_notification_records_appointment_milestone
    ON public.notification_records(user_id, reference_id, ((metadata ->> 'milestoneJobId')))
    WHERE type = 'REMINDER'
      AND reference_type = 'APPOINTMENT'
      AND metadata ? 'milestoneJobId';
