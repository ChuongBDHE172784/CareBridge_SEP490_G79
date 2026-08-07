# Database consolidation — feature retirement tracker (R0a → R5c)

**Programme:** `08_References/Database_Table_Audit_And_Consolidation V3.md` (target database)
and `08_References/Database_Consolidation_Source_Code_Refactor_Plan.md` (source-code impact and
deployment order).

This folder covers the **feature-retirement half** of the programme only: the workstreams whose
deployment pattern is *client removal → backend module removal → observation → DB drop*. The
persistence-consolidation half (reminder times array, appointment `rules_jsonb`, safety typed
columns, checklist v2, consultation booking, `notification_jobs`) follows the *expand → backfill →
cutover → observe → contract* pattern and is not started here.

## Release-owner decisions recorded for this programme

| Decision | Value | Consequence |
| --- | --- | --- |
| External clients | **None** — app has never shipped outside the team | Plan §5.1 deprecation window waived; client cleanup and backend endpoint removal may share a release. No deprecation response shim is built. |
| Migration authorship | In-repo Flyway files, additive-only history | No historical migration is edited (plan §2.6). Contract migrations are new `V2026…` files. |
| Migration rehearsal | `Postgresql18CanonicalSchemaIntegrationTest` on zonky embedded Postgres 18 | Applies the whole Flyway chain from scratch; stands in for the "clone from logical dump" rehearsal (V3 §7.5) until a Supabase clone is available. |

## Table delta delivered by this half

| Object | V3 §  | Δ tables |
| --- | --- | ---: |
| `account_deletion_requests` | 3.1.1 | −1 |
| `account_lock_appeals` | 3.1.2 | −1 |
| `archived_records` | 3.1.3 | −1 |
| `device_connections` | 3.2 | −1 |
| `partner_organizations` | 3.3 | −1 |
| **Total** | | **−5 of the −11 programme target** |

Plus: view `nearby_support_interactions` dropped, `care_facilities.partner_id` dropped, four
`direct_conversations` legacy read columns dropped, `health_observations.device_connection_id`
dropped.

## Scripts

| File | Kind | When |
| --- | --- | --- |
| `00_preflight_dependencies.sql` | read-only | Immediately before every contract migration. Output goes in the change ticket. |
| `01_data_gates.sql` | read-only | Before R0b, again after R0b, again before contract. Eight gates, each with an expected result. |
| `02_r0b_reconciliation.sql` | **mutating templates** | R0b and R3b only, block by block, never as a whole file. |
| `03_r9_checklist_gate.sql` | read-only | Superseded: promoted to Flyway migration `V20260806176000`. Kept for ad-hoc re-checking. |
| `R11b_RUNBOOK.md` | procedure | The step-by-step cutover for the notification queue, including the correction that the feature is currently switched off and the planner/worker code cutover is still unwritten. |
| `06_contract_drop_source_tables.sql` | superseded | Promoted to `V20260807090000__contract_drop_consolidated_source_tables.sql` on 2026-08-07 once `05` reported OBSERVATION COMPLETE. Kept for reference. |
| `05_r11b_observation.sql` | read-only | **Step 4.** Run repeatedly during the R11b observation window; prints the §7.5 job gates and a verdict of OBSERVING / OBSERVATION COMPLETE / ROLL BACK. |
| `04_readiness_check.sql` | read-only | **The one command for the two remaining blockers.** Prints PASS/FAIL/INFO per gate for R9 and R11b plus an overall verdict. Run it twice ≥5 min apart for the queue-stability gate. |

`04_readiness_check.sql` is exercised by
`ConsolidationReadinessScriptEmbeddedPostgresTest`, which runs it against a real PostgreSQL
instance and asserts the classification. R9's gate now asserts `PASS`; the only gate still
needing a human is R11b's queue-stability comparison, which no single run can answer.

## Gate → release mapping

