# ENGINEERING DOCUMENTATION STANDARD (EDS) v2.0

# TEST SPECIFICATION — Monitor Shared Family Care

| Field | Value |
| --- | --- |
| Document ID | `UC-FM-05-TEST-SPEC` |
| Version | `0.1` |
| Date | `2026-08-23` |
| Status | `Draft` |
| Feature / Gap ID | `UC-FM-05` |
| Module | `Family Cooperative Care` |
| Paired TDS | `UC-FM-05-TDS` |
| Priority | `Medium` |
| Platforms | `Mobile / Backend` |
| Data Classification | `Restricted care-group relationship, shared-care task, appointment, note, permission, and consent-scoped data` |
| Compliance Scope | `PDPA relationship/consent scoping, least privilege, cross-member isolation, and auditability of permission-changing actions` |
| Owner | `CareBridge Team` |
| Reviewer / Approver |  |
| Source Baseline | Current worktree `2026-08-23`; SRS/TDS `UC-FM-05` and exact code/test sources below |

## CHANGELOG

| Version | Date | Author | Change | Status |
| --- | --- | --- | --- | --- |
| 0.1 | 2026-08-23 | CareBridge Team | Initial evidence-first full-form Draft | Draft |

## TABLE OF CONTENTS

1. Module Information and AI Generation Context
2. Logic Issues Resolved
3. Test Design Specification
4. Test Case Specification
5. Red-Green-Refactor Tracker
6. Entry, Exit, and Suspension Criteria
7. Rollback Plan
8. CASE 2.0 Anti-Pattern Detection

## 1. Module Information and AI Generation Context

### 1.1 Module Information

