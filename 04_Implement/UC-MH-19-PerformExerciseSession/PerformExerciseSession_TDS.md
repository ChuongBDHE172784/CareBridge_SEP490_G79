# ENGINEERING DOCUMENTATION STANDARD (EDS) v2.0

# TECHNICAL DESIGN SPECIFICATION — Perform Exercise Session and Review Results

| Field | Value |
| --- | --- |
| Document ID | `UC-MH-19-TDS` |
| Version | `0.1` |
| Date | `2026-08-23` |
| Status | `Draft` |
| Function ID | `UC-MH-19` |
| Canonical Use Case | `UC-MH-19 — Perform Exercise Session and Review Results` |
| Module / Bounded Context | `Mother Journey and Health` |
| Primary Actor | `Mother` |
| Platforms | `Mobile / Backend / Camera / Posture Sidecar` |
| Priority | `Medium` |
| Data Classification | `Restricted maternal health, screening, journey, record, and attachment data; Confidential schedule/preferences` |
| Compliance Scope | `PDPA health-data minimization, consent/ownership enforcement, clinical disclaimer where applicable, and purpose-bound file access` |
| Owner | `CareBridge Team` |
| Reviewer / Approver |  |
| Source Baseline | Current worktree on `2026-08-23`; SRS `UC-MH-19`; exact evidence in Section 1.4 |

## CHANGELOG

| Version | Date | Author | Change | Status |
| --- | --- | --- | --- | --- |
| 0.1 | 2026-08-23 | CareBridge Team | Initial evidence-first full-form draft | Draft |

## TABLE OF CONTENTS

1. Module Overview
2. Traceability Matrix
3. Architecture Decision Records
4. Non-Functional Requirements and SLA
5. Static Modeling
6. Dynamic Modeling
7. Domain Event Catalog
8. Interface Specification
9. API Specification
10. Error Codes
11. Implementation and Deployment Plan
12. Rollback and Incident Runbook
13. Verification Scenario Groups
14. Verification Methods
15. Verification Samples
16. Authorization Matrix
17. AI Prompt Constraints — CASE 2.0

## 1. Module Overview

### 1.1 Actor Goal, Trigger, and Outcome

- **Goal:** Load the active posture configuration, start an eligible exercise session, run posture analysis, complete/abort it, and review the result or history.
- **Trigger:** The actor enters Nested Mobile exercise session, result, and history screens.
- **Outcome:** Complete/abort and review the stored result/history.
- **Current state:** `High` confidence from reachable code/test audit; no manifest-level exclusion is recorded.
- **Target state:** Preserve current code-backed behavior and resolve only explicitly evidenced limitations through approved implementation work.

### 1.2 Scope

**In scope**

- Nested Mobile exercise session, result, and history screens

- GET `/api/v1/exercises/{id}/posture-config`
- POST `/api/v1/exercises/{id}/sessions`
- Exercise-session endpoints under `/api/v1/exercises/sessions/**`
- POST `/v1/inference/landmarks`

**Out of scope / limitations**

- Not applicable — no manifest-level exclusion is recorded. Scope is limited to the entry points and exact contracts listed above; unrelated handlers in the same module are excluded.

### 1.3 Preconditions and Postconditions

| Type | Condition |
| --- | --- |
| Precondition | Mother is authenticated/authorized where the current contract requires it. |
| Precondition | Required ownership, membership, consent, resource state, and device/provider prerequisites pass current policies. |
| Postcondition | Complete/abort and review the stored result/history. |
| Postcondition | No side effect outside the feature-owned persistence/event/provider boundary occurs. |

### 1.4 Evidence Baseline

| Source ID | Type | Exact path | Revision | Authority |
| --- | --- | --- | --- | --- |
| `SRC-SRS-01` | Requirement | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-MH-19 | 2026-08-23 | Draft code-first requirement |
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

### 1.5 Open Contradictions / Questions

- No material contradiction was found among the exact sources cited in Section 1.4. Behavior not evidenced by those sources is not approved or implied by this Draft.

## 2. Traceability Matrix