| Gate | Expectation | Blocks |
| --- | --- | --- |
| 1 — pending deletion requests | `0` | R1b |
| 2 — pending lock appeals | `0` | R2b |
| 3 — `users.role = 'PARTNER'` | `0` | R3c |
| 4 — device provenance | `linked_observations = 0` **or** fully backfilled with no leaked credential | R4c |
| 5 — `care_facilities.partner_id` | `0` | R5b, archive drop |
| 6 — chat legacy read state | `legacy_ahead_of_cursor = 0` | R5c |
| 7 — nearby view rows | `0` | R4b |
| 8 — retained objects present | 19 relations + `users.settings_jsonb` | every contract migration |

## Static exit-gate scans

Run from `05_Development`. Classification follows plan §9 — zero-required vs. allowlisted.

```powershell
rg -n "AccountDeletionRequest|account_deletion_requests" --glob '!**/db/migration*/**' .
rg -n "AccountLockAppeal|account_lock_appeals|lock-appeals" --glob '!**/db/migration*/**' .
rg -n "PartnerOrganization|partner_organizations|PARTNER" --glob '!**/db/migration*/**' .
rg -n "device_connections|HealthDeviceConnection|DeviceConnection" --glob '!**/db/migration*/**' .
rg -n "nearby_support_interactions|NearbySupport|nearbycare" --glob '!**/db/migration*/**' .
rg -n "mother_last_read_at|expert_last_read_at" --glob '!**/db/migration*/**' .
```

### Allowlist (plan §9 — each entry needs file, symbol and reason)

Verified against `HEAD` after the retirement releases: these are the **only** places a
retired name still appears in application source or tests.

| File | Symbol | Reason |
| --- | --- | --- |
| `CareBridgeAPI/.../audit/entity/AuditAction.java` | `PARTNER_PROFILE_*`, `PARTNER_SERVICE_*`, `PARTNER_CAMPAIGN_*`, `PARTNER_CONTENT_*` | Historical `audit_events` rows still carry these values and must keep deserializing. V3/plan §4.4 defer removal to a separate migration gated on `distinct audit value count = 0`. Nothing writes them. |
| `CareBridgeWebApp/.../notification/pages/NotificationCenterPage.tsx` | `case 'PARTNER'` in label/badge/icon switches | Renders historical PARTNER-typed notifications. The **filter option** that let an admin request them was removed, so no new ones are produced. |
| `CareBridgeAPI/.../migration/ConsolidationContractEmbeddedPostgresTest.java` | all retired table/column names | Asserts each one is **absent**; the literals are the assertion. |
| `CareBridgeAPI/.../migration/Postgresql18CanonicalSchemaIntegrationTest.java` | all retired table/view names | Same — proves the fresh-bootstrap schema no longer contains them. |
| `CareBridgeAPI/.../security/CanonicalRoleSchemaIntegrationTest.java` | string `PARTNER` | Asserts `users_role_check` rejects the value. |
| `CareBridgeMobileApp/test/core/network/account_block_parser_test.dart` | `appealToken`, `appealStatus` | Feeds stale appeal metadata to the parser to prove it is ignored, not honoured. |
| `CareBridgeWebApp/.../auth/pages/BlockedAccountPage.test.tsx` | `appealToken`, `appealStatus` | Same negative test on the web side. |
| `CareBridgeAPI/.../DirectConversationServiceImplReadRaceIntegrationTest.java` | `expert_last_read_at` (comment only) | Explains why the assertion now reads the cursor table. |
| `CareBridgeMobileApp/lib/features/auth/services/auth_service.dart` | `account_deletion_requests` (comment only) | Records why `requestAccountDeletion()` is gone. |
| `CareBridgeAPI/src/main/resources/db/migration/**`, `src/test/resources/db/migration-legacy/**` | all retired names | Applied migration history is immutable (plan §2.6). The two new `V20260806*` contract migrations name them because they drop them. |
| `05_Development/Database/**`, `08_References/**` | all retired names | Audit and design documents describing the pre-consolidation state. |
| `CareBridgeMobileApp/lib/features/health/models/health_metric_model.dart` | `SourceType.device` | Historical observations keep device provenance; plan §4.3 retains this enum value. |

Anything outside this allowlist that still names a retired object fails the gate.

## Support-contact assumption (needs release-owner confirmation)

