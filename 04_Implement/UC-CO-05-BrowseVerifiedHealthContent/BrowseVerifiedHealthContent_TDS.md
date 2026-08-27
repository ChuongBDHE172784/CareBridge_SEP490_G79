# ENGINEERING DOCUMENTATION STANDARD (EDS) v2.0

# TECHNICAL DESIGN SPECIFICATION — Browse Verified Health Content

| Field | Value |
| --- | --- |
| Document ID | `UC-CO-05-TDS` |
| Version | `0.1` |
| Date | `2026-08-23` |
| Status | `Draft` |
| Function ID | `UC-CO-05` |
| Canonical Use Case | `UC-CO-05 — Browse Verified Health Content` |
| Module / Bounded Context | `Community and Content Consumption` |
| Primary Actor | `Authenticated User` |
| Platforms | `Mobile / Backend` |
| Priority | `Medium` |
| Data Classification | `Internal community/content engagement data; Confidential reporter identity and moderation-bound submissions` |
| Compliance Scope | `PDPA account minimization, moderation visibility, reporter confidentiality, and attachment access/cleanup controls` |
| Owner | `CareBridge Team` |
| Reviewer / Approver |  |
| Source Baseline | Current worktree on `2026-08-23`; SRS `UC-CO-05`; exact evidence in Section 1.4 |

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

- **Goal:** Browse/search consumer-visible verified content and open lifecycle/checklist content eligible for the current stage.
- **Trigger:** The actor enters Mobile `/content`.
- **Outcome:** Open a visible detail/checklist and follow its allowed next action.
- **Current state:** `High` confidence from reachable code/test audit; no manifest-level exclusion is recorded.
- **Target state:** Preserve current code-backed behavior and resolve only explicitly evidenced limitations through approved implementation work.

### 1.2 Scope

**In scope**

- Mobile `/content`
- View Content and Verified Content Detail screens

- GET `/api/v1/content`
- GET `/api/v1/content/{id}`
- GET `/api/v1/content/search`
- GET `/api/v1/content/lifecycle`
- GET `/api/v1/content/lifecycle/{id}`
- GET `/api/v1/content/checklists`

**Out of scope / limitations**

- Not applicable — no manifest-level exclusion is recorded. Scope is limited to the entry points and exact contracts listed above; unrelated handlers in the same module are excluded.

### 1.3 Preconditions and Postconditions

| Type | Condition |
| --- | --- |
| Precondition | Authenticated User is authenticated/authorized where the current contract requires it. |
| Precondition | Required ownership, membership, consent, resource state, and device/provider prerequisites pass current policies. |
| Postcondition | Open a visible detail/checklist and follow its allowed next action. |
| Postcondition | No side effect outside the feature-owned persistence/event/provider boundary occurs. |

### 1.4 Evidence Baseline

| Source ID | Type | Exact path | Revision | Authority |
| --- | --- | --- | --- | --- |
| `SRC-SRS-01` | Requirement | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-CO-05 | 2026-08-23 | Draft code-first requirement |
| `SRC-CODE-01` | Current code | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/ContentController.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-02` | Current code | `05_Development/CareBridgeMobileApp/lib/features/community/screens/view_content_screen.dart` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-03` | Current code | `05_Development/CareBridgeMobileApp/lib/features/community/screens/verified_content_detail_screen.dart` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-01` | Existing test | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/content/unit/ContentControllerTest.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-02` | Existing test | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/content/unit/ContentSearchServiceTest.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-03` | Existing test | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/content/unit/LifecycleContentServiceTest.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-04` | Existing test | `05_Development/CareBridgeMobileApp/test/features/community/services/content_service_test.dart` | Worktree `2026-08-23` | Current-state evidence |

### 1.5 Open Contradictions / Questions

- No material contradiction was found among the exact sources cited in Section 1.4. Behavior not evidenced by those sources is not approved or implied by this Draft.

## 2. Traceability Matrix

