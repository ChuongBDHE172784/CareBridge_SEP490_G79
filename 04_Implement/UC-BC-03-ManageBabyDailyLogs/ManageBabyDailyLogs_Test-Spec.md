# ENGINEERING DOCUMENTATION STANDARD (EDS) v2.0

# TEST SPECIFICATION — Manage Baby Daily Logs

| Field | Value |
| --- | --- |
| Document ID | `UC-BC-03-TEST-SPEC` |
| Version | `0.1` |
| Date | `2026-08-23` |
| Status | `Draft` |
| Feature / Gap ID | `UC-BC-03` |
| Module | `Baby Care` |
| Paired TDS | `UC-BC-03-TDS` |
| Priority | `Medium` |
| Platforms | `Mobile / Web / Backend` |
| Data Classification | `Restricted child health, growth, milestone, vaccination, and daily-care data` |
| Compliance Scope | `PDPA child/health-data minimization, caregiver authorization, non-diagnostic presentation, and retention/audit controls` |
| Owner | `CareBridge Team` |
| Reviewer / Approver |  |
| Source Baseline | Current worktree `2026-08-23`; SRS/TDS `UC-BC-03` and exact code/test sources below |

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
| Actor goal | Create, view, edit, and remove eligible baby feeding, sleep, diaper, or other supported daily logs. | SRS `UC-BC-03` |
| Current state | `High` confidence; gaps are listed in Section 2 | Exact current code/test sources below |
| Entry points | Baby detail log modal and Mobile `/babies/:babyId/daily-logs/:logId*`; Web baby-care log forms | Current client/router evidence |
| Authorization boundary | `Mother / Authorized Caregiver` plus exact authentication/role/ownership/membership/consent policy | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/carejourney/controller/BabyDailyLogController.java` |
| Primary operations | Choose a supported log type and enter type-specific data.; Persist and inspect the log detail.; Edit or delete it when ownership/state permits. | SRS `UC-BC-03` Normal Flow |
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
| `SRC-SRS` | Requirement | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-BC-03 | 2026-08-23 | Draft code-first requirement |
| `SRC-TDS` | Design | Paired `UC-BC-03-TDS` | 0.1 | Draft design |
| `SRC-CODE-01` | Current code | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/carejourney/controller/BabyDailyLogController.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-02` | Current code | `05_Development/CareBridgeMobileApp/lib/features/baby/screens/edit_baby_daily_log_screen.dart` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-03` | Current code | `05_Development/CareBridgeWebApp/src/features/babyCare/pages/BabyCareHubPage.tsx` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-01` | Existing test | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/carejourney/BabyDailyLogServiceTest.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-ERROR-01` | Current exception advice | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/common/exception/GlobalExceptionHandler.java` | Worktree `2026-08-23` | Current-state evidence |

## 2. Logic Issues Resolved

| Issue ID | Discrepancy | Impact | Resolution | Oracle | Status |
| --- | --- | --- | --- | --- | --- |
| `LI-01` | Broad 43-UC catalogue previously obscured this boundary | Generic TCs could not map to `Manage Baby Daily Logs` | Split as `UC-BC-03` using the audited current code boundary | SRS 3.1 and `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/carejourney/controller/BabyDailyLogController.java` | Resolved in Draft |

Architecture-, schema-, authorization-, and test-changing Open items must be resolved before implementation approval.

## 3. Test Design Specification

### TDS-01 — Risk-Based Scope

| Risk ID | Failure mode | Severity | Likelihood | Detectability | Levels | Conditions |
| --- | --- | --- | --- | --- | --- | --- |
| `RISK-01` | Failure of: Choose a supported log type and enter type-specific data. | High | Medium | High | Unit/Integration/Contract/applicable UI | `COND-01` |
| `RISK-02` | Failure of: Persist and inspect the log detail. | High | Medium | High | Unit/Integration/Contract/applicable UI | `COND-02` |
| `RISK-03` | Failure of: Edit or delete it when ownership/state permits. | High | Medium | High | Unit/Integration/Contract/applicable UI | `COND-03` |
| `RISK-AUTH` | Cross-user/role/member/consent data access | Critical | Medium | Medium | Security/Integration/Contract | `COND-AUTH` |
| `RISK-GAP` | Documentation claims an unreachable or broken path as complete | High | Medium | High | Characterization/Contract/UI | `COND-GAP` |

#### Platform and Test-Level Applicability Matrix

