# ENGINEERING DOCUMENTATION STANDARD (EDS) v2.0

# TECHNICAL DESIGN SPECIFICATION — Manage Personal Checklist and Roadmap

| Field | Value |
| --- | --- |
| Document ID | `UC-MH-16-TDS` |
| Version | `0.1` |
| Date | `2026-08-23` |
| Status | `Draft` |
| Function ID | `UC-MH-16` |
| Canonical Use Case | `UC-MH-16 — Manage Personal Checklist and Roadmap` |
| Module / Bounded Context | `Mother Journey and Health` |
| Primary Actor | `Mother / Authorized Family` |
| Platforms | `Mobile / Backend` |
| Priority | `Medium` |
| Data Classification | `Restricted maternal health, screening, journey, record, and attachment data; Confidential schedule/preferences` |
| Compliance Scope | `PDPA health-data minimization, consent/ownership enforcement, clinical disclaimer where applicable, and purpose-bound file access` |
| Owner | `CareBridge Team` |
| Reviewer / Approver |  |
| Source Baseline | Current worktree on `2026-08-23`; SRS `UC-MH-16`; exact evidence in Section 1.4 |

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

- **Goal:** View the lifecycle checklist roadmap, import optional templates, create personal items, and manage eligible checklist history/actions.
- **Trigger:** The actor enters Mobile `/checklists/roadmap` and embedded add/import/history surfaces.
- **Outcome:** Perform an allowed item action and review history.
- **Current state:** `High` confidence from reachable code/test audit; no manifest-level exclusion is recorded.
- **Target state:** Preserve current code-backed behavior and resolve only explicitly evidenced limitations through approved implementation work.

### 1.2 Scope

**In scope**

- Mobile `/checklists/roadmap` and embedded add/import/history surfaces

- Endpoints under `/api/v1/user-checklist-items/**`
- Current/history endpoints under `/api/v1/checklists/**`

**Out of scope / limitations**

- Not applicable — no manifest-level exclusion is recorded. Scope is limited to the entry points and exact contracts listed above; unrelated handlers in the same module are excluded.

### 1.3 Preconditions and Postconditions

| Type | Condition |
| --- | --- |
| Precondition | Mother / Authorized Family is authenticated/authorized where the current contract requires it. |
| Precondition | Required ownership, membership, consent, resource state, and device/provider prerequisites pass current policies. |
| Postcondition | Perform an allowed item action and review history. |
| Postcondition | No side effect outside the feature-owned persistence/event/provider boundary occurs. |

### 1.4 Evidence Baseline

| Source ID | Type | Exact path | Revision | Authority |
| --- | --- | --- | --- | --- |
| `SRC-SRS-01` | Requirement | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-MH-16 | 2026-08-23 | Draft code-first requirement |
| `SRC-CODE-01` | Current code | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/controller/UserChecklistItemController.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-02` | Current code | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/today/controller/CurrentChecklistController.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-03` | Current code | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/history/controller/ChecklistHistoryController.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-04` | Current code | `05_Development/CareBridgeMobileApp/lib/features/checklist/screens/checklist_roadmap_screen.dart` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-01` | Existing test | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/checklist/UserCreatedChecklistTaskServiceTest.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-02` | Existing test | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/checklist/history/ChecklistHistoryServiceTest.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-03` | Existing test | `05_Development/CareBridgeMobileApp/test/features/checklist/checklist_roadmap_screen_test.dart` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-ERROR-01` | Current exception advice | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/common/exception/GlobalExceptionHandler.java` | Worktree `2026-08-23` | Current-state evidence |

### 1.5 Open Contradictions / Questions

- No material contradiction was found among the exact sources cited in Section 1.4. Behavior not evidenced by those sources is not approved or implied by this Draft.

## 2. Traceability Matrix

