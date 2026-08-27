# ENGINEERING DOCUMENTATION STANDARD (EDS) v2.0

# TECHNICAL DESIGN SPECIFICATION — Configure and Test Fall Detection

| Field | Value |
| --- | --- |
| Document ID | `UC-ES-04-TDS` |
| Version | `0.1` |
| Date | `2026-08-23` |
| Status | `Draft` |
| Function ID | `UC-ES-04` |
| Canonical Use Case | `UC-ES-04 — Configure and Test Fall Detection` |
| Module / Bounded Context | `Emergency and Safety` |
| Primary Actor | `Mother` |
| Platforms | `Mobile / Backend / Device Sensors` |
| Priority | `High` |
| Data Classification | `Restricted emergency, live-location, consent, sensor, and family-alert data` |
| Compliance Scope | `PDPA explicit-purpose location/sensor processing, consent, least disclosure, safety-event integrity, and bounded retention` |
| Owner | `CareBridge Team` |
| Reviewer / Approver |  |
| Source Baseline | Current worktree on `2026-08-23`; SRS `UC-ES-04`; exact evidence in Section 1.4 |

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

- **Goal:** Grant required consent/permissions, enable or disable fall detection, configure current safety settings, and complete sensor self-test.
- **Trigger:** The actor enters Mobile `/safety/fall-detection/enable`.
- **Outcome:** Run and complete the supported sensor self-test or disable monitoring.
- **Current state:** `High` confidence from reachable code/test audit; no manifest-level exclusion is recorded.
- **Target state:** Preserve current code-backed behavior and resolve only explicitly evidenced limitations through approved implementation work.

### 1.2 Scope

**In scope**

- Mobile `/safety/fall-detection/enable`
- Mobile `/safety`

- GET/PUT `/api/v1/safety/config`
- POST `/api/v1/safety/fall-detection/enable`
- POST `/api/v1/safety/fall-detection/disable`
- POST `/api/v1/safety/events/sensor-self-test`
- POST `/api/v1/safety/events/{eventId}/sensor-self-test/complete`

**Out of scope / limitations**

- Not applicable — no manifest-level exclusion is recorded. Scope is limited to the entry points and exact contracts listed above; unrelated handlers in the same module are excluded.

### 1.3 Preconditions and Postconditions

| Type | Condition |
| --- | --- |
| Precondition | Mother is authenticated/authorized where the current contract requires it. |
| Precondition | Required ownership, membership, consent, resource state, and device/provider prerequisites pass current policies. |
| Postcondition | Run and complete the supported sensor self-test or disable monitoring. |
| Postcondition | No side effect outside the feature-owned persistence/event/provider boundary occurs. |

### 1.4 Evidence Baseline

| Source ID | Type | Exact path | Revision | Authority |
| --- | --- | --- | --- | --- |
| `SRC-SRS-01` | Requirement | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-ES-04 | 2026-08-23 | Draft code-first requirement |
| `SRC-CODE-01` | Current code | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/safety/controller/SafetyConfigController.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-02` | Current code | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/safety/controller/SensorSelfTestController.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-03` | Current code | `05_Development/CareBridgeMobileApp/lib/features/safety/screens/enable_fall_detection_screen.dart` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-04` | Current code | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/safety/controller/FallDetectionController.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-01` | Existing test | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/safety/SafetyConfigControllerTest.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-02` | Existing test | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/safety/SafetyConfigServiceTest.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-03` | Existing test | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/safety/SensorSelfTestServiceTest.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-04` | Existing test | `05_Development/CareBridgeMobileApp/test/features/safety/safety_contract_test.dart` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-ERROR-01` | Current exception advice | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/common/exception/GlobalExceptionHandler.java` | Worktree `2026-08-23` | Current-state evidence |

### 1.5 Open Contradictions / Questions

- No material contradiction was found among the exact sources cited in Section 1.4. Behavior not evidenced by those sources is not approved or implied by this Draft.

## 2. Traceability Matrix

| Requirement | Behavior | Exact oracle source | Component | Test condition / case |
| --- | --- | --- | --- | --- |
| `UC-ES-04-FR-01` | Review consent/device permission prerequisites. | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-ES-04 Normal Flow 1 | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/safety/controller/SafetyConfigController.java` | `COND-01` / `UC-ES-04-TC-001` |
| `UC-ES-04-FR-02` | Enable/configure fall detection. | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-ES-04 Normal Flow 2 | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/safety/controller/SafetyConfigController.java` | `COND-02` / `UC-ES-04-TC-002` |
| `UC-ES-04-FR-03` | Run and complete the supported sensor self-test or disable monitoring. | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-ES-04 Normal Flow 3 | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/safety/controller/SensorSelfTestController.java` | `COND-03` / `UC-ES-04-TC-003` |
| `BR-01` | Consent, device permission, and latest server configuration are required before monitoring. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/safety/controller/FallDetectionController.java` | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/safety/controller/FallDetectionController.java` | `COND-BR-01` / `UC-ES-04-TC-BR-001` |
| `BR-02` | A failed self-test must not be presented as an enabled healthy detector. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/safety/controller/SafetyConfigController.java` | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/safety/controller/SafetyConfigController.java` | `COND-BR-02` / `UC-ES-04-TC-BR-002` |

