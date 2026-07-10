ALTER TABLE public.growth_measurements
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ NULL;
