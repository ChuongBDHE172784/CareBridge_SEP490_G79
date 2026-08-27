---
title: Supabase Rebaseline Runbook
project: CareBridge_SEP490_G79
applies_to: Shared Supabase PostgreSQL database
created: 2026-06-25
status: AWAITING_PHASE_C_APPROVAL
---

# Supabase Rebaseline Runbook

## Current Status

**The Supabase database has NOT been rebaselined.**

The new `V1__init_schema.sql` (71 tables) has not been applied to Supabase.
Applying the new V1 to Supabase is a **Phase C action** that requires explicit
team approval and execution of the guarded reset runbook.

**Do not apply V1 to Supabase manually, via psql, or via Spring Boot startup.**

---

## Why Supabase Cannot Be Simply Repaired

The shared Supabase database was created with the old migration history
(V1–V12). The 46 new tables in `V1__init_schema.sql` do not exist there.

> ⚠️ `flyway:repair` against Supabase would update the V1 checksum in
> `flyway_schema_history` but would leave the 46 new tables absent.
> The backend would then fail at startup with `Table not found` errors
> as soon as any code referencing the new tables is deployed.
>
> **Do not run `flyway:repair` against Supabase under any circumstances.**

---

## What Must Happen (Phase C)

The Supabase rebaseline requires:

1. **Team sign-off** — all developers acknowledge that Supabase data will be
   erased and that the new V1 schema will replace the current schema.

2. **Data backup** — any Supabase data worth preserving must be exported before
   the reset. The application currently stores no production user data in Supabase
   (development/test data only), but confirm this before proceeding.

3. **Guarded reset execution** — run the guarded reset SQL script
   (`docs/plans/claude/SUPABASE_RESET_EXECUTION_RUNBOOK.md`) with both required
   GUC flags set. The script uses `DROP SCHEMA public CASCADE` + `CREATE SCHEMA public`
   to erase the old schema.

4. **V1 migration** — after the reset, start the Spring Boot backend against
   the empty Supabase DB once. Flyway will apply `V1__init_schema.sql` automatically
   on startup, creating all 71 tables.

5. **Verify** — confirm 71 tables exist and `flyway_schema_history` has exactly
   one `V1` entry.

---

## Phase C Checklist (Do Not Skip Steps)

```
[ ] All team members notified of impending Supabase reset
[ ] Supabase data backup created and stored
[ ] All feature branches that depend on Supabase schema state identified
[ ] Guarded reset SQL executed with BOTH required GUC flags
[ ] Spring Boot started against Supabase (Flyway auto-migrates V1)
[ ] 71 tables verified: SELECT COUNT(*) FROM information_schema.tables
    WHERE table_schema='public' AND table_type='BASE TABLE'
    AND table_name != 'flyway_schema_history'; → 71
[ ] flyway_schema_history has exactly one row: version=1, success=true
[ ] Backend contextLoads test passes against Supabase profile
[ ] All team members update local branches from dev after reset
```

---

## Connection to Application

The application connects to Supabase using the `supabase` Spring profile.
Connection details come from environment variables — never hardcoded.

The application uses `ddl-auto: validate` in all profiles.
**Never change this to `create`, `update`, or `create-drop`.**

---

## Absolute Prohibitions

```
❌ NEVER run flyway:repair against Supabase
❌ NEVER run flyway:clean against Supabase
❌ NEVER set ddl-auto=update/create/create-drop against Supabase
❌ NEVER apply the new V1 to Supabase before Phase C approval
❌ NEVER run DROP, TRUNCATE, DELETE, or ALTER TABLE against Supabase outside
   the guarded reset procedure
❌ NEVER commit .env files containing Supabase credentials
```

---

## Related Documents

- `05_Development/Database/postgres/LOCAL_DATABASE_REBASELINE_RUNBOOK.md` — local dev DB
- `docs/plans/claude/SUPABASE_RESET_EXECUTION_RUNBOOK.md` — the guarded reset SQL
- `03_Design/Architecture/ADR-001-core-platform-postgresql-supabase.md` — architecture decisions