| Platform / Layer | Unit | Integration | Contract / Component | Widget / UI | E2E | Security |
| --- | --- | --- | --- | --- | --- | --- |
| Backend | Applicable — current backend/API contracts | Applicable — current backend/API contracts | Applicable — current backend/API contracts | Not applicable — backend has no UI | Applicable — current backend/API contracts | Applicable — current backend/API contracts |
| Web | Applicable — current Web entry points | Applicable — current Web entry points | Applicable — current Web entry points | Applicable — current Web entry points | Applicable — current Web entry points | Applicable — current Web entry points |
| Mobile | Applicable — current Mobile entry points | Applicable — current Mobile entry points | Applicable — current Mobile entry points | Applicable — current Mobile entry points | Applicable — current Mobile entry points | Applicable — current Mobile entry points |
| AI Service | Not applicable — no Python AI contract in this UC | Not applicable — no Python AI contract in this UC | Not applicable — no Python AI contract in this UC | Not applicable — Python service has no actor UI | Not applicable — no Python AI contract in this UC | Not applicable — no Python AI contract in this UC |

### TDS-02 — Test Basis and Oracle Hierarchy

| Basis | Requirement / behavior | Exact source | Oracle | Conditions |
| --- | --- | --- | --- | --- |
| `BASIS-01` | `UC-BC-03-FR-01` — Choose a supported log type and enter type-specific data. | SRS `UC-BC-03` Normal Flow 1; TDS Section 2 | Choose a supported log type and enter type-specific data. | `COND-01` |
| `BASIS-02` | `UC-BC-03-FR-02` — Persist and inspect the log detail. | SRS `UC-BC-03` Normal Flow 2; TDS Section 2 | Persist and inspect the log detail. | `COND-02` |
| `BASIS-03` | `UC-BC-03-FR-03` — Edit or delete it when ownership/state permits. | SRS `UC-BC-03` Normal Flow 3; TDS Section 2 | Edit or delete it when ownership/state permits. | `COND-03` |

Oracle precedence: approved user decision → approved BR/ADR/security policy → paired TDS → current implementation for characterization → existing test as regression evidence.

### TDS-03 — Test Conditions and Coverage Items

| Condition | Basis / risk | Behavior | Layer | Coverage | Test cases |
| --- | --- | --- | --- | --- | --- |
| `COND-01` | `BASIS-01` / `RISK-01` | Choose a supported log type and enter type-specific data. | Mobile / Web / Backend | Positive + applicable boundary/state coverage | `UC-BC-03-TC-001` |
| `COND-02` | `BASIS-02` / `RISK-02` | Persist and inspect the log detail. | Mobile / Web / Backend | Positive + applicable boundary/state coverage | `UC-BC-03-TC-002` |
| `COND-03` | `BASIS-03` / `RISK-03` | Edit or delete it when ownership/state permits. | Mobile / Web / Backend | Positive + applicable boundary/state coverage | `UC-BC-03-TC-003` |
| `COND-API-001` | Exact handler/client composition contract | GET /api/v1/babies/{babyId}/daily-logs → getDailyLogs contract | Applicable backend/client contract layer | Contract + DTO/status/authorization evidence | `UC-BC-03-TC-API-001` |
| `COND-API-002` | Exact handler/client composition contract | POST /api/v1/babies/{babyId}/daily-logs → addDailyLog contract | Applicable backend/client contract layer | Contract + DTO/status/authorization evidence | `UC-BC-03-TC-API-002` |
| `COND-API-002-VAL` | Exact handler/client composition contract | POST /api/v1/babies/{babyId}/daily-logs rejects a declared request-field boundary | Applicable backend/client contract layer | Contract + DTO/status/authorization evidence | `UC-BC-03-TC-API-002-VAL` |
| `COND-API-003` | Exact handler/client composition contract | DELETE /api/v1/babies/{babyId}/daily-logs/{logId} → deleteLog contract | Applicable backend/client contract layer | Contract + DTO/status/authorization evidence | `UC-BC-03-TC-API-003` |
| `COND-API-004` | Exact handler/client composition contract | GET /api/v1/babies/{babyId}/daily-logs/{logId} → getDailyLogDetail contract | Applicable backend/client contract layer | Contract + DTO/status/authorization evidence | `UC-BC-03-TC-API-004` |
| `COND-API-005` | Exact handler/client composition contract | PUT /api/v1/babies/{babyId}/daily-logs/{logId} → updateLog contract | Applicable backend/client contract layer | Contract + DTO/status/authorization evidence | `UC-BC-03-TC-API-005` |
| `COND-API-005-VAL` | Exact handler/client composition contract | PUT /api/v1/babies/{babyId}/daily-logs/{logId} rejects a declared request-field boundary | Applicable backend/client contract layer | Contract + DTO/status/authorization evidence | `UC-BC-03-TC-API-005-VAL` |
| `COND-BR-01` | `BR-01` | Type-specific field validation and baby authorization are server authoritative. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/carejourney/controller/BabyDailyLogController.java` | Negative / decision / state | `UC-BC-03-TC-BR-001` |
| `COND-BR-02` | `BR-02` | Log time/order uses server-normalized values. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/carejourney/controller/BabyDailyLogController.java` | Negative / decision / state | `UC-BC-03-TC-BR-002` |
| `COND-AUTH` | `RISK-AUTH` | Reject wrong authentication, role, ownership, membership, consent, or state scope | All protected layers | Security | `UC-BC-03-TC-SEC-001` |
| `COND-GAP` | `RISK-GAP` | Characterize current limitation/reachability without false completion | Applicable layer | Gap/Regression | `UC-BC-03-TC-GAP-001` |

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
| Actor | Synthetic `Mother / Authorized Caregiver` plus closest wrong-role/cross-owner identities | Authenticated, unauthenticated, wrong scope, consent revoked | Reset principals/tokens |
| Resource | Minimum valid feature-owned object | Missing, malformed, boundary, stale, already-final, cross-owner | Transaction rollback or isolated repository cleanup |
| Provider/device | Deterministic fake only when applicable | Success, timeout, malformed, permission denied | Reset fake/timers/device state |
| Protected fields | Synthetic non-production values only | Redaction and disclosure checks | Never persist in snapshots/log fixtures |

