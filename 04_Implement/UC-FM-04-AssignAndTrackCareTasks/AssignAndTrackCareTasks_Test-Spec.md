# ENGINEERING DOCUMENTATION STANDARD (EDS) v2.0

# TEST SPECIFICATION — Assign and Track Family Care Tasks

| Field | Value |
| --- | --- |
| Document ID | `UC-FM-04-TEST-SPEC` |
| Version | `0.1` |
| Date | `2026-08-23` |
| Status | `Draft` |
| Feature / Gap ID | `UC-FM-04` |
| Module | `Family Cooperative Care` |
| Paired TDS | `UC-FM-04-TDS` |
| Priority | `Medium` |
| Platforms | `Mobile / Backend` |
| Data Classification | `Restricted care-group relationship, shared-care task, appointment, note, permission, and consent-scoped data` |
| Compliance Scope | `PDPA relationship/consent scoping, least privilege, cross-member isolation, and auditability of permission-changing actions` |
| Owner | `CareBridge Team` |
| Reviewer / Approver |  |
| Source Baseline | Current worktree `2026-08-23`; SRS/TDS `UC-FM-04` and exact code/test sources below |

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
| Actor goal | Assign a care task to an eligible member, view it, update status/details, or cancel it through the task lifecycle. | SRS `UC-FM-04` |
| Current state | `High` confidence; gaps are listed in Section 2 | Exact current code/test sources below |
| Entry points | Mobile Assigned Tasks screen | Current client/router evidence |
| Authorization boundary | `Mother / Authorized Family` plus exact authentication/role/ownership/membership/consent policy | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` |
| Primary operations | Create a task for an eligible group/member.; Load assigned/owned tasks and current state.; Update status/details or cancel using an allowed transition. | SRS `UC-FM-04` Normal Flow |
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
| `SRC-SRS` | Requirement | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-FM-04 | 2026-08-23 | Draft code-first requirement |
| `SRC-TDS` | Design | Paired `UC-FM-04-TDS` | 0.1 | Draft design |
| `SRC-CODE-01` | Current code | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-02` | Current code | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/service/impl/CareTaskServiceImpl.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-03` | Current code | `05_Development/CareBridgeMobileApp/lib/features/familySync/screens/assigned_tasks_screen.dart` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-01` | Existing test | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/CareTaskAssignmentIntegrationTest.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-02` | Existing test | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/service/CareTaskServiceImplTest.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-03` | Existing test | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/entity/CareTaskStatusFsmTest.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-ERROR-01` | Current exception advice | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/common/exception/GlobalExceptionHandler.java` | Worktree `2026-08-23` | Current-state evidence |

## 2. Logic Issues Resolved

| Issue ID | Discrepancy | Impact | Resolution | Oracle | Status |
| --- | --- | --- | --- | --- | --- |
| `LI-01` | Broad 43-UC catalogue previously obscured this boundary | Generic TCs could not map to `Assign and Track Family Care Tasks` | Split as `UC-FM-04` using the audited current code boundary | SRS 3.1 and `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` | Resolved in Draft |

Architecture-, schema-, authorization-, and test-changing Open items must be resolved before implementation approval.

## 3. Test Design Specification

### TDS-01 — Risk-Based Scope

| Risk ID | Failure mode | Severity | Likelihood | Detectability | Levels | Conditions |
| --- | --- | --- | --- | --- | --- | --- |
| `RISK-01` | Failure of: Create a task for an eligible group/member. | High | Medium | High | Unit/Integration/Contract/applicable UI | `COND-01` |
| `RISK-02` | Failure of: Load assigned/owned tasks and current state. | High | Medium | High | Unit/Integration/Contract/applicable UI | `COND-02` |
| `RISK-03` | Failure of: Update status/details or cancel using an allowed transition. | High | Medium | High | Unit/Integration/Contract/applicable UI | `COND-03` |
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
| `BASIS-01` | `UC-FM-04-FR-01` — Create a task for an eligible group/member. | SRS `UC-FM-04` Normal Flow 1; TDS Section 2 | Create a task for an eligible group/member. | `COND-01` |
| `BASIS-02` | `UC-FM-04-FR-02` — Load assigned/owned tasks and current state. | SRS `UC-FM-04` Normal Flow 2; TDS Section 2 | Load assigned/owned tasks and current state. | `COND-02` |
| `BASIS-03` | `UC-FM-04-FR-03` — Update status/details or cancel using an allowed transition. | SRS `UC-FM-04` Normal Flow 3; TDS Section 2 | Update status/details or cancel using an allowed transition. | `COND-03` |

Oracle precedence: approved user decision → approved BR/ADR/security policy → paired TDS → current implementation for characterization → existing test as regression evidence.

