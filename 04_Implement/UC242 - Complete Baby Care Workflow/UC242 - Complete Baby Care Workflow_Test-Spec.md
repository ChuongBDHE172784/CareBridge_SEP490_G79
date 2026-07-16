# TEST-DRIVEN DEVELOPMENT SPECIFICATION TEMPLATE
# Test Specification — UC-242 Complete Baby Care Workflow

**Version:** `0.6`  
**Status:** `Partially Implemented — 2026-07-15 (Sprint 1 in progress)`  
**Approval Record:** `Project owner approval recorded 2026-07-15`  
**Created / Updated:** `2026-07-15`  
**Document Owner:** `PhuongNT`  
**Author:** `OpenAI Codex — Specification Author`  
**DPO Sign-off:** `[ ] Pending`  
**Implementation Authorization:** `[x] Approved by project owner; production release remains gated by required role sign-offs`

---

## CHANGELOG

| Version | Date | Author | Change |
|---|---|---|---|
| 0.1 | 2026-07-15 | OpenAI Codex | Initial Red-phase test specification for UC-242. |
| 0.2 | 2026-07-15 | OpenAI Codex | Recorded project-owner approval; retained Tech Lead, QA Lead, and DPO entry/release gates. |
| 0.3 | 2026-07-15 | OpenAI Codex | Recorded Green evidence for 60 targeted backend tests and 37 full mobile tests; added controller authorization contracts, growth Red/Green evidence, and caregiver revocation coverage. |
| 0.4 | 2026-07-15 | OpenAI Codex | Added four PostgreSQL full-stack security tests; corrected canonical `FAMILY` role and JSONB permission mapping through Red/Green evidence. |
| 0.5 | 2026-07-15 | OpenAI Codex | Expanded PostgreSQL coverage to growth delete/history, per-baby summary isolation, and pending/expired caregiver denial; recorded 68-test UC242 Green gate and unrelated full-suite baseline failures. |
| 0.6 | 2026-07-15 | OpenAI Codex | Applied adversarial review fixes for archived/expired caregiver access, denial audit evidence, daily-log collection, SYMPTOM validation, and safe mobile growth parsing; targeted gate now has 78 passing tests. |
| 0.7 | 2026-07-15 | OpenAI Codex | Closed milestone path-IDOR by enforcing milestone/baby path binding; added update/delete mismatch regression tests. |

---

## MỤC LỤC

1. Thông tin Module
2. Logic Issues Resolved
3. Test Design Specification
4. Test Case Specification
5. Red-Green-Refactor Tracker
6. Entry / Exit Criteria
7. Rollback Plan
8. CASE 2.0 Anti-Pattern Detection

## 1. Thông tin Module

| Field | Value |
|---|---|
| Module | `UC-242 Complete Baby Care Workflow` |
| Workflow | `MF-03` |
| Test level | Unit, repository, API integration, contract, Flutter widget/integration, security, E2E |
| Platforms | Spring Boot API + Flutter Mobile; Web excluded |
| Risk | High: sensitive child data, object authorization, reminders, health-support boundary |
| TDS | `UC242 - Complete Baby Care Workflow_TDS.md` v0.1 Draft |
| Required method | Strict Red → Green → Refactor, sprint order |

### 1.1 AI Generation Context (CASE 2.0)

```yaml
module: UC242
parent_workflow: MF-03
source_of_truth:
  - approved UC242 TDS and this Test-Spec
  - SRS Detailed Scope 121UC FR/UC-032..045
  - named business rules
forbidden_assumptions:
  - clinical diagnosis or developmental assessment
  - owner equivalence for any authenticated role
  - production mock data
  - invented retention/SLA/permission semantics
gates:
  - tests must fail for the intended reason before production code
  - sprint N+1 cannot start until sprint N exit criteria pass
```

## 2. Logic Issues Resolved

