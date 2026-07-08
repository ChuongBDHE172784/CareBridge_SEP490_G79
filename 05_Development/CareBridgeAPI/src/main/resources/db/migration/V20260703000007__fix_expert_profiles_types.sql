-- =============================================================================
-- V20260703000007__fix_expert_profiles_types.sql
-- Purpose: Fix column type mismatch — experience_years is SMALLINT in the
--          DB (from V06 or remote migration) but ExpertProfile.java maps it
--          as Integer (INTEGER). Cast to INTEGER to match the entity.
-- =============================================================================

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name  = 'expert_profiles'
          AND column_name = 'experience_years'
          AND udt_name = 'int2'
    ) THEN
        ALTER TABLE public.expert_profiles
            ALTER COLUMN experience_years TYPE INTEGER
            USING experience_years::INTEGER;
        RAISE NOTICE 'Cast experience_years from SMALLINT to INTEGER in expert_profiles';
    END IF;
END $$;
