# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC22 — Canonical Mother Lifecycle and Transition History

**Document ID:** `CB-JOURNEY-TDD-006-01`  
**Version:** `1.0`  
**Date:** `2026-07-18`  
**Status:** `Implemented — Done (Story gate)`  
**Standard:** ISO/IEC/IEEE 29119-3:2021 structure adapted for CareBridge  
**Author:** `Codex — Test Architecture Support`  
**Reviewed by:** `User approval recorded in Codex session`  
**DPO Sign-off:** `[ ] Pending`  
**Approved by:** `[x] User — Project Approver, 2026-07-18`  
**Classification:** `Internal — Confidential`

**References:**

- `UC22 - Canonical Mother Lifecycle and Transition History_TDS.md`
- `_bmad-output/planning-artifacts/prd.md` — FR43
- `_bmad-output/planning-artifacts/epics.md` — Story 6.1
- `02_Requirements/SRS/3_Functional_Specification.md` — UC22/UC23
- `02_Requirements/SRS/3_Functional_Specification_Detailed_Scope_121UC.md` — aliases UC19/UC20
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql`

All tests use SYNTHETIC data. Production PII is prohibited.

---

## CHANGELOG

| Date | Author | Change |
| --- | --- | --- |
| 2026-07-18 | Codex — Test Architecture Support | Initial Story 6.1 risk-based Test-Spec. |
| 2026-07-18 | User — Project Approver | Approved Story 6.1 Test-Spec for implementation handoff. |
| 2026-07-18 | Codex — Implementation Support | Completed RED/GREEN implementation cycle; all 17 specified tests pass and Story 6.1 moved to review. |
| 2026-07-18 | Codex — Mobile Gap-Fix Support | Added and executed downstream Flutter regression tests and physical-device smoke verification for the failed mobile cases. |
| 2026-07-18 | Codex — Review Remediation | Closed 21 review findings; reran PostgreSQL, coverage, analyzer, targeted, and full Flutter gates; accepted the scoped backend baseline waiver. |

---

## TABLE OF CONTENTS

1. Module Information
2. Logic Issues Resolved
3. Test Design Specification
4. Test Case Specification
5. Red-Green-Refactor Tracker
6. Entry / Exit Criteria
7. Rollback Plan
8. CASE 2.0 Anti-Pattern Detection

---

## 1. Module Information

| Field | Value |
| --- | --- |
| **Feature / Gap ID** | `OV01-GAP-01 / Story 6.1` |
| **Module** | `journey — Canonical Mother Lifecycle` |
| **Source Spec** | `CB-JOURNEY-IMP-006-01` |
| **Priority** | P0 |
| **Sprint** | Open |
| **Milestone** | Epic 6 Wave 0 |
| **Data Classification** | Sensitive-PII |
| **Compliance Scope** | Project ownership, consent, audit, minimum-necessary, and no-real-PII test rules |
| **Upstream Dependencies** | Security context, users, AuditService, PostgreSQL/Flyway |
| **Downstream Consumers** | dashboard, postpartum, baby, reminders, health, content, safety |

### 1.1 AI Generation Context

| Field | Value |
| --- | --- |
| **AI Assisted?** | Yes |
| **Constraint Source** | `CB-JOURNEY-IMP-006-01 §3, §5, §6, §9, §16, §17` |
| **Constraints Injected** | canonical uniqueness, atomic history, ownership, optimistic concurrency, forward migration, BABY_CARE compatibility |
| **Model** | Codex |
| **Trust Level** | T3 — Red/Green evidence and human-approved review remediation complete |

---

## 2. Logic Issues Resolved

| ID | Source discrepancy | Current evidence | Test oracle |
| --- | --- | --- | --- |
| L1 | Current implementation permits one ACTIVE journey per type | repository/service scope duplicate check by type | One canonical ACTIVE maternal lifecycle per owner (`FR43`, Story 6.1 AC1) |
| L2 | Dashboard selects newest ACTIVE | repository `findFirst...OrderByCreatedAtDesc` | Canonical query returns the sole active maternal lifecycle |
| L3 | Updates overwrite dates/stage | no history table | Each accepted mutation writes exactly one immutable transition |
| L4 | No database concurrent-create protection | only application pre-check | Partial unique index makes one create win and one return 409 |
| L5 | No lost-update detection | no `@Version` | optimistic conflict returns `JOURNEY-017` |
| L6 | Legacy specs use UC22/23; approved 121-UC uses UC19/20 | conflicting repository artifacts | Use UC22/23 as implementation IDs and record aliases until Story 6.10 |
| L7 | Template requests V1 sync; project rule forbids editing applied migration | project context | Test the new forward migration; do not modify V1 |
| L8 | BABY_CARE exists in current enum/mobile | OV-01 maternal stages exclude it | Keep readable and outside canonical unique predicate until Stories 6.4/6.5 |

---

## 3. Test Design Specification

### TDS-01 — Scope

```text
Journey test scope
├── Domain/policy: transition matrix, provenance, canonical predicate
├── Service: create/update/history, ownership, audit, transaction behavior
├── Repository/migration: PostgreSQL uniqueness, backfill, constraints, indexes
├── Controller: request validation, auth, errors, pagination
└── Integration: concurrency and atomic current/history persistence
```

Mobile/Web tests were not part of the originally approved Story 6.1 backend scope. The approved downstream mobile gap-fix addendum is tracked separately below; Web remains outside this Test-Spec.

### TDS-02 — Test Basis

| Source | Items Derived |
| --- | --- |
| FR43 | canonical lifecycle and history |
| Story 6.1 AC1–AC5 | uniqueness, transition policy, history fields, migration, concurrency/ownership |
| BR-OWNERSHIP | owner-only access |
| BR-JOURNEY-01/02 | non-diagnostic dates and refresh after accepted changes |
| ADR-JOURNEY-006-01..03 | current/history ownership, concurrency, legacy compatibility |
| TDS §9/§10/§16 | API, errors, authorization |
| V1 + new migration | schema and persistence oracle |

### TDS-03 — Test Conditions and Coverage

| Condition ID | Condition | Level | Priority | Risk | Cases |
| --- | --- | --- | --- | --- | --- |
| `JRN-COND-001` | First canonical create succeeds and records history | Service/API integration | P0 | R-01 | 001, INT-001 |
| `JRN-COND-002` | Sequential and concurrent duplicate create rejected | Unit/PostgreSQL integration | P0 | R-01 | 002, INT-002 |
| `JRN-COND-003` | Allowed stage/date mutation succeeds | Unit/service | P1 | R-03 | 003 |
| `JRN-COND-004` | Invalid or terminal transition rejected without side effects | Unit/service | P0 | R-03 | 004 |
| `JRN-COND-005` | Current row, transition, audit/event remain consistent | Integration | P0 | R-02 | 005, INT-003 |
| `JRN-COND-006` | Migration backfill and duplicate preflight | Migration integration | P0 | R-04 | INT-004, INT-005 |
| `JRN-COND-007` | Authentication, role, ownership, IDOR | Controller/service | P0 | R-05 | SEC-001..003 |
| `JRN-COND-008` | Date provenance required; history is minimum-necessary | DTO/service/security | P1 | R-06 | 006, SEC-004 |
| `JRN-COND-009` | Optimistic update conflict returns stable 409 | Integration | P0 | R-07 | INT-006 |
| `JRN-COND-010` | BABY_CARE remains readable but not canonical | Repository/service | P1 | R-08 | 007 |

Priority denotes risk/business priority, not execution timing.

### TDS-04 — Risk Assessment and Techniques

| Risk | Category | Description | P | I | Score | Mitigation / test |
| --- | --- | --- | --- | --- | --- | --- |
| R-01 | DATA | Multiple canonical ACTIVE rows | 3 | 3 | 9 | DB unique/concurrency tests |
| R-02 | DATA | Current state commits without history | 2 | 3 | 6 | forced rollback/atomicity test |
| R-03 | BUS | Invalid lifecycle transition accepted | 2 | 3 | 6 | state-transition coverage |
| R-04 | OPS | Migration changes existing records or fails late | 2 | 3 | 6 | preflight/backfill tests |
| R-05 | SEC | IDOR exposes or changes journey history | 2 | 3 | 6 | owner/non-owner tests |
| R-06 | SEC/DATA | History stores unnecessary sensitive data | 2 | 3 | 6 | payload allow-list assertion |
| R-07 | DATA | Lost update under concurrency | 2 | 3 | 6 | optimistic locking test |
| R-08 | TECH | BABY_CARE compatibility breaks current mobile | 2 | 2 | 4 | compatibility regression |

Techniques: state-transition testing, decision tables, equivalence partitioning, boundary analysis, concurrency testing, fault injection, authorization abuse cases, and migration verification.

### TDS-05 — Test Data Requirements

| Fixture | Type | Synthetic value | Purpose |
| --- | --- | --- | --- |
| `FX-M01` | User | Mother owner UUID `...0101` | authorized owner |
| `FX-M02` | User | other Mother UUID `...0102` | IDOR |
| `FX-E01` | User | Expert UUID `...0201` | role denial |
| `FX-J01` | Journey | ACTIVE PRE_PREGNANCY version 0 | transition source |
| `FX-J02` | Journey | COMPLETED POSTPARTUM | terminal rejection |
| `FX-J03` | Journey | legacy ACTIVE BABY_CARE | compatibility |
| `FX-D01` | Dates | LMP `2026-06-01`, source SELF_REPORTED, confidence ESTIMATED | date mutation |
| `FX-C01` | Clock | fixed `2026-07-18T03:00:00Z` | deterministic timestamps |

Use a fresh factory instance for every test. PostgreSQL integration data is rolled back or truncated between cases.

---

## 4. Test Case Specification

All cases were implemented through the recorded RED/GREEN cycle. Each expected value cites the TDS/FR/AC oracle.

### Props Isolation Boilerplate

Planned `JourneyLifecycleTestFactory` returns new User/Journey/Request objects per call. No mutable entity is shared across tests.

### JRN-TC-001 — Create first canonical lifecycle

**Severity:** Critical  
**Feature:** `JourneyTransitionServiceImpl.createJourney`  
**Test File:** `src/test/java/com/carebridge/backend/journey/JourneyCanonicalLifecycleServiceTest.java`  
**Condition:** `JRN-COND-001`  
**Oracle:** FR43; Story 6.1 AC1/AC3; TDS §6 invariant 1–2

Preconditions: `FX-M01`; no canonical ACTIVE row.  
Steps: arrange valid pregnancy request with `FX-D01`; call create; capture saved current and transition.  
Expected: ACTIVE current row owned by caller, version 0; exactly one CREATED history row with actor/source/confidence/effective time; `JOURNEY_CREATED` audit emitted.  
Failure signature: missing history, wrong owner, or more than one save.  
**Status:** Passed — 2026-07-18

### JRN-TC-002 — Existing canonical lifecycle rejects another stage

**Severity:** Critical  
**Condition:** `JRN-COND-002`  
**Oracle:** Story 6.1 AC1; TDS ADR-JOURNEY-006-02

Preconditions: owner has ACTIVE PRE_PREGNANCY.  
Steps: request PREGNANCY create for same owner.  
Expected: HTTP/service conflict `JOURNEY-015`; no new current/history/audit record.  
**Status:** Passed — 2026-07-18

### JRN-TC-003 — Allowed date correction records previous/new values

**Severity:** High  
**Condition:** `JRN-COND-003`, `JRN-COND-005`  
**Oracle:** Story 6.1 AC2/AC3; TDS §5.2

Expected: current dates/provenance update, version increments once, DATES_CHANGED row contains only changed fields and `{previous,new}`, event/audit emitted after commit.  
**Status:** Passed — 2026-07-18

### JRN-TC-004 — Invalid transition has no side effects

**Severity:** Critical  
**Condition:** `JRN-COND-004`  
**Oracle:** Story 6.1 AC2; TDS §6.3

Preconditions: COMPLETED journey or transition outside approved matrix.  
Expected: `JOURNEY-012` for terminal row or `JOURNEY-016` for invalid transition; current version unchanged; no history/audit/event.  
**Status:** Passed — 2026-07-18

### JRN-TC-005 — History failure rolls back current mutation

**Severity:** Critical  
**Condition:** `JRN-COND-005`  
**Oracle:** ADR-JOURNEY-006-01

Inject repository failure while inserting transition.  
Expected: transaction rollback; current row retains all previous values/version; no audit/domain event.  
**Status:** Passed — 2026-07-18

### JRN-TC-006 — Date change without provenance rejected

**Severity:** High  
**Condition:** `JRN-COND-008`  
**Oracle:** FR45 inherited by Epic 6; TDS §9.2

Expected: 400 `JOURNEY-018`; note-only update remains compatible without provenance.  
**Status:** Passed — 2026-07-18

### JRN-TC-007 — Legacy BABY_CARE excluded from canonical selection

**Severity:** High  
**Condition:** `JRN-COND-010`  
**Oracle:** ADR-JOURNEY-006-03

Expected: record remains readable through legacy paths; canonical dashboard query does not select it as maternal lifecycle; migration does not archive/delete it.  
**Status:** Passed — 2026-07-18

### SECURITY TEST CASES

### JRN-TC-SEC-001 — Unauthenticated access

**Severity:** Critical  
**OWASP:** A01 Broken Access Control  
**Condition:** `JRN-COND-007`  
**Oracle:** TDS §16

POST, PUT, history GET without JWT return 401 and produce no mutation/history.  
**Status:** Passed — 2026-07-18

### JRN-TC-SEC-002 — Expert role cannot create/update/read history

**Severity:** Critical  
**Condition:** `JRN-COND-007`  
**Oracle:** TDS §16

Expected: 403; no repository mutation.  
**Status:** Passed — 2026-07-18

### JRN-TC-SEC-003 — Other Mother cannot read or mutate history

**Severity:** Critical  
**CWE:** CWE-639 Authorization Bypass Through User-Controlled Key  
**Condition:** `JRN-COND-007`  
**Oracle:** BR-OWNERSHIP; Story 6.1 AC5

Expected: 403 `JOURNEY-011`; response contains no target state/history; no side effect.  
**Status:** Passed — 2026-07-18

### JRN-TC-SEC-004 — History payload is allow-listed

**Severity:** High  
**Condition:** `JRN-COND-008`  
**Oracle:** TDS §4.3 and §5.2

Expected: transition JSON contains changed lifecycle/date fields only; notes, tokens, contact data, and unrelated health data absent from DB payload, response, audit, and logs.  
**Status:** Passed — 2026-07-18

### INTEGRATION TEST CASES

### JRN-TC-INT-001 — PostgreSQL current/history happy path

**Severity:** Critical  
**Test File:** `src/test/java/com/carebridge/backend/journey/JourneyCanonicalLifecycleIntegrationTest.java`  
**Condition:** `JRN-COND-001`  
**Oracle:** TDS §5.2

Apply Flyway to PostgreSQL test instance, create via API, assert one current and one CREATED transition plus response version.  
**Status:** Passed — 2026-07-18

### JRN-TC-INT-002 — Concurrent creates produce one winner

**Severity:** Critical  
**Condition:** `JRN-COND-002`  
**Oracle:** ADR-JOURNEY-006-02

Use two independent transactions/barrier for the same owner and different canonical stages.  
Expected: one 201, one 409 `JOURNEY-015`; one canonical current row; one CREATED transition.  
**Status:** Passed — 2026-07-18

### JRN-TC-INT-003 — Current/history/audit atomicity

**Severity:** Critical  
**Condition:** `JRN-COND-005`  
**Oracle:** ADR-JOURNEY-006-01

Force transition insert failure and assert current/audit rollback; then successful retry creates exactly one transition.  
**Status:** Passed — 2026-07-18

### JRN-TC-INT-004 — Migration backfills existing journeys

**Severity:** Critical  
**Condition:** `JRN-COND-006`  
**Oracle:** Story 6.1 AC4; TDS §5.2

Seed valid legacy rows before applying the migration.  
Expected: version/provenance columns exist; one MIGRATED row per existing canonical journey; no journey is deleted/archived.  
**Status:** Passed — 2026-07-18

### JRN-TC-INT-005 — Migration aborts on duplicate canonical active owner

**Severity:** Critical  
**Condition:** `JRN-COND-006`  
**Oracle:** TDS §5.2 existing-data policy

Seed one owner with ACTIVE PRE_PREGNANCY and ACTIVE PREGNANCY.  
Expected: migration fails with canonical-duplicate diagnostic before creating the unique index; records remain unchanged.  
**Status:** Passed — 2026-07-18

### JRN-TC-INT-006 — Optimistic conflict prevents lost update

**Severity:** Critical  
**Condition:** `JRN-COND-009`  
**Oracle:** Story 6.1 AC5; ADR-JOURNEY-006-02

Load the same row in two transactions; commit both different date changes.  
Expected: first commits; second returns 409 `JOURNEY-017`; final current state and history reflect only the winner.  
**Status:** Passed — 2026-07-18

---

## 5. Red-Green-Refactor Tracker

| TC group | Planned file | RED confirmed | GREEN commit | Refactor note |
| --- | --- | --- | --- | --- |
| 001–007 | `JourneyCanonicalLifecycleServiceTest.java` | [x] 7 intended failures/errors | 7/7 pass in working tree | Policy/orchestration consolidated in `JourneyTransitionServiceImpl` |
| SEC-001..004 | `JourneyCanonicalLifecycleControllerTest.java` | [x] 4 intended failures/errors | 4/4 pass in working tree | Assertions aligned with the existing `ApiErrorResponse.error` contract |
| INT-001..006 | `JourneyCanonicalLifecycleIntegrationTest.java` | [x] 6 intended failures/errors | 6/6 pass in working tree | Uses PostgreSQL, real Flyway migration, transaction races, rollback, and optimistic locking |

### 5.1 Red Gate Protocol

Before production implementation:

1. Add planned interfaces/entities only as compile-safe stubs where required.
2. Make orchestration methods throw `UnsupportedOperationException`.
3. Run targeted tests.
4. Every new behavior test must fail for the intended missing behavior, not compilation mistakes.
5. Any test that passes against the stub is rejected and rewritten.

```powershell
cd 05_Development/CareBridgeAPI
.\mvnw.cmd -Dtest="JourneyCanonicalLifecycle*Test" test
```

Red evidence path:

`06_Testing/TestResults/epic-6/story-6-1/red-gate-evidence.log`

Red Gate executed on 2026-07-18: 17 tests ran with 8 failures and 9 errors caused by the intentionally missing lifecycle behavior. The migration itself applied successfully in PostgreSQL Testcontainers. The retained log is the immutable pre-implementation evidence.

Green evidence path:

`06_Testing/TestResults/epic-6/story-6-1/green-gate-evidence.log`

Green Gate executed on 2026-07-18: 17/17 targeted tests passed.

---

## 6. Entry / Exit Criteria

### Entry Criteria

- [x] TDS and Test-Spec approved by the project approver.
- [x] ADR-JOURNEY-006-01..03 accepted.
- [ ] DPO/privacy review complete.
- [ ] Target database duplicate-active preflight result available. Isolated PostgreSQL migration and duplicate-abort fixtures pass; deployment-target evidence remains pending.
- [x] Migration name/version confirmed against the branch at implementation time.
- [x] Synthetic factories and PostgreSQL test environment ready.

### Suspension Criteria

- Existing duplicate canonical ACTIVE data has no approved reconciliation.
- Product rejects BABY_CARE compatibility boundary.
- An implementation requires editing applied V1.
- PostgreSQL integration environment cannot execute Flyway.

### Exit Criteria

- P0 cases: 100% pass.
- P1 cases: at least 95% pass and no open high/critical defect.
- Service/policy changed code has at least 80% line coverage; branch coverage is reported.
- Full backend test and package commands pass, or non-Story baseline failures are explicitly inventoried and waived for the Story gate without waiving any Journey failure.
- No current/history mismatch in fault/concurrency tests.
- No PII/secret leakage.
- Red Gate evidence exists and shows meaningful failures before implementation.

### Exit Status — 2026-07-18

- [x] All Story 6.1 review cases pass: backend 45/45 and mobile behavior 15/15.
- [x] Changed service/policy coverage passes: 90.83% line; 60.53% branch reported.
- [x] Full backend baseline is dispositioned for this Story only: 2238 tests ran with 1 failure, 109 errors, and 1 skipped outside Journey; no Journey failure is waived.
- [x] Current/history consistency passes fault, create-race, and optimistic-conflict tests.
- [x] History response and persistence payloads use the minimum allow-list covered by SEC-004.
- [x] RED and GREEN evidence logs are retained.
- [x] Backend package succeeds with `-DskipTests package`.

### Downstream mobile gap-fix verification — 2026-07-18

| Gate | Result | Evidence |
| --- | --- | --- |
| Mobile gap regression | PASS — 15/15 behavior tests | `05_Development/CareBridgeMobileApp/test/features/journey/story_6_1_mobile_gap_test.dart` |
| Full Flutter regression | PASS — 187/187 | `flutter test` after the final cache reconciliation patch |
| Targeted Flutter analysis | PASS — no issues | journey, Mother Home, and Story 6.1 test |
| Full Flutter analysis | BASELINE — 19 diagnostics outside Story files | community, consultation, directChat, home, reminder, and unrelated tests; no changed Story-file diagnostic |
| Android debug APK | PASS — build and install | `API_BASE_URL=http://127.0.0.1:8080` |
| Physical-device smoke | PASS — routing, history, back accessibility | `06_Testing/TestResults/epic-6/story-6-1/mobile-fix-verification-2026-07-18/` |
| Mobile manual closure | PASS — 7/7 targeted; composite 16/16 | `06_Testing/TestResults/epic-6/story-6-1/mobile-gap-fix-rerun-2026-07-18/README.md` |

