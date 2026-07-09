ALTER TABLE public.notification_records
    ADD COLUMN IF NOT EXISTS metadata jsonb;