| Requirement | Behavior | Exact oracle source | Component | Test condition / case |
| --- | --- | --- | --- | --- |
| `UC-MH-16-FR-01` | Load the current lifecycle roadmap/checklist. | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-MH-16 Normal Flow 1 | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/today/controller/CurrentChecklistController.java` | `COND-01` / `UC-MH-16-TC-001` |
| `UC-MH-16-FR-02` | Import an optional template or create a personal item. | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-MH-16 Normal Flow 2 | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/history/controller/ChecklistHistoryController.java` | `COND-02` / `UC-MH-16-TC-002` |
| `UC-MH-16-FR-03` | Perform an allowed item action and review history. | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-MH-16 Normal Flow 3 | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/today/controller/CurrentChecklistController.java` | `COND-03` / `UC-MH-16-TC-003` |
| `BR-01` | System-distributed and user-created items have different mutation rules. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/controller/UserChecklistItemController.java` | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/controller/UserChecklistItemController.java` | `COND-BR-01` / `UC-MH-16-TC-BR-001` |
| `BR-02` | Checklist operations remain scoped to the active lifecycle/authorized group. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/history/controller/ChecklistHistoryController.java` | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/history/controller/ChecklistHistoryController.java` | `COND-BR-02` / `UC-MH-16-TC-BR-002` |

## 3. Architecture Decision Records (ADR)

### ADR-UC-MH-16-01 — Use a distinct actor-goal boundary

| Item | Decision |
| --- | --- |
| Context | The retired 43-UC catalogue grouped multiple triggers, lifecycles, and permission boundaries, making implementation/test traceability generic. |
| Options | Keep the broad catalogue; split by screen; split by actor goal plus lifecycle/security boundary. |
| Decision | Use `UC-MH-16 — Manage Personal Checklist and Roadmap` as the canonical boundary because its operations share the stated actor outcome and current implementation evidence. |
| Consequences | Related supporting screens/endpoints stay in one TDS; different lifecycle/actor outcomes have separate UCs. |
| Source / Status | SRS Section 3.1 and current code audit / Draft |

### ADR-UC-MH-16-02 — Preserve unknowns as Open

| Item | Decision |
| --- | --- |
| Context | Exact schema fields, SLA values, and some controller error codes are not fully evidenced by this manifest alone. |
| Decision | Do not invent them. Mark them `Open` and require the exact DTO/entity/migration/policy oracle before production-code changes. |
| Consequences | This document accurately characterizes current scope; unresolved design-changing items block implementation approval. |
| Source / Status | `create-specs` evidence discipline / Draft |

## 4. Non-Functional Requirements and SLA

| NFR | Target | Oracle Source | Verification |
| --- | --- | --- | --- |
| Authorization / isolation | All requests follow exact role/ownership/membership/consent policy in current code. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/controller/UserChecklistItemController.java` | Negative security cases in paired Test-Spec |
| Protected-data handling | No secrets or unnecessary health/location/identity/conversation/file payloads in logs, fixtures, screenshots, or audit detail. | Data classification header plus exact Section 9 request/response field inventories | Log/fixture review plus security tests |
| Availability / latency | Open — no approved feature-specific numeric SLA found. | Evidence needed: approved NFR/SLA source | Measure only; do not assert a fixed threshold |
| Accessibility | Open — confirm project-standard criteria for reachable UI surfaces. | Evidence needed: approved UX/accessibility standard | Applicable Web/Mobile UI checks |
| Retry / idempotency | Apply only semantics explicitly implemented by the owning service. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/today/controller/CurrentChecklistController.java` | State/duplicate/concurrency cases where applicable |

## 5. Static Modeling

### 5.1 Component Responsibilities and Change Disposition

| Exact path | Disposition | Responsibility |
| --- | --- | --- |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/controller/UserChecklistItemController.java` | Reuse | Current implementation evidence for Manage Personal Checklist and Roadmap; inspect the exact symbol before implementation changes. |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/today/controller/CurrentChecklistController.java` | Reuse | Current implementation evidence for Manage Personal Checklist and Roadmap; inspect the exact symbol before implementation changes. |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/history/controller/ChecklistHistoryController.java` | Reuse | Current implementation evidence for Manage Personal Checklist and Roadmap; inspect the exact symbol before implementation changes. |
| `05_Development/CareBridgeMobileApp/lib/features/checklist/screens/checklist_roadmap_screen.dart` | Reuse | Current implementation evidence for Manage Personal Checklist and Roadmap; inspect the exact symbol before implementation changes. |

### 5.2 Current Component Diagram

```plantuml
@startuml
skinparam classAttributeIconSize 0
class UserChecklistItemController as "UserChecklistItemController.java"
class CurrentChecklistController as "CurrentChecklistController.java"
UserChecklistItemController --> CurrentChecklistController
class ChecklistHistoryController as "ChecklistHistoryController.java"
CurrentChecklistController --> ChecklistHistoryController
class checklist_roadmap_screen as "checklist_roadmap_screen.dart"
ChecklistHistoryController --> checklist_roadmap_screen
@enduml
```

### 5.3 Data / Schema / Migration Assessment

| Item | Assessment |
| --- | --- |
| Current stores/entities | No entity/repository import is present in the cited entry/source set; follow the exact handler-to-service call path before any schema change |
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
Actor -> Client: Enter Manage Personal Checklist and Roadmap
Client -> Domain: Load the current lifecycle roadmap/checklist.
Domain --> Client: Result for step 1
Client -> Domain: Import an optional template or create a personal item.
Domain --> Client: Result for step 2
Client -> Domain: Perform an allowed item action and review history.
Domain --> Client: Result for step 3
Client --> Actor: Render canonical outcome
@enduml
```

### 6.2 Alternative, Error, Retry, and Concurrency Flows

| Flow | Expected design behavior | Oracle |
| --- | --- | --- |
| Cancel before mutation | No unintended write or provider side effect. | SRS UC-MH-16 Alternative Flow |
| Invalid input/state | Reject with the current contract; keep canonical state unchanged. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/controller/UserChecklistItemController.java` |
| Wrong actor/scope | Fail closed without protected resource disclosure. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/controller/UserChecklistItemController.java` |
| Dependency failure | Use only the implemented bounded fallback/retry; never report false success. | Not applicable at this cited baseline — no external adapter/provider import is evidenced in the listed implementation sources |
| Duplicate/concurrent mutation | Apply only current lock/version/idempotency semantics. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/today/controller/CurrentChecklistController.java` |