The downstream mobile quality gate is closed. Coverage is measured and the non-Journey backend baseline is explicitly waived for the Story gate; repository-wide release status remains red until that baseline debt is resolved.

---

## 7. Rollback Plan

Tests must prove application rollback compatibility with the additive schema. Production tests must never drop tables/indexes or delete Flyway history. Destructive rollback commands are not part of this Test-Spec.

If the migration fails:

- capture Flyway/PostgreSQL evidence;
- verify existing rows remain unchanged;
- resolve data conflict or migration defect in a new reviewed change;
- rerun from a clean test database before staging.

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP | Risk | Review check | Gate |
| --- | --- | --- | --- |
| AP-AI-001 | Unconstrained generation | every case references FR/AC/ADR/TDS | G-0 |
| AP-AI-002 | Green from birth | every new behavior fails against stub | G-2 |
| AP-AI-003 | Implicit decision | expected transitions/errors exist in TDS | G-1 |
| AP-AI-004 | Layer violation | controller tests do not expect business logic in controller | G-4 |
| AP-AI-005 | Hallucinated contract | planned symbols compile before test execution | G-3 |

Review status:

- [x] Specification contains oracle references.
- [x] Test data is synthetic and isolated.
- [x] Risk IDs and coverage conditions are bidirectionally mapped.
- [x] Human project approver approved the TDS decisions and Red Gate plan.
- [x] Human-approved code review remediation is complete; all 21 Story findings are closed. Repository-wide baseline debt remains release-visible under the scoped waiver.