| Item | Specification | Oracle Source |
| --- | --- | --- |
| Actor goal | View family dashboard projections, permitted shared maternal/baby data, care-group checklist state/actions, and quick-note history. | SRS `UC-FM-05` |
| Current state | `High` confidence; gaps are listed in Section 2 | Exact current code/test sources below |
| Entry points | Family home, care-group detail, shared-data, and quick-note history screens | Current client/router evidence |
| Authorization boundary | `Authorized Family Member / Mother` plus exact authentication/role/ownership/membership/consent policy | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/FamilyDashboardController.java` |
| Primary operations | Select an authorized care group.; Load dashboard/shared-data/checklist projections constrained by permissions.; Apply an allowed checklist action or review/add a quick note and refresh history. | SRS `UC-FM-05` Normal Flow |
| Sensitive data | Use the classification header and exact request/response field inventories in paired TDS Sections 5 and 9; synthesize only fields exercised by the case | Paired TDS Sections 5 and 9 |

### 1.2 AI Generation Context (CASE 2.0)

- Generation mode: evidence-first; no invented field, error, SLA, accuracy, or pass result.
- Trust level: Draft until human review.
- Unknown handling: `Open — question/evidence needed`.
- Existing tests are regression evidence and must be rerun before a Green claim.
- The AI architecture source is immutable/reference-only in this workflow.

### 1.3 Reference Baseline

| Ref ID | Type | Exact path | Revision | Authority |
| --- | --- | --- | --- | --- |
| `SRC-SRS` | Requirement | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-FM-05 | 2026-08-23 | Draft code-first requirement |
| `SRC-TDS` | Design | Paired `UC-FM-05-TDS` | 0.1 | Draft design |
| `SRC-CODE-01` | Current code | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/FamilyDashboardController.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-02` | Current code | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/SharedDataController.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-03` | Current code | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/today/controller/CareGroupChecklistController.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-04` | Current code | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/FamilyQuickNoteController.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-05` | Current code | `05_Development/CareBridgeMobileApp/lib/features/home/screens/family_member_home_screen.dart` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-01` | Existing test | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/service/FamilyDashboardServiceTest.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-02` | Existing test | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/service/SharedDataServiceImplTest.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-03` | Existing test | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/service/FamilyQuickNoteServiceTest.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-04` | Existing test | `05_Development/CareBridgeMobileApp/test/features/familySync/family_dashboard_contract_test.dart` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-ERROR-01` | Current exception advice | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/common/exception/GlobalExceptionHandler.java` | Worktree `2026-08-23` | Current-state evidence |

## 2. Logic Issues Resolved

| Issue ID | Discrepancy | Impact | Resolution | Oracle | Status |
| --- | --- | --- | --- | --- | --- |
| `LI-01` | Broad 43-UC catalogue previously obscured this boundary | Generic TCs could not map to `Monitor Shared Family Care` | Split as `UC-FM-05` using the audited current code boundary | SRS 3.1 and `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/FamilyDashboardController.java` | Resolved in Draft |

Architecture-, schema-, authorization-, and test-changing Open items must be resolved before implementation approval.

## 3. Test Design Specification

### TDS-01 — Risk-Based Scope

| Risk ID | Failure mode | Severity | Likelihood | Detectability | Levels | Conditions |
| --- | --- | --- | --- | --- | --- | --- |
| `RISK-01` | Failure of: Select an authorized care group. | High | Medium | High | Unit/Integration/Contract/applicable UI | `COND-01` |
| `RISK-02` | Failure of: Load dashboard/shared-data/checklist projections constrained by permissions. | High | Medium | High | Unit/Integration/Contract/applicable UI | `COND-02` |
| `RISK-03` | Failure of: Apply an allowed checklist action or review/add a quick note and refresh history. | High | Medium | High | Unit/Integration/Contract/applicable UI | `COND-03` |
| `RISK-AUTH` | Cross-user/role/member/consent data access | Critical | Medium | Medium | Security/Integration/Contract | `COND-AUTH` |
| `RISK-GAP` | Documentation claims an unreachable or broken path as complete | High | Medium | High | Characterization/Contract/UI | `COND-GAP` |

#### Platform and Test-Level Applicability Matrix

| Platform / Layer | Unit | Integration | Contract / Component | Widget / UI | E2E | Security |
| --- | --- | --- | --- | --- | --- | --- |
| Backend | Applicable — current backend/API contracts | Applicable — current backend/API contracts | Applicable — current backend/API contracts | Not applicable — backend has no UI | Applicable — current backend/API contracts | Applicable — current backend/API contracts |
| Web | Not applicable — no reachable Web consumer in this UC | Not applicable — no reachable Web consumer in this UC | Not applicable — no reachable Web consumer in this UC | Not applicable — no reachable Web consumer in this UC | Not applicable — no reachable Web consumer in this UC | Not applicable — no reachable Web consumer in this UC |
| Mobile | Applicable — current Mobile entry points | Applicable — current Mobile entry points | Applicable — current Mobile entry points | Applicable — current Mobile entry points | Applicable — current Mobile entry points | Applicable — current Mobile entry points |
| AI Service | Not applicable — no Python AI contract in this UC | Not applicable — no Python AI contract in this UC | Not applicable — no Python AI contract in this UC | Not applicable — Python service has no actor UI | Not applicable — no Python AI contract in this UC | Not applicable — no Python AI contract in this UC |

### TDS-02 — Test Basis and Oracle Hierarchy

| Basis | Requirement / behavior | Exact source | Oracle | Conditions |
| --- | --- | --- | --- | --- |
| `BASIS-01` | `UC-FM-05-FR-01` — Select an authorized care group. | SRS `UC-FM-05` Normal Flow 1; TDS Section 2 | Select an authorized care group. | `COND-01` |
| `BASIS-02` | `UC-FM-05-FR-02` — Load dashboard/shared-data/checklist projections constrained by permissions. | SRS `UC-FM-05` Normal Flow 2; TDS Section 2 | Load dashboard/shared-data/checklist projections constrained by permissions. | `COND-02` |
| `BASIS-03` | `UC-FM-05-FR-03` — Apply an allowed checklist action or review/add a quick note and refresh history. | SRS `UC-FM-05` Normal Flow 3; TDS Section 2 | Apply an allowed checklist action or review/add a quick note and refresh history. | `COND-03` |

Oracle precedence: approved user decision → approved BR/ADR/security policy → paired TDS → current implementation for characterization → existing test as regression evidence.

### TDS-03 — Test Conditions and Coverage Items

| Condition | Basis / risk | Behavior | Layer | Coverage | Test cases |
| --- | --- | --- | --- | --- | --- |
| `COND-01` | `BASIS-01` / `RISK-01` | Select an authorized care group. | Mobile / Backend | Positive + applicable boundary/state coverage | `UC-FM-05-TC-001` |
| `COND-02` | `BASIS-02` / `RISK-02` | Load dashboard/shared-data/checklist projections constrained by permissions. | Mobile / Backend | Positive + applicable boundary/state coverage | `UC-FM-05-TC-002` |
| `COND-03` | `BASIS-03` / `RISK-03` | Apply an allowed checklist action or review/add a quick note and refresh history. | Mobile / Backend | Positive + applicable boundary/state coverage | `UC-FM-05-TC-003` |
| `COND-API-001` | Exact handler/client composition contract | GET /api/v1/care-groups/{careGroupId}/checklists/current/tasks → getCurrentTasks contract | Applicable backend/client contract layer | Contract + DTO/status/authorization evidence | `UC-FM-05-TC-API-001` |
| `COND-API-002` | Exact handler/client composition contract | GET /api/v1/care-groups/{careGroupId}/checklists/history → listHistory contract | Applicable backend/client contract layer | Contract + DTO/status/authorization evidence | `UC-FM-05-TC-API-002` |
| `COND-API-003` | Exact handler/client composition contract | POST /api/v1/care-groups/{careGroupId}/checklists/tasks/{taskId}/actions → applyAction contract | Applicable backend/client contract layer | Contract + DTO/status/authorization evidence | `UC-FM-05-TC-API-003` |
| `COND-API-003-VAL` | Exact handler/client composition contract | POST /api/v1/care-groups/{careGroupId}/checklists/tasks/{taskId}/actions rejects a declared request-field boundary | Applicable backend/client contract layer | Contract + DTO/status/authorization evidence | `UC-FM-05-TC-API-003-VAL` |
| `COND-API-004` | Exact handler/client composition contract | GET /api/v1/care-groups/{careGroupId}/quick-notes → getHistory contract | Applicable backend/client contract layer | Contract + DTO/status/authorization evidence | `UC-FM-05-TC-API-004` |
| `COND-API-005` | Exact handler/client composition contract | GET /api/v1/care-groups/{groupId}/shared-data → getSharedData contract | Applicable backend/client contract layer | Contract + DTO/status/authorization evidence | `UC-FM-05-TC-API-005` |
| `COND-API-006` | Exact handler/client composition contract | GET /api/v1/family/dashboard → getDashboard contract | Applicable backend/client contract layer | Contract + DTO/status/authorization evidence | `UC-FM-05-TC-API-006` |
| `COND-BR-01` | `BR-01` | Membership, permission, and consent are rechecked for every projection. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/today/controller/CareGroupChecklistController.java` | Negative / decision / state | `UC-FM-05-TC-BR-001` |
| `COND-BR-02` | `BR-02` | Family emergency alerts are specified under UC-ES-03, not duplicated here. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/FamilyDashboardController.java` | Negative / decision / state | `UC-FM-05-TC-BR-002` |
| `COND-AUTH` | `RISK-AUTH` | Reject wrong authentication, role, ownership, membership, consent, or state scope | All protected layers | Security | `UC-FM-05-TC-SEC-001` |
| `COND-GAP` | `RISK-GAP` | Characterize current limitation/reachability without false completion | Applicable layer | Gap/Regression | `UC-FM-05-TC-GAP-001` |

### TDS-04 — Test Techniques

| Technique | Applied to | Rationale |
| --- | --- | --- |
| Equivalence partitioning | Eligible/ineligible actors, inputs, resources, and states | Covers supported and rejected current classes. |
| Boundary value analysis | Exact DTO ranges/lengths/time boundaries | Use the per-handler validators extracted in paired TDS Section 9; service-only bounds require their cited policy/service oracle. |
| Decision table | Role/ownership/membership/consent x state x action | Prevents UI-only authorization assumptions. |
| State-transition testing | Ordered operations and guarded lifecycle transitions | Detects stale, duplicate, and forbidden actions. |
| Contract testing | Every exact endpoint/provider boundary | Confirms status/schema/error without inventing fields. |
| Error guessing | Each recorded gap and historical broad-UC mismatch | Prevents regression to unreachable or grouped behavior. |

### TDS-05 — Test Data, Fixtures, Environment, and Isolation

| Data | Synthetic fixture | Variants | Cleanup |
| --- | --- | --- | --- |
| Actor | Synthetic `Authorized Family Member / Mother` plus closest wrong-role/cross-owner identities | Authenticated, unauthenticated, wrong scope, consent revoked | Reset principals/tokens |
| Resource | Minimum valid feature-owned object | Missing, malformed, boundary, stale, already-final, cross-owner | Transaction rollback or isolated repository cleanup |
| Provider/device | Deterministic fake only when applicable | Success, timeout, malformed, permission denied | Reset fake/timers/device state |
| Protected fields | Synthetic non-production values only | Redaction and disclosure checks | Never persist in snapshots/log fixtures |

Existing test evidence:

- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/service/FamilyDashboardServiceTest.java`
- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/service/SharedDataServiceImplTest.java`
- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/service/FamilyQuickNoteServiceTest.java`
- `05_Development/CareBridgeMobileApp/test/features/familySync/family_dashboard_contract_test.dart`

