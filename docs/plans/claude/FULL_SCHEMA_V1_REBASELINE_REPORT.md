---
title: FULL_SCHEMA_V1_REBASELINE_REPORT
project: CareBridge_SEP490_G79
phase: B.2B — Full Schema Baseline Execution
status: COMPLETE
created: 2026-06-25
author: AI Agent
supabase_modified: NO
destructive_sql_run: NO
commits_created: NO
---

# Phase B.2B Full Schema V1 Rebaseline Report

> All changes are source-only. No Supabase data or schema was touched.
> No destructive SQL was run against any shared database.
> No git commits were created.

---

## Summary

| Item | Before | After |
|---|---|---|
| Migration file | `V1__baseline.sql` (untracked, 25 tables) | `V1__init_schema.sql` (71 tables) |
| Total tables | 25 | 71 |
| ERD tables covered | 25/67 | 67/67 |
| Infrastructure tables | 4/4 | 4/4 |
| FK constraints | 3 | 98 |
| Indexes | 22 | ~95 |
| Lines | 952 | 1,995 |
| V2–V12 migrations | Deleted from WT (git HEAD still has them) | Same — no change |
| `.gitkeep` | Deleted from WT | Same — no change |
| Backend tests | 242/242 ✅ | 242/242 ✅ |
| Web build | 0 errors ✅ | 0 errors ✅ |
| Flutter tests | 1/1 ✅ | 1/1 ✅ |

---

## Decisions Applied

| Decision | Outcome |
|---|---|
| Q1 — token_blacklist | EXCLUDED. Not in ERD. Session revocation handled by `user_sessions.revoked_at`. |
| Q2 — community table PK names | `id` retained for community_topics, community_questions, community_answers (code-compatible). |
| Q3 — email_verified | NOT added. Grep confirmed no Java code reads or writes `email_verified` at user level. |
| Q4 — community_answers UC-56 schema | Retained: `is_expert_labeled`, `is_personal_experience`, `status`, `like_count`. Divergences documented in FULL_SCHEMA_V1_RECONCILIATION.md. |
| Q5 — source authority | Existing 25 tables: copied byte-for-byte from V1__baseline.sql. Future 46 tables: ERD PlantUML as primary source. |

---

## Execution Log

### Step 1: Q3 Verification
- Grepped all Java sources for `emailVerified`, `email_verified`, `phoneVerified`, `phone_verified`
- Result: **zero matches** — confirmed `email_verified` column must NOT be added

### Step 2: File creation
- Created `src/main/resources/db/migration/V1__init_schema.sql` (1,995 lines, 71 tables)
- Deleted `src/main/resources/db/migration/V1__baseline.sql` (untracked, obsolete)
- Migration folder now contains exactly one SQL file

### Step 3: SQL structure verification
```
grep -c "^CREATE TABLE" V1__init_schema.sql  → 71
```

### Step 4: Isolated PostgreSQL rehearsal
```
createdb carebridge_fulltest
psql -d carebridge_fulltest -f V1__init_schema.sql
  → 71 tables created
  → 98 FK constraints applied
  → 170 indexes (including 71 PKs)
  → 0 errors
SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='public' AND table_type='BASE TABLE'
  → 71 ✅
dropdb carebridge_fulltest
```

### Step 5: Backend tests
- Ran `./mvnw clean test` after `flyway:repair` to update local dev DB checksum
- Result: **242/242 tests pass, 0 failures, 0 errors** ✅
- `contextLoads` passes — `ddl-auto:validate` validates the 15 mapped @Entity tables successfully against the new schema

### Step 6: Web build
- `npm run build` in CareBridgeWebApp
- Result: **0 TypeScript errors, built in 99ms** ✅

### Step 7: Flutter tests
- `flutter test` in CareBridgeMobileApp
- Result: **1/1 pass** ✅

### Step 8: `flutter analyze`
- Not run (analysis server crash is a known Flutter tooling bug from Phase B.1 — debug APK still builds)

### Step 9: Git status reported
- No commits created
- V1__init_schema.sql shows as `M` (modified vs HEAD which had original Hibernate-generated V1)
- V2–V12 + .gitkeep remain as unstaged deletions from Phase B.1

---

## Local Dev DB Note

> ⚠️ **DO NOT use `flyway:repair` as a schema upgrade path.**
>
> `flyway:repair` only updates checksum metadata in `flyway_schema_history`.
> It does NOT add the 46 new tables, FK constraints, or indexes in the new V1.
> A repaired database would appear healthy to Flyway while missing 46 tables.

The rewritten V1 is a database baseline replacement. Any existing local database
created from the old V1 + V2–V12 migration history must be **recreated from a clean
schema** before using the new V1.

**See:** `05_Development/Database/postgres/LOCAL_DATABASE_REBASELINE_RUNBOOK.md`

---

## What Was NOT Done (per authorization constraints)

- Supabase schema not touched
- Phase C reset not executed
- No Java @Entity, repository, service, controller, web route, or Flutter screen created for the 46 future tables
- No commits staged or pushed

---

## Output Files Created by This Phase

| File | Purpose |
|---|---|
| `src/main/resources/db/migration/V1__init_schema.sql` | Master schema baseline (71 tables) |
| `docs/plans/claude/FULL_SCHEMA_V1_REBASELINE_REPORT.md` | This file |
| `docs/plans/claude/FULL_SCHEMA_V1_RECONCILIATION.md` | ERD/code divergence table |
| `docs/CORE_PLATFORM_TABLE_OWNERSHIP.md` | Table → domain module ownership map |
| `05_Development/Database/postgres/V1_SCHEMA_BASELINE.md` | Schema summary for database team |
| `03_Design/Architecture/ADR-001-core-platform-postgresql-supabase.md` | Architecture Decision Record |