## 3. Architecture Decision Records (ADR)

### ADR-UC-ES-04-01 — Use a distinct actor-goal boundary

| Item | Decision |
| --- | --- |
| Context | The retired 43-UC catalogue grouped multiple triggers, lifecycles, and permission boundaries, making implementation/test traceability generic. |
| Options | Keep the broad catalogue; split by screen; split by actor goal plus lifecycle/security boundary. |
| Decision | Use `UC-ES-04 — Configure and Test Fall Detection` as the canonical boundary because its operations share the stated actor outcome and current implementation evidence. |
| Consequences | Related supporting screens/endpoints stay in one TDS; different lifecycle/actor outcomes have separate UCs. |
| Source / Status | SRS Section 3.1 and current code audit / Draft |

### ADR-UC-ES-04-02 — Preserve unknowns as Open

| Item | Decision |
| --- | --- |
| Context | Exact schema fields, SLA values, and some controller error codes are not fully evidenced by this manifest alone. |
| Decision | Do not invent them. Mark them `Open` and require the exact DTO/entity/migration/policy oracle before production-code changes. |
| Consequences | This document accurately characterizes current scope; unresolved design-changing items block implementation approval. |
| Source / Status | `create-specs` evidence discipline / Draft |

## 4. Non-Functional Requirements and SLA

| NFR | Target | Oracle Source | Verification |
| --- | --- | --- | --- |
| Authorization / isolation | All requests follow exact role/ownership/membership/consent policy in current code. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/safety/controller/SafetyConfigController.java` | Negative security cases in paired Test-Spec |
| Protected-data handling | No secrets or unnecessary health/location/identity/conversation/file payloads in logs, fixtures, screenshots, or audit detail. | Data classification header plus exact Section 9 request/response field inventories | Log/fixture review plus security tests |
| Availability / latency | Open — no approved feature-specific numeric SLA found. | Evidence needed: approved NFR/SLA source | Measure only; do not assert a fixed threshold |
| Accessibility | Open — confirm project-standard criteria for reachable UI surfaces. | Evidence needed: approved UX/accessibility standard | Applicable Web/Mobile UI checks |
| Retry / idempotency | Apply only semantics explicitly implemented by the owning service. | No explicit lock/version/idempotency marker is evidenced in the cited implementation sources; preserve observed behavior and add characterization before changing concurrency semantics | State/duplicate/concurrency cases where applicable |

## 5. Static Modeling

### 5.1 Component Responsibilities and Change Disposition

| Exact path | Disposition | Responsibility |
| --- | --- | --- |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/safety/controller/SafetyConfigController.java` | Reuse | Current implementation evidence for Configure and Test Fall Detection; inspect the exact symbol before implementation changes. |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/safety/controller/SensorSelfTestController.java` | Reuse | Current implementation evidence for Configure and Test Fall Detection; inspect the exact symbol before implementation changes. |
| `05_Development/CareBridgeMobileApp/lib/features/safety/screens/enable_fall_detection_screen.dart` | Reuse | Current implementation evidence for Configure and Test Fall Detection; inspect the exact symbol before implementation changes. |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/safety/controller/FallDetectionController.java` | Reuse | Current implementation evidence for Configure and Test Fall Detection; inspect the exact symbol before implementation changes. |

