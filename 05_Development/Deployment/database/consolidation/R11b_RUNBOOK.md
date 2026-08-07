# R11b runbook — cutting the notification queue over to `notification_jobs`

**Spec:** V3 §3.8 · **Plan:** §4.9 (cutover runbook), §7.5 (quantitative gates)

Read this whole file before starting. Two facts change the shape of the work compared to the
generic plan, and both were measured, not assumed.

---

## Fact 1 — the notification feature is currently switched OFF

Both planners and both workers are gated on one flag per domain, and both default to `false`:

| Flag (env) | Property | Default | Gates |
| --- | --- | --- | --- |
| `REMINDER_SCHEDULE_NOTIFICATION_ENABLED` | `carebridge.notification.reminder-schedule.enabled` | `false` | `ReminderScheduleHorizonPlanner` + `ReminderScheduleWorker` |
| `APPOINTMENT_NOTIFICATION_ENABLED` | `carebridge.notification.appointment.enabled` | `false` | `AppointmentNotificationHorizonPlanner` + `AppointmentNotificationWorker` |

`CareBridgeAPI/.env` sets neither, so in this deployment nothing plans and nothing claims.
The 221 reminder-schedule / 10 appointment rows measured on 2026-08-06 are a **static
backlog**, and `PROCESSING = 0` is true trivially rather than because a queue drained.

**Consequence:** the hard part of plan §4.9 — freezing a live queue and racing a final delta
against it — does not apply while the flags are off. If they are off at cutover, there is no
delta to replay. Confirm they are off **in the deployed environment**, not just in the repo:

```bash
# in the running container / on the host that runs the service
printenv REMINDER_SCHEDULE_NOTIFICATION_ENABLED APPOINTMENT_NOTIFICATION_ENABLED
```

Empty or `false` for both ⇒ proceed on the simple path (§A). Anything `true` ⇒ use the
freeze path (§B).

## Fact 2 — the planner/worker code cutover is NOT written yet

`notification_jobs` exists, is backfilled, and has a tested `NotificationJobRepository`
(R11a). But these still target the retired tables:

| File | Currently uses |
| --- | --- |
| `reminder/schedule/service/ReminderScheduleProcessingService.java` | `ReminderScheduleJobRepository` |
| `reminder/schedule/service/ReminderScheduleServiceImpl.java` | `ReminderScheduleJobRepository` |
| `reminder/notification/service/AppointmentNotificationProcessingService.java` | `AppointmentNotificationJobRepository` |
| `reminder/notification/service/AppointmentNotificationScheduleService.java` | `AppointmentNotificationJobRepository` |

**Step 1 below is development work, not an operational step.** It is the actual content of
R11b; everything else is sequencing around it.

---

## Step 0 — preconditions

```bash
cd 05_Development/CareBridgeAPI
set -a && . ./.env && set +a
java -cp "<scratch>;<postgres-jdbc.jar>" SqlRunner \
  ../Deployment/database/consolidation/04_readiness_check.sql
```

Required before going further:

- rows 1–3 `PASS` (no `PROCESSING`, no `job_id` collision)
- a **tested** restore point or PITR timestamp recorded in the change ticket, with the name
  of whoever verified it
- both flags confirmed off in the deployed environment (Fact 1)

> **The deploy applies more than R11b.** Flyway has no partial mode: starting the service
> applies **all 10 pending consolidation migrations in order**, and two of them
> (`V20260806100000`, `V20260806110000`) contain `DROP TABLE` for the Partner, account
> workflow, device and archive objects. Run `01_data_gates.sql` and
> `00_preflight_dependencies.sql` first and store the output. After those DROPs commit,
> rollback is forward-fix or PITR only.

## Step 1 — switch planners and workers to the common repository

Replace the two source repositories with `NotificationJobRepository`, passing the
discriminator on every call. The identity checks map like this:

| Old call | New call |
| --- | --- |
| `existsByScheduleIdAndScheduleRevisionAndOccurrenceDateAndLocalTime(…)` | `existsByJobTypeAndScheduleIdAndScheduleRevisionAndOccurrenceDateAndLocalTime(REMINDER_SCHEDULE, …)` |
| `existsByReminderIdAndOccurrenceIdAndConfigRevisionAndOffsetMinutes(…)` | `existsByJobTypeAndReminderIdAndOccurrenceIdAndConfigRevisionAndOffsetMinutes(APPOINTMENT, …)` |
| `findClaimableIds(status, now, page)` | `findClaimableIds(jobType, status, now, page)` |
| `claim(jobId, workerId, …)` | `claim(jobId, jobType, workerId, …)` |
| `requeueStale(cutoff, …)` | `requeueStale(jobType, cutoff, …)` |
| `transitionAfterProcessing(jobId, workerId, …)` | `transitionAfterProcessing(jobId, jobType, workerId, …)` |
| `cancelObsoleteRevisions(scheduleId, …)` | `cancelObsoleteScheduleRevisions(scheduleId, …)` |
| `cancelObsoleteRevisions(reminderId, …)` | `cancelObsoleteConfigRevisions(reminderId, …)` |

Job creation switches from the two entity builders to `NotificationJob.builder()` with
`jobType` set and only that branch's fields populated — the database CHECK rejects a row
carrying both branches, so a half-converted builder fails loudly rather than writing
nonsense.

**Do not delete the old entities or repositories in this step.** They are the rollback path
until R12.

Verification for step 1:

```bash
./mvnw.cmd test -Dtest='NotificationJobRepositoryEmbeddedPostgresTest,ReminderScheduleProcessingServiceTest,AppointmentNotificationProcessingServiceTest,ReminderScheduleServiceTest,AppointmentNotificationScheduleServiceTest,JobTransitionFlushContractTest'
```

## Step A — cutover with the feature off (expected path)

1. Deploy. Flyway applies the 10 migrations; `V20260806160000` creates and backfills
   `notification_jobs`.
2. Re-run `04_readiness_check.sql`. Rows 4 and 5 must be `PASS` — every source row is in the
   target. With the planners off, no delta can have appeared.
3. Verify the counts landed:
   ```sql
   SELECT job_type, status, count(*) FROM notification_jobs GROUP BY 1,2 ORDER BY 1,2;
   -- expect 221 REMINDER_SCHEDULE + 10 APPOINTMENT, same status split as the source tables
   ```
4. Go to Step 4 (observation). There is no final delta to run.

## Step B — cutover with the feature on (freeze path)

Only if either flag is `true` in the deployed environment.

1. **Disable both flags and restart**, so planners stop *and* workers stop claiming. Plan
   §4.9 is explicit that "disable planners" must include the workers; a worker left running
   would keep mutating the source queue while the delta is copied.
2. Wait for `PROCESSING = 0`. Re-run `04_readiness_check.sql` until rows 1–2 are `PASS`.
   Requeue anything stuck past the stale window rather than waiting indefinitely.
3. Run the readiness check **twice, at least 5 minutes apart**, and compare row 6:
   ```
   6 | R11b | queue depth snapshot … | 221 schedule / 10 appointment, at <t1>
   6 | R11b | queue depth snapshot … | 221 schedule / 10 appointment, at <t2>
   ```
   Identical depths in both runs, with `t2 - t1 >= 5 min`, is what §7.5 means by *queue
   quiesced*. Different depths mean something is still writing — find it before continuing.
