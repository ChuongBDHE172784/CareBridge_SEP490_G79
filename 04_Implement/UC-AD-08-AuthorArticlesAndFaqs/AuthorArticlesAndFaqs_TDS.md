# ENGINEERING DOCUMENTATION STANDARD (EDS) v2.0

# TECHNICAL DESIGN SPECIFICATION — Author and Version Articles and FAQs

| Field | Value |
| --- | --- |
| Document ID | `UC-AD-08-TDS` |
| Version | `0.1` |
| Date | `2026-08-23` |
| Status | `Draft` |
| Function ID | `UC-AD-08` |
| Canonical Use Case | `UC-AD-08 — Author and Version Articles and FAQs` |
| Module / Bounded Context | `Administration and Operations` |
| Primary Actor | `Content Admin` |
| Platforms | `Web / Backend / File Storage` |
| Priority | `Medium` |
| Data Classification | `Confidential administrative configuration/audit/moderation data; Restricted account, identity, credential, and report evidence where applicable` |
| Compliance Scope | `Least-privilege administration, immutable/auditable decisions, reason capture, protected-evidence minimization, and secret redaction` |
| Owner | `CareBridge Team` |
| Reviewer / Approver |  |
| Source Baseline | Current worktree on `2026-08-23`; SRS `UC-AD-08`; exact evidence in Section 1.4 |

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

- **Goal:** Create, edit, version, preview, list, tag with the current recommendation catalogue, and manage supported draft lifecycle of verified articles and FAQs.
- **Trigger:** The actor enters Web `/content/articles*`, `/content/faq*`, `/content/list`, `/content/:id*`.
- **Outcome:** Save/submit/preview according to the authoring lifecycle.
- **Current state:** `High` confidence from reachable code/test audit; no manifest-level exclusion is recorded.
- **Target state:** Preserve current code-backed behavior and resolve only explicitly evidenced limitations through approved implementation work.

### 1.2 Scope

**In scope**

- Web `/content/articles*`, `/content/faq*`, `/content/list`, `/content/:id*`

- GET/POST `/api/v1/admin/content`
- GET/PUT `/api/v1/admin/content/{id}`
- GET `/api/v1/admin/content/{id}/versions`
- POST `/api/v1/admin/content/import-batch`
- GET `/api/v1/admin/content/checklists`
- GET `/api/v1/admin/content/recommendation-tags`

**Out of scope / limitations**

- Not applicable — no manifest-level exclusion is recorded. Scope is limited to the entry points and exact contracts listed above; unrelated handlers in the same module are excluded.

### 1.3 Preconditions and Postconditions

| Type | Condition |
| --- | --- |
| Precondition | Content Admin is authenticated/authorized where the current contract requires it. |
| Precondition | Required ownership, membership, consent, resource state, and device/provider prerequisites pass current policies. |
| Postcondition | Save/submit/preview according to the authoring lifecycle. |
| Postcondition | No side effect outside the feature-owned persistence/event/provider boundary occurs. |

### 1.4 Evidence Baseline