### 5.2 Current Component Diagram

```plantuml
@startuml
skinparam classAttributeIconSize 0
class SafetyConfigController as "SafetyConfigController.java"
class SensorSelfTestController as "SensorSelfTestController.java"
SafetyConfigController --> SensorSelfTestController
class enable_fall_detection_screen as "enable_fall_detection_screen.dart"
SensorSelfTestController --> enable_fall_detection_screen
class FallDetectionController as "FallDetectionController.java"
enable_fall_detection_screen --> FallDetectionController
@enduml
```

### 5.3 Data / Schema / Migration Assessment

| Item | Assessment |
| --- | --- |
| Current stores/entities | No entity/repository import is present in the cited entry/source set; follow the exact handler-to-service call path before any schema change |
| Sensitive fields | Restricted emergency, live-location, consent, sensor, and family-alert data. Exact transport fields and validators are enumerated per handler in Section 9; entity-only fields require the cited service/entity source before a schema change. |
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
Actor -> Client: Enter Configure and Test Fall Detection
Client -> Domain: Review consent/device permission prerequisites.
Domain --> Client: Result for step 1
Client -> Domain: Enable/configure fall detection.
Domain --> Client: Result for step 2
Client -> Domain: Run and complete the supported sensor self-test or disable monitoring.
Domain --> Client: Result for step 3
Client --> Actor: Render canonical outcome
@enduml
```

### 6.2 Alternative, Error, Retry, and Concurrency Flows

| Flow | Expected design behavior | Oracle |
| --- | --- | --- |
| Cancel before mutation | No unintended write or provider side effect. | SRS UC-ES-04 Alternative Flow |
| Invalid input/state | Reject with the current contract; keep canonical state unchanged. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/safety/controller/SafetyConfigController.java` |
| Wrong actor/scope | Fail closed without protected resource disclosure. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/safety/controller/SafetyConfigController.java` |
| Dependency failure | Use only the implemented bounded fallback/retry; never report false success. | Not applicable at this cited baseline — no external adapter/provider import is evidenced in the listed implementation sources |
| Duplicate/concurrent mutation | Apply only current lock/version/idempotency semantics. | No explicit lock/version/idempotency marker is evidenced in the cited implementation sources; preserve observed behavior and add characterization before changing concurrency semantics |

### 6.3 State Model and Invariants

```plantuml
@startuml
[*] --> Eligible
Eligible --> InProgress : Review consent/device permission prerequisites.
InProgress --> Outcome : Run and complete the supported sensor self-test or disable monitoring.
InProgress --> Rejected : validation / authorization / state failure
Outcome --> [*]
Rejected --> Eligible : actor corrects eligible input
@enduml
```

- Consent, device permission, and latest server configuration are required before monitoring.
- A failed self-test must not be presented as an enabled healthy detector.

## 7. Domain Event Catalog

| Direction | Event | Producer / Consumer | Payload / delivery / idempotency |
| --- | --- | --- | --- |
| Publish / consume | Current event evidence | Not applicable at this cited baseline — no event type, publisher, or listener is evidenced in the listed implementation sources | Preserve only events evidenced by the cited source set; if later call-path inspection finds none, the flow remains synchronous. |

## 8. Interface Specification

### 8.1 User / Operator Interfaces

| # | Entry point | Actor | Contract |
| ---: | --- | --- | --- |
| 1 | Mobile `/safety/fall-detection/enable` | Mother | Reachable current entry point |
| 2 | Mobile `/safety` | Mother | Reachable current entry point |

### 8.2 Service / Repository Interfaces

| Interface evidence | Required responsibility |
| --- | --- |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/safety/controller/SafetyConfigController.java` | Support the mapped operations without broadening authorization or lifecycle semantics. |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/safety/controller/SensorSelfTestController.java` | Support the mapped operations without broadening authorization or lifecycle semantics. |
| `05_Development/CareBridgeMobileApp/lib/features/safety/screens/enable_fall_detection_screen.dart` | Support the mapped operations without broadening authorization or lifecycle semantics. |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/safety/controller/FallDetectionController.java` | Support the mapped operations without broadening authorization or lifecycle semantics. |

## 9. API Specification

| ID | Method / path or grouped controller surface | Auth / role | Request / response / errors |
| --- | --- | --- | --- |
| `API-01` | `GET /api/v1/safety/config` | hasRole('MOTHER') | Handler `getConfig`; parameters: principal `principal`: `Principal`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<SafetyConfigResponse>>`; response payload fields: `id`: `UUID` (no field annotation in current DTO); `userId`: `UUID` (no field annotation in current DTO); `fallDetectionEnabled`: `boolean` (no field annotation in current DTO); `sensitivityLevel`: `String` (no field annotation in current DTO); `emergencyAutoAlert`: `boolean` (no field annotation in current DTO); `locationSharingEnabled`: `boolean` (no field annotation in current DTO); `countdownSeconds`: `int` (no field annotation in current DTO); `sensorPermissionGranted`: `boolean` (no field annotation in current DTO); `sensorPermissionRecordedAt`: `Instant` (no field annotation in current DTO); `updatedAt`: `Instant` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/safety/controller/SafetyConfigController.java` |
| `API-02` | `PUT /api/v1/safety/config` | hasRole('MOTHER') | Handler `configure`; parameters: body `request`: `SafetyConfigRequest`; principal `principal`: `Principal`; request body: `SafetyConfigRequest`; request fields/validation: `fallDetectionEnabled`: `Boolean` (@NotNull); `sensitivityLevel`: `String` (@NotNull, @Pattern(regexp = "LOW\|MEDIUM\|HIGH", message = "sensitivityLevel must be LOW, MEDIUM, or HIGH")); `emergencyAutoAlert`: `Boolean` (@NotNull); `locationSharingEnabled`: `Boolean` (no field annotation in current DTO); `countdownSeconds`: `Integer` (no field annotation in current DTO); `sensorPermissionGranted`: `Boolean` (no field annotation in current DTO); response: `ResponseEntity<ApiResponse<SafetyConfigResponse>>`; response payload fields: `id`: `UUID` (no field annotation in current DTO); `userId`: `UUID` (no field annotation in current DTO); `fallDetectionEnabled`: `boolean` (no field annotation in current DTO); `sensitivityLevel`: `String` (no field annotation in current DTO); `emergencyAutoAlert`: `boolean` (no field annotation in current DTO); `locationSharingEnabled`: `boolean` (no field annotation in current DTO); `countdownSeconds`: `int` (no field annotation in current DTO); `sensorPermissionGranted`: `boolean` (no field annotation in current DTO); `sensorPermissionRecordedAt`: `Instant` (no field annotation in current DTO); `updatedAt`: `Instant` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/safety/controller/SafetyConfigController.java` |
| `API-03` | `POST /api/v1/safety/events/sensor-self-test` | hasRole('MOTHER') | Handler `create`; parameters: body `request`: `SensorSelfTestEventRequest`; principal `principal`: `Principal`; request body: `SensorSelfTestEventRequest`; request fields/validation: `testId`: `String` (@NotBlank, @Size(max = 140)); `detectedAt`: `Instant` (@NotNull); `accelerationMagnitude`: `Double` (@NotNull, @PositiveOrZero); `gyroscopeMagnitude`: `Double` (@NotNull, @PositiveOrZero); response: `ResponseEntity<ApiResponse<SafetyEventResponse>>`; response payload fields: `id`: `UUID` (no field annotation in current DTO); `userId`: `UUID` (no field annotation in current DTO); `eventType`: `String` (no field annotation in current DTO); `magnitude`: `double` (no field annotation in current DTO); `userLatitude`: `BigDecimal` (no field annotation in current DTO); `userLongitude`: `BigDecimal` (no field annotation in current DTO); `detectedAt`: `Instant` (no field annotation in current DTO); `clientDetectedAt`: `Instant` (no field annotation in current DTO); `status`: `String` (no field annotation in current DTO); `resolvedAt`: `Instant` (no field annotation in current DTO); `notes`: `String` (no field annotation in current DTO); `countdownDeadlineAt`: `Instant` (no field annotation in current DTO); `responseType`: `String` (no field annotation in current DTO); `responseReason`: `String` (no field annotation in current DTO); `respondedAt`: `Instant` (no field annotation in current DTO); `emergencySessionId`: `UUID` (no field annotation in current DTO); explicit/documented statuses: `201`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/safety/controller/SensorSelfTestController.java` |
| `API-04` | `POST /api/v1/safety/events/{eventId}/sensor-self-test/complete` | hasRole('MOTHER') | Handler `complete`; parameters: path `eventId`: `UUID`; body `request`: `SensorSelfTestCompletionRequest`; principal `principal`: `Principal`; request body: `SensorSelfTestCompletionRequest`; request fields/validation: `outcome`: `String` (@NotBlank, @Pattern(regexp = "TIMEOUT\|NEED_HELP")); response: `ResponseEntity<ApiResponse<SafetyEventResponse>>`; response payload fields: `id`: `UUID` (no field annotation in current DTO); `userId`: `UUID` (no field annotation in current DTO); `eventType`: `String` (no field annotation in current DTO); `magnitude`: `double` (no field annotation in current DTO); `userLatitude`: `BigDecimal` (no field annotation in current DTO); `userLongitude`: `BigDecimal` (no field annotation in current DTO); `detectedAt`: `Instant` (no field annotation in current DTO); `clientDetectedAt`: `Instant` (no field annotation in current DTO); `status`: `String` (no field annotation in current DTO); `resolvedAt`: `Instant` (no field annotation in current DTO); `notes`: `String` (no field annotation in current DTO); `countdownDeadlineAt`: `Instant` (no field annotation in current DTO); `responseType`: `String` (no field annotation in current DTO); `responseReason`: `String` (no field annotation in current DTO); `respondedAt`: `Instant` (no field annotation in current DTO); `emergencySessionId`: `UUID` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/safety/controller/SensorSelfTestController.java` |
| `API-05` | `POST /api/v1/safety/fall-detection/disable` | hasRole('MOTHER') | Handler `disable`; parameters: principal `principal`: `Principal`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<Void>>`; response payload fields: Not applicable or unresolved from the handler import; explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/safety/controller/FallDetectionController.java` |
| `API-06` | `POST /api/v1/safety/fall-detection/enable` | hasRole('MOTHER') | Handler `enable`; parameters: principal `principal`: `Principal`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<ImuMonitoringSessionResponse>>`; response payload fields: `sessionId`: `UUID` (no field annotation in current DTO); `userId`: `UUID` (no field annotation in current DTO); `status`: `String` (no field annotation in current DTO); `sensitivityLevel`: `String` (no field annotation in current DTO); `startedAt`: `Instant` (no field annotation in current DTO); `endedAt`: `Instant` (no field annotation in current DTO); explicit/documented statuses: `201`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/safety/controller/FallDetectionController.java` |

Method-level Spring handlers, authorization annotations, request DTO fields/validators, response payload fields, and explicit/documented statuses above were extracted from the cited current source. Service/advice-only application error codes are not claimed where the controller does not declare them.

### 9.1 Handler Contract — `GET /api/v1/safety/config`

| Item | Exact current contract |
| --- | --- |
| Handler | `getConfig` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/safety/controller/SafetyConfigController.java` |
| Authorization annotation / boundary | hasRole('MOTHER') |
| Parameters | principal `principal`: `Principal` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<SafetyConfigResponse>>` |
| Response payload fields | `id`: `UUID` (no field annotation in current DTO); `userId`: `UUID` (no field annotation in current DTO); `fallDetectionEnabled`: `boolean` (no field annotation in current DTO); `sensitivityLevel`: `String` (no field annotation in current DTO); `emergencyAutoAlert`: `boolean` (no field annotation in current DTO); `locationSharingEnabled`: `boolean` (no field annotation in current DTO); `countdownSeconds`: `int` (no field annotation in current DTO); `sensorPermissionGranted`: `boolean` (no field annotation in current DTO); `sensorPermissionRecordedAt`: `Instant` (no field annotation in current DTO); `updatedAt`: `Instant` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-001` / `UC-ES-04-TC-API-001` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.2 Handler Contract — `PUT /api/v1/safety/config`

