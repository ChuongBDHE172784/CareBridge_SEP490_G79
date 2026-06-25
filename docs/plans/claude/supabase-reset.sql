-- =============================================================================
-- supabase-reset.sql — CareBridge Supabase schema reset script
-- Status: GUARDED — do NOT execute until APPROVE_SHARED_DB_RESET is confirmed
-- =============================================================================
--
-- Safety checklist (must all be ✅ before execution):
-- [ ] Phase B exit gates in PLAN-CORE-PLATFORM-REBASELINE.md all pass
-- [ ] ./mvnw test → 0 failures, 0 errors
-- [ ] npm run build → 0 TypeScript errors
-- [ ] flutter build apk --debug → 0 compilation errors
-- [ ] git status → clean working tree on HuyND
-- [ ] .env is NOT staged or committed
-- [ ] User has confirmed DB row counts are all zero (no live data)
-- [ ] User has typed APPROVE_SHARED_DB_RESET in the session
--
-- What this script does:
-- 1. Terminates all application connections (run from a DBA/admin session)
-- 2. Drops all tables in the public schema (CASCADE handles FK order)
-- 3. Drops flyway_schema_history so Flyway applies V1__baseline.sql fresh
-- 4. Recreates the public schema
-- After running: start the application → Flyway runs V1 → schema created
--
-- Connection: execute via Supabase SQL editor or psql with SUPABASE_DB_* credentials
-- =============================================================================

-- ⚠️  DO NOT RUN THIS FILE DIRECTLY WITHOUT PHASE C EXPLICIT APPROVAL ⚠️

-- Step 1: Drop Flyway history table (lets Flyway re-apply V1 on next startup)
DROP TABLE IF EXISTS public.flyway_schema_history;

-- Step 2: Drop and recreate public schema (drops all application tables)
-- This is equivalent to dropping each table individually but handles FK cycles.
DROP SCHEMA public CASCADE;
CREATE SCHEMA public;

-- Step 3: Restore default schema privileges (required for Supabase pooled connections)
-- These grants are managed by Supabase infrastructure; uncomment if needed:
-- GRANT ALL ON SCHEMA public TO postgres;
-- GRANT ALL ON SCHEMA public TO public;

-- After execution: start the application with SPRING_PROFILES_ACTIVE=supabase
-- Flyway will detect empty schema, create flyway_schema_history, apply V1__baseline.sql
-- Hibernate will validate the created schema → application starts normally