| Issue | Current evidence | Required target / test oracle |
|---|---|---|
| Incorrect care-group lookup | `BabyAccessPolicy.canView()` uses baby profile ID as care-group ID | Owner relation + accepted, unexpired explicit permission; SEC-001…004 |
| Role inconsistency | Domain controllers previously mixed MOTHER, non-canonical FAMILY_MEMBER, authenticated | Canonical `FAMILY` role is the coarse gate; object policy is authoritative |
| Daily-log mobile gap | Backend create exists; production mobile workflow incomplete | API-backed create/list/detail/summary; INT-001 |
| Mock/unsafe growth UI | Hard-coded chart/“developing well” copy | Persisted values + neutral provenance; SAFE-001 |
| Milestone read gap | Add/update/delete exist; list/detail absent | New scoped list/detail contracts; UC242-TC-007 |
| Vaccination route mismatch | Backend canonical route differs from mobile service | Mobile uses `/api/v1/vaccination/babies/{babyId}/...`; CONTRACT-001 |
| Dummy reminder scheduling | Reminder module returns `dummy-job-id` | Production profile requires real idempotent dispatcher; INT-004 |
| Missing MF-03 projections | No baby timeline/preparation summary | Deterministic derived reads; API-015…017 |

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

**In scope:** profile selection context; care overview; journal CRUD/summary; growth CRUD/history/chart; milestone CRUD/list/detail; vaccination schedule/records/reminder state; due notification dispatch; timeline; appointment preparation; Flutter navigation/state/accessibility; object-level security and privacy.

**Out of scope:** Web UI, diagnosis/prescription/developmental assessment, official vaccination certification, offline write synchronization, load targets not approved in the TDS.

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Basis | Behavior |
|---|---|
| MF-03 Activity Diagram | Complete baby-care workflow and observation-only boundary |
| FR/UC-032…045 | Current Release P0 user outcomes |
| BR-BABY-01…07 | Profile, overview, logs, deletion and summary rules |
| BR-DEVELOPMENT-01…02 | Observed milestone, owner-entered modification |
| BR-GROWTH-01…03 | Source/time and non-diagnostic chart |
| BR-VACCINE-01…02 | Personal support record + informational reference |
| BR-OWNERSHIP | Baby data private by default |
| ADR-242-001…005 | Architecture and safety decisions |
| PDPA / project RBAC/audit policy | Sensitive data controls |

### TDS-03 — Test Conditions and Coverage Items

| ID | Condition | Coverage |
|---|---|---|
| CND-01 | Owner, accepted caregiver, pending/revoked/expired caregiver, unrelated user | decision table |
| CND-02 | Baby A record used through Baby B path | every read/write resource type |
| CND-03 | Empty/one/many/deleted records | each projection/list |
| CND-04 | Same event timestamp across source types | deterministic timeline ordering |
| CND-05 | Reminder due/not due/snoozed/completed/preference off/no token/FCM fail/concurrent workers | state + concurrency |
| CND-06 | Forbidden clinical wording/reference provenance | all relevant DTO/UI text |
| CND-07 | Active baby switch during load/back navigation | Flutter race/state isolation |
| CND-08 | 401/403/404/400/409/500/503 | API/mobile error mapping |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

- Decision-table testing for role, ownership, membership, consent, and explicit permission.
- Boundary-value testing for page size 0/1/50/51, dates, numeric measurements, and notes.
- State-transition testing for reminders and archived/deleted records.
- Pairwise testing across record type, actor type, and operation.
- Property-based testing for timeline stable ordering and cross-baby isolation.
- Mutation testing focus: remove policy call, invert consent check, remove unique delivery key, add forbidden conclusion.
- Contract testing between Flutter services and Spring routes/schema.

### TDS-05 — Test Data Requirements

| Fixture | Type | Minimal data | Purpose |
|---|---|---|---|
| `FX-OWNER-A` | JWT/user | MOTHER owner of Baby A | full access |
| `FX-CAREGIVER-ALLOW` | JWT/member | FAMILY, ACCEPTED, unexpired scoped permissions | delegated access |
| `FX-CAREGIVER-DENY-*` | membership | pending/revoked/expired/missing permission | deny matrix |
| `FX-BABY-A/B` | DB seed | distinct owners/data | isolation |
| `FX-MF03-DATA` | DB seed | logs, growth, milestones, vaccines, reminders | projections |
| `FX-REFERENCE-V1` | DB seed | active source/version/effective date | provenance |
| `FX-DUE-REMINDER` | DB seed | PENDING due occurrence | dispatcher |
| `FX-FCM` | fake adapter | success/failure/retry counters | deterministic delivery |