| Source ID | Type | Exact path | Revision | Authority |
| --- | --- | --- | --- | --- |
| `SRC-SRS-01` | Requirement | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-AD-08 | 2026-08-23 | Draft code-first requirement |
| `SRC-CODE-01` | Current code | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/AdminContentController.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-02` | Current code | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/recommendation/controller/RecommendationAdminController.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-03` | Current code | `05_Development/CareBridgeWebApp/src/features/contentManagement/pages/ContentListPage.tsx` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-04` | Current code | `05_Development/CareBridgeWebApp/src/features/contentManagement/pages/EditContentPage.tsx` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-01` | Existing test | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/content/AdminContentControllerTest.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-02` | Existing test | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/integration/UpdateContentIntegrationTest.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-03` | Existing test | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/content/policy/HtmlContentSanitizerTest.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-04` | Existing test | `05_Development/CareBridgeWebApp/src/features/contentManagement/components/RichTextEditor.test.tsx` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-ERROR-01` | Current exception advice | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/common/exception/GlobalExceptionHandler.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-SUPP-01` | Historical architecture/design evidence | `04_Implement/ContentImageOrphanCleanup/ContentImageOrphanCleanup_Architecture-Evidence.md` | Worktree `2026-08-23` | Historical evidence only; current code and canonical SRS/TDS override conflicts |
| `SRC-SUPP-02` | Historical verification evidence | `04_Implement/ContentImageOrphanCleanup/ContentImageOrphanCleanup_Verification-Evidence.md` | Worktree `2026-08-23` | Historical evidence only; current code and canonical SRS/TDS override conflicts |
| `SRC-SUPP-03` | Historical architecture/design evidence | `04_Implement/ContentRichTextEditor/ContentRichTextEditor_Architecture-Evidence.md` | Worktree `2026-08-23` | Historical evidence only; current code and canonical SRS/TDS override conflicts |
| `SRC-SUPP-04` | Historical verification evidence | `04_Implement/ContentRichTextEditor/ContentRichTextEditor_Verification-Evidence.md` | Worktree `2026-08-23` | Historical evidence only; current code and canonical SRS/TDS override conflicts |

### 1.5 Open Contradictions / Questions

- No material contradiction was found among the exact sources cited in Section 1.4. Behavior not evidenced by those sources is not approved or implied by this Draft.

## 2. Traceability Matrix

| Requirement | Behavior | Exact oracle source | Component | Test condition / case |
| --- | --- | --- | --- | --- |
| `UC-AD-08-FR-01` | Create or load an article/FAQ draft and current recommendation tags. | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-AD-08 Normal Flow 1 | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/AdminContentController.java` | `COND-01` / `UC-AD-08-TC-001` |
| `UC-AD-08-FR-02` | Edit sanitized rich text, metadata, media, and version state. | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-AD-08 Normal Flow 2 | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/AdminContentController.java` | `COND-02` / `UC-AD-08-TC-002` |
| `UC-AD-08-FR-03` | Save/submit/preview according to the authoring lifecycle. | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-AD-08 Normal Flow 3 | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/AdminContentController.java` | `COND-03` / `UC-AD-08-TC-003` |
| `BR-01` | Draft/version/publication state is server authoritative. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/AdminContentController.java` | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/AdminContentController.java` | `COND-BR-01` / `UC-AD-08-TC-BR-001` |
| `BR-02` | Rich text sanitization and image orphan cleanup follow current policies. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/recommendation/controller/RecommendationAdminController.java` | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/recommendation/controller/RecommendationAdminController.java` | `COND-BR-02` / `UC-AD-08-TC-BR-002` |

## 3. Architecture Decision Records (ADR)

### ADR-UC-AD-08-01 — Use a distinct actor-goal boundary

| Item | Decision |
| --- | --- |
| Context | The retired 43-UC catalogue grouped multiple triggers, lifecycles, and permission boundaries, making implementation/test traceability generic. |
| Options | Keep the broad catalogue; split by screen; split by actor goal plus lifecycle/security boundary. |
| Decision | Use `UC-AD-08 — Author and Version Articles and FAQs` as the canonical boundary because its operations share the stated actor outcome and current implementation evidence. |
| Consequences | Related supporting screens/endpoints stay in one TDS; different lifecycle/actor outcomes have separate UCs. |
| Source / Status | SRS Section 3.1 and current code audit / Draft |

### ADR-UC-AD-08-02 — Preserve unknowns as Open

| Item | Decision |
| --- | --- |
| Context | Exact schema fields, SLA values, and some controller error codes are not fully evidenced by this manifest alone. |
| Decision | Do not invent them. Mark them `Open` and require the exact DTO/entity/migration/policy oracle before production-code changes. |
| Consequences | This document accurately characterizes current scope; unresolved design-changing items block implementation approval. |
| Source / Status | `create-specs` evidence discipline / Draft |

## 4. Non-Functional Requirements and SLA