Retiring the appeal workflow leaves customer support as a locked user's only route back,
but the codebase had no support contact anywhere. Both clients now read one from
configuration, defaulting to `support@carebridge.dev` (matching the existing
`*@carebridge.dev` convention):

- Web — `src/shared/config/support.ts`, env `VITE_SUPPORT_EMAIL` / `VITE_SUPPORT_PHONE`
- Mobile — `lib/core/constants/support_contact.dart`, `--dart-define=SUPPORT_EMAIL=…` / `SUPPORT_PHONE`

**Set these to a real, monitored address before release.** The default is a placeholder,
not a working inbox.

## Persistence consolidation (R6 → R11a) — expand + backfill + cutover done

Pattern here is *DB additive expand → backfill/reconcile → target-only read → observe →
contract*, not the feature-retirement pattern above. In every case the source table still
exists **and still receives writes**, because plan §2.7 forbids target-only writing while a
rollback would read the source. Those mirror writes are removed at R12, before the drop.

| Wave | Migration | Source (kept as rollback path) | Target |
| --- | --- | --- | --- |
| R6 | `V20260806120000` | `reminder_schedule_times` | `reminder_schedules.local_times time[]` |
| R7 | `V20260806130000` | `appointment_notification_rules` | `appointment_notification_configs.rules_jsonb` |
| R8 | `V20260806140000` | `safety_configs` | 8 typed columns on `users` |
| R10 | `V20260806145000` + `V20260806150000` | `consultation_sessions` | session fields on `consultation_bookings` |
| R11a | `V20260806160000` | `reminder_schedule_jobs`, `appointment_notification_jobs` | `notification_jobs` (typed-polymorphic) |

Each migration carries its own reconciliation gate that raises `R<n>_…` and aborts rather
than completing a partial move. Collection rules live in `IMMUTABLE` validator functions
because PostgreSQL `CHECK` cannot contain a subquery:
`carebridge_validate_reminder_local_times`, `carebridge_validate_appointment_rules`.

### Decisions taken during implementation

| Decision | Why |
| --- | --- |
| **R7 backfill does not bump `config_revision`** | Jobs snapshot the revision they were materialised from. Bumping during a representation-only backfill would make every in-flight PENDING job look obsolete to `cancelObsoleteRevisions` and cancel valid notifications. V3 §3.7's requirement is atomicity between rules and revision, which the application path still honours. |
| **`SafetyConfigResponse.id` now equals `userId`** | The config is 1:1 with the user, so there is no separate `safety_config_id` any more. The mobile model parses `id` as `String?`, so no parser breaks. |
| **`occurrence_generation` stays out of the appointment identity** | `ReminderOccurrenceIdGenerationContractTest` proves occurrence-ID v2 folds the generation into `occurrence_id`. **If that test ever fails, `notification_jobs_appointment_identity_uk` is wrong** and must gain the column, per the V3 §3.8 fallback. |
| **Paid consultation bookings converted in their own migration** | V3 §3.11 requires a per-booking conversion and forbids waiving the zero-row gate. `V20260806145000` performs and logs it (`R10_PAID_BOOKING_CONVERTED …`) so the discarded price data survives in the migration log; the gate in the expand migration stays an independent check. |

### R11b is blocked on an operational step, not on code

The consolidated queue exists, is backfilled, and has a tested common repository (R11a).
**R11b is still development work, not just an operational window** — the planners, workers
and the two processing services all still target `ReminderScheduleJobRepository` and
`AppointmentNotificationJobRepository`. Switching them to `NotificationJobRepository` is the
substance of R11b.

The operational half is simpler than plan §4.9 assumes, because the notification feature is
currently **switched off** in this deployment: `REMINDER_SCHEDULE_NOTIFICATION_ENABLED` and
`APPOINTMENT_NOTIFICATION_ENABLED` both default to `false` and `.env` overrides neither. The
221 / 10 jobs are a static backlog and `PROCESSING = 0` holds trivially, so with the flags
off there is no delta to race. See `R11b_RUNBOOK.md` for both paths.

```sql
-- R11b readiness, run against the live database
SELECT 'reminder_schedule_jobs' AS queue, status, count(*)
  FROM reminder_schedule_jobs GROUP BY status
UNION ALL
SELECT 'appointment_notification_jobs', status, count(*)
  FROM appointment_notification_jobs GROUP BY status;
-- required before the final delta: no PROCESSING rows, counts stable across 5 minutes
```