Factories generate unique UUIDs and freeze `Clock`; tests never depend on wall-clock time, execution order, or shared mutable fixtures.

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```dart
setUp(() {
  fakeClock = FakeClock(DateTime.utc(2026, 7, 15, 3));
  repository = FakeBabyCareRepository.empty();
});

tearDown(() {
  repository.dispose();
});
```

```java
@BeforeEach
void isolate() {
  reset(repositories, notificationService);
  clock = Clock.fixed(Instant.parse("2026-07-15T03:00:00Z"), ZoneOffset.UTC);
}
```

### UC242-TC-001 — Active baby selection changes context only

**Level:** Flutter integration + API contract  
**Oracle Source:** `UC-34`, `BR-BABY-03`, ADR-242-001  
**Given:** Owner can view Baby A and Baby B.  
**When:** User switches from A to B while overview A is loading.  
**Then:** Every visible tab resolves only B data; late A response is discarded; ownership/permissions are unchanged.

### UC242-TC-002 — Overview is backend-backed and descriptive

**Level:** Service/API/widget  
**Oracle Source:** `UC-35`, `BR-BABY-04`, C1/C2  
**Then:** Returned counts/items equal persisted fixtures and contain provenance/notice; no hard-coded fallback or clinical conclusion appears.

### UC242-TC-003 — Add supported daily log

**Oracle Source:** `UC-36`, `BR-BABY-05`  
Valid feeding/sleep/diaper observation persists once with caller, baby, source, and event time. Unsupported/missing fields return 400 with no row.

### UC242-TC-004 — Update/delete only permitted entered log

**Oracle Source:** `UC-37`, `BR-BABY-06`, BR-OWNERSHIP  
Owner/permitted actor may modify allowed record; cross-baby or unauthorized attempt returns safe deny and no mutation; delete follows existing retention/audit behavior.

### UC242-TC-005 — Summary uses 24h/7d windows

**Oracle Source:** `UC-38`, `BR-BABY-07`  
Boundary timestamps are included/excluded per documented half-open UTC interval; output is observational totals/patterns only.

### UC242-TC-006 — Record observed milestone

**Oracle Source:** `UC-39`, `BR-DEVELOPMENT-01`  
Persists type/date/note as caregiver observation; no inferred status/disorder is generated.

### UC242-TC-007 — Milestone list/detail are scoped

**Oracle Source:** ADR-242-001/002  
Pagination is stable; deleted entries excluded; milestone from another baby cannot be retrieved using the requested baby path.

### UC242-TC-008 — Update/delete milestone authorization

**Oracle Source:** `UC-40`, `BR-DEVELOPMENT-02`  
Only owner-entered or explicitly permitted policy behavior succeeds; denial causes no state/audit misinformation.

### UC242-TC-009 — Growth source/time validation

**Oracle Source:** `UC-41`, `BR-GROWTH-01`  
Required source/time and measurement boundaries are validated; future/invalid/non-finite values fail.

### UC242-TC-010 — Growth mutation and audit

**Oracle Source:** `UC-42`, `BR-GROWTH-02`  
Update/delete affects exactly one baby-scoped row and preserves required audit metadata.

### UC242-TC-011 — Growth history/chart safety

**Oracle Source:** `UC-43`, `BR-GROWTH-03`  
Chart points match persisted values/order and never output healthy/normal/developing-well or diagnosis; reference labels include provenance.

### UC242-TC-012 — Vaccination record management

**Oracle Source:** `UC-44`, `BR-VACCINE-01`  
CRUD uses the canonical backend route, remains a personal support record, and is baby-scoped.

### UC242-TC-013 — Reference schedule and state

**Oracle Source:** `UC-45`, `BR-VACCINE-02`  
Expected date derives from approved active reference + birth date; completed/recorded and due/overdue state are deterministic; provider-confirmation notice and source version are present.