| Requirement | Behavior | Exact oracle source | Component | Test condition / case |
| --- | --- | --- | --- | --- |
| `UC-MH-19-FR-01` | Load the active posture configuration and start a session after a passed safety check/camera permission. | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-MH-19 Normal Flow 1 | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/ExerciseSessionController.java` | `COND-01` / `UC-MH-19-TC-001` |
| `UC-MH-19-FR-02` | Process supported posture observations through the configured posture-correction sidecar or implemented fallback. | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-MH-19 Normal Flow 2 | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/ExerciseSessionController.java` | `COND-02` / `UC-MH-19-TC-002` |
| `UC-MH-19-FR-03` | Complete/abort and review the stored result/history. | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-MH-19 Normal Flow 3 | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/ExerciseSessionController.java` | `COND-03` / `UC-MH-19-TC-003` |
| `BR-01` | Server session state is canonical; camera/model feedback is advisory. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/service/impl/PostureAnalysisServiceImpl.java` | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/service/impl/PostureAnalysisServiceImpl.java` | `COND-BR-01` / `UC-MH-19-TC-BR-001` |
| `BR-02` | The sidecar inference contract validates sequence and landmark payloads; provider failure follows the implemented backend fallback/degraded path. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/service/impl/PostureAnalysisServiceImpl.java` | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/service/impl/PostureAnalysisServiceImpl.java` | `COND-BR-02` / `UC-MH-19-TC-BR-002` |
| `BR-03` | Late frames and retries must not mutate a completed/aborted session. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/service/impl/PostureAnalysisServiceImpl.java` | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/service/impl/PostureAnalysisServiceImpl.java` | `COND-BR-03` / `UC-MH-19-TC-BR-003` |

## 3. Architecture Decision Records (ADR)

### ADR-UC-MH-19-01 — Use a distinct actor-goal boundary

| Item | Decision |
| --- | --- |
| Context | The retired 43-UC catalogue grouped multiple triggers, lifecycles, and permission boundaries, making implementation/test traceability generic. |
| Options | Keep the broad catalogue; split by screen; split by actor goal plus lifecycle/security boundary. |
| Decision | Use `UC-MH-19 — Perform Exercise Session and Review Results` as the canonical boundary because its operations share the stated actor outcome and current implementation evidence. |
| Consequences | Related supporting screens/endpoints stay in one TDS; different lifecycle/actor outcomes have separate UCs. |
| Source / Status | SRS Section 3.1 and current code audit / Draft |

### ADR-UC-MH-19-02 — Preserve unknowns as Open

| Item | Decision |
| --- | --- |
| Context | Exact schema fields, SLA values, and some controller error codes are not fully evidenced by this manifest alone. |
| Decision | Do not invent them. Mark them `Open` and require the exact DTO/entity/migration/policy oracle before production-code changes. |
| Consequences | This document accurately characterizes current scope; unresolved design-changing items block implementation approval. |
| Source / Status | `create-specs` evidence discipline / Draft |

## 4. Non-Functional Requirements and SLA

| NFR | Target | Oracle Source | Verification |
| --- | --- | --- | --- |
| Authorization / isolation | All requests follow exact role/ownership/membership/consent policy in current code. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/ExerciseController.java` | Negative security cases in paired Test-Spec |
| Protected-data handling | No secrets or unnecessary health/location/identity/conversation/file payloads in logs, fixtures, screenshots, or audit detail. | Data classification header plus exact Section 9 request/response field inventories | Log/fixture review plus security tests |
| Availability / latency | Open — no approved feature-specific numeric SLA found. | Evidence needed: approved NFR/SLA source | Measure only; do not assert a fixed threshold |
| Accessibility | Open — confirm project-standard criteria for reachable UI surfaces. | Evidence needed: approved UX/accessibility standard | Applicable Web/Mobile UI checks |
| Retry / idempotency | Apply only semantics explicitly implemented by the owning service. | No explicit lock/version/idempotency marker is evidenced in the cited implementation sources; preserve observed behavior and add characterization before changing concurrency semantics | State/duplicate/concurrency cases where applicable |

## 5. Static Modeling

### 5.1 Component Responsibilities and Change Disposition

| Exact path | Disposition | Responsibility |
| --- | --- | --- |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/ExerciseController.java` | Reuse | Current implementation evidence for Perform Exercise Session and Review Results; inspect the exact symbol before implementation changes. |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/ExerciseSessionController.java` | Reuse | Current implementation evidence for Perform Exercise Session and Review Results; inspect the exact symbol before implementation changes. |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/service/impl/PostureAnalysisServiceImpl.java` | Reuse | Current implementation evidence for Perform Exercise Session and Review Results; inspect the exact symbol before implementation changes. |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/inference/ExerciseCorrectionHttpAdapter.java` | Reuse | Current implementation evidence for Perform Exercise Session and Review Results; inspect the exact symbol before implementation changes. |
| `05_Development/MachineLearning/MediaPipe_Posture/exercise_correction_sidecar/app/main.py` | Reuse | Current implementation evidence for Perform Exercise Session and Review Results; inspect the exact symbol before implementation changes. |
| `05_Development/CareBridgeMobileApp/lib/features/exercise/screens/exercise_session_screen.dart` | Reuse | Current implementation evidence for Perform Exercise Session and Review Results; inspect the exact symbol before implementation changes. |

### 5.2 Current Component Diagram

```plantuml
@startuml
skinparam classAttributeIconSize 0
class ExerciseController as "ExerciseController.java"
class ExerciseSessionController as "ExerciseSessionController.java"
ExerciseController --> ExerciseSessionController
class PostureAnalysisServiceImpl as "PostureAnalysisServiceImpl.java"
ExerciseSessionController --> PostureAnalysisServiceImpl
class ExerciseCorrectionHttpAdapter as "ExerciseCorrectionHttpAdapter.java"
PostureAnalysisServiceImpl --> ExerciseCorrectionHttpAdapter
class main as "main.py"
ExerciseCorrectionHttpAdapter --> main
class exercise_session_screen as "exercise_session_screen.dart"
main --> exercise_session_screen
@enduml
```

### 5.3 Data / Schema / Migration Assessment