| Item | Exact current contract |
| --- | --- |
| Handler | `configure` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/safety/controller/SafetyConfigController.java` |
| Authorization annotation / boundary | hasRole('MOTHER') |
| Parameters | body `request`: `SafetyConfigRequest`; principal `principal`: `Principal` |
| Request body type | `SafetyConfigRequest` |
| Request fields and validators | `fallDetectionEnabled`: `Boolean` (@NotNull); `sensitivityLevel`: `String` (@NotNull, @Pattern(regexp = "LOW\|MEDIUM\|HIGH", message = "sensitivityLevel must be LOW, MEDIUM, or HIGH")); `emergencyAutoAlert`: `Boolean` (@NotNull); `locationSharingEnabled`: `Boolean` (no field annotation in current DTO); `countdownSeconds`: `Integer` (no field annotation in current DTO); `sensorPermissionGranted`: `Boolean` (no field annotation in current DTO) |
| Response type | `ResponseEntity<ApiResponse<SafetyConfigResponse>>` |
| Response payload fields | `id`: `UUID` (no field annotation in current DTO); `userId`: `UUID` (no field annotation in current DTO); `fallDetectionEnabled`: `boolean` (no field annotation in current DTO); `sensitivityLevel`: `String` (no field annotation in current DTO); `emergencyAutoAlert`: `boolean` (no field annotation in current DTO); `locationSharingEnabled`: `boolean` (no field annotation in current DTO); `countdownSeconds`: `int` (no field annotation in current DTO); `sensorPermissionGranted`: `boolean` (no field annotation in current DTO); `sensorPermissionRecordedAt`: `Instant` (no field annotation in current DTO); `updatedAt`: `Instant` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-002` / `UC-ES-04-TC-API-002` |
| Negative test mapping | `COND-API-002-VAL` / `UC-ES-04-TC-API-002-VAL`; plus `COND-AUTH` for protected access |