| NFR | Target | Oracle Source | Verification |
| --- | --- | --- | --- |
| Authorization / isolation | All requests follow exact role/ownership/membership/consent policy in current code. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/AdminContentController.java` | Negative security cases in paired Test-Spec |
| Protected-data handling | No secrets or unnecessary health/location/identity/conversation/file payloads in logs, fixtures, screenshots, or audit detail. | Data classification header plus exact Section 9 request/response field inventories | Log/fixture review plus security tests |
| Availability / latency | Open — no approved feature-specific numeric SLA found. | Evidence needed: approved NFR/SLA source | Measure only; do not assert a fixed threshold |
| Accessibility | Open — confirm project-standard criteria for reachable UI surfaces. | Evidence needed: approved UX/accessibility standard | Applicable Web/Mobile UI checks |
| Retry / idempotency | Apply only semantics explicitly implemented by the owning service. | No explicit lock/version/idempotency marker is evidenced in the cited implementation sources; preserve observed behavior and add characterization before changing concurrency semantics | State/duplicate/concurrency cases where applicable |

## 5. Static Modeling

### 5.1 Component Responsibilities and Change Disposition

| Exact path | Disposition | Responsibility |
| --- | --- | --- |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/AdminContentController.java` | Reuse | Current implementation evidence for Author and Version Articles and FAQs; inspect the exact symbol before implementation changes. |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/recommendation/controller/RecommendationAdminController.java` | Reuse | Current implementation evidence for Author and Version Articles and FAQs; inspect the exact symbol before implementation changes. |
| `05_Development/CareBridgeWebApp/src/features/contentManagement/pages/ContentListPage.tsx` | Reuse | Current implementation evidence for Author and Version Articles and FAQs; inspect the exact symbol before implementation changes. |
| `05_Development/CareBridgeWebApp/src/features/contentManagement/pages/EditContentPage.tsx` | Reuse | Current implementation evidence for Author and Version Articles and FAQs; inspect the exact symbol before implementation changes. |

### 5.2 Current Component Diagram

```plantuml
@startuml
skinparam classAttributeIconSize 0
class AdminContentController as "AdminContentController.java"
class RecommendationAdminController as "RecommendationAdminController.java"
AdminContentController --> RecommendationAdminController
class ContentListPage as "ContentListPage.tsx"
RecommendationAdminController --> ContentListPage
class EditContentPage as "EditContentPage.tsx"
ContentListPage --> EditContentPage
@enduml
```

### 5.3 Data / Schema / Migration Assessment

| Item | Assessment |
| --- | --- |
| Current stores/entities | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/entity/ChecklistTemplateStatus.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/entity/ContentStage.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/entity/ContentStatus.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/entity/ContentType.java` |
| Sensitive fields | Confidential administrative configuration/audit/moderation data; Restricted account, identity, credential, and report evidence where applicable. Exact transport fields and validators are enumerated per handler in Section 9; entity-only fields require the cited service/entity source before a schema change. |
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
Actor -> Client: Enter Author and Version Articles and FAQs
Client -> Domain: Create or load an article/FAQ draft and current recommendation tags.
Domain --> Client: Result for step 1
Client -> Domain: Edit sanitized rich text, metadata, media, and version state.
Domain --> Client: Result for step 2
Client -> Domain: Save/submit/preview according to the authoring lifecycle.
Domain --> Client: Result for step 3
Client --> Actor: Render canonical outcome
@enduml
```

### 6.2 Alternative, Error, Retry, and Concurrency Flows

| Flow | Expected design behavior | Oracle |
| --- | --- | --- |
| Cancel before mutation | No unintended write or provider side effect. | SRS UC-AD-08 Alternative Flow |
| Invalid input/state | Reject with the current contract; keep canonical state unchanged. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/AdminContentController.java` |
| Wrong actor/scope | Fail closed without protected resource disclosure. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/AdminContentController.java` |
| Dependency failure | Use only the implemented bounded fallback/retry; never report false success. | Not applicable at this cited baseline — no external adapter/provider import is evidenced in the listed implementation sources |
| Duplicate/concurrent mutation | Apply only current lock/version/idempotency semantics. | No explicit lock/version/idempotency marker is evidenced in the cited implementation sources; preserve observed behavior and add characterization before changing concurrency semantics |

### 6.3 State Model and Invariants

```plantuml
@startuml
[*] --> Eligible
Eligible --> InProgress : Create or load an article/FAQ draft and current recommendation tags.
InProgress --> Outcome : Save/submit/preview according to the authoring lifecycle.
InProgress --> Rejected : validation / authorization / state failure
Outcome --> [*]
Rejected --> Eligible : actor corrects eligible input
@enduml
```

- Draft/version/publication state is server authoritative.
- Rich text sanitization and image orphan cleanup follow current policies.

## 7. Domain Event Catalog

| Direction | Event | Producer / Consumer | Payload / delivery / idempotency |
| --- | --- | --- | --- |
| Publish / consume | Current event evidence | Not applicable at this cited baseline — no event type, publisher, or listener is evidenced in the listed implementation sources | Preserve only events evidenced by the cited source set; if later call-path inspection finds none, the flow remains synchronous. |

## 8. Interface Specification

### 8.1 User / Operator Interfaces

| # | Entry point | Actor | Contract |
| ---: | --- | --- | --- |
| 1 | Web `/content/articles*`, `/content/faq*`, `/content/list`, `/content/:id*` | Content Admin | Reachable current entry point |

### 8.2 Service / Repository Interfaces

| Interface evidence | Required responsibility |
| --- | --- |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/AdminContentController.java` | Support the mapped operations without broadening authorization or lifecycle semantics. |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/recommendation/controller/RecommendationAdminController.java` | Support the mapped operations without broadening authorization or lifecycle semantics. |
| `05_Development/CareBridgeWebApp/src/features/contentManagement/pages/ContentListPage.tsx` | Support the mapped operations without broadening authorization or lifecycle semantics. |
| `05_Development/CareBridgeWebApp/src/features/contentManagement/pages/EditContentPage.tsx` | Support the mapped operations without broadening authorization or lifecycle semantics. |

## 9. API Specification

| ID | Method / path or grouped controller surface | Auth / role | Request / response / errors |
| --- | --- | --- | --- |
| `API-01` | `GET /api/v1/admin/content` | hasAnyRole('CONTENT_ADMIN','SYSTEM_ADMIN') | Handler `getContents`; parameters: query `status`: `ContentStatus`; query `type`: `ContentType`; query `stage`: `ContentStage`; query `keyword`: `String`; query `page`: `int`; query `size`: `int`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<Page<StaffContentDetailResponse>>>`; response payload fields: `content`: `ContentDetailResponse` (no field annotation in current DTO); `latestReviewFeedback`: `ReviewFeedbackResponse` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/AdminContentController.java` |
| `API-02` | `POST /api/v1/admin/content` | hasRole('CONTENT_ADMIN') | Handler `createContent`; parameters: body `request`: `CreateContentRequest`; principal `principal`: `Principal`; request body: `CreateContentRequest`; request fields/validation: `type`: `ContentType` (@NotNull(message = "Content type is required")); `title`: `String` (@NotBlank(message = "Title is required"), @Size(max = 500, message = "Title must not exceed 500 characters")); `body`: `String` (@Size(max = 50000, message = "Body must not exceed 50000 characters")); `summary`: `String` (@Size(max = 150, message = "Summary must not exceed 150 characters")); `stage`: `ContentStage` (@NotNull(message = "Stage is required")); `topicId`: `UUID` (no field annotation in current DTO); `tagIds`: `List<UUID>` (no field annotation in current DTO); `eligibleFromWeek`: `Integer` (no field annotation in current DTO); `eligibleToWeek`: `Integer` (no field annotation in current DTO); `recommendationPriority`: `Integer` (no field annotation in current DTO); response: `ResponseEntity<ApiResponse<CreateContentResponse>>`; response payload fields: `id`: `UUID` (no field annotation in current DTO); `type`: `ContentType` (no field annotation in current DTO); `title`: `String` (no field annotation in current DTO); `stage`: `ContentStage` (no field annotation in current DTO); `status`: `String` (no field annotation in current DTO); `version`: `Integer` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO); explicit/documented statuses: `201`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/AdminContentController.java` |
| `API-03` | `GET /api/v1/admin/content/checklists` | hasAnyRole('CONTENT_ADMIN','SYSTEM_ADMIN') | Handler `getChecklists`; parameters: query `stage`: `ContentStage`; query `status`: `ChecklistTemplateStatus`; query `keyword`: `String`; query `page`: `int`; query `size`: `int`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<PaginatedResponse<AdminChecklistTemplateResponse>>`; response payload fields: Not applicable or unresolved from the handler import; explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/AdminContentController.java` |
| `API-04` | `POST /api/v1/admin/content/import-batch` | hasRole('CONTENT_ADMIN') | Handler `importContentBatch`; parameters: body `request`: `com.carebridge.backend.content.dto.request.BulkImportContentRequest`; principal `principal`: `Principal`; request body: `com.carebridge.backend.content.dto.request.BulkImportContentRequest`; request fields/validation: Not applicable or unresolved from the handler import; response: `ResponseEntity<ApiResponse<com.carebridge.backend.content.dto.response.BulkImportResponse>>`; response payload fields: Not applicable or unresolved from the handler import; explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/AdminContentController.java` |
| `API-05` | `GET /api/v1/admin/content/recommendation-tags` | hasAnyRole('CONTENT_ADMIN','SYSTEM_ADMIN') | Handler `getCatalog`; parameters: No explicit handler parameter; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<RecommendationTagCatalogResponse>>`; response payload fields: `catalogVersion`: `String` (no field annotation in current DTO); `items`: `List<Item>` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/recommendation/controller/RecommendationAdminController.java` |
| `API-06` | `GET /api/v1/admin/content/{id}` | hasAnyRole('CONTENT_ADMIN','SYSTEM_ADMIN') | Handler `getContent`; parameters: path `id`: `UUID`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<StaffContentDetailResponse>>`; response payload fields: `content`: `ContentDetailResponse` (no field annotation in current DTO); `latestReviewFeedback`: `ReviewFeedbackResponse` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/AdminContentController.java` |
| `API-07` | `PUT /api/v1/admin/content/{id}` | hasRole('CONTENT_ADMIN') | Handler `updateContent`; parameters: path `id`: `UUID`; body `request`: `UpdateContentRequest`; principal `principal`: `Principal`; request body: `UpdateContentRequest`; request fields/validation: `title`: `String` (@NotBlank, @Size(max = 500)); `body`: `String` (@Size(max = 50000)); `summary`: `String` (@Size(max = 150)); `stage`: `ContentStage` (@NotNull); `topicId`: `UUID` (no field annotation in current DTO); `tagIds`: `List<UUID>` (no field annotation in current DTO); `eligibleFromWeek`: `Integer` (no field annotation in current DTO); `eligibleToWeek`: `Integer` (no field annotation in current DTO); `recommendationPriority`: `Integer` (no field annotation in current DTO); `status`: `ContentStatus` (@NotNull); `sourceLabel`: `String` (no field annotation in current DTO); `sources`: `List<.validation.Valid ContentSourceRequest>` (no field annotation in current DTO); response: `ResponseEntity<ApiResponse<UpdateContentResponse>>`; response payload fields: `id`: `UUID` (no field annotation in current DTO); `type`: `ContentType` (no field annotation in current DTO); `title`: `String` (no field annotation in current DTO); `body`: `String` (no field annotation in current DTO); `stage`: `ContentStage` (no field annotation in current DTO); `topicId`: `UUID` (no field annotation in current DTO); `eligibleFromWeek`: `Short` (no field annotation in current DTO); `eligibleToWeek`: `Short` (no field annotation in current DTO); `recommendationPriority`: `Short` (no field annotation in current DTO); `status`: `ContentStatus` (no field annotation in current DTO); `versionNo`: `Integer` (no field annotation in current DTO); `updatedAt`: `Instant` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/AdminContentController.java` |
| `API-08` | `GET /api/v1/admin/content/{id}/versions` | hasAnyRole('CONTENT_ADMIN','SYSTEM_ADMIN') | Handler `getVersionHistory`; parameters: path `id`: `UUID`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<List<ContentVersionSnapshotResponse>>>`; response payload fields: `versionNo`: `Integer` (no field annotation in current DTO); `title`: `String` (no field annotation in current DTO); `stage`: `String` (no field annotation in current DTO); `status`: `String` (no field annotation in current DTO); `sourceSummary`: `String` (no field annotation in current DTO); `tagIds`: `List<UUID>` (no field annotation in current DTO); `eligibleFromWeek`: `Short` (no field annotation in current DTO); `eligibleToWeek`: `Short` (no field annotation in current DTO); `recommendationPriority`: `Short` (no field annotation in current DTO); `changedBy`: `UUID` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/AdminContentController.java` |