| Item | Assessment |
| --- | --- |
| Current stores/entities | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/entity/AnalysisMode.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/entity/DifficultyLevel.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/entity/ExerciseSession.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/entity/PostureAnalysisConfig.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/entity/SessionStatus.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/entity/TrimesterScope.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/repository/ExerciseSessionRepository.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/repository/PostureAnalysisConfigRepository.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/repository/PostureFeedbackEventRepository.java` |
| Sensitive fields | Restricted maternal health, screening, journey, record, and attachment data; Confidential schedule/preferences. Exact transport fields and validators are enumerated per handler in Section 9; entity-only fields require the cited service/entity source before a schema change. |
| Schema delta for documentation alignment | Not applicable — this Draft does not change runtime schema. |
| Future implementation migration | Use additive, versioned migrations only when an approved behavior requires schema change. |
| V1 synchronization | Not applicable — this documentation alignment changes no schema; any future schema work must inspect current Flyway history and must never rewrite an applied migration. |

## 6. Dynamic Modeling

### 6.1 Happy Path Sequence

```plantuml
@startuml
actor Actor
participant Client
participant Domain
Actor -> Client: Enter Perform Exercise Session and Review Results
Client -> Domain: Load the active posture configuration and start a session after a passed safety check/camera permission.
Domain --> Client: Result for step 1
Client -> Domain: Process supported posture observations through the configured posture-correction sidecar or implemented fallback.
Domain --> Client: Result for step 2
Client -> Domain: Complete/abort and review the stored result/history.
Domain --> Client: Result for step 3
Client --> Actor: Render canonical outcome
@enduml
```

### 6.2 Alternative, Error, Retry, and Concurrency Flows

| Flow | Expected design behavior | Oracle |
| --- | --- | --- |
| Cancel before mutation | No unintended write or provider side effect. | SRS UC-MH-19 Alternative Flow |
| Invalid input/state | Reject with the current contract; keep canonical state unchanged. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/ExerciseController.java` |
| Wrong actor/scope | Fail closed without protected resource disclosure. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/ExerciseController.java` |
| Dependency failure | Use only the implemented bounded fallback/retry; never report false success. | Not applicable at this cited baseline — no external adapter/provider import is evidenced in the listed implementation sources |
| Duplicate/concurrent mutation | Apply only current lock/version/idempotency semantics. | No explicit lock/version/idempotency marker is evidenced in the cited implementation sources; preserve observed behavior and add characterization before changing concurrency semantics |

### 6.3 State Model and Invariants

```plantuml
@startuml
[*] --> Eligible
Eligible --> InProgress : Load the active posture configuration and start a session after a passed safety check/camera permission.
InProgress --> Outcome : Complete/abort and review the stored result/history.
InProgress --> Rejected : validation / authorization / state failure
Outcome --> [*]
Rejected --> Eligible : actor corrects eligible input
@enduml
```

- Server session state is canonical; camera/model feedback is advisory.
- The sidecar inference contract validates sequence and landmark payloads; provider failure follows the implemented backend fallback/degraded path.
- Late frames and retries must not mutate a completed/aborted session.

## 7. Domain Event Catalog

| Direction | Event | Producer / Consumer | Payload / delivery / idempotency |
| --- | --- | --- | --- |
| Publish / consume | Current event evidence | Not applicable at this cited baseline — no event type, publisher, or listener is evidenced in the listed implementation sources | Preserve only events evidenced by the cited source set; if later call-path inspection finds none, the flow remains synchronous. |

## 8. Interface Specification

### 8.1 User / Operator Interfaces

| # | Entry point | Actor | Contract |
| ---: | --- | --- | --- |
| 1 | Nested Mobile exercise session, result, and history screens | Mother | Reachable current entry point |

### 8.2 Service / Repository Interfaces

| Interface evidence | Required responsibility |
| --- | --- |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/ExerciseController.java` | Support the mapped operations without broadening authorization or lifecycle semantics. |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/ExerciseSessionController.java` | Support the mapped operations without broadening authorization or lifecycle semantics. |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/service/impl/PostureAnalysisServiceImpl.java` | Support the mapped operations without broadening authorization or lifecycle semantics. |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/inference/ExerciseCorrectionHttpAdapter.java` | Support the mapped operations without broadening authorization or lifecycle semantics. |
| `05_Development/MachineLearning/MediaPipe_Posture/exercise_correction_sidecar/app/main.py` | Support the mapped operations without broadening authorization or lifecycle semantics. |
| `05_Development/CareBridgeMobileApp/lib/features/exercise/screens/exercise_session_screen.dart` | Support the mapped operations without broadening authorization or lifecycle semantics. |

## 9. API Specification

| ID | Method / path or grouped controller surface | Auth / role | Request / response / errors |
| --- | --- | --- | --- |
| `API-01` | `GET /api/v1/exercises/sessions/history` | hasAnyRole('MOTHER', 'SYSTEM_ADMIN') | Handler `getSessionHistory`; parameters: query `trimesterScope`: `TrimesterScope`; query `from`: `OffsetDateTime`; query `to`: `OffsetDateTime`; query `page`: `int`; query `size`: `int`; principal `principal`: `Principal`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<PaginatedResponse<ExerciseSessionHistorySummary>>`; response payload fields: Not applicable or unresolved from the handler import; explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/ExerciseSessionController.java` |
| `API-02` | `PATCH /api/v1/exercises/sessions/{sessionId}/complete` | hasAnyRole('MOTHER', 'SYSTEM_ADMIN') | Handler `completeSession`; parameters: path `sessionId`: `UUID`; principal `principal`: `Principal`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<SessionResultResponse>>`; response payload fields: `exerciseSessionId`: `UUID` (no field annotation in current DTO); `exerciseId`: `UUID` (no field annotation in current DTO); `exerciseTitle`: `String` (no field annotation in current DTO); `sessionStatus`: `String` (no field annotation in current DTO); `startedAt`: `OffsetDateTime` (no field annotation in current DTO); `endedAt`: `OffsetDateTime` (no field annotation in current DTO); `actualDurationSeconds`: `Long` (no field annotation in current DTO); `completionPercent`: `BigDecimal` (no field annotation in current DTO); `postureScore`: `BigDecimal` (no field annotation in current DTO); `warningCount`: `Integer` (no field annotation in current DTO); `summaryJson`: `String` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/ExerciseSessionController.java` |
| `API-03` | `PATCH /api/v1/exercises/sessions/{sessionId}/pause` | hasAnyRole('MOTHER', 'SYSTEM_ADMIN') | Handler `pauseSession`; parameters: path `sessionId`: `UUID`; principal `principal`: `Principal`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<SessionStateResponse>>`; response payload fields: `exerciseSessionId`: `UUID` (no field annotation in current DTO); `sessionStatus`: `String` (no field annotation in current DTO); `pausedSeconds`: `Integer` (no field annotation in current DTO); `warningCount`: `Integer` (no field annotation in current DTO); `updatedAt`: `OffsetDateTime` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/ExerciseSessionController.java` |
| `API-04` | `POST /api/v1/exercises/sessions/{sessionId}/posture-events` | hasAnyRole('MOTHER', 'SYSTEM_ADMIN') | Handler `analyzePosture`; parameters: path `sessionId`: `UUID`; body `request`: `PostureEventRequest`; principal `principal`: `Principal`; request body: `PostureEventRequest`; request fields/validation: `eventTimeMs`: `Long` (@NotNull, @PositiveOrZero); `keypointSummaryJson`: `Map<String, Object>` (@NotNull); response: `ResponseEntity<ApiResponse<PostureFeedbackResponse>>`; response payload fields: `postureCode`: `String` (no field annotation in current DTO); `confidenceScore`: `BigDecimal` (no field annotation in current DTO); `severity`: `String` (no field annotation in current DTO); `feedbackText`: `String` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/ExerciseSessionController.java` |
| `API-05` | `GET /api/v1/exercises/sessions/{sessionId}/result` | hasAnyRole('MOTHER', 'SYSTEM_ADMIN') | Handler `getSessionResult`; parameters: path `sessionId`: `UUID`; principal `principal`: `Principal`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<SessionResultResponse>>`; response payload fields: `exerciseSessionId`: `UUID` (no field annotation in current DTO); `exerciseId`: `UUID` (no field annotation in current DTO); `exerciseTitle`: `String` (no field annotation in current DTO); `sessionStatus`: `String` (no field annotation in current DTO); `startedAt`: `OffsetDateTime` (no field annotation in current DTO); `endedAt`: `OffsetDateTime` (no field annotation in current DTO); `actualDurationSeconds`: `Long` (no field annotation in current DTO); `completionPercent`: `BigDecimal` (no field annotation in current DTO); `postureScore`: `BigDecimal` (no field annotation in current DTO); `warningCount`: `Integer` (no field annotation in current DTO); `summaryJson`: `String` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/ExerciseSessionController.java` |
| `API-06` | `PATCH /api/v1/exercises/sessions/{sessionId}/resume` | hasAnyRole('MOTHER', 'SYSTEM_ADMIN') | Handler `resumeSession`; parameters: path `sessionId`: `UUID`; principal `principal`: `Principal`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<SessionStateResponse>>`; response payload fields: `exerciseSessionId`: `UUID` (no field annotation in current DTO); `sessionStatus`: `String` (no field annotation in current DTO); `pausedSeconds`: `Integer` (no field annotation in current DTO); `warningCount`: `Integer` (no field annotation in current DTO); `updatedAt`: `OffsetDateTime` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/ExerciseSessionController.java` |
| `API-07` | `GET /api/v1/exercises/{exerciseId}/posture-config` | hasAnyRole('MOTHER', 'SYSTEM_ADMIN') | Handler `getPostureConfig`; parameters: path `exerciseId`: `UUID`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<PostureConfigResponse>>`; response payload fields: `postureConfigId`: `UUID` (no field annotation in current DTO); `exerciseId`: `UUID` (no field annotation in current DTO); `analysisMode`: `String` (no field annotation in current DTO); `ruleOrModelVersion`: `String` (no field annotation in current DTO); `confidenceThreshold`: `BigDecimal` (no field annotation in current DTO); `feedbackLevel`: `String` (no field annotation in current DTO); `effectiveFrom`: `OffsetDateTime` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/ExerciseController.java` |
| `API-08` | `POST /api/v1/exercises/{exerciseId}/sessions` | hasAnyRole('MOTHER', 'SYSTEM_ADMIN') | Handler `startSession`; parameters: path `exerciseId`: `UUID`; body `request`: `StartSessionRequest`; principal `principal`: `Principal`; request body: `StartSessionRequest`; request fields/validation: `safetyCheckId`: `UUID` (@NotNull); `journeyId`: `UUID` (no field annotation in current DTO); response: `ResponseEntity<ApiResponse<StartSessionResponse>>`; response payload fields: `exerciseSessionId`: `UUID` (no field annotation in current DTO); `exerciseId`: `UUID` (no field annotation in current DTO); `userId`: `UUID` (no field annotation in current DTO); `safetyCheckId`: `UUID` (no field annotation in current DTO); `journeyId`: `UUID` (no field annotation in current DTO); `sessionStatus`: `String` (no field annotation in current DTO); `startedAt`: `OffsetDateTime` (no field annotation in current DTO); `supportsPostureAnalysis`: `Boolean` (no field annotation in current DTO); explicit/documented statuses: `201`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/ExerciseController.java` |
| `API-09` | `POST /v1/inference/landmarks` | No explicit internal-key dependency on this handler; router/application policy must be checked | Handler `infer`; parameters: body `payload`: `InferenceRequest`; request body: `InferenceRequest`; request fields/validation: `schemaVersion`: `Literal[LANDMARK_SCHEMA_VERSION]` (`required`); `modelVersion`: `Literal[MODEL_VERSION]` (`required`); `exerciseKey`: `str` (`Field(min_length=1, max_length=64, pattern=r"^[a-z0-9_]+$")`); `sequenceNumber`: `int` (`Field(ge=0, le=MAX_SEQUENCE_NUMBER)`); `inferenceStreamId`: `str \| None` (`Field(default=None, min_length=1, max_length=128, pattern=r"^[A-Za-z0-9._:-]+$")`); `landmarks`: `dict[str, Landmark]` (`Field(min_length=1, max_length=33)`); response: `InferenceResponse`; response payload fields: `schemaVersion`: `Literal[RESPONSE_SCHEMA_VERSION]` (`RESPONSE_SCHEMA_VERSION`); `modelVersion`: `Literal[MODEL_VERSION]` (`MODEL_VERSION`); `exerciseKey`: `str` (`required`); `sequenceNumber`: `int` (`required`); `inferenceStreamId`: `str \| None` (`None`); `predictedClass`: `str` (`required`); `stage`: `str \| None` (`None`); `confidence`: `float` (`Field(ge=0.0, le=1.0)`); `correct`: `bool` (`required`); `score`: `float` (`Field(ge=0.0, le=100.0)`); `feedback`: `list[Feedback]` (`required`); explicit/documented statuses: `200, 400, 422, 503`; source: `05_Development/MachineLearning/MediaPipe_Posture/exercise_correction_sidecar/app/main.py` |

Method-level Spring handlers, authorization annotations, request DTO fields/validators, response payload fields, and explicit/documented statuses above were extracted from the cited current source. Service/advice-only application error codes are not claimed where the controller does not declare them.

### 9.1 Handler Contract — `GET /api/v1/exercises/sessions/history`

| Item | Exact current contract |
| --- | --- |
| Handler | `getSessionHistory` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/ExerciseSessionController.java` |
| Authorization annotation / boundary | hasAnyRole('MOTHER', 'SYSTEM_ADMIN') |
| Parameters | query `trimesterScope`: `TrimesterScope`; query `from`: `OffsetDateTime`; query `to`: `OffsetDateTime`; query `page`: `int`; query `size`: `int`; principal `principal`: `Principal` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<PaginatedResponse<ExerciseSessionHistorySummary>>` |
| Response payload fields | Not applicable or unresolved from the handler import |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-001` / `UC-MH-19-TC-API-001` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.2 Handler Contract — `PATCH /api/v1/exercises/sessions/{sessionId}/complete`