### UC242-TC-014 — Mobile vaccination contract

**Oracle Source:** ADR-242-001, current canonical route  
Every Flutter request targets `/api/v1/vaccination/babies/{babyId}/...`; legacy `/api/v1/vaccinations/{id}` fails the contract test.

### UC242-TC-015 — Timeline composition and stable cursor

**Oracle Source:** ADR-242-003  
All permitted source types normalize, order by occurredAt/type/id, exclude deleted data, and paginate without duplicates/skips for an unchanged snapshot.

### UC242-TC-016 — Appointment-preparation facts only

**Oracle Source:** MF-03 boundary, ADR-242-003/005  
Summary includes recent observations, latest sourced measurements, milestones, vaccine/reference state and reminders; contains no recommendation, priority, diagnosis, or treatment instruction.

### UC242-TC-017 — Empty and failure projections

**Oracle Source:** approved UI states, ADR-242-003  
No data returns valid empty sections. A source failure returns a stable error/partial-state contract approved before Green; it never substitutes fake data.

### SECURITY TEST CASES

### UC242-TC-SEC-001 — Access decision table

Owner allowed; accepted caregiver allowed only by scoped permission; pending/revoked/expired/unrelated denied. Cover view plus every write permission.

### UC242-TC-SEC-002 — IDOR across child resources

For log, growth, milestone, vaccine, timeline cursor, and reminder IDs, substitute an ID from Baby B in Baby A path. Expected: no disclosure/mutation and stable safe error.

### UC242-TC-SEC-003 — JWT identity cannot be overridden

Inject owner IDs in query/body/header. Expected: ignored/rejected; caller remains JWT subject.

### UC242-TC-SEC-004 — Consent revocation during session

After an overview succeeds, revoke permission before a write/deep link. Expected: service re-check returns 403 and stale UI clears sensitive content.

### UC242-TC-SEC-005 — Push/log privacy

Generic push only; logs/audit contain identifiers/actions/correlation but no baby name, notes, measurement values, token, or sensitive notification body.

### UC242-TC-SEC-006 — Health-boundary vocabulary

Static and runtime scan rejects diagnosis/prescription/development assessment and unqualified healthy/normal claims across DTOs, localization, widget text, and notification templates.

### INTEGRATION TEST CASES

### UC242-TC-INT-001 — Complete MF-03 traversal

Create/select baby → add journal → add growth → add milestone → add vaccination record → create reminder → view overview/timeline/preparation. Assert persisted values and audit continuity.

### UC242-TC-INT-002 — API/mobile contract suite

Generated/OpenAPI schemas match Dart serializers, nullability, enums, pagination, error envelopes, and canonical paths.

### UC242-TC-INT-003 — Composite read isolation

Mixed Baby A/B source fixtures return only authorized selected-baby data, including nested DTOs and counts.

### UC242-TC-INT-004 — Concurrent reminder dispatch

Two scheduler workers claim the same due occurrence. Exactly one delivery key and at most one FCM send occur. Preference-off skips send; no-token/failure is auditable; retry preserves idempotency.

### UC242-TC-E2E-001 — Hub navigation and UI states

On supported devices, traverse all tabs and verify loading, empty, error, retry, permission-denied, and no-network read states without mock production content.

### UC242-TC-E2E-002 — Accessibility and Warm Claymorphism consistency

Verify WCAG 2.1 AA contrast, semantics, text scaling, 48dp targets, keyboard/focus where applicable, reduced motion, and design-token use.

### UC242-TC-E2E-003 — Notification deep link reauthorization

Receive generic reminder push, expire/revoke session or baby permission, open it, and verify authentication/object authorization precedes detail rendering.

## 5. Red-Green-Refactor Tracker

