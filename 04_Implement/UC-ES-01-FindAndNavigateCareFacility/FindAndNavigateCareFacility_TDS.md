# ENGINEERING DOCUMENTATION STANDARD (EDS) v2.0

# TECHNICAL DESIGN SPECIFICATION — Find and Navigate to Care Facility

| Field | Value |
| --- | --- |
| Document ID | `UC-ES-01-TDS` |
| Version | `0.1` |
| Date | `2026-08-23` |
| Status | `Draft` |
| Function ID | `UC-ES-01` |
| Canonical Use Case | `UC-ES-01 — Find and Navigate to Care Facility` |
| Module / Bounded Context | `Emergency and Safety` |
| Primary Actor | `Mother / Family` |
| Platforms | `Mobile / Backend / Map Provider` |
| Priority | `High` |
| Data Classification | `Restricted emergency, live-location, consent, sensor, and family-alert data` |
| Compliance Scope | `PDPA explicit-purpose location/sensor processing, consent, least disclosure, safety-event integrity, and bounded retention` |
| Owner | `CareBridge Team` |
| Reviewer / Approver |  |
| Source Baseline | Current worktree on `2026-08-23`; SRS `UC-ES-01`; exact evidence in Section 1.4 |

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

- **Goal:** Find nearby or listed eligible care facilities, inspect one, calculate a route, and hand off to external navigation when supported.
- **Trigger:** The actor enters Mobile `/emergency/map`.
- **Outcome:** Calculate a route or hand off to supported navigation.
- **Current state:** `High` confidence from reachable code/test audit; no manifest-level exclusion is recorded.
- **Target state:** Preserve current code-backed behavior and resolve only explicitly evidenced limitations through approved implementation work.

### 1.2 Scope

**In scope**

- Mobile `/emergency/map`

- GET `/api/v1/map/facilities`
- GET `/api/v1/map/nearby-facilities`
- GET `/api/v1/map/facilities/{id}`
- POST `/api/v1/map/route`
- POST `/api/v1/map/emergency/handoff`

**Out of scope / limitations**

- Not applicable — no manifest-level exclusion is recorded. Scope is limited to the entry points and exact contracts listed above; unrelated handlers in the same module are excluded.

### 1.3 Preconditions and Postconditions

| Type | Condition |
| --- | --- |
| Precondition | Mother / Family is authenticated/authorized where the current contract requires it. |
| Precondition | Required ownership, membership, consent, resource state, and device/provider prerequisites pass current policies. |
| Postcondition | Calculate a route or hand off to supported navigation. |
| Postcondition | No side effect outside the feature-owned persistence/event/provider boundary occurs. |

### 1.4 Evidence Baseline

| Source ID | Type | Exact path | Revision | Authority |
| --- | --- | --- | --- | --- |
| `SRC-SRS-01` | Requirement | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-ES-01 | 2026-08-23 | Draft code-first requirement |
| `SRC-CODE-01` | Current code | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/map/controller/CareFacilityController.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-02` | Current code | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/emergency/controller/EmergencyMapHandoffController.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-03` | Current code | `05_Development/CareBridgeMobileApp/lib/features/emergency/screens/emergency_map_screen.dart` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-01` | Existing test | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/map/CareFacilityServiceImplTest.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-02` | Existing test | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/emergency/EmergencyMapHandoffServiceImplTest.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-03` | Existing test | `05_Development/CareBridgeMobileApp/test/features/emergency/emergency_map_screen_test.dart` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-04` | Existing test | `05_Development/CareBridgeMobileApp/test/features/emergency/trackasia_web_contract_test.dart` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-ERROR-01` | Current exception advice | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/common/exception/GlobalExceptionHandler.java` | Worktree `2026-08-23` | Current-state evidence |

### 1.5 Open Contradictions / Questions

- No material contradiction was found among the exact sources cited in Section 1.4. Behavior not evidenced by those sources is not approved or implied by this Draft.

## 2. Traceability Matrix