| Item | Exact current contract |
| --- | --- |
| Handler | `completeSession` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/ExerciseSessionController.java` |
| Authorization annotation / boundary | hasAnyRole('MOTHER', 'SYSTEM_ADMIN') |
| Parameters | path `sessionId`: `UUID`; principal `principal`: `Principal` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<SessionResultResponse>>` |
| Response payload fields | `exerciseSessionId`: `UUID` (no field annotation in current DTO); `exerciseId`: `UUID` (no field annotation in current DTO); `exerciseTitle`: `String` (no field annotation in current DTO); `sessionStatus`: `String` (no field annotation in current DTO); `startedAt`: `OffsetDateTime` (no field annotation in current DTO); `endedAt`: `OffsetDateTime` (no field annotation in current DTO); `actualDurationSeconds`: `Long` (no field annotation in current DTO); `completionPercent`: `BigDecimal` (no field annotation in current DTO); `postureScore`: `BigDecimal` (no field annotation in current DTO); `warningCount`: `Integer` (no field annotation in current DTO); `summaryJson`: `String` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-002` / `UC-MH-19-TC-API-002` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.3 Handler Contract — `PATCH /api/v1/exercises/sessions/{sessionId}/pause`

| Item | Exact current contract |
| --- | --- |
| Handler | `pauseSession` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/ExerciseSessionController.java` |
| Authorization annotation / boundary | hasAnyRole('MOTHER', 'SYSTEM_ADMIN') |
| Parameters | path `sessionId`: `UUID`; principal `principal`: `Principal` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<SessionStateResponse>>` |
| Response payload fields | `exerciseSessionId`: `UUID` (no field annotation in current DTO); `sessionStatus`: `String` (no field annotation in current DTO); `pausedSeconds`: `Integer` (no field annotation in current DTO); `warningCount`: `Integer` (no field annotation in current DTO); `updatedAt`: `OffsetDateTime` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-003` / `UC-MH-19-TC-API-003` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.4 Handler Contract — `POST /api/v1/exercises/sessions/{sessionId}/posture-events`