Existing test evidence:

- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/carejourney/BabyDailyLogServiceTest.java`

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

### UC-BC-03-TC-001 — Choose a supported log type and enter type-specific data

| Field | Specification |
| --- | --- |
| Stable ID | `UC-BC-03-TC-001` |
| Severity | `High` |
| Test Condition | `COND-01` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Web / Backend` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `SRS UC-BC-03 Normal Flow 1; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/carejourney/controller/BabyDailyLogController.java` |
| Preconditions | Synthetic `Mother / Authorized Caregiver` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/carejourney/BabyDailyLogServiceTest.java` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-01`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Choose a supported log type and enter type-specific data.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | The observable outcome is exactly `Choose a supported log type and enter type-specific data.` The applicable handler, request/response type, status, and authorization evidence are those enumerated in paired TDS Sections 2, 9, 10, and 16; no additional state is claimed. | `SRS UC-BC-03 Normal Flow 1; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/carejourney/controller/BabyDailyLogController.java` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-BC-03` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (The observable outcome is exactly `Choose a supported log type and enter type-specific data.` The applicable handler, request/response type, status, and authorization evidence are those enumerated in paired TDS Sections 2, 9, 10, and 16; no additional state is claimed.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-BC-03-TC-002 — Persist and inspect the log detail

| Field | Specification |
| --- | --- |
| Stable ID | `UC-BC-03-TC-002` |
| Severity | `High` |
| Test Condition | `COND-02` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Web / Backend` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `SRS UC-BC-03 Normal Flow 2; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/carejourney/controller/BabyDailyLogController.java` |
| Preconditions | Synthetic `Mother / Authorized Caregiver` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/carejourney/BabyDailyLogServiceTest.java` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-02`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Persist and inspect the log detail.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | The observable outcome is exactly `Persist and inspect the log detail.` The applicable handler, request/response type, status, and authorization evidence are those enumerated in paired TDS Sections 2, 9, 10, and 16; no additional state is claimed. | `SRS UC-BC-03 Normal Flow 2; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/carejourney/controller/BabyDailyLogController.java` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-BC-03` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (The observable outcome is exactly `Persist and inspect the log detail.` The applicable handler, request/response type, status, and authorization evidence are those enumerated in paired TDS Sections 2, 9, 10, and 16; no additional state is claimed.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-BC-03-TC-003 — Edit or delete it when ownership/state permits

| Field | Specification |
| --- | --- |
| Stable ID | `UC-BC-03-TC-003` |
| Severity | `High` |
| Test Condition | `COND-03` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Web / Backend` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `SRS UC-BC-03 Normal Flow 3; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/carejourney/controller/BabyDailyLogController.java` |
| Preconditions | Synthetic `Mother / Authorized Caregiver` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/carejourney/BabyDailyLogServiceTest.java` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-03`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Edit or delete it when ownership/state permits.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | The observable outcome is exactly `Edit or delete it when ownership/state permits.` The applicable handler, request/response type, status, and authorization evidence are those enumerated in paired TDS Sections 2, 9, 10, and 16; no additional state is claimed. | `SRS UC-BC-03 Normal Flow 3; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/carejourney/controller/BabyDailyLogController.java` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-BC-03` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (The observable outcome is exactly `Edit or delete it when ownership/state permits.` The applicable handler, request/response type, status, and authorization evidence are those enumerated in paired TDS Sections 2, 9, 10, and 16; no additional state is claimed.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-BC-03-TC-API-001 — GET /api/v1/babies/{babyId}/daily-logs → getDailyLogs contract

| Field | Specification |
| --- | --- |
| Stable ID | `UC-BC-03-TC-API-001` |
| Severity | `High` |
| Test Condition | `COND-API-001` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Web / Backend` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/carejourney/controller/BabyDailyLogController.java` |
| Preconditions | Synthetic `Mother / Authorized Caregiver` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | Planned — `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/carejourney/controller/BabyDailyLogControllerTest.java` (not present at Draft baseline) |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-API-001`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Invoke `GET /api/v1/babies/{babyId}/daily-logs` so `getDailyLogs` receives path `babyId`: `UUID`; principal `principal`: `Principal`; satisfy `isAuthenticated()` and the extracted `None` constraints.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | Success status `200` returns `ApiResponse<List<BabyDailyLogResponse>>` with payload fields `babyLogId`: `UUID` (no field annotation in current DTO); `babyId`: `UUID` (no field annotation in current DTO); `logType`: `String` (no field annotation in current DTO); `startedAt`: `Instant` (no field annotation in current DTO); `endedAt`: `Instant` (no field annotation in current DTO); `quantity`: `BigDecimal` (no field annotation in current DTO); `unit`: `String` (no field annotation in current DTO); `note`: `String` (no field annotation in current DTO); `recordedBy`: `UUID` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO); `updatedAt`: `Instant` (no field annotation in current DTO); the request body is `None` with Not applicable — no request body. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/carejourney/controller/BabyDailyLogController.java` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-BC-03` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (Success status `200` returns `ApiResponse<List<BabyDailyLogResponse>>` with payload fields `babyLogId`: `UUID` (no field annotation in current DTO); `babyId`: `UUID` (no field annotation in current DTO); `logType`: `String` (no field annotation in current DTO); `startedAt`: `Instant` (no field annotation in current DTO); `endedAt`: `Instant` (no field annotation in current DTO); `quantity`: `BigDecimal` (no field annotation in current DTO); `unit`: `String` (no field annotation in current DTO); `note`: `String` (no field annotation in current DTO); `recordedBy`: `UUID` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO); `updatedAt`: `Instant` (no field annotation in current DTO); the request body is `None` with Not applicable — no request body.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-BC-03-TC-API-002 — POST /api/v1/babies/{babyId}/daily-logs → addDailyLog contract

