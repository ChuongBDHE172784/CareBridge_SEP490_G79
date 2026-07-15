# UC-242 Complete Baby Care Workflow — Coding Handoff

| Field | Value |
|---|---|
| Status | Direct implementation in progress |
| Approved specification | TDS v0.6 + Test-Spec v0.4 |
| Approval recorded | 2026-07-15 |
| Delivery order | Sprint 1 → Sprint 2 → Sprint 3 |
| Method | Strict Red → Green → Refactor |
| Current authorized increment | Sprint 1 only |
| Implementation status | Sprint 1 in progress; targeted backend/mobile verification passed |
| Latest security fix | Journal detail/delete now reject a path baby ID that differs from the stored log baby ID |
| Latest contract fix | Missing growth measurement dates fail fast; source and non-future date are required for new measurements |
| Production release gates | Tech Lead, QA Lead, and DPO sign-offs remain pending |

## Source Artifacts

1. `UC242 - Complete Baby Care Workflow_TDS.md`
2. `UC242 - Complete Baby Care Workflow_Test-Spec.md`
3. `03_Design/ActivityDiagram/CareBridge-Main-Workflows.drawio`, MF-03 page — evidence only.
4. Detailed Scope 121UC SRS, FR/UC-032…045 — requirement oracle.

Legacy UC specs and current source code are implementation evidence only. They cannot override the approved UC242 constraints.

## Non-Negotiable Constraints

1. Do not write production behavior before valid Red evidence exists.
2. Use persisted backend data in production; remove no mock until its replacement test is Red and the real path is Green.
3. Every baby object requires owner or accepted, unexpired, explicitly scoped caregiver permission.
4. JWT subject is the caller identity; never accept owner identity from request input.
5. Do not diagnose, prescribe, assess development, or state/imply that growth is healthy, normal, abnormal, or “developing well.”
6. Night mode is Flutter presentation state only and is never a daily-log type.
7. Keep the canonical vaccination route `/api/v1/vaccination/babies/{babyId}/...`.
8. Do not edit applied Flyway migrations.
9. Preserve unrelated dirty-worktree changes.

## Sprint 1 — Access Foundation, Daily Journal, Growth

### Sprint Goal

Make current journal and growth branches production-backed, baby-scoped, permission-correct, non-diagnostic, and fully reachable from Flutter. Sprint 2 must not start until the Sprint 1 gate passes.

### Red Phase

Create focused tests first for the following approved Test-Spec cases:

- `UC242-TC-SEC-001` — owner/accepted/pending/revoked/expired/unrelated decision table.
- `UC242-TC-SEC-002` — cross-baby IDOR for daily logs and growth records.
- `UC242-TC-SEC-003` — request owner-ID override cannot replace JWT subject.
- `UC242-TC-SEC-004` — permission revocation before write denies and clears stale client state.
- `UC242-TC-003` — add supported daily log.
- `UC242-TC-004` — update/delete only permitted baby-scoped daily log.
- `UC242-TC-005` — exact 24-hour and 7-day summary boundaries.
- `UC242-TC-009` — growth source/time/value validation.
- `UC242-TC-010` — baby-scoped growth update/delete and audit.
- `UC242-TC-011` — persisted chart/history and health-boundary copy.
- Sprint 1 Flutter contract/widget tests for create/list/detail/summary, growth history/chart routing, all UI states, and active-baby response isolation.

Red evidence must record the command, failing test names, failure reason, and worktree/commit state. Compile/fixture/environment failures do not satisfy the Red gate. Existing behavior that is already correct should be protected by characterization tests; do not force it to fail artificially.

### Backend Change Targets

Inspect impact with code-review-graph before editing. Expected targets include:

- `baby/policy/BabyAccessPolicy.java`
- family membership/permission repositories and enums used by that policy
- `carejourney/controller/BabyDailyLogController.java`
- `carejourney/controller/BabyLogSummaryController.java`
- `carejourney/service/impl/BabyDailyLogServiceImpl.java`
- `carejourney/controller/GrowthMeasurementController.java`
- `carejourney/controller/GrowthChartController.java`
- `carejourney/service/impl/GrowthServiceImpl.java`
- associated repositories, DTOs, exception mapping, and audit calls

Required behavior:

1. Correct the baby-to-owner/care-group relation; do not query a care-group using a baby ID unless schema evidence explicitly proves that relation.
2. Centralize view/write decisions in `BabyAccessPolicy`; controller role checks are only coarse gates.
3. Permit canonical roles `MOTHER` and `FAMILY` at controller level where relevant, then enforce the exact domain permission in the service.
4. Scope child-record reads/writes by both `babyId` and record ID.
5. Preserve existing response envelopes and canonical routes unless an approved contract test requires a compatible addition.
6. Daily summary uses a frozen/injected clock and documented UTC half-open intervals.
7. Growth chart/history returns entered facts and provenance only.

### Flutter Change Targets

Before UI edits, follow `ui-skill-system` and reuse existing design tokens/components. Expected targets include:

- `features/baby/services/baby_log_service.dart`
- baby daily-log create/list/detail/summary screens and routes
- `features/healthRecords/services/growth_measurement_service.dart`
- growth history/chart/detail screens and routes
- active-baby state/provider and shared loading/error/empty/permission components

Required behavior:

1. Add the missing API-backed daily-log create/list flow without production fake data.
2. Ensure summary requests use `/api/v1/babies/{babyId}/daily-logs/summary` and render persisted response values.
3. Complete growth history/chart navigation and serializers.
4. Remove or replace unsafe “developing well/WHO” conclusions with source-labelled neutral context and a qualified-provider notice.
5. On active-baby switch, cancel or discard stale responses from the previous baby.
6. Support loading, empty, error/retry, permission-denied, and network-failure states; night mode changes theme only.
7. Maintain WCAG 2.1 AA contrast, semantics, text scaling, and at least 48dp touch targets.

### Green Phase

Implement the minimum behavior needed to satisfy each valid Red test. Run targeted tests after each behavior, then the complete Sprint 1 backend and Flutter suites. Never mark a test Green without an actual passing command result.

### Refactor Phase

While tests remain Green:

- remove duplicated object-authorization logic;
- consolidate API/UI state mapping;
- keep DTOs separate from entities;
- remove unreachable production mock paths and forbidden health-boundary copy;
- scan for queries that fetch child records by ID without baby scope;
- run format/analyze/lint conventions for touched stacks.

### Sprint 1 Verification Commands

Run from the relevant project directory:

```powershell
.\mvnw.cmd test
.\mvnw.cmd clean package
flutter analyze
flutter test
```

Use narrower `-Dtest=...` or Flutter test-file commands during Red/Green iterations, but full commands above are mandatory before closing Sprint 1.

### Sprint 1 Exit Gate

- [x] Red evidence recorded for the summary observation-only boundary.
- [ ] All named Sprint 1 tests pass after implementation and refactor; security/contract cases remain.
- [ ] Existing backend regression suite passes; unrelated pre-existing integration failures remain.
- [x] Flutter test suite passes; analyzer reports only five pre-existing Community infos.
- [ ] No object-level authorization path relies on role alone.
- [ ] Cross-baby IDOR tests pass for journal and growth.
- [ ] No production mock/hard-coded journal or growth data remains reachable.
- [ ] No diagnostic/development conclusion remains in touched API or Flutter copy.
- [ ] Actual test evidence is truthfully synced to the Test-Spec tracker.
- [ ] Code review completed before starting Sprint 2.

## Later Sprints — Do Not Start Early

### Sprint 2

Milestone list/detail, vaccination mobile route alignment, composite baby-care overview, and tabbed Flutter hub. Governed by `UC242-TC-001/002/006…008/012…014`, contract and composite-isolation tests.

### Sprint 3

Notification idempotency migration/dispatcher, derived care timeline, appointment-preparation summary, safe deep link, and full E2E. Governed by `UC242-TC-015…017`, `SEC-005/006`, `INT-004`, and `E2E-001…003`.

## Required Implementation Report

At the end of Sprint 1, return:

1. created/modified files grouped by Backend, Mobile, Tests, Migration, and Specs;
2. Red evidence per test group;
3. targeted and full test commands with actual pass/fail counts;
4. unresolved defects/open decisions;
5. CASE 2.0, PDPA, authorization, and health-boundary verification;
6. confirmation that Sprint 2 has not started before the Sprint 1 gate.