| Item | Exact current contract |
| --- | --- |
| Handler | `analyzePosture` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/ExerciseSessionController.java` |
| Authorization annotation / boundary | hasAnyRole('MOTHER', 'SYSTEM_ADMIN') |
| Parameters | path `sessionId`: `UUID`; body `request`: `PostureEventRequest`; principal `principal`: `Principal` |
| Request body type | `PostureEventRequest` |
| Request fields and validators | `eventTimeMs`: `Long` (@NotNull, @PositiveOrZero); `keypointSummaryJson`: `Map<String, Object>` (@NotNull) |
| Response type | `ResponseEntity<ApiResponse<PostureFeedbackResponse>>` |
| Response payload fields | `postureCode`: `String` (no field annotation in current DTO); `confidenceScore`: `BigDecimal` (no field annotation in current DTO); `severity`: `String` (no field annotation in current DTO); `feedbackText`: `String` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-004` / `UC-MH-19-TC-API-004` |
| Negative test mapping | `COND-API-004-VAL` / `UC-MH-19-TC-API-004-VAL`; plus `COND-AUTH` for protected access |

### 9.5 Handler Contract — `GET /api/v1/exercises/sessions/{sessionId}/result`

| Item | Exact current contract |
| --- | --- |
| Handler | `getSessionResult` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/ExerciseSessionController.java` |
| Authorization annotation / boundary | hasAnyRole('MOTHER', 'SYSTEM_ADMIN') |
| Parameters | path `sessionId`: `UUID`; principal `principal`: `Principal` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<SessionResultResponse>>` |
| Response payload fields | `exerciseSessionId`: `UUID` (no field annotation in current DTO); `exerciseId`: `UUID` (no field annotation in current DTO); `exerciseTitle`: `String` (no field annotation in current DTO); `sessionStatus`: `String` (no field annotation in current DTO); `startedAt`: `OffsetDateTime` (no field annotation in current DTO); `endedAt`: `OffsetDateTime` (no field annotation in current DTO); `actualDurationSeconds`: `Long` (no field annotation in current DTO); `completionPercent`: `BigDecimal` (no field annotation in current DTO); `postureScore`: `BigDecimal` (no field annotation in current DTO); `warningCount`: `Integer` (no field annotation in current DTO); `summaryJson`: `String` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-005` / `UC-MH-19-TC-API-005` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.6 Handler Contract — `PATCH /api/v1/exercises/sessions/{sessionId}/resume`

| Item | Exact current contract |
| --- | --- |
| Handler | `resumeSession` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/ExerciseSessionController.java` |
| Authorization annotation / boundary | hasAnyRole('MOTHER', 'SYSTEM_ADMIN') |
| Parameters | path `sessionId`: `UUID`; principal `principal`: `Principal` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<SessionStateResponse>>` |
| Response payload fields | `exerciseSessionId`: `UUID` (no field annotation in current DTO); `sessionStatus`: `String` (no field annotation in current DTO); `pausedSeconds`: `Integer` (no field annotation in current DTO); `warningCount`: `Integer` (no field annotation in current DTO); `updatedAt`: `OffsetDateTime` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-006` / `UC-MH-19-TC-API-006` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.7 Handler Contract — `GET /api/v1/exercises/{exerciseId}/posture-config`

