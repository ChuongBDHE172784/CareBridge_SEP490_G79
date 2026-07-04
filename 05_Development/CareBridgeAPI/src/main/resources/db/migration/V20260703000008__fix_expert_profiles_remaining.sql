-- =============================================================================
-- V20260703000008__fix_expert_profiles_remaining.sql
-- Purpose: Add verification_status and updated_at columns that were missing
--          from all earlier migrations.
-- =============================================================================

DO $$
DECLARE
    col_count integer;
BEGIN
    SELECT COUNT(*) INTO col_count FROM information_schema.columns
     WHERE table_schema = 'public' AND table_name = 'expert_profiles'
       AND column_name = 'verification_status';
    IF col_count = 0 THEN
        ALTER TABLE public.expert_profiles
            ADD COLUMN verification_status varchar(30) NOT NULL DEFAULT 'PENDING';
        RAISE NOTICE 'Added verification_status to expert_profiles';
    END IF;

    SELECT COUNT(*) INTO col_count FROM information_schema.columns
     WHERE table_schema = 'public' AND table_name = 'expert_profiles'
       AND column_name = 'created_at';
    IF col_count = 0 THEN
        ALTER TABLE public.expert_profiles
            ADD COLUMN created_at timestamptz NOT NULL DEFAULT now();
        RAISE NOTICE 'Added created_at to expert_profiles';
    END IF;

    SELECT COUNT(*) INTO col_count FROM information_schema.columns
     WHERE table_schema = 'public' AND table_name = 'expert_profiles'
       AND column_name = 'updated_at';
    IF col_count = 0 THEN
        ALTER TABLE public.expert_profiles
            ADD COLUMN updated_at timestamptz NOT NULL DEFAULT now();
        RAISE NOTICE 'Added updated_at to expert_profiles';
    END IF;
END $$;