| Requirement | Behavior | Exact oracle source | Component | Test condition / case |
| --- | --- | --- | --- | --- |
| `UC-CO-05-FR-01` | Load/search consumer-visible verified content. | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-CO-05 Normal Flow 1 | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/ContentController.java` | `COND-01` / `UC-CO-05-TC-001` |
| `UC-CO-05-FR-02` | Filter by current lifecycle/stage when supported. | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-CO-05 Normal Flow 2 | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/ContentController.java` | `COND-02` / `UC-CO-05-TC-002` |
| `UC-CO-05-FR-03` | Open a visible detail/checklist and follow its allowed next action. | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-CO-05 Normal Flow 3 | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/ContentController.java` | `COND-03` / `UC-CO-05-TC-003` |
| `BR-01` | Only publication/lifecycle-eligible content is consumer-visible. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/ContentController.java` | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/ContentController.java` | `COND-BR-01` / `UC-CO-05-TC-BR-001` |
| `BR-02` | Verified content is editorial content, not community Q&A. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/ContentController.java` | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/ContentController.java` | `COND-BR-02` / `UC-CO-05-TC-BR-002` |

## 3. Architecture Decision Records (ADR)

### ADR-UC-CO-05-01 — Use a distinct actor-goal boundary

| Item | Decision |
| --- | --- |
| Context | The retired 43-UC catalogue grouped multiple triggers, lifecycles, and permission boundaries, making implementation/test traceability generic. |
| Options | Keep the broad catalogue; split by screen; split by actor goal plus lifecycle/security boundary. |
| Decision | Use `UC-CO-05 — Browse Verified Health Content` as the canonical boundary because its operations share the stated actor outcome and current implementation evidence. |
| Consequences | Related supporting screens/endpoints stay in one TDS; different lifecycle/actor outcomes have separate UCs. |
| Source / Status | SRS Section 3.1 and current code audit / Draft |

### ADR-UC-CO-05-02 — Preserve unknowns as Open

| Item | Decision |
| --- | --- |
| Context | Exact schema fields, SLA values, and some controller error codes are not fully evidenced by this manifest alone. |
| Decision | Do not invent them. Mark them `Open` and require the exact DTO/entity/migration/policy oracle before production-code changes. |
| Consequences | This document accurately characterizes current scope; unresolved design-changing items block implementation approval. |
| Source / Status | `create-specs` evidence discipline / Draft |

## 4. Non-Functional Requirements and SLA

| NFR | Target | Oracle Source | Verification |
| --- | --- | --- | --- |
| Authorization / isolation | All requests follow exact role/ownership/membership/consent policy in current code. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/ContentController.java` | Negative security cases in paired Test-Spec |
| Protected-data handling | No secrets or unnecessary health/location/identity/conversation/file payloads in logs, fixtures, screenshots, or audit detail. | Data classification header plus exact Section 9 request/response field inventories | Log/fixture review plus security tests |
| Availability / latency | Open — no approved feature-specific numeric SLA found. | Evidence needed: approved NFR/SLA source | Measure only; do not assert a fixed threshold |
| Accessibility | Open — confirm project-standard criteria for reachable UI surfaces. | Evidence needed: approved UX/accessibility standard | Applicable Web/Mobile UI checks |
| Retry / idempotency | Apply only semantics explicitly implemented by the owning service. | No explicit lock/version/idempotency marker is evidenced in the cited implementation sources; preserve observed behavior and add characterization before changing concurrency semantics | State/duplicate/concurrency cases where applicable |

## 5. Static Modeling

### 5.1 Component Responsibilities and Change Disposition

| Exact path | Disposition | Responsibility |
| --- | --- | --- |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/ContentController.java` | Reuse | Current implementation evidence for Browse Verified Health Content; inspect the exact symbol before implementation changes. |
| `05_Development/CareBridgeMobileApp/lib/features/community/screens/view_content_screen.dart` | Reuse | Current implementation evidence for Browse Verified Health Content; inspect the exact symbol before implementation changes. |
| `05_Development/CareBridgeMobileApp/lib/features/community/screens/verified_content_detail_screen.dart` | Reuse | Current implementation evidence for Browse Verified Health Content; inspect the exact symbol before implementation changes. |

### 5.2 Current Component Diagram

```plantuml
@startuml
skinparam classAttributeIconSize 0
class ContentController as "ContentController.java"
class view_content_screen as "view_content_screen.dart"
ContentController --> view_content_screen
class verified_content_detail_screen as "verified_content_detail_screen.dart"
view_content_screen --> verified_content_detail_screen
@enduml
```

### 5.3 Data / Schema / Migration Assessment

| Item | Assessment |
| --- | --- |
| Current stores/entities | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/entity/ContentStage.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/entity/ContentType.java` |
| Sensitive fields | Internal community/content engagement data; Confidential reporter identity and moderation-bound submissions. Exact transport fields and validators are enumerated per handler in Section 9; entity-only fields require the cited service/entity source before a schema change. |
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
Actor -> Client: Enter Browse Verified Health Content
Client -> Domain: Load/search consumer-visible verified content.
Domain --> Client: Result for step 1
Client -> Domain: Filter by current lifecycle/stage when supported.
Domain --> Client: Result for step 2
Client -> Domain: Open a visible detail/checklist and follow its allowed next action.
Domain --> Client: Result for step 3
Client --> Actor: Render canonical outcome
@enduml
```

### 6.2 Alternative, Error, Retry, and Concurrency Flows

| Flow | Expected design behavior | Oracle |
| --- | --- | --- |
| Cancel before mutation | No unintended write or provider side effect. | SRS UC-CO-05 Alternative Flow |
| Invalid input/state | Reject with the current contract; keep canonical state unchanged. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/ContentController.java` |
| Wrong actor/scope | Fail closed without protected resource disclosure. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/ContentController.java` |
| Dependency failure | Use only the implemented bounded fallback/retry; never report false success. | Not applicable at this cited baseline — no external adapter/provider import is evidenced in the listed implementation sources |
| Duplicate/concurrent mutation | Apply only current lock/version/idempotency semantics. | No explicit lock/version/idempotency marker is evidenced in the cited implementation sources; preserve observed behavior and add characterization before changing concurrency semantics |

### 6.3 State Model and Invariants

```plantuml
@startuml
[*] --> Eligible
Eligible --> InProgress : Load/search consumer-visible verified content.
InProgress --> Outcome : Open a visible detail/checklist and follow its allowed next action.
InProgress --> Rejected : validation / authorization / state failure
Outcome --> [*]
Rejected --> Eligible : actor corrects eligible input
@enduml
```

- Only publication/lifecycle-eligible content is consumer-visible.
- Verified content is editorial content, not community Q&A.

## 7. Domain Event Catalog

| Direction | Event | Producer / Consumer | Payload / delivery / idempotency |
| --- | --- | --- | --- |
| Publish / consume | Current event evidence | Not applicable at this cited baseline — no event type, publisher, or listener is evidenced in the listed implementation sources | Preserve only events evidenced by the cited source set; if later call-path inspection finds none, the flow remains synchronous. |

## 8. Interface Specification

### 8.1 User / Operator Interfaces

| # | Entry point | Actor | Contract |
| ---: | --- | --- | --- |
| 1 | Mobile `/content` | Authenticated User | Reachable current entry point |
| 2 | View Content and Verified Content Detail screens | Authenticated User | Reachable current entry point |

### 8.2 Service / Repository Interfaces

| Interface evidence | Required responsibility |
| --- | --- |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/ContentController.java` | Support the mapped operations without broadening authorization or lifecycle semantics. |
| `05_Development/CareBridgeMobileApp/lib/features/community/screens/view_content_screen.dart` | Support the mapped operations without broadening authorization or lifecycle semantics. |
| `05_Development/CareBridgeMobileApp/lib/features/community/screens/verified_content_detail_screen.dart` | Support the mapped operations without broadening authorization or lifecycle semantics. |

## 9. API Specification

| ID | Method / path or grouped controller surface | Auth / role | Request / response / errors |
| --- | --- | --- | --- |
| `API-01` | `GET /api/v1/content` | No @PreAuthorize on handler/class; effective access comes from the security chain | Handler `getContents`; parameters: query `type`: `ContentType`; query `stage`: `ContentStage`; query `topicId`: `UUID`; query `page`: `int`; query `size`: `int`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<PaginatedResponse<ContentListResponse>>`; response payload fields: Not applicable or unresolved from the handler import; explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/ContentController.java` |
| `API-02` | `GET /api/v1/content/checklists` | No @PreAuthorize on handler/class; effective access comes from the security chain | Handler `getChecklists`; parameters: query `stage`: `ContentStage`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<List<ChecklistTemplateResponse>>>`; response payload fields: `id`: `UUID` (no field annotation in current DTO); `name`: `String` (no field annotation in current DTO); `stage`: `ContentStage` (no field annotation in current DTO); `description`: `String` (no field annotation in current DTO); `templateType`: `ChecklistTemplateType` (no field annotation in current DTO); `checklistContractVersion`: `Short` (no field annotation in current DTO); `planNumber`: `Integer` (no field annotation in current DTO); `section`: `String` (no field annotation in current DTO); `scheduleType`: `ChecklistScheduleType` (no field annotation in current DTO); `materializationPolicy`: `ChecklistMaterializationPolicy` (no field annotation in current DTO); `scheduleGroupKey`: `String` (no field annotation in current DTO); `scheduleContextType`: `ChecklistCareContextType` (no field annotation in current DTO); `scheduleEndMode`: `ChecklistScheduleEndMode` (no field annotation in current DTO); `weekBoundaryRule`: `ChecklistWeekBoundaryRule` (no field annotation in current DTO); `eligibilityStartInclusive`: `Integer` (no field annotation in current DTO); `eligibilityEndInclusive`: `Integer` (no field annotation in current DTO); `items`: `List<ChecklistItemResponse>` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/ContentController.java` |
| `API-03` | `GET /api/v1/content/lifecycle` | hasRole('MOTHER') | Handler `getLifecycleContents`; parameters: query `type`: `ContentType`; query `topicId`: `UUID`; query `page`: `int`; query `size`: `int`; principal `principal`: `Principal`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<LifecycleContentEnvelope<PaginatedResponse<ContentListResponse>>>>`; response payload fields: No serializable fields extracted from `main/java/com/carebridge/backend/content/dto/response/LifecycleContentEnvelope.java`; explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/ContentController.java` |
| `API-04` | `GET /api/v1/content/lifecycle/{id}` | hasRole('MOTHER') | Handler `getLifecycleContentById`; parameters: path `id`: `UUID`; principal `principal`: `Principal`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<LifecycleContentEnvelope<ContentDetailResponse>>>`; response payload fields: No serializable fields extracted from `main/java/com/carebridge/backend/content/dto/response/LifecycleContentEnvelope.java`; explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/ContentController.java` |
| `API-05` | `GET /api/v1/content/search` | No @PreAuthorize on handler/class; effective access comes from the security chain | Handler `searchContent`; parameters: query `keyword`: `String`; query `type`: `ContentType`; query `stage`: `ContentStage`; query `topicId`: `UUID`; query `page`: `int`; query `size`: `int`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<PaginatedResponse<ContentSearchResponse>>`; response payload fields: Not applicable or unresolved from the handler import; explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/ContentController.java` |
| `API-06` | `GET /api/v1/content/{id}` | No @PreAuthorize on handler/class; effective access comes from the security chain | Handler `getContentById`; parameters: path `id`: `UUID`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<ContentDetailResponse>>`; response payload fields: `id`: `UUID` (no field annotation in current DTO); `type`: `ContentType` (no field annotation in current DTO); `title`: `String` (no field annotation in current DTO); `body`: `String` (no field annotation in current DTO); `summary`: `String` (no field annotation in current DTO); `stage`: `ContentStage` (no field annotation in current DTO); `topicId`: `UUID` (no field annotation in current DTO); `tagIds`: `List<UUID>` (no field annotation in current DTO); `eligibleFromWeek`: `Short` (no field annotation in current DTO); `eligibleToWeek`: `Short` (no field annotation in current DTO); `recommendationPriority`: `Short` (no field annotation in current DTO); `version`: `Integer` (no field annotation in current DTO); `status`: `ContentStatus` (no field annotation in current DTO); `sourceLabel`: `String` (no field annotation in current DTO); `publishedAt`: `Instant` (no field annotation in current DTO); `updatedAt`: `Instant` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO); `sources`: `List<ContentSourceResponse>` (no field annotation in current DTO); `contentStale`: `boolean` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/ContentController.java` |

Method-level Spring handlers, authorization annotations, request DTO fields/validators, response payload fields, and explicit/documented statuses above were extracted from the cited current source. Service/advice-only application error codes are not claimed where the controller does not declare them.

### 9.1 Handler Contract — `GET /api/v1/content`

| Item | Exact current contract |
| --- | --- |
| Handler | `getContents` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/ContentController.java` |
| Authorization annotation / boundary | No @PreAuthorize on handler/class; effective access comes from the security chain |
| Parameters | query `type`: `ContentType`; query `stage`: `ContentStage`; query `topicId`: `UUID`; query `page`: `int`; query `size`: `int` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<PaginatedResponse<ContentListResponse>>` |
| Response payload fields | Not applicable or unresolved from the handler import |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-001` / `UC-CO-05-TC-API-001` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.2 Handler Contract — `GET /api/v1/content/checklists`

| Item | Exact current contract |
| --- | --- |
| Handler | `getChecklists` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/ContentController.java` |
| Authorization annotation / boundary | No @PreAuthorize on handler/class; effective access comes from the security chain |
| Parameters | query `stage`: `ContentStage` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<List<ChecklistTemplateResponse>>>` |
| Response payload fields | `id`: `UUID` (no field annotation in current DTO); `name`: `String` (no field annotation in current DTO); `stage`: `ContentStage` (no field annotation in current DTO); `description`: `String` (no field annotation in current DTO); `templateType`: `ChecklistTemplateType` (no field annotation in current DTO); `checklistContractVersion`: `Short` (no field annotation in current DTO); `planNumber`: `Integer` (no field annotation in current DTO); `section`: `String` (no field annotation in current DTO); `scheduleType`: `ChecklistScheduleType` (no field annotation in current DTO); `materializationPolicy`: `ChecklistMaterializationPolicy` (no field annotation in current DTO); `scheduleGroupKey`: `String` (no field annotation in current DTO); `scheduleContextType`: `ChecklistCareContextType` (no field annotation in current DTO); `scheduleEndMode`: `ChecklistScheduleEndMode` (no field annotation in current DTO); `weekBoundaryRule`: `ChecklistWeekBoundaryRule` (no field annotation in current DTO); `eligibilityStartInclusive`: `Integer` (no field annotation in current DTO); `eligibilityEndInclusive`: `Integer` (no field annotation in current DTO); `items`: `List<ChecklistItemResponse>` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-002` / `UC-CO-05-TC-API-002` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.3 Handler Contract — `GET /api/v1/content/lifecycle`

| Item | Exact current contract |
| --- | --- |
| Handler | `getLifecycleContents` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/ContentController.java` |
| Authorization annotation / boundary | hasRole('MOTHER') |
| Parameters | query `type`: `ContentType`; query `topicId`: `UUID`; query `page`: `int`; query `size`: `int`; principal `principal`: `Principal` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<LifecycleContentEnvelope<PaginatedResponse<ContentListResponse>>>>` |
| Response payload fields | No serializable fields extracted from `main/java/com/carebridge/backend/content/dto/response/LifecycleContentEnvelope.java` |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-003` / `UC-CO-05-TC-API-003` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.4 Handler Contract — `GET /api/v1/content/lifecycle/{id}`

| Item | Exact current contract |
| --- | --- |
| Handler | `getLifecycleContentById` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/ContentController.java` |
| Authorization annotation / boundary | hasRole('MOTHER') |
| Parameters | path `id`: `UUID`; principal `principal`: `Principal` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<LifecycleContentEnvelope<ContentDetailResponse>>>` |
| Response payload fields | No serializable fields extracted from `main/java/com/carebridge/backend/content/dto/response/LifecycleContentEnvelope.java` |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-004` / `UC-CO-05-TC-API-004` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.5 Handler Contract — `GET /api/v1/content/search`

| Item | Exact current contract |
| --- | --- |
| Handler | `searchContent` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/ContentController.java` |
| Authorization annotation / boundary | No @PreAuthorize on handler/class; effective access comes from the security chain |
| Parameters | query `keyword`: `String`; query `type`: `ContentType`; query `stage`: `ContentStage`; query `topicId`: `UUID`; query `page`: `int`; query `size`: `int` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<PaginatedResponse<ContentSearchResponse>>` |
| Response payload fields | Not applicable or unresolved from the handler import |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-005` / `UC-CO-05-TC-API-005` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.6 Handler Contract — `GET /api/v1/content/{id}`

| Item | Exact current contract |
| --- | --- |
| Handler | `getContentById` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/ContentController.java` |
| Authorization annotation / boundary | No @PreAuthorize on handler/class; effective access comes from the security chain |
| Parameters | path `id`: `UUID` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<ContentDetailResponse>>` |
| Response payload fields | `id`: `UUID` (no field annotation in current DTO); `type`: `ContentType` (no field annotation in current DTO); `title`: `String` (no field annotation in current DTO); `body`: `String` (no field annotation in current DTO); `summary`: `String` (no field annotation in current DTO); `stage`: `ContentStage` (no field annotation in current DTO); `topicId`: `UUID` (no field annotation in current DTO); `tagIds`: `List<UUID>` (no field annotation in current DTO); `eligibleFromWeek`: `Short` (no field annotation in current DTO); `eligibleToWeek`: `Short` (no field annotation in current DTO); `recommendationPriority`: `Short` (no field annotation in current DTO); `version`: `Integer` (no field annotation in current DTO); `status`: `ContentStatus` (no field annotation in current DTO); `sourceLabel`: `String` (no field annotation in current DTO); `publishedAt`: `Instant` (no field annotation in current DTO); `updatedAt`: `Instant` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO); `sources`: `List<ContentSourceResponse>` (no field annotation in current DTO); `contentStale`: `boolean` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-006` / `UC-CO-05-TC-API-006` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