### 9.3 Handler Contract — `POST /api/v1/safety/events/sensor-self-test`

| Item | Exact current contract |
| --- | --- |
| Handler | `create` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/safety/controller/SensorSelfTestController.java` |
| Authorization annotation / boundary | hasRole('MOTHER') |
| Parameters | body `request`: `SensorSelfTestEventRequest`; principal `principal`: `Principal` |
| Request body type | `SensorSelfTestEventRequest` |
| Request fields and validators | `testId`: `String` (@NotBlank, @Size(max = 140)); `detectedAt`: `Instant` (@NotNull); `accelerationMagnitude`: `Double` (@NotNull, @PositiveOrZero); `gyroscopeMagnitude`: `Double` (@NotNull, @PositiveOrZero) |
| Response type | `ResponseEntity<ApiResponse<SafetyEventResponse>>` |
| Response payload fields | `id`: `UUID` (no field annotation in current DTO); `userId`: `UUID` (no field annotation in current DTO); `eventType`: `String` (no field annotation in current DTO); `magnitude`: `double` (no field annotation in current DTO); `userLatitude`: `BigDecimal` (no field annotation in current DTO); `userLongitude`: `BigDecimal` (no field annotation in current DTO); `detectedAt`: `Instant` (no field annotation in current DTO); `clientDetectedAt`: `Instant` (no field annotation in current DTO); `status`: `String` (no field annotation in current DTO); `resolvedAt`: `Instant` (no field annotation in current DTO); `notes`: `String` (no field annotation in current DTO); `countdownDeadlineAt`: `Instant` (no field annotation in current DTO); `responseType`: `String` (no field annotation in current DTO); `responseReason`: `String` (no field annotation in current DTO); `respondedAt`: `Instant` (no field annotation in current DTO); `emergencySessionId`: `UUID` (no field annotation in current DTO) |
| Explicit/documented statuses | `201` |
| Positive test mapping | `COND-API-003` / `UC-ES-04-TC-API-003` |
| Negative test mapping | `COND-API-003-VAL` / `UC-ES-04-TC-API-003-VAL`; plus `COND-AUTH` for protected access |

### 9.4 Handler Contract — `POST /api/v1/safety/events/{eventId}/sensor-self-test/complete`

| Item | Exact current contract |
| --- | --- |
| Handler | `complete` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/safety/controller/SensorSelfTestController.java` |
| Authorization annotation / boundary | hasRole('MOTHER') |
| Parameters | path `eventId`: `UUID`; body `request`: `SensorSelfTestCompletionRequest`; principal `principal`: `Principal` |
| Request body type | `SensorSelfTestCompletionRequest` |
| Request fields and validators | `outcome`: `String` (@NotBlank, @Pattern(regexp = "TIMEOUT\|NEED_HELP")) |
| Response type | `ResponseEntity<ApiResponse<SafetyEventResponse>>` |
| Response payload fields | `id`: `UUID` (no field annotation in current DTO); `userId`: `UUID` (no field annotation in current DTO); `eventType`: `String` (no field annotation in current DTO); `magnitude`: `double` (no field annotation in current DTO); `userLatitude`: `BigDecimal` (no field annotation in current DTO); `userLongitude`: `BigDecimal` (no field annotation in current DTO); `detectedAt`: `Instant` (no field annotation in current DTO); `clientDetectedAt`: `Instant` (no field annotation in current DTO); `status`: `String` (no field annotation in current DTO); `resolvedAt`: `Instant` (no field annotation in current DTO); `notes`: `String` (no field annotation in current DTO); `countdownDeadlineAt`: `Instant` (no field annotation in current DTO); `responseType`: `String` (no field annotation in current DTO); `responseReason`: `String` (no field annotation in current DTO); `respondedAt`: `Instant` (no field annotation in current DTO); `emergencySessionId`: `UUID` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-004` / `UC-ES-04-TC-API-004` |
| Negative test mapping | `COND-API-004-VAL` / `UC-ES-04-TC-API-004-VAL`; plus `COND-AUTH` for protected access |

### 9.5 Handler Contract — `POST /api/v1/safety/fall-detection/disable`

| Item | Exact current contract |
| --- | --- |
| Handler | `disable` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/safety/controller/FallDetectionController.java` |
| Authorization annotation / boundary | hasRole('MOTHER') |
| Parameters | principal `principal`: `Principal` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<Void>>` |
| Response payload fields | Not applicable or unresolved from the handler import |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-005` / `UC-ES-04-TC-API-005` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.6 Handler Contract — `POST /api/v1/safety/fall-detection/enable`

