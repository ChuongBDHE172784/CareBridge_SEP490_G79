# ENGINEERING DOCUMENTATION STANDARD (EDS) v2.0

# TEST SPECIFICATION — Perform Exercise Session and Review Results

| Field | Value |
| --- | --- |
| Document ID | `UC-MH-19-TEST-SPEC` |
| Version | `0.1` |
| Date | `2026-08-23` |
| Status | `Draft` |
| Feature / Gap ID | `UC-MH-19` |
| Module | `Mother Journey and Health` |
| Paired TDS | `UC-MH-19-TDS` |
| Priority | `Medium` |
| Platforms | `Mobile / Backend / Camera / Posture Sidecar` |
| Data Classification | `Restricted maternal health, screening, journey, record, and attachment data; Confidential schedule/preferences` |
| Compliance Scope | `PDPA health-data minimization, consent/ownership enforcement, clinical disclaimer where applicable, and purpose-bound file access` |
| Owner | `CareBridge Team` |
| Reviewer / Approver |  |
| Source Baseline | Current worktree `2026-08-23`; SRS/TDS `UC-MH-19` and exact code/test sources below |

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
| Actor goal | Load the active posture configuration, start an eligible exercise session, run posture analysis, complete/abort it, and review the result or history. | SRS `UC-MH-19` |
| Current state | `High` confidence; gaps are listed in Section 2 | Exact current code/test sources below |
| Entry points | Nested Mobile exercise session, result, and history screens | Current client/router evidence |
| Authorization boundary | `Mother` plus exact authentication/role/ownership/membership/consent policy | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/ExerciseController.java` |
| Primary operations | Load the active posture configuration and start a session after a passed safety check/camera permission.; Process supported posture observations through the configured posture-correction sidecar or implemented fallback.; Complete/abort and review the stored result/history. | SRS `UC-MH-19` Normal Flow |
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
| `SRC-SRS` | Requirement | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-MH-19 | 2026-08-23 | Draft code-first requirement |
| `SRC-TDS` | Design | Paired `UC-MH-19-TDS` | 0.1 | Draft design |
| `SRC-CODE-01` | Current code | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/ExerciseController.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-02` | Current code | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/ExerciseSessionController.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-03` | Current code | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/service/impl/PostureAnalysisServiceImpl.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-04` | Current code | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/inference/ExerciseCorrectionHttpAdapter.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-05` | Current code | `05_Development/MachineLearning/MediaPipe_Posture/exercise_correction_sidecar/app/main.py` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-06` | Current code | `05_Development/CareBridgeMobileApp/lib/features/exercise/screens/exercise_session_screen.dart` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-01` | Existing test | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/exercise/ExerciseSessionServiceTest.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-02` | Existing test | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/exercise/ExerciseSessionHistoryEmbeddedPostgresTest.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-03` | Existing test | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/exercise/PostureAnalysisServiceTest.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-04` | Existing test | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/exercise/inference/ExerciseCorrectionHttpAdapterTest.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-05` | Existing test | `05_Development/MachineLearning/MediaPipe_Posture/exercise_correction_sidecar/tests/test_http_contract.py` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-06` | Existing test | `05_Development/CareBridgeMobileApp/test/features/exercise/exercise_session_screen_test.dart` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-ERROR-01` | Current exception advice | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/common/exception/GlobalExceptionHandler.java` | Worktree `2026-08-23` | Current-state evidence |

## 2. Logic Issues Resolved

| Issue ID | Discrepancy | Impact | Resolution | Oracle | Status |
| --- | --- | --- | --- | --- | --- |
| `LI-01` | Broad 43-UC catalogue previously obscured this boundary | Generic TCs could not map to `Perform Exercise Session and Review Results` | Split as `UC-MH-19` using the audited current code boundary | SRS 3.1 and `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/ExerciseController.java` | Resolved in Draft |

Architecture-, schema-, authorization-, and test-changing Open items must be resolved before implementation approval.

## 3. Test Design Specification

### TDS-01 — Risk-Based Scope

| Risk ID | Failure mode | Severity | Likelihood | Detectability | Levels | Conditions |
| --- | --- | --- | --- | --- | --- | --- |
| `RISK-01` | Failure of: Load the active posture configuration and start a session after a passed safety check/camera permission. | High | Medium | High | Unit/Integration/Contract/applicable UI | `COND-01` |
| `RISK-02` | Failure of: Process supported posture observations through the configured posture-correction sidecar or implemented fallback. | High | Medium | High | Unit/Integration/Contract/applicable UI | `COND-02` |
| `RISK-03` | Failure of: Complete/abort and review the stored result/history. | High | Medium | High | Unit/Integration/Contract/applicable UI | `COND-03` |
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
| `BASIS-01` | `UC-MH-19-FR-01` — Load the active posture configuration and start a session after a passed safety check/camera permission. | SRS `UC-MH-19` Normal Flow 1; TDS Section 2 | Load the active posture configuration and start a session after a passed safety check/camera permission. | `COND-01` |
| `BASIS-02` | `UC-MH-19-FR-02` — Process supported posture observations through the configured posture-correction sidecar or implemented fallback. | SRS `UC-MH-19` Normal Flow 2; TDS Section 2 | Process supported posture observations through the configured posture-correction sidecar or implemented fallback. | `COND-02` |
| `BASIS-03` | `UC-MH-19-FR-03` — Complete/abort and review the stored result/history. | SRS `UC-MH-19` Normal Flow 3; TDS Section 2 | Complete/abort and review the stored result/history. | `COND-03` |

Oracle precedence: approved user decision → approved BR/ADR/security policy → paired TDS → current implementation for characterization → existing test as regression evidence.

### TDS-03 — Test Conditions and Coverage Items