### 6.3 State Model and Invariants

```plantuml
@startuml
[*] --> Eligible
Eligible --> InProgress : Load the current lifecycle roadmap/checklist.
InProgress --> Outcome : Perform an allowed item action and review history.
InProgress --> Rejected : validation / authorization / state failure
Outcome --> [*]
Rejected --> Eligible : actor corrects eligible input
@enduml
```

- System-distributed and user-created items have different mutation rules.
- Checklist operations remain scoped to the active lifecycle/authorized group.

## 7. Domain Event Catalog

| Direction | Event | Producer / Consumer | Payload / delivery / idempotency |
| --- | --- | --- | --- |
| Publish / consume | Current event evidence | Not applicable at this cited baseline — no event type, publisher, or listener is evidenced in the listed implementation sources | Preserve only events evidenced by the cited source set; if later call-path inspection finds none, the flow remains synchronous. |

## 8. Interface Specification

### 8.1 User / Operator Interfaces

| # | Entry point | Actor | Contract |
| ---: | --- | --- | --- |
| 1 | Mobile `/checklists/roadmap` and embedded add/import/history surfaces | Mother / Authorized Family | Reachable current entry point |

### 8.2 Service / Repository Interfaces

| Interface evidence | Required responsibility |
| --- | --- |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/controller/UserChecklistItemController.java` | Support the mapped operations without broadening authorization or lifecycle semantics. |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/today/controller/CurrentChecklistController.java` | Support the mapped operations without broadening authorization or lifecycle semantics. |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/history/controller/ChecklistHistoryController.java` | Support the mapped operations without broadening authorization or lifecycle semantics. |
| `05_Development/CareBridgeMobileApp/lib/features/checklist/screens/checklist_roadmap_screen.dart` | Support the mapped operations without broadening authorization or lifecycle semantics. |

## 9. API Specification

| ID | Method / path or grouped controller surface | Auth / role | Request / response / errors |
| --- | --- | --- | --- |
| `API-01` | `GET /api/v1/checklists/current/tasks` | hasAnyRole('MOTHER', 'FAMILY') | Handler `getCurrentTasks`; parameters: query `date`: `LocalDate`; context `timezone`: `String`; principal `principal`: `Principal`; request body: `None`; request fields/validation: Not applicable — no request body; response: `CurrentChecklistResponse`; response payload fields: `asOf`: `Instant` (no field annotation in current DTO); `zoneId`: `String` (no field annotation in current DTO); `horizonDays`: `int` (no field annotation in current DTO); `sections`: `CurrentChecklistSections` (no field annotation in current DTO); `counts`: `TodayTaskCounts` (no field annotation in current DTO); `correlationId`: `UUID` (no field annotation in current DTO); `sequence`: `TodaySequenceProjection` (no field annotation in current DTO); explicit/documented statuses: `Not explicit in handler syntax; inherited from framework/advice`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/today/controller/CurrentChecklistController.java` |
| `API-02` | `GET /api/v1/checklists/history` | hasRole('MOTHER') | Handler `listHistory`; parameters: query `page`: `int`; query `size`: `int`; query `targetSubject`: `ChecklistTargetSubject`; principal `principal`: `Principal`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<ChecklistHistoryPageResponse>>`; response payload fields: `items`: `List<ChecklistHistoryItemResponse>` (no field annotation in current DTO); `page`: `int` (no field annotation in current DTO); `size`: `int` (no field annotation in current DTO); `totalElements`: `long` (no field annotation in current DTO); `totalPages`: `int` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/history/controller/ChecklistHistoryController.java` |
| `API-03` | `GET /api/v1/checklists/journeys/{journeyId}/tasks` | hasAnyRole('MOTHER', 'FAMILY', 'EXPERT', 'ADMIN') | Handler `getJourneyTasks`; parameters: path `journeyId`: `UUID`; query `date`: `LocalDate`; context `timezone`: `String`; request body: `None`; request fields/validation: Not applicable — no request body; response: `CurrentChecklistResponse`; response payload fields: `asOf`: `Instant` (no field annotation in current DTO); `zoneId`: `String` (no field annotation in current DTO); `horizonDays`: `int` (no field annotation in current DTO); `sections`: `CurrentChecklistSections` (no field annotation in current DTO); `counts`: `TodayTaskCounts` (no field annotation in current DTO); `correlationId`: `UUID` (no field annotation in current DTO); `sequence`: `TodaySequenceProjection` (no field annotation in current DTO); explicit/documented statuses: `404`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/today/controller/CurrentChecklistController.java` |
| `API-04` | `POST /api/v1/checklists/tasks/{taskId}/actions` | hasAnyRole('MOTHER', 'FAMILY') | Handler `applyAction`; parameters: path `taskId`: `UUID`; body `request`: `TaskActionRequest`; principal `principal`: `Principal`; request body: `TaskActionRequest`; request fields/validation: `action`: `TaskAction` (@NotNull); `clientRequestId`: `UUID` (@NotNull); `reason`: `String` (@Pattern(regexp = "^[A-Z0-9_]{1,80}$")); response: `CurrentChecklistActionResponse`; response payload fields: `taskId`: `UUID` (no field annotation in current DTO); `instanceId`: `UUID` (no field annotation in current DTO); `action`: `TaskAction` (no field annotation in current DTO); `previousStatus`: `String` (no field annotation in current DTO); `status`: `String` (no field annotation in current DTO); `appliedAt`: `Instant` (no field annotation in current DTO); `idempotentReplay`: `boolean` (no field annotation in current DTO); `correlationId`: `UUID` (no field annotation in current DTO); explicit/documented statuses: `400`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/today/controller/CurrentChecklistController.java` |
| `API-05` | `GET /api/v1/checklists/users/{userId}/tasks` | hasAnyRole('EXPERT', 'ADMIN') | Handler `getUserTasks`; parameters: path `userId`: `UUID`; query `date`: `LocalDate`; context `timezone`: `String`; request body: `None`; request fields/validation: Not applicable — no request body; response: `CurrentChecklistResponse`; response payload fields: `asOf`: `Instant` (no field annotation in current DTO); `zoneId`: `String` (no field annotation in current DTO); `horizonDays`: `int` (no field annotation in current DTO); `sections`: `CurrentChecklistSections` (no field annotation in current DTO); `counts`: `TodayTaskCounts` (no field annotation in current DTO); `correlationId`: `UUID` (no field annotation in current DTO); `sequence`: `TodaySequenceProjection` (no field annotation in current DTO); explicit/documented statuses: `Not explicit in handler syntax; inherited from framework/advice`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/today/controller/CurrentChecklistController.java` |
| `API-06` | `GET /api/v1/user-checklist-items` | hasRole('MOTHER') | Handler `listItems`; parameters: query `journeyId`: `UUID`; query `babyId`: `UUID`; principal `principal`: `Principal`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<List<ChecklistItemResponse>>>`; response payload fields: Not applicable or unresolved from the handler import; explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/controller/UserChecklistItemController.java` |
| `API-07` | `POST /api/v1/user-checklist-items` | hasAnyRole('MOTHER', 'FAMILY') | Handler `addItem`; parameters: body `request`: `AddChecklistItemRequest`; context `contractVersionHeader`: `String`; principal `principal`: `Principal`; request body: `AddChecklistItemRequest`; request fields/validation: `journeyId`: `UUID` (no field annotation in current DTO); `babyId`: `UUID` (no field annotation in current DTO); `itemText`: `String` (@NotBlank(message = "CHECKLIST-001: itemText is required"), @Size(max = 500, message = "CHECKLIST-002: itemText exceeds 500 characters")); `category`: `ChecklistCategory` (no field annotation in current DTO); `itemOrder`: `Integer` (no field annotation in current DTO); `targetSubject`: `ChecklistTargetSubject` (no field annotation in current DTO); `clientTaskId`: `UUID` (no field annotation in current DTO); `careGroupId`: `UUID` (no field annotation in current DTO); response: `ResponseEntity<ApiResponse<ChecklistItemResponse>>`; response payload fields: Not applicable or unresolved from the handler import; explicit/documented statuses: `201`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/controller/UserChecklistItemController.java` |
| `API-08` | `POST /api/v1/user-checklist-items/from-template` | hasRole('MOTHER') | Handler `selfAssignFromTemplate`; parameters: body `request`: `SelfAssignChecklistTemplateRequest`; principal `principal`: `Principal`; request body: `SelfAssignChecklistTemplateRequest`; request fields/validation: `templateId`: `UUID` (@NotNull); `journeyId`: `UUID` (no field annotation in current DTO); `babyId`: `UUID` (no field annotation in current DTO); response: `ResponseEntity<ApiResponse<ChecklistDistributionResult>>`; response payload fields: `createdInstances`: `int` (no field annotation in current DTO); `existingInstances`: `int` (no field annotation in current DTO); `createdTasks`: `int` (no field annotation in current DTO); `existingTasks`: `int` (no field annotation in current DTO); `cancelledInstances`: `int` (no field annotation in current DTO); `deniedRecipients`: `int` (no field annotation in current DTO); `conflicts`: `int` (no field annotation in current DTO); `failures`: `int` (no field annotation in current DTO); explicit/documented statuses: `201`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/controller/UserChecklistItemController.java` |
| `API-09` | `POST /api/v1/user-checklist-items/import` | hasRole('MOTHER') | Handler `importFromTemplate`; parameters: body `request`: `ImportFromTemplateRequest`; principal `principal`: `Principal`; request body: `ImportFromTemplateRequest`; request fields/validation: `journeyId`: `UUID` (no field annotation in current DTO); `babyId`: `UUID` (no field annotation in current DTO); `templateItemIds`: `List<UUID>` (@NotNull(message = "CHECKLIST-001: templateItemIds is required"), @Size(min = 1, max = 50, message = "CHECKLIST-001: templateItemIds must contain 1 to 50 entries"), @NotNull(message = "CHECKLIST-001: templateItemIds cannot contain null")); response: `ResponseEntity<ApiResponse<List<ChecklistItemResponse>>>`; response payload fields: Not applicable or unresolved from the handler import; explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/controller/UserChecklistItemController.java` |
| `API-10` | `DELETE /api/v1/user-checklist-items/{itemId}` | hasRole('MOTHER') | Handler `deleteItem`; parameters: path `itemId`: `UUID`; principal `principal`: `Principal`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<Void>`; response payload fields: Not applicable or unresolved from the handler import; explicit/documented statuses: `204`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/controller/UserChecklistItemController.java` |
| `API-11` | `PUT /api/v1/user-checklist-items/{itemId}` | hasRole('MOTHER') | Handler `updateItem`; parameters: path `itemId`: `UUID`; body `request`: `UpdateChecklistItemRequest`; principal `principal`: `Principal`; request body: `UpdateChecklistItemRequest`; request fields/validation: `itemText`: `String` (@Size(max = 500, message = "CHECKLIST-002: itemText exceeds 500 characters")); `category`: `ChecklistCategory` (no field annotation in current DTO); `itemOrder`: `Integer` (no field annotation in current DTO); response: `ResponseEntity<ApiResponse<ChecklistItemResponse>>`; response payload fields: Not applicable or unresolved from the handler import; explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/controller/UserChecklistItemController.java` |
| `API-12` | `PATCH /api/v1/user-checklist-items/{itemId}/toggle` | hasRole('MOTHER') | Handler `toggleComplete`; parameters: path `itemId`: `UUID`; principal `principal`: `Principal`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<ChecklistItemResponse>>`; response payload fields: Not applicable or unresolved from the handler import; explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/controller/UserChecklistItemController.java` |

Method-level Spring handlers, authorization annotations, request DTO fields/validators, response payload fields, and explicit/documented statuses above were extracted from the cited current source. Service/advice-only application error codes are not claimed where the controller does not declare them.

### 9.1 Handler Contract — `GET /api/v1/checklists/current/tasks`

| Item | Exact current contract |
| --- | --- |
| Handler | `getCurrentTasks` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/today/controller/CurrentChecklistController.java` |
| Authorization annotation / boundary | hasAnyRole('MOTHER', 'FAMILY') |
| Parameters | query `date`: `LocalDate`; context `timezone`: `String`; principal `principal`: `Principal` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `CurrentChecklistResponse` |
| Response payload fields | `asOf`: `Instant` (no field annotation in current DTO); `zoneId`: `String` (no field annotation in current DTO); `horizonDays`: `int` (no field annotation in current DTO); `sections`: `CurrentChecklistSections` (no field annotation in current DTO); `counts`: `TodayTaskCounts` (no field annotation in current DTO); `correlationId`: `UUID` (no field annotation in current DTO); `sequence`: `TodaySequenceProjection` (no field annotation in current DTO) |
| Explicit/documented statuses | `Not explicit in handler syntax; inherited from framework/advice` |
| Positive test mapping | `COND-API-001` / `UC-MH-16-TC-API-001` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.2 Handler Contract — `GET /api/v1/checklists/history`