| Item | Exact current contract |
| --- | --- |
| Handler | `getPostureConfig` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/ExerciseController.java` |
| Authorization annotation / boundary | hasAnyRole('MOTHER', 'SYSTEM_ADMIN') |
| Parameters | path `exerciseId`: `UUID` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<PostureConfigResponse>>` |
| Response payload fields | `postureConfigId`: `UUID` (no field annotation in current DTO); `exerciseId`: `UUID` (no field annotation in current DTO); `analysisMode`: `String` (no field annotation in current DTO); `ruleOrModelVersion`: `String` (no field annotation in current DTO); `confidenceThreshold`: `BigDecimal` (no field annotation in current DTO); `feedbackLevel`: `String` (no field annotation in current DTO); `effectiveFrom`: `OffsetDateTime` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-007` / `UC-MH-19-TC-API-007` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.8 Handler Contract — `POST /api/v1/exercises/{exerciseId}/sessions`

| Item | Exact current contract |
| --- | --- |
| Handler | `startSession` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/ExerciseController.java` |
| Authorization annotation / boundary | hasAnyRole('MOTHER', 'SYSTEM_ADMIN') |
| Parameters | path `exerciseId`: `UUID`; body `request`: `StartSessionRequest`; principal `principal`: `Principal` |
| Request body type | `StartSessionRequest` |
| Request fields and validators | `safetyCheckId`: `UUID` (@NotNull); `journeyId`: `UUID` (no field annotation in current DTO) |
| Response type | `ResponseEntity<ApiResponse<StartSessionResponse>>` |
| Response payload fields | `exerciseSessionId`: `UUID` (no field annotation in current DTO); `exerciseId`: `UUID` (no field annotation in current DTO); `userId`: `UUID` (no field annotation in current DTO); `safetyCheckId`: `UUID` (no field annotation in current DTO); `journeyId`: `UUID` (no field annotation in current DTO); `sessionStatus`: `String` (no field annotation in current DTO); `startedAt`: `OffsetDateTime` (no field annotation in current DTO); `supportsPostureAnalysis`: `Boolean` (no field annotation in current DTO) |
| Explicit/documented statuses | `201` |
| Positive test mapping | `COND-API-008` / `UC-MH-19-TC-API-008` |
| Negative test mapping | `COND-API-008-VAL` / `UC-MH-19-TC-API-008-VAL`; plus `COND-AUTH` for protected access |