| Field | Specification |
| --- | --- |
| Stable ID | `UC-BC-03-TC-API-002` |
| Severity | `High` |
| Test Condition | `COND-API-002` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Web / Backend` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/carejourney/controller/BabyDailyLogController.java` |
| Preconditions | Synthetic `Mother / Authorized Caregiver` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/carejourney/BabyDailyLogServiceTest.java` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-API-002`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Invoke `POST /api/v1/babies/{babyId}/daily-logs` so `addDailyLog` receives path `babyId`: `UUID`; body `request`: `AddBabyDailyLogRequest`; principal `principal`: `Principal`; satisfy `hasAnyRole('MOTHER', 'FAMILY')` and the extracted `AddBabyDailyLogRequest` constraints.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | Success status `200, 201` returns `ApiResponse<AddBabyDailyLogResponse>` with payload fields `babyLogId`: `UUID` (no field annotation in current DTO); `babyId`: `UUID` (no field annotation in current DTO); `logType`: `String` (no field annotation in current DTO); `startedAt`: `Instant` (no field annotation in current DTO); `endedAt`: `Instant` (no field annotation in current DTO); `quantity`: `BigDecimal` (no field annotation in current DTO); `unit`: `String` (no field annotation in current DTO); `note`: `String` (no field annotation in current DTO); `recordedBy`: `UUID` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO); the request body is `AddBabyDailyLogRequest` with `logType`: `String` (@Pattern(regexp = "FEEDING\|SLEEP\|DIAPER\|FEVER\|VOMITING\|MEDICINE\|SYMPTOM", message = "log_type must be one of: FEEDING, SLEEP, DIAPER, FEVER, VOMITING, MEDICINE, SYMPTOM")); `startedAt`: `Instant` (no field annotation in current DTO); `endedAt`: `Instant` (no field annotation in current DTO); `quantity`: `BigDecimal` (@DecimalMin(value = "0.00", message = "quantity must be non-negative")); `unit`: `String` (@Size(max = 20, message = "unit must not exceed 20 characters")); `note`: `String` (@Size(max = 2000, message = "note must not exceed 2000 characters")). | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/carejourney/controller/BabyDailyLogController.java` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-BC-03` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (Success status `200, 201` returns `ApiResponse<AddBabyDailyLogResponse>` with payload fields `babyLogId`: `UUID` (no field annotation in current DTO); `babyId`: `UUID` (no field annotation in current DTO); `logType`: `String` (no field annotation in current DTO); `startedAt`: `Instant` (no field annotation in current DTO); `endedAt`: `Instant` (no field annotation in current DTO); `quantity`: `BigDecimal` (no field annotation in current DTO); `unit`: `String` (no field annotation in current DTO); `note`: `String` (no field annotation in current DTO); `recordedBy`: `UUID` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO); the request body is `AddBabyDailyLogRequest` with `logType`: `String` (@Pattern(regexp = "FEEDING|SLEEP|DIAPER|FEVER|VOMITING|MEDICINE|SYMPTOM", message = "log_type must be one of: FEEDING, SLEEP, DIAPER, FEVER, VOMITING, MEDICINE, SYMPTOM")); `startedAt`: `Instant` (no field annotation in current DTO); `endedAt`: `Instant` (no field annotation in current DTO); `quantity`: `BigDecimal` (@DecimalMin(value = "0.00", message = "quantity must be non-negative")); `unit`: `String` (@Size(max = 20, message = "unit must not exceed 20 characters")); `note`: `String` (@Size(max = 2000, message = "note must not exceed 2000 characters")).), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-BC-03-TC-API-002-VAL — POST /api/v1/babies/{babyId}/daily-logs rejects a declared request-field boundary

