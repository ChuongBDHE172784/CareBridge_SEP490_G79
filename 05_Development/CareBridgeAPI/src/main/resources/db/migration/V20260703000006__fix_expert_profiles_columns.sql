-- =============================================================================
-- V20260703000006__fix_expert_profiles_columns.sql
-- Purpose: Reconcile expert_profiles columns after remote migration
--          20260703170640 renamed expert_profile_id and dropped additional
--          columns. Earlier V20260703000005 fixes the PK rename; this
--          migration adds ALL missing columns in one pass.
-- =============================================================================

DO $$
DECLARE
    col_count integer;
BEGIN
    -- All columns expected by ExpertProfile.java entity.
    -- Each block is independently idempotent (IF NOT EXISTS guard).

    SELECT COUNT(*) INTO col_count FROM information_schema.columns
     WHERE table_schema = 'public' AND table_name = 'expert_profiles'
       AND column_name = 'specialty';
    IF col_count = 0 THEN
        ALTER TABLE public.expert_profiles ADD COLUMN specialty varchar(100);
        RAISE NOTICE 'Added specialty to expert_profiles';
    END IF;

    SELECT COUNT(*) INTO col_count FROM information_schema.columns
     WHERE table_schema = 'public' AND table_name = 'expert_profiles'
       AND column_name = 'professional_title';
    IF col_count = 0 THEN
        ALTER TABLE public.expert_profiles ADD COLUMN professional_title varchar(150);
        RAISE NOTICE 'Added professional_title to expert_profiles';
    END IF;

    SELECT COUNT(*) INTO col_count FROM information_schema.columns
     WHERE table_schema = 'public' AND table_name = 'expert_profiles'
       AND column_name = 'experience_years';
    IF col_count = 0 THEN
        ALTER TABLE public.expert_profiles ADD COLUMN experience_years smallint;
        RAISE NOTICE 'Added experience_years to expert_profiles';
    END IF;

    SELECT COUNT(*) INTO col_count FROM information_schema.columns
     WHERE table_schema = 'public' AND table_name = 'expert_profiles'
       AND column_name = 'workplace';
    IF col_count = 0 THEN
        ALTER TABLE public.expert_profiles ADD COLUMN workplace varchar(200);
        RAISE NOTICE 'Added workplace to expert_profiles';
    END IF;

    SELECT COUNT(*) INTO col_count FROM information_schema.columns
     WHERE table_schema = 'public' AND table_name = 'expert_profiles'
       AND column_name = 'consultation_scope';
    IF col_count = 0 THEN
        ALTER TABLE public.expert_profiles ADD COLUMN consultation_scope text;
        RAISE NOTICE 'Added consultation_scope to expert_profiles';
    END IF;

    SELECT COUNT(*) INTO col_count FROM information_schema.columns
     WHERE table_schema = 'public' AND table_name = 'expert_profiles'
       AND column_name = 'verified_at';
    IF col_count = 0 THEN
        ALTER TABLE public.expert_profiles ADD COLUMN verified_at timestamptz;
        RAISE NOTICE 'Added verified_at to expert_profiles';
    END IF;

    SELECT COUNT(*) INTO col_count FROM information_schema.columns
     WHERE table_schema = 'public' AND table_name = 'expert_profiles'
       AND column_name = 'verified_by';
    IF col_count = 0 THEN
        ALTER TABLE public.expert_profiles ADD COLUMN verified_by uuid;
        RAISE NOTICE 'Added verified_by to expert_profiles';
    END IF;

    SELECT COUNT(*) INTO col_count FROM information_schema.columns
     WHERE table_schema = 'public' AND table_name = 'expert_profiles'
       AND column_name = 'rating_avg';
    IF col_count = 0 THEN
        ALTER TABLE public.expert_profiles ADD COLUMN rating_avg numeric;
        RAISE NOTICE 'Added rating_avg to expert_profiles';
    END IF;
END $$;