| Requirement | Behavior | Exact oracle source | Component | Test condition / case |
| --- | --- | --- | --- | --- |
| `UC-ES-01-FR-01` | Provide/confirm allowed location context. | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-ES-01 Normal Flow 1 | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/emergency/controller/EmergencyMapHandoffController.java` | `COND-01` / `UC-ES-01-TC-001` |
| `UC-ES-01-FR-02` | Load nearby/listed facilities and choose one. | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-ES-01 Normal Flow 2 | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/map/controller/CareFacilityController.java` | `COND-02` / `UC-ES-01-TC-002` |
| `UC-ES-01-FR-03` | Calculate a route or hand off to supported navigation. | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-ES-01 Normal Flow 3 | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/map/controller/CareFacilityController.java` | `COND-03` / `UC-ES-01-TC-003` |
| `BR-01` | Location use is consent/permission gated. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/emergency/controller/EmergencyMapHandoffController.java` | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/emergency/controller/EmergencyMapHandoffController.java` | `COND-BR-01` / `UC-ES-01-TC-BR-001` |
| `BR-02` | Map/provider failure must not report a false route or facility verification state. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/map/controller/CareFacilityController.java` | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/map/controller/CareFacilityController.java` | `COND-BR-02` / `UC-ES-01-TC-BR-002` |

## 3. Architecture Decision Records (ADR)

### ADR-UC-ES-01-01 — Use a distinct actor-goal boundary

| Item | Decision |
| --- | --- |
| Context | The retired 43-UC catalogue grouped multiple triggers, lifecycles, and permission boundaries, making implementation/test traceability generic. |
| Options | Keep the broad catalogue; split by screen; split by actor goal plus lifecycle/security boundary. |
| Decision | Use `UC-ES-01 — Find and Navigate to Care Facility` as the canonical boundary because its operations share the stated actor outcome and current implementation evidence. |
| Consequences | Related supporting screens/endpoints stay in one TDS; different lifecycle/actor outcomes have separate UCs. |
| Source / Status | SRS Section 3.1 and current code audit / Draft |

### ADR-UC-ES-01-02 — Preserve unknowns as Open

| Item | Decision |
| --- | --- |
| Context | Exact schema fields, SLA values, and some controller error codes are not fully evidenced by this manifest alone. |
| Decision | Do not invent them. Mark them `Open` and require the exact DTO/entity/migration/policy oracle before production-code changes. |
| Consequences | This document accurately characterizes current scope; unresolved design-changing items block implementation approval. |
| Source / Status | `create-specs` evidence discipline / Draft |

## 4. Non-Functional Requirements and SLA

| NFR | Target | Oracle Source | Verification |
| --- | --- | --- | --- |
| Authorization / isolation | All requests follow exact role/ownership/membership/consent policy in current code. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/map/controller/CareFacilityController.java` | Negative security cases in paired Test-Spec |
| Protected-data handling | No secrets or unnecessary health/location/identity/conversation/file payloads in logs, fixtures, screenshots, or audit detail. | Data classification header plus exact Section 9 request/response field inventories | Log/fixture review plus security tests |
| Availability / latency | Open — no approved feature-specific numeric SLA found. | Evidence needed: approved NFR/SLA source | Measure only; do not assert a fixed threshold |
| Accessibility | Open — confirm project-standard criteria for reachable UI surfaces. | Evidence needed: approved UX/accessibility standard | Applicable Web/Mobile UI checks |
| Retry / idempotency | Apply only semantics explicitly implemented by the owning service. | No explicit lock/version/idempotency marker is evidenced in the cited implementation sources; preserve observed behavior and add characterization before changing concurrency semantics | State/duplicate/concurrency cases where applicable |

## 5. Static Modeling

### 5.1 Component Responsibilities and Change Disposition

| Exact path | Disposition | Responsibility |
| --- | --- | --- |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/map/controller/CareFacilityController.java` | Reuse | Current implementation evidence for Find and Navigate to Care Facility; inspect the exact symbol before implementation changes. |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/emergency/controller/EmergencyMapHandoffController.java` | Reuse | Current implementation evidence for Find and Navigate to Care Facility; inspect the exact symbol before implementation changes. |
| `05_Development/CareBridgeMobileApp/lib/features/emergency/screens/emergency_map_screen.dart` | Reuse | Current implementation evidence for Find and Navigate to Care Facility; inspect the exact symbol before implementation changes. |

### 5.2 Current Component Diagram

```plantuml
@startuml
skinparam classAttributeIconSize 0
class CareFacilityController as "CareFacilityController.java"
class EmergencyMapHandoffController as "EmergencyMapHandoffController.java"
CareFacilityController --> EmergencyMapHandoffController
class emergency_map_screen as "emergency_map_screen.dart"
EmergencyMapHandoffController --> emergency_map_screen
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
Actor -> Client: Enter Find and Navigate to Care Facility
Client -> Domain: Provide/confirm allowed location context.
Domain --> Client: Result for step 1
Client -> Domain: Load nearby/listed facilities and choose one.
Domain --> Client: Result for step 2
Client -> Domain: Calculate a route or hand off to supported navigation.
Domain --> Client: Result for step 3
Client --> Actor: Render canonical outcome
@enduml
```

### 6.2 Alternative, Error, Retry, and Concurrency Flows

| Flow | Expected design behavior | Oracle |
| --- | --- | --- |
| Cancel before mutation | No unintended write or provider side effect. | SRS UC-ES-01 Alternative Flow |
| Invalid input/state | Reject with the current contract; keep canonical state unchanged. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/map/controller/CareFacilityController.java` |
| Wrong actor/scope | Fail closed without protected resource disclosure. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/map/controller/CareFacilityController.java` |
| Dependency failure | Use only the implemented bounded fallback/retry; never report false success. | Not applicable at this cited baseline — no external adapter/provider import is evidenced in the listed implementation sources |
| Duplicate/concurrent mutation | Apply only current lock/version/idempotency semantics. | No explicit lock/version/idempotency marker is evidenced in the cited implementation sources; preserve observed behavior and add characterization before changing concurrency semantics |

### 6.3 State Model and Invariants

```plantuml
@startuml
[*] --> Eligible
Eligible --> InProgress : Provide/confirm allowed location context.
InProgress --> Outcome : Calculate a route or hand off to supported navigation.
InProgress --> Rejected : validation / authorization / state failure
Outcome --> [*]
Rejected --> Eligible : actor corrects eligible input
@enduml
```

- Location use is consent/permission gated.
- Map/provider failure must not report a false route or facility verification state.

## 7. Domain Event Catalog

| Direction | Event | Producer / Consumer | Payload / delivery / idempotency |
| --- | --- | --- | --- |
| Publish / consume | Current event evidence | Not applicable at this cited baseline — no event type, publisher, or listener is evidenced in the listed implementation sources | Preserve only events evidenced by the cited source set; if later call-path inspection finds none, the flow remains synchronous. |

## 8. Interface Specification

### 8.1 User / Operator Interfaces

| # | Entry point | Actor | Contract |
| ---: | --- | --- | --- |
| 1 | Mobile `/emergency/map` | Mother / Family | Reachable current entry point |

### 8.2 Service / Repository Interfaces

| Interface evidence | Required responsibility |
| --- | --- |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/map/controller/CareFacilityController.java` | Support the mapped operations without broadening authorization or lifecycle semantics. |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/emergency/controller/EmergencyMapHandoffController.java` | Support the mapped operations without broadening authorization or lifecycle semantics. |
| `05_Development/CareBridgeMobileApp/lib/features/emergency/screens/emergency_map_screen.dart` | Support the mapped operations without broadening authorization or lifecycle semantics. |

## 9. API Specification