## 10. Error Codes

| Error class | HTTP / code | Trigger | Client behavior | Oracle |
| --- | --- | --- | --- | --- |
| Validation/business rejection | No 4xx declared by selected handler syntax; framework/service/advice mapping applies | Invalid field/range/state/ownership input | No write or false success; show only current mapped error | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/ContentController.java` plus exact exception advice/service characterization |
| Authentication/authorization | `401/403` only where the security chain or handler policy maps them | Missing credential or disallowed role/scope | Fail closed with no protected response fields | Security configuration and `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/ContentController.java` |
| Dependency/internal failure | No feature-specific status declared by selected handler syntax | Provider/storage/network/internal failure | Only implemented retry/degraded/terminal behavior | Owning adapter/advice source characterization required |

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
| `VG-01` | Load/search consumer-visible verified content. | `COND-01` | `UC-CO-05-TC-001` |
| `VG-02` | Filter by current lifecycle/stage when supported. | `COND-02` | `UC-CO-05-TC-002` |
| `VG-03` | Open a visible detail/checklist and follow its allowed next action. | `COND-03` | `UC-CO-05-TC-003` |
| `VG-AUTH` | Reject wrong authentication/role/ownership/membership/consent scope | `COND-AUTH` | `UC-CO-05-TC-SEC-001` |
| `VG-GAP` | Characterize each known gap without claiming a false completed path | `COND-GAP` | `UC-CO-05-TC-GAP-001` |

## 14. Verification Methods

Existing focused evidence:

- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/content/unit/ContentControllerTest.java`
- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/content/unit/ContentSearchServiceTest.java`
- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/content/unit/LifecycleContentServiceTest.java`
- `05_Development/CareBridgeMobileApp/test/features/community/services/content_service_test.dart`

Exact supported commands derived from the audited test paths:

- `cd 05_Development/CareBridgeAPI && ./mvnw -Dtest=ContentControllerTest test`
- `cd 05_Development/CareBridgeAPI && ./mvnw -Dtest=ContentSearchServiceTest test`
- `cd 05_Development/CareBridgeAPI && ./mvnw -Dtest=LifecycleContentServiceTest test`
- `cd 05_Development/CareBridgeMobileApp && flutter test test/features/community/services/content_service_test.dart`

Record pass/fail/skip counts only after executing these commands on the exact revision.

## 15. Verification Samples

| Sample | Value |
| --- | --- |
| Primary contract | GET `/api/v1/content` |
| Request | `GET /api/v1/content` → `getContents`; `None` with Not applicable — no request body; authorization: No @PreAuthorize on handler/class; effective access comes from the security chain. |
| Success response | `ResponseEntity<PaginatedResponse<ContentListResponse>>` with Not applicable or unresolved from the handler import; explicit/documented statuses `200`. |
| Negative sample | Use wrong role/owner/state and assert the exact mapped error without protected payload. |

## 16. Authorization Matrix

| Actor / role | Operation | Decision |
| --- | --- | --- |
| Authenticated User | `GET /api/v1/content` | No @PreAuthorize on handler/class; effective access comes from the security chain; handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/ContentController.java` |
| Authenticated User | `GET /api/v1/content/checklists` | No @PreAuthorize on handler/class; effective access comes from the security chain; handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/ContentController.java` |
| Authenticated User | `GET /api/v1/content/lifecycle` | hasRole('MOTHER'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/ContentController.java` |
| Authenticated User | `GET /api/v1/content/lifecycle/{id}` | hasRole('MOTHER'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/ContentController.java` |
| Authenticated User | `GET /api/v1/content/search` | No @PreAuthorize on handler/class; effective access comes from the security chain; handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/ContentController.java` |
| Authenticated User | `GET /api/v1/content/{id}` | No @PreAuthorize on handler/class; effective access comes from the security chain; handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/ContentController.java` |
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
