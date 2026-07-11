ALTER TABLE public.vaccination_records
    ADD COLUMN IF NOT EXISTS postpone_reason TEXT;
