-- =============================================================================
-- V20260721000003__fix_expert_profiles_experience_column.sql
-- Purpose: Fix experience column name mismatch between entity (experience_years)
--          and potential legacy column (years_of_experience) in remote DB.
--          The entity maps to "experience_years" but Supabase may have
--          "years_of_experience" with NOT NULL constraint from legacy migration.
-- =============================================================================

DO $$
BEGIN
    -- 1. If legacy column "years_of_experience" exists, copy data and drop it
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'expert_profiles'
          AND column_name = 'years_of_experience'
    ) THEN
        -- Copy data to correct column if experience_years is empty
        UPDATE public.expert_profiles
        SET experience_years = years_of_experience
        WHERE experience_years IS NULL AND years_of_experience IS NOT NULL;

        -- Drop NOT NULL constraint on legacy column if exists
        ALTER TABLE public.expert_profiles
        ALTER COLUMN years_of_experience DROP NOT NULL;

        -- Drop any check constraint on legacy column
        IF EXISTS (
            SELECT 1 FROM information_schema.check_constraints
            WHERE constraint_name = 'expert_profiles_years_of_experience_check'
        ) THEN
            ALTER TABLE public.expert_profiles
            DROP CONSTRAINT expert_profiles_years_of_experience_check;
        END IF;

        -- Drop the legacy column
        ALTER TABLE public.expert_profiles
        DROP COLUMN years_of_experience;

        RAISE NOTICE 'Migrated and dropped legacy years_of_experience column';
    END IF;

    -- 2. Ensure experience_years is nullable (entity allows null)
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'expert_profiles'
          AND column_name = 'experience_years'
          AND is_nullable = 'NO'
    ) THEN
        ALTER TABLE public.expert_profiles
        ALTER COLUMN experience_years DROP NOT NULL;
        RAISE NOTICE 'Made experience_years nullable';
    END IF;

    -- 3. Drop any check constraint on experience_years that may violate nullable
    IF EXISTS (
        SELECT 1 FROM information_schema.check_constraints
        WHERE constraint_name = 'expert_profiles_experience_years_check'
           OR constraint_name = 'expert_profiles_years_of_experience_check'
    ) THEN
        ALTER TABLE public.expert_profiles
        DROP CONSTRAINT IF EXISTS expert_profiles_experience_years_check,
        DROP CONSTRAINT IF EXISTS expert_profiles_years_of_experience_check;
        RAISE NOTICE 'Dropped check constraints on experience columns';
    END IF;

    -- 4. Ensure column type is INTEGER (matches entity Integer)
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'expert_profiles'
          AND column_name = 'experience_years'
          AND udt_name = 'int2'
    ) THEN
        ALTER TABLE public.expert_profiles
        ALTER COLUMN experience_years TYPE INTEGER
        USING experience_years::INTEGER;
        RAISE NOTICE 'Cast experience_years from SMALLINT to INTEGER';
    END IF;

END $$;