# ENGINEERING DOCUMENTATION STANDARD (EDS) v2.0

# TECHNICAL DESIGN SPECIFICATION — Assign and Track Family Care Tasks

| Field | Value |
| --- | --- |
| Document ID | `UC-FM-04-TDS` |
| Version | `0.1` |
| Date | `2026-08-23` |
| Status | `Draft` |
| Function ID | `UC-FM-04` |
| Canonical Use Case | `UC-FM-04 — Assign and Track Family Care Tasks` |
| Module / Bounded Context | `Family Cooperative Care` |
| Primary Actor | `Mother / Authorized Family` |
| Platforms | `Mobile / Backend` |
| Priority | `Medium` |
| Data Classification | `Restricted care-group relationship, shared-care task, appointment, note, permission, and consent-scoped data` |
| Compliance Scope | `PDPA relationship/consent scoping, least privilege, cross-member isolation, and auditability of permission-changing actions` |
| Owner | `CareBridge Team` |
| Reviewer / Approver |  |
| Source Baseline | Current worktree on `2026-08-23`; SRS `UC-FM-04`; exact evidence in Section 1.4 |

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

- **Goal:** Assign a care task to an eligible member, view it, update status/details, or cancel it through the task lifecycle.
- **Trigger:** The actor enters Mobile Assigned Tasks screen.
- **Outcome:** Update status/details or cancel using an allowed transition.
- **Current state:** `High` confidence from reachable code/test audit; no manifest-level exclusion is recorded.
- **Target state:** Preserve current code-backed behavior and resolve only explicitly evidenced limitations through approved implementation work.

### 1.2 Scope

**In scope**

- Mobile Assigned Tasks screen

- GET/POST `/api/v1/care-groups/{groupId}/tasks`
- GET/PATCH `/api/v1/care-groups/{groupId}/tasks/{taskId}`
- PATCH `/api/v1/care-groups/{groupId}/tasks/{taskId}/status`
- POST `/api/v1/care-groups/{groupId}/tasks/{taskId}/cancel`

**Out of scope / limitations**

- Not applicable — no manifest-level exclusion is recorded. Scope is limited to the entry points and exact contracts listed above; unrelated handlers in the same module are excluded.

### 1.3 Preconditions and Postconditions

| Type | Condition |
| --- | --- |
| Precondition | Mother / Authorized Family is authenticated/authorized where the current contract requires it. |
| Precondition | Required ownership, membership, consent, resource state, and device/provider prerequisites pass current policies. |
| Postcondition | Update status/details or cancel using an allowed transition. |
| Postcondition | No side effect outside the feature-owned persistence/event/provider boundary occurs. |

### 1.4 Evidence Baseline

| Source ID | Type | Exact path | Revision | Authority |
| --- | --- | --- | --- | --- |
| `SRC-SRS-01` | Requirement | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-FM-04 | 2026-08-23 | Draft code-first requirement |
| `SRC-CODE-01` | Current code | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-02` | Current code | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/service/impl/CareTaskServiceImpl.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-03` | Current code | `05_Development/CareBridgeMobileApp/lib/features/familySync/screens/assigned_tasks_screen.dart` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-01` | Existing test | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/CareTaskAssignmentIntegrationTest.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-02` | Existing test | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/service/CareTaskServiceImplTest.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-03` | Existing test | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/entity/CareTaskStatusFsmTest.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-ERROR-01` | Current exception advice | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/common/exception/GlobalExceptionHandler.java` | Worktree `2026-08-23` | Current-state evidence |

### 1.5 Open Contradictions / Questions

- No material contradiction was found among the exact sources cited in Section 1.4. Behavior not evidenced by those sources is not approved or implied by this Draft.

## 2. Traceability Matrix

| Requirement | Behavior | Exact oracle source | Component | Test condition / case |
| --- | --- | --- | --- | --- |
| `UC-FM-04-FR-01` | Create a task for an eligible group/member. | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-FM-04 Normal Flow 1 | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` | `COND-01` / `UC-FM-04-TC-001` |
| `UC-FM-04-FR-02` | Load assigned/owned tasks and current state. | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-FM-04 Normal Flow 2 | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` | `COND-02` / `UC-FM-04-TC-002` |
| `UC-FM-04-FR-03` | Update status/details or cancel using an allowed transition. | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-FM-04 Normal Flow 3 | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` | `COND-03` / `UC-FM-04-TC-003` |
| `BR-01` | Assignee membership, ownership, and finite-state transitions are server authoritative. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/service/impl/CareTaskServiceImpl.java` | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/service/impl/CareTaskServiceImpl.java` | `COND-BR-01` / `UC-FM-04-TC-BR-001` |
| `BR-02` | Retries cannot create duplicate assignments beyond current policy. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/service/impl/CareTaskServiceImpl.java` | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/service/impl/CareTaskServiceImpl.java` | `COND-BR-02` / `UC-FM-04-TC-BR-002` |

## 3. Architecture Decision Records (ADR)

### ADR-UC-FM-04-01 — Use a distinct actor-goal boundary

| Item | Decision |
| --- | --- |
| Context | The retired 43-UC catalogue grouped multiple triggers, lifecycles, and permission boundaries, making implementation/test traceability generic. |
| Options | Keep the broad catalogue; split by screen; split by actor goal plus lifecycle/security boundary. |
| Decision | Use `UC-FM-04 — Assign and Track Family Care Tasks` as the canonical boundary because its operations share the stated actor outcome and current implementation evidence. |
| Consequences | Related supporting screens/endpoints stay in one TDS; different lifecycle/actor outcomes have separate UCs. |
| Source / Status | SRS Section 3.1 and current code audit / Draft |

### ADR-UC-FM-04-02 — Preserve unknowns as Open

| Item | Decision |
| --- | --- |
| Context | Exact schema fields, SLA values, and some controller error codes are not fully evidenced by this manifest alone. |
| Decision | Do not invent them. Mark them `Open` and require the exact DTO/entity/migration/policy oracle before production-code changes. |
| Consequences | This document accurately characterizes current scope; unresolved design-changing items block implementation approval. |
| Source / Status | `create-specs` evidence discipline / Draft |

## 4. Non-Functional Requirements and SLA

| NFR | Target | Oracle Source | Verification |
| --- | --- | --- | --- |
| Authorization / isolation | All requests follow exact role/ownership/membership/consent policy in current code. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` | Negative security cases in paired Test-Spec |
| Protected-data handling | No secrets or unnecessary health/location/identity/conversation/file payloads in logs, fixtures, screenshots, or audit detail. | Data classification header plus exact Section 9 request/response field inventories | Log/fixture review plus security tests |
| Availability / latency | Open — no approved feature-specific numeric SLA found. | Evidence needed: approved NFR/SLA source | Measure only; do not assert a fixed threshold |
| Accessibility | Open — confirm project-standard criteria for reachable UI surfaces. | Evidence needed: approved UX/accessibility standard | Applicable Web/Mobile UI checks |
| Retry / idempotency | Apply only semantics explicitly implemented by the owning service. | No explicit lock/version/idempotency marker is evidenced in the cited implementation sources; preserve observed behavior and add characterization before changing concurrency semantics | State/duplicate/concurrency cases where applicable |

## 5. Static Modeling

### 5.1 Component Responsibilities and Change Disposition

| Exact path | Disposition | Responsibility |
| --- | --- | --- |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` | Reuse | Current implementation evidence for Assign and Track Family Care Tasks; inspect the exact symbol before implementation changes. |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/service/impl/CareTaskServiceImpl.java` | Reuse | Current implementation evidence for Assign and Track Family Care Tasks; inspect the exact symbol before implementation changes. |
| `05_Development/CareBridgeMobileApp/lib/features/familySync/screens/assigned_tasks_screen.dart` | Reuse | Current implementation evidence for Assign and Track Family Care Tasks; inspect the exact symbol before implementation changes. |

### 5.2 Current Component Diagram

```plantuml
@startuml
skinparam classAttributeIconSize 0
class CareGroupController as "CareGroupController.java"
class CareTaskServiceImpl as "CareTaskServiceImpl.java"
CareGroupController --> CareTaskServiceImpl
class assigned_tasks_screen as "assigned_tasks_screen.dart"
CareTaskServiceImpl --> assigned_tasks_screen
@enduml
```

### 5.3 Data / Schema / Migration Assessment

| Item | Assessment |
| --- | --- |
| Current stores/entities | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/entity/AuditAction.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/entity/CareGroupMember.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/entity/CareTask.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/entity/CareTaskStatus.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/entity/InviteStatus.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/repository/CareGroupMemberRepository.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/repository/CareGroupRepository.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/repository/CareTaskRepository.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/entity/DeviceToken.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/repository/DeviceTokenRepository.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/repository/UserRepository.java` |
| Sensitive fields | Restricted care-group relationship, shared-care task, appointment, note, permission, and consent-scoped data. Exact transport fields and validators are enumerated per handler in Section 9; entity-only fields require the cited service/entity source before a schema change. |
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
Actor -> Client: Enter Assign and Track Family Care Tasks
Client -> Domain: Create a task for an eligible group/member.
Domain --> Client: Result for step 1
Client -> Domain: Load assigned/owned tasks and current state.
Domain --> Client: Result for step 2
Client -> Domain: Update status/details or cancel using an allowed transition.
Domain --> Client: Result for step 3
Client --> Actor: Render canonical outcome
@enduml
```

### 6.2 Alternative, Error, Retry, and Concurrency Flows

| Flow | Expected design behavior | Oracle |
| --- | --- | --- |
| Cancel before mutation | No unintended write or provider side effect. | SRS UC-FM-04 Alternative Flow |
| Invalid input/state | Reject with the current contract; keep canonical state unchanged. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` |
| Wrong actor/scope | Fail closed without protected resource disclosure. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` |
| Dependency failure | Use only the implemented bounded fallback/retry; never report false success. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/entity/DeviceToken.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/repository/DeviceTokenRepository.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/service/FcmService.java` |
| Duplicate/concurrent mutation | Apply only current lock/version/idempotency semantics. | No explicit lock/version/idempotency marker is evidenced in the cited implementation sources; preserve observed behavior and add characterization before changing concurrency semantics |

### 6.3 State Model and Invariants

```plantuml
@startuml
[*] --> Eligible
Eligible --> InProgress : Create a task for an eligible group/member.
InProgress --> Outcome : Update status/details or cancel using an allowed transition.
InProgress --> Rejected : validation / authorization / state failure
Outcome --> [*]
Rejected --> Eligible : actor corrects eligible input
@enduml
```

- Assignee membership, ownership, and finite-state transitions are server authoritative.
- Retries cannot create duplicate assignments beyond current policy.

## 7. Domain Event Catalog

| Direction | Event | Producer / Consumer | Payload / delivery / idempotency |
| --- | --- | --- | --- |
| Publish / consume | Current event evidence | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/event/CareTaskCancelled.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/event/CareTaskUpdated.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/event/FamilyTaskAssigned.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/service/impl/CareTaskServiceImpl.java` | Preserve only events evidenced by the cited source set; if later call-path inspection finds none, the flow remains synchronous. |

## 8. Interface Specification

### 8.1 User / Operator Interfaces

| # | Entry point | Actor | Contract |
| ---: | --- | --- | --- |
| 1 | Mobile Assigned Tasks screen | Mother / Authorized Family | Reachable current entry point |

### 8.2 Service / Repository Interfaces

| Interface evidence | Required responsibility |
| --- | --- |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` | Support the mapped operations without broadening authorization or lifecycle semantics. |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/service/impl/CareTaskServiceImpl.java` | Support the mapped operations without broadening authorization or lifecycle semantics. |
| `05_Development/CareBridgeMobileApp/lib/features/familySync/screens/assigned_tasks_screen.dart` | Support the mapped operations without broadening authorization or lifecycle semantics. |

## 9. API Specification

| ID | Method / path or grouped controller surface | Auth / role | Request / response / errors |
| --- | --- | --- | --- |
| `API-01` | `GET /api/v1/care-groups/{groupId}/tasks` | isAuthenticated() | Handler `listTasks`; parameters: path `groupId`: `UUID`; principal `principal`: `Principal`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<CareTasksResponse>>`; response payload fields: `groupId`: `UUID` (no field annotation in current DTO); `totalTasks`: `int` (no field annotation in current DTO); `tasks`: `List<CareTaskDto>` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` |
| `API-02` | `POST /api/v1/care-groups/{groupId}/tasks` | hasRole('MOTHER') | Handler `assignTask`; parameters: path `groupId`: `UUID`; body `request`: `AssignFamilyTaskRequest`; principal `principal`: `Principal`; request body: `AssignFamilyTaskRequest`; request fields/validation: `assigneeMemberId`: `UUID` (@NotNull); `title`: `String` (@NotBlank, @Size(max = 255)); `description`: `String` (@Size(max = 2000)); `dueAt`: `Instant` (@NotNull); `targetSubject`: `ChecklistTargetSubject` (@NotNull); response: `ResponseEntity<ApiResponse<AssignFamilyTaskResponse>>`; response payload fields: `careTaskId`: `UUID` (no field annotation in current DTO); `careGroupId`: `UUID` (no field annotation in current DTO); `assignedTo`: `UUID` (no field annotation in current DTO); `assignedBy`: `UUID` (no field annotation in current DTO); `title`: `String` (no field annotation in current DTO); `description`: `String` (no field annotation in current DTO); `dueAt`: `Instant` (no field annotation in current DTO); `status`: `String` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO); explicit/documented statuses: `201`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` |
| `API-03` | `GET /api/v1/care-groups/{groupId}/tasks/{taskId}` | isAuthenticated() | Handler `getTaskDetail`; parameters: path `groupId`: `UUID`; path `taskId`: `UUID`; principal `principal`: `Principal`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<CareTaskDetailResponse>>`; response payload fields: `careTaskId`: `UUID` (no field annotation in current DTO); `careGroupId`: `UUID` (no field annotation in current DTO); `title`: `String` (no field annotation in current DTO); `description`: `String` (no field annotation in current DTO); `dueAt`: `Instant` (no field annotation in current DTO); `status`: `String` (no field annotation in current DTO); `assignedTo`: `UUID` (no field annotation in current DTO); `assignedToName`: `String` (no field annotation in current DTO); `assignedBy`: `UUID` (no field annotation in current DTO); `assignedByName`: `String` (no field annotation in current DTO); `completedAt`: `Instant` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO); `updatedAt`: `Instant` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` |
| `API-04` | `PATCH /api/v1/care-groups/{groupId}/tasks/{taskId}` | hasRole('MOTHER') | Handler `updateTask`; parameters: path `groupId`: `UUID`; path `taskId`: `UUID`; body `request`: `UpdateFamilyTaskRequest`; principal `principal`: `Principal`; request body: `UpdateFamilyTaskRequest`; request fields/validation: `title`: `String` (no field annotation in current DTO); `description`: `String` (no field annotation in current DTO); `dueAt`: `Instant` (no field annotation in current DTO); `assigneeMemberId`: `UUID` (no field annotation in current DTO); response: `ResponseEntity<ApiResponse<UpdateFamilyTaskResponse>>`; response payload fields: `careTaskId`: `UUID` (no field annotation in current DTO); `careGroupId`: `UUID` (no field annotation in current DTO); `assignedTo`: `UUID` (no field annotation in current DTO); `assignedBy`: `UUID` (no field annotation in current DTO); `title`: `String` (no field annotation in current DTO); `description`: `String` (no field annotation in current DTO); `dueAt`: `Instant` (no field annotation in current DTO); `status`: `String` (no field annotation in current DTO); `completedAt`: `Instant` (no field annotation in current DTO); `updatedAt`: `Instant` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` |
| `API-05` | `POST /api/v1/care-groups/{groupId}/tasks/{taskId}/cancel` | hasRole('MOTHER') | Handler `cancelTask`; parameters: path `groupId`: `UUID`; path `taskId`: `UUID`; principal `principal`: `Principal`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<CancelFamilyTaskResponse>>`; response payload fields: `careTaskId`: `UUID` (no field annotation in current DTO); `careGroupId`: `UUID` (no field annotation in current DTO); `assignedTo`: `UUID` (no field annotation in current DTO); `assignedBy`: `UUID` (no field annotation in current DTO); `title`: `String` (no field annotation in current DTO); `status`: `String` (no field annotation in current DTO); `updatedAt`: `Instant` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` |
| `API-06` | `PATCH /api/v1/care-groups/{groupId}/tasks/{taskId}/status` | isAuthenticated() | Handler `updateTaskStatus`; parameters: path `groupId`: `UUID`; path `taskId`: `UUID`; body `request`: `UpdateTaskStatusRequest`; principal `principal`: `Principal`; request body: `UpdateTaskStatusRequest`; request fields/validation: `status`: `String` (@NotBlank(message = "status must not be blank")); response: `ResponseEntity<ApiResponse<UpdateTaskStatusResponse>>`; response payload fields: `careTaskId`: `UUID` (no field annotation in current DTO); `careGroupId`: `UUID` (no field annotation in current DTO); `status`: `String` (no field annotation in current DTO); `completedAt`: `Instant` (no field annotation in current DTO); `updatedAt`: `Instant` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` |

Method-level Spring handlers, authorization annotations, request DTO fields/validators, response payload fields, and explicit/documented statuses above were extracted from the cited current source. Service/advice-only application error codes are not claimed where the controller does not declare them.

### 9.1 Handler Contract — `GET /api/v1/care-groups/{groupId}/tasks`

| Item | Exact current contract |
| --- | --- |
| Handler | `listTasks` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` |
| Authorization annotation / boundary | isAuthenticated() |
| Parameters | path `groupId`: `UUID`; principal `principal`: `Principal` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<CareTasksResponse>>` |
| Response payload fields | `groupId`: `UUID` (no field annotation in current DTO); `totalTasks`: `int` (no field annotation in current DTO); `tasks`: `List<CareTaskDto>` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-001` / `UC-FM-04-TC-API-001` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.2 Handler Contract — `POST /api/v1/care-groups/{groupId}/tasks`

| Item | Exact current contract |
| --- | --- |
| Handler | `assignTask` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` |
| Authorization annotation / boundary | hasRole('MOTHER') |
| Parameters | path `groupId`: `UUID`; body `request`: `AssignFamilyTaskRequest`; principal `principal`: `Principal` |
| Request body type | `AssignFamilyTaskRequest` |
| Request fields and validators | `assigneeMemberId`: `UUID` (@NotNull); `title`: `String` (@NotBlank, @Size(max = 255)); `description`: `String` (@Size(max = 2000)); `dueAt`: `Instant` (@NotNull); `targetSubject`: `ChecklistTargetSubject` (@NotNull) |
| Response type | `ResponseEntity<ApiResponse<AssignFamilyTaskResponse>>` |
| Response payload fields | `careTaskId`: `UUID` (no field annotation in current DTO); `careGroupId`: `UUID` (no field annotation in current DTO); `assignedTo`: `UUID` (no field annotation in current DTO); `assignedBy`: `UUID` (no field annotation in current DTO); `title`: `String` (no field annotation in current DTO); `description`: `String` (no field annotation in current DTO); `dueAt`: `Instant` (no field annotation in current DTO); `status`: `String` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO) |
| Explicit/documented statuses | `201` |
| Positive test mapping | `COND-API-002` / `UC-FM-04-TC-API-002` |
| Negative test mapping | `COND-API-002-VAL` / `UC-FM-04-TC-API-002-VAL`; plus `COND-AUTH` for protected access |

### 9.3 Handler Contract — `GET /api/v1/care-groups/{groupId}/tasks/{taskId}`

| Item | Exact current contract |
| --- | --- |
| Handler | `getTaskDetail` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` |
| Authorization annotation / boundary | isAuthenticated() |
| Parameters | path `groupId`: `UUID`; path `taskId`: `UUID`; principal `principal`: `Principal` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<CareTaskDetailResponse>>` |
| Response payload fields | `careTaskId`: `UUID` (no field annotation in current DTO); `careGroupId`: `UUID` (no field annotation in current DTO); `title`: `String` (no field annotation in current DTO); `description`: `String` (no field annotation in current DTO); `dueAt`: `Instant` (no field annotation in current DTO); `status`: `String` (no field annotation in current DTO); `assignedTo`: `UUID` (no field annotation in current DTO); `assignedToName`: `String` (no field annotation in current DTO); `assignedBy`: `UUID` (no field annotation in current DTO); `assignedByName`: `String` (no field annotation in current DTO); `completedAt`: `Instant` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO); `updatedAt`: `Instant` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-003` / `UC-FM-04-TC-API-003` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.4 Handler Contract — `PATCH /api/v1/care-groups/{groupId}/tasks/{taskId}`

| Item | Exact current contract |
| --- | --- |
| Handler | `updateTask` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` |
| Authorization annotation / boundary | hasRole('MOTHER') |
| Parameters | path `groupId`: `UUID`; path `taskId`: `UUID`; body `request`: `UpdateFamilyTaskRequest`; principal `principal`: `Principal` |
| Request body type | `UpdateFamilyTaskRequest` |
| Request fields and validators | `title`: `String` (no field annotation in current DTO); `description`: `String` (no field annotation in current DTO); `dueAt`: `Instant` (no field annotation in current DTO); `assigneeMemberId`: `UUID` (no field annotation in current DTO) |
| Response type | `ResponseEntity<ApiResponse<UpdateFamilyTaskResponse>>` |
| Response payload fields | `careTaskId`: `UUID` (no field annotation in current DTO); `careGroupId`: `UUID` (no field annotation in current DTO); `assignedTo`: `UUID` (no field annotation in current DTO); `assignedBy`: `UUID` (no field annotation in current DTO); `title`: `String` (no field annotation in current DTO); `description`: `String` (no field annotation in current DTO); `dueAt`: `Instant` (no field annotation in current DTO); `status`: `String` (no field annotation in current DTO); `completedAt`: `Instant` (no field annotation in current DTO); `updatedAt`: `Instant` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-004` / `UC-FM-04-TC-API-004` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.5 Handler Contract — `POST /api/v1/care-groups/{groupId}/tasks/{taskId}/cancel`

| Item | Exact current contract |
| --- | --- |
| Handler | `cancelTask` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` |
| Authorization annotation / boundary | hasRole('MOTHER') |
| Parameters | path `groupId`: `UUID`; path `taskId`: `UUID`; principal `principal`: `Principal` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<CancelFamilyTaskResponse>>` |
| Response payload fields | `careTaskId`: `UUID` (no field annotation in current DTO); `careGroupId`: `UUID` (no field annotation in current DTO); `assignedTo`: `UUID` (no field annotation in current DTO); `assignedBy`: `UUID` (no field annotation in current DTO); `title`: `String` (no field annotation in current DTO); `status`: `String` (no field annotation in current DTO); `updatedAt`: `Instant` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-005` / `UC-FM-04-TC-API-005` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.6 Handler Contract — `PATCH /api/v1/care-groups/{groupId}/tasks/{taskId}/status`

| Item | Exact current contract |
| --- | --- |
| Handler | `updateTaskStatus` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` |
| Authorization annotation / boundary | isAuthenticated() |
| Parameters | path `groupId`: `UUID`; path `taskId`: `UUID`; body `request`: `UpdateTaskStatusRequest`; principal `principal`: `Principal` |
| Request body type | `UpdateTaskStatusRequest` |
| Request fields and validators | `status`: `String` (@NotBlank(message = "status must not be blank")) |
| Response type | `ResponseEntity<ApiResponse<UpdateTaskStatusResponse>>` |
| Response payload fields | `careTaskId`: `UUID` (no field annotation in current DTO); `careGroupId`: `UUID` (no field annotation in current DTO); `status`: `String` (no field annotation in current DTO); `completedAt`: `Instant` (no field annotation in current DTO); `updatedAt`: `Instant` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-006` / `UC-FM-04-TC-API-006` |
| Negative test mapping | `COND-API-006-VAL` / `UC-FM-04-TC-API-006-VAL`; plus `COND-AUTH` for protected access |

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
| `VG-01` | Create a task for an eligible group/member. | `COND-01` | `UC-FM-04-TC-001` |
| `VG-02` | Load assigned/owned tasks and current state. | `COND-02` | `UC-FM-04-TC-002` |
| `VG-03` | Update status/details or cancel using an allowed transition. | `COND-03` | `UC-FM-04-TC-003` |
| `VG-AUTH` | Reject wrong authentication/role/ownership/membership/consent scope | `COND-AUTH` | `UC-FM-04-TC-SEC-001` |
| `VG-GAP` | Characterize each known gap without claiming a false completed path | `COND-GAP` | `UC-FM-04-TC-GAP-001` |

## 14. Verification Methods

Existing focused evidence:

- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/CareTaskAssignmentIntegrationTest.java`
- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/service/CareTaskServiceImplTest.java`
- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/entity/CareTaskStatusFsmTest.java`

Exact supported commands derived from the audited test paths:

- `cd 05_Development/CareBridgeAPI && ./mvnw -Dtest=CareTaskAssignmentIntegrationTest test`
- `cd 05_Development/CareBridgeAPI && ./mvnw -Dtest=CareTaskServiceImplTest test`
- `cd 05_Development/CareBridgeAPI && ./mvnw -Dtest=CareTaskStatusFsmTest test`

Record pass/fail/skip counts only after executing these commands on the exact revision.

## 15. Verification Samples

| Sample | Value |
| --- | --- |
| Primary contract | GET/POST `/api/v1/care-groups/{groupId}/tasks` |
| Request | `GET /api/v1/care-groups/{groupId}/tasks` → `listTasks`; `None` with Not applicable — no request body; authorization: isAuthenticated(). |
| Success response | `ResponseEntity<ApiResponse<CareTasksResponse>>` with `groupId`: `UUID` (no field annotation in current DTO); `totalTasks`: `int` (no field annotation in current DTO); `tasks`: `List<CareTaskDto>` (no field annotation in current DTO); explicit/documented statuses `200`. |
| Negative sample | Use wrong role/owner/state and assert the exact mapped error without protected payload. |

## 16. Authorization Matrix

| Actor / role | Operation | Decision |
| --- | --- | --- |
| Mother / Authorized Family | `GET /api/v1/care-groups/{groupId}/tasks` | isAuthenticated(); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` |
| Mother / Authorized Family | `POST /api/v1/care-groups/{groupId}/tasks` | hasRole('MOTHER'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` |
| Mother / Authorized Family | `GET /api/v1/care-groups/{groupId}/tasks/{taskId}` | isAuthenticated(); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` |
| Mother / Authorized Family | `PATCH /api/v1/care-groups/{groupId}/tasks/{taskId}` | hasRole('MOTHER'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` |
| Mother / Authorized Family | `POST /api/v1/care-groups/{groupId}/tasks/{taskId}/cancel` | hasRole('MOTHER'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` |
| Mother / Authorized Family | `PATCH /api/v1/care-groups/{groupId}/tasks/{taskId}/status` | isAuthenticated(); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` |
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
