-- =============================================================================
-- V20260703000005__fix_expert_profile_id_column.sql
-- Purpose: Reconcile expert_profiles column name after remote migration
--          20260703170640 renamed expert_profile_id → id; this migration
--          restores the column name expected by local JPA entities.
-- =============================================================================

DO $$
BEGIN
    -- If expert_profile_id already exists, nothing to do
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name  = 'expert_profiles'
          AND column_name = 'expert_profile_id'
    ) THEN
        RAISE NOTICE 'expert_profile_id already exists in expert_profiles — skipping';
    ELSIF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name  = 'expert_profiles'
          AND column_name = 'id'
    ) THEN
        -- Remote migration renamed it to 'id' — rename back
        ALTER TABLE public.expert_profiles RENAME COLUMN id TO expert_profile_id;
        RAISE NOTICE 'Renamed id → expert_profile_id in expert_profiles';
    ELSE
        -- Neither column exists — create it
        ALTER TABLE public.expert_profiles
            ADD COLUMN expert_profile_id uuid NOT NULL DEFAULT gen_random_uuid();
        RAISE NOTICE 'Added expert_profile_id column to expert_profiles';
    END IF;
END $$;