4. Deploy (applies the migrations, including the backfill).
5. Run the final delta **in one transaction**, for rows created after the expand backfill:
   ```sql
   BEGIN;

   INSERT INTO public.notification_jobs (
       job_id, job_type, due_at, status, attempt_count, next_attempt_at,
       locked_by, locked_at, notification_record_id, last_error_code,
       created_at, updated_at,
       schedule_id, schedule_revision, occurrence_date, local_time, time_zone)
   SELECT j.job_id, 'REMINDER_SCHEDULE', j.due_at, j.status, j.attempt_count, j.next_attempt_at,
          j.locked_by, j.locked_at, j.notification_record_id, j.last_error_code,
          j.created_at, j.updated_at,
          j.schedule_id, j.schedule_revision, j.occurrence_date, j.local_time, j.time_zone
   FROM public.reminder_schedule_jobs j
   WHERE NOT EXISTS (SELECT 1 FROM public.notification_jobs t WHERE t.job_id = j.job_id);

   INSERT INTO public.notification_jobs (
       job_id, job_type, due_at, status, attempt_count, next_attempt_at,
       locked_by, locked_at, notification_record_id, last_error_code,
       created_at, updated_at,
       reminder_id, occurrence_id, occurrence_generation, occurrence_scheduled_at,
       config_revision, offset_minutes)
   SELECT j.job_id, 'APPOINTMENT', j.due_at, j.status, j.attempt_count, j.next_attempt_at,
          j.locked_by, j.locked_at, j.notification_record_id, j.last_error_code,
          j.created_at, j.updated_at,
          j.reminder_id, j.occurrence_id, j.occurrence_generation, j.occurrence_scheduled_at,
          j.config_revision, j.offset_minutes
   FROM public.appointment_notification_jobs j
   WHERE NOT EXISTS (SELECT 1 FROM public.notification_jobs t WHERE t.job_id = j.job_id);

   -- Parity check inside the same transaction: roll back rather than commit a partial move.
   DO $$
   DECLARE s_src bigint; s_tgt bigint; a_src bigint; a_tgt bigint;
   BEGIN
       SELECT count(*) INTO s_src FROM public.reminder_schedule_jobs;
       SELECT count(*) INTO a_src FROM public.appointment_notification_jobs;
       SELECT count(*) INTO s_tgt FROM public.notification_jobs WHERE job_type='REMINDER_SCHEDULE';
       SELECT count(*) INTO a_tgt FROM public.notification_jobs WHERE job_type='APPOINTMENT';
       IF s_src <> s_tgt OR a_src <> a_tgt THEN
           RAISE EXCEPTION 'R11B_DELTA_MISMATCH: schedule %/%, appointment %/%',
               s_src, s_tgt, a_src, a_tgt;
       END IF;
   END $$;

   COMMIT;
   ```
   Never resolve a `job_id` clash with `ON CONFLICT DO NOTHING` — V3 §3.8 forbids it because
   it silently drops a job. A clash means row 3 of the readiness check was not honoured.

## Step 4 — enable and observe

Turn the flags back on (or on for the first time) and watch for the §7.5 job gates:

- at least **two planner cycles** — the crons are `0 20 2 * * *` (reminder schedule) and
  `0 15 2 * * *` (appointment), so this is two days unless you shorten
  `REMINDER_SCHEDULE_PLANNER_CRON` / `APPOINTMENT_NOTIFICATION_PLANNER_CRON` for the window
- at least **one retry path** exercised
- at least **one stale-lock requeue** (`*_STALE_MINUTES`, default 10)
- **no duplicate notification and no duplicate job identity**

```sql
-- must stay empty for the whole observation window
SELECT job_type, status, count(*) FROM notification_jobs GROUP BY 1,2 ORDER BY 1,2;

SELECT count(*) AS duplicate_schedule_identity FROM (
  SELECT schedule_id, schedule_revision, occurrence_date, local_time
    FROM notification_jobs WHERE job_type='REMINDER_SCHEDULE'
   GROUP BY 1,2,3,4 HAVING count(*) > 1) d;

SELECT count(*) AS duplicate_appointment_identity FROM (
  SELECT reminder_id, occurrence_id, config_revision, offset_minutes
    FROM notification_jobs WHERE job_type='APPOINTMENT'
   GROUP BY 1,2,3,4 HAVING count(*) > 1) d;

-- source queues must be frozen: these counts must not move
SELECT (SELECT count(*) FROM reminder_schedule_jobs) AS src_schedule,
       (SELECT count(*) FROM appointment_notification_jobs) AS src_appointment;
```

## Step 4b — accelerating the observation window

Two obstacles make "just wait" the slow path, and one of them never resolves by waiting:

1. **The planner is idempotent.** With the 35-day horizon already materialised, a healthy
   cycle inserts nothing, so gates 6 and 7 stay `INFO` no matter how long you wait. Cycles
   only become visible when the horizon rolls forward or a schedule is created/changed.
2. **Retry and stale-lock need a failure.** Neither path runs unless a delivery fails or a
   worker dies mid-job.

### Fast, honest path (~15 minutes)

Set for the window, then restore:

| Env | Window value | Restore to |
| --- | --- | --- |
| `REMINDER_SCHEDULE_PLANNER_CRON` | `0 */2 * * * *` | `0 20 2 * * *` |
| `APPOINTMENT_NOTIFICATION_PLANNER_CRON` | `0 */2 * * * *` | `0 15 2 * * *` |
| `REMINDER_SCHEDULE_STALE_MINUTES` | `1` | `10` |
| `APPOINTMENT_NOTIFICATION_STALE_MINUTES` | `1` | `10` |

Then exercise the paths for real, as a user would:

1. **Planner cycle + worker claim + terminal transition.** Create a reminder schedule whose
   local time is ~4 minutes out, via `POST /api/v1/reminder-schedules`. The planner
   materialises a job (gate 6 moves), the worker claims it within 15s of `due_at`, processes
   it and writes a terminal status. Repeat once so gate 7 sees two distinct cycles.
2. **Retry.** A delivery failure produces `attempt_count > 1` (gate 8). This happens on its
   own if push delivery is unavailable for that user; do not fabricate one by editing rows —
   a hand-edited job proves nothing about the code path.
3. **Stale lock.** With `STALE_MINUTES=1`, restart the service while a job is `PROCESSING`.
   The abandoned lock ages out and the next `requeueStale` returns it to `PENDING` with the
   attempt count already advanced (gate 9).

Restore the four values afterwards. Leaving a 2-minute planner cron in production means the
horizon planner runs 720 times a day for no benefit.

### If retry and stale-lock stay unobserved

§7.5 permits relaxing a gate with Tech Lead / Release Owner approval. The defensible
relaxation here is retry and stale-lock **only**, on this evidence:

- `NotificationJobRepositoryEmbeddedPostgresTest` exercises stale-lock requeue, claim
  concurrency (4 workers racing one job, exactly one wins) and discriminator isolation
  against real PostgreSQL 18.
- `AppointmentNotificationProcessingServiceTest` and `ReminderScheduleProcessingServiceTest`
  cover the retry/backoff and terminal-transition paths, including the fenced transition.

Record the approval, who gave it and what evidence it rests on. Do **not** relax the
duplicate-identity or frozen-source-queue gates: those are the ones that detect a real
cutover fault, and no unit test can substitute for them on live data.

### R12 can move before the contract migration

Worth separating, because they are gated differently:

- **R12** deletes the now-unused source entities, repositories and the two mirror writes. It
  drops no data. Its real precondition is "nothing calls the source path any more", which
  gates 1 and 2 already demonstrate (both source queues frozen at their cutover counts).
- **The queue contract migration** drops `reminder_schedule_jobs` and
  `appointment_notification_jobs`. That is irreversible and is what the full observation
  protects.

Doing R12 early is therefore reasonable, at one explicit cost: the source entities are the
cheap rollback path, and deleting them means a rollback becomes a redeploy of the previous
build rather than a config flip. Given the recorded waiver that this database holds
disposable test data, that cost is likely acceptable — but it is a Release Owner decision,
not an implementation detail.

## Rollback

| Point of failure | Action |
| --- | --- |
| Before deploy | Nothing to undo. |
| After deploy, before enabling flags | Both flags off ⇒ nothing is processing. Redeploy the previous build; `notification_jobs` is simply unused. The source tables are untouched and authoritative. **Except** the two contract migrations have already dropped Partner/account/device/archive objects — that part is forward-fix or PITR only. |
| After enabling, duplicates or drift observed | Disable both flags, redeploy the previous build (it reads the source queues, which the new code never deleted from), then reconcile `notification_jobs` before retrying. |

Roll back immediately on any duplicate notification, duplicate job identity, or job-state
drift — §7.5 treats those as unconditional rollback triggers, not judgement calls.

## R12, afterwards

Only once the observation gates are met: delete `ReminderScheduleJob`,
`AppointmentNotificationJob` and their repositories, plus the mirror writes left in
`ReminderScheduleServiceImpl#writeTimes` and
`AppointmentNotificationScheduleService#mirrorRulesToSourceTable`. Then, and only then, the
contract migration that drops `reminder_schedule_jobs` and `appointment_notification_jobs`.