| Field | Specification |
| --- | --- |
| Stable ID | `UC-BC-03-TC-API-002-VAL` |
| Severity | `High` |
| Test Condition | `COND-API-002-VAL` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Web / Backend` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/carejourney/controller/BabyDailyLogController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/common/exception/GlobalExceptionHandler.java#handleMethodArgumentNotValid` |
| Preconditions | Synthetic `Mother / Authorized Caregiver` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/carejourney/BabyDailyLogServiceTest.java` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-API-002-VAL`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Submit `AddBabyDailyLogRequest` with one field violating its cited validator: `logType`: `String` (@Pattern(regexp = "FEEDING|SLEEP|DIAPER|FEVER|VOMITING|MEDICINE|SYMPTOM", message = "log_type must be one of: FEEDING, SLEEP, DIAPER, FEVER, VOMITING, MEDICINE, SYMPTOM")); `startedAt`: `Instant` (no field annotation in current DTO); `endedAt`: `Instant` (no field annotation in current DTO); `quantity`: `BigDecimal` (@DecimalMin(value = "0.00", message = "quantity must be non-negative")); `unit`: `String` (@Size(max = 20, message = "unit must not exceed 20 characters")); `note`: `String` (@Size(max = 2000, message = "note must not exceed 2000 characters"))

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | HTTP `400` returns the current `ErrorResponse` with `status=400`, `error=VALIDATION_ERROR`, `message=Invalid request`, the request path, and field details with sensitive rejected values masked; the handler/service is not invoked. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/carejourney/controller/BabyDailyLogController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/common/exception/GlobalExceptionHandler.java#handleMethodArgumentNotValid` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-BC-03` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (HTTP `400` returns the current `ErrorResponse` with `status=400`, `error=VALIDATION_ERROR`, `message=Invalid request`, the request path, and field details with sensitive rejected values masked; the handler/service is not invoked.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-BC-03-TC-API-003 — DELETE /api/v1/babies/{babyId}/daily-logs/{logId} → deleteLog contract

| Field | Specification |
| --- | --- |
| Stable ID | `UC-BC-03-TC-API-003` |
| Severity | `High` |
| Test Condition | `COND-API-003` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Web / Backend` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/carejourney/controller/BabyDailyLogController.java` |
| Preconditions | Synthetic `Mother / Authorized Caregiver` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/carejourney/BabyDailyLogServiceTest.java` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-API-003`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Invoke `DELETE /api/v1/babies/{babyId}/daily-logs/{logId}` so `deleteLog` receives path `babyId`: `UUID`; path `logId`: `UUID`; principal `principal`: `Principal`; satisfy `hasAnyRole('MOTHER', 'FAMILY')` and the extracted `None` constraints.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | Success status `200` returns `ApiResponse<Void>` with payload fields Not applicable or unresolved from the handler import; the request body is `None` with Not applicable — no request body. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/carejourney/controller/BabyDailyLogController.java` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-BC-03` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (Success status `200` returns `ApiResponse<Void>` with payload fields Not applicable or unresolved from the handler import; the request body is `None` with Not applicable — no request body.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-BC-03-TC-API-004 — GET /api/v1/babies/{babyId}/daily-logs/{logId} → getDailyLogDetail contract

| Field | Specification |
| --- | --- |
| Stable ID | `UC-BC-03-TC-API-004` |
| Severity | `High` |
| Test Condition | `COND-API-004` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Web / Backend` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/carejourney/controller/BabyDailyLogController.java` |
| Preconditions | Synthetic `Mother / Authorized Caregiver` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/carejourney/BabyDailyLogServiceTest.java` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-API-004`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Invoke `GET /api/v1/babies/{babyId}/daily-logs/{logId}` so `getDailyLogDetail` receives path `babyId`: `UUID`; path `logId`: `UUID`; principal `principal`: `Principal`; satisfy `isAuthenticated()` and the extracted `None` constraints.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | Success status `200` returns `ApiResponse<BabyDailyLogResponse>` with payload fields `babyLogId`: `UUID` (no field annotation in current DTO); `babyId`: `UUID` (no field annotation in current DTO); `logType`: `String` (no field annotation in current DTO); `startedAt`: `Instant` (no field annotation in current DTO); `endedAt`: `Instant` (no field annotation in current DTO); `quantity`: `BigDecimal` (no field annotation in current DTO); `unit`: `String` (no field annotation in current DTO); `note`: `String` (no field annotation in current DTO); `recordedBy`: `UUID` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO); `updatedAt`: `Instant` (no field annotation in current DTO); the request body is `None` with Not applicable — no request body. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/carejourney/controller/BabyDailyLogController.java` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-BC-03` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (Success status `200` returns `ApiResponse<BabyDailyLogResponse>` with payload fields `babyLogId`: `UUID` (no field annotation in current DTO); `babyId`: `UUID` (no field annotation in current DTO); `logType`: `String` (no field annotation in current DTO); `startedAt`: `Instant` (no field annotation in current DTO); `endedAt`: `Instant` (no field annotation in current DTO); `quantity`: `BigDecimal` (no field annotation in current DTO); `unit`: `String` (no field annotation in current DTO); `note`: `String` (no field annotation in current DTO); `recordedBy`: `UUID` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO); `updatedAt`: `Instant` (no field annotation in current DTO); the request body is `None` with Not applicable — no request body.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-BC-03-TC-API-005 — PUT /api/v1/babies/{babyId}/daily-logs/{logId} → updateLog contract