| ID | Method / path or grouped controller surface | Auth / role | Request / response / errors |
| --- | --- | --- | --- |
| `API-01` | `POST /api/v1/map/emergency/handoff` | hasAnyRole('MOTHER', 'FAMILY') | Handler `createHandoff`; parameters: principal `principal`: `Principal`; body `request`: `CreateEmergencyHandoffRequest`; request body: `CreateEmergencyHandoffRequest`; request fields/validation: `triageHandoffId`: `UUID` (@NotNull); `riskLevel`: `String` (@NotNull); `userLatitude`: `BigDecimal` (no field annotation in current DTO); `userLongitude`: `BigDecimal` (no field annotation in current DTO); `symptomSummary`: `String` (no field annotation in current DTO); `selectedFacilityId`: `UUID` (no field annotation in current DTO); response: `ResponseEntity<ApiResponse<EmergencyHandoffResponse>>`; response payload fields: `handoffId`: `UUID` (no field annotation in current DTO); `userId`: `UUID` (no field annotation in current DTO); `triageHandoffId`: `UUID` (no field annotation in current DTO); `riskLevel`: `String` (no field annotation in current DTO); `userLatitude`: `BigDecimal` (no field annotation in current DTO); `userLongitude`: `BigDecimal` (no field annotation in current DTO); `selectedFacilityId`: `UUID` (no field annotation in current DTO); `summary`: `String` (no field annotation in current DTO); `status`: `String` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO); `updatedAt`: `Instant` (no field annotation in current DTO); explicit/documented statuses: `201`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/emergency/controller/EmergencyMapHandoffController.java` |
| `API-02` | `GET /api/v1/map/facilities` | isAuthenticated() | Handler `getAllFacilities`; parameters: No explicit handler parameter; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<List<FacilityResponse>>>`; response payload fields: `facilityId`: `UUID` (no field annotation in current DTO); `name`: `String` (no field annotation in current DTO); `facilityType`: `String` (no field annotation in current DTO); `facilityLevel`: `String` (no field annotation in current DTO); `ownershipType`: `String` (no field annotation in current DTO); `address`: `String` (no field annotation in current DTO); `provinceId`: `String` (no field annotation in current DTO); `districtId`: `String` (no field annotation in current DTO); `latitude`: `BigDecimal` (no field annotation in current DTO); `longitude`: `BigDecimal` (no field annotation in current DTO); `phone`: `String` (no field annotation in current DTO); `openingHoursJson`: `String` (no field annotation in current DTO); `sourceType`: `String` (no field annotation in current DTO); `externalSourceId`: `String` (no field annotation in current DTO); `verificationStatus`: `String` (no field annotation in current DTO); `active`: `Boolean` (no field annotation in current DTO); `searchable`: `Boolean` (no field annotation in current DTO); `distanceMeters`: `Integer` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/map/controller/CareFacilityController.java` |
| `API-03` | `GET /api/v1/map/facilities/{id}` | isAuthenticated() | Handler `getFacility`; parameters: path `id`: `UUID`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<FacilityResponse>>`; response payload fields: `facilityId`: `UUID` (no field annotation in current DTO); `name`: `String` (no field annotation in current DTO); `facilityType`: `String` (no field annotation in current DTO); `facilityLevel`: `String` (no field annotation in current DTO); `ownershipType`: `String` (no field annotation in current DTO); `address`: `String` (no field annotation in current DTO); `provinceId`: `String` (no field annotation in current DTO); `districtId`: `String` (no field annotation in current DTO); `latitude`: `BigDecimal` (no field annotation in current DTO); `longitude`: `BigDecimal` (no field annotation in current DTO); `phone`: `String` (no field annotation in current DTO); `openingHoursJson`: `String` (no field annotation in current DTO); `sourceType`: `String` (no field annotation in current DTO); `externalSourceId`: `String` (no field annotation in current DTO); `verificationStatus`: `String` (no field annotation in current DTO); `active`: `Boolean` (no field annotation in current DTO); `searchable`: `Boolean` (no field annotation in current DTO); `distanceMeters`: `Integer` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/map/controller/CareFacilityController.java` |
| `API-04` | `GET /api/v1/map/nearby-facilities` | hasAnyRole('MOTHER', 'FAMILY') | Handler `searchNearby`; parameters: query `lat`: `BigDecimal`; query `lng`: `BigDecimal`; query `radiusMeters`: `Integer`; query `type`: `String`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<NearbyResponse>>`; response payload fields: `facilities`: `List<FacilityResponse>` (no field annotation in current DTO); `totalCount`: `Integer` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/map/controller/CareFacilityController.java` |
| `API-05` | `POST /api/v1/map/route` | hasAnyRole('MOTHER', 'FAMILY') | Handler `getRoute`; parameters: body `request`: `RouteRequest`; request body: `RouteRequest`; request fields/validation: `fromLat`: `BigDecimal` (@NotNull, @DecimalMin(value = "-90.0"), @DecimalMax(value = "90.0")); `fromLng`: `BigDecimal` (@NotNull, @DecimalMin(value = "-180.0"), @DecimalMax(value = "180.0")); `toLat`: `BigDecimal` (@NotNull, @DecimalMin(value = "-90.0"), @DecimalMax(value = "90.0")); `toLng`: `BigDecimal` (@NotNull, @DecimalMin(value = "-180.0"), @DecimalMax(value = "180.0")); `transportMode`: `String` (no field annotation in current DTO); response: `ResponseEntity<ApiResponse<RouteResponse>>`; response payload fields: `distanceMeters`: `BigDecimal` (no field annotation in current DTO); `etaMinutes`: `Integer` (no field annotation in current DTO); `durationSeconds`: `Integer` (no field annotation in current DTO); `encodedPolyline`: `String` (no field annotation in current DTO); `points`: `List<RoutePoint>` (no field annotation in current DTO); `steps`: `List<RouteStep>` (no field annotation in current DTO); `transportMode`: `String` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/map/controller/CareFacilityController.java` |