| Condition | Basis / risk | Behavior | Layer | Coverage | Test cases |
| --- | --- | --- | --- | --- | --- |
| `COND-01` | `BASIS-01` / `RISK-01` | Load the active posture configuration and start a session after a passed safety check/camera permission. | Mobile / Backend / Camera / Posture Sidecar | Positive + applicable boundary/state coverage | `UC-MH-19-TC-001` |
| `COND-02` | `BASIS-02` / `RISK-02` | Process supported posture observations through the configured posture-correction sidecar or implemented fallback. | Mobile / Backend / Camera / Posture Sidecar | Positive + applicable boundary/state coverage | `UC-MH-19-TC-002` |
| `COND-03` | `BASIS-03` / `RISK-03` | Complete/abort and review the stored result/history. | Mobile / Backend / Camera / Posture Sidecar | Positive + applicable boundary/state coverage | `UC-MH-19-TC-003` |
| `COND-API-001` | Exact handler/client composition contract | GET /api/v1/exercises/sessions/history → getSessionHistory contract | Applicable backend/client contract layer | Contract + DTO/status/authorization evidence | `UC-MH-19-TC-API-001` |
| `COND-API-002` | Exact handler/client composition contract | PATCH /api/v1/exercises/sessions/{sessionId}/complete → completeSession contract | Applicable backend/client contract layer | Contract + DTO/status/authorization evidence | `UC-MH-19-TC-API-002` |
| `COND-API-003` | Exact handler/client composition contract | PATCH /api/v1/exercises/sessions/{sessionId}/pause → pauseSession contract | Applicable backend/client contract layer | Contract + DTO/status/authorization evidence | `UC-MH-19-TC-API-003` |
| `COND-API-004` | Exact handler/client composition contract | POST /api/v1/exercises/sessions/{sessionId}/posture-events → analyzePosture contract | Applicable backend/client contract layer | Contract + DTO/status/authorization evidence | `UC-MH-19-TC-API-004` |
| `COND-API-004-VAL` | Exact handler/client composition contract | POST /api/v1/exercises/sessions/{sessionId}/posture-events rejects a declared request-field boundary | Applicable backend/client contract layer | Contract + DTO/status/authorization evidence | `UC-MH-19-TC-API-004-VAL` |
| `COND-API-005` | Exact handler/client composition contract | GET /api/v1/exercises/sessions/{sessionId}/result → getSessionResult contract | Applicable backend/client contract layer | Contract + DTO/status/authorization evidence | `UC-MH-19-TC-API-005` |
| `COND-API-006` | Exact handler/client composition contract | PATCH /api/v1/exercises/sessions/{sessionId}/resume → resumeSession contract | Applicable backend/client contract layer | Contract + DTO/status/authorization evidence | `UC-MH-19-TC-API-006` |
| `COND-API-007` | Exact handler/client composition contract | GET /api/v1/exercises/{exerciseId}/posture-config → getPostureConfig contract | Applicable backend/client contract layer | Contract + DTO/status/authorization evidence | `UC-MH-19-TC-API-007` |
| `COND-API-008` | Exact handler/client composition contract | POST /api/v1/exercises/{exerciseId}/sessions → startSession contract | Applicable backend/client contract layer | Contract + DTO/status/authorization evidence | `UC-MH-19-TC-API-008` |
| `COND-API-008-VAL` | Exact handler/client composition contract | POST /api/v1/exercises/{exerciseId}/sessions rejects a declared request-field boundary | Applicable backend/client contract layer | Contract + DTO/status/authorization evidence | `UC-MH-19-TC-API-008-VAL` |
| `COND-API-009` | Exact handler/client composition contract | POST /v1/inference/landmarks → infer contract | Applicable backend/client contract layer | Contract + DTO/status/authorization evidence | `UC-MH-19-TC-API-009` |
| `COND-API-009-VAL` | Exact handler/client composition contract | POST /v1/inference/landmarks rejects a declared request-field boundary | Applicable backend/client contract layer | Contract + DTO/status/authorization evidence | `UC-MH-19-TC-API-009-VAL` |
| `COND-BR-01` | `BR-01` | Server session state is canonical; camera/model feedback is advisory. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/service/impl/PostureAnalysisServiceImpl.java` | Negative / decision / state | `UC-MH-19-TC-BR-001` |
| `COND-BR-02` | `BR-02` | The sidecar inference contract validates sequence and landmark payloads; provider failure follows the implemented backend fallback/degraded path. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/service/impl/PostureAnalysisServiceImpl.java` | Negative / decision / state | `UC-MH-19-TC-BR-002` |
| `COND-BR-03` | `BR-03` | Late frames and retries must not mutate a completed/aborted session. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/service/impl/PostureAnalysisServiceImpl.java` | Negative / decision / state | `UC-MH-19-TC-BR-003` |
| `COND-AUTH` | `RISK-AUTH` | Reject wrong authentication, role, ownership, membership, consent, or state scope | All protected layers | Security | `UC-MH-19-TC-SEC-001` |
| `COND-GAP` | `RISK-GAP` | Characterize current limitation/reachability without false completion | Applicable layer | Gap/Regression | `UC-MH-19-TC-GAP-001` |

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
| Actor | Synthetic `Mother` plus closest wrong-role/cross-owner identities | Authenticated, unauthenticated, wrong scope, consent revoked | Reset principals/tokens |
| Resource | Minimum valid feature-owned object | Missing, malformed, boundary, stale, already-final, cross-owner | Transaction rollback or isolated repository cleanup |
| Provider/device | Deterministic fake only when applicable | Success, timeout, malformed, permission denied | Reset fake/timers/device state |
| Protected fields | Synthetic non-production values only | Redaction and disclosure checks | Never persist in snapshots/log fixtures |

Existing test evidence:

- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/exercise/ExerciseSessionServiceTest.java`
- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/exercise/ExerciseSessionHistoryEmbeddedPostgresTest.java`
- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/exercise/PostureAnalysisServiceTest.java`
- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/exercise/inference/ExerciseCorrectionHttpAdapterTest.java`
- `05_Development/MachineLearning/MediaPipe_Posture/exercise_correction_sidecar/tests/test_http_contract.py`
- `05_Development/CareBridgeMobileApp/test/features/exercise/exercise_session_screen_test.dart`

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

### UC-MH-19-TC-001 — Load the active posture configuration and start a session after a passed safety check/camera permission

| Field | Specification |
| --- | --- |
| Stable ID | `UC-MH-19-TC-001` |
| Severity | `High` |
| Test Condition | `COND-01` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Backend / Camera / Posture Sidecar` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `SRS UC-MH-19 Normal Flow 1; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/ExerciseSessionController.java` |
| Preconditions | Synthetic `Mother` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/exercise/ExerciseSessionServiceTest.java` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-01`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Load the active posture configuration and start a session after a passed safety check/camera permission.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | The observable outcome is exactly `Load the active posture configuration and start a session after a passed safety check/camera permission.` The applicable handler, request/response type, status, and authorization evidence are those enumerated in paired TDS Sections 2, 9, 10, and 16; no additional state is claimed. | `SRS UC-MH-19 Normal Flow 1; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/ExerciseSessionController.java` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-MH-19` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (The observable outcome is exactly `Load the active posture configuration and start a session after a passed safety check/camera permission.` The applicable handler, request/response type, status, and authorization evidence are those enumerated in paired TDS Sections 2, 9, 10, and 16; no additional state is claimed.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-MH-19-TC-002 — Process supported posture observations through the configured posture-correction sidecar or implemented fallback

| Field | Specification |
| --- | --- |
| Stable ID | `UC-MH-19-TC-002` |
| Severity | `High` |
| Test Condition | `COND-02` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Backend / Camera / Posture Sidecar` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `SRS UC-MH-19 Normal Flow 2; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/ExerciseSessionController.java` |
| Preconditions | Synthetic `Mother` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/exercise/ExerciseSessionHistoryEmbeddedPostgresTest.java` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-02`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Process supported posture observations through the configured posture-correction sidecar or implemented fallback.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | The observable outcome is exactly `Process supported posture observations through the configured posture-correction sidecar or implemented fallback.` The applicable handler, request/response type, status, and authorization evidence are those enumerated in paired TDS Sections 2, 9, 10, and 16; no additional state is claimed. | `SRS UC-MH-19 Normal Flow 2; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/ExerciseSessionController.java` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-MH-19` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (The observable outcome is exactly `Process supported posture observations through the configured posture-correction sidecar or implemented fallback.` The applicable handler, request/response type, status, and authorization evidence are those enumerated in paired TDS Sections 2, 9, 10, and 16; no additional state is claimed.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-MH-19-TC-003 — Complete/abort and review the stored result/history

| Field | Specification |
| --- | --- |
| Stable ID | `UC-MH-19-TC-003` |
| Severity | `High` |
| Test Condition | `COND-03` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Backend / Camera / Posture Sidecar` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `SRS UC-MH-19 Normal Flow 3; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/ExerciseSessionController.java` |
| Preconditions | Synthetic `Mother` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/exercise/PostureAnalysisServiceTest.java` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-03`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Complete/abort and review the stored result/history.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | The observable outcome is exactly `Complete/abort and review the stored result/history.` The applicable handler, request/response type, status, and authorization evidence are those enumerated in paired TDS Sections 2, 9, 10, and 16; no additional state is claimed. | `SRS UC-MH-19 Normal Flow 3; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/ExerciseSessionController.java` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-MH-19` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (The observable outcome is exactly `Complete/abort and review the stored result/history.` The applicable handler, request/response type, status, and authorization evidence are those enumerated in paired TDS Sections 2, 9, 10, and 16; no additional state is claimed.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-MH-19-TC-API-001 — GET /api/v1/exercises/sessions/history → getSessionHistory contract

| Field | Specification |
| --- | --- |
| Stable ID | `UC-MH-19-TC-API-001` |
| Severity | `High` |
| Test Condition | `COND-API-001` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Backend / Camera / Posture Sidecar` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/ExerciseSessionController.java` |
| Preconditions | Synthetic `Mother` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/exercise/ExerciseSessionHistoryEmbeddedPostgresTest.java` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-API-001`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Invoke `GET /api/v1/exercises/sessions/history` so `getSessionHistory` receives query `trimesterScope`: `TrimesterScope`; query `from`: `OffsetDateTime`; query `to`: `OffsetDateTime`; query `page`: `int`; query `size`: `int`; principal `principal`: `Principal`; satisfy `hasAnyRole('MOTHER', 'SYSTEM_ADMIN')` and the extracted `None` constraints.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | Success status `200` returns `ResponseEntity<PaginatedResponse<ExerciseSessionHistorySummary>>` with payload fields Not applicable or unresolved from the handler import; the request body is `None` with Not applicable — no request body. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/ExerciseSessionController.java` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-MH-19` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (Success status `200` returns `ResponseEntity<PaginatedResponse<ExerciseSessionHistorySummary>>` with payload fields Not applicable or unresolved from the handler import; the request body is `None` with Not applicable — no request body.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-MH-19-TC-API-002 — PATCH /api/v1/exercises/sessions/{sessionId}/complete → completeSession contract

| Field | Specification |
| --- | --- |
| Stable ID | `UC-MH-19-TC-API-002` |
| Severity | `High` |
| Test Condition | `COND-API-002` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Backend / Camera / Posture Sidecar` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/ExerciseSessionController.java` |
| Preconditions | Synthetic `Mother` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | Planned — `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/exercise/controller/ExerciseSessionControllerTest.java` (not present at Draft baseline) |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-API-002`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Invoke `PATCH /api/v1/exercises/sessions/{sessionId}/complete` so `completeSession` receives path `sessionId`: `UUID`; principal `principal`: `Principal`; satisfy `hasAnyRole('MOTHER', 'SYSTEM_ADMIN')` and the extracted `None` constraints.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | Success status `200` returns `ResponseEntity<ApiResponse<SessionResultResponse>>` with payload fields `exerciseSessionId`: `UUID` (no field annotation in current DTO); `exerciseId`: `UUID` (no field annotation in current DTO); `exerciseTitle`: `String` (no field annotation in current DTO); `sessionStatus`: `String` (no field annotation in current DTO); `startedAt`: `OffsetDateTime` (no field annotation in current DTO); `endedAt`: `OffsetDateTime` (no field annotation in current DTO); `actualDurationSeconds`: `Long` (no field annotation in current DTO); `completionPercent`: `BigDecimal` (no field annotation in current DTO); `postureScore`: `BigDecimal` (no field annotation in current DTO); `warningCount`: `Integer` (no field annotation in current DTO); `summaryJson`: `String` (no field annotation in current DTO); the request body is `None` with Not applicable — no request body. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/ExerciseSessionController.java` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-MH-19` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (Success status `200` returns `ResponseEntity<ApiResponse<SessionResultResponse>>` with payload fields `exerciseSessionId`: `UUID` (no field annotation in current DTO); `exerciseId`: `UUID` (no field annotation in current DTO); `exerciseTitle`: `String` (no field annotation in current DTO); `sessionStatus`: `String` (no field annotation in current DTO); `startedAt`: `OffsetDateTime` (no field annotation in current DTO); `endedAt`: `OffsetDateTime` (no field annotation in current DTO); `actualDurationSeconds`: `Long` (no field annotation in current DTO); `completionPercent`: `BigDecimal` (no field annotation in current DTO); `postureScore`: `BigDecimal` (no field annotation in current DTO); `warningCount`: `Integer` (no field annotation in current DTO); `summaryJson`: `String` (no field annotation in current DTO); the request body is `None` with Not applicable — no request body.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-MH-19-TC-API-003 — PATCH /api/v1/exercises/sessions/{sessionId}/pause → pauseSession contract

| Field | Specification |
| --- | --- |
| Stable ID | `UC-MH-19-TC-API-003` |
| Severity | `High` |
| Test Condition | `COND-API-003` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Backend / Camera / Posture Sidecar` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/ExerciseSessionController.java` |
| Preconditions | Synthetic `Mother` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | Planned — `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/exercise/controller/ExerciseSessionControllerTest.java` (not present at Draft baseline) |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-API-003`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Invoke `PATCH /api/v1/exercises/sessions/{sessionId}/pause` so `pauseSession` receives path `sessionId`: `UUID`; principal `principal`: `Principal`; satisfy `hasAnyRole('MOTHER', 'SYSTEM_ADMIN')` and the extracted `None` constraints.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | Success status `200` returns `ResponseEntity<ApiResponse<SessionStateResponse>>` with payload fields `exerciseSessionId`: `UUID` (no field annotation in current DTO); `sessionStatus`: `String` (no field annotation in current DTO); `pausedSeconds`: `Integer` (no field annotation in current DTO); `warningCount`: `Integer` (no field annotation in current DTO); `updatedAt`: `OffsetDateTime` (no field annotation in current DTO); the request body is `None` with Not applicable — no request body. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/ExerciseSessionController.java` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-MH-19` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (Success status `200` returns `ResponseEntity<ApiResponse<SessionStateResponse>>` with payload fields `exerciseSessionId`: `UUID` (no field annotation in current DTO); `sessionStatus`: `String` (no field annotation in current DTO); `pausedSeconds`: `Integer` (no field annotation in current DTO); `warningCount`: `Integer` (no field annotation in current DTO); `updatedAt`: `OffsetDateTime` (no field annotation in current DTO); the request body is `None` with Not applicable — no request body.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-MH-19-TC-API-004 — POST /api/v1/exercises/sessions/{sessionId}/posture-events → analyzePosture contract

| Field | Specification |
| --- | --- |
| Stable ID | `UC-MH-19-TC-API-004` |
| Severity | `High` |
| Test Condition | `COND-API-004` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Backend / Camera / Posture Sidecar` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/ExerciseSessionController.java` |
| Preconditions | Synthetic `Mother` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/exercise/PostureAnalysisServiceTest.java` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-API-004`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Invoke `POST /api/v1/exercises/sessions/{sessionId}/posture-events` so `analyzePosture` receives path `sessionId`: `UUID`; body `request`: `PostureEventRequest`; principal `principal`: `Principal`; satisfy `hasAnyRole('MOTHER', 'SYSTEM_ADMIN')` and the extracted `PostureEventRequest` constraints.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | Success status `200` returns `ResponseEntity<ApiResponse<PostureFeedbackResponse>>` with payload fields `postureCode`: `String` (no field annotation in current DTO); `confidenceScore`: `BigDecimal` (no field annotation in current DTO); `severity`: `String` (no field annotation in current DTO); `feedbackText`: `String` (no field annotation in current DTO); the request body is `PostureEventRequest` with `eventTimeMs`: `Long` (@NotNull, @PositiveOrZero); `keypointSummaryJson`: `Map<String, Object>` (@NotNull). | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/ExerciseSessionController.java` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-MH-19` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (Success status `200` returns `ResponseEntity<ApiResponse<PostureFeedbackResponse>>` with payload fields `postureCode`: `String` (no field annotation in current DTO); `confidenceScore`: `BigDecimal` (no field annotation in current DTO); `severity`: `String` (no field annotation in current DTO); `feedbackText`: `String` (no field annotation in current DTO); the request body is `PostureEventRequest` with `eventTimeMs`: `Long` (@NotNull, @PositiveOrZero); `keypointSummaryJson`: `Map<String, Object>` (@NotNull).), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-MH-19-TC-API-004-VAL — POST /api/v1/exercises/sessions/{sessionId}/posture-events rejects a declared request-field boundary

| Field | Specification |
| --- | --- |
| Stable ID | `UC-MH-19-TC-API-004-VAL` |
| Severity | `High` |
| Test Condition | `COND-API-004-VAL` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Backend / Camera / Posture Sidecar` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/ExerciseSessionController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/common/exception/GlobalExceptionHandler.java#handleMethodArgumentNotValid` |
| Preconditions | Synthetic `Mother` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/exercise/PostureAnalysisServiceTest.java` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-API-004-VAL`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Submit `PostureEventRequest` with one field violating its cited validator: `eventTimeMs`: `Long` (@NotNull, @PositiveOrZero); `keypointSummaryJson`: `Map<String, Object>` (@NotNull)

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | HTTP `400` returns the current `ErrorResponse` with `status=400`, `error=VALIDATION_ERROR`, `message=Invalid request`, the request path, and field details with sensitive rejected values masked; the handler/service is not invoked. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/ExerciseSessionController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/common/exception/GlobalExceptionHandler.java#handleMethodArgumentNotValid` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-MH-19` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (HTTP `400` returns the current `ErrorResponse` with `status=400`, `error=VALIDATION_ERROR`, `message=Invalid request`, the request path, and field details with sensitive rejected values masked; the handler/service is not invoked.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-MH-19-TC-API-005 — GET /api/v1/exercises/sessions/{sessionId}/result → getSessionResult contract

| Field | Specification |
| --- | --- |
| Stable ID | `UC-MH-19-TC-API-005` |
| Severity | `High` |
| Test Condition | `COND-API-005` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Backend / Camera / Posture Sidecar` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/ExerciseSessionController.java` |
| Preconditions | Synthetic `Mother` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | Planned — `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/exercise/controller/ExerciseSessionControllerTest.java` (not present at Draft baseline) |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-API-005`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Invoke `GET /api/v1/exercises/sessions/{sessionId}/result` so `getSessionResult` receives path `sessionId`: `UUID`; principal `principal`: `Principal`; satisfy `hasAnyRole('MOTHER', 'SYSTEM_ADMIN')` and the extracted `None` constraints.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | Success status `200` returns `ResponseEntity<ApiResponse<SessionResultResponse>>` with payload fields `exerciseSessionId`: `UUID` (no field annotation in current DTO); `exerciseId`: `UUID` (no field annotation in current DTO); `exerciseTitle`: `String` (no field annotation in current DTO); `sessionStatus`: `String` (no field annotation in current DTO); `startedAt`: `OffsetDateTime` (no field annotation in current DTO); `endedAt`: `OffsetDateTime` (no field annotation in current DTO); `actualDurationSeconds`: `Long` (no field annotation in current DTO); `completionPercent`: `BigDecimal` (no field annotation in current DTO); `postureScore`: `BigDecimal` (no field annotation in current DTO); `warningCount`: `Integer` (no field annotation in current DTO); `summaryJson`: `String` (no field annotation in current DTO); the request body is `None` with Not applicable — no request body. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/ExerciseSessionController.java` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-MH-19` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (Success status `200` returns `ResponseEntity<ApiResponse<SessionResultResponse>>` with payload fields `exerciseSessionId`: `UUID` (no field annotation in current DTO); `exerciseId`: `UUID` (no field annotation in current DTO); `exerciseTitle`: `String` (no field annotation in current DTO); `sessionStatus`: `String` (no field annotation in current DTO); `startedAt`: `OffsetDateTime` (no field annotation in current DTO); `endedAt`: `OffsetDateTime` (no field annotation in current DTO); `actualDurationSeconds`: `Long` (no field annotation in current DTO); `completionPercent`: `BigDecimal` (no field annotation in current DTO); `postureScore`: `BigDecimal` (no field annotation in current DTO); `warningCount`: `Integer` (no field annotation in current DTO); `summaryJson`: `String` (no field annotation in current DTO); the request body is `None` with Not applicable — no request body.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-MH-19-TC-API-006 — PATCH /api/v1/exercises/sessions/{sessionId}/resume → resumeSession contract

| Field | Specification |
| --- | --- |
| Stable ID | `UC-MH-19-TC-API-006` |
| Severity | `High` |
| Test Condition | `COND-API-006` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Backend / Camera / Posture Sidecar` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/ExerciseSessionController.java` |
| Preconditions | Synthetic `Mother` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | Planned — `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/exercise/controller/ExerciseSessionControllerTest.java` (not present at Draft baseline) |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-API-006`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Invoke `PATCH /api/v1/exercises/sessions/{sessionId}/resume` so `resumeSession` receives path `sessionId`: `UUID`; principal `principal`: `Principal`; satisfy `hasAnyRole('MOTHER', 'SYSTEM_ADMIN')` and the extracted `None` constraints.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | Success status `200` returns `ResponseEntity<ApiResponse<SessionStateResponse>>` with payload fields `exerciseSessionId`: `UUID` (no field annotation in current DTO); `sessionStatus`: `String` (no field annotation in current DTO); `pausedSeconds`: `Integer` (no field annotation in current DTO); `warningCount`: `Integer` (no field annotation in current DTO); `updatedAt`: `OffsetDateTime` (no field annotation in current DTO); the request body is `None` with Not applicable — no request body. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/ExerciseSessionController.java` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-MH-19` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (Success status `200` returns `ResponseEntity<ApiResponse<SessionStateResponse>>` with payload fields `exerciseSessionId`: `UUID` (no field annotation in current DTO); `sessionStatus`: `String` (no field annotation in current DTO); `pausedSeconds`: `Integer` (no field annotation in current DTO); `warningCount`: `Integer` (no field annotation in current DTO); `updatedAt`: `OffsetDateTime` (no field annotation in current DTO); the request body is `None` with Not applicable — no request body.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-MH-19-TC-API-007 — GET /api/v1/exercises/{exerciseId}/posture-config → getPostureConfig contract

| Field | Specification |
| --- | --- |
| Stable ID | `UC-MH-19-TC-API-007` |
| Severity | `High` |
| Test Condition | `COND-API-007` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Backend / Camera / Posture Sidecar` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/ExerciseController.java` |
| Preconditions | Synthetic `Mother` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | Planned — `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/exercise/controller/ExerciseControllerTest.java` (not present at Draft baseline) |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-API-007`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Invoke `GET /api/v1/exercises/{exerciseId}/posture-config` so `getPostureConfig` receives path `exerciseId`: `UUID`; satisfy `hasAnyRole('MOTHER', 'SYSTEM_ADMIN')` and the extracted `None` constraints.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | Success status `200` returns `ResponseEntity<ApiResponse<PostureConfigResponse>>` with payload fields `postureConfigId`: `UUID` (no field annotation in current DTO); `exerciseId`: `UUID` (no field annotation in current DTO); `analysisMode`: `String` (no field annotation in current DTO); `ruleOrModelVersion`: `String` (no field annotation in current DTO); `confidenceThreshold`: `BigDecimal` (no field annotation in current DTO); `feedbackLevel`: `String` (no field annotation in current DTO); `effectiveFrom`: `OffsetDateTime` (no field annotation in current DTO); the request body is `None` with Not applicable — no request body. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/ExerciseController.java` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-MH-19` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (Success status `200` returns `ResponseEntity<ApiResponse<PostureConfigResponse>>` with payload fields `postureConfigId`: `UUID` (no field annotation in current DTO); `exerciseId`: `UUID` (no field annotation in current DTO); `analysisMode`: `String` (no field annotation in current DTO); `ruleOrModelVersion`: `String` (no field annotation in current DTO); `confidenceThreshold`: `BigDecimal` (no field annotation in current DTO); `feedbackLevel`: `String` (no field annotation in current DTO); `effectiveFrom`: `OffsetDateTime` (no field annotation in current DTO); the request body is `None` with Not applicable — no request body.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-MH-19-TC-API-008 — POST /api/v1/exercises/{exerciseId}/sessions → startSession contract

| Field | Specification |
| --- | --- |
| Stable ID | `UC-MH-19-TC-API-008` |
| Severity | `High` |
| Test Condition | `COND-API-008` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Backend / Camera / Posture Sidecar` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/ExerciseController.java` |
| Preconditions | Synthetic `Mother` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/exercise/ExerciseSessionServiceTest.java` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-API-008`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Invoke `POST /api/v1/exercises/{exerciseId}/sessions` so `startSession` receives path `exerciseId`: `UUID`; body `request`: `StartSessionRequest`; principal `principal`: `Principal`; satisfy `hasAnyRole('MOTHER', 'SYSTEM_ADMIN')` and the extracted `StartSessionRequest` constraints.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | Success status `201` returns `ResponseEntity<ApiResponse<StartSessionResponse>>` with payload fields `exerciseSessionId`: `UUID` (no field annotation in current DTO); `exerciseId`: `UUID` (no field annotation in current DTO); `userId`: `UUID` (no field annotation in current DTO); `safetyCheckId`: `UUID` (no field annotation in current DTO); `journeyId`: `UUID` (no field annotation in current DTO); `sessionStatus`: `String` (no field annotation in current DTO); `startedAt`: `OffsetDateTime` (no field annotation in current DTO); `supportsPostureAnalysis`: `Boolean` (no field annotation in current DTO); the request body is `StartSessionRequest` with `safetyCheckId`: `UUID` (@NotNull); `journeyId`: `UUID` (no field annotation in current DTO). | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/ExerciseController.java` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-MH-19` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (Success status `201` returns `ResponseEntity<ApiResponse<StartSessionResponse>>` with payload fields `exerciseSessionId`: `UUID` (no field annotation in current DTO); `exerciseId`: `UUID` (no field annotation in current DTO); `userId`: `UUID` (no field annotation in current DTO); `safetyCheckId`: `UUID` (no field annotation in current DTO); `journeyId`: `UUID` (no field annotation in current DTO); `sessionStatus`: `String` (no field annotation in current DTO); `startedAt`: `OffsetDateTime` (no field annotation in current DTO); `supportsPostureAnalysis`: `Boolean` (no field annotation in current DTO); the request body is `StartSessionRequest` with `safetyCheckId`: `UUID` (@NotNull); `journeyId`: `UUID` (no field annotation in current DTO).), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-MH-19-TC-API-008-VAL — POST /api/v1/exercises/{exerciseId}/sessions rejects a declared request-field boundary

| Field | Specification |
| --- | --- |
| Stable ID | `UC-MH-19-TC-API-008-VAL` |
| Severity | `High` |
| Test Condition | `COND-API-008-VAL` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Backend / Camera / Posture Sidecar` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/ExerciseController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/common/exception/GlobalExceptionHandler.java#handleMethodArgumentNotValid` |
| Preconditions | Synthetic `Mother` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/exercise/ExerciseSessionServiceTest.java` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-API-008-VAL`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Submit `StartSessionRequest` with one field violating its cited validator: `safetyCheckId`: `UUID` (@NotNull); `journeyId`: `UUID` (no field annotation in current DTO)

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | HTTP `400` returns the current `ErrorResponse` with `status=400`, `error=VALIDATION_ERROR`, `message=Invalid request`, the request path, and field details with sensitive rejected values masked; the handler/service is not invoked. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/ExerciseController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/common/exception/GlobalExceptionHandler.java#handleMethodArgumentNotValid` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-MH-19` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (HTTP `400` returns the current `ErrorResponse` with `status=400`, `error=VALIDATION_ERROR`, `message=Invalid request`, the request path, and field details with sensitive rejected values masked; the handler/service is not invoked.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-MH-19-TC-API-009 — POST /v1/inference/landmarks → infer contract

| Field | Specification |
| --- | --- |
| Stable ID | `UC-MH-19-TC-API-009` |
| Severity | `High` |
| Test Condition | `COND-API-009` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Backend / Camera / Posture Sidecar` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/MachineLearning/MediaPipe_Posture/exercise_correction_sidecar/app/main.py` |
| Preconditions | Isolated CareBridge Backend adapter request; deterministic sidecar model registry; synthetic landmark payload; no direct end-user invocation |
| Intended Test File | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/exercise/PostureAnalysisServiceTest.java` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-API-009`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Invoke `POST /v1/inference/landmarks` so `infer` receives body `payload`: `InferenceRequest`; satisfy `No explicit internal-key dependency on this handler; router/application policy must be checked` and the extracted `InferenceRequest` constraints.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | Success status `200` returns `InferenceResponse` with payload fields `schemaVersion`: `Literal[RESPONSE_SCHEMA_VERSION]` (`RESPONSE_SCHEMA_VERSION`); `modelVersion`: `Literal[MODEL_VERSION]` (`MODEL_VERSION`); `exerciseKey`: `str` (`required`); `sequenceNumber`: `int` (`required`); `inferenceStreamId`: `str \| None` (`None`); `predictedClass`: `str` (`required`); `stage`: `str \| None` (`None`); `confidence`: `float` (`Field(ge=0.0, le=1.0)`); `correct`: `bool` (`required`); `score`: `float` (`Field(ge=0.0, le=100.0)`); `feedback`: `list[Feedback]` (`required`); the request body is `InferenceRequest` with `schemaVersion`: `Literal[LANDMARK_SCHEMA_VERSION]` (`required`); `modelVersion`: `Literal[MODEL_VERSION]` (`required`); `exerciseKey`: `str` (`Field(min_length=1, max_length=64, pattern=r"^[a-z0-9_]+$")`); `sequenceNumber`: `int` (`Field(ge=0, le=MAX_SEQUENCE_NUMBER)`); `inferenceStreamId`: `str \| None` (`Field(default=None, min_length=1, max_length=128, pattern=r"^[A-Za-z0-9._:-]+$")`); `landmarks`: `dict[str, Landmark]` (`Field(min_length=1, max_length=33)`). | `05_Development/MachineLearning/MediaPipe_Posture/exercise_correction_sidecar/app/main.py` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-MH-19` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (Success status `200` returns `InferenceResponse` with payload fields `schemaVersion`: `Literal[RESPONSE_SCHEMA_VERSION]` (`RESPONSE_SCHEMA_VERSION`); `modelVersion`: `Literal[MODEL_VERSION]` (`MODEL_VERSION`); `exerciseKey`: `str` (`required`); `sequenceNumber`: `int` (`required`); `inferenceStreamId`: `str | None` (`None`); `predictedClass`: `str` (`required`); `stage`: `str | None` (`None`); `confidence`: `float` (`Field(ge=0.0, le=1.0)`); `correct`: `bool` (`required`); `score`: `float` (`Field(ge=0.0, le=100.0)`); `feedback`: `list[Feedback]` (`required`); the request body is `InferenceRequest` with `schemaVersion`: `Literal[LANDMARK_SCHEMA_VERSION]` (`required`); `modelVersion`: `Literal[MODEL_VERSION]` (`required`); `exerciseKey`: `str` (`Field(min_length=1, max_length=64, pattern=r"^[a-z0-9_]+$")`); `sequenceNumber`: `int` (`Field(ge=0, le=MAX_SEQUENCE_NUMBER)`); `inferenceStreamId`: `str | None` (`Field(default=None, min_length=1, max_length=128, pattern=r"^[A-Za-z0-9._:-]+$")`); `landmarks`: `dict[str, Landmark]` (`Field(min_length=1, max_length=33)`).), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-MH-19-TC-API-009-VAL — POST /v1/inference/landmarks rejects a declared request-field boundary

| Field | Specification |
| --- | --- |
| Stable ID | `UC-MH-19-TC-API-009-VAL` |
| Severity | `High` |
| Test Condition | `COND-API-009-VAL` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Backend / Camera / Posture Sidecar` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/MachineLearning/MediaPipe_Posture/exercise_correction_sidecar/app/main.py` |
| Preconditions | Isolated CareBridge Backend adapter request; deterministic sidecar model registry; synthetic landmark payload; no direct end-user invocation |
| Intended Test File | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/exercise/PostureAnalysisServiceTest.java` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-API-009-VAL`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Submit `InferenceRequest` with one field violating its cited validator: `schemaVersion`: `Literal[LANDMARK_SCHEMA_VERSION]` (`required`); `modelVersion`: `Literal[MODEL_VERSION]` (`required`); `exerciseKey`: `str` (`Field(min_length=1, max_length=64, pattern=r"^[a-z0-9_]+$")`); `sequenceNumber`: `int` (`Field(ge=0, le=MAX_SEQUENCE_NUMBER)`); `inferenceStreamId`: `str | None` (`Field(default=None, min_length=1, max_length=128, pattern=r"^[A-Za-z0-9._:-]+$")`); `landmarks`: `dict[str, Landmark]` (`Field(min_length=1, max_length=33)`)

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | HTTP `422` returns the sidecar `ErrorResponse` with `code=INVALID_INPUT` and `message=The inference request is invalid`; no inference result is produced. | `05_Development/MachineLearning/MediaPipe_Posture/exercise_correction_sidecar/app/main.py` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-MH-19` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (HTTP `422` returns the sidecar `ErrorResponse` with `code=INVALID_INPUT` and `message=The inference request is invalid`; no inference result is produced.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-MH-19-TC-BR-001 — Enforce business rule 1

| Field | Specification |
| --- | --- |
| Stable ID | `UC-MH-19-TC-BR-001` |
| Severity | `High` |
| Test Condition | `COND-BR-01` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Backend / Camera / Posture Sidecar` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `TDS BR-01; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/service/impl/PostureAnalysisServiceImpl.java` |
| Preconditions | Synthetic `Mother` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/exercise/ExerciseSessionServiceTest.java` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-BR-01`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Exercise the eligible and ineligible partitions for this rule: Server session state is canonical; camera/model feedback is advisory.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | The server enforces: Server session state is canonical; camera/model feedback is advisory. No disallowed state or protected data is produced. | `TDS BR-01; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/service/impl/PostureAnalysisServiceImpl.java` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-MH-19` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (The server enforces: Server session state is canonical; camera/model feedback is advisory. No disallowed state or protected data is produced.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-MH-19-TC-BR-002 — Enforce business rule 2

| Field | Specification |
| --- | --- |
| Stable ID | `UC-MH-19-TC-BR-002` |
| Severity | `High` |
| Test Condition | `COND-BR-02` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Backend / Camera / Posture Sidecar` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `TDS BR-02; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/service/impl/PostureAnalysisServiceImpl.java` |
| Preconditions | Synthetic `Mother` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/exercise/ExerciseSessionHistoryEmbeddedPostgresTest.java` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-BR-02`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Exercise the eligible and ineligible partitions for this rule: The sidecar inference contract validates sequence and landmark payloads; provider failure follows the implemented backend fallback/degraded path.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | The server enforces: The sidecar inference contract validates sequence and landmark payloads; provider failure follows the implemented backend fallback/degraded path. No disallowed state or protected data is produced. | `TDS BR-02; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/service/impl/PostureAnalysisServiceImpl.java` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-MH-19` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (The server enforces: The sidecar inference contract validates sequence and landmark payloads; provider failure follows the implemented backend fallback/degraded path. No disallowed state or protected data is produced.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-MH-19-TC-BR-003 — Enforce business rule 3

| Field | Specification |
| --- | --- |
| Stable ID | `UC-MH-19-TC-BR-003` |
| Severity | `High` |
| Test Condition | `COND-BR-03` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Backend / Camera / Posture Sidecar` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `TDS BR-03; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/service/impl/PostureAnalysisServiceImpl.java` |
| Preconditions | Synthetic `Mother` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/exercise/PostureAnalysisServiceTest.java` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-BR-03`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Exercise the eligible and ineligible partitions for this rule: Late frames and retries must not mutate a completed/aborted session.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | The server enforces: Late frames and retries must not mutate a completed/aborted session. No disallowed state or protected data is produced. | `TDS BR-03; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/service/impl/PostureAnalysisServiceImpl.java` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-MH-19` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (The server enforces: Late frames and retries must not mutate a completed/aborted session. No disallowed state or protected data is produced.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-MH-19-TC-SEC-001 — Reject wrong authentication, role, ownership, membership, or consent scope

| Field | Specification |
| --- | --- |
| Stable ID | `UC-MH-19-TC-SEC-001` |
| Severity | `Critical` |
| Test Condition | `COND-AUTH` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Backend / Camera / Posture Sidecar` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `TDS Sections 4 and 16; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/ExerciseController.java` |
| Preconditions | Synthetic `Mother` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/exercise/ExerciseSessionServiceTest.java` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-AUTH`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Invoke every protected operation with an unauthenticated principal and the closest disallowed role/scope partition.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | The request fails closed using the exact current mapped status/code and returns no protected resource fields or side effects. | `TDS Sections 4 and 16; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/ExerciseController.java` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-MH-19` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (The request fails closed using the exact current mapped status/code and returns no protected resource fields or side effects.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-MH-19-TC-GAP-001 — Prove the current actor entry path and owning contract remain reachable

| Field | Specification |
| --- | --- |
| Stable ID | `UC-MH-19-TC-GAP-001` |
| Severity | `Medium` |
| Test Condition | `COND-GAP` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Backend / Camera / Posture Sidecar` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `SRS UC-MH-19 Implemented Entry Points/Contracts; 05_Development/CareBridgeMobileApp/lib/features/exercise/screens/exercise_session_screen.dart` |
| Preconditions | Synthetic `Mother` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/exercise/ExerciseSessionServiceTest.java` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-GAP`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Start from `Nested Mobile exercise session, result, and history screens`, perform `Load the active posture configuration and start a session after a passed safety check/camera permission.`, and observe the owning contract `GET `/api/v1/exercises/{id}/posture-config`` rather than a retired or unrelated route.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | The current entry path remains reachable for `Perform Exercise Session and Review Results` and invokes only the documented owning contract; an unreachable, static, or wrong-method path fails this case. | `SRS UC-MH-19 Implemented Entry Points/Contracts; 05_Development/CareBridgeMobileApp/lib/features/exercise/screens/exercise_session_screen.dart` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-MH-19` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (The current entry path remains reachable for `Perform Exercise Session and Review Results` and invokes only the documented owning contract; an unreachable, static, or wrong-method path fails this case.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### 4.3 Coverage Families

| Behavior family | Conditions | Cases |
| --- | --- | --- |
| Load the active posture configuration and start a session after a passed safety check/camera permission. | `COND-01` | `UC-MH-19-TC-001` |
| Process supported posture observations through the configured posture-correction sidecar or implemented fallback. | `COND-02` | `UC-MH-19-TC-002` |
| Complete/abort and review the stored result/history. | `COND-03` | `UC-MH-19-TC-003` |
| GET /api/v1/exercises/sessions/history → getSessionHistory contract | `COND-API-001` | `UC-MH-19-TC-API-001` |
| PATCH /api/v1/exercises/sessions/{sessionId}/complete → completeSession contract | `COND-API-002` | `UC-MH-19-TC-API-002` |
| PATCH /api/v1/exercises/sessions/{sessionId}/pause → pauseSession contract | `COND-API-003` | `UC-MH-19-TC-API-003` |
| POST /api/v1/exercises/sessions/{sessionId}/posture-events → analyzePosture contract | `COND-API-004` | `UC-MH-19-TC-API-004` |
| POST /api/v1/exercises/sessions/{sessionId}/posture-events rejects a declared request-field boundary | `COND-API-004-VAL` | `UC-MH-19-TC-API-004-VAL` |
| GET /api/v1/exercises/sessions/{sessionId}/result → getSessionResult contract | `COND-API-005` | `UC-MH-19-TC-API-005` |
| PATCH /api/v1/exercises/sessions/{sessionId}/resume → resumeSession contract | `COND-API-006` | `UC-MH-19-TC-API-006` |
| GET /api/v1/exercises/{exerciseId}/posture-config → getPostureConfig contract | `COND-API-007` | `UC-MH-19-TC-API-007` |
| POST /api/v1/exercises/{exerciseId}/sessions → startSession contract | `COND-API-008` | `UC-MH-19-TC-API-008` |
| POST /api/v1/exercises/{exerciseId}/sessions rejects a declared request-field boundary | `COND-API-008-VAL` | `UC-MH-19-TC-API-008-VAL` |
| POST /v1/inference/landmarks → infer contract | `COND-API-009` | `UC-MH-19-TC-API-009` |
| POST /v1/inference/landmarks rejects a declared request-field boundary | `COND-API-009-VAL` | `UC-MH-19-TC-API-009-VAL` |
| Business-rule partitions | `COND-BR-*` | `UC-MH-19-TC-BR-*` |
| Authentication / authorization / ownership / consent | `COND-AUTH` | `UC-MH-19-TC-SEC-001` |
| Current gap / reachability boundary | `COND-GAP` | `UC-MH-19-TC-GAP-001` |

## 5. Red-Green-Refactor Tracker

| TC ID | Intended file | Red evidence | Green evidence | Refactor verification | Status |
| --- | --- | --- | --- | --- | --- |
| `UC-MH-19-TC-001` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/exercise/ExerciseSessionServiceTest.java` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-MH-19-TC-002` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/exercise/ExerciseSessionServiceTest.java` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-MH-19-TC-003` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/exercise/ExerciseSessionServiceTest.java` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-MH-19-TC-API-001` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/exercise/ExerciseSessionHistoryEmbeddedPostgresTest.java` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-MH-19-TC-API-002` | Planned — `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/exercise/controller/ExerciseSessionControllerTest.java` (not present at Draft baseline) | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-MH-19-TC-API-003` | Planned — `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/exercise/controller/ExerciseSessionControllerTest.java` (not present at Draft baseline) | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-MH-19-TC-API-004` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/exercise/PostureAnalysisServiceTest.java` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-MH-19-TC-API-004-VAL` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/exercise/PostureAnalysisServiceTest.java` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-MH-19-TC-API-005` | Planned — `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/exercise/controller/ExerciseSessionControllerTest.java` (not present at Draft baseline) | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-MH-19-TC-API-006` | Planned — `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/exercise/controller/ExerciseSessionControllerTest.java` (not present at Draft baseline) | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-MH-19-TC-API-007` | Planned — `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/exercise/controller/ExerciseControllerTest.java` (not present at Draft baseline) | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-MH-19-TC-API-008` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/exercise/ExerciseSessionServiceTest.java` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-MH-19-TC-API-008-VAL` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/exercise/ExerciseSessionServiceTest.java` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-MH-19-TC-API-009` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/exercise/PostureAnalysisServiceTest.java` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-MH-19-TC-API-009-VAL` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/exercise/PostureAnalysisServiceTest.java` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-MH-19-TC-BR-001` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/exercise/ExerciseSessionServiceTest.java` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-MH-19-TC-BR-002` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/exercise/ExerciseSessionServiceTest.java` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-MH-19-TC-BR-003` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/exercise/ExerciseSessionServiceTest.java` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-MH-19-TC-SEC-001` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/exercise/ExerciseSessionServiceTest.java` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-MH-19-TC-GAP-001` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/exercise/ExerciseSessionServiceTest.java` | Not run | Not run | Not run | 🔴 Not written / rerun required |

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
| Generic test matrix | Case titles/actions reference `Perform Exercise Session and Review Results` operations and rules. | Pass |
| False Green claim | Current command/time/count evidence is required. | Pass — all rows remain Red/not rerun. |
| Hidden contradiction | Section 2 records each known gap. | Resolved broad-boundary issue recorded |
| Missing Props Isolation | Applicable Java/TS/Dart factory pattern is present. | Pass at specification level |
| Cross-test pollution | TDS-05 defines actor/resource/provider cleanup. | Draft gate — implementation review must prove teardown/rollback before Green evidence is accepted |
| Wrong-layer test | Applicability matrix marks absent consumers/layers Not applicable. | Pass |
| Uncovered contract | Operations/rules/auth/gap map to conditions and detailed TCs. | Pass for handler/DTO/status/operation/rule/auth/gap mappings; service-only events/codes remain visible in paired TDS |
| Unsafe data | Synthetic-only rule; no production credentials/protected data. | Pass at specification level |
| AI safety bypass | Deterministic policy cannot be lowered by model output when AI applies. | Not applicable — no clinical/moderation generation in this UC |

- [ ] Human reviewer confirms all eight sections, oracle sources, detailed TCs, applicability, Red Gate, rollback, and paired-TDS traceability before approval.