| Item | Exact current contract |
| --- | --- |
| Handler | `listHistory` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/history/controller/ChecklistHistoryController.java` |
| Authorization annotation / boundary | hasRole('MOTHER') |
| Parameters | query `page`: `int`; query `size`: `int`; query `targetSubject`: `ChecklistTargetSubject`; principal `principal`: `Principal` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<ChecklistHistoryPageResponse>>` |
| Response payload fields | `items`: `List<ChecklistHistoryItemResponse>` (no field annotation in current DTO); `page`: `int` (no field annotation in current DTO); `size`: `int` (no field annotation in current DTO); `totalElements`: `long` (no field annotation in current DTO); `totalPages`: `int` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-002` / `UC-MH-16-TC-API-002` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.3 Handler Contract — `GET /api/v1/checklists/journeys/{journeyId}/tasks`

| Item | Exact current contract |
| --- | --- |
| Handler | `getJourneyTasks` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/today/controller/CurrentChecklistController.java` |
| Authorization annotation / boundary | hasAnyRole('MOTHER', 'FAMILY', 'EXPERT', 'ADMIN') |
| Parameters | path `journeyId`: `UUID`; query `date`: `LocalDate`; context `timezone`: `String` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `CurrentChecklistResponse` |
| Response payload fields | `asOf`: `Instant` (no field annotation in current DTO); `zoneId`: `String` (no field annotation in current DTO); `horizonDays`: `int` (no field annotation in current DTO); `sections`: `CurrentChecklistSections` (no field annotation in current DTO); `counts`: `TodayTaskCounts` (no field annotation in current DTO); `correlationId`: `UUID` (no field annotation in current DTO); `sequence`: `TodaySequenceProjection` (no field annotation in current DTO) |
| Explicit/documented statuses | `404` |
| Positive test mapping | `COND-API-003` / `UC-MH-16-TC-API-003` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.4 Handler Contract — `POST /api/v1/checklists/tasks/{taskId}/actions`