| Item | Exact current contract |
| --- | --- |
| Handler | `enable` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/safety/controller/FallDetectionController.java` |
| Authorization annotation / boundary | hasRole('MOTHER') |
| Parameters | principal `principal`: `Principal` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<ImuMonitoringSessionResponse>>` |
| Response payload fields | `sessionId`: `UUID` (no field annotation in current DTO); `userId`: `UUID` (no field annotation in current DTO); `status`: `String` (no field annotation in current DTO); `sensitivityLevel`: `String` (no field annotation in current DTO); `startedAt`: `Instant` (no field annotation in current DTO); `endedAt`: `Instant` (no field annotation in current DTO) |
| Explicit/documented statuses | `201` |
| Positive test mapping | `COND-API-006` / `UC-ES-04-TC-API-006` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

## 10. Error Codes

| Error class | HTTP / code | Trigger | Client behavior | Oracle |
| --- | --- | --- | --- | --- |
| Validation/business rule | `400` | Malformed field, range, enum, or rejected transition | Return the current stable error envelope without protected data or unintended mutation | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/common/exception/GlobalExceptionHandler.java`; application error code is limited to what those sources/advice explicitly declare |

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
| `VG-01` | Review consent/device permission prerequisites. | `COND-01` | `UC-ES-04-TC-001` |
| `VG-02` | Enable/configure fall detection. | `COND-02` | `UC-ES-04-TC-002` |
| `VG-03` | Run and complete the supported sensor self-test or disable monitoring. | `COND-03` | `UC-ES-04-TC-003` |
| `VG-AUTH` | Reject wrong authentication/role/ownership/membership/consent scope | `COND-AUTH` | `UC-ES-04-TC-SEC-001` |
| `VG-GAP` | Characterize each known gap without claiming a false completed path | `COND-GAP` | `UC-ES-04-TC-GAP-001` |

## 14. Verification Methods

Existing focused evidence:

- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/safety/SafetyConfigControllerTest.java`
- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/safety/SafetyConfigServiceTest.java`
- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/safety/SensorSelfTestServiceTest.java`
- `05_Development/CareBridgeMobileApp/test/features/safety/safety_contract_test.dart`