Method-level Spring handlers, authorization annotations, request DTO fields/validators, response payload fields, and explicit/documented statuses above were extracted from the cited current source. Service/advice-only application error codes are not claimed where the controller does not declare them.

### 9.1 Handler Contract — `GET /api/v1/admin/content`

| Item | Exact current contract |
| --- | --- |
| Handler | `getContents` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/AdminContentController.java` |
| Authorization annotation / boundary | hasAnyRole('CONTENT_ADMIN','SYSTEM_ADMIN') |
| Parameters | query `status`: `ContentStatus`; query `type`: `ContentType`; query `stage`: `ContentStage`; query `keyword`: `String`; query `page`: `int`; query `size`: `int` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<Page<StaffContentDetailResponse>>>` |
| Response payload fields | `content`: `ContentDetailResponse` (no field annotation in current DTO); `latestReviewFeedback`: `ReviewFeedbackResponse` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-001` / `UC-AD-08-TC-API-001` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.2 Handler Contract — `POST /api/v1/admin/content`

| Item | Exact current contract |
| --- | --- |
| Handler | `createContent` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/AdminContentController.java` |
| Authorization annotation / boundary | hasRole('CONTENT_ADMIN') |
| Parameters | body `request`: `CreateContentRequest`; principal `principal`: `Principal` |
| Request body type | `CreateContentRequest` |
| Request fields and validators | `type`: `ContentType` (@NotNull(message = "Content type is required")); `title`: `String` (@NotBlank(message = "Title is required"), @Size(max = 500, message = "Title must not exceed 500 characters")); `body`: `String` (@Size(max = 50000, message = "Body must not exceed 50000 characters")); `summary`: `String` (@Size(max = 150, message = "Summary must not exceed 150 characters")); `stage`: `ContentStage` (@NotNull(message = "Stage is required")); `topicId`: `UUID` (no field annotation in current DTO); `tagIds`: `List<UUID>` (no field annotation in current DTO); `eligibleFromWeek`: `Integer` (no field annotation in current DTO); `eligibleToWeek`: `Integer` (no field annotation in current DTO); `recommendationPriority`: `Integer` (no field annotation in current DTO) |
| Response type | `ResponseEntity<ApiResponse<CreateContentResponse>>` |
| Response payload fields | `id`: `UUID` (no field annotation in current DTO); `type`: `ContentType` (no field annotation in current DTO); `title`: `String` (no field annotation in current DTO); `stage`: `ContentStage` (no field annotation in current DTO); `status`: `String` (no field annotation in current DTO); `version`: `Integer` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO) |
| Explicit/documented statuses | `201` |
| Positive test mapping | `COND-API-002` / `UC-AD-08-TC-API-002` |
| Negative test mapping | `COND-API-002-VAL` / `UC-AD-08-TC-API-002-VAL`; plus `COND-AUTH` for protected access |

### 9.3 Handler Contract — `GET /api/v1/admin/content/checklists`

| Item | Exact current contract |
| --- | --- |
| Handler | `getChecklists` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/AdminContentController.java` |
| Authorization annotation / boundary | hasAnyRole('CONTENT_ADMIN','SYSTEM_ADMIN') |
| Parameters | query `stage`: `ContentStage`; query `status`: `ChecklistTemplateStatus`; query `keyword`: `String`; query `page`: `int`; query `size`: `int` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<PaginatedResponse<AdminChecklistTemplateResponse>>` |
| Response payload fields | Not applicable or unresolved from the handler import |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-003` / `UC-AD-08-TC-API-003` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.4 Handler Contract — `POST /api/v1/admin/content/import-batch`

| Item | Exact current contract |
| --- | --- |
| Handler | `importContentBatch` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/AdminContentController.java` |
| Authorization annotation / boundary | hasRole('CONTENT_ADMIN') |
| Parameters | body `request`: `com.carebridge.backend.content.dto.request.BulkImportContentRequest`; principal `principal`: `Principal` |
| Request body type | `com.carebridge.backend.content.dto.request.BulkImportContentRequest` |
| Request fields and validators | Not applicable or unresolved from the handler import |
| Response type | `ResponseEntity<ApiResponse<com.carebridge.backend.content.dto.response.BulkImportResponse>>` |
| Response payload fields | Not applicable or unresolved from the handler import |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-004` / `UC-AD-08-TC-API-004` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.5 Handler Contract — `GET /api/v1/admin/content/recommendation-tags`

| Item | Exact current contract |
| --- | --- |
| Handler | `getCatalog` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/recommendation/controller/RecommendationAdminController.java` |
| Authorization annotation / boundary | hasAnyRole('CONTENT_ADMIN','SYSTEM_ADMIN') |
| Parameters | No explicit handler parameter |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<RecommendationTagCatalogResponse>>` |
| Response payload fields | `catalogVersion`: `String` (no field annotation in current DTO); `items`: `List<Item>` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-005` / `UC-AD-08-TC-API-005` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.6 Handler Contract — `GET /api/v1/admin/content/{id}`

