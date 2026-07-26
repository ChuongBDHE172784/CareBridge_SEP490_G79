ALTER TABLE public.professional_profiles
    ADD COLUMN IF NOT EXISTS consultation_fee_vnd BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_professional_profiles_consultation_fee_vnd_nonnegative'
          AND conrelid = 'public.professional_profiles'::regclass
    ) THEN
        ALTER TABLE public.professional_profiles
            ADD CONSTRAINT chk_professional_profiles_consultation_fee_vnd_nonnegative
            CHECK (consultation_fee_vnd IS NULL OR consultation_fee_vnd >= 0);
    END IF;
END
$$;