| Item | Exact current contract |
| --- | --- |
| Handler | `applyAction` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/today/controller/CurrentChecklistController.java` |
| Authorization annotation / boundary | hasAnyRole('MOTHER', 'FAMILY') |
| Parameters | path `taskId`: `UUID`; body `request`: `TaskActionRequest`; principal `principal`: `Principal` |
| Request body type | `TaskActionRequest` |
| Request fields and validators | `action`: `TaskAction` (@NotNull); `clientRequestId`: `UUID` (@NotNull); `reason`: `String` (@Pattern(regexp = "^[A-Z0-9_]{1,80}$")) |
| Response type | `CurrentChecklistActionResponse` |
| Response payload fields | `taskId`: `UUID` (no field annotation in current DTO); `instanceId`: `UUID` (no field annotation in current DTO); `action`: `TaskAction` (no field annotation in current DTO); `previousStatus`: `String` (no field annotation in current DTO); `status`: `String` (no field annotation in current DTO); `appliedAt`: `Instant` (no field annotation in current DTO); `idempotentReplay`: `boolean` (no field annotation in current DTO); `correlationId`: `UUID` (no field annotation in current DTO) |
| Explicit/documented statuses | `400` |
| Positive test mapping | `COND-API-004` / `UC-MH-16-TC-API-004` |
| Negative test mapping | `COND-API-004-VAL` / `UC-MH-16-TC-API-004-VAL`; plus `COND-AUTH` for protected access |

### 9.5 Handler Contract — `GET /api/v1/checklists/users/{userId}/tasks`

| Item | Exact current contract |
| --- | --- |
| Handler | `getUserTasks` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/today/controller/CurrentChecklistController.java` |
| Authorization annotation / boundary | hasAnyRole('EXPERT', 'ADMIN') |
| Parameters | path `userId`: `UUID`; query `date`: `LocalDate`; context `timezone`: `String` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `CurrentChecklistResponse` |
| Response payload fields | `asOf`: `Instant` (no field annotation in current DTO); `zoneId`: `String` (no field annotation in current DTO); `horizonDays`: `int` (no field annotation in current DTO); `sections`: `CurrentChecklistSections` (no field annotation in current DTO); `counts`: `TodayTaskCounts` (no field annotation in current DTO); `correlationId`: `UUID` (no field annotation in current DTO); `sequence`: `TodaySequenceProjection` (no field annotation in current DTO) |
| Explicit/documented statuses | `Not explicit in handler syntax; inherited from framework/advice` |
| Positive test mapping | `COND-API-005` / `UC-MH-16-TC-API-005` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.6 Handler Contract — `GET /api/v1/user-checklist-items`