Clock, randomness/IDs, database/container, provider, event, file/media, AI model, camera/sensor/location controls must be fixed in the applicable test; irrelevant controls are `Not applicable — no such dependency in the tested operation`.

## 4. Test Case Specification

### 4.1 Props Isolation Boilerplate (CASE 2.0 — Required)

Use only the applicable platform factory; keep overrides minimal.

```java
private Request makeValidRequest(Consumer<RequestBuilder> overrides) {
    RequestBuilder builder = RequestBuilder.validDefaults();
    overrides.accept(builder);
    return builder.build();
}
```

```ts
const makeProps = (overrides: Partial<Props> = {}): Props => ({
  subject: makeSubject(),
  onAction: vi.fn(),
  ...overrides,
});
```

```dart
Widget makeSubject({Repository? repository, User? actor}) => TestApp(
  repository: repository ?? FakeRepository.withDefaults(),
  currentUser: actor ?? UserFactory.valid(),
  child: const SubjectScreen(),
);
```

For an absent platform, the corresponding factory is `Not applicable` according to the TDS-01 matrix.

### 4.2 Detailed Test Cases

### UC-FM-05-TC-001 — Select an authorized care group

| Field | Specification |
| --- | --- |
| Stable ID | `UC-FM-05-TC-001` |
| Severity | `High` |
| Test Condition | `COND-01` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Backend` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `SRS UC-FM-05 Normal Flow 1; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/today/controller/CareGroupChecklistController.java` |
| Preconditions | Synthetic `Authorized Family Member / Mother` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/service/FamilyDashboardServiceTest.java` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-01`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Select an authorized care group.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | The observable outcome is exactly `Select an authorized care group.` The applicable handler, request/response type, status, and authorization evidence are those enumerated in paired TDS Sections 2, 9, 10, and 16; no additional state is claimed. | `SRS UC-FM-05 Normal Flow 1; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/today/controller/CareGroupChecklistController.java` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-FM-05` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (The observable outcome is exactly `Select an authorized care group.` The applicable handler, request/response type, status, and authorization evidence are those enumerated in paired TDS Sections 2, 9, 10, and 16; no additional state is claimed.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-FM-05-TC-002 — Load dashboard/shared-data/checklist projections constrained by permissions

| Field | Specification |
| --- | --- |
| Stable ID | `UC-FM-05-TC-002` |
| Severity | `High` |
| Test Condition | `COND-02` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Backend` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `SRS UC-FM-05 Normal Flow 2; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/today/controller/CareGroupChecklistController.java` |
| Preconditions | Synthetic `Authorized Family Member / Mother` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/service/SharedDataServiceImplTest.java` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-02`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Load dashboard/shared-data/checklist projections constrained by permissions.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | The observable outcome is exactly `Load dashboard/shared-data/checklist projections constrained by permissions.` The applicable handler, request/response type, status, and authorization evidence are those enumerated in paired TDS Sections 2, 9, 10, and 16; no additional state is claimed. | `SRS UC-FM-05 Normal Flow 2; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/today/controller/CareGroupChecklistController.java` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-FM-05` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (The observable outcome is exactly `Load dashboard/shared-data/checklist projections constrained by permissions.` The applicable handler, request/response type, status, and authorization evidence are those enumerated in paired TDS Sections 2, 9, 10, and 16; no additional state is claimed.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-FM-05-TC-003 — Apply an allowed checklist action or review/add a quick note and refresh history

| Field | Specification |
| --- | --- |
| Stable ID | `UC-FM-05-TC-003` |
| Severity | `High` |
| Test Condition | `COND-03` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Backend` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `SRS UC-FM-05 Normal Flow 3; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/today/controller/CareGroupChecklistController.java` |
| Preconditions | Synthetic `Authorized Family Member / Mother` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/service/FamilyQuickNoteServiceTest.java` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-03`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Apply an allowed checklist action or review/add a quick note and refresh history.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | The observable outcome is exactly `Apply an allowed checklist action or review/add a quick note and refresh history.` The applicable handler, request/response type, status, and authorization evidence are those enumerated in paired TDS Sections 2, 9, 10, and 16; no additional state is claimed. | `SRS UC-FM-05 Normal Flow 3; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/today/controller/CareGroupChecklistController.java` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-FM-05` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (The observable outcome is exactly `Apply an allowed checklist action or review/add a quick note and refresh history.` The applicable handler, request/response type, status, and authorization evidence are those enumerated in paired TDS Sections 2, 9, 10, and 16; no additional state is claimed.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-FM-05-TC-API-001 — GET /api/v1/care-groups/{careGroupId}/checklists/current/tasks → getCurrentTasks contract

| Field | Specification |
| --- | --- |
| Stable ID | `UC-FM-05-TC-API-001` |
| Severity | `High` |
| Test Condition | `COND-API-001` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Backend` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/today/controller/CareGroupChecklistController.java` |
| Preconditions | Synthetic `Authorized Family Member / Mother` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | Planned — `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/checklist/today/controller/CareGroupChecklistControllerTest.java` (not present at Draft baseline) |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-API-001`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Invoke `GET /api/v1/care-groups/{careGroupId}/checklists/current/tasks` so `getCurrentTasks` receives path `careGroupId`: `UUID`; query `date`: `LocalDate`; context `timezone`: `String`; principal `principal`: `Principal`; satisfy `hasRole('FAMILY')` and the extracted `None` constraints.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | Success status `the success status explicitly produced by the handler` returns `CurrentChecklistResponse` with payload fields `asOf`: `Instant` (no field annotation in current DTO); `zoneId`: `String` (no field annotation in current DTO); `horizonDays`: `int` (no field annotation in current DTO); `sections`: `CurrentChecklistSections` (no field annotation in current DTO); `counts`: `TodayTaskCounts` (no field annotation in current DTO); `correlationId`: `UUID` (no field annotation in current DTO); `sequence`: `TodaySequenceProjection` (no field annotation in current DTO); the request body is `None` with Not applicable — no request body. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/today/controller/CareGroupChecklistController.java` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-FM-05` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (Success status `the success status explicitly produced by the handler` returns `CurrentChecklistResponse` with payload fields `asOf`: `Instant` (no field annotation in current DTO); `zoneId`: `String` (no field annotation in current DTO); `horizonDays`: `int` (no field annotation in current DTO); `sections`: `CurrentChecklistSections` (no field annotation in current DTO); `counts`: `TodayTaskCounts` (no field annotation in current DTO); `correlationId`: `UUID` (no field annotation in current DTO); `sequence`: `TodaySequenceProjection` (no field annotation in current DTO); the request body is `None` with Not applicable — no request body.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-FM-05-TC-API-002 — GET /api/v1/care-groups/{careGroupId}/checklists/history → listHistory contract

| Field | Specification |
| --- | --- |
| Stable ID | `UC-FM-05-TC-API-002` |
| Severity | `High` |
| Test Condition | `COND-API-002` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Backend` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/today/controller/CareGroupChecklistController.java` |
| Preconditions | Synthetic `Authorized Family Member / Mother` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | Planned — `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/checklist/today/controller/CareGroupChecklistControllerTest.java` (not present at Draft baseline) |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-API-002`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Invoke `GET /api/v1/care-groups/{careGroupId}/checklists/history` so `listHistory` receives path `careGroupId`: `UUID`; query `page`: `int`; query `size`: `int`; query `targetSubject`: `ChecklistTargetSubject`; principal `principal`: `Principal`; satisfy `hasRole('FAMILY')` and the extracted `None` constraints.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | Success status `200` returns `ResponseEntity<ApiResponse<ChecklistHistoryPageResponse>>` with payload fields `items`: `List<ChecklistHistoryItemResponse>` (no field annotation in current DTO); `page`: `int` (no field annotation in current DTO); `size`: `int` (no field annotation in current DTO); `totalElements`: `long` (no field annotation in current DTO); `totalPages`: `int` (no field annotation in current DTO); the request body is `None` with Not applicable — no request body. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/today/controller/CareGroupChecklistController.java` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-FM-05` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (Success status `200` returns `ResponseEntity<ApiResponse<ChecklistHistoryPageResponse>>` with payload fields `items`: `List<ChecklistHistoryItemResponse>` (no field annotation in current DTO); `page`: `int` (no field annotation in current DTO); `size`: `int` (no field annotation in current DTO); `totalElements`: `long` (no field annotation in current DTO); `totalPages`: `int` (no field annotation in current DTO); the request body is `None` with Not applicable — no request body.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-FM-05-TC-API-003 — POST /api/v1/care-groups/{careGroupId}/checklists/tasks/{taskId}/actions → applyAction contract

| Field | Specification |
| --- | --- |
| Stable ID | `UC-FM-05-TC-API-003` |
| Severity | `High` |
| Test Condition | `COND-API-003` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Backend` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/today/controller/CareGroupChecklistController.java` |
| Preconditions | Synthetic `Authorized Family Member / Mother` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | Planned — `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/checklist/today/controller/CareGroupChecklistControllerTest.java` (not present at Draft baseline) |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-API-003`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Invoke `POST /api/v1/care-groups/{careGroupId}/checklists/tasks/{taskId}/actions` so `applyAction` receives path `careGroupId`: `UUID`; path `taskId`: `UUID`; body `request`: `TaskActionRequest`; principal `principal`: `Principal`; satisfy `hasRole('FAMILY')` and the extracted `TaskActionRequest` constraints.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | Success status `the success status explicitly produced by the handler` returns `CurrentChecklistActionResponse` with payload fields `taskId`: `UUID` (no field annotation in current DTO); `instanceId`: `UUID` (no field annotation in current DTO); `action`: `TaskAction` (no field annotation in current DTO); `previousStatus`: `String` (no field annotation in current DTO); `status`: `String` (no field annotation in current DTO); `appliedAt`: `Instant` (no field annotation in current DTO); `idempotentReplay`: `boolean` (no field annotation in current DTO); `correlationId`: `UUID` (no field annotation in current DTO); the request body is `TaskActionRequest` with `action`: `TaskAction` (@NotNull); `clientRequestId`: `UUID` (@NotNull); `reason`: `String` (@Pattern(regexp = "^[A-Z0-9_]{1,80}$")). | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/today/controller/CareGroupChecklistController.java` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-FM-05` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (Success status `the success status explicitly produced by the handler` returns `CurrentChecklistActionResponse` with payload fields `taskId`: `UUID` (no field annotation in current DTO); `instanceId`: `UUID` (no field annotation in current DTO); `action`: `TaskAction` (no field annotation in current DTO); `previousStatus`: `String` (no field annotation in current DTO); `status`: `String` (no field annotation in current DTO); `appliedAt`: `Instant` (no field annotation in current DTO); `idempotentReplay`: `boolean` (no field annotation in current DTO); `correlationId`: `UUID` (no field annotation in current DTO); the request body is `TaskActionRequest` with `action`: `TaskAction` (@NotNull); `clientRequestId`: `UUID` (@NotNull); `reason`: `String` (@Pattern(regexp = "^[A-Z0-9_]{1,80}$")).), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-FM-05-TC-API-003-VAL — POST /api/v1/care-groups/{careGroupId}/checklists/tasks/{taskId}/actions rejects a declared request-field boundary

| Field | Specification |
| --- | --- |
| Stable ID | `UC-FM-05-TC-API-003-VAL` |
| Severity | `High` |
| Test Condition | `COND-API-003-VAL` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Backend` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/today/controller/CareGroupChecklistController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/common/exception/GlobalExceptionHandler.java#handleMethodArgumentNotValid` |
| Preconditions | Synthetic `Authorized Family Member / Mother` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | Planned — `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/checklist/today/controller/CareGroupChecklistControllerTest.java` (not present at Draft baseline) |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-API-003-VAL`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Submit `TaskActionRequest` with one field violating its cited validator: `action`: `TaskAction` (@NotNull); `clientRequestId`: `UUID` (@NotNull); `reason`: `String` (@Pattern(regexp = "^[A-Z0-9_]{1,80}$"))

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | HTTP `400` returns the current `ErrorResponse` with `status=400`, `error=VALIDATION_ERROR`, `message=Invalid request`, the request path, and field details with sensitive rejected values masked; the handler/service is not invoked. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/today/controller/CareGroupChecklistController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/common/exception/GlobalExceptionHandler.java#handleMethodArgumentNotValid` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-FM-05` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (HTTP `400` returns the current `ErrorResponse` with `status=400`, `error=VALIDATION_ERROR`, `message=Invalid request`, the request path, and field details with sensitive rejected values masked; the handler/service is not invoked.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-FM-05-TC-API-004 — GET /api/v1/care-groups/{careGroupId}/quick-notes → getHistory contract

| Field | Specification |
| --- | --- |
| Stable ID | `UC-FM-05-TC-API-004` |
| Severity | `High` |
| Test Condition | `COND-API-004` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Backend` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/FamilyQuickNoteController.java` |
| Preconditions | Synthetic `Authorized Family Member / Mother` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/service/FamilyQuickNoteServiceTest.java` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-API-004`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Invoke `GET /api/v1/care-groups/{careGroupId}/quick-notes` so `getHistory` receives path `careGroupId`: `UUID`; query `metricType`: `MetricType`; query `from`: `Instant`; query `to`: `Instant`; principal `principal`: `Principal`; satisfy `hasAnyRole('MOTHER', 'FAMILY')` and the extracted `None` constraints.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | Success status `200` returns `ResponseEntity<ApiResponse<MetricTrendResponse>>` with payload fields `metricType`: `String` (no field annotation in current DTO); `unit`: `String` (no field annotation in current DTO); `dataPoints`: `List<MetricDataPoint>` (no field annotation in current DTO); `disclaimer`: `String` (no field annotation in current DTO); the request body is `None` with Not applicable — no request body. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/FamilyQuickNoteController.java` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-FM-05` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (Success status `200` returns `ResponseEntity<ApiResponse<MetricTrendResponse>>` with payload fields `metricType`: `String` (no field annotation in current DTO); `unit`: `String` (no field annotation in current DTO); `dataPoints`: `List<MetricDataPoint>` (no field annotation in current DTO); `disclaimer`: `String` (no field annotation in current DTO); the request body is `None` with Not applicable — no request body.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-FM-05-TC-API-005 — GET /api/v1/care-groups/{groupId}/shared-data → getSharedData contract

| Field | Specification |
| --- | --- |
| Stable ID | `UC-FM-05-TC-API-005` |
| Severity | `High` |
| Test Condition | `COND-API-005` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Backend` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/SharedDataController.java` |
| Preconditions | Synthetic `Authorized Family Member / Mother` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/service/FamilyDashboardServiceTest.java` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-API-005`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Invoke `GET /api/v1/care-groups/{groupId}/shared-data` so `getSharedData` receives path `groupId`: `UUID`; query `category`: `String`; query `page`: `int`; query `size`: `int`; principal `principal`: `Principal`; satisfy `isAuthenticated()` and the extracted `None` constraints.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | Success status `200` returns `ResponseEntity<ApiResponse<SharedDataResponse>>` with payload fields `groupId`: `UUID` (no field annotation in current DTO); `category`: `String` (no field annotation in current DTO); `totalItems`: `int` (no field annotation in current DTO); `items`: `List<SharedDataItemDto>` (no field annotation in current DTO); `asOf`: `Instant` (no field annotation in current DTO); the request body is `None` with Not applicable — no request body. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/SharedDataController.java` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-FM-05` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (Success status `200` returns `ResponseEntity<ApiResponse<SharedDataResponse>>` with payload fields `groupId`: `UUID` (no field annotation in current DTO); `category`: `String` (no field annotation in current DTO); `totalItems`: `int` (no field annotation in current DTO); `items`: `List<SharedDataItemDto>` (no field annotation in current DTO); `asOf`: `Instant` (no field annotation in current DTO); the request body is `None` with Not applicable — no request body.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-FM-05-TC-API-006 — GET /api/v1/family/dashboard → getDashboard contract

| Field | Specification |
| --- | --- |
| Stable ID | `UC-FM-05-TC-API-006` |
| Severity | `High` |
| Test Condition | `COND-API-006` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Backend` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/FamilyDashboardController.java` |
| Preconditions | Synthetic `Authorized Family Member / Mother` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | Planned — `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/controller/FamilyDashboardControllerTest.java` (not present at Draft baseline) |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-API-006`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Invoke `GET /api/v1/family/dashboard` so `getDashboard` receives query `selectedCareGroupId`: `UUID`; principal `principal`: `Principal`; satisfy `isAuthenticated()` and the extracted `None` constraints.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | Success status `200` returns `ResponseEntity<ApiResponse<FamilyDashboardResponse>>` with payload fields `groups`: `List<Group>` (no field annotation in current DTO); `globalAggregate`: `Aggregate` (no field annotation in current DTO); `selectedCareGroupId`: `UUID` (no field annotation in current DTO); `selectedGroupDetail`: `Detail` (no field annotation in current DTO); the request body is `None` with Not applicable — no request body. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/FamilyDashboardController.java` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-FM-05` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (Success status `200` returns `ResponseEntity<ApiResponse<FamilyDashboardResponse>>` with payload fields `groups`: `List<Group>` (no field annotation in current DTO); `globalAggregate`: `Aggregate` (no field annotation in current DTO); `selectedCareGroupId`: `UUID` (no field annotation in current DTO); `selectedGroupDetail`: `Detail` (no field annotation in current DTO); the request body is `None` with Not applicable — no request body.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-FM-05-TC-BR-001 — Enforce business rule 1

| Field | Specification |
| --- | --- |
| Stable ID | `UC-FM-05-TC-BR-001` |
| Severity | `High` |
| Test Condition | `COND-BR-01` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Backend` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `TDS BR-01; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/today/controller/CareGroupChecklistController.java` |
| Preconditions | Synthetic `Authorized Family Member / Mother` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/service/FamilyDashboardServiceTest.java` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-BR-01`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Exercise the eligible and ineligible partitions for this rule: Membership, permission, and consent are rechecked for every projection.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | The server enforces: Membership, permission, and consent are rechecked for every projection. No disallowed state or protected data is produced. | `TDS BR-01; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/today/controller/CareGroupChecklistController.java` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-FM-05` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (The server enforces: Membership, permission, and consent are rechecked for every projection. No disallowed state or protected data is produced.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-FM-05-TC-BR-002 — Enforce business rule 2

| Field | Specification |
| --- | --- |
| Stable ID | `UC-FM-05-TC-BR-002` |
| Severity | `High` |
| Test Condition | `COND-BR-02` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Backend` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `TDS BR-02; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/FamilyDashboardController.java` |
| Preconditions | Synthetic `Authorized Family Member / Mother` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/service/SharedDataServiceImplTest.java` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-BR-02`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Exercise the eligible and ineligible partitions for this rule: Family emergency alerts are specified under UC-ES-03, not duplicated here.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | The server enforces: Family emergency alerts are specified under UC-ES-03, not duplicated here. No disallowed state or protected data is produced. | `TDS BR-02; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/FamilyDashboardController.java` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-FM-05` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (The server enforces: Family emergency alerts are specified under UC-ES-03, not duplicated here. No disallowed state or protected data is produced.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-FM-05-TC-SEC-001 — Reject wrong authentication, role, ownership, membership, or consent scope

| Field | Specification |
| --- | --- |
| Stable ID | `UC-FM-05-TC-SEC-001` |
| Severity | `Critical` |
| Test Condition | `COND-AUTH` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Backend` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `TDS Sections 4 and 16; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/FamilyDashboardController.java` |
| Preconditions | Synthetic `Authorized Family Member / Mother` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/service/FamilyDashboardServiceTest.java` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-AUTH`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Invoke every protected operation with an unauthenticated principal and the closest disallowed role/scope partition.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | The request fails closed using the exact current mapped status/code and returns no protected resource fields or side effects. | `TDS Sections 4 and 16; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/FamilyDashboardController.java` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-FM-05` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (The request fails closed using the exact current mapped status/code and returns no protected resource fields or side effects.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-FM-05-TC-GAP-001 — Prove the current actor entry path and owning contract remain reachable

| Field | Specification |
| --- | --- |
| Stable ID | `UC-FM-05-TC-GAP-001` |
| Severity | `Medium` |
| Test Condition | `COND-GAP` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Backend` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `SRS UC-FM-05 Implemented Entry Points/Contracts; 05_Development/CareBridgeMobileApp/lib/features/home/screens/family_member_home_screen.dart` |
| Preconditions | Synthetic `Authorized Family Member / Mother` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/service/FamilyDashboardServiceTest.java` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-GAP`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Start from `Family home, care-group detail, shared-data, and quick-note history screens`, perform `Select an authorized care group.`, and observe the owning contract `GET `/api/v1/family/dashboard`` rather than a retired or unrelated route.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | The current entry path remains reachable for `Monitor Shared Family Care` and invokes only the documented owning contract; an unreachable, static, or wrong-method path fails this case. | `SRS UC-FM-05 Implemented Entry Points/Contracts; 05_Development/CareBridgeMobileApp/lib/features/home/screens/family_member_home_screen.dart` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-FM-05` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (The current entry path remains reachable for `Monitor Shared Family Care` and invokes only the documented owning contract; an unreachable, static, or wrong-method path fails this case.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### 4.3 Coverage Families

| Behavior family | Conditions | Cases |
| --- | --- | --- |
| Select an authorized care group. | `COND-01` | `UC-FM-05-TC-001` |
| Load dashboard/shared-data/checklist projections constrained by permissions. | `COND-02` | `UC-FM-05-TC-002` |
| Apply an allowed checklist action or review/add a quick note and refresh history. | `COND-03` | `UC-FM-05-TC-003` |
| GET /api/v1/care-groups/{careGroupId}/checklists/current/tasks → getCurrentTasks contract | `COND-API-001` | `UC-FM-05-TC-API-001` |
| GET /api/v1/care-groups/{careGroupId}/checklists/history → listHistory contract | `COND-API-002` | `UC-FM-05-TC-API-002` |
| POST /api/v1/care-groups/{careGroupId}/checklists/tasks/{taskId}/actions → applyAction contract | `COND-API-003` | `UC-FM-05-TC-API-003` |
| POST /api/v1/care-groups/{careGroupId}/checklists/tasks/{taskId}/actions rejects a declared request-field boundary | `COND-API-003-VAL` | `UC-FM-05-TC-API-003-VAL` |
| GET /api/v1/care-groups/{careGroupId}/quick-notes → getHistory contract | `COND-API-004` | `UC-FM-05-TC-API-004` |
| GET /api/v1/care-groups/{groupId}/shared-data → getSharedData contract | `COND-API-005` | `UC-FM-05-TC-API-005` |
| GET /api/v1/family/dashboard → getDashboard contract | `COND-API-006` | `UC-FM-05-TC-API-006` |
| Business-rule partitions | `COND-BR-*` | `UC-FM-05-TC-BR-*` |
| Authentication / authorization / ownership / consent | `COND-AUTH` | `UC-FM-05-TC-SEC-001` |
| Current gap / reachability boundary | `COND-GAP` | `UC-FM-05-TC-GAP-001` |

## 5. Red-Green-Refactor Tracker

| TC ID | Intended file | Red evidence | Green evidence | Refactor verification | Status |
| --- | --- | --- | --- | --- | --- |
| `UC-FM-05-TC-001` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/service/FamilyDashboardServiceTest.java` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-FM-05-TC-002` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/service/FamilyDashboardServiceTest.java` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-FM-05-TC-003` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/service/FamilyDashboardServiceTest.java` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-FM-05-TC-API-001` | Planned — `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/checklist/today/controller/CareGroupChecklistControllerTest.java` (not present at Draft baseline) | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-FM-05-TC-API-002` | Planned — `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/checklist/today/controller/CareGroupChecklistControllerTest.java` (not present at Draft baseline) | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-FM-05-TC-API-003` | Planned — `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/checklist/today/controller/CareGroupChecklistControllerTest.java` (not present at Draft baseline) | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-FM-05-TC-API-003-VAL` | Planned — `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/checklist/today/controller/CareGroupChecklistControllerTest.java` (not present at Draft baseline) | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-FM-05-TC-API-004` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/service/FamilyQuickNoteServiceTest.java` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-FM-05-TC-API-005` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/service/FamilyDashboardServiceTest.java` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-FM-05-TC-API-006` | Planned — `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/controller/FamilyDashboardControllerTest.java` (not present at Draft baseline) | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-FM-05-TC-BR-001` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/service/FamilyDashboardServiceTest.java` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-FM-05-TC-BR-002` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/service/FamilyDashboardServiceTest.java` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-FM-05-TC-SEC-001` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/service/FamilyDashboardServiceTest.java` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-FM-05-TC-GAP-001` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/service/FamilyDashboardServiceTest.java` | Not run | Not run | Not run | 🔴 Not written / rerun required |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

1. Write the narrowest applicable test from Section 4.
2. Run the exact repository-supported command.
3. Confirm the intended behavioral failure, not setup/environment noise.
4. Record command, revision, time, environment, and failure signature.
5. Implement the smallest approved change during implementation, not specification.
6. Rerun for Green and then run affected suites after refactor.

No row above is Green until current execution evidence is recorded.

## 6. Entry, Exit, and Suspension Criteria

### Entry

- [ ] Paired TDS has all 17 sections.
- [ ] Exact DTO fields, error codes, states, auth, events, and schema used by a test are sourced or explicitly Open.
- [ ] Design-changing contradictions/gaps are resolved before implementation.
- [ ] Synthetic fixtures and deterministic provider/device controls are available.

### Exit

- [ ] Every operation and business rule maps to a condition and detailed TC.
- [ ] Every applicable critical/high case has current Red/Green/refactor evidence.
- [ ] Targeted and affected suites pass with recorded commands/counts.
- [ ] No real protected data/secrets appear in fixtures, logs, or snapshots.
- [ ] Reviewer/approver sign-off is recorded.

### Suspension

Suspend when the expected behavior/source conflicts, the environment cannot distinguish product failure, testing risks real credentials/protected data, or a destructive/shared migration procedure would be required. Resume only after the oracle/environment/safe procedure is restored.

## 7. Rollback Plan

| Artifact / risk | Safe action | Verification |
| --- | --- | --- |
| Test code | Revert only feature-owned tests/factories on the working branch. | Existing focused baseline returns to its prior result. |
| Fixtures/config | Restore versioned test config; remove only feature-owned synthetic data. | Unrelated suites remain isolated. |
| Client/server change | Use the paired TDS rollback runbook and preserve compatible contracts. | Targeted contract/UI tests. |
| Schema | Use additive corrective migration or recreate isolated test DB; never edit applied Flyway history. | Migration validation and integrity checks. |
| Provider | Disable optional integration/use approved degraded behavior only. | Fake/sandbox contract test. |

## 8. CASE 2.0 Anti-Pattern Detection

| Anti-pattern | Required evidence | Draft result |
| --- | --- | --- |
| Hallucinated oracle | Every expected row cites SRS/TDS/exact current source. | Pass for extracted handler/DTO/status expectations; unresolved service-only codes remain explicitly identified in paired TDS Section 10. |
| Generic test matrix | Case titles/actions reference `Monitor Shared Family Care` operations and rules. | Pass |
| False Green claim | Current command/time/count evidence is required. | Pass — all rows remain Red/not rerun. |
| Hidden contradiction | Section 2 records each known gap. | Resolved broad-boundary issue recorded |
| Missing Props Isolation | Applicable Java/TS/Dart factory pattern is present. | Pass at specification level |
| Cross-test pollution | TDS-05 defines actor/resource/provider cleanup. | Draft gate — implementation review must prove teardown/rollback before Green evidence is accepted |
| Wrong-layer test | Applicability matrix marks absent consumers/layers Not applicable. | Pass |
| Uncovered contract | Operations/rules/auth/gap map to conditions and detailed TCs. | Pass for handler/DTO/status/operation/rule/auth/gap mappings; service-only events/codes remain visible in paired TDS |
| Unsafe data | Synthetic-only rule; no production credentials/protected data. | Pass at specification level |
| AI safety bypass | Deterministic policy cannot be lowered by model output when AI applies. | Not applicable — no clinical/moderation generation in this UC |

- [ ] Human reviewer confirms all eight sections, oracle sources, detailed TCs, applicability, Red Gate, rollback, and paired-TDS traceability before approval.