| Item | Exact current contract |
| --- | --- |
| Handler | `getContent` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/AdminContentController.java` |
| Authorization annotation / boundary | hasAnyRole('CONTENT_ADMIN','SYSTEM_ADMIN') |
| Parameters | path `id`: `UUID` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<StaffContentDetailResponse>>` |
| Response payload fields | `content`: `ContentDetailResponse` (no field annotation in current DTO); `latestReviewFeedback`: `ReviewFeedbackResponse` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-006` / `UC-AD-08-TC-API-006` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.7 Handler Contract — `PUT /api/v1/admin/content/{id}`

| Item | Exact current contract |
| --- | --- |
| Handler | `updateContent` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/AdminContentController.java` |
| Authorization annotation / boundary | hasRole('CONTENT_ADMIN') |
| Parameters | path `id`: `UUID`; body `request`: `UpdateContentRequest`; principal `principal`: `Principal` |
| Request body type | `UpdateContentRequest` |
| Request fields and validators | `title`: `String` (@NotBlank, @Size(max = 500)); `body`: `String` (@Size(max = 50000)); `summary`: `String` (@Size(max = 150)); `stage`: `ContentStage` (@NotNull); `topicId`: `UUID` (no field annotation in current DTO); `tagIds`: `List<UUID>` (no field annotation in current DTO); `eligibleFromWeek`: `Integer` (no field annotation in current DTO); `eligibleToWeek`: `Integer` (no field annotation in current DTO); `recommendationPriority`: `Integer` (no field annotation in current DTO); `status`: `ContentStatus` (@NotNull); `sourceLabel`: `String` (no field annotation in current DTO); `sources`: `List<.validation.Valid ContentSourceRequest>` (no field annotation in current DTO) |
| Response type | `ResponseEntity<ApiResponse<UpdateContentResponse>>` |
| Response payload fields | `id`: `UUID` (no field annotation in current DTO); `type`: `ContentType` (no field annotation in current DTO); `title`: `String` (no field annotation in current DTO); `body`: `String` (no field annotation in current DTO); `stage`: `ContentStage` (no field annotation in current DTO); `topicId`: `UUID` (no field annotation in current DTO); `eligibleFromWeek`: `Short` (no field annotation in current DTO); `eligibleToWeek`: `Short` (no field annotation in current DTO); `recommendationPriority`: `Short` (no field annotation in current DTO); `status`: `ContentStatus` (no field annotation in current DTO); `versionNo`: `Integer` (no field annotation in current DTO); `updatedAt`: `Instant` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-007` / `UC-AD-08-TC-API-007` |
| Negative test mapping | `COND-API-007-VAL` / `UC-AD-08-TC-API-007-VAL`; plus `COND-AUTH` for protected access |

### 9.8 Handler Contract — `GET /api/v1/admin/content/{id}/versions`

| Item | Exact current contract |
| --- | --- |
| Handler | `getVersionHistory` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/AdminContentController.java` |
| Authorization annotation / boundary | hasAnyRole('CONTENT_ADMIN','SYSTEM_ADMIN') |
| Parameters | path `id`: `UUID` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<List<ContentVersionSnapshotResponse>>>` |
| Response payload fields | `versionNo`: `Integer` (no field annotation in current DTO); `title`: `String` (no field annotation in current DTO); `stage`: `String` (no field annotation in current DTO); `status`: `String` (no field annotation in current DTO); `sourceSummary`: `String` (no field annotation in current DTO); `tagIds`: `List<UUID>` (no field annotation in current DTO); `eligibleFromWeek`: `Short` (no field annotation in current DTO); `eligibleToWeek`: `Short` (no field annotation in current DTO); `recommendationPriority`: `Short` (no field annotation in current DTO); `changedBy`: `UUID` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-008` / `UC-AD-08-TC-API-008` |
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
| `VG-01` | Create or load an article/FAQ draft and current recommendation tags. | `COND-01` | `UC-AD-08-TC-001` |
| `VG-02` | Edit sanitized rich text, metadata, media, and version state. | `COND-02` | `UC-AD-08-TC-002` |
| `VG-03` | Save/submit/preview according to the authoring lifecycle. | `COND-03` | `UC-AD-08-TC-003` |
| `VG-AUTH` | Reject wrong authentication/role/ownership/membership/consent scope | `COND-AUTH` | `UC-AD-08-TC-SEC-001` |
| `VG-GAP` | Characterize each known gap without claiming a false completed path | `COND-GAP` | `UC-AD-08-TC-GAP-001` |

## 14. Verification Methods

Existing focused evidence:

- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/content/AdminContentControllerTest.java`
- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/integration/UpdateContentIntegrationTest.java`
- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/content/policy/HtmlContentSanitizerTest.java`
- `05_Development/CareBridgeWebApp/src/features/contentManagement/components/RichTextEditor.test.tsx`

Exact supported commands derived from the audited test paths:

- `cd 05_Development/CareBridgeAPI && ./mvnw -Dtest=AdminContentControllerTest test`
- `cd 05_Development/CareBridgeAPI && ./mvnw -Dtest=UpdateContentIntegrationTest test`
- `cd 05_Development/CareBridgeAPI && ./mvnw -Dtest=HtmlContentSanitizerTest test`
- `cd 05_Development/CareBridgeWebApp && npm test -- src/features/contentManagement/components/RichTextEditor.test.tsx`

Record pass/fail/skip counts only after executing these commands on the exact revision.

## 15. Verification Samples

| Sample | Value |
| --- | --- |
| Primary contract | GET/POST `/api/v1/admin/content` |
| Request | `GET /api/v1/admin/content` → `getContents`; `None` with Not applicable — no request body; authorization: hasAnyRole('CONTENT_ADMIN','SYSTEM_ADMIN'). |
| Success response | `ResponseEntity<ApiResponse<Page<StaffContentDetailResponse>>>` with `content`: `ContentDetailResponse` (no field annotation in current DTO); `latestReviewFeedback`: `ReviewFeedbackResponse` (no field annotation in current DTO); explicit/documented statuses `200`. |
| Negative sample | Use wrong role/owner/state and assert the exact mapped error without protected payload. |

## 16. Authorization Matrix

| Actor / role | Operation | Decision |
| --- | --- | --- |
| Content Admin | `GET /api/v1/admin/content` | hasAnyRole('CONTENT_ADMIN','SYSTEM_ADMIN'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/AdminContentController.java` |
| Content Admin | `POST /api/v1/admin/content` | hasRole('CONTENT_ADMIN'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/AdminContentController.java` |
| Content Admin | `GET /api/v1/admin/content/checklists` | hasAnyRole('CONTENT_ADMIN','SYSTEM_ADMIN'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/AdminContentController.java` |
| Content Admin | `POST /api/v1/admin/content/import-batch` | hasRole('CONTENT_ADMIN'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/AdminContentController.java` |
| Content Admin | `GET /api/v1/admin/content/recommendation-tags` | hasAnyRole('CONTENT_ADMIN','SYSTEM_ADMIN'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/recommendation/controller/RecommendationAdminController.java` |
| Content Admin | `GET /api/v1/admin/content/{id}` | hasAnyRole('CONTENT_ADMIN','SYSTEM_ADMIN'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/AdminContentController.java` |
| Content Admin | `PUT /api/v1/admin/content/{id}` | hasRole('CONTENT_ADMIN'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/AdminContentController.java` |
| Content Admin | `GET /api/v1/admin/content/{id}/versions` | hasAnyRole('CONTENT_ADMIN','SYSTEM_ADMIN'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/AdminContentController.java` |
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