### 9.9 Handler Contract — `POST /v1/inference/landmarks`

| Item | Exact current contract |
| --- | --- |
| Handler | `infer` |
| Source | `05_Development/MachineLearning/MediaPipe_Posture/exercise_correction_sidecar/app/main.py` |
| Authorization annotation / boundary | No explicit internal-key dependency on this handler; router/application policy must be checked |
| Parameters | body `payload`: `InferenceRequest` |
| Request body type | `InferenceRequest` |
| Request fields and validators | `schemaVersion`: `Literal[LANDMARK_SCHEMA_VERSION]` (`required`); `modelVersion`: `Literal[MODEL_VERSION]` (`required`); `exerciseKey`: `str` (`Field(min_length=1, max_length=64, pattern=r"^[a-z0-9_]+$")`); `sequenceNumber`: `int` (`Field(ge=0, le=MAX_SEQUENCE_NUMBER)`); `inferenceStreamId`: `str \| None` (`Field(default=None, min_length=1, max_length=128, pattern=r"^[A-Za-z0-9._:-]+$")`); `landmarks`: `dict[str, Landmark]` (`Field(min_length=1, max_length=33)`) |
| Response type | `InferenceResponse` |
| Response payload fields | `schemaVersion`: `Literal[RESPONSE_SCHEMA_VERSION]` (`RESPONSE_SCHEMA_VERSION`); `modelVersion`: `Literal[MODEL_VERSION]` (`MODEL_VERSION`); `exerciseKey`: `str` (`required`); `sequenceNumber`: `int` (`required`); `inferenceStreamId`: `str \| None` (`None`); `predictedClass`: `str` (`required`); `stage`: `str \| None` (`None`); `confidence`: `float` (`Field(ge=0.0, le=1.0)`); `correct`: `bool` (`required`); `score`: `float` (`Field(ge=0.0, le=100.0)`); `feedback`: `list[Feedback]` (`required`) |
| Explicit/documented statuses | `200, 400, 422, 503` |
| Positive test mapping | `COND-API-009` / `UC-MH-19-TC-API-009` |
| Negative test mapping | `COND-API-009-VAL` / `UC-MH-19-TC-API-009-VAL`; plus `COND-AUTH` for protected access |

## 10. Error Codes