Method-level Spring handlers, authorization annotations, request DTO fields/validators, response payload fields, and explicit/documented statuses above were extracted from the cited current source. Service/advice-only application error codes are not claimed where the controller does not declare them.

### 9.1 Handler Contract — `POST /api/v1/map/emergency/handoff`

| Item | Exact current contract |
| --- | --- |
| Handler | `createHandoff` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/emergency/controller/EmergencyMapHandoffController.java` |
| Authorization annotation / boundary | hasAnyRole('MOTHER', 'FAMILY') |
| Parameters | principal `principal`: `Principal`; body `request`: `CreateEmergencyHandoffRequest` |
| Request body type | `CreateEmergencyHandoffRequest` |
| Request fields and validators | `triageHandoffId`: `UUID` (@NotNull); `riskLevel`: `String` (@NotNull); `userLatitude`: `BigDecimal` (no field annotation in current DTO); `userLongitude`: `BigDecimal` (no field annotation in current DTO); `symptomSummary`: `String` (no field annotation in current DTO); `selectedFacilityId`: `UUID` (no field annotation in current DTO) |
| Response type | `ResponseEntity<ApiResponse<EmergencyHandoffResponse>>` |
| Response payload fields | `handoffId`: `UUID` (no field annotation in current DTO); `userId`: `UUID` (no field annotation in current DTO); `triageHandoffId`: `UUID` (no field annotation in current DTO); `riskLevel`: `String` (no field annotation in current DTO); `userLatitude`: `BigDecimal` (no field annotation in current DTO); `userLongitude`: `BigDecimal` (no field annotation in current DTO); `selectedFacilityId`: `UUID` (no field annotation in current DTO); `summary`: `String` (no field annotation in current DTO); `status`: `String` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO); `updatedAt`: `Instant` (no field annotation in current DTO) |
| Explicit/documented statuses | `201` |
| Positive test mapping | `COND-API-001` / `UC-ES-01-TC-API-001` |
| Negative test mapping | `COND-API-001-VAL` / `UC-ES-01-TC-API-001-VAL`; plus `COND-AUTH` for protected access |

### 9.2 Handler Contract — `GET /api/v1/map/facilities`

| Item | Exact current contract |
| --- | --- |
| Handler | `getAllFacilities` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/map/controller/CareFacilityController.java` |
| Authorization annotation / boundary | isAuthenticated() |
| Parameters | No explicit handler parameter |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<List<FacilityResponse>>>` |
| Response payload fields | `facilityId`: `UUID` (no field annotation in current DTO); `name`: `String` (no field annotation in current DTO); `facilityType`: `String` (no field annotation in current DTO); `facilityLevel`: `String` (no field annotation in current DTO); `ownershipType`: `String` (no field annotation in current DTO); `address`: `String` (no field annotation in current DTO); `provinceId`: `String` (no field annotation in current DTO); `districtId`: `String` (no field annotation in current DTO); `latitude`: `BigDecimal` (no field annotation in current DTO); `longitude`: `BigDecimal` (no field annotation in current DTO); `phone`: `String` (no field annotation in current DTO); `openingHoursJson`: `String` (no field annotation in current DTO); `sourceType`: `String` (no field annotation in current DTO); `externalSourceId`: `String` (no field annotation in current DTO); `verificationStatus`: `String` (no field annotation in current DTO); `active`: `Boolean` (no field annotation in current DTO); `searchable`: `Boolean` (no field annotation in current DTO); `distanceMeters`: `Integer` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-002` / `UC-ES-01-TC-API-002` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.3 Handler Contract — `GET /api/v1/map/facilities/{id}`