| Item | Exact current contract |
| --- | --- |
| Handler | `listItems` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/controller/UserChecklistItemController.java` |
| Authorization annotation / boundary | hasRole('MOTHER') |
| Parameters | query `journeyId`: `UUID`; query `babyId`: `UUID`; principal `principal`: `Principal` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<List<ChecklistItemResponse>>>` |
| Response payload fields | Not applicable or unresolved from the handler import |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-006` / `UC-MH-16-TC-API-006` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.7 Handler Contract — `POST /api/v1/user-checklist-items`

| Item | Exact current contract |
| --- | --- |
| Handler | `addItem` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/controller/UserChecklistItemController.java` |
| Authorization annotation / boundary | hasAnyRole('MOTHER', 'FAMILY') |
| Parameters | body `request`: `AddChecklistItemRequest`; context `contractVersionHeader`: `String`; principal `principal`: `Principal` |
| Request body type | `AddChecklistItemRequest` |
| Request fields and validators | `journeyId`: `UUID` (no field annotation in current DTO); `babyId`: `UUID` (no field annotation in current DTO); `itemText`: `String` (@NotBlank(message = "CHECKLIST-001: itemText is required"), @Size(max = 500, message = "CHECKLIST-002: itemText exceeds 500 characters")); `category`: `ChecklistCategory` (no field annotation in current DTO); `itemOrder`: `Integer` (no field annotation in current DTO); `targetSubject`: `ChecklistTargetSubject` (no field annotation in current DTO); `clientTaskId`: `UUID` (no field annotation in current DTO); `careGroupId`: `UUID` (no field annotation in current DTO) |
| Response type | `ResponseEntity<ApiResponse<ChecklistItemResponse>>` |
| Response payload fields | Not applicable or unresolved from the handler import |
| Explicit/documented statuses | `201` |
| Positive test mapping | `COND-API-007` / `UC-MH-16-TC-API-007` |
| Negative test mapping | `COND-API-007-VAL` / `UC-MH-16-TC-API-007-VAL`; plus `COND-AUTH` for protected access |

### 9.8 Handler Contract — `POST /api/v1/user-checklist-items/from-template`