## R9 (legacy checklist) — resolved and cut over

The backfill for `preparation_checklist_items` was **already written and already ran**,
inlined in `V20260731070000`, keeping each legacy `checklist_item_id` as the v2
`checklist_task_instance_id`. R9 was therefore never a backfill task — only the removal of
the compatibility read.

`03_r9_checklist_gate.sql` showed two legacy rows had never reached v2, which meant the
`putIfAbsent` merge in `UserChecklistItemController#listItems` was the only thing keeping
them visible. Inspecting them on the linked database settled it: two seeded demo items for
the seeded mother `10000000-…-0004`, never completed, no inbound foreign key, against a
healthy v2 holding 144 tasks / 33 instances. No real user data.

Resolved via option 2 (retire as demo data, Product decision):

| Migration | What it does |
| --- | --- |
| `V20260806175000` | Logs each row's full payload (`R9_LEGACY_ITEM_RETIRED …`), refuses if any row carries completion state, then deletes **the two inspected ids only** |
| `V20260806176000` | The gate, promoted from script to migration now that it passes. Anything unmigrated outside the inspected set fails here rather than being swept up |

Then the legacy read merge was removed: `listItems` now returns checklist v2 alone.
`UserChecklistItem`, its repository and its service remain in the tree as uncalled rollback
artifacts until R12.

The deletion is deliberately ID-specific. A blanket "delete anything unmigrated" would
destroy rows nobody has inspected; the gate exists so a human sees those instead.

### A production 500 found on the way

Making the v2 path the sole source surfaced a pre-existing NPE in
`UserCreatedChecklistTaskService#isVisibleTemplate`: a `USER_CREATED` instance has a null
`templateVersionId`, `templatesByVersion` returns an immutable `Map.of()` when there is
nothing to look up, and `Map.of().get(null)` throws. `GET /api/v1/user-checklist-items`
returned **500 for any user with a self-created checklist item**, before this programme and
independently of it. Confirmed pre-existing by reverting the controller to `HEAD` and
watching the same failure. Fixed by not routing a null version through the map.

## Production state — 11 migrations APPLIED 2026-08-07

Applied to the linked project `CareBridge` (`wqsunmakzdaxwyknegkq`, PostgreSQL 17.6) with
Flyway, using the same locations/out-of-order/validate settings as `application.yaml`, so
`flyway_schema_history` reads exactly as a normal deploy would leave it. Now at
**v20260806176000, 36 applied, 0 failed**.

**Release Owner waiver, recorded:** the PITR/backup precondition of V3 §7.1 was explicitly
waived on 2026-08-07 on the grounds that the database holds test data only and its loss is
acceptable. At the time of the run `pitr_enabled` was `false` and `backups` was empty, so
the dropped objects are **not recoverable**. This waiver applies to this database only and
must not be carried forward to an environment holding real user data.

### What the run did

| Migration | Effect on production |
| --- | --- |
| `…095000` | Retired the one `PARTNER` user (`partner@carebridge.dev`, dev seed, 0 live sessions): credentials revoked, role cleared, deactivated. Logged as `R3B_PARTNER_USER_RETIRED`. |
| `…100000` | Dropped `partner_organizations`, the `nearby_support_interactions` view + trigger + function, and the four `direct_conversations` legacy read columns. Rebuilt `users_role_check` without `PARTNER`. |
| `…110000` | Dropped `account_deletion_requests`, `account_lock_appeals`, `device_connections`, `archived_records`, `care_facilities.partner_id`, `health_observations.device_connection_id`. |
| `…120000`–`…160000` | Expanded and backfilled `local_times`, `rules_jsonb`, the safety columns, the booking session fields, and `notification_jobs`. |
| `…145000` | Converted the one paid consultation booking (200000 VND snapshot logged before removal). |
| `…175000` / `…176000` | Retired the two demo checklist rows, then the gate reported `R9_BACKFILL_COMPLETE`. |

### Verified after the run