| Item | Exact current contract |
| --- | --- |
| Handler | `getFacility` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/map/controller/CareFacilityController.java` |
| Authorization annotation / boundary | isAuthenticated() |
| Parameters | path `id`: `UUID` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<FacilityResponse>>` |
| Response payload fields | `facilityId`: `UUID` (no field annotation in current DTO); `name`: `String` (no field annotation in current DTO); `facilityType`: `String` (no field annotation in current DTO); `facilityLevel`: `String` (no field annotation in current DTO); `ownershipType`: `String` (no field annotation in current DTO); `address`: `String` (no field annotation in current DTO); `provinceId`: `String` (no field annotation in current DTO); `districtId`: `String` (no field annotation in current DTO); `latitude`: `BigDecimal` (no field annotation in current DTO); `longitude`: `BigDecimal` (no field annotation in current DTO); `phone`: `String` (no field annotation in current DTO); `openingHoursJson`: `String` (no field annotation in current DTO); `sourceType`: `String` (no field annotation in current DTO); `externalSourceId`: `String` (no field annotation in current DTO); `verificationStatus`: `String` (no field annotation in current DTO); `active`: `Boolean` (no field annotation in current DTO); `searchable`: `Boolean` (no field annotation in current DTO); `distanceMeters`: `Integer` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-003` / `UC-ES-01-TC-API-003` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.4 Handler Contract — `GET /api/v1/map/nearby-facilities`

| Item | Exact current contract |
| --- | --- |
| Handler | `searchNearby` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/map/controller/CareFacilityController.java` |
| Authorization annotation / boundary | hasAnyRole('MOTHER', 'FAMILY') |
| Parameters | query `lat`: `BigDecimal`; query `lng`: `BigDecimal`; query `radiusMeters`: `Integer`; query `type`: `String` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<NearbyResponse>>` |
| Response payload fields | `facilities`: `List<FacilityResponse>` (no field annotation in current DTO); `totalCount`: `Integer` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-004` / `UC-ES-01-TC-API-004` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.5 Handler Contract — `POST /api/v1/map/route`

| Item | Exact current contract |
| --- | --- |
| Handler | `getRoute` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/map/controller/CareFacilityController.java` |
| Authorization annotation / boundary | hasAnyRole('MOTHER', 'FAMILY') |
| Parameters | body `request`: `RouteRequest` |
| Request body type | `RouteRequest` |
| Request fields and validators | `fromLat`: `BigDecimal` (@NotNull, @DecimalMin(value = "-90.0"), @DecimalMax(value = "90.0")); `fromLng`: `BigDecimal` (@NotNull, @DecimalMin(value = "-180.0"), @DecimalMax(value = "180.0")); `toLat`: `BigDecimal` (@NotNull, @DecimalMin(value = "-90.0"), @DecimalMax(value = "90.0")); `toLng`: `BigDecimal` (@NotNull, @DecimalMin(value = "-180.0"), @DecimalMax(value = "180.0")); `transportMode`: `String` (no field annotation in current DTO) |
| Response type | `ResponseEntity<ApiResponse<RouteResponse>>` |
| Response payload fields | `distanceMeters`: `BigDecimal` (no field annotation in current DTO); `etaMinutes`: `Integer` (no field annotation in current DTO); `durationSeconds`: `Integer` (no field annotation in current DTO); `encodedPolyline`: `String` (no field annotation in current DTO); `points`: `List<RoutePoint>` (no field annotation in current DTO); `steps`: `List<RouteStep>` (no field annotation in current DTO); `transportMode`: `String` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-005` / `UC-ES-01-TC-API-005` |
| Negative test mapping | `COND-API-005-VAL` / `UC-ES-01-TC-API-005-VAL`; plus `COND-AUTH` for protected access |

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
| `VG-01` | Provide/confirm allowed location context. | `COND-01` | `UC-ES-01-TC-001` |
| `VG-02` | Load nearby/listed facilities and choose one. | `COND-02` | `UC-ES-01-TC-002` |
| `VG-03` | Calculate a route or hand off to supported navigation. | `COND-03` | `UC-ES-01-TC-003` |
| `VG-AUTH` | Reject wrong authentication/role/ownership/membership/consent scope | `COND-AUTH` | `UC-ES-01-TC-SEC-001` |
| `VG-GAP` | Characterize each known gap without claiming a false completed path | `COND-GAP` | `UC-ES-01-TC-GAP-001` |

