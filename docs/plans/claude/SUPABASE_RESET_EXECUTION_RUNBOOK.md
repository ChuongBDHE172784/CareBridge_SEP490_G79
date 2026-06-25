---
title: SUPABASE_RESET_EXECUTION_RUNBOOK
project: CareBridge_SEP490_G79
phase: C (Shared-Dev DB Reset)
status: AWAITING_PHASE_C_APPROVAL
created: 2026-06-25
---

# Supabase Shared-Dev Reset Execution Runbook

> **STOP.** This runbook executes a destructive, irreversible reset of the shared-dev Supabase database.
> Every step below requires explicit human confirmation before proceeding.
> Do NOT skip or reorder steps.

---

## Absolute Prerequisites (Do NOT proceed without all)

- [ ] Phase C has been explicitly approved in writing by the team lead
- [ ] All team members have been notified of the maintenance window
- [ ] No active backend or frontend deployment is running against this Supabase instance
- [ ] The `CORE_PLATFORM_RELEASE_AUDIT_V2.md` exists and its final verdict is `READY_FOR_SHARED_DB_RESET`

---

## Step 1 — Preflight: Run the read-only diagnostic

Open the Supabase SQL Editor for the shared-dev project.

Run `docs/plans/claude/supabase-preflight.sql` (copy and paste the entire file).

**Review the output:**
- Confirm `db_name` matches the shared-dev project (not production)
- Confirm the Flyway history shows legacy V2-V12 entries (`RESET REQUIRED` recommendation)
- Note the row counts for each core table — these will be lost
- Confirm `connected_as` is a service-role user with DDL privileges

Do NOT proceed if: db_name is wrong, recommendation is `NO RESET NEEDED`, or you are unsure.

- [ ] Preflight output reviewed and RESET REQUIRED confirmed

---

## Step 2 — Take a schema-only backup

From a terminal with `pg_dump` and the Supabase DB connection string:

```bash
pg_dump \
  --schema-only \
  --no-owner \
  --no-privileges \
  --file="carebridge_supabase_pre_reset_$(date +%Y%m%d_%H%M%S).sql" \
  "${SUPABASE_DB_URL}"
```

Wait for the file to be written.

- [ ] Backup file exists and is non-empty

---

## Step 3 — Verify the backup

Open the backup file and confirm it contains `CREATE TABLE` statements. Spot-check at least:
- `users`
- `consent_grants`
- `community_topics`

- [ ] Backup file verified (contains expected CREATE TABLE statements)

---

## Step 4 — Second approval checkpoint

The lead developer or team lead must review the preflight output and the backup, then explicitly say:

> "I approve the Phase C Supabase shared-dev reset. Proceed."

Document this approval (Slack screenshot, email, or git commit message).

- [ ] Second approval obtained and documented

---

## Step 5 — Execute the guarded reset

In the Supabase SQL Editor, run the following **two SET statements first**, then paste the full reset script:

```sql
-- Run these two SET lines FIRST in the same session:
SET carebridge.reset_confirmation = 'RESET_SHARED_DEV_DATABASE';
SET carebridge.target_environment = 'shared-dev';
```

Then paste and execute `docs/plans/claude/supabase-reset-guarded.sql`.

Expected output:
```
NOTICE: Guards passed. Proceeding with destructive reset on <db_name> at <timestamp>.
NOTICE: Reset complete. Public schema is empty. Apply V1__baseline.sql via backend startup.
DO
```

If you see an EXCEPTION about safety guards — STOP. Check the SET values.

- [ ] Reset script executed successfully
- [ ] Output shows "Reset complete" message

---

## Step 6 — Apply V1 baseline via backend startup

Start the backend with the Supabase profile active. Flyway will automatically apply `V1__baseline.sql` to the clean schema:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=supabase
```

Or with environment variables:
```bash
SPRING_PROFILES_ACTIVE=supabase \
SUPABASE_DB_URL=<url> \
SUPABASE_DB_USERNAME=<user> \
SUPABASE_DB_PASSWORD=<pass> \
./mvnw spring-boot:run
```

Expected log output: `Successfully applied 1 migration to schema "public"`.

- [ ] Backend started successfully against Supabase
- [ ] Flyway log shows "Successfully applied 1 migration"

---

## Step 7 — Verify Flyway history via preflight

Run `supabase-preflight.sql` again. Confirm:
- Flyway history contains **exactly 1 entry**: V1 with `success = true`
- No V2-V12 entries
- `recommendation = 'NO RESET NEEDED'`
- Table count shows exactly **25 tables** in public schema

- [ ] Post-reset preflight passed: exactly 1 Flyway entry (V1), 25 tables

---

## Step 8 — Smoke tests

Run the backend test suite against the freshly migrated Supabase schema:

```bash
SPRING_PROFILES_ACTIVE=supabase \
SUPABASE_DB_URL=<url> \
SUPABASE_DB_USERNAME=<user> \
SUPABASE_DB_PASSWORD=<pass> \
./mvnw test
```

All tests must pass.

- [ ] `./mvnw test` passes (all tests green)

---

## Step 9 — Deploy and end-to-end smoke

Deploy the backend to the shared-dev environment. Verify:
- `POST /api/v1/auth/login` returns 200 (OTP sent)
- `POST /api/v1/auth/verify-otp` returns 200 with tokens
- `GET /api/v1/community/questions` returns 200
- `POST /api/v1/rag/answer` returns 200 with disclaimer

- [ ] End-to-end smoke tests passed

---

## Step 10 — Remove supabase profile restriction comment

Now that the DB is clean, the application.yaml supabase profile comment ("app will refuse to start") is no longer accurate — the app starts fine. Update the comment in `src/main/resources/application.yaml` to reflect post-reset state.

- [ ] Comment updated in application.yaml

---

## Rollback Instructions

If the reset succeeds but V1 migration fails:
1. The public schema is empty — no application data is lost (it was already wiped)
2. Fix the SQL error in `V1__baseline.sql`
3. Drop and recreate the public schema again (repeat Step 5)
4. Restart the backend (Step 6)

If you need to restore pre-reset data:
1. Use the `pg_dump` backup from Step 2
2. Restore with: `psql "${SUPABASE_DB_URL}" < carebridge_supabase_pre_reset_*.sql`
3. This restores the old schema — NOT the new V1 baseline

---

## Files Referenced

| File | Purpose |
|---|---|
| `supabase-preflight.sql` | Read-only diagnostic before and after reset |
| `supabase-reset-guarded.sql` | Guarded destructive reset script |
| `SUPABASE_RESET_EXECUTION_RUNBOOK.md` | This file |
| `V1__baseline.sql` | Migration applied by Flyway after reset |
| `CORE_PLATFORM_RELEASE_AUDIT_V2.md` | Release gate document (must be READY) |
