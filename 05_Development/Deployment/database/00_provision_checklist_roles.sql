-- Provision the four deployment-owned roles that V20260731070000 requires.
--
-- WHY THIS EXISTS
-- V20260731070000__canonical_post_20260719180000_schema.sql refuses to run unless
-- these roles already exist with exact attributes, and raises 42501 with
-- CHECKLIST_RETENTION_OWNER_ROLE_REQUIRED / CHECKLIST_OPERATIONS_ROLE_REQUIRED /
-- CHECKLIST_SCHEMA_OWNER_ROLE_REQUIRED / CAREBRIDGE_APPLICATION_ROLE_REQUIRED
-- otherwise. Flyway deliberately carries no CREATEROLE privilege, so it cannot
-- create them itself. Run this ONCE per database, BEFORE the first Flyway run.
--
-- WHO RUNS IT
-- A principal holding CREATEROLE. On Supabase that is the `postgres` role.
-- It must NOT be the Flyway login: the same migration raises
-- CHECKLIST_FLYWAY_ROLE_MUST_BE_SEPARATE if Flyway connects as any of these four.
--
-- ATTRIBUTES ARE CHECKED EXACTLY
-- Every role needs NOINHERIT. PostgreSQL defaults to INHERIT, so a role created
-- by hand without it will exist and still fail the migration. This script ALTERs
-- an existing role back to the required shape rather than assuming it is correct.
--
-- PASSWORDS
-- The two LOGIN roles need passwords to connect, but none are set here — this
-- file is in source control. Set them from the deployment secret store after
-- running this script (see the runbook step 0).
--
--   psql "$SUPABASE_DB_URL" -v ON_ERROR_STOP=1 -f 00_provision_checklist_roles.sql

\set ON_ERROR_STOP on

BEGIN;

DO $$
DECLARE
    -- name, needs LOGIN
    target record;
BEGIN
    FOR target IN
        SELECT * FROM (VALUES
            ('carebridge_checklist_schema_owner',    false),
            ('carebridge_checklist_retention_owner', false),
            ('checklist_operations',                 true),
            ('carebridge_application',               true)
        ) AS t(role_name, needs_login)
    LOOP
        IF NOT EXISTS (
            SELECT 1 FROM pg_catalog.pg_roles WHERE rolname = target.role_name
        ) THEN
            EXECUTE format(
                'CREATE ROLE %I %s NOSUPERUSER NOCREATEDB NOCREATEROLE '
                'NOINHERIT NOREPLICATION NOBYPASSRLS',
                target.role_name,
                CASE WHEN target.needs_login THEN 'LOGIN' ELSE 'NOLOGIN' END);
            RAISE NOTICE 'created role %', target.role_name;
        ELSE
            -- Normalise: an existing role with the wrong attributes fails the
            -- migration just as loudly as a missing one, and is harder to spot.
            EXECUTE format(
                'ALTER ROLE %I %s NOSUPERUSER NOCREATEDB NOCREATEROLE '
                'NOINHERIT NOREPLICATION NOBYPASSRLS',
                target.role_name,
                CASE WHEN target.needs_login THEN 'LOGIN' ELSE 'NOLOGIN' END);
            RAISE NOTICE 'normalised existing role %', target.role_name;
        END IF;
    END LOOP;
END $$;

-- Fail closed if anything still does not match what the migration asserts.
-- This mirrors V20260731070000 lines 3099-3157 predicate for predicate.
DO $$
DECLARE
    bad text;
BEGIN
    SELECT string_agg(role_name, ', ') INTO bad
    FROM (VALUES
        ('carebridge_checklist_schema_owner',    false),
        ('carebridge_checklist_retention_owner', false),
        ('checklist_operations',                 true),
        ('carebridge_application',               true)
    ) AS t(role_name, needs_login)
    WHERE NOT EXISTS (
        SELECT 1 FROM pg_catalog.pg_roles r
        WHERE r.rolname = t.role_name
          AND r.rolcanlogin   = t.needs_login
          AND r.rolsuper      = false
          AND r.rolcreatedb   = false
          AND r.rolcreaterole = false
          AND r.rolinherit    = false
          AND r.rolreplication = false
          AND r.rolbypassrls  = false
    );

    IF bad IS NOT NULL THEN
        RAISE EXCEPTION 'CHECKLIST_ROLE_PROVISIONING_INCOMPLETE: %', bad;
    END IF;

    -- The migration also rejects Flyway connecting as one of these principals.
    IF current_user IN (
        'carebridge_application',
        'checklist_operations',
        'carebridge_checklist_retention_owner',
        'carebridge_checklist_schema_owner'
    ) THEN
        RAISE EXCEPTION
            'CHECKLIST_PROVISIONER_MUST_BE_SEPARATE: ran as %', current_user;
    END IF;

    RAISE NOTICE 'CHECKLIST_ROLE_PROVISIONING_VERIFIED';
END $$;

COMMIT;

-- Read-only confirmation; safe to re-run on its own.
SELECT rolname,
       rolcanlogin,
       rolinherit,
       rolsuper,
       rolcreatedb,
       rolcreaterole,
       rolreplication,
       rolbypassrls
FROM pg_catalog.pg_roles
WHERE rolname IN (
    'carebridge_checklist_schema_owner',
    'carebridge_checklist_retention_owner',
    'checklist_operations',
    'carebridge_application'
)
ORDER BY rolname;
