---
title: Local Database Rebaseline Runbook
project: CareBridge_SEP490_G79
applies_to: Local developer PostgreSQL databases
created: 2026-06-25
---

# Local Database Rebaseline Runbook

## Why This Runbook Exists

The CareBridge V1 migration (`V1__init_schema.sql`) was rewritten in Phase B.2B.
It now creates **71 tables** instead of the previous 25.

> ⚠️ **Critical Safety Rule**
>
> `flyway:repair` only updates checksum metadata in `flyway_schema_history`.
> It does **NOT** add tables, FK constraints, or indexes.
>
> A local database that went through `flyway:repair` would appear healthy to Flyway
> (checksum matches, validate passes) while silently missing 46 tables.
>
> **Do not use `flyway:repair` as a schema upgrade mechanism.**

Any local database created from the old V1 + V2–V12 migration history must be
recreated from a clean schema before using the new V1.

---

## Case A — Disposable Local Database (No Data Worth Keeping)

Use this path if your local database contains only test data or seed data that
can be recreated.

```bash
# 1. (Optional) Create a backup before dropping
pg_dump -U postgres carebridge > carebridge_backup_$(date +%Y%m%d_%H%M%S).sql

# 2. Drop and recreate the database
dropdb -U postgres carebridge
createdb -U postgres carebridge

# 3. Apply the new V1 via Flyway
cd 05_Development/CareBridgeAPI
./mvnw flyway:migrate \
  -Dflyway.url=jdbc:postgresql://localhost:5432/carebridge \
  -Dflyway.user=postgres \
  -Dflyway.password=password

# 4. Verify 71 tables were created
psql -U postgres -d carebridge -c \
  "SELECT COUNT(*) FROM information_schema.tables \
   WHERE table_schema='public' AND table_type='BASE TABLE' \
   AND table_name != 'flyway_schema_history';"
# Expected: 71

# 5. Run backend tests to confirm everything works
./mvnw test
# Expected: 242/242 pass (or current count)
```

### Quick Variant (if you never back up local DBs)

```bash
dropdb -U postgres carebridge && createdb -U postgres carebridge
cd 05_Development/CareBridgeAPI
./mvnw flyway:migrate \
  -Dflyway.url=jdbc:postgresql://localhost:5432/carebridge \
  -Dflyway.user=postgres \
  -Dflyway.password=password
./mvnw test
```

---

## Case B — Local Database With Data Worth Keeping

If your local database contains data that cannot be recreated (e.g., manually
entered test cases, consultation records, seeded partner data), follow this path.

> ⚠️ **You cannot simply replay the old data into the new schema without a
> migration plan.** The 46 new tables have FK constraints that reference existing
> tables. Column shapes may differ. A dump-and-restore is not guaranteed to work.

Recommended steps:

1. **Backup first — mandatory:**
   ```bash
   pg_dump -U postgres --format=custom carebridge > carebridge_prebaseline.dump
   ```

2. **Identify which data is worth migrating:**
   List the tables you care about and check if their columns match the new schema.
   The new V1 is the authority: `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql`.

3. **Do NOT run `flyway:repair`** — it will not add the missing tables.

4. **Drop, recreate, and apply V1:**
   ```bash
   dropdb -U postgres carebridge
   createdb -U postgres carebridge
   cd 05_Development/CareBridgeAPI
   ./mvnw flyway:migrate \
     -Dflyway.url=jdbc:postgresql://localhost:5432/carebridge \
     -Dflyway.user=postgres \
     -Dflyway.password=password
   ```

5. **Restore only the tables you need from the backup:**
   ```bash
   pg_restore -U postgres -d carebridge --table=users \
     --data-only --disable-triggers carebridge_prebaseline.dump
   # Repeat for each table with data worth keeping
   ```

6. **Verify and test:**
   ```bash
   psql -U postgres -d carebridge -c \
     "SELECT COUNT(*) FROM information_schema.tables \
      WHERE table_schema='public' AND table_type='BASE TABLE' \
      AND table_name != 'flyway_schema_history';"
   # Expected: 71
   cd 05_Development/CareBridgeAPI
   ./mvnw test
   ```

---

## What `flyway:repair` Does (and Does Not Do)

| Action | `flyway:repair` | Drop + Recreate + `flyway:migrate` |
|---|---|---|
| Updates checksum in `flyway_schema_history` | ✅ Yes | ✅ Yes (new entry) |
| Creates new tables from V1 | ❌ **No** | ✅ Yes (71 tables) |
| Adds FK constraints from V1 | ❌ **No** | ✅ Yes (98 FKs) |
| Adds indexes from V1 | ❌ **No** | ✅ Yes (~95 indexes) |
| Preserves existing data | ✅ Yes (unchanged) | ❌ No (drop erases data) |

`flyway:repair` is a valid tool when a migration file's checksum changes but the
**schema content is identical** (e.g., whitespace change). It is **not** a substitute
for applying a migration that adds new schema objects.

---

## Supabase

Do **not** apply this runbook to Supabase. The shared Supabase database requires
a Phase C guarded reset with explicit team approval before V1 can be applied.

See: `05_Development/Database/postgres/SUPABASE_REBASELINE_RUNBOOK.md`