| Item | Exact current contract |
| --- | --- |
| Handler | `selfAssignFromTemplate` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/controller/UserChecklistItemController.java` |
| Authorization annotation / boundary | hasRole('MOTHER') |
| Parameters | body `request`: `SelfAssignChecklistTemplateRequest`; principal `principal`: `Principal` |
| Request body type | `SelfAssignChecklistTemplateRequest` |
| Request fields and validators | `templateId`: `UUID` (@NotNull); `journeyId`: `UUID` (no field annotation in current DTO); `babyId`: `UUID` (no field annotation in current DTO) |
| Response type | `ResponseEntity<ApiResponse<ChecklistDistributionResult>>` |
| Response payload fields | `createdInstances`: `int` (no field annotation in current DTO); `existingInstances`: `int` (no field annotation in current DTO); `createdTasks`: `int` (no field annotation in current DTO); `existingTasks`: `int` (no field annotation in current DTO); `cancelledInstances`: `int` (no field annotation in current DTO); `deniedRecipients`: `int` (no field annotation in current DTO); `conflicts`: `int` (no field annotation in current DTO); `failures`: `int` (no field annotation in current DTO) |
| Explicit/documented statuses | `201` |
| Positive test mapping | `COND-API-008` / `UC-MH-16-TC-API-008` |
| Negative test mapping | `COND-API-008-VAL` / `UC-MH-16-TC-API-008-VAL`; plus `COND-AUTH` for protected access |

### 9.9 Handler Contract — `POST /api/v1/user-checklist-items/import`

| Item | Exact current contract |
| --- | --- |
| Handler | `importFromTemplate` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/controller/UserChecklistItemController.java` |
| Authorization annotation / boundary | hasRole('MOTHER') |
| Parameters | body `request`: `ImportFromTemplateRequest`; principal `principal`: `Principal` |
| Request body type | `ImportFromTemplateRequest` |
| Request fields and validators | `journeyId`: `UUID` (no field annotation in current DTO); `babyId`: `UUID` (no field annotation in current DTO); `templateItemIds`: `List<UUID>` (@NotNull(message = "CHECKLIST-001: templateItemIds is required"), @Size(min = 1, max = 50, message = "CHECKLIST-001: templateItemIds must contain 1 to 50 entries"), @NotNull(message = "CHECKLIST-001: templateItemIds cannot contain null")) |
| Response type | `ResponseEntity<ApiResponse<List<ChecklistItemResponse>>>` |
| Response payload fields | Not applicable or unresolved from the handler import |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-009` / `UC-MH-16-TC-API-009` |
| Negative test mapping | `COND-API-009-VAL` / `UC-MH-16-TC-API-009-VAL`; plus `COND-AUTH` for protected access |

### 9.10 Handler Contract — `DELETE /api/v1/user-checklist-items/{itemId}`

| Item | Exact current contract |
| --- | --- |
| Handler | `deleteItem` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/controller/UserChecklistItemController.java` |
| Authorization annotation / boundary | hasRole('MOTHER') |
| Parameters | path `itemId`: `UUID`; principal `principal`: `Principal` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<Void>` |
| Response payload fields | Not applicable or unresolved from the handler import |
| Explicit/documented statuses | `204` |
| Positive test mapping | `COND-API-010` / `UC-MH-16-TC-API-010` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.11 Handler Contract — `PUT /api/v1/user-checklist-items/{itemId}`

| Item | Exact current contract |
| --- | --- |
| Handler | `updateItem` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/controller/UserChecklistItemController.java` |
| Authorization annotation / boundary | hasRole('MOTHER') |
| Parameters | path `itemId`: `UUID`; body `request`: `UpdateChecklistItemRequest`; principal `principal`: `Principal` |
| Request body type | `UpdateChecklistItemRequest` |
| Request fields and validators | `itemText`: `String` (@Size(max = 500, message = "CHECKLIST-002: itemText exceeds 500 characters")); `category`: `ChecklistCategory` (no field annotation in current DTO); `itemOrder`: `Integer` (no field annotation in current DTO) |
| Response type | `ResponseEntity<ApiResponse<ChecklistItemResponse>>` |
| Response payload fields | Not applicable or unresolved from the handler import |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-011` / `UC-MH-16-TC-API-011` |
| Negative test mapping | `COND-API-011-VAL` / `UC-MH-16-TC-API-011-VAL`; plus `COND-AUTH` for protected access |

### 9.12 Handler Contract — `PATCH /api/v1/user-checklist-items/{itemId}/toggle`

| Item | Exact current contract |
| --- | --- |
| Handler | `toggleComplete` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/controller/UserChecklistItemController.java` |
| Authorization annotation / boundary | hasRole('MOTHER') |
| Parameters | path `itemId`: `UUID`; principal `principal`: `Principal` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<ChecklistItemResponse>>` |
| Response payload fields | Not applicable or unresolved from the handler import |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-012` / `UC-MH-16-TC-API-012` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

## 10. Error Codes