| Field | Specification |
| --- | --- |
| Stable ID | `UC-BC-03-TC-API-005` |
| Severity | `High` |
| Test Condition | `COND-API-005` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Web / Backend` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/carejourney/controller/BabyDailyLogController.java` |
| Preconditions | Synthetic `Mother / Authorized Caregiver` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/carejourney/BabyDailyLogServiceTest.java` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-API-005`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Invoke `PUT /api/v1/babies/{babyId}/daily-logs/{logId}` so `updateLog` receives path `babyId`: `UUID`; path `logId`: `UUID`; body `request`: `UpdateBabyDailyLogRequest`; principal `principal`: `Principal`; satisfy `hasAnyRole('MOTHER', 'FAMILY')` and the extracted `UpdateBabyDailyLogRequest` constraints.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | Success status `200` returns `ApiResponse<BabyDailyLogResponse>` with payload fields `babyLogId`: `UUID` (no field annotation in current DTO); `babyId`: `UUID` (no field annotation in current DTO); `logType`: `String` (no field annotation in current DTO); `startedAt`: `Instant` (no field annotation in current DTO); `endedAt`: `Instant` (no field annotation in current DTO); `quantity`: `BigDecimal` (no field annotation in current DTO); `unit`: `String` (no field annotation in current DTO); `note`: `String` (no field annotation in current DTO); `recordedBy`: `UUID` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO); `updatedAt`: `Instant` (no field annotation in current DTO); the request body is `UpdateBabyDailyLogRequest` with `startedAt`: `Instant` (no field annotation in current DTO); `endedAt`: `Instant` (no field annotation in current DTO); `quantity`: `BigDecimal` (@DecimalMin(value = "0", message = "quantity must be non-negative")); `unit`: `String` (@Size(max = 20, message = "unit must not exceed 20 characters")); `note`: `String` (@Size(max = 500, message = "note must not exceed 500 characters")). | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/carejourney/controller/BabyDailyLogController.java` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-BC-03` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (Success status `200` returns `ApiResponse<BabyDailyLogResponse>` with payload fields `babyLogId`: `UUID` (no field annotation in current DTO); `babyId`: `UUID` (no field annotation in current DTO); `logType`: `String` (no field annotation in current DTO); `startedAt`: `Instant` (no field annotation in current DTO); `endedAt`: `Instant` (no field annotation in current DTO); `quantity`: `BigDecimal` (no field annotation in current DTO); `unit`: `String` (no field annotation in current DTO); `note`: `String` (no field annotation in current DTO); `recordedBy`: `UUID` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO); `updatedAt`: `Instant` (no field annotation in current DTO); the request body is `UpdateBabyDailyLogRequest` with `startedAt`: `Instant` (no field annotation in current DTO); `endedAt`: `Instant` (no field annotation in current DTO); `quantity`: `BigDecimal` (@DecimalMin(value = "0", message = "quantity must be non-negative")); `unit`: `String` (@Size(max = 20, message = "unit must not exceed 20 characters")); `note`: `String` (@Size(max = 500, message = "note must not exceed 500 characters")).), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-BC-03-TC-API-005-VAL — PUT /api/v1/babies/{babyId}/daily-logs/{logId} rejects a declared request-field boundary

