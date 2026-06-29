-- UC-45: CreateAppointmentReminder — recurrence type + FCM job tracking
ALTER TABLE reminders
    ADD COLUMN IF NOT EXISTS recurrence_type    VARCHAR(30),
    ADD COLUMN IF NOT EXISTS recurrence_end_date TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS fcm_job_id          VARCHAR(255);
