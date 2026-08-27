# ENGINEERING DOCUMENTATION STANDARD (EDS) v2.0

# TECHNICAL DESIGN SPECIFICATION — Track Fetal Movement Sessions

| Field | Value |
| --- | --- |
| Document ID | `UC-MH-08-TDS` |
| Version | `0.1` |
| Date | `2026-08-23` |
| Status | `Draft` |
| Function ID | `UC-MH-08` |
| Canonical Use Case | `UC-MH-08 — Track Fetal Movement Sessions` |
| Module / Bounded Context | `Mother Journey and Health` |
| Primary Actor | `Mother` |
| Platforms | `Mobile / Backend` |
| Priority | `Medium` |
| Data Classification | `Restricted maternal health, screening, journey, record, and attachment data; Confidential schedule/preferences` |
| Compliance Scope | `PDPA health-data minimization, consent/ownership enforcement, clinical disclaimer where applicable, and purpose-bound file access` |
| Owner | `CareBridge Team` |
| Reviewer / Approver |  |
| Source Baseline | Current worktree on `2026-08-23`; SRS `UC-MH-08`; exact evidence in Section 1.4 |

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

- **Goal:** Run a timed fetal-movement observation session and store the supported session result as a journey metric.
- **Trigger:** The actor enters Mobile metric add route with `metricType=FETAL_MOVEMENT_SESSION`.
- **Outcome:** Complete and persist the session observation.
- **Current state:** `High` confidence from reachable code/test audit; no manifest-level exclusion is recorded.
- **Target state:** Preserve current code-backed behavior and resolve only explicitly evidenced limitations through approved implementation work.

### 1.2 Scope

**In scope**

- Mobile metric add route with `metricType=FETAL_MOVEMENT_SESSION`

- GET/POST/PUT `/api/v1/journeys/{journeyId}/metrics/**`

**Out of scope / limitations**

- Not applicable — no manifest-level exclusion is recorded. Scope is limited to the entry points and exact contracts listed above; unrelated handlers in the same module are excluded.

### 1.3 Preconditions and Postconditions

| Type | Condition |
| --- | --- |
| Precondition | Mother is authenticated/authorized where the current contract requires it. |
| Precondition | Required ownership, membership, consent, resource state, and device/provider prerequisites pass current policies. |
| Postcondition | Complete and persist the session observation. |
| Postcondition | No side effect outside the feature-owned persistence/event/provider boundary occurs. |

### 1.4 Evidence Baseline

| Source ID | Type | Exact path | Revision | Authority |
| --- | --- | --- | --- | --- |
| `SRC-SRS-01` | Requirement | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-MH-08 | 2026-08-23 | Draft code-first requirement |
| `SRC-CODE-01` | Current code | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/health/controller/JourneyMetricController.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-02` | Current code | `05_Development/CareBridgeMobileApp/lib/features/healthRecords/screens/fetal_movement_tracker_screen.dart` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-03` | Current code | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/health/service/MetricObservationValidator.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-01` | Existing test | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/health/MetricObservationValidatorTest.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-ERROR-01` | Current exception advice | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/common/exception/GlobalExceptionHandler.java` | Worktree `2026-08-23` | Current-state evidence |

### 1.5 Open Contradictions / Questions

- No material contradiction was found among the exact sources cited in Section 1.4. Behavior not evidenced by those sources is not approved or implied by this Draft.

## 2. Traceability Matrix

| Requirement | Behavior | Exact oracle source | Component | Test condition / case |
| --- | --- | --- | --- | --- |
| `UC-MH-08-FR-01` | Start a fetal-movement session for an eligible journey. | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-MH-08 Normal Flow 1 | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/health/controller/JourneyMetricController.java` | `COND-01` / `UC-MH-08-TC-001` |
| `UC-MH-08-FR-02` | Record movements and the observation period. | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-MH-08 Normal Flow 2 | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/health/controller/JourneyMetricController.java` | `COND-02` / `UC-MH-08-TC-002` |
| `UC-MH-08-FR-03` | Complete and persist the session observation. | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-MH-08 Normal Flow 3 | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/health/controller/JourneyMetricController.java` | `COND-03` / `UC-MH-08-TC-003` |
| `BR-01` | Period start/end and movement values follow server validation. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/health/service/MetricObservationValidator.java` | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/health/service/MetricObservationValidator.java` | `COND-BR-01` / `UC-MH-08-TC-BR-001` |
| `BR-02` | This is tracking, not a diagnostic conclusion. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/health/service/MetricObservationValidator.java` | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/health/service/MetricObservationValidator.java` | `COND-BR-02` / `UC-MH-08-TC-BR-002` |

## 3. Architecture Decision Records (ADR)

### ADR-UC-MH-08-01 — Use a distinct actor-goal boundary

| Item | Decision |
| --- | --- |
| Context | The retired 43-UC catalogue grouped multiple triggers, lifecycles, and permission boundaries, making implementation/test traceability generic. |
| Options | Keep the broad catalogue; split by screen; split by actor goal plus lifecycle/security boundary. |
| Decision | Use `UC-MH-08 — Track Fetal Movement Sessions` as the canonical boundary because its operations share the stated actor outcome and current implementation evidence. |
| Consequences | Related supporting screens/endpoints stay in one TDS; different lifecycle/actor outcomes have separate UCs. |
| Source / Status | SRS Section 3.1 and current code audit / Draft |

### ADR-UC-MH-08-02 — Preserve unknowns as Open

| Item | Decision |
| --- | --- |
| Context | Exact schema fields, SLA values, and some controller error codes are not fully evidenced by this manifest alone. |
| Decision | Do not invent them. Mark them `Open` and require the exact DTO/entity/migration/policy oracle before production-code changes. |
| Consequences | This document accurately characterizes current scope; unresolved design-changing items block implementation approval. |
| Source / Status | `create-specs` evidence discipline / Draft |

## 4. Non-Functional Requirements and SLA

| NFR | Target | Oracle Source | Verification |
| --- | --- | --- | --- |
| Authorization / isolation | All requests follow exact role/ownership/membership/consent policy in current code. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/health/controller/JourneyMetricController.java` | Negative security cases in paired Test-Spec |
| Protected-data handling | No secrets or unnecessary health/location/identity/conversation/file payloads in logs, fixtures, screenshots, or audit detail. | Data classification header plus exact Section 9 request/response field inventories | Log/fixture review plus security tests |
| Availability / latency | Open — no approved feature-specific numeric SLA found. | Evidence needed: approved NFR/SLA source | Measure only; do not assert a fixed threshold |
| Accessibility | Open — confirm project-standard criteria for reachable UI surfaces. | Evidence needed: approved UX/accessibility standard | Applicable Web/Mobile UI checks |
| Retry / idempotency | Apply only semantics explicitly implemented by the owning service. | No explicit lock/version/idempotency marker is evidenced in the cited implementation sources; preserve observed behavior and add characterization before changing concurrency semantics | State/duplicate/concurrency cases where applicable |

## 5. Static Modeling

### 5.1 Component Responsibilities and Change Disposition

| Exact path | Disposition | Responsibility |
| --- | --- | --- |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/health/controller/JourneyMetricController.java` | Reuse | Current implementation evidence for Track Fetal Movement Sessions; inspect the exact symbol before implementation changes. |
| `05_Development/CareBridgeMobileApp/lib/features/healthRecords/screens/fetal_movement_tracker_screen.dart` | Reuse | Current implementation evidence for Track Fetal Movement Sessions; inspect the exact symbol before implementation changes. |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/health/service/MetricObservationValidator.java` | Reuse | Current implementation evidence for Track Fetal Movement Sessions; inspect the exact symbol before implementation changes. |

### 5.2 Current Component Diagram

```plantuml
@startuml
skinparam classAttributeIconSize 0
class JourneyMetricController as "JourneyMetricController.java"
class fetal_movement_tracker_screen as "fetal_movement_tracker_screen.dart"
JourneyMetricController --> fetal_movement_tracker_screen
class MetricObservationValidator as "MetricObservationValidator.java"
fetal_movement_tracker_screen --> MetricObservationValidator
@enduml
```

### 5.3 Data / Schema / Migration Assessment

| Item | Assessment |
| --- | --- |
| Current stores/entities | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/health/entity/DataSource.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/health/entity/MetricDefinition.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/health/entity/MetricType.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/health/entity/ObservationShape.java` |
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
Actor -> Client: Enter Track Fetal Movement Sessions
Client -> Domain: Start a fetal-movement session for an eligible journey.
Domain --> Client: Result for step 1
Client -> Domain: Record movements and the observation period.
Domain --> Client: Result for step 2
Client -> Domain: Complete and persist the session observation.
Domain --> Client: Result for step 3
Client --> Actor: Render canonical outcome
@enduml
```

### 6.2 Alternative, Error, Retry, and Concurrency Flows

| Flow | Expected design behavior | Oracle |
| --- | --- | --- |
| Cancel before mutation | No unintended write or provider side effect. | SRS UC-MH-08 Alternative Flow |
| Invalid input/state | Reject with the current contract; keep canonical state unchanged. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/health/controller/JourneyMetricController.java` |
| Wrong actor/scope | Fail closed without protected resource disclosure. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/health/controller/JourneyMetricController.java` |
| Dependency failure | Use only the implemented bounded fallback/retry; never report false success. | Not applicable at this cited baseline — no external adapter/provider import is evidenced in the listed implementation sources |
| Duplicate/concurrent mutation | Apply only current lock/version/idempotency semantics. | No explicit lock/version/idempotency marker is evidenced in the cited implementation sources; preserve observed behavior and add characterization before changing concurrency semantics |

### 6.3 State Model and Invariants

```plantuml
@startuml
[*] --> Eligible
Eligible --> InProgress : Start a fetal-movement session for an eligible journey.
InProgress --> Outcome : Complete and persist the session observation.
InProgress --> Rejected : validation / authorization / state failure
Outcome --> [*]
Rejected --> Eligible : actor corrects eligible input
@enduml
```

- Period start/end and movement values follow server validation.
- This is tracking, not a diagnostic conclusion.

## 7. Domain Event Catalog

| Direction | Event | Producer / Consumer | Payload / delivery / idempotency |
| --- | --- | --- | --- |
| Publish / consume | Current event evidence | Not applicable at this cited baseline — no event type, publisher, or listener is evidenced in the listed implementation sources | Preserve only events evidenced by the cited source set; if later call-path inspection finds none, the flow remains synchronous. |

## 8. Interface Specification

### 8.1 User / Operator Interfaces

| # | Entry point | Actor | Contract |
| ---: | --- | --- | --- |
| 1 | Mobile metric add route with `metricType=FETAL_MOVEMENT_SESSION` | Mother | Reachable current entry point |

### 8.2 Service / Repository Interfaces

| Interface evidence | Required responsibility |
| --- | --- |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/health/controller/JourneyMetricController.java` | Support the mapped operations without broadening authorization or lifecycle semantics. |
| `05_Development/CareBridgeMobileApp/lib/features/healthRecords/screens/fetal_movement_tracker_screen.dart` | Support the mapped operations without broadening authorization or lifecycle semantics. |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/health/service/MetricObservationValidator.java` | Support the mapped operations without broadening authorization or lifecycle semantics. |

## 9. API Specification

| ID | Method / path or grouped controller surface | Auth / role | Request / response / errors |
| --- | --- | --- | --- |
| `API-01` | `GET /api/v1/journeys/{journeyId}/metrics` | hasAnyRole('MOTHER', 'FAMILY', 'EXPERT') | Handler `getMetricTrend`; parameters: path `journeyId`: `UUID`; query `metricType`: `MetricType`; query `from`: `Instant`; query `to`: `Instant`; principal `principal`: `Principal`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<MetricTrendResponse>>`; response payload fields: `metricType`: `String` (no field annotation in current DTO); `unit`: `String` (no field annotation in current DTO); `dataPoints`: `List<MetricDataPoint>` (no field annotation in current DTO); `disclaimer`: `String` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/health/controller/JourneyMetricController.java` |
| `API-02` | `POST /api/v1/journeys/{journeyId}/metrics` | hasRole('MOTHER') | Handler `addMetric`; parameters: path `journeyId`: `UUID`; body `request`: `AddMetricRequest`; principal `principal`: `Principal`; request body: `AddMetricRequest`; request fields/validation: `metricType`: `MetricType` (@NotNull); `valueNumeric`: `BigDecimal` (@NotNull); `valueSecondary`: `BigDecimal` (no field annotation in current DTO); `unit`: `String` (@Size(max = 30)); `measuredAt`: `Instant` (@NotNull); `sourceType`: `DataSource` (no field annotation in current DTO); `note`: `String` (@Size(max = 2000)); `context`: `Map<String, Object>` (no field annotation in current DTO); `periodStart`: `Instant` (no field annotation in current DTO); `periodEnd`: `Instant` (no field annotation in current DTO); `definitionVersion`: `Integer` (no field annotation in current DTO); response: `ResponseEntity<ApiResponse<MetricResponse>>`; response payload fields: `metricId`: `UUID` (no field annotation in current DTO); `journeyId`: `UUID` (no field annotation in current DTO); `metricType`: `String` (no field annotation in current DTO); `valueNumeric`: `BigDecimal` (no field annotation in current DTO); `valueSecondary`: `BigDecimal` (no field annotation in current DTO); `unit`: `String` (no field annotation in current DTO); `measuredAt`: `Instant` (no field annotation in current DTO); `sourceType`: `String` (no field annotation in current DTO); `note`: `String` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO); `updatedAt`: `Instant` (no field annotation in current DTO); `context`: `Map<String, Object>` (no field annotation in current DTO); `periodStart`: `Instant` (no field annotation in current DTO); `periodEnd`: `Instant` (no field annotation in current DTO); `qualityLabel`: `String` (no field annotation in current DTO); `disclaimer`: `String` (no field annotation in current DTO); `definitionVersion`: `Integer` (no field annotation in current DTO); `aiInsight`: `String` (no field annotation in current DTO); `redFlagAlert`: `boolean` (no field annotation in current DTO); explicit/documented statuses: `201`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/health/controller/JourneyMetricController.java` |
| `API-03` | `GET /api/v1/journeys/{journeyId}/metrics/capabilities` | hasRole('MOTHER') | Handler `getCapabilities`; parameters: path `journeyId`: `UUID`; principal `principal`: `Principal`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<java.util.List<MetricCapabilityResponse>>>`; response payload fields: Not applicable or unresolved from the handler import; explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/health/controller/JourneyMetricController.java` |
| `API-04` | `PUT /api/v1/journeys/{journeyId}/metrics/{metricId}` | hasRole('MOTHER') | Handler `updateMetric`; parameters: path `journeyId`: `UUID`; path `metricId`: `UUID`; body `request`: `UpdateMetricRequest`; principal `principal`: `Principal`; request body: `UpdateMetricRequest`; request fields/validation: `valueNumeric`: `BigDecimal` (no field annotation in current DTO); `valueSecondary`: `BigDecimal` (no field annotation in current DTO); `unit`: `String` (@Size(max = 30)); `measuredAt`: `Instant` (no field annotation in current DTO); `note`: `String` (@Size(max = 2000)); `context`: `Map<String, Object>` (no field annotation in current DTO); `periodStart`: `Instant` (no field annotation in current DTO); `periodEnd`: `Instant` (no field annotation in current DTO); response: `ResponseEntity<ApiResponse<MetricResponse>>`; response payload fields: `metricId`: `UUID` (no field annotation in current DTO); `journeyId`: `UUID` (no field annotation in current DTO); `metricType`: `String` (no field annotation in current DTO); `valueNumeric`: `BigDecimal` (no field annotation in current DTO); `valueSecondary`: `BigDecimal` (no field annotation in current DTO); `unit`: `String` (no field annotation in current DTO); `measuredAt`: `Instant` (no field annotation in current DTO); `sourceType`: `String` (no field annotation in current DTO); `note`: `String` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO); `updatedAt`: `Instant` (no field annotation in current DTO); `context`: `Map<String, Object>` (no field annotation in current DTO); `periodStart`: `Instant` (no field annotation in current DTO); `periodEnd`: `Instant` (no field annotation in current DTO); `qualityLabel`: `String` (no field annotation in current DTO); `disclaimer`: `String` (no field annotation in current DTO); `definitionVersion`: `Integer` (no field annotation in current DTO); `aiInsight`: `String` (no field annotation in current DTO); `redFlagAlert`: `boolean` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/health/controller/JourneyMetricController.java` |

Method-level Spring handlers, authorization annotations, request DTO fields/validators, response payload fields, and explicit/documented statuses above were extracted from the cited current source. Service/advice-only application error codes are not claimed where the controller does not declare them.

### 9.1 Handler Contract — `GET /api/v1/journeys/{journeyId}/metrics`

| Item | Exact current contract |
| --- | --- |
| Handler | `getMetricTrend` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/health/controller/JourneyMetricController.java` |
| Authorization annotation / boundary | hasAnyRole('MOTHER', 'FAMILY', 'EXPERT') |
| Parameters | path `journeyId`: `UUID`; query `metricType`: `MetricType`; query `from`: `Instant`; query `to`: `Instant`; principal `principal`: `Principal` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<MetricTrendResponse>>` |
| Response payload fields | `metricType`: `String` (no field annotation in current DTO); `unit`: `String` (no field annotation in current DTO); `dataPoints`: `List<MetricDataPoint>` (no field annotation in current DTO); `disclaimer`: `String` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-001` / `UC-MH-08-TC-API-001` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.2 Handler Contract — `POST /api/v1/journeys/{journeyId}/metrics`

| Item | Exact current contract |
| --- | --- |
| Handler | `addMetric` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/health/controller/JourneyMetricController.java` |
| Authorization annotation / boundary | hasRole('MOTHER') |
| Parameters | path `journeyId`: `UUID`; body `request`: `AddMetricRequest`; principal `principal`: `Principal` |
| Request body type | `AddMetricRequest` |
| Request fields and validators | `metricType`: `MetricType` (@NotNull); `valueNumeric`: `BigDecimal` (@NotNull); `valueSecondary`: `BigDecimal` (no field annotation in current DTO); `unit`: `String` (@Size(max = 30)); `measuredAt`: `Instant` (@NotNull); `sourceType`: `DataSource` (no field annotation in current DTO); `note`: `String` (@Size(max = 2000)); `context`: `Map<String, Object>` (no field annotation in current DTO); `periodStart`: `Instant` (no field annotation in current DTO); `periodEnd`: `Instant` (no field annotation in current DTO); `definitionVersion`: `Integer` (no field annotation in current DTO) |
| Response type | `ResponseEntity<ApiResponse<MetricResponse>>` |
| Response payload fields | `metricId`: `UUID` (no field annotation in current DTO); `journeyId`: `UUID` (no field annotation in current DTO); `metricType`: `String` (no field annotation in current DTO); `valueNumeric`: `BigDecimal` (no field annotation in current DTO); `valueSecondary`: `BigDecimal` (no field annotation in current DTO); `unit`: `String` (no field annotation in current DTO); `measuredAt`: `Instant` (no field annotation in current DTO); `sourceType`: `String` (no field annotation in current DTO); `note`: `String` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO); `updatedAt`: `Instant` (no field annotation in current DTO); `context`: `Map<String, Object>` (no field annotation in current DTO); `periodStart`: `Instant` (no field annotation in current DTO); `periodEnd`: `Instant` (no field annotation in current DTO); `qualityLabel`: `String` (no field annotation in current DTO); `disclaimer`: `String` (no field annotation in current DTO); `definitionVersion`: `Integer` (no field annotation in current DTO); `aiInsight`: `String` (no field annotation in current DTO); `redFlagAlert`: `boolean` (no field annotation in current DTO) |
| Explicit/documented statuses | `201` |
| Positive test mapping | `COND-API-002` / `UC-MH-08-TC-API-002` |
| Negative test mapping | `COND-API-002-VAL` / `UC-MH-08-TC-API-002-VAL`; plus `COND-AUTH` for protected access |

### 9.3 Handler Contract — `GET /api/v1/journeys/{journeyId}/metrics/capabilities`

| Item | Exact current contract |
| --- | --- |
| Handler | `getCapabilities` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/health/controller/JourneyMetricController.java` |
| Authorization annotation / boundary | hasRole('MOTHER') |
| Parameters | path `journeyId`: `UUID`; principal `principal`: `Principal` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<java.util.List<MetricCapabilityResponse>>>` |
| Response payload fields | Not applicable or unresolved from the handler import |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-003` / `UC-MH-08-TC-API-003` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.4 Handler Contract — `PUT /api/v1/journeys/{journeyId}/metrics/{metricId}`

| Item | Exact current contract |
| --- | --- |
| Handler | `updateMetric` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/health/controller/JourneyMetricController.java` |
| Authorization annotation / boundary | hasRole('MOTHER') |
| Parameters | path `journeyId`: `UUID`; path `metricId`: `UUID`; body `request`: `UpdateMetricRequest`; principal `principal`: `Principal` |
| Request body type | `UpdateMetricRequest` |
| Request fields and validators | `valueNumeric`: `BigDecimal` (no field annotation in current DTO); `valueSecondary`: `BigDecimal` (no field annotation in current DTO); `unit`: `String` (@Size(max = 30)); `measuredAt`: `Instant` (no field annotation in current DTO); `note`: `String` (@Size(max = 2000)); `context`: `Map<String, Object>` (no field annotation in current DTO); `periodStart`: `Instant` (no field annotation in current DTO); `periodEnd`: `Instant` (no field annotation in current DTO) |
| Response type | `ResponseEntity<ApiResponse<MetricResponse>>` |
| Response payload fields | `metricId`: `UUID` (no field annotation in current DTO); `journeyId`: `UUID` (no field annotation in current DTO); `metricType`: `String` (no field annotation in current DTO); `valueNumeric`: `BigDecimal` (no field annotation in current DTO); `valueSecondary`: `BigDecimal` (no field annotation in current DTO); `unit`: `String` (no field annotation in current DTO); `measuredAt`: `Instant` (no field annotation in current DTO); `sourceType`: `String` (no field annotation in current DTO); `note`: `String` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO); `updatedAt`: `Instant` (no field annotation in current DTO); `context`: `Map<String, Object>` (no field annotation in current DTO); `periodStart`: `Instant` (no field annotation in current DTO); `periodEnd`: `Instant` (no field annotation in current DTO); `qualityLabel`: `String` (no field annotation in current DTO); `disclaimer`: `String` (no field annotation in current DTO); `definitionVersion`: `Integer` (no field annotation in current DTO); `aiInsight`: `String` (no field annotation in current DTO); `redFlagAlert`: `boolean` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-004` / `UC-MH-08-TC-API-004` |
| Negative test mapping | `COND-API-004-VAL` / `UC-MH-08-TC-API-004-VAL`; plus `COND-AUTH` for protected access |

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
| `VG-01` | Start a fetal-movement session for an eligible journey. | `COND-01` | `UC-MH-08-TC-001` |
| `VG-02` | Record movements and the observation period. | `COND-02` | `UC-MH-08-TC-002` |
| `VG-03` | Complete and persist the session observation. | `COND-03` | `UC-MH-08-TC-003` |
| `VG-AUTH` | Reject wrong authentication/role/ownership/membership/consent scope | `COND-AUTH` | `UC-MH-08-TC-SEC-001` |
| `VG-GAP` | Characterize each known gap without claiming a false completed path | `COND-GAP` | `UC-MH-08-TC-GAP-001` |

## 14. Verification Methods

Existing focused evidence:

- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/health/MetricObservationValidatorTest.java`

Exact supported commands derived from the audited test paths:

- `cd 05_Development/CareBridgeAPI && ./mvnw -Dtest=MetricObservationValidatorTest test`

Record pass/fail/skip counts only after executing these commands on the exact revision.

## 15. Verification Samples

| Sample | Value |
| --- | --- |
| Primary contract | GET/POST/PUT `/api/v1/journeys/{journeyId}/metrics/**` |
| Request | `GET /api/v1/journeys/{journeyId}/metrics` → `getMetricTrend`; `None` with Not applicable — no request body; authorization: hasAnyRole('MOTHER', 'FAMILY', 'EXPERT'). |
| Success response | `ResponseEntity<ApiResponse<MetricTrendResponse>>` with `metricType`: `String` (no field annotation in current DTO); `unit`: `String` (no field annotation in current DTO); `dataPoints`: `List<MetricDataPoint>` (no field annotation in current DTO); `disclaimer`: `String` (no field annotation in current DTO); explicit/documented statuses `200`. |
| Negative sample | Use wrong role/owner/state and assert the exact mapped error without protected payload. |

## 16. Authorization Matrix

| Actor / role | Operation | Decision |
| --- | --- | --- |
| Mother | `GET /api/v1/journeys/{journeyId}/metrics` | hasAnyRole('MOTHER', 'FAMILY', 'EXPERT'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/health/controller/JourneyMetricController.java` |
| Mother | `POST /api/v1/journeys/{journeyId}/metrics` | hasRole('MOTHER'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/health/controller/JourneyMetricController.java` |
| Mother | `GET /api/v1/journeys/{journeyId}/metrics/capabilities` | hasRole('MOTHER'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/health/controller/JourneyMetricController.java` |
| Mother | `PUT /api/v1/journeys/{journeyId}/metrics/{metricId}` | hasRole('MOTHER'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/health/controller/JourneyMetricController.java` |
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
