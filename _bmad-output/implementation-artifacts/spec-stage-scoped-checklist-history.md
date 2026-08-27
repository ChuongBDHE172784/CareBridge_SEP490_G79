---
title: 'Stage-scoped current checklist and separate checklist history'
type: 'bugfix'
created: '2026-08-01'
status: 'done'
baseline_commit: '096f46bf70fe136debbc291f341083b47523f075'
context:
  - '{project-root}/_bmad-output/project-context.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** After a Mother lifecycle or Baby age-window change A → B, Today returns both because it reads every non-cancelled instance without validating canonical stage/window. Checklist history has no separate surface.

**Approach:** Reconcile a one-way parent history marker from server-owned Mother/Baby context, return only B through Today, and expose unchanged A children through a paginated read-only API/screen.

## Boundaries & Constraints

**Always:** Scope automatic history to `MOTHER` + `SYSTEM_TEMPLATE`. Current means the canonical Journey stage and exact window, or an owned active Baby plus exact `BIRTH_DATE` window. Mark only the A parent historical; preserve every child status/timestamp and reject stale actions. History is owner-only, newest-first, read-only, paginated, and separate from Journey history. Reconciliation/B materialization are idempotent and lock context → parent → children.

**Ask First:** Changing FAMILY/CareTask or `USER_CREATED`; restoring a `BABY_CARE` DB stage; adding a fourth checklist table; deleting/rekeying instances or ledger rows.

**Never:** Filter in Flutter, accept client stage, overwrite child evidence, edit applied migrations, leak ownership, or merge with Journey history.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Mother A → B | A has mixed statuses; Journey is B | Today contains eligible B only; A appears unchanged in history | No B template means empty current data, never stale A |
| Baby A → B | Active owned Baby crosses a `BIRTH_DATE` window | B is current; A is history labelled `BABY_CARE` | Missing birth date fails closed |
| New pregnancy | Same Journey ID, new pregnancy dates | Old windows stay historical; new window appears once | Refresh remains duplicate-free |
| Stale action | Complete/reopen targets A | No mutation or success audit | Neutral `404 TASK_NOT_FOUND` |
| Unauthorized | FAMILY, guest, or another Mother requests history | No history is disclosed | Existing `401/403`; owner mismatch is neutral |

</frozen-after-approval>

## Code Map