- Six retired relations confirmed **DROPPED**; `device_tokens`, `reminder_occurrence_aliases`,
  `direct_conversation_read_cursors`, `growth_measurements`, `safety_events` and
  `users.settings_jsonb` all **survived** (plan §4.14 negative scope).
- `notification_jobs` holds **231 rows = 221 + 10**, with the source status split preserved
  (204 PENDING / 8 FAILED / 3 SENT / 6 SUPPRESSED schedule; 2 SENT / 8 SUPPRESSED appointment).
- `04_readiness_check.sql` verdict: **ALL GATES PASS**.

### Still to do — needs a deploy, not a database change

The application running against this database is still the **old build**. Its entities map
columns that no longer exist (`device_connection_id`, the chat read columns,
`care_facilities.partner_id`) and its `Role` enum still contains `PARTNER`. **It will fail
`ddl-auto: validate` at startup until the current build is deployed.** Deploy before
restarting the service.

After deploying, R11b's remaining work is Step 4 of `R11b_RUNBOOK.md`: enable
`REMINDER_SCHEDULE_NOTIFICATION_ENABLED` / `APPOINTMENT_NOTIFICATION_ENABLED` and observe
two planner cycles, one retry and one stale-lock requeue. There is no final delta to run —
rows 4 and 5 of the readiness check are already `PASS`.

## R12 — source artifacts removed 2026-08-07

Code-only. No table was dropped; the queue contract migration is still pending the
observation window. What went:

| Removed | Was the source for |
| --- | --- |
| `ReminderScheduleJob` + repository | R11 |
| `AppointmentNotificationJob` + repository | R11 |
| `ReminderScheduleTime` + repository | R6 |
| `AppointmentNotificationRule` + repository | R7 |
| `ConsultationSession` + repository | R10 (already dead code — no runtime caller) |
| `ISafetyConfigRepository` | R8 |

Both mirror writes went with them: `ReminderScheduleServiceImpl#writeTimes` no longer
touches the child table, and `AppointmentNotificationScheduleService#mirrorRulesToSourceTable`
is gone. `SafetyMonitoringConfig` stopped being an `@Entity` and is now the plain domain
value object `SafetyConfigStore` projects from the `users` columns — the outcome plan §4.10
sanctioned, keeping the DTOs, mapper and `SafetyConfigChanged` event unchanged.

**Static gate: 11/11 retired symbols at zero references** across `main` and `test`. The only
surviving mentions of the retired table names are comments explaining where the data moved,
plus `ConsolidationContractEmbeddedPostgresTest`, which names them in order to assert they
are absent.

One test was quietly wrong after R8 and is now fixed:
`SafetyMonitoringConcurrencyPostgresIntegrationTest` seeded `safety_configs` directly, a
table the application had stopped reading, so its fixture no longer influenced the behaviour
under test. It now seeds the `users` columns. It needs Docker (`AbstractPostgresIntegrationTest`
uses raw Testcontainers with no `disabledWithoutDocker` guard), so it was compiled but not
executed here.

### Rollback cost, now paid

The source entities were the cheap rollback path. With them gone, reverting the queue
cutover means redeploying the previous build rather than flipping a flag. Accepted
deliberately: the tables still hold their data, so a redeploy of the old build still finds a
correct queue.

### Still pending

`reminder_schedule_jobs`, `appointment_notification_jobs`, `reminder_schedule_times`,
`appointment_notification_rules`, `safety_configs`, `consultation_sessions` and
`preparation_checklist_items` all still **exist** in the database, unread. Dropping them is
the queue/persistence contract migration, gated on `05_r11b_observation.sql` reaching
`OBSERVATION COMPLETE`.

The legacy checklist source (`UserChecklistItem`, its repository, `IUserChecklistItemService`
and `UserChecklistItemServiceImpl`) was **not** removed: it is referenced by eight test
classes whose value has to be judged individually rather than deleted wholesale. The
controller no longer calls it, so it is already dead at runtime.

## Legacy checklist source — analysis for the removal decision

R12 stopped short of `UserChecklistItem`, its repository, `IUserChecklistItemService` and
`UserChecklistItemServiceImpl`. The controller no longer calls them, so they are already dead
at runtime, but eight test classes reference them and their value differs case by case.
Classified so the decision is a review rather than an investigation:

| Test | References | Verdict |
| --- | --- | --- |
| `UserChecklistItemServiceImplTest` | legacy service impl | **Dies with the impl.** Its one test covers `listItems` on the legacy read path, which nothing invokes. |
| `UserChecklistItemRepositoryIntegrationTest` | legacy repository | **Dies with the repository.** It asserts the compatibility repo exposed reads only — a guard for a window that has closed. |
| `ChecklistImportBoundaryTest` | legacy repo + impl | **Dies with the impl.** Constructs `UserChecklistItemServiceImpl` directly. |
| `ChecklistServiceTest` | legacy service | **Split.** `everyLegacyMutationFailsClosedBeforePersistence` is a live contract (retired routes must keep failing closed) and should be kept, retargeted at the controller. `legacyReadRemainsReadOnlyDuringCutover` is moot. |
| `UserChecklistItemSystemTaskMutationTest` | legacy types as fixtures | **Keep, retarget.** Asserts system tasks reject mutation — still true and still worth testing on the v2 path. |
| `ChecklistImportControllerTest` | `UserChecklistItemController` + service as `@MockitoBean` | **Keep.** Tests the controller that stays; only the mock declaration needs removing. |
| `Story69ContentSecurityTest` | same | **Keep.** Same situation — security tests for a live controller. |
| `ChecklistTemplateAdminIntegrationTest`, `ChecklistImportConcurrencyPostgresTest` | mixed | **Review individually** — they span admin template import, a different feature. |

So the removal is roughly: delete three test classes, split one, edit two mock declarations,
review two. Once done, `preparation_checklist_items` can join the contract drop — until then
it must stay, because Hibernate would fail startup validation against a missing table for a
still-mapped entity.

## Observation closed 2026-08-07 — contract migration staged, NOT yet applied

`05_r11b_observation.sql` on the linked project: **all eleven gates PASS**,
verdict `OBSERVATION COMPLETE`.

| Evidence | Measured |
| --- | --- |
| Jobs planned since cutover | 216, across **3 distinct planner cycles** |
| Retry path | 1 |
| Stale-lock requeue | 1 |
| Source queues | still frozen at 221 / 10 — the new code never wrote to them |
| Duplicate identity / notification record | 0 / 0 |
| PENDING | 204 → 420 |

`V20260807090000__contract_drop_consolidated_source_tables.sql` is now in
`db/migration` and verified on a full chain from an empty database (13/13). It drops
`reminder_schedule_jobs`, `appointment_notification_jobs`, `reminder_schedule_times`,
`appointment_notification_rules`, `safety_configs` and `consultation_sessions`, each behind
a gate that re-derives its own precondition.

**It has not been applied to production.** The next service start applies it automatically —
that is the normal path and needs no separate tooling. There is no restore on this database,
so nothing about that step is reversible.

`preparation_checklist_items` is **not** in the drop list. The R12 test-side cleanup landed
(three legacy test classes gone, the rest retargeted, 54/54 green), but the four main classes
— `UserChecklistItem`, its repository, `IUserChecklistItemService`, `UserChecklistItemServiceImpl`
— still exist and still map the table. Dropping it now would fail `ddl-auto: validate` at
startup. Remove those four, then add the table to a follow-up contract migration.

## Deliberately untouched (plan §4.14 negative scope)

`users.settings_jsonb`, `reminder_occurrence_aliases`, `direct_conversation_read_cursors`,
`device_tokens` and push notifications, `growth_measurements`, `safety_events`, `audit_events`,
`auth_sessions`, `auth_challenges`, `care_groups`, `care_group_members`, `vaccination_records`,
`vaccination_schedules`, `development_milestones`, `data_permissions`, `moderation_cases`,
`triage_sessions`, `triage_session_evidence`, `knowledge_source_reviews`, `content_item_topics`,
`content_item_sources`, `professional_specialties`, `flyway_schema_history`.

The care-facility map/search feature stays: retiring nearby **support requests** does not retire
nearby **facilities** (plan §4.6).