| Field | Specification |
| --- | --- |
| Stable ID | `UC-BC-03-TC-API-005-VAL` |
| Severity | `High` |
| Test Condition | `COND-API-005-VAL` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Web / Backend` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/carejourney/controller/BabyDailyLogController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/common/exception/GlobalExceptionHandler.java#handleMethodArgumentNotValid` |
| Preconditions | Synthetic `Mother / Authorized Caregiver` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/carejourney/BabyDailyLogServiceTest.java` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-API-005-VAL`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Submit `UpdateBabyDailyLogRequest` with one field violating its cited validator: `startedAt`: `Instant` (no field annotation in current DTO); `endedAt`: `Instant` (no field annotation in current DTO); `quantity`: `BigDecimal` (@DecimalMin(value = "0", message = "quantity must be non-negative")); `unit`: `String` (@Size(max = 20, message = "unit must not exceed 20 characters")); `note`: `String` (@Size(max = 500, message = "note must not exceed 500 characters"))

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | HTTP `400` returns the current `ErrorResponse` with `status=400`, `error=VALIDATION_ERROR`, `message=Invalid request`, the request path, and field details with sensitive rejected values masked; the handler/service is not invoked. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/carejourney/controller/BabyDailyLogController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/common/exception/GlobalExceptionHandler.java#handleMethodArgumentNotValid` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-BC-03` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (HTTP `400` returns the current `ErrorResponse` with `status=400`, `error=VALIDATION_ERROR`, `message=Invalid request`, the request path, and field details with sensitive rejected values masked; the handler/service is not invoked.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-BC-03-TC-BR-001 — Enforce business rule 1