- API root: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/`.
- Migration root: `05_Development/CareBridgeAPI/src/main/resources/db/migration/` — forward-only schema change.
- Mobile root: `05_Development/CareBridgeMobileApp/lib/`; test roots: API `src/test/java/com/carebridge/backend/checklist/`, Mobile `test/features/`.

## Tasks & Acceptance

**Execution:**
- [x] Migration root `V20260801120000__add_stage_scoped_checklist_history.sql`; API root `entity/ChecklistInstance.java`, `repository/ChecklistInstanceRepository.java` — add paired history fields, constraints/partial indexes, and retain three tables.
- [x] API root `distribution/ChecklistCurrentScopePolicy.java`, `ChecklistHistoryReconciliationService.java` — evaluate Journey/exact window and Baby `BIRTH_DATE`; mark stale parents once without changing children.
- [x] API root `today/provider/ChecklistTodayTaskProvider.java`, `ChecklistTaskActionHandler.java`, `today/service/UnifiedTodayTaskServiceImpl.java` — reconcile before projection, return B only, deny actions on A.
- [x] API root `history/controller/ChecklistHistoryController.java`, `history/service/ChecklistHistoryService.java`, `history/dto/` — implement MOTHER-only `GET /api/v1/checklists/history?page&size&targetSubject`, grouped DTOs, batch loading, pagination, isolation.
- [x] Mobile root `features/checklist/{models,services,screens}/`, `core/routes/app_router.dart`, `features/home/screens/mother_home_screen.dart`, `features/reminder/screens/today_tasks_screen.dart` — add `/checklists/history`, Mother/Baby filters, resilient states, and non-Journey entries.
- [x] Both test roots — add unit/widget/PostgreSQL cases and extend Today/action/router tests for the matrix, same-ID pregnancy, concurrency, accessibility, guards, and A → B refresh.

**Acceptance Criteria:**
- Given a Mother/Baby transition, when Today refreshes, then no prior window is returned and eligible B materializes at most once.
- Given mixed A states, when history loads, then every child retains its status/timestamps and exposes no mutation control.
- Given stale IDs or cross-account calls, when endpoints run, then no data, mutation, or success audit occurs.
- Given migration/regressions, when catalog checks run, then three tables and FAMILY/`USER_CREATED` contracts remain unchanged.

## Spec Change Log

## Design Notes

`BABY_CARE` remains a label: content stays `POSTPARTUM`; `BABY` context plus `BIRTH_DATE` defines Baby windows. A parent marker preserves workflow evidence and prevents A from reviving.

## Verification

**Commands:**
- `mvn -q -Dtest="*Checklist*History*,ChecklistTodayTaskProviderBatchLoadingTest,ChecklistTask*Action*Test,ChecklistRequestMaterializationEmbeddedPostgresTest" test` — expected: focused backend and PostgreSQL cases pass.
- `flutter test test/features/checklist test/features/home/mother_home_screen_test.dart test/features/reminder/today_tasks_navigation_contract_test.dart` — expected: history UX/navigation and A → B refresh pass.
- `dart analyze lib test` and `git diff --check` — expected: no diagnostics or whitespace errors.
## Suggested Review Order

**Lifecycle current/history boundary**

- Parent marker is schema-only; child evidence stays untouched.
  [`V20260801120000__add_stage_scoped_checklist_history.sql:6`](../../05_Development/CareBridgeAPI/src/main/resources/db/migration/V20260801120000__add_stage_scoped_checklist_history.sql#L6)

- Entity fields mirror migration for JPA validation.
  [`ChecklistInstance.java:105`](../../05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/entity/ChecklistInstance.java#L105)

- Scope policy defines Mother-only, system-template current checks.
  [`ChecklistCurrentScopePolicy.java:32`](../../05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/distribution/ChecklistCurrentScopePolicy.java#L32)

- Reconciliation locks and marks obsolete current parents once.
  [`ChecklistHistoryReconciliationService.java:51`](../../05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/distribution/ChecklistHistoryReconciliationService.java#L51)

- Distribution routes Mother to history, non-Mother to legacy cancellation.
  [`ChecklistDistributionService.java:267`](../../05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/distribution/ChecklistDistributionService.java#L267)

- FAMILY regression preserves old cancellation and no history marker.
  [`ChecklistDistributionServiceTest.java:372`](../../05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/checklist/distribution/ChecklistDistributionServiceTest.java#L372)

**Today/action contract**

- Today projects only non-historical checklist parents.
  [`ChecklistTodayTaskProvider.java:42`](../../05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/today/provider/ChecklistTodayTaskProvider.java#L42)

- Today service reconciles before projecting task sections.
  [`UnifiedTodayTaskServiceImpl.java:88`](../../05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/today/service/UnifiedTodayTaskServiceImpl.java#L88)

- Stale checklist actions fail as neutral not-found.
  [`ChecklistTaskActionHandler.java:222`](../../05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/today/provider/ChecklistTaskActionHandler.java#L222)

**Dedicated history API and screen**

- Mother-only endpoint exposes separate paginated checklist history.
  [`ChecklistHistoryController.java:25`](../../05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/history/controller/ChecklistHistoryController.java#L25)

- Service groups historical tasks and null-safes legacy labels.
  [`ChecklistHistoryService.java:85`](../../05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/history/service/ChecklistHistoryService.java#L85)

- Mobile service calls the dedicated history endpoint.
  [`checklist_history_service.dart:23`](../../05_Development/CareBridgeMobileApp/lib/features/checklist/services/checklist_history_service.dart#L23)

- Screen ignores stale filter responses and remains read-only.
  [`checklist_history_screen.dart:49`](../../05_Development/CareBridgeMobileApp/lib/features/checklist/screens/checklist_history_screen.dart#L49)

- Mother Home links history without merging Journey history.
  [`mother_home_screen.dart:808`](../../05_Development/CareBridgeMobileApp/lib/features/home/screens/mother_home_screen.dart#L808)

- Today Tasks links to the separate history screen.
  [`today_tasks_screen.dart:48`](../../05_Development/CareBridgeMobileApp/lib/features/reminder/screens/today_tasks_screen.dart#L48)

**Regression coverage**

- Migration and reconciliation tests cover schema plus lifecycle cases.
  [`ChecklistStageScopedHistoryMigrationContractTest.java:17`](../../05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/checklist/distribution/ChecklistStageScopedHistoryMigrationContractTest.java#L17)

- Widget test locks grouped history and filter race behavior.
  [`checklist_history_screen_test.dart:76`](../../05_Development/CareBridgeMobileApp/test/features/checklist/checklist_history_screen_test.dart#L76)
