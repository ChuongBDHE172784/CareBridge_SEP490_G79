-- =============================================================================
-- supabase-reset-guarded.sql — CareBridge Shared-Dev Supabase Guarded Reset
-- Purpose : Destroy and recreate the public schema on the shared-dev Supabase.
--           Must be executed as part of Phase C DB Reset (see RUNBOOK).
-- Safety  : Two GUC guards MUST be set before this script is pasted.
--           If either guard is wrong the script raises an exception and exits.
--           DESTRUCTIVE — run ONLY after:
--             (a) pg_dump backup verified
--             (b) preflight.sql reviewed and RESET REQUIRED confirmed
--             (c) team lead has approved Phase C in writing
-- =============================================================================

DO $$
DECLARE
  v_confirm  text := current_setting('carebridge.reset_confirmation', true);
  v_env      text := current_setting('carebridge.target_environment', true);
BEGIN
  -- Guard 1: explicit confirmation phrase
  IF v_confirm IS DISTINCT FROM 'RESET_SHARED_DEV_DATABASE' THEN
    RAISE EXCEPTION
      'Safety guard failed. SET carebridge.reset_confirmation = ''RESET_SHARED_DEV_DATABASE'' first. Got: %',
      COALESCE(v_confirm, '<not set>');
  END IF;

  -- Guard 2: explicit target environment
  IF v_env IS DISTINCT FROM 'shared-dev' THEN
    RAISE EXCEPTION
      'Safety guard failed. SET carebridge.target_environment = ''shared-dev'' first. Got: %',
      COALESCE(v_env, '<not set>');
  END IF;

  RAISE NOTICE 'Guards passed. Proceeding with destructive reset on % at %.',
    current_database(), now();

  -- === DESTRUCTIVE SECTION ===
  -- Drop the entire public schema and recreate it clean.
  -- All tables, sequences, indexes, functions, and triggers are removed.
  EXECUTE 'DROP SCHEMA public CASCADE';
  EXECUTE 'CREATE SCHEMA public';
  EXECUTE 'GRANT ALL ON SCHEMA public TO postgres';
  EXECUTE 'GRANT ALL ON SCHEMA public TO public';

  -- Drop the Flyway history so the backend will apply V1__baseline.sql cleanly.
  -- flyway_schema_history lives in the public schema so it is already dropped above.
  -- No additional action needed.

  RAISE NOTICE 'Reset complete. Public schema is empty. Apply V1__baseline.sql via backend startup.';
END;
$$;

-- =============================================================================
-- HOW TO USE:
--
--   Step 1 — Set guards in the same SQL session before pasting this script:
--
--       SET carebridge.reset_confirmation = 'RESET_SHARED_DEV_DATABASE';
--       SET carebridge.target_environment = 'shared-dev';
--
--   Step 2 — Paste and execute this entire file.
--
--   Step 3 — Start the backend with --spring.profiles.active=supabase.
--             Flyway will apply V1__baseline.sql and create the 25 tables.
--
--   Step 4 — Verify via supabase-preflight.sql that V1 is the only history entry.
--
-- See SUPABASE_RESET_EXECUTION_RUNBOOK.md for the complete 10-step process.
-- =============================================================================