| Field | Specification |
| --- | --- |
| Stable ID | `UC-BC-03-TC-BR-001` |
| Severity | `High` |
| Test Condition | `COND-BR-01` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Web / Backend` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `TDS BR-01; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/carejourney/controller/BabyDailyLogController.java` |
| Preconditions | Synthetic `Mother / Authorized Caregiver` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/carejourney/BabyDailyLogServiceTest.java` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-BR-01`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Exercise the eligible and ineligible partitions for this rule: Type-specific field validation and baby authorization are server authoritative.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | The server enforces: Type-specific field validation and baby authorization are server authoritative. No disallowed state or protected data is produced. | `TDS BR-01; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/carejourney/controller/BabyDailyLogController.java` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-BC-03` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (The server enforces: Type-specific field validation and baby authorization are server authoritative. No disallowed state or protected data is produced.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-BC-03-TC-BR-002 — Enforce business rule 2

| Field | Specification |
| --- | --- |
| Stable ID | `UC-BC-03-TC-BR-002` |
| Severity | `High` |
| Test Condition | `COND-BR-02` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Web / Backend` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `TDS BR-02; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/carejourney/controller/BabyDailyLogController.java` |
| Preconditions | Synthetic `Mother / Authorized Caregiver` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/carejourney/BabyDailyLogServiceTest.java` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-BR-02`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Exercise the eligible and ineligible partitions for this rule: Log time/order uses server-normalized values.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | The server enforces: Log time/order uses server-normalized values. No disallowed state or protected data is produced. | `TDS BR-02; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/carejourney/controller/BabyDailyLogController.java` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-BC-03` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (The server enforces: Log time/order uses server-normalized values. No disallowed state or protected data is produced.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-BC-03-TC-SEC-001 — Reject wrong authentication, role, ownership, membership, or consent scope

| Field | Specification |
| --- | --- |
| Stable ID | `UC-BC-03-TC-SEC-001` |
| Severity | `Critical` |
| Test Condition | `COND-AUTH` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Web / Backend` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `TDS Sections 4 and 16; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/carejourney/controller/BabyDailyLogController.java` |
| Preconditions | Synthetic `Mother / Authorized Caregiver` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/carejourney/BabyDailyLogServiceTest.java` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-AUTH`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Invoke every protected operation with an unauthenticated principal and the closest disallowed role/scope partition.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | The request fails closed using the exact current mapped status/code and returns no protected resource fields or side effects. | `TDS Sections 4 and 16; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/carejourney/controller/BabyDailyLogController.java` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-BC-03` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (The request fails closed using the exact current mapped status/code and returns no protected resource fields or side effects.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-BC-03-TC-GAP-001 — Prove the current actor entry path and owning contract remain reachable

| Field | Specification |
| --- | --- |
| Stable ID | `UC-BC-03-TC-GAP-001` |
| Severity | `Medium` |
| Test Condition | `COND-GAP` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Web / Backend` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `SRS UC-BC-03 Implemented Entry Points/Contracts; 05_Development/CareBridgeMobileApp/lib/features/baby/screens/edit_baby_daily_log_screen.dart` |
| Preconditions | Synthetic `Mother / Authorized Caregiver` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/carejourney/BabyDailyLogServiceTest.java` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-GAP`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Start from `Baby detail log modal and Mobile `/babies/:babyId/daily-logs/:logId*``, perform `Choose a supported log type and enter type-specific data.`, and observe the owning contract `Daily-log endpoints under `/api/v1/babies/{babyId}/daily-logs/**`` rather than a retired or unrelated route.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | The current entry path remains reachable for `Manage Baby Daily Logs` and invokes only the documented owning contract; an unreachable, static, or wrong-method path fails this case. | `SRS UC-BC-03 Implemented Entry Points/Contracts; 05_Development/CareBridgeMobileApp/lib/features/baby/screens/edit_baby_daily_log_screen.dart` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-BC-03` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (The current entry path remains reachable for `Manage Baby Daily Logs` and invokes only the documented owning contract; an unreachable, static, or wrong-method path fails this case.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### 4.3 Coverage Families

| Behavior family | Conditions | Cases |
| --- | --- | --- |
| Choose a supported log type and enter type-specific data. | `COND-01` | `UC-BC-03-TC-001` |
| Persist and inspect the log detail. | `COND-02` | `UC-BC-03-TC-002` |
| Edit or delete it when ownership/state permits. | `COND-03` | `UC-BC-03-TC-003` |
| GET /api/v1/babies/{babyId}/daily-logs → getDailyLogs contract | `COND-API-001` | `UC-BC-03-TC-API-001` |
| POST /api/v1/babies/{babyId}/daily-logs → addDailyLog contract | `COND-API-002` | `UC-BC-03-TC-API-002` |
| POST /api/v1/babies/{babyId}/daily-logs rejects a declared request-field boundary | `COND-API-002-VAL` | `UC-BC-03-TC-API-002-VAL` |
| DELETE /api/v1/babies/{babyId}/daily-logs/{logId} → deleteLog contract | `COND-API-003` | `UC-BC-03-TC-API-003` |
| GET /api/v1/babies/{babyId}/daily-logs/{logId} → getDailyLogDetail contract | `COND-API-004` | `UC-BC-03-TC-API-004` |
| PUT /api/v1/babies/{babyId}/daily-logs/{logId} → updateLog contract | `COND-API-005` | `UC-BC-03-TC-API-005` |
| PUT /api/v1/babies/{babyId}/daily-logs/{logId} rejects a declared request-field boundary | `COND-API-005-VAL` | `UC-BC-03-TC-API-005-VAL` |
| Business-rule partitions | `COND-BR-*` | `UC-BC-03-TC-BR-*` |
| Authentication / authorization / ownership / consent | `COND-AUTH` | `UC-BC-03-TC-SEC-001` |
| Current gap / reachability boundary | `COND-GAP` | `UC-BC-03-TC-GAP-001` |

## 5. Red-Green-Refactor Tracker

| TC ID | Intended file | Red evidence | Green evidence | Refactor verification | Status |
| --- | --- | --- | --- | --- | --- |
| `UC-BC-03-TC-001` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/carejourney/BabyDailyLogServiceTest.java` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-BC-03-TC-002` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/carejourney/BabyDailyLogServiceTest.java` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-BC-03-TC-003` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/carejourney/BabyDailyLogServiceTest.java` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-BC-03-TC-API-001` | Planned — `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/carejourney/controller/BabyDailyLogControllerTest.java` (not present at Draft baseline) | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-BC-03-TC-API-002` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/carejourney/BabyDailyLogServiceTest.java` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-BC-03-TC-API-002-VAL` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/carejourney/BabyDailyLogServiceTest.java` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-BC-03-TC-API-003` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/carejourney/BabyDailyLogServiceTest.java` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-BC-03-TC-API-004` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/carejourney/BabyDailyLogServiceTest.java` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-BC-03-TC-API-005` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/carejourney/BabyDailyLogServiceTest.java` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-BC-03-TC-API-005-VAL` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/carejourney/BabyDailyLogServiceTest.java` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-BC-03-TC-BR-001` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/carejourney/BabyDailyLogServiceTest.java` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-BC-03-TC-BR-002` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/carejourney/BabyDailyLogServiceTest.java` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-BC-03-TC-SEC-001` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/carejourney/BabyDailyLogServiceTest.java` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-BC-03-TC-GAP-001` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/carejourney/BabyDailyLogServiceTest.java` | Not run | Not run | Not run | 🔴 Not written / rerun required |

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
| Generic test matrix | Case titles/actions reference `Manage Baby Daily Logs` operations and rules. | Pass |
| False Green claim | Current command/time/count evidence is required. | Pass — all rows remain Red/not rerun. |
| Hidden contradiction | Section 2 records each known gap. | Resolved broad-boundary issue recorded |
| Missing Props Isolation | Applicable Java/TS/Dart factory pattern is present. | Pass at specification level |
| Cross-test pollution | TDS-05 defines actor/resource/provider cleanup. | Draft gate — implementation review must prove teardown/rollback before Green evidence is accepted |
| Wrong-layer test | Applicability matrix marks absent consumers/layers Not applicable. | Pass |
| Uncovered contract | Operations/rules/auth/gap map to conditions and detailed TCs. | Pass for handler/DTO/status/operation/rule/auth/gap mappings; service-only events/codes remain visible in paired TDS |
| Unsafe data | Synthetic-only rule; no production credentials/protected data. | Pass at specification level |
| AI safety bypass | Deterministic policy cannot be lowered by model output when AI applies. | Not applicable — no clinical/moderation generation in this UC |

- [ ] Human reviewer confirms all eight sections, oracle sources, detailed TCs, applicability, Red Gate, rollback, and paired-TDS traceability before approval.