## 14. Verification Methods

Existing focused evidence:

- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/map/CareFacilityServiceImplTest.java`
- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/emergency/EmergencyMapHandoffServiceImplTest.java`
- `05_Development/CareBridgeMobileApp/test/features/emergency/emergency_map_screen_test.dart`
- `05_Development/CareBridgeMobileApp/test/features/emergency/trackasia_web_contract_test.dart`

Exact supported commands derived from the audited test paths:

- `cd 05_Development/CareBridgeAPI && ./mvnw -Dtest=CareFacilityServiceImplTest test`
- `cd 05_Development/CareBridgeAPI && ./mvnw -Dtest=EmergencyMapHandoffServiceImplTest test`
- `cd 05_Development/CareBridgeMobileApp && flutter test test/features/emergency/emergency_map_screen_test.dart`
- `cd 05_Development/CareBridgeMobileApp && flutter test test/features/emergency/trackasia_web_contract_test.dart`

Record pass/fail/skip counts only after executing these commands on the exact revision.

## 15. Verification Samples

| Sample | Value |
| --- | --- |
| Primary contract | GET `/api/v1/map/facilities` |
| Request | `POST /api/v1/map/emergency/handoff` → `createHandoff`; `CreateEmergencyHandoffRequest` with `triageHandoffId`: `UUID` (@NotNull); `riskLevel`: `String` (@NotNull); `userLatitude`: `BigDecimal` (no field annotation in current DTO); `userLongitude`: `BigDecimal` (no field annotation in current DTO); `symptomSummary`: `String` (no field annotation in current DTO); `selectedFacilityId`: `UUID` (no field annotation in current DTO); authorization: hasAnyRole('MOTHER', 'FAMILY'). |
| Success response | `ResponseEntity<ApiResponse<EmergencyHandoffResponse>>` with `handoffId`: `UUID` (no field annotation in current DTO); `userId`: `UUID` (no field annotation in current DTO); `triageHandoffId`: `UUID` (no field annotation in current DTO); `riskLevel`: `String` (no field annotation in current DTO); `userLatitude`: `BigDecimal` (no field annotation in current DTO); `userLongitude`: `BigDecimal` (no field annotation in current DTO); `selectedFacilityId`: `UUID` (no field annotation in current DTO); `summary`: `String` (no field annotation in current DTO); `status`: `String` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO); `updatedAt`: `Instant` (no field annotation in current DTO); explicit/documented statuses `201`. |
| Negative sample | Use wrong role/owner/state and assert the exact mapped error without protected payload. |

## 16. Authorization Matrix

| Actor / role | Operation | Decision |
| --- | --- | --- |
| Mother / Family | `POST /api/v1/map/emergency/handoff` | hasAnyRole('MOTHER', 'FAMILY'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/emergency/controller/EmergencyMapHandoffController.java` |
| Mother / Family | `GET /api/v1/map/facilities` | isAuthenticated(); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/map/controller/CareFacilityController.java` |
| Mother / Family | `GET /api/v1/map/facilities/{id}` | isAuthenticated(); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/map/controller/CareFacilityController.java` |
| Mother / Family | `GET /api/v1/map/nearby-facilities` | hasAnyRole('MOTHER', 'FAMILY'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/map/controller/CareFacilityController.java` |
| Mother / Family | `POST /api/v1/map/route` | hasAnyRole('MOTHER', 'FAMILY'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/map/controller/CareFacilityController.java` |
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