Exact supported commands derived from the audited test paths:

- `cd 05_Development/CareBridgeAPI && ./mvnw -Dtest=SafetyConfigControllerTest test`
- `cd 05_Development/CareBridgeAPI && ./mvnw -Dtest=SafetyConfigServiceTest test`
- `cd 05_Development/CareBridgeAPI && ./mvnw -Dtest=SensorSelfTestServiceTest test`
- `cd 05_Development/CareBridgeMobileApp && flutter test test/features/safety/safety_contract_test.dart`

Record pass/fail/skip counts only after executing these commands on the exact revision.

## 15. Verification Samples

| Sample | Value |
| --- | --- |
| Primary contract | GET/PUT `/api/v1/safety/config` |
| Request | `GET /api/v1/safety/config` → `getConfig`; `None` with Not applicable — no request body; authorization: hasRole('MOTHER'). |
| Success response | `ResponseEntity<ApiResponse<SafetyConfigResponse>>` with `id`: `UUID` (no field annotation in current DTO); `userId`: `UUID` (no field annotation in current DTO); `fallDetectionEnabled`: `boolean` (no field annotation in current DTO); `sensitivityLevel`: `String` (no field annotation in current DTO); `emergencyAutoAlert`: `boolean` (no field annotation in current DTO); `locationSharingEnabled`: `boolean` (no field annotation in current DTO); `countdownSeconds`: `int` (no field annotation in current DTO); `sensorPermissionGranted`: `boolean` (no field annotation in current DTO); `sensorPermissionRecordedAt`: `Instant` (no field annotation in current DTO); `updatedAt`: `Instant` (no field annotation in current DTO); explicit/documented statuses `200`. |
| Negative sample | Use wrong role/owner/state and assert the exact mapped error without protected payload. |

## 16. Authorization Matrix

| Actor / role | Operation | Decision |
| --- | --- | --- |
| Mother | `GET /api/v1/safety/config` | hasRole('MOTHER'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/safety/controller/SafetyConfigController.java` |
| Mother | `PUT /api/v1/safety/config` | hasRole('MOTHER'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/safety/controller/SafetyConfigController.java` |
| Mother | `POST /api/v1/safety/events/sensor-self-test` | hasRole('MOTHER'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/safety/controller/SensorSelfTestController.java` |
| Mother | `POST /api/v1/safety/events/{eventId}/sensor-self-test/complete` | hasRole('MOTHER'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/safety/controller/SensorSelfTestController.java` |
| Mother | `POST /api/v1/safety/fall-detection/disable` | hasRole('MOTHER'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/safety/controller/FallDetectionController.java` |
| Mother | `POST /api/v1/safety/fall-detection/enable` | hasRole('MOTHER'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/safety/controller/FallDetectionController.java` |
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