| Sprint | Red suite required first | Green scope | Refactor gate | Status |
|---|---|---|---|---|
| 1 | SEC-001…004, TC-003…005, TC-009…011, CONTRACT baseline | Access policy + journal + growth + milestone path binding | no duplicate guards; neutral copy | `🟡 In Progress — 80 targeted backend + 37 mobile tests Green; SEC-006 static scan added; traceability/product gates pending` |
| 2 | TC-001/002, TC-006…008, TC-012…014, INT-002/003 | milestone + vaccination + hub | shared states/components/routes | `[ ] Blocked by Sprint 1 gate` |
| 3 | TC-015…017, INT-004, E2E-001…003, SEC-005/006 | dispatcher + timeline + preparation | observability/performance cleanup | `[ ] Blocked by Sprint 2 gate` |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

For each sprint:

1. Add/modify tests only; run the smallest deterministic command.
2. Capture failing test name, failure message, command, commit/worktree state, and why failure proves missing behavior.
3. Reject failures caused by compile errors, broken fixtures, wrong endpoints not under change, or environment outage.
4. Obtain reviewer acknowledgement of valid Red evidence.
5. Implement the minimum Green behavior; run targeted then regression suites.
6. Refactor only while all tests remain Green.

No test may be weakened, skipped, deleted, or rewritten to mirror implementation without oracle review.

## 6. Entry / Exit Criteria

### Entry Criteria (Điều kiện bắt đầu)

- [ ] UC242 TDS/Test-Spec approved and DPO sign-off recorded.
- [ ] OPEN-PERM-01 resolved before caregiver writes.
- [ ] Test fixtures, frozen clock, fake FCM adapter, and supported device matrix available.
- [ ] Existing regression baseline is Green or unrelated failures are documented/owned.
- [ ] Sprint-specific Red tests reviewed.

### Exit Criteria (Điều kiện kết thúc — DoD)

- [ ] All Sprint 1→2→3 tests pass in order; no skipped/quarantined UC242 critical test.
- [ ] Backend unit/integration/package and Flutter analyze/test pass.
- [ ] Contract, IDOR, consent revocation, privacy-log, idempotency, and health-boundary tests pass.
- [ ] Required statement/branch targets are approved by QA (`OPEN-QA-01`); critical authorization/safety branches are 100% condition-covered.
- [ ] Migration and rollback rehearsal pass in non-production.
- [ ] Product, QA, Tech Lead, DPO approve release evidence.

### Suspension Criteria (Điều kiện tạm dừng)

Suspend when the requirement oracle conflicts, permission/retention/reference provenance is unresolved for touched behavior, Red evidence is invalid, sensitive data is exposed, production dummy path is reachable, or a clinical inference is introduced.

## 7. Rollback Plan

1. Disable composite-view and reminder-dispatch feature flags.
2. Stop scheduled dispatch before application rollback.
3. Re-deploy previous artifacts; retain additive nullable schema unless DBA approves a forward corrective migration.
4. Preserve notification/audit evidence and reconcile any in-flight claimed reminder.
5. Run legacy domain smoke tests plus SEC-001/002/005.
6. Keep the gap/open item active; do not mark Green/Done after rollback.

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| Anti-pattern | Automated/review signal | Decision |
|---|---|---|
| Self-fulfilling test | Expected value copied from implementation or same helper | Reject test |
| Missing Red evidence | Test added after production code or never failed correctly | Gate fail |
| Role-only security | No object-policy assertion/mutation test | Gate fail |
| Happy-path-only | No denial, boundary, empty, failure, concurrency coverage | Gate fail |
| Mock production data | fake/constants reachable in release flavor | Release block |
| Clinical assertion | test expects healthy/normal/diagnostic output | Reject requirement/test |
| Weak idempotency | verifies DB count but not FCM call count under concurrency | Gate fail |
| Cross-baby blind spot | child resource queried only by record ID | Gate fail |
| Snapshot-only UI test | no semantic/accessibility/interaction oracle | Insufficient |
| Unapproved numeric target | invented retention/SLO/coverage number | Mark OPEN and stop |

### Open Test Decisions

| ID | Owner | Decision |
|---|---|---|
| OPEN-QA-01 | QA Lead | Overall statement/branch coverage thresholds beyond critical-path 100% condition coverage |
| OPEN-TEST-02 | Product/Tech Lead | Exact partial-response contract if one composite source fails |
| OPEN-TEST-03 | QA/Mobile Lead | Supported device/OS matrix and network profiles |