| Error class | HTTP / code | Trigger | Client behavior | Oracle |
| --- | --- | --- | --- | --- |
| Validation/business rule | `400` | Malformed field, range, enum, or rejected transition | Return the current stable error envelope without protected data or unintended mutation | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/common/exception/GlobalExceptionHandler.java`; `05_Development/MachineLearning/MediaPipe_Posture/exercise_correction_sidecar/app/main.py`; application error code is limited to what those sources/advice explicitly declare |
| Semantic validation | `422` | Well-formed request violates a semantic constraint | Return the current stable error envelope without protected data or unintended mutation | `05_Development/MachineLearning/MediaPipe_Posture/exercise_correction_sidecar/app/main.py`; application error code is limited to what those sources/advice explicitly declare |
| Dependency unavailable | `503` | Required provider or downstream service is unavailable | Return the current stable error envelope without protected data or unintended mutation | `05_Development/MachineLearning/MediaPipe_Posture/exercise_correction_sidecar/app/main.py`; application error code is limited to what those sources/advice explicitly declare |

## 11. Implementation and Deployment Plan

1. Preserve this current code-backed boundary and resolve every `Open` item that changes tests, schema, auth, API, or state.
2. Map exact DTO fields, service/repository symbols, migrations, events, and error codes from the listed evidence.
3. Write paired Test-Spec Red cases before production changes.
4. Implement only the approved gap; reuse current components listed in Section 5.
5. Run targeted and affected suites; record commands/counts only after execution.
6. Deploy compatible server/schema changes before clients that depend on them; preserve old-client compatibility where required.

## 12. Rollback and Incident Runbook

| Trigger | Safe rollback / containment | Verification |
| --- | --- | --- |
| Documentation error | Revert only this generated pair/manifest change to the last reviewed version. | Regenerate and run document validators. |
| Client regression | Disable/revert the feature-owned client change while keeping compatible server contracts. | Targeted route/widget/component tests. |
| Server regression | Revert the feature-owned change or deploy a forward corrective fix. | Targeted backend/AI suite and smoke contract. |
| Schema issue | Stop rollout; restore through additive corrective migration or isolated backup procedure. Never edit applied Flyway history. | Migration validation and data-integrity checks. |
| Provider incident | Disable optional integration or use only the approved degraded mode. | Provider-fake/sandbox contract tests. |

## 13. Verification Scenario Groups

| Group | Behavior | Condition | Test case |
| --- | --- | --- | --- |
| `VG-01` | Load the active posture configuration and start a session after a passed safety check/camera permission. | `COND-01` | `UC-MH-19-TC-001` |
| `VG-02` | Process supported posture observations through the configured posture-correction sidecar or implemented fallback. | `COND-02` | `UC-MH-19-TC-002` |
| `VG-03` | Complete/abort and review the stored result/history. | `COND-03` | `UC-MH-19-TC-003` |
| `VG-AUTH` | Reject wrong authentication/role/ownership/membership/consent scope | `COND-AUTH` | `UC-MH-19-TC-SEC-001` |
| `VG-GAP` | Characterize each known gap without claiming a false completed path | `COND-GAP` | `UC-MH-19-TC-GAP-001` |

## 14. Verification Methods

Existing focused evidence:

- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/exercise/ExerciseSessionServiceTest.java`
- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/exercise/ExerciseSessionHistoryEmbeddedPostgresTest.java`
- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/exercise/PostureAnalysisServiceTest.java`
- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/exercise/inference/ExerciseCorrectionHttpAdapterTest.java`
- `05_Development/MachineLearning/MediaPipe_Posture/exercise_correction_sidecar/tests/test_http_contract.py`
- `05_Development/CareBridgeMobileApp/test/features/exercise/exercise_session_screen_test.dart`

Exact supported commands derived from the audited test paths:

- `cd 05_Development/CareBridgeAPI && ./mvnw -Dtest=ExerciseSessionServiceTest test`
- `cd 05_Development/CareBridgeAPI && ./mvnw -Dtest=ExerciseSessionHistoryEmbeddedPostgresTest test`
- `cd 05_Development/CareBridgeAPI && ./mvnw -Dtest=PostureAnalysisServiceTest test`
- `cd 05_Development/CareBridgeAPI && ./mvnw -Dtest=ExerciseCorrectionHttpAdapterTest test`
- `cd 05_Development/CareBridgeMobileApp && flutter test test/features/exercise/exercise_session_screen_test.dart`

Record pass/fail/skip counts only after executing these commands on the exact revision.

## 15. Verification Samples

| Sample | Value |
| --- | --- |
| Primary contract | GET `/api/v1/exercises/{id}/posture-config` |
| Request | `GET /api/v1/exercises/sessions/history` → `getSessionHistory`; `None` with Not applicable — no request body; authorization: hasAnyRole('MOTHER', 'SYSTEM_ADMIN'). |
| Success response | `ResponseEntity<PaginatedResponse<ExerciseSessionHistorySummary>>` with Not applicable or unresolved from the handler import; explicit/documented statuses `200`. |
| Negative sample | Use wrong role/owner/state and assert the exact mapped error without protected payload. |

## 16. Authorization Matrix

| Actor / role | Operation | Decision |
| --- | --- | --- |
| Mother | `GET /api/v1/exercises/sessions/history` | hasAnyRole('MOTHER', 'SYSTEM_ADMIN'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/ExerciseSessionController.java` |
| Mother | `PATCH /api/v1/exercises/sessions/{sessionId}/complete` | hasAnyRole('MOTHER', 'SYSTEM_ADMIN'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/ExerciseSessionController.java` |
| Mother | `PATCH /api/v1/exercises/sessions/{sessionId}/pause` | hasAnyRole('MOTHER', 'SYSTEM_ADMIN'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/ExerciseSessionController.java` |
| Mother | `POST /api/v1/exercises/sessions/{sessionId}/posture-events` | hasAnyRole('MOTHER', 'SYSTEM_ADMIN'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/ExerciseSessionController.java` |
| Mother | `GET /api/v1/exercises/sessions/{sessionId}/result` | hasAnyRole('MOTHER', 'SYSTEM_ADMIN'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/ExerciseSessionController.java` |
| Mother | `PATCH /api/v1/exercises/sessions/{sessionId}/resume` | hasAnyRole('MOTHER', 'SYSTEM_ADMIN'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/ExerciseSessionController.java` |
| Mother | `GET /api/v1/exercises/{exerciseId}/posture-config` | hasAnyRole('MOTHER', 'SYSTEM_ADMIN'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/ExerciseController.java` |
| Mother | `POST /api/v1/exercises/{exerciseId}/sessions` | hasAnyRole('MOTHER', 'SYSTEM_ADMIN'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/ExerciseController.java` |
| CareBridge Backend posture-inference adapter | `POST /v1/inference/landmarks` | No explicit internal-key dependency on this handler; router/application policy must be checked; handler oracle `05_Development/MachineLearning/MediaPipe_Posture/exercise_correction_sidecar/app/main.py` |
| Unauthenticated / wrong role / wrong owner-member | All protected operations | Deny without resource disclosure |

## 17. AI Prompt Constraints — CASE 2.0

- Not applicable — this UC does not generate clinical AI output. Generic documentation assistance remains evidence-first.

### Quality and Anti-Pattern Checklist

- [ ] All 17 sections remain present.
- [ ] Every known semantic value has an exact source; unresolved values are `Open` with evidence needed.
- [ ] Requirement → component → condition → test traceability is preserved.
- [ ] No historical pass count, SLA, accuracy, or provider claim is copied without current evidence.
- [ ] No generic endpoint group is treated as an implementation-ready field/error contract.
- [ ] No production code, schema, or immutable AI architecture source is modified by spec generation.
