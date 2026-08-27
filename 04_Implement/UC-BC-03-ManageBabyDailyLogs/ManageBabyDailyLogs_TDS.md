# ENGINEERING DOCUMENTATION STANDARD (EDS) v2.0

# TECHNICAL DESIGN SPECIFICATION — Manage Baby Daily Logs

| Field | Value |
| --- | --- |
| Document ID | `UC-BC-03-TDS` |
| Version | `0.1` |
| Date | `2026-08-23` |
| Status | `Draft` |
| Function ID | `UC-BC-03` |
| Canonical Use Case | `UC-BC-03 — Manage Baby Daily Logs` |
| Module / Bounded Context | `Baby Care` |
| Primary Actor | `Mother / Authorized Caregiver` |
| Platforms | `Mobile / Web / Backend` |
| Priority | `Medium` |
| Data Classification | `Restricted child health, growth, milestone, vaccination, and daily-care data` |
| Compliance Scope | `PDPA child/health-data minimization, caregiver authorization, non-diagnostic presentation, and retention/audit controls` |
| Owner | `CareBridge Team` |
| Reviewer / Approver |  |
| Source Baseline | Current worktree on `2026-08-23`; SRS `UC-BC-03`; exact evidence in Section 1.4 |

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

- **Goal:** Create, view, edit, and remove eligible baby feeding, sleep, diaper, or other supported daily logs.
- **Trigger:** The actor enters Baby detail log modal and Mobile `/babies/:babyId/daily-logs/:logId*`.
- **Outcome:** Edit or delete it when ownership/state permits.
- **Current state:** `High` confidence from reachable code/test audit; no manifest-level exclusion is recorded.
- **Target state:** Preserve current code-backed behavior and resolve only explicitly evidenced limitations through approved implementation work.

### 1.2 Scope

**In scope**

- Baby detail log modal and Mobile `/babies/:babyId/daily-logs/:logId*`
- Web baby-care log forms

- Daily-log endpoints under `/api/v1/babies/{babyId}/daily-logs/**`

**Out of scope / limitations**

- Not applicable — no manifest-level exclusion is recorded. Scope is limited to the entry points and exact contracts listed above; unrelated handlers in the same module are excluded.

### 1.3 Preconditions and Postconditions

| Type | Condition |
| --- | --- |
| Precondition | Mother / Authorized Caregiver is authenticated/authorized where the current contract requires it. |
| Precondition | Required ownership, membership, consent, resource state, and device/provider prerequisites pass current policies. |
| Postcondition | Edit or delete it when ownership/state permits. |
| Postcondition | No side effect outside the feature-owned persistence/event/provider boundary occurs. |

### 1.4 Evidence Baseline

| Source ID | Type | Exact path | Revision | Authority |
| --- | --- | --- | --- | --- |
| `SRC-SRS-01` | Requirement | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-BC-03 | 2026-08-23 | Draft code-first requirement |
| `SRC-CODE-01` | Current code | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/carejourney/controller/BabyDailyLogController.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-02` | Current code | `05_Development/CareBridgeMobileApp/lib/features/baby/screens/edit_baby_daily_log_screen.dart` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-03` | Current code | `05_Development/CareBridgeWebApp/src/features/babyCare/pages/BabyCareHubPage.tsx` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-01` | Existing test | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/carejourney/BabyDailyLogServiceTest.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-ERROR-01` | Current exception advice | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/common/exception/GlobalExceptionHandler.java` | Worktree `2026-08-23` | Current-state evidence |

### 1.5 Open Contradictions / Questions

- No material contradiction was found among the exact sources cited in Section 1.4. Behavior not evidenced by those sources is not approved or implied by this Draft.

## 2. Traceability Matrix

| Requirement | Behavior | Exact oracle source | Component | Test condition / case |
| --- | --- | --- | --- | --- |
| `UC-BC-03-FR-01` | Choose a supported log type and enter type-specific data. | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-BC-03 Normal Flow 1 | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/carejourney/controller/BabyDailyLogController.java` | `COND-01` / `UC-BC-03-TC-001` |
| `UC-BC-03-FR-02` | Persist and inspect the log detail. | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-BC-03 Normal Flow 2 | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/carejourney/controller/BabyDailyLogController.java` | `COND-02` / `UC-BC-03-TC-002` |
| `UC-BC-03-FR-03` | Edit or delete it when ownership/state permits. | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-BC-03 Normal Flow 3 | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/carejourney/controller/BabyDailyLogController.java` | `COND-03` / `UC-BC-03-TC-003` |
| `BR-01` | Type-specific field validation and baby authorization are server authoritative. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/carejourney/controller/BabyDailyLogController.java` | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/carejourney/controller/BabyDailyLogController.java` | `COND-BR-01` / `UC-BC-03-TC-BR-001` |
| `BR-02` | Log time/order uses server-normalized values. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/carejourney/controller/BabyDailyLogController.java` | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/carejourney/controller/BabyDailyLogController.java` | `COND-BR-02` / `UC-BC-03-TC-BR-002` |

## 3. Architecture Decision Records (ADR)

### ADR-UC-BC-03-01 — Use a distinct actor-goal boundary

| Item | Decision |
| --- | --- |
| Context | The retired 43-UC catalogue grouped multiple triggers, lifecycles, and permission boundaries, making implementation/test traceability generic. |
| Options | Keep the broad catalogue; split by screen; split by actor goal plus lifecycle/security boundary. |
| Decision | Use `UC-BC-03 — Manage Baby Daily Logs` as the canonical boundary because its operations share the stated actor outcome and current implementation evidence. |
| Consequences | Related supporting screens/endpoints stay in one TDS; different lifecycle/actor outcomes have separate UCs. |
| Source / Status | SRS Section 3.1 and current code audit / Draft |

### ADR-UC-BC-03-02 — Preserve unknowns as Open

| Item | Decision |
| --- | --- |
| Context | Exact schema fields, SLA values, and some controller error codes are not fully evidenced by this manifest alone. |
| Decision | Do not invent them. Mark them `Open` and require the exact DTO/entity/migration/policy oracle before production-code changes. |
| Consequences | This document accurately characterizes current scope; unresolved design-changing items block implementation approval. |
| Source / Status | `create-specs` evidence discipline / Draft |

## 4. Non-Functional Requirements and SLA

| NFR | Target | Oracle Source | Verification |
| --- | --- | --- | --- |
| Authorization / isolation | All requests follow exact role/ownership/membership/consent policy in current code. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/carejourney/controller/BabyDailyLogController.java` | Negative security cases in paired Test-Spec |
| Protected-data handling | No secrets or unnecessary health/location/identity/conversation/file payloads in logs, fixtures, screenshots, or audit detail. | Data classification header plus exact Section 9 request/response field inventories | Log/fixture review plus security tests |
| Availability / latency | Open — no approved feature-specific numeric SLA found. | Evidence needed: approved NFR/SLA source | Measure only; do not assert a fixed threshold |
| Accessibility | Open — confirm project-standard criteria for reachable UI surfaces. | Evidence needed: approved UX/accessibility standard | Applicable Web/Mobile UI checks |
| Retry / idempotency | Apply only semantics explicitly implemented by the owning service. | No explicit lock/version/idempotency marker is evidenced in the cited implementation sources; preserve observed behavior and add characterization before changing concurrency semantics | State/duplicate/concurrency cases where applicable |

## 5. Static Modeling

### 5.1 Component Responsibilities and Change Disposition

| Exact path | Disposition | Responsibility |
| --- | --- | --- |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/carejourney/controller/BabyDailyLogController.java` | Reuse | Current implementation evidence for Manage Baby Daily Logs; inspect the exact symbol before implementation changes. |
| `05_Development/CareBridgeMobileApp/lib/features/baby/screens/edit_baby_daily_log_screen.dart` | Reuse | Current implementation evidence for Manage Baby Daily Logs; inspect the exact symbol before implementation changes. |
| `05_Development/CareBridgeWebApp/src/features/babyCare/pages/BabyCareHubPage.tsx` | Reuse | Current implementation evidence for Manage Baby Daily Logs; inspect the exact symbol before implementation changes. |

### 5.2 Current Component Diagram

```plantuml
@startuml
skinparam classAttributeIconSize 0
class BabyDailyLogController as "BabyDailyLogController.java"
class edit_baby_daily_log_screen as "edit_baby_daily_log_screen.dart"
BabyDailyLogController --> edit_baby_daily_log_screen
class BabyCareHubPage as "BabyCareHubPage.tsx"
edit_baby_daily_log_screen --> BabyCareHubPage
@enduml
```

### 5.3 Data / Schema / Migration Assessment

| Item | Assessment |
| --- | --- |
| Current stores/entities | No entity/repository import is present in the cited entry/source set; follow the exact handler-to-service call path before any schema change |
| Sensitive fields | Restricted child health, growth, milestone, vaccination, and daily-care data. Exact transport fields and validators are enumerated per handler in Section 9; entity-only fields require the cited service/entity source before a schema change. |
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
Actor -> Client: Enter Manage Baby Daily Logs
Client -> Domain: Choose a supported log type and enter type-specific data.
Domain --> Client: Result for step 1
Client -> Domain: Persist and inspect the log detail.
Domain --> Client: Result for step 2
Client -> Domain: Edit or delete it when ownership/state permits.
Domain --> Client: Result for step 3
Client --> Actor: Render canonical outcome
@enduml
```

### 6.2 Alternative, Error, Retry, and Concurrency Flows

| Flow | Expected design behavior | Oracle |
| --- | --- | --- |
| Cancel before mutation | No unintended write or provider side effect. | SRS UC-BC-03 Alternative Flow |
| Invalid input/state | Reject with the current contract; keep canonical state unchanged. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/carejourney/controller/BabyDailyLogController.java` |
| Wrong actor/scope | Fail closed without protected resource disclosure. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/carejourney/controller/BabyDailyLogController.java` |
| Dependency failure | Use only the implemented bounded fallback/retry; never report false success. | Not applicable at this cited baseline — no external adapter/provider import is evidenced in the listed implementation sources |
| Duplicate/concurrent mutation | Apply only current lock/version/idempotency semantics. | No explicit lock/version/idempotency marker is evidenced in the cited implementation sources; preserve observed behavior and add characterization before changing concurrency semantics |

### 6.3 State Model and Invariants

```plantuml
@startuml
[*] --> Eligible
Eligible --> InProgress : Choose a supported log type and enter type-specific data.
InProgress --> Outcome : Edit or delete it when ownership/state permits.
InProgress --> Rejected : validation / authorization / state failure
Outcome --> [*]
Rejected --> Eligible : actor corrects eligible input
@enduml
```

- Type-specific field validation and baby authorization are server authoritative.
- Log time/order uses server-normalized values.

## 7. Domain Event Catalog

| Direction | Event | Producer / Consumer | Payload / delivery / idempotency |
| --- | --- | --- | --- |
| Publish / consume | Current event evidence | Not applicable at this cited baseline — no event type, publisher, or listener is evidenced in the listed implementation sources | Preserve only events evidenced by the cited source set; if later call-path inspection finds none, the flow remains synchronous. |

## 8. Interface Specification

### 8.1 User / Operator Interfaces

| # | Entry point | Actor | Contract |
| ---: | --- | --- | --- |
| 1 | Baby detail log modal and Mobile `/babies/:babyId/daily-logs/:logId*` | Mother / Authorized Caregiver | Reachable current entry point |
| 2 | Web baby-care log forms | Mother / Authorized Caregiver | Reachable current entry point |

### 8.2 Service / Repository Interfaces

| Interface evidence | Required responsibility |
| --- | --- |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/carejourney/controller/BabyDailyLogController.java` | Support the mapped operations without broadening authorization or lifecycle semantics. |
| `05_Development/CareBridgeMobileApp/lib/features/baby/screens/edit_baby_daily_log_screen.dart` | Support the mapped operations without broadening authorization or lifecycle semantics. |
| `05_Development/CareBridgeWebApp/src/features/babyCare/pages/BabyCareHubPage.tsx` | Support the mapped operations without broadening authorization or lifecycle semantics. |

## 9. API Specification

| ID | Method / path or grouped controller surface | Auth / role | Request / response / errors |
| --- | --- | --- | --- |
| `API-01` | `GET /api/v1/babies/{babyId}/daily-logs` | isAuthenticated() | Handler `getDailyLogs`; parameters: path `babyId`: `UUID`; principal `principal`: `Principal`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ApiResponse<List<BabyDailyLogResponse>>`; response payload fields: `babyLogId`: `UUID` (no field annotation in current DTO); `babyId`: `UUID` (no field annotation in current DTO); `logType`: `String` (no field annotation in current DTO); `startedAt`: `Instant` (no field annotation in current DTO); `endedAt`: `Instant` (no field annotation in current DTO); `quantity`: `BigDecimal` (no field annotation in current DTO); `unit`: `String` (no field annotation in current DTO); `note`: `String` (no field annotation in current DTO); `recordedBy`: `UUID` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO); `updatedAt`: `Instant` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/carejourney/controller/BabyDailyLogController.java` |
| `API-02` | `POST /api/v1/babies/{babyId}/daily-logs` | hasAnyRole('MOTHER', 'FAMILY') | Handler `addDailyLog`; parameters: path `babyId`: `UUID`; body `request`: `AddBabyDailyLogRequest`; principal `principal`: `Principal`; request body: `AddBabyDailyLogRequest`; request fields/validation: `logType`: `String` (@Pattern(regexp = "FEEDING\|SLEEP\|DIAPER\|FEVER\|VOMITING\|MEDICINE\|SYMPTOM", message = "log_type must be one of: FEEDING, SLEEP, DIAPER, FEVER, VOMITING, MEDICINE, SYMPTOM")); `startedAt`: `Instant` (no field annotation in current DTO); `endedAt`: `Instant` (no field annotation in current DTO); `quantity`: `BigDecimal` (@DecimalMin(value = "0.00", message = "quantity must be non-negative")); `unit`: `String` (@Size(max = 20, message = "unit must not exceed 20 characters")); `note`: `String` (@Size(max = 2000, message = "note must not exceed 2000 characters")); response: `ApiResponse<AddBabyDailyLogResponse>`; response payload fields: `babyLogId`: `UUID` (no field annotation in current DTO); `babyId`: `UUID` (no field annotation in current DTO); `logType`: `String` (no field annotation in current DTO); `startedAt`: `Instant` (no field annotation in current DTO); `endedAt`: `Instant` (no field annotation in current DTO); `quantity`: `BigDecimal` (no field annotation in current DTO); `unit`: `String` (no field annotation in current DTO); `note`: `String` (no field annotation in current DTO); `recordedBy`: `UUID` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO); explicit/documented statuses: `200, 201`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/carejourney/controller/BabyDailyLogController.java` |
| `API-03` | `DELETE /api/v1/babies/{babyId}/daily-logs/{logId}` | hasAnyRole('MOTHER', 'FAMILY') | Handler `deleteLog`; parameters: path `babyId`: `UUID`; path `logId`: `UUID`; principal `principal`: `Principal`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ApiResponse<Void>`; response payload fields: Not applicable or unresolved from the handler import; explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/carejourney/controller/BabyDailyLogController.java` |
| `API-04` | `GET /api/v1/babies/{babyId}/daily-logs/{logId}` | isAuthenticated() | Handler `getDailyLogDetail`; parameters: path `babyId`: `UUID`; path `logId`: `UUID`; principal `principal`: `Principal`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ApiResponse<BabyDailyLogResponse>`; response payload fields: `babyLogId`: `UUID` (no field annotation in current DTO); `babyId`: `UUID` (no field annotation in current DTO); `logType`: `String` (no field annotation in current DTO); `startedAt`: `Instant` (no field annotation in current DTO); `endedAt`: `Instant` (no field annotation in current DTO); `quantity`: `BigDecimal` (no field annotation in current DTO); `unit`: `String` (no field annotation in current DTO); `note`: `String` (no field annotation in current DTO); `recordedBy`: `UUID` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO); `updatedAt`: `Instant` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/carejourney/controller/BabyDailyLogController.java` |
| `API-05` | `PUT /api/v1/babies/{babyId}/daily-logs/{logId}` | hasAnyRole('MOTHER', 'FAMILY') | Handler `updateLog`; parameters: path `babyId`: `UUID`; path `logId`: `UUID`; body `request`: `UpdateBabyDailyLogRequest`; principal `principal`: `Principal`; request body: `UpdateBabyDailyLogRequest`; request fields/validation: `startedAt`: `Instant` (no field annotation in current DTO); `endedAt`: `Instant` (no field annotation in current DTO); `quantity`: `BigDecimal` (@DecimalMin(value = "0", message = "quantity must be non-negative")); `unit`: `String` (@Size(max = 20, message = "unit must not exceed 20 characters")); `note`: `String` (@Size(max = 500, message = "note must not exceed 500 characters")); response: `ApiResponse<BabyDailyLogResponse>`; response payload fields: `babyLogId`: `UUID` (no field annotation in current DTO); `babyId`: `UUID` (no field annotation in current DTO); `logType`: `String` (no field annotation in current DTO); `startedAt`: `Instant` (no field annotation in current DTO); `endedAt`: `Instant` (no field annotation in current DTO); `quantity`: `BigDecimal` (no field annotation in current DTO); `unit`: `String` (no field annotation in current DTO); `note`: `String` (no field annotation in current DTO); `recordedBy`: `UUID` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO); `updatedAt`: `Instant` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/carejourney/controller/BabyDailyLogController.java` |

Method-level Spring handlers, authorization annotations, request DTO fields/validators, response payload fields, and explicit/documented statuses above were extracted from the cited current source. Service/advice-only application error codes are not claimed where the controller does not declare them.

### 9.1 Handler Contract — `GET /api/v1/babies/{babyId}/daily-logs`

| Item | Exact current contract |
| --- | --- |
| Handler | `getDailyLogs` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/carejourney/controller/BabyDailyLogController.java` |
| Authorization annotation / boundary | isAuthenticated() |
| Parameters | path `babyId`: `UUID`; principal `principal`: `Principal` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ApiResponse<List<BabyDailyLogResponse>>` |
| Response payload fields | `babyLogId`: `UUID` (no field annotation in current DTO); `babyId`: `UUID` (no field annotation in current DTO); `logType`: `String` (no field annotation in current DTO); `startedAt`: `Instant` (no field annotation in current DTO); `endedAt`: `Instant` (no field annotation in current DTO); `quantity`: `BigDecimal` (no field annotation in current DTO); `unit`: `String` (no field annotation in current DTO); `note`: `String` (no field annotation in current DTO); `recordedBy`: `UUID` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO); `updatedAt`: `Instant` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-001` / `UC-BC-03-TC-API-001` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.2 Handler Contract — `POST /api/v1/babies/{babyId}/daily-logs`

| Item | Exact current contract |
| --- | --- |
| Handler | `addDailyLog` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/carejourney/controller/BabyDailyLogController.java` |
| Authorization annotation / boundary | hasAnyRole('MOTHER', 'FAMILY') |
| Parameters | path `babyId`: `UUID`; body `request`: `AddBabyDailyLogRequest`; principal `principal`: `Principal` |
| Request body type | `AddBabyDailyLogRequest` |
| Request fields and validators | `logType`: `String` (@Pattern(regexp = "FEEDING\|SLEEP\|DIAPER\|FEVER\|VOMITING\|MEDICINE\|SYMPTOM", message = "log_type must be one of: FEEDING, SLEEP, DIAPER, FEVER, VOMITING, MEDICINE, SYMPTOM")); `startedAt`: `Instant` (no field annotation in current DTO); `endedAt`: `Instant` (no field annotation in current DTO); `quantity`: `BigDecimal` (@DecimalMin(value = "0.00", message = "quantity must be non-negative")); `unit`: `String` (@Size(max = 20, message = "unit must not exceed 20 characters")); `note`: `String` (@Size(max = 2000, message = "note must not exceed 2000 characters")) |
| Response type | `ApiResponse<AddBabyDailyLogResponse>` |
| Response payload fields | `babyLogId`: `UUID` (no field annotation in current DTO); `babyId`: `UUID` (no field annotation in current DTO); `logType`: `String` (no field annotation in current DTO); `startedAt`: `Instant` (no field annotation in current DTO); `endedAt`: `Instant` (no field annotation in current DTO); `quantity`: `BigDecimal` (no field annotation in current DTO); `unit`: `String` (no field annotation in current DTO); `note`: `String` (no field annotation in current DTO); `recordedBy`: `UUID` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO) |
| Explicit/documented statuses | `200, 201` |
| Positive test mapping | `COND-API-002` / `UC-BC-03-TC-API-002` |
| Negative test mapping | `COND-API-002-VAL` / `UC-BC-03-TC-API-002-VAL`; plus `COND-AUTH` for protected access |

### 9.3 Handler Contract — `DELETE /api/v1/babies/{babyId}/daily-logs/{logId}`

| Item | Exact current contract |
| --- | --- |
| Handler | `deleteLog` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/carejourney/controller/BabyDailyLogController.java` |
| Authorization annotation / boundary | hasAnyRole('MOTHER', 'FAMILY') |
| Parameters | path `babyId`: `UUID`; path `logId`: `UUID`; principal `principal`: `Principal` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ApiResponse<Void>` |
| Response payload fields | Not applicable or unresolved from the handler import |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-003` / `UC-BC-03-TC-API-003` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.4 Handler Contract — `GET /api/v1/babies/{babyId}/daily-logs/{logId}`

| Item | Exact current contract |
| --- | --- |
| Handler | `getDailyLogDetail` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/carejourney/controller/BabyDailyLogController.java` |
| Authorization annotation / boundary | isAuthenticated() |
| Parameters | path `babyId`: `UUID`; path `logId`: `UUID`; principal `principal`: `Principal` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ApiResponse<BabyDailyLogResponse>` |
| Response payload fields | `babyLogId`: `UUID` (no field annotation in current DTO); `babyId`: `UUID` (no field annotation in current DTO); `logType`: `String` (no field annotation in current DTO); `startedAt`: `Instant` (no field annotation in current DTO); `endedAt`: `Instant` (no field annotation in current DTO); `quantity`: `BigDecimal` (no field annotation in current DTO); `unit`: `String` (no field annotation in current DTO); `note`: `String` (no field annotation in current DTO); `recordedBy`: `UUID` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO); `updatedAt`: `Instant` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-004` / `UC-BC-03-TC-API-004` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.5 Handler Contract — `PUT /api/v1/babies/{babyId}/daily-logs/{logId}`

| Item | Exact current contract |
| --- | --- |
| Handler | `updateLog` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/carejourney/controller/BabyDailyLogController.java` |
| Authorization annotation / boundary | hasAnyRole('MOTHER', 'FAMILY') |
| Parameters | path `babyId`: `UUID`; path `logId`: `UUID`; body `request`: `UpdateBabyDailyLogRequest`; principal `principal`: `Principal` |
| Request body type | `UpdateBabyDailyLogRequest` |
| Request fields and validators | `startedAt`: `Instant` (no field annotation in current DTO); `endedAt`: `Instant` (no field annotation in current DTO); `quantity`: `BigDecimal` (@DecimalMin(value = "0", message = "quantity must be non-negative")); `unit`: `String` (@Size(max = 20, message = "unit must not exceed 20 characters")); `note`: `String` (@Size(max = 500, message = "note must not exceed 500 characters")) |
| Response type | `ApiResponse<BabyDailyLogResponse>` |
| Response payload fields | `babyLogId`: `UUID` (no field annotation in current DTO); `babyId`: `UUID` (no field annotation in current DTO); `logType`: `String` (no field annotation in current DTO); `startedAt`: `Instant` (no field annotation in current DTO); `endedAt`: `Instant` (no field annotation in current DTO); `quantity`: `BigDecimal` (no field annotation in current DTO); `unit`: `String` (no field annotation in current DTO); `note`: `String` (no field annotation in current DTO); `recordedBy`: `UUID` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO); `updatedAt`: `Instant` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-005` / `UC-BC-03-TC-API-005` |
| Negative test mapping | `COND-API-005-VAL` / `UC-BC-03-TC-API-005-VAL`; plus `COND-AUTH` for protected access |

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
| `VG-01` | Choose a supported log type and enter type-specific data. | `COND-01` | `UC-BC-03-TC-001` |
| `VG-02` | Persist and inspect the log detail. | `COND-02` | `UC-BC-03-TC-002` |
| `VG-03` | Edit or delete it when ownership/state permits. | `COND-03` | `UC-BC-03-TC-003` |
| `VG-AUTH` | Reject wrong authentication/role/ownership/membership/consent scope | `COND-AUTH` | `UC-BC-03-TC-SEC-001` |
| `VG-GAP` | Characterize each known gap without claiming a false completed path | `COND-GAP` | `UC-BC-03-TC-GAP-001` |

## 14. Verification Methods

Existing focused evidence:

- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/carejourney/BabyDailyLogServiceTest.java`

Exact supported commands derived from the audited test paths:

- `cd 05_Development/CareBridgeAPI && ./mvnw -Dtest=BabyDailyLogServiceTest test`

Record pass/fail/skip counts only after executing these commands on the exact revision.

## 15. Verification Samples

| Sample | Value |
| --- | --- |
| Primary contract | Daily-log endpoints under `/api/v1/babies/{babyId}/daily-logs/**` |
| Request | `GET /api/v1/babies/{babyId}/daily-logs` → `getDailyLogs`; `None` with Not applicable — no request body; authorization: isAuthenticated(). |
| Success response | `ApiResponse<List<BabyDailyLogResponse>>` with `babyLogId`: `UUID` (no field annotation in current DTO); `babyId`: `UUID` (no field annotation in current DTO); `logType`: `String` (no field annotation in current DTO); `startedAt`: `Instant` (no field annotation in current DTO); `endedAt`: `Instant` (no field annotation in current DTO); `quantity`: `BigDecimal` (no field annotation in current DTO); `unit`: `String` (no field annotation in current DTO); `note`: `String` (no field annotation in current DTO); `recordedBy`: `UUID` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO); `updatedAt`: `Instant` (no field annotation in current DTO); explicit/documented statuses `200`. |
| Negative sample | Use wrong role/owner/state and assert the exact mapped error without protected payload. |

## 16. Authorization Matrix

| Actor / role | Operation | Decision |
| --- | --- | --- |
| Mother / Authorized Caregiver | `GET /api/v1/babies/{babyId}/daily-logs` | isAuthenticated(); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/carejourney/controller/BabyDailyLogController.java` |
| Mother / Authorized Caregiver | `POST /api/v1/babies/{babyId}/daily-logs` | hasAnyRole('MOTHER', 'FAMILY'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/carejourney/controller/BabyDailyLogController.java` |
| Mother / Authorized Caregiver | `DELETE /api/v1/babies/{babyId}/daily-logs/{logId}` | hasAnyRole('MOTHER', 'FAMILY'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/carejourney/controller/BabyDailyLogController.java` |
| Mother / Authorized Caregiver | `GET /api/v1/babies/{babyId}/daily-logs/{logId}` | isAuthenticated(); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/carejourney/controller/BabyDailyLogController.java` |
| Mother / Authorized Caregiver | `PUT /api/v1/babies/{babyId}/daily-logs/{logId}` | hasAnyRole('MOTHER', 'FAMILY'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/carejourney/controller/BabyDailyLogController.java` |
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