### TDS-03 — Test Conditions and Coverage Items

| Condition | Basis / risk | Behavior | Layer | Coverage | Test cases |
| --- | --- | --- | --- | --- | --- |
| `COND-01` | `BASIS-01` / `RISK-01` | Create a task for an eligible group/member. | Mobile / Backend | Positive + applicable boundary/state coverage | `UC-FM-04-TC-001` |
| `COND-02` | `BASIS-02` / `RISK-02` | Load assigned/owned tasks and current state. | Mobile / Backend | Positive + applicable boundary/state coverage | `UC-FM-04-TC-002` |
| `COND-03` | `BASIS-03` / `RISK-03` | Update status/details or cancel using an allowed transition. | Mobile / Backend | Positive + applicable boundary/state coverage | `UC-FM-04-TC-003` |
| `COND-API-001` | Exact handler/client composition contract | GET /api/v1/care-groups/{groupId}/tasks → listTasks contract | Applicable backend/client contract layer | Contract + DTO/status/authorization evidence | `UC-FM-04-TC-API-001` |
| `COND-API-002` | Exact handler/client composition contract | POST /api/v1/care-groups/{groupId}/tasks → assignTask contract | Applicable backend/client contract layer | Contract + DTO/status/authorization evidence | `UC-FM-04-TC-API-002` |
| `COND-API-002-VAL` | Exact handler/client composition contract | POST /api/v1/care-groups/{groupId}/tasks rejects a declared request-field boundary | Applicable backend/client contract layer | Contract + DTO/status/authorization evidence | `UC-FM-04-TC-API-002-VAL` |
| `COND-API-003` | Exact handler/client composition contract | GET /api/v1/care-groups/{groupId}/tasks/{taskId} → getTaskDetail contract | Applicable backend/client contract layer | Contract + DTO/status/authorization evidence | `UC-FM-04-TC-API-003` |
| `COND-API-004` | Exact handler/client composition contract | PATCH /api/v1/care-groups/{groupId}/tasks/{taskId} → updateTask contract | Applicable backend/client contract layer | Contract + DTO/status/authorization evidence | `UC-FM-04-TC-API-004` |
| `COND-API-005` | Exact handler/client composition contract | POST /api/v1/care-groups/{groupId}/tasks/{taskId}/cancel → cancelTask contract | Applicable backend/client contract layer | Contract + DTO/status/authorization evidence | `UC-FM-04-TC-API-005` |
| `COND-API-006` | Exact handler/client composition contract | PATCH /api/v1/care-groups/{groupId}/tasks/{taskId}/status → updateTaskStatus contract | Applicable backend/client contract layer | Contract + DTO/status/authorization evidence | `UC-FM-04-TC-API-006` |
| `COND-API-006-VAL` | Exact handler/client composition contract | PATCH /api/v1/care-groups/{groupId}/tasks/{taskId}/status rejects a declared request-field boundary | Applicable backend/client contract layer | Contract + DTO/status/authorization evidence | `UC-FM-04-TC-API-006-VAL` |
| `COND-BR-01` | `BR-01` | Assignee membership, ownership, and finite-state transitions are server authoritative. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/service/impl/CareTaskServiceImpl.java` | Negative / decision / state | `UC-FM-04-TC-BR-001` |
| `COND-BR-02` | `BR-02` | Retries cannot create duplicate assignments beyond current policy. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/service/impl/CareTaskServiceImpl.java` | Negative / decision / state | `UC-FM-04-TC-BR-002` |
| `COND-AUTH` | `RISK-AUTH` | Reject wrong authentication, role, ownership, membership, consent, or state scope | All protected layers | Security | `UC-FM-04-TC-SEC-001` |
| `COND-GAP` | `RISK-GAP` | Characterize current limitation/reachability without false completion | Applicable layer | Gap/Regression | `UC-FM-04-TC-GAP-001` |

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
| Actor | Synthetic `Mother / Authorized Family` plus closest wrong-role/cross-owner identities | Authenticated, unauthenticated, wrong scope, consent revoked | Reset principals/tokens |
| Resource | Minimum valid feature-owned object | Missing, malformed, boundary, stale, already-final, cross-owner | Transaction rollback or isolated repository cleanup |
| Provider/device | Deterministic fake only when applicable | Success, timeout, malformed, permission denied | Reset fake/timers/device state |
| Protected fields | Synthetic non-production values only | Redaction and disclosure checks | Never persist in snapshots/log fixtures |

Existing test evidence:

- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/CareTaskAssignmentIntegrationTest.java`
- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/service/CareTaskServiceImplTest.java`
- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/entity/CareTaskStatusFsmTest.java`

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

### UC-FM-04-TC-001 — Create a task for an eligible group/member

| Field | Specification |
| --- | --- |
| Stable ID | `UC-FM-04-TC-001` |
| Severity | `High` |
| Test Condition | `COND-01` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Backend` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `SRS UC-FM-04 Normal Flow 1; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` |
| Preconditions | Synthetic `Mother / Authorized Family` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/CareTaskAssignmentIntegrationTest.java` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-01`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Create a task for an eligible group/member.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | The observable outcome is exactly `Create a task for an eligible group/member.` The applicable handler, request/response type, status, and authorization evidence are those enumerated in paired TDS Sections 2, 9, 10, and 16; no additional state is claimed. | `SRS UC-FM-04 Normal Flow 1; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-FM-04` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (The observable outcome is exactly `Create a task for an eligible group/member.` The applicable handler, request/response type, status, and authorization evidence are those enumerated in paired TDS Sections 2, 9, 10, and 16; no additional state is claimed.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-FM-04-TC-002 — Load assigned/owned tasks and current state

| Field | Specification |
| --- | --- |
| Stable ID | `UC-FM-04-TC-002` |
| Severity | `High` |
| Test Condition | `COND-02` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Backend` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `SRS UC-FM-04 Normal Flow 2; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` |
| Preconditions | Synthetic `Mother / Authorized Family` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/service/CareTaskServiceImplTest.java` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-02`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Load assigned/owned tasks and current state.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | The observable outcome is exactly `Load assigned/owned tasks and current state.` The applicable handler, request/response type, status, and authorization evidence are those enumerated in paired TDS Sections 2, 9, 10, and 16; no additional state is claimed. | `SRS UC-FM-04 Normal Flow 2; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-FM-04` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (The observable outcome is exactly `Load assigned/owned tasks and current state.` The applicable handler, request/response type, status, and authorization evidence are those enumerated in paired TDS Sections 2, 9, 10, and 16; no additional state is claimed.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-FM-04-TC-003 — Update status/details or cancel using an allowed transition

| Field | Specification |
| --- | --- |
| Stable ID | `UC-FM-04-TC-003` |
| Severity | `High` |
| Test Condition | `COND-03` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Backend` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `SRS UC-FM-04 Normal Flow 3; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` |
| Preconditions | Synthetic `Mother / Authorized Family` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/entity/CareTaskStatusFsmTest.java` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-03`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Update status/details or cancel using an allowed transition.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | The observable outcome is exactly `Update status/details or cancel using an allowed transition.` The applicable handler, request/response type, status, and authorization evidence are those enumerated in paired TDS Sections 2, 9, 10, and 16; no additional state is claimed. | `SRS UC-FM-04 Normal Flow 3; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-FM-04` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (The observable outcome is exactly `Update status/details or cancel using an allowed transition.` The applicable handler, request/response type, status, and authorization evidence are those enumerated in paired TDS Sections 2, 9, 10, and 16; no additional state is claimed.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-FM-04-TC-API-001 — GET /api/v1/care-groups/{groupId}/tasks → listTasks contract

| Field | Specification |
| --- | --- |
| Stable ID | `UC-FM-04-TC-API-001` |
| Severity | `High` |
| Test Condition | `COND-API-001` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Backend` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` |
| Preconditions | Synthetic `Mother / Authorized Family` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/CareTaskAssignmentIntegrationTest.java` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-API-001`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Invoke `GET /api/v1/care-groups/{groupId}/tasks` so `listTasks` receives path `groupId`: `UUID`; principal `principal`: `Principal`; satisfy `isAuthenticated()` and the extracted `None` constraints.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | Success status `200` returns `ResponseEntity<ApiResponse<CareTasksResponse>>` with payload fields `groupId`: `UUID` (no field annotation in current DTO); `totalTasks`: `int` (no field annotation in current DTO); `tasks`: `List<CareTaskDto>` (no field annotation in current DTO); the request body is `None` with Not applicable — no request body. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-FM-04` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (Success status `200` returns `ResponseEntity<ApiResponse<CareTasksResponse>>` with payload fields `groupId`: `UUID` (no field annotation in current DTO); `totalTasks`: `int` (no field annotation in current DTO); `tasks`: `List<CareTaskDto>` (no field annotation in current DTO); the request body is `None` with Not applicable — no request body.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-FM-04-TC-API-002 — POST /api/v1/care-groups/{groupId}/tasks → assignTask contract

| Field | Specification |
| --- | --- |
| Stable ID | `UC-FM-04-TC-API-002` |
| Severity | `High` |
| Test Condition | `COND-API-002` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Backend` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` |
| Preconditions | Synthetic `Mother / Authorized Family` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | Planned — `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/controller/CareGroupControllerTest.java` (not present at Draft baseline) |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-API-002`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Invoke `POST /api/v1/care-groups/{groupId}/tasks` so `assignTask` receives path `groupId`: `UUID`; body `request`: `AssignFamilyTaskRequest`; principal `principal`: `Principal`; satisfy `hasRole('MOTHER')` and the extracted `AssignFamilyTaskRequest` constraints.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | Success status `201` returns `ResponseEntity<ApiResponse<AssignFamilyTaskResponse>>` with payload fields `careTaskId`: `UUID` (no field annotation in current DTO); `careGroupId`: `UUID` (no field annotation in current DTO); `assignedTo`: `UUID` (no field annotation in current DTO); `assignedBy`: `UUID` (no field annotation in current DTO); `title`: `String` (no field annotation in current DTO); `description`: `String` (no field annotation in current DTO); `dueAt`: `Instant` (no field annotation in current DTO); `status`: `String` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO); the request body is `AssignFamilyTaskRequest` with `assigneeMemberId`: `UUID` (@NotNull); `title`: `String` (@NotBlank, @Size(max = 255)); `description`: `String` (@Size(max = 2000)); `dueAt`: `Instant` (@NotNull); `targetSubject`: `ChecklistTargetSubject` (@NotNull). | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-FM-04` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (Success status `201` returns `ResponseEntity<ApiResponse<AssignFamilyTaskResponse>>` with payload fields `careTaskId`: `UUID` (no field annotation in current DTO); `careGroupId`: `UUID` (no field annotation in current DTO); `assignedTo`: `UUID` (no field annotation in current DTO); `assignedBy`: `UUID` (no field annotation in current DTO); `title`: `String` (no field annotation in current DTO); `description`: `String` (no field annotation in current DTO); `dueAt`: `Instant` (no field annotation in current DTO); `status`: `String` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO); the request body is `AssignFamilyTaskRequest` with `assigneeMemberId`: `UUID` (@NotNull); `title`: `String` (@NotBlank, @Size(max = 255)); `description`: `String` (@Size(max = 2000)); `dueAt`: `Instant` (@NotNull); `targetSubject`: `ChecklistTargetSubject` (@NotNull).), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-FM-04-TC-API-002-VAL — POST /api/v1/care-groups/{groupId}/tasks rejects a declared request-field boundary

| Field | Specification |
| --- | --- |
| Stable ID | `UC-FM-04-TC-API-002-VAL` |
| Severity | `High` |
| Test Condition | `COND-API-002-VAL` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Backend` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/common/exception/GlobalExceptionHandler.java#handleMethodArgumentNotValid` |
| Preconditions | Synthetic `Mother / Authorized Family` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | Planned — `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/controller/CareGroupControllerTest.java` (not present at Draft baseline) |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-API-002-VAL`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Submit `AssignFamilyTaskRequest` with one field violating its cited validator: `assigneeMemberId`: `UUID` (@NotNull); `title`: `String` (@NotBlank, @Size(max = 255)); `description`: `String` (@Size(max = 2000)); `dueAt`: `Instant` (@NotNull); `targetSubject`: `ChecklistTargetSubject` (@NotNull)

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | HTTP `400` returns the current `ErrorResponse` with `status=400`, `error=VALIDATION_ERROR`, `message=Invalid request`, the request path, and field details with sensitive rejected values masked; the handler/service is not invoked. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/common/exception/GlobalExceptionHandler.java#handleMethodArgumentNotValid` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-FM-04` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (HTTP `400` returns the current `ErrorResponse` with `status=400`, `error=VALIDATION_ERROR`, `message=Invalid request`, the request path, and field details with sensitive rejected values masked; the handler/service is not invoked.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-FM-04-TC-API-003 — GET /api/v1/care-groups/{groupId}/tasks/{taskId} → getTaskDetail contract

| Field | Specification |
| --- | --- |
| Stable ID | `UC-FM-04-TC-API-003` |
| Severity | `High` |
| Test Condition | `COND-API-003` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Backend` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` |
| Preconditions | Synthetic `Mother / Authorized Family` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | Planned — `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/controller/CareGroupControllerTest.java` (not present at Draft baseline) |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-API-003`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Invoke `GET /api/v1/care-groups/{groupId}/tasks/{taskId}` so `getTaskDetail` receives path `groupId`: `UUID`; path `taskId`: `UUID`; principal `principal`: `Principal`; satisfy `isAuthenticated()` and the extracted `None` constraints.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | Success status `200` returns `ResponseEntity<ApiResponse<CareTaskDetailResponse>>` with payload fields `careTaskId`: `UUID` (no field annotation in current DTO); `careGroupId`: `UUID` (no field annotation in current DTO); `title`: `String` (no field annotation in current DTO); `description`: `String` (no field annotation in current DTO); `dueAt`: `Instant` (no field annotation in current DTO); `status`: `String` (no field annotation in current DTO); `assignedTo`: `UUID` (no field annotation in current DTO); `assignedToName`: `String` (no field annotation in current DTO); `assignedBy`: `UUID` (no field annotation in current DTO); `assignedByName`: `String` (no field annotation in current DTO); `completedAt`: `Instant` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO); `updatedAt`: `Instant` (no field annotation in current DTO); the request body is `None` with Not applicable — no request body. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-FM-04` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (Success status `200` returns `ResponseEntity<ApiResponse<CareTaskDetailResponse>>` with payload fields `careTaskId`: `UUID` (no field annotation in current DTO); `careGroupId`: `UUID` (no field annotation in current DTO); `title`: `String` (no field annotation in current DTO); `description`: `String` (no field annotation in current DTO); `dueAt`: `Instant` (no field annotation in current DTO); `status`: `String` (no field annotation in current DTO); `assignedTo`: `UUID` (no field annotation in current DTO); `assignedToName`: `String` (no field annotation in current DTO); `assignedBy`: `UUID` (no field annotation in current DTO); `assignedByName`: `String` (no field annotation in current DTO); `completedAt`: `Instant` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO); `updatedAt`: `Instant` (no field annotation in current DTO); the request body is `None` with Not applicable — no request body.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-FM-04-TC-API-004 — PATCH /api/v1/care-groups/{groupId}/tasks/{taskId} → updateTask contract

| Field | Specification |
| --- | --- |
| Stable ID | `UC-FM-04-TC-API-004` |
| Severity | `High` |
| Test Condition | `COND-API-004` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Backend` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` |
| Preconditions | Synthetic `Mother / Authorized Family` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | Planned — `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/controller/CareGroupControllerTest.java` (not present at Draft baseline) |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-API-004`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Invoke `PATCH /api/v1/care-groups/{groupId}/tasks/{taskId}` so `updateTask` receives path `groupId`: `UUID`; path `taskId`: `UUID`; body `request`: `UpdateFamilyTaskRequest`; principal `principal`: `Principal`; satisfy `hasRole('MOTHER')` and the extracted `UpdateFamilyTaskRequest` constraints.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | Success status `200` returns `ResponseEntity<ApiResponse<UpdateFamilyTaskResponse>>` with payload fields `careTaskId`: `UUID` (no field annotation in current DTO); `careGroupId`: `UUID` (no field annotation in current DTO); `assignedTo`: `UUID` (no field annotation in current DTO); `assignedBy`: `UUID` (no field annotation in current DTO); `title`: `String` (no field annotation in current DTO); `description`: `String` (no field annotation in current DTO); `dueAt`: `Instant` (no field annotation in current DTO); `status`: `String` (no field annotation in current DTO); `completedAt`: `Instant` (no field annotation in current DTO); `updatedAt`: `Instant` (no field annotation in current DTO); the request body is `UpdateFamilyTaskRequest` with `title`: `String` (no field annotation in current DTO); `description`: `String` (no field annotation in current DTO); `dueAt`: `Instant` (no field annotation in current DTO); `assigneeMemberId`: `UUID` (no field annotation in current DTO). | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-FM-04` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (Success status `200` returns `ResponseEntity<ApiResponse<UpdateFamilyTaskResponse>>` with payload fields `careTaskId`: `UUID` (no field annotation in current DTO); `careGroupId`: `UUID` (no field annotation in current DTO); `assignedTo`: `UUID` (no field annotation in current DTO); `assignedBy`: `UUID` (no field annotation in current DTO); `title`: `String` (no field annotation in current DTO); `description`: `String` (no field annotation in current DTO); `dueAt`: `Instant` (no field annotation in current DTO); `status`: `String` (no field annotation in current DTO); `completedAt`: `Instant` (no field annotation in current DTO); `updatedAt`: `Instant` (no field annotation in current DTO); the request body is `UpdateFamilyTaskRequest` with `title`: `String` (no field annotation in current DTO); `description`: `String` (no field annotation in current DTO); `dueAt`: `Instant` (no field annotation in current DTO); `assigneeMemberId`: `UUID` (no field annotation in current DTO).), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-FM-04-TC-API-005 — POST /api/v1/care-groups/{groupId}/tasks/{taskId}/cancel → cancelTask contract

| Field | Specification |
| --- | --- |
| Stable ID | `UC-FM-04-TC-API-005` |
| Severity | `High` |
| Test Condition | `COND-API-005` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Backend` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` |
| Preconditions | Synthetic `Mother / Authorized Family` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | Planned — `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/controller/CareGroupControllerTest.java` (not present at Draft baseline) |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-API-005`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Invoke `POST /api/v1/care-groups/{groupId}/tasks/{taskId}/cancel` so `cancelTask` receives path `groupId`: `UUID`; path `taskId`: `UUID`; principal `principal`: `Principal`; satisfy `hasRole('MOTHER')` and the extracted `None` constraints.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | Success status `200` returns `ResponseEntity<ApiResponse<CancelFamilyTaskResponse>>` with payload fields `careTaskId`: `UUID` (no field annotation in current DTO); `careGroupId`: `UUID` (no field annotation in current DTO); `assignedTo`: `UUID` (no field annotation in current DTO); `assignedBy`: `UUID` (no field annotation in current DTO); `title`: `String` (no field annotation in current DTO); `status`: `String` (no field annotation in current DTO); `updatedAt`: `Instant` (no field annotation in current DTO); the request body is `None` with Not applicable — no request body. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-FM-04` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (Success status `200` returns `ResponseEntity<ApiResponse<CancelFamilyTaskResponse>>` with payload fields `careTaskId`: `UUID` (no field annotation in current DTO); `careGroupId`: `UUID` (no field annotation in current DTO); `assignedTo`: `UUID` (no field annotation in current DTO); `assignedBy`: `UUID` (no field annotation in current DTO); `title`: `String` (no field annotation in current DTO); `status`: `String` (no field annotation in current DTO); `updatedAt`: `Instant` (no field annotation in current DTO); the request body is `None` with Not applicable — no request body.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-FM-04-TC-API-006 — PATCH /api/v1/care-groups/{groupId}/tasks/{taskId}/status → updateTaskStatus contract

| Field | Specification |
| --- | --- |
| Stable ID | `UC-FM-04-TC-API-006` |
| Severity | `High` |
| Test Condition | `COND-API-006` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Backend` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` |
| Preconditions | Synthetic `Mother / Authorized Family` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | Planned — `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/controller/CareGroupControllerTest.java` (not present at Draft baseline) |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-API-006`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Invoke `PATCH /api/v1/care-groups/{groupId}/tasks/{taskId}/status` so `updateTaskStatus` receives path `groupId`: `UUID`; path `taskId`: `UUID`; body `request`: `UpdateTaskStatusRequest`; principal `principal`: `Principal`; satisfy `isAuthenticated()` and the extracted `UpdateTaskStatusRequest` constraints.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | Success status `200` returns `ResponseEntity<ApiResponse<UpdateTaskStatusResponse>>` with payload fields `careTaskId`: `UUID` (no field annotation in current DTO); `careGroupId`: `UUID` (no field annotation in current DTO); `status`: `String` (no field annotation in current DTO); `completedAt`: `Instant` (no field annotation in current DTO); `updatedAt`: `Instant` (no field annotation in current DTO); the request body is `UpdateTaskStatusRequest` with `status`: `String` (@NotBlank(message = "status must not be blank")). | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-FM-04` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (Success status `200` returns `ResponseEntity<ApiResponse<UpdateTaskStatusResponse>>` with payload fields `careTaskId`: `UUID` (no field annotation in current DTO); `careGroupId`: `UUID` (no field annotation in current DTO); `status`: `String` (no field annotation in current DTO); `completedAt`: `Instant` (no field annotation in current DTO); `updatedAt`: `Instant` (no field annotation in current DTO); the request body is `UpdateTaskStatusRequest` with `status`: `String` (@NotBlank(message = "status must not be blank")).), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-FM-04-TC-API-006-VAL — PATCH /api/v1/care-groups/{groupId}/tasks/{taskId}/status rejects a declared request-field boundary

| Field | Specification |
| --- | --- |
| Stable ID | `UC-FM-04-TC-API-006-VAL` |
| Severity | `High` |
| Test Condition | `COND-API-006-VAL` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Backend` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/common/exception/GlobalExceptionHandler.java#handleMethodArgumentNotValid` |
| Preconditions | Synthetic `Mother / Authorized Family` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | Planned — `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/controller/CareGroupControllerTest.java` (not present at Draft baseline) |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-API-006-VAL`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Submit `UpdateTaskStatusRequest` with one field violating its cited validator: `status`: `String` (@NotBlank(message = "status must not be blank"))

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | HTTP `400` returns the current `ErrorResponse` with `status=400`, `error=VALIDATION_ERROR`, `message=Invalid request`, the request path, and field details with sensitive rejected values masked; the handler/service is not invoked. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/common/exception/GlobalExceptionHandler.java#handleMethodArgumentNotValid` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-FM-04` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (HTTP `400` returns the current `ErrorResponse` with `status=400`, `error=VALIDATION_ERROR`, `message=Invalid request`, the request path, and field details with sensitive rejected values masked; the handler/service is not invoked.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-FM-04-TC-BR-001 — Enforce business rule 1

| Field | Specification |
| --- | --- |
| Stable ID | `UC-FM-04-TC-BR-001` |
| Severity | `High` |
| Test Condition | `COND-BR-01` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Backend` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `TDS BR-01; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/service/impl/CareTaskServiceImpl.java` |
| Preconditions | Synthetic `Mother / Authorized Family` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/CareTaskAssignmentIntegrationTest.java` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-BR-01`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Exercise the eligible and ineligible partitions for this rule: Assignee membership, ownership, and finite-state transitions are server authoritative.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | The server enforces: Assignee membership, ownership, and finite-state transitions are server authoritative. No disallowed state or protected data is produced. | `TDS BR-01; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/service/impl/CareTaskServiceImpl.java` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-FM-04` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (The server enforces: Assignee membership, ownership, and finite-state transitions are server authoritative. No disallowed state or protected data is produced.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-FM-04-TC-BR-002 — Enforce business rule 2

| Field | Specification |
| --- | --- |
| Stable ID | `UC-FM-04-TC-BR-002` |
| Severity | `High` |
| Test Condition | `COND-BR-02` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Backend` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `TDS BR-02; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/service/impl/CareTaskServiceImpl.java` |
| Preconditions | Synthetic `Mother / Authorized Family` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/service/CareTaskServiceImplTest.java` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-BR-02`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Exercise the eligible and ineligible partitions for this rule: Retries cannot create duplicate assignments beyond current policy.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | The server enforces: Retries cannot create duplicate assignments beyond current policy. No disallowed state or protected data is produced. | `TDS BR-02; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/service/impl/CareTaskServiceImpl.java` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-FM-04` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (The server enforces: Retries cannot create duplicate assignments beyond current policy. No disallowed state or protected data is produced.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-FM-04-TC-SEC-001 — Reject wrong authentication, role, ownership, membership, or consent scope

| Field | Specification |
| --- | --- |
| Stable ID | `UC-FM-04-TC-SEC-001` |
| Severity | `Critical` |
| Test Condition | `COND-AUTH` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Backend` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `TDS Sections 4 and 16; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` |
| Preconditions | Synthetic `Mother / Authorized Family` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/CareTaskAssignmentIntegrationTest.java` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-AUTH`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Invoke every protected operation with an unauthenticated principal and the closest disallowed role/scope partition.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | The request fails closed using the exact current mapped status/code and returns no protected resource fields or side effects. | `TDS Sections 4 and 16; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-FM-04` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (The request fails closed using the exact current mapped status/code and returns no protected resource fields or side effects.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-FM-04-TC-GAP-001 — Prove the current actor entry path and owning contract remain reachable

| Field | Specification |
| --- | --- |
| Stable ID | `UC-FM-04-TC-GAP-001` |
| Severity | `Medium` |
| Test Condition | `COND-GAP` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Backend` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `SRS UC-FM-04 Implemented Entry Points/Contracts; 05_Development/CareBridgeMobileApp/lib/features/familySync/screens/assigned_tasks_screen.dart` |
| Preconditions | Synthetic `Mother / Authorized Family` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/CareTaskAssignmentIntegrationTest.java` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-GAP`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Start from `Mobile Assigned Tasks screen`, perform `Create a task for an eligible group/member.`, and observe the owning contract `GET/POST `/api/v1/care-groups/{groupId}/tasks`` rather than a retired or unrelated route.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | The current entry path remains reachable for `Assign and Track Family Care Tasks` and invokes only the documented owning contract; an unreachable, static, or wrong-method path fails this case. | `SRS UC-FM-04 Implemented Entry Points/Contracts; 05_Development/CareBridgeMobileApp/lib/features/familySync/screens/assigned_tasks_screen.dart` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-FM-04` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (The current entry path remains reachable for `Assign and Track Family Care Tasks` and invokes only the documented owning contract; an unreachable, static, or wrong-method path fails this case.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### 4.3 Coverage Families

| Behavior family | Conditions | Cases |
| --- | --- | --- |
| Create a task for an eligible group/member. | `COND-01` | `UC-FM-04-TC-001` |
| Load assigned/owned tasks and current state. | `COND-02` | `UC-FM-04-TC-002` |
| Update status/details or cancel using an allowed transition. | `COND-03` | `UC-FM-04-TC-003` |
| GET /api/v1/care-groups/{groupId}/tasks → listTasks contract | `COND-API-001` | `UC-FM-04-TC-API-001` |
| POST /api/v1/care-groups/{groupId}/tasks → assignTask contract | `COND-API-002` | `UC-FM-04-TC-API-002` |
| POST /api/v1/care-groups/{groupId}/tasks rejects a declared request-field boundary | `COND-API-002-VAL` | `UC-FM-04-TC-API-002-VAL` |
| GET /api/v1/care-groups/{groupId}/tasks/{taskId} → getTaskDetail contract | `COND-API-003` | `UC-FM-04-TC-API-003` |
| PATCH /api/v1/care-groups/{groupId}/tasks/{taskId} → updateTask contract | `COND-API-004` | `UC-FM-04-TC-API-004` |
| POST /api/v1/care-groups/{groupId}/tasks/{taskId}/cancel → cancelTask contract | `COND-API-005` | `UC-FM-04-TC-API-005` |
| PATCH /api/v1/care-groups/{groupId}/tasks/{taskId}/status → updateTaskStatus contract | `COND-API-006` | `UC-FM-04-TC-API-006` |
| PATCH /api/v1/care-groups/{groupId}/tasks/{taskId}/status rejects a declared request-field boundary | `COND-API-006-VAL` | `UC-FM-04-TC-API-006-VAL` |
| Business-rule partitions | `COND-BR-*` | `UC-FM-04-TC-BR-*` |
| Authentication / authorization / ownership / consent | `COND-AUTH` | `UC-FM-04-TC-SEC-001` |
| Current gap / reachability boundary | `COND-GAP` | `UC-FM-04-TC-GAP-001` |

## 5. Red-Green-Refactor Tracker

| TC ID | Intended file | Red evidence | Green evidence | Refactor verification | Status |
| --- | --- | --- | --- | --- | --- |
| `UC-FM-04-TC-001` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/CareTaskAssignmentIntegrationTest.java` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-FM-04-TC-002` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/CareTaskAssignmentIntegrationTest.java` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-FM-04-TC-003` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/CareTaskAssignmentIntegrationTest.java` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-FM-04-TC-API-001` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/CareTaskAssignmentIntegrationTest.java` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-FM-04-TC-API-002` | Planned — `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/controller/CareGroupControllerTest.java` (not present at Draft baseline) | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-FM-04-TC-API-002-VAL` | Planned — `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/controller/CareGroupControllerTest.java` (not present at Draft baseline) | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-FM-04-TC-API-003` | Planned — `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/controller/CareGroupControllerTest.java` (not present at Draft baseline) | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-FM-04-TC-API-004` | Planned — `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/controller/CareGroupControllerTest.java` (not present at Draft baseline) | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-FM-04-TC-API-005` | Planned — `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/controller/CareGroupControllerTest.java` (not present at Draft baseline) | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-FM-04-TC-API-006` | Planned — `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/controller/CareGroupControllerTest.java` (not present at Draft baseline) | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-FM-04-TC-API-006-VAL` | Planned — `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/controller/CareGroupControllerTest.java` (not present at Draft baseline) | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-FM-04-TC-BR-001` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/CareTaskAssignmentIntegrationTest.java` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-FM-04-TC-BR-002` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/CareTaskAssignmentIntegrationTest.java` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-FM-04-TC-SEC-001` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/CareTaskAssignmentIntegrationTest.java` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-FM-04-TC-GAP-001` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/CareTaskAssignmentIntegrationTest.java` | Not run | Not run | Not run | 🔴 Not written / rerun required |

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
| Generic test matrix | Case titles/actions reference `Assign and Track Family Care Tasks` operations and rules. | Pass |
| False Green claim | Current command/time/count evidence is required. | Pass — all rows remain Red/not rerun. |
| Hidden contradiction | Section 2 records each known gap. | Resolved broad-boundary issue recorded |
| Missing Props Isolation | Applicable Java/TS/Dart factory pattern is present. | Pass at specification level |
| Cross-test pollution | TDS-05 defines actor/resource/provider cleanup. | Draft gate — implementation review must prove teardown/rollback before Green evidence is accepted |
| Wrong-layer test | Applicability matrix marks absent consumers/layers Not applicable. | Pass |
| Uncovered contract | Operations/rules/auth/gap map to conditions and detailed TCs. | Pass for handler/DTO/status/operation/rule/auth/gap mappings; service-only events/codes remain visible in paired TDS |
| Unsafe data | Synthetic-only rule; no production credentials/protected data. | Pass at specification level |
| AI safety bypass | Deterministic policy cannot be lowered by model output when AI applies. | Not applicable — no clinical/moderation generation in this UC |

- [ ] Human reviewer confirms all eight sections, oracle sources, detailed TCs, applicability, Red Gate, rollback, and paired-TDS traceability before approval.