| Error class | HTTP / code | Trigger | Client behavior | Oracle |
| --- | --- | --- | --- | --- |
| Validation/business rule | `400` | Malformed field, range, enum, or rejected transition | Return the current stable error envelope without protected data or unintended mutation | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/today/controller/CurrentChecklistController.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/common/exception/GlobalExceptionHandler.java`; application error code is limited to what those sources/advice explicitly declare |
| Missing/inaccessible resource | `404` | Identifier is absent or deliberately hidden by access policy | Return the current stable error envelope without protected data or unintended mutation | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/today/controller/CurrentChecklistController.java`; application error code is limited to what those sources/advice explicitly declare |

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
| `VG-01` | Load the current lifecycle roadmap/checklist. | `COND-01` | `UC-MH-16-TC-001` |
| `VG-02` | Import an optional template or create a personal item. | `COND-02` | `UC-MH-16-TC-002` |
| `VG-03` | Perform an allowed item action and review history. | `COND-03` | `UC-MH-16-TC-003` |
| `VG-AUTH` | Reject wrong authentication/role/ownership/membership/consent scope | `COND-AUTH` | `UC-MH-16-TC-SEC-001` |
| `VG-GAP` | Characterize each known gap without claiming a false completed path | `COND-GAP` | `UC-MH-16-TC-GAP-001` |

## 14. Verification Methods

Existing focused evidence:

- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/checklist/UserCreatedChecklistTaskServiceTest.java`
- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/checklist/history/ChecklistHistoryServiceTest.java`
- `05_Development/CareBridgeMobileApp/test/features/checklist/checklist_roadmap_screen_test.dart`

Exact supported commands derived from the audited test paths:

- `cd 05_Development/CareBridgeAPI && ./mvnw -Dtest=UserCreatedChecklistTaskServiceTest test`
- `cd 05_Development/CareBridgeAPI && ./mvnw -Dtest=ChecklistHistoryServiceTest test`
- `cd 05_Development/CareBridgeMobileApp && flutter test test/features/checklist/checklist_roadmap_screen_test.dart`

Record pass/fail/skip counts only after executing these commands on the exact revision.

## 15. Verification Samples

| Sample | Value |
| --- | --- |
| Primary contract | Endpoints under `/api/v1/user-checklist-items/**` |
| Request | `GET /api/v1/checklists/current/tasks` → `getCurrentTasks`; `None` with Not applicable — no request body; authorization: hasAnyRole('MOTHER', 'FAMILY'). |
| Success response | `CurrentChecklistResponse` with `asOf`: `Instant` (no field annotation in current DTO); `zoneId`: `String` (no field annotation in current DTO); `horizonDays`: `int` (no field annotation in current DTO); `sections`: `CurrentChecklistSections` (no field annotation in current DTO); `counts`: `TodayTaskCounts` (no field annotation in current DTO); `correlationId`: `UUID` (no field annotation in current DTO); `sequence`: `TodaySequenceProjection` (no field annotation in current DTO); explicit/documented statuses `Not explicit in handler syntax; inherited from framework/advice`. |
| Negative sample | Use wrong role/owner/state and assert the exact mapped error without protected payload. |

## 16. Authorization Matrix

| Actor / role | Operation | Decision |
| --- | --- | --- |
| Mother / Authorized Family | `GET /api/v1/checklists/current/tasks` | hasAnyRole('MOTHER', 'FAMILY'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/today/controller/CurrentChecklistController.java` |
| Mother / Authorized Family | `GET /api/v1/checklists/history` | hasRole('MOTHER'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/history/controller/ChecklistHistoryController.java` |
| Mother / Authorized Family | `GET /api/v1/checklists/journeys/{journeyId}/tasks` | hasAnyRole('MOTHER', 'FAMILY', 'EXPERT', 'ADMIN'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/today/controller/CurrentChecklistController.java` |
| Mother / Authorized Family | `POST /api/v1/checklists/tasks/{taskId}/actions` | hasAnyRole('MOTHER', 'FAMILY'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/today/controller/CurrentChecklistController.java` |
| Mother / Authorized Family | `GET /api/v1/checklists/users/{userId}/tasks` | hasAnyRole('EXPERT', 'ADMIN'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/today/controller/CurrentChecklistController.java` |
| Mother / Authorized Family | `GET /api/v1/user-checklist-items` | hasRole('MOTHER'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/controller/UserChecklistItemController.java` |
| Mother / Authorized Family | `POST /api/v1/user-checklist-items` | hasAnyRole('MOTHER', 'FAMILY'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/controller/UserChecklistItemController.java` |
| Mother / Authorized Family | `POST /api/v1/user-checklist-items/from-template` | hasRole('MOTHER'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/controller/UserChecklistItemController.java` |
| Mother / Authorized Family | `POST /api/v1/user-checklist-items/import` | hasRole('MOTHER'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/controller/UserChecklistItemController.java` |
| Mother / Authorized Family | `DELETE /api/v1/user-checklist-items/{itemId}` | hasRole('MOTHER'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/controller/UserChecklistItemController.java` |
| Mother / Authorized Family | `PUT /api/v1/user-checklist-items/{itemId}` | hasRole('MOTHER'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/controller/UserChecklistItemController.java` |
| Mother / Authorized Family | `PATCH /api/v1/user-checklist-items/{itemId}/toggle` | hasRole('MOTHER'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/controller/UserChecklistItemController.java` |
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
