# ENGINEERING DOCUMENTATION STANDARD (EDS) v2.0

# TECHNICAL DESIGN SPECIFICATION — Start and Resolve Emergency Session

| Field | Value |
| --- | --- |
| Document ID | `UC-ES-02-TDS` |
| Version | `0.1` |
| Date | `2026-08-23` |
| Status | `Draft` |
| Function ID | `UC-ES-02` |
| Canonical Use Case | `UC-ES-02 — Start and Resolve Emergency Session` |
| Module / Bounded Context | `Emergency and Safety` |
| Primary Actor | `Mother` |
| Platforms | `Mobile / Backend / Notification / Location` |
| Priority | `High` |
| Data Classification | `Restricted emergency, live-location, consent, sensor, and family-alert data` |
| Compliance Scope | `PDPA explicit-purpose location/sensor processing, consent, least disclosure, safety-event integrity, and bounded retention` |
| Owner | `CareBridge Team` |
| Reviewer / Approver |  |
| Source Baseline | Current worktree on `2026-08-23`; SRS `UC-ES-02`; exact evidence in Section 1.4 |

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

- **Goal:** Open an emergency session, share permitted location/alert context with family, view the active session, and resolve it when the incident ends.
- **Trigger:** The actor enters Mobile `/emergency/map`.
- **Outcome:** Resolve the owned active session when safe.
- **Current state:** `High` confidence from reachable code/test audit; no manifest-level exclusion is recorded.
- **Target state:** Preserve current code-backed behavior and resolve only explicitly evidenced limitations through approved implementation work.

### 1.2 Scope

**In scope**

- Mobile `/emergency/map`
- Mobile `/emergency/alert/:sessionId`

- POST `/api/v1/emergency/sessions`
- GET `/api/v1/emergency/sessions/active`
- PATCH `/api/v1/emergency/sessions/{id}/resolve`
- POST `/api/v1/emergency/location-shares`

**Out of scope / limitations**

- Not applicable — no manifest-level exclusion is recorded. Scope is limited to the entry points and exact contracts listed above; unrelated handlers in the same module are excluded.

### 1.3 Preconditions and Postconditions

| Type | Condition |
| --- | --- |
| Precondition | Mother is authenticated/authorized where the current contract requires it. |
| Precondition | Required ownership, membership, consent, resource state, and device/provider prerequisites pass current policies. |
| Postcondition | Resolve the owned active session when safe. |
| Postcondition | No side effect outside the feature-owned persistence/event/provider boundary occurs. |

### 1.4 Evidence Baseline

| Source ID | Type | Exact path | Revision | Authority |
| --- | --- | --- | --- | --- |
| `SRC-SRS-01` | Requirement | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-ES-02 | 2026-08-23 | Draft code-first requirement |
| `SRC-CODE-01` | Current code | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/emergency/controller/EmergencyController.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-02` | Current code | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/emergency/controller/FamilyLocationShareController.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-03` | Current code | `05_Development/CareBridgeMobileApp/lib/features/emergency/screens/emergency_alert_detail_screen.dart` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-01` | Existing test | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/emergency/EmergencyControllerTest.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-02` | Existing test | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/emergency/EmergencyServiceTest.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-03` | Existing test | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/emergency/EmergencySessionOpenedHandlerTest.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-04` | Existing test | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/emergency/FamilyLocationShareServiceTest.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-ERROR-01` | Current exception advice | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/common/exception/GlobalExceptionHandler.java` | Worktree `2026-08-23` | Current-state evidence |

### 1.5 Open Contradictions / Questions

- No material contradiction was found among the exact sources cited in Section 1.4. Behavior not evidenced by those sources is not approved or implied by this Draft.

## 2. Traceability Matrix

| Requirement | Behavior | Exact oracle source | Component | Test condition / case |
| --- | --- | --- | --- | --- |
| `UC-ES-02-FR-01` | Confirm/start an emergency session. | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-ES-02 Normal Flow 1 | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/emergency/controller/FamilyLocationShareController.java` | `COND-01` / `UC-ES-02-TC-001` |
| `UC-ES-02-FR-02` | Publish permitted alert/location context and show the active session. | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-ES-02 Normal Flow 2 | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/emergency/controller/EmergencyController.java` | `COND-02` / `UC-ES-02-TC-002` |
| `UC-ES-02-FR-03` | Resolve the owned active session when safe. | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-ES-02 Normal Flow 3 | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/emergency/controller/EmergencyController.java` | `COND-03` / `UC-ES-02-TC-003` |
| `BR-01` | Emergency session state is canonical; delivery retries cannot create duplicate active incidents. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/emergency/controller/EmergencyController.java` | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/emergency/controller/EmergencyController.java` | `COND-BR-01` / `UC-ES-02-TC-BR-001` |
| `BR-02` | Location sharing is consent gated and is a subflow, not a separate UC. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/emergency/controller/FamilyLocationShareController.java` | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/emergency/controller/FamilyLocationShareController.java` | `COND-BR-02` / `UC-ES-02-TC-BR-002` |

## 3. Architecture Decision Records (ADR)

### ADR-UC-ES-02-01 — Use a distinct actor-goal boundary

| Item | Decision |
| --- | --- |
| Context | The retired 43-UC catalogue grouped multiple triggers, lifecycles, and permission boundaries, making implementation/test traceability generic. |
| Options | Keep the broad catalogue; split by screen; split by actor goal plus lifecycle/security boundary. |
| Decision | Use `UC-ES-02 — Start and Resolve Emergency Session` as the canonical boundary because its operations share the stated actor outcome and current implementation evidence. |
| Consequences | Related supporting screens/endpoints stay in one TDS; different lifecycle/actor outcomes have separate UCs. |
| Source / Status | SRS Section 3.1 and current code audit / Draft |

### ADR-UC-ES-02-02 — Preserve unknowns as Open

| Item | Decision |
| --- | --- |
| Context | Exact schema fields, SLA values, and some controller error codes are not fully evidenced by this manifest alone. |
| Decision | Do not invent them. Mark them `Open` and require the exact DTO/entity/migration/policy oracle before production-code changes. |
| Consequences | This document accurately characterizes current scope; unresolved design-changing items block implementation approval. |
| Source / Status | `create-specs` evidence discipline / Draft |

## 4. Non-Functional Requirements and SLA

| NFR | Target | Oracle Source | Verification |
| --- | --- | --- | --- |
| Authorization / isolation | All requests follow exact role/ownership/membership/consent policy in current code. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/emergency/controller/EmergencyController.java` | Negative security cases in paired Test-Spec |
| Protected-data handling | No secrets or unnecessary health/location/identity/conversation/file payloads in logs, fixtures, screenshots, or audit detail. | Data classification header plus exact Section 9 request/response field inventories | Log/fixture review plus security tests |
| Availability / latency | Open — no approved feature-specific numeric SLA found. | Evidence needed: approved NFR/SLA source | Measure only; do not assert a fixed threshold |
| Accessibility | Open — confirm project-standard criteria for reachable UI surfaces. | Evidence needed: approved UX/accessibility standard | Applicable Web/Mobile UI checks |
| Retry / idempotency | Apply only semantics explicitly implemented by the owning service. | No explicit lock/version/idempotency marker is evidenced in the cited implementation sources; preserve observed behavior and add characterization before changing concurrency semantics | State/duplicate/concurrency cases where applicable |

## 5. Static Modeling

### 5.1 Component Responsibilities and Change Disposition

| Exact path | Disposition | Responsibility |
| --- | --- | --- |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/emergency/controller/EmergencyController.java` | Reuse | Current implementation evidence for Start and Resolve Emergency Session; inspect the exact symbol before implementation changes. |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/emergency/controller/FamilyLocationShareController.java` | Reuse | Current implementation evidence for Start and Resolve Emergency Session; inspect the exact symbol before implementation changes. |
| `05_Development/CareBridgeMobileApp/lib/features/emergency/screens/emergency_alert_detail_screen.dart` | Reuse | Current implementation evidence for Start and Resolve Emergency Session; inspect the exact symbol before implementation changes. |

### 5.2 Current Component Diagram

```plantuml
@startuml
skinparam classAttributeIconSize 0
class EmergencyController as "EmergencyController.java"
class FamilyLocationShareController as "FamilyLocationShareController.java"
EmergencyController --> FamilyLocationShareController
class emergency_alert_detail_screen as "emergency_alert_detail_screen.dart"
FamilyLocationShareController --> emergency_alert_detail_screen
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
Actor -> Client: Enter Start and Resolve Emergency Session
Client -> Domain: Confirm/start an emergency session.
Domain --> Client: Result for step 1
Client -> Domain: Publish permitted alert/location context and show the active session.
Domain --> Client: Result for step 2
Client -> Domain: Resolve the owned active session when safe.
Domain --> Client: Result for step 3
Client --> Actor: Render canonical outcome
@enduml
```

### 6.2 Alternative, Error, Retry, and Concurrency Flows

| Flow | Expected design behavior | Oracle |
| --- | --- | --- |
| Cancel before mutation | No unintended write or provider side effect. | SRS UC-ES-02 Alternative Flow |
| Invalid input/state | Reject with the current contract; keep canonical state unchanged. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/emergency/controller/EmergencyController.java` |
| Wrong actor/scope | Fail closed without protected resource disclosure. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/emergency/controller/EmergencyController.java` |
| Dependency failure | Use only the implemented bounded fallback/retry; never report false success. | Not applicable at this cited baseline — no external adapter/provider import is evidenced in the listed implementation sources |
| Duplicate/concurrent mutation | Apply only current lock/version/idempotency semantics. | No explicit lock/version/idempotency marker is evidenced in the cited implementation sources; preserve observed behavior and add characterization before changing concurrency semantics |

### 6.3 State Model and Invariants

```plantuml
@startuml
[*] --> Eligible
Eligible --> InProgress : Confirm/start an emergency session.
InProgress --> Outcome : Resolve the owned active session when safe.
InProgress --> Rejected : validation / authorization / state failure
Outcome --> [*]
Rejected --> Eligible : actor corrects eligible input
@enduml
```

- Emergency session state is canonical; delivery retries cannot create duplicate active incidents.
- Location sharing is consent gated and is a subflow, not a separate UC.

## 7. Domain Event Catalog

| Direction | Event | Producer / Consumer | Payload / delivery / idempotency |
| --- | --- | --- | --- |
| Publish / consume | Current event evidence | Not applicable at this cited baseline — no event type, publisher, or listener is evidenced in the listed implementation sources | Preserve only events evidenced by the cited source set; if later call-path inspection finds none, the flow remains synchronous. |

## 8. Interface Specification

### 8.1 User / Operator Interfaces

| # | Entry point | Actor | Contract |
| ---: | --- | --- | --- |
| 1 | Mobile `/emergency/map` | Mother | Reachable current entry point |
| 2 | Mobile `/emergency/alert/:sessionId` | Mother | Reachable current entry point |

### 8.2 Service / Repository Interfaces

| Interface evidence | Required responsibility |
| --- | --- |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/emergency/controller/EmergencyController.java` | Support the mapped operations without broadening authorization or lifecycle semantics. |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/emergency/controller/FamilyLocationShareController.java` | Support the mapped operations without broadening authorization or lifecycle semantics. |
| `05_Development/CareBridgeMobileApp/lib/features/emergency/screens/emergency_alert_detail_screen.dart` | Support the mapped operations without broadening authorization or lifecycle semantics. |

## 9. API Specification

| ID | Method / path or grouped controller surface | Auth / role | Request / response / errors |
| --- | --- | --- | --- |
| `API-01` | `POST /api/v1/emergency/location-shares` | hasRole('MOTHER') | Handler `share`; parameters: body `request`: `ShareLocationRequest`; principal `principal`: `Principal`; request body: `ShareLocationRequest`; request fields/validation: `latitude`: `BigDecimal` (@NotNull, @DecimalMin("-90.0"), @DecimalMax("90.0")); `longitude`: `BigDecimal` (@NotNull, @DecimalMin("-180.0"), @DecimalMax("180.0")); response: `ResponseEntity<ApiResponse<LocationShareResponse>>`; response payload fields: `shareId`: `UUID` (no field annotation in current DTO); `recipientCount`: `int` (no field annotation in current DTO); `pushDeliveredCount`: `int` (no field annotation in current DTO); `sharedAt`: `Instant` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/emergency/controller/FamilyLocationShareController.java` |
| `API-02` | `POST /api/v1/emergency/sessions` | hasAnyRole('MOTHER', 'FAMILY') | Handler `openFlow`; parameters: body `request`: `OpenEmergencyRequest`; principal `principal`: `Principal`; request body: `OpenEmergencyRequest`; request fields/validation: `triggerSource`: `String` (@NotBlank(message = "triggerSource must not be blank")); `userLatitude`: `BigDecimal` (no field annotation in current DTO); `userLongitude`: `BigDecimal` (no field annotation in current DTO); response: `ResponseEntity<ApiResponse<EmergencySessionResponse>>`; response payload fields: `sessionId`: `UUID` (no field annotation in current DTO); `userId`: `UUID` (no field annotation in current DTO); `status`: `String` (no field annotation in current DTO); `triggerSource`: `String` (no field annotation in current DTO); `userLatitude`: `BigDecimal` (no field annotation in current DTO); `userLongitude`: `BigDecimal` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO); `resolvedAt`: `Instant` (no field annotation in current DTO); explicit/documented statuses: `201`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/emergency/controller/EmergencyController.java` |
| `API-03` | `GET /api/v1/emergency/sessions/active` | hasAnyRole('MOTHER', 'FAMILY') | Handler `getActive`; parameters: principal `principal`: `Principal`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<EmergencySessionResponse>>`; response payload fields: `sessionId`: `UUID` (no field annotation in current DTO); `userId`: `UUID` (no field annotation in current DTO); `status`: `String` (no field annotation in current DTO); `triggerSource`: `String` (no field annotation in current DTO); `userLatitude`: `BigDecimal` (no field annotation in current DTO); `userLongitude`: `BigDecimal` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO); `resolvedAt`: `Instant` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/emergency/controller/EmergencyController.java` |
| `API-04` | `PATCH /api/v1/emergency/sessions/{id}/resolve` | hasAnyRole('MOTHER', 'FAMILY') | Handler `resolve`; parameters: path `id`: `UUID`; principal `principal`: `Principal`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<EmergencySessionResponse>>`; response payload fields: `sessionId`: `UUID` (no field annotation in current DTO); `userId`: `UUID` (no field annotation in current DTO); `status`: `String` (no field annotation in current DTO); `triggerSource`: `String` (no field annotation in current DTO); `userLatitude`: `BigDecimal` (no field annotation in current DTO); `userLongitude`: `BigDecimal` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO); `resolvedAt`: `Instant` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/emergency/controller/EmergencyController.java` |

Method-level Spring handlers, authorization annotations, request DTO fields/validators, response payload fields, and explicit/documented statuses above were extracted from the cited current source. Service/advice-only application error codes are not claimed where the controller does not declare them.

### 9.1 Handler Contract — `POST /api/v1/emergency/location-shares`

| Item | Exact current contract |
| --- | --- |
| Handler | `share` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/emergency/controller/FamilyLocationShareController.java` |
| Authorization annotation / boundary | hasRole('MOTHER') |
| Parameters | body `request`: `ShareLocationRequest`; principal `principal`: `Principal` |
| Request body type | `ShareLocationRequest` |
| Request fields and validators | `latitude`: `BigDecimal` (@NotNull, @DecimalMin("-90.0"), @DecimalMax("90.0")); `longitude`: `BigDecimal` (@NotNull, @DecimalMin("-180.0"), @DecimalMax("180.0")) |
| Response type | `ResponseEntity<ApiResponse<LocationShareResponse>>` |
| Response payload fields | `shareId`: `UUID` (no field annotation in current DTO); `recipientCount`: `int` (no field annotation in current DTO); `pushDeliveredCount`: `int` (no field annotation in current DTO); `sharedAt`: `Instant` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-001` / `UC-ES-02-TC-API-001` |
| Negative test mapping | `COND-API-001-VAL` / `UC-ES-02-TC-API-001-VAL`; plus `COND-AUTH` for protected access |

### 9.2 Handler Contract — `POST /api/v1/emergency/sessions`

| Item | Exact current contract |
| --- | --- |
| Handler | `openFlow` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/emergency/controller/EmergencyController.java` |
| Authorization annotation / boundary | hasAnyRole('MOTHER', 'FAMILY') |
| Parameters | body `request`: `OpenEmergencyRequest`; principal `principal`: `Principal` |
| Request body type | `OpenEmergencyRequest` |
| Request fields and validators | `triggerSource`: `String` (@NotBlank(message = "triggerSource must not be blank")); `userLatitude`: `BigDecimal` (no field annotation in current DTO); `userLongitude`: `BigDecimal` (no field annotation in current DTO) |
| Response type | `ResponseEntity<ApiResponse<EmergencySessionResponse>>` |
| Response payload fields | `sessionId`: `UUID` (no field annotation in current DTO); `userId`: `UUID` (no field annotation in current DTO); `status`: `String` (no field annotation in current DTO); `triggerSource`: `String` (no field annotation in current DTO); `userLatitude`: `BigDecimal` (no field annotation in current DTO); `userLongitude`: `BigDecimal` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO); `resolvedAt`: `Instant` (no field annotation in current DTO) |
| Explicit/documented statuses | `201` |
| Positive test mapping | `COND-API-002` / `UC-ES-02-TC-API-002` |
| Negative test mapping | `COND-API-002-VAL` / `UC-ES-02-TC-API-002-VAL`; plus `COND-AUTH` for protected access |

### 9.3 Handler Contract — `GET /api/v1/emergency/sessions/active`

| Item | Exact current contract |
| --- | --- |
| Handler | `getActive` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/emergency/controller/EmergencyController.java` |
| Authorization annotation / boundary | hasAnyRole('MOTHER', 'FAMILY') |
| Parameters | principal `principal`: `Principal` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<EmergencySessionResponse>>` |
| Response payload fields | `sessionId`: `UUID` (no field annotation in current DTO); `userId`: `UUID` (no field annotation in current DTO); `status`: `String` (no field annotation in current DTO); `triggerSource`: `String` (no field annotation in current DTO); `userLatitude`: `BigDecimal` (no field annotation in current DTO); `userLongitude`: `BigDecimal` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO); `resolvedAt`: `Instant` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-003` / `UC-ES-02-TC-API-003` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.4 Handler Contract — `PATCH /api/v1/emergency/sessions/{id}/resolve`

| Item | Exact current contract |
| --- | --- |
| Handler | `resolve` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/emergency/controller/EmergencyController.java` |
| Authorization annotation / boundary | hasAnyRole('MOTHER', 'FAMILY') |
| Parameters | path `id`: `UUID`; principal `principal`: `Principal` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<EmergencySessionResponse>>` |
| Response payload fields | `sessionId`: `UUID` (no field annotation in current DTO); `userId`: `UUID` (no field annotation in current DTO); `status`: `String` (no field annotation in current DTO); `triggerSource`: `String` (no field annotation in current DTO); `userLatitude`: `BigDecimal` (no field annotation in current DTO); `userLongitude`: `BigDecimal` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO); `resolvedAt`: `Instant` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-004` / `UC-ES-02-TC-API-004` |
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
| `VG-01` | Confirm/start an emergency session. | `COND-01` | `UC-ES-02-TC-001` |
| `VG-02` | Publish permitted alert/location context and show the active session. | `COND-02` | `UC-ES-02-TC-002` |
| `VG-03` | Resolve the owned active session when safe. | `COND-03` | `UC-ES-02-TC-003` |
| `VG-AUTH` | Reject wrong authentication/role/ownership/membership/consent scope | `COND-AUTH` | `UC-ES-02-TC-SEC-001` |
| `VG-GAP` | Characterize each known gap without claiming a false completed path | `COND-GAP` | `UC-ES-02-TC-GAP-001` |

## 14. Verification Methods

Existing focused evidence:

- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/emergency/EmergencyControllerTest.java`
- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/emergency/EmergencyServiceTest.java`
- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/emergency/EmergencySessionOpenedHandlerTest.java`
- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/emergency/FamilyLocationShareServiceTest.java`

Exact supported commands derived from the audited test paths:

- `cd 05_Development/CareBridgeAPI && ./mvnw -Dtest=EmergencyControllerTest test`
- `cd 05_Development/CareBridgeAPI && ./mvnw -Dtest=EmergencyServiceTest test`
- `cd 05_Development/CareBridgeAPI && ./mvnw -Dtest=EmergencySessionOpenedHandlerTest test`
- `cd 05_Development/CareBridgeAPI && ./mvnw -Dtest=FamilyLocationShareServiceTest test`

Record pass/fail/skip counts only after executing these commands on the exact revision.

## 15. Verification Samples

| Sample | Value |
| --- | --- |
| Primary contract | POST `/api/v1/emergency/sessions` |
| Request | `POST /api/v1/emergency/location-shares` → `share`; `ShareLocationRequest` with `latitude`: `BigDecimal` (@NotNull, @DecimalMin("-90.0"), @DecimalMax("90.0")); `longitude`: `BigDecimal` (@NotNull, @DecimalMin("-180.0"), @DecimalMax("180.0")); authorization: hasRole('MOTHER'). |
| Success response | `ResponseEntity<ApiResponse<LocationShareResponse>>` with `shareId`: `UUID` (no field annotation in current DTO); `recipientCount`: `int` (no field annotation in current DTO); `pushDeliveredCount`: `int` (no field annotation in current DTO); `sharedAt`: `Instant` (no field annotation in current DTO); explicit/documented statuses `200`. |
| Negative sample | Use wrong role/owner/state and assert the exact mapped error without protected payload. |

## 16. Authorization Matrix

| Actor / role | Operation | Decision |
| --- | --- | --- |
| Mother | `POST /api/v1/emergency/location-shares` | hasRole('MOTHER'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/emergency/controller/FamilyLocationShareController.java` |
| Mother | `POST /api/v1/emergency/sessions` | hasAnyRole('MOTHER', 'FAMILY'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/emergency/controller/EmergencyController.java` |
| Mother | `GET /api/v1/emergency/sessions/active` | hasAnyRole('MOTHER', 'FAMILY'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/emergency/controller/EmergencyController.java` |
| Mother | `PATCH /api/v1/emergency/sessions/{id}/resolve` | hasAnyRole('MOTHER', 'FAMILY'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/emergency/controller/EmergencyController.java` |
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
