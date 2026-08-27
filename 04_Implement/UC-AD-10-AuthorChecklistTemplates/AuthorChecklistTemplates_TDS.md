# ENGINEERING DOCUMENTATION STANDARD (EDS) v2.0

# TECHNICAL DESIGN SPECIFICATION — Author Checklist Templates

| Field | Value |
| --- | --- |
| Document ID | `UC-AD-10-TDS` |
| Version | `0.1` |
| Date | `2026-08-23` |
| Status | `Draft` |
| Function ID | `UC-AD-10` |
| Canonical Use Case | `UC-AD-10 — Author Checklist Templates` |
| Module / Bounded Context | `Administration and Operations` |
| Primary Actor | `Content Admin` |
| Platforms | `Web / Backend` |
| Priority | `Medium` |
| Data Classification | `Confidential administrative configuration/audit/moderation data; Restricted account, identity, credential, and report evidence where applicable` |
| Compliance Scope | `Least-privilege administration, immutable/auditable decisions, reason capture, protected-evidence minimization, and secret redaction` |
| Owner | `CareBridge Team` |
| Reviewer / Approver |  |
| Source Baseline | Current worktree on `2026-08-23`; SRS `UC-AD-10`; exact evidence in Section 1.4 |

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

- **Goal:** Create, edit, clone, import, version, and archive checklist templates before administrative approval.
- **Trigger:** The actor enters Web `/content/checklists*`.
- **Outcome:** Save/submit/archive using an allowed author lifecycle action.
- **Current state:** `High` confidence from reachable code/test audit; no manifest-level exclusion is recorded.
- **Target state:** Preserve current code-backed behavior and resolve only explicitly evidenced limitations through approved implementation work.

### 1.2 Scope

**In scope**

- Web `/content/checklists*`

- Checklist-template CRUD/clone/import/archive endpoints under `/api/v1/admin/checklist-templates/**`

**Out of scope / limitations**

- Not applicable — no manifest-level exclusion is recorded. Scope is limited to the entry points and exact contracts listed above; unrelated handlers in the same module are excluded.

### 1.3 Preconditions and Postconditions

| Type | Condition |
| --- | --- |
| Precondition | Content Admin is authenticated/authorized where the current contract requires it. |
| Precondition | Required ownership, membership, consent, resource state, and device/provider prerequisites pass current policies. |
| Postcondition | Save/submit/archive using an allowed author lifecycle action. |
| Postcondition | No side effect outside the feature-owned persistence/event/provider boundary occurs. |

### 1.4 Evidence Baseline

| Source ID | Type | Exact path | Revision | Authority |
| --- | --- | --- | --- | --- |
| `SRC-SRS-01` | Requirement | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-AD-10 | 2026-08-23 | Draft code-first requirement |
| `SRC-CODE-01` | Current code | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/AdminChecklistTemplateController.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-02` | Current code | `05_Development/CareBridgeWebApp/src/features/contentManagement/pages/ChecklistListPage.tsx` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-03` | Current code | `05_Development/CareBridgeWebApp/src/features/contentManagement/pages/ChecklistFormPage.tsx` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-01` | Existing test | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/content/AdminChecklistTemplateControllerTest.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-02` | Existing test | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/content/AdminChecklistTemplateServiceImplTest.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-03` | Existing test | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/checklist/ChecklistImportControllerTest.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-ERROR-01` | Current exception advice | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/common/exception/GlobalExceptionHandler.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-SUPP-01` | Historical architecture/design evidence | `04_Implement/ChecklistDistributionE2E/ChecklistDistributionE2E_Architecture-Evidence.md` | Worktree `2026-08-23` | Historical evidence only; current code and canonical SRS/TDS override conflicts |
| `SRC-SUPP-02` | Historical verification evidence | `04_Implement/ChecklistDistributionE2E/ChecklistDistributionE2E_Verification-Evidence.md` | Worktree `2026-08-23` | Historical evidence only; current code and canonical SRS/TDS override conflicts |

### 1.5 Open Contradictions / Questions

- No material contradiction was found among the exact sources cited in Section 1.4. Behavior not evidenced by those sources is not approved or implied by this Draft.

## 2. Traceability Matrix

| Requirement | Behavior | Exact oracle source | Component | Test condition / case |
| --- | --- | --- | --- | --- |
| `UC-AD-10-FR-01` | Create or load a checklist template/version. | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-AD-10 Normal Flow 1 | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/AdminChecklistTemplateController.java` | `COND-01` / `UC-AD-10-TC-001` |
| `UC-AD-10-FR-02` | Edit tasks/rules or clone/import validated content. | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-AD-10 Normal Flow 2 | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/AdminChecklistTemplateController.java` | `COND-02` / `UC-AD-10-TC-002` |
| `UC-AD-10-FR-03` | Save/submit/archive using an allowed author lifecycle action. | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-AD-10 Normal Flow 3 | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/AdminChecklistTemplateController.java` | `COND-03` / `UC-AD-10-TC-003` |
| `BR-01` | Content Admin authors; approval/activation is a separate System Admin UC. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/AdminChecklistTemplateController.java` | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/AdminChecklistTemplateController.java` | `COND-BR-01` / `UC-AD-10-TC-BR-001` |
| `BR-02` | Import/version/template validation is server authoritative. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/AdminChecklistTemplateController.java` | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/AdminChecklistTemplateController.java` | `COND-BR-02` / `UC-AD-10-TC-BR-002` |

## 3. Architecture Decision Records (ADR)

### ADR-UC-AD-10-01 — Use a distinct actor-goal boundary

| Item | Decision |
| --- | --- |
| Context | The retired 43-UC catalogue grouped multiple triggers, lifecycles, and permission boundaries, making implementation/test traceability generic. |
| Options | Keep the broad catalogue; split by screen; split by actor goal plus lifecycle/security boundary. |
| Decision | Use `UC-AD-10 — Author Checklist Templates` as the canonical boundary because its operations share the stated actor outcome and current implementation evidence. |
| Consequences | Related supporting screens/endpoints stay in one TDS; different lifecycle/actor outcomes have separate UCs. |
| Source / Status | SRS Section 3.1 and current code audit / Draft |

### ADR-UC-AD-10-02 — Preserve unknowns as Open

| Item | Decision |
| --- | --- |
| Context | Exact schema fields, SLA values, and some controller error codes are not fully evidenced by this manifest alone. |
| Decision | Do not invent them. Mark them `Open` and require the exact DTO/entity/migration/policy oracle before production-code changes. |
| Consequences | This document accurately characterizes current scope; unresolved design-changing items block implementation approval. |
| Source / Status | `create-specs` evidence discipline / Draft |

## 4. Non-Functional Requirements and SLA

| NFR | Target | Oracle Source | Verification |
| --- | --- | --- | --- |
| Authorization / isolation | All requests follow exact role/ownership/membership/consent policy in current code. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/AdminChecklistTemplateController.java` | Negative security cases in paired Test-Spec |
| Protected-data handling | No secrets or unnecessary health/location/identity/conversation/file payloads in logs, fixtures, screenshots, or audit detail. | Data classification header plus exact Section 9 request/response field inventories | Log/fixture review plus security tests |
| Availability / latency | Open — no approved feature-specific numeric SLA found. | Evidence needed: approved NFR/SLA source | Measure only; do not assert a fixed threshold |
| Accessibility | Open — confirm project-standard criteria for reachable UI surfaces. | Evidence needed: approved UX/accessibility standard | Applicable Web/Mobile UI checks |
| Retry / idempotency | Apply only semantics explicitly implemented by the owning service. | No explicit lock/version/idempotency marker is evidenced in the cited implementation sources; preserve observed behavior and add characterization before changing concurrency semantics | State/duplicate/concurrency cases where applicable |

## 5. Static Modeling

### 5.1 Component Responsibilities and Change Disposition

| Exact path | Disposition | Responsibility |
| --- | --- | --- |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/AdminChecklistTemplateController.java` | Reuse | Current implementation evidence for Author Checklist Templates; inspect the exact symbol before implementation changes. |
| `05_Development/CareBridgeWebApp/src/features/contentManagement/pages/ChecklistListPage.tsx` | Reuse | Current implementation evidence for Author Checklist Templates; inspect the exact symbol before implementation changes. |
| `05_Development/CareBridgeWebApp/src/features/contentManagement/pages/ChecklistFormPage.tsx` | Reuse | Current implementation evidence for Author Checklist Templates; inspect the exact symbol before implementation changes. |

### 5.2 Current Component Diagram

```plantuml
@startuml
skinparam classAttributeIconSize 0
class AdminChecklistTemplateController as "AdminChecklistTemplateController.java"
class ChecklistListPage as "ChecklistListPage.tsx"
AdminChecklistTemplateController --> ChecklistListPage
class ChecklistFormPage as "ChecklistFormPage.tsx"
ChecklistListPage --> ChecklistFormPage
@enduml
```

### 5.3 Data / Schema / Migration Assessment

| Item | Assessment |
| --- | --- |
| Current stores/entities | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/entity/ChecklistTemplateStatus.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/entity/ContentStage.java` |
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
Actor -> Client: Enter Author Checklist Templates
Client -> Domain: Create or load a checklist template/version.
Domain --> Client: Result for step 1
Client -> Domain: Edit tasks/rules or clone/import validated content.
Domain --> Client: Result for step 2
Client -> Domain: Save/submit/archive using an allowed author lifecycle action.
Domain --> Client: Result for step 3
Client --> Actor: Render canonical outcome
@enduml
```

### 6.2 Alternative, Error, Retry, and Concurrency Flows

| Flow | Expected design behavior | Oracle |
| --- | --- | --- |
| Cancel before mutation | No unintended write or provider side effect. | SRS UC-AD-10 Alternative Flow |
| Invalid input/state | Reject with the current contract; keep canonical state unchanged. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/AdminChecklistTemplateController.java` |
| Wrong actor/scope | Fail closed without protected resource disclosure. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/AdminChecklistTemplateController.java` |
| Dependency failure | Use only the implemented bounded fallback/retry; never report false success. | Not applicable at this cited baseline — no external adapter/provider import is evidenced in the listed implementation sources |
| Duplicate/concurrent mutation | Apply only current lock/version/idempotency semantics. | No explicit lock/version/idempotency marker is evidenced in the cited implementation sources; preserve observed behavior and add characterization before changing concurrency semantics |

### 6.3 State Model and Invariants

```plantuml
@startuml
[*] --> Eligible
Eligible --> InProgress : Create or load a checklist template/version.
InProgress --> Outcome : Save/submit/archive using an allowed author lifecycle action.
InProgress --> Rejected : validation / authorization / state failure
Outcome --> [*]
Rejected --> Eligible : actor corrects eligible input
@enduml
```

- Content Admin authors; approval/activation is a separate System Admin UC.
- Import/version/template validation is server authoritative.

## 7. Domain Event Catalog

| Direction | Event | Producer / Consumer | Payload / delivery / idempotency |
| --- | --- | --- | --- |
| Publish / consume | Current event evidence | Not applicable at this cited baseline — no event type, publisher, or listener is evidenced in the listed implementation sources | Preserve only events evidenced by the cited source set; if later call-path inspection finds none, the flow remains synchronous. |

## 8. Interface Specification

### 8.1 User / Operator Interfaces

| # | Entry point | Actor | Contract |
| ---: | --- | --- | --- |
| 1 | Web `/content/checklists*` | Content Admin | Reachable current entry point |

### 8.2 Service / Repository Interfaces

| Interface evidence | Required responsibility |
| --- | --- |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/AdminChecklistTemplateController.java` | Support the mapped operations without broadening authorization or lifecycle semantics. |
| `05_Development/CareBridgeWebApp/src/features/contentManagement/pages/ChecklistListPage.tsx` | Support the mapped operations without broadening authorization or lifecycle semantics. |
| `05_Development/CareBridgeWebApp/src/features/contentManagement/pages/ChecklistFormPage.tsx` | Support the mapped operations without broadening authorization or lifecycle semantics. |

## 9. API Specification

| ID | Method / path or grouped controller surface | Auth / role | Request / response / errors |
| --- | --- | --- | --- |
| `API-01` | `GET /api/v1/admin/checklist-templates` | hasAnyRole('CONTENT_ADMIN','SYSTEM_ADMIN') | Handler `list`; parameters: query `status`: `ChecklistTemplateStatus`; query `stage`: `ContentStage`; query `keyword`: `String`; query `page`: `int`; query `size`: `int`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<Page<AdminChecklistTemplateDetailResponse>>>`; response payload fields: `id`: `UUID` (no field annotation in current DTO); `name`: `String` (no field annotation in current DTO); `stage`: `ContentStage` (no field annotation in current DTO); `displayOrder`: `Integer` (no field annotation in current DTO); `status`: `ChecklistTemplateStatus` (no field annotation in current DTO); `description`: `String` (no field annotation in current DTO); `versionNo`: `Integer` (no field annotation in current DTO); `lineageId`: `UUID` (no field annotation in current DTO); `versionId`: `UUID` (no field annotation in current DTO); `recipientRoles`: `List<ChecklistRecipientRole>` (no field annotation in current DTO); `substage`: `ChecklistSubstageResponse` (no field annotation in current DTO); `migrationReviewRequired`: `Boolean` (no field annotation in current DTO); `distributionEnabled`: `Boolean` (no field annotation in current DTO); `templateType`: `ChecklistTemplateType` (no field annotation in current DTO); `checklistContractVersion`: `Short` (no field annotation in current DTO); `planNumber`: `Integer` (no field annotation in current DTO); `section`: `String` (no field annotation in current DTO); `scheduleType`: `ChecklistScheduleType` (no field annotation in current DTO); `materializationPolicy`: `ChecklistMaterializationPolicy` (no field annotation in current DTO); `scheduleGroupKey`: `String` (no field annotation in current DTO); `scheduleContextType`: `ChecklistCareContextType` (no field annotation in current DTO); `scheduleEndMode`: `ChecklistScheduleEndMode` (no field annotation in current DTO); `weekBoundaryRule`: `ChecklistWeekBoundaryRule` (no field annotation in current DTO); `eligibilityStartInclusive`: `Integer` (no field annotation in current DTO); `eligibilityEndInclusive`: `Integer` (no field annotation in current DTO); `checklistQuarantineReasonCode`: `String` (no field annotation in current DTO); `approvedAt`: `Instant` (no field annotation in current DTO); `approvedBy`: `UUID` (no field annotation in current DTO); `migrationReviewedAt`: `Instant` (no field annotation in current DTO); `migrationReviewedBy`: `UUID` (no field annotation in current DTO); `provenance`: `ChecklistProvenanceResponse` (no field annotation in current DTO); `items`: `List<ChecklistItemResponse>` (no field annotation in current DTO); `latestReviewFeedback`: `ReviewFeedbackResponse` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/AdminChecklistTemplateController.java` |
| `API-02` | `POST /api/v1/admin/checklist-templates` | hasRole('CONTENT_ADMIN') | Handler `create`; parameters: body `request`: `CreateChecklistTemplateRequest`; principal `principal`: `Principal`; request body: `CreateChecklistTemplateRequest`; request fields/validation: `name`: `String` (@NotBlank, @Size(max = 200)); `description`: `String` (@Size(max = 2000)); `templateType`: `ChecklistTemplateType` (no field annotation in current DTO); `checklistContractVersion`: `Short` (no field annotation in current DTO); `recipientRoles`: `Set<ChecklistRecipientRole>` (no field annotation in current DTO); `stage`: `ContentStage` (no field annotation in current DTO); `substage`: `ChecklistSubstageRequest` (no field annotation in current DTO); `items`: `List<ChecklistItemRequest>` (no field annotation in current DTO); `displayOrder`: `Integer` (no field annotation in current DTO); `scheduleType`: `ChecklistScheduleType` (no field annotation in current DTO); `materializationPolicy`: `ChecklistMaterializationPolicy` (no field annotation in current DTO); `scheduleGroupKey`: `String` (no field annotation in current DTO); `scheduleContextType`: `ChecklistCareContextType` (no field annotation in current DTO); `scheduleEndMode`: `ChecklistScheduleEndMode` (no field annotation in current DTO); `weekBoundaryRule`: `ChecklistWeekBoundaryRule` (no field annotation in current DTO); response: `ResponseEntity<ApiResponse<AdminChecklistTemplateDetailResponse>>`; response payload fields: `id`: `UUID` (no field annotation in current DTO); `name`: `String` (no field annotation in current DTO); `stage`: `ContentStage` (no field annotation in current DTO); `displayOrder`: `Integer` (no field annotation in current DTO); `status`: `ChecklistTemplateStatus` (no field annotation in current DTO); `description`: `String` (no field annotation in current DTO); `versionNo`: `Integer` (no field annotation in current DTO); `lineageId`: `UUID` (no field annotation in current DTO); `versionId`: `UUID` (no field annotation in current DTO); `recipientRoles`: `List<ChecklistRecipientRole>` (no field annotation in current DTO); `substage`: `ChecklistSubstageResponse` (no field annotation in current DTO); `migrationReviewRequired`: `Boolean` (no field annotation in current DTO); `distributionEnabled`: `Boolean` (no field annotation in current DTO); `templateType`: `ChecklistTemplateType` (no field annotation in current DTO); `checklistContractVersion`: `Short` (no field annotation in current DTO); `planNumber`: `Integer` (no field annotation in current DTO); `section`: `String` (no field annotation in current DTO); `scheduleType`: `ChecklistScheduleType` (no field annotation in current DTO); `materializationPolicy`: `ChecklistMaterializationPolicy` (no field annotation in current DTO); `scheduleGroupKey`: `String` (no field annotation in current DTO); `scheduleContextType`: `ChecklistCareContextType` (no field annotation in current DTO); `scheduleEndMode`: `ChecklistScheduleEndMode` (no field annotation in current DTO); `weekBoundaryRule`: `ChecklistWeekBoundaryRule` (no field annotation in current DTO); `eligibilityStartInclusive`: `Integer` (no field annotation in current DTO); `eligibilityEndInclusive`: `Integer` (no field annotation in current DTO); `checklistQuarantineReasonCode`: `String` (no field annotation in current DTO); `approvedAt`: `Instant` (no field annotation in current DTO); `approvedBy`: `UUID` (no field annotation in current DTO); `migrationReviewedAt`: `Instant` (no field annotation in current DTO); `migrationReviewedBy`: `UUID` (no field annotation in current DTO); `provenance`: `ChecklistProvenanceResponse` (no field annotation in current DTO); `items`: `List<ChecklistItemResponse>` (no field annotation in current DTO); `latestReviewFeedback`: `ReviewFeedbackResponse` (no field annotation in current DTO); explicit/documented statuses: `201`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/AdminChecklistTemplateController.java` |
| `API-03` | `POST /api/v1/admin/checklist-templates/import-batch` | hasRole('CONTENT_ADMIN') | Handler `importBatch`; parameters: body `request`: `ChecklistTemplateBatchImportRequest`; principal `principal`: `Principal`; request body: `ChecklistTemplateBatchImportRequest`; request fields/validation: `templates`: `List<ChecklistTemplateBatchImportRowRequest>` (@NotEmpty, @Size(max = 100, message = "templates must contain at most 100 entries"), @NotNull); response: `ResponseEntity<ApiResponse<ChecklistTemplateBatchImportResponse>>`; response payload fields: `totalRows`: `int` (no field annotation in current DTO); `successCount`: `int` (no field annotation in current DTO); `failedCount`: `int` (no field annotation in current DTO); `errors`: `List<String>` (no field annotation in current DTO); `createdIds`: `List<UUID>` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/AdminChecklistTemplateController.java` |
| `API-04` | `GET /api/v1/admin/checklist-templates/{id}` | hasAnyRole('CONTENT_ADMIN','SYSTEM_ADMIN') | Handler `getById`; parameters: path `id`: `UUID`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<AdminChecklistTemplateDetailResponse>>`; response payload fields: `id`: `UUID` (no field annotation in current DTO); `name`: `String` (no field annotation in current DTO); `stage`: `ContentStage` (no field annotation in current DTO); `displayOrder`: `Integer` (no field annotation in current DTO); `status`: `ChecklistTemplateStatus` (no field annotation in current DTO); `description`: `String` (no field annotation in current DTO); `versionNo`: `Integer` (no field annotation in current DTO); `lineageId`: `UUID` (no field annotation in current DTO); `versionId`: `UUID` (no field annotation in current DTO); `recipientRoles`: `List<ChecklistRecipientRole>` (no field annotation in current DTO); `substage`: `ChecklistSubstageResponse` (no field annotation in current DTO); `migrationReviewRequired`: `Boolean` (no field annotation in current DTO); `distributionEnabled`: `Boolean` (no field annotation in current DTO); `templateType`: `ChecklistTemplateType` (no field annotation in current DTO); `checklistContractVersion`: `Short` (no field annotation in current DTO); `planNumber`: `Integer` (no field annotation in current DTO); `section`: `String` (no field annotation in current DTO); `scheduleType`: `ChecklistScheduleType` (no field annotation in current DTO); `materializationPolicy`: `ChecklistMaterializationPolicy` (no field annotation in current DTO); `scheduleGroupKey`: `String` (no field annotation in current DTO); `scheduleContextType`: `ChecklistCareContextType` (no field annotation in current DTO); `scheduleEndMode`: `ChecklistScheduleEndMode` (no field annotation in current DTO); `weekBoundaryRule`: `ChecklistWeekBoundaryRule` (no field annotation in current DTO); `eligibilityStartInclusive`: `Integer` (no field annotation in current DTO); `eligibilityEndInclusive`: `Integer` (no field annotation in current DTO); `checklistQuarantineReasonCode`: `String` (no field annotation in current DTO); `approvedAt`: `Instant` (no field annotation in current DTO); `approvedBy`: `UUID` (no field annotation in current DTO); `migrationReviewedAt`: `Instant` (no field annotation in current DTO); `migrationReviewedBy`: `UUID` (no field annotation in current DTO); `provenance`: `ChecklistProvenanceResponse` (no field annotation in current DTO); `items`: `List<ChecklistItemResponse>` (no field annotation in current DTO); `latestReviewFeedback`: `ReviewFeedbackResponse` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/AdminChecklistTemplateController.java` |
| `API-05` | `PUT /api/v1/admin/checklist-templates/{id}` | hasRole('CONTENT_ADMIN') | Handler `update`; parameters: path `id`: `UUID`; body `request`: `UpdateChecklistTemplateRequest`; principal `principal`: `Principal`; request body: `UpdateChecklistTemplateRequest`; request fields/validation: `name`: `String` (@NotBlank, @Size(max = 200)); `description`: `String` (@Size(max = 2000)); `templateType`: `ChecklistTemplateType` (no field annotation in current DTO); `checklistContractVersion`: `Short` (no field annotation in current DTO); `recipientRoles`: `Set<ChecklistRecipientRole>` (no field annotation in current DTO); `stage`: `ContentStage` (no field annotation in current DTO); `substage`: `ChecklistSubstageRequest` (no field annotation in current DTO); `status`: `ChecklistTemplateStatus` (@NotNull); `items`: `full replace List<ChecklistItemRequest>` (no field annotation in current DTO); `displayOrder`: `Integer` (no field annotation in current DTO); `scheduleType`: `ChecklistScheduleType` (no field annotation in current DTO); `materializationPolicy`: `ChecklistMaterializationPolicy` (no field annotation in current DTO); `scheduleGroupKey`: `String` (no field annotation in current DTO); `scheduleContextType`: `ChecklistCareContextType` (no field annotation in current DTO); `scheduleEndMode`: `ChecklistScheduleEndMode` (no field annotation in current DTO); `weekBoundaryRule`: `ChecklistWeekBoundaryRule` (no field annotation in current DTO); response: `ResponseEntity<ApiResponse<AdminChecklistTemplateDetailResponse>>`; response payload fields: `id`: `UUID` (no field annotation in current DTO); `name`: `String` (no field annotation in current DTO); `stage`: `ContentStage` (no field annotation in current DTO); `displayOrder`: `Integer` (no field annotation in current DTO); `status`: `ChecklistTemplateStatus` (no field annotation in current DTO); `description`: `String` (no field annotation in current DTO); `versionNo`: `Integer` (no field annotation in current DTO); `lineageId`: `UUID` (no field annotation in current DTO); `versionId`: `UUID` (no field annotation in current DTO); `recipientRoles`: `List<ChecklistRecipientRole>` (no field annotation in current DTO); `substage`: `ChecklistSubstageResponse` (no field annotation in current DTO); `migrationReviewRequired`: `Boolean` (no field annotation in current DTO); `distributionEnabled`: `Boolean` (no field annotation in current DTO); `templateType`: `ChecklistTemplateType` (no field annotation in current DTO); `checklistContractVersion`: `Short` (no field annotation in current DTO); `planNumber`: `Integer` (no field annotation in current DTO); `section`: `String` (no field annotation in current DTO); `scheduleType`: `ChecklistScheduleType` (no field annotation in current DTO); `materializationPolicy`: `ChecklistMaterializationPolicy` (no field annotation in current DTO); `scheduleGroupKey`: `String` (no field annotation in current DTO); `scheduleContextType`: `ChecklistCareContextType` (no field annotation in current DTO); `scheduleEndMode`: `ChecklistScheduleEndMode` (no field annotation in current DTO); `weekBoundaryRule`: `ChecklistWeekBoundaryRule` (no field annotation in current DTO); `eligibilityStartInclusive`: `Integer` (no field annotation in current DTO); `eligibilityEndInclusive`: `Integer` (no field annotation in current DTO); `checklistQuarantineReasonCode`: `String` (no field annotation in current DTO); `approvedAt`: `Instant` (no field annotation in current DTO); `approvedBy`: `UUID` (no field annotation in current DTO); `migrationReviewedAt`: `Instant` (no field annotation in current DTO); `migrationReviewedBy`: `UUID` (no field annotation in current DTO); `provenance`: `ChecklistProvenanceResponse` (no field annotation in current DTO); `items`: `List<ChecklistItemResponse>` (no field annotation in current DTO); `latestReviewFeedback`: `ReviewFeedbackResponse` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/AdminChecklistTemplateController.java` |
| `API-06` | `POST /api/v1/admin/checklist-templates/{id}/archive` | hasRole('CONTENT_ADMIN') | Handler `archive`; parameters: path `id`: `UUID`; body `request`: `HideChecklistTemplateRequest`; principal `principal`: `Principal`; request body: `HideChecklistTemplateRequest`; request fields/validation: `reason`: `String` (@NotBlank, @Size(max = 1000)); response: `ResponseEntity<ApiResponse<HideChecklistTemplateResponse>>`; response payload fields: `id`: `UUID` (no field annotation in current DTO); `previousStatus`: `ChecklistTemplateStatus` (no field annotation in current DTO); `newStatus`: `ChecklistTemplateStatus` (no field annotation in current DTO); `reason`: `String` (no field annotation in current DTO); `hiddenByAdminId`: `UUID` (no field annotation in current DTO); `hiddenAt`: `Instant` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/AdminChecklistTemplateController.java` |
| `API-07` | `POST /api/v1/admin/checklist-templates/{id}/clone` | hasRole('CONTENT_ADMIN') | Handler `cloneVersion`; parameters: path `id`: `UUID`; principal `principal`: `Principal`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<AdminChecklistTemplateDetailResponse>>`; response payload fields: `id`: `UUID` (no field annotation in current DTO); `name`: `String` (no field annotation in current DTO); `stage`: `ContentStage` (no field annotation in current DTO); `displayOrder`: `Integer` (no field annotation in current DTO); `status`: `ChecklistTemplateStatus` (no field annotation in current DTO); `description`: `String` (no field annotation in current DTO); `versionNo`: `Integer` (no field annotation in current DTO); `lineageId`: `UUID` (no field annotation in current DTO); `versionId`: `UUID` (no field annotation in current DTO); `recipientRoles`: `List<ChecklistRecipientRole>` (no field annotation in current DTO); `substage`: `ChecklistSubstageResponse` (no field annotation in current DTO); `migrationReviewRequired`: `Boolean` (no field annotation in current DTO); `distributionEnabled`: `Boolean` (no field annotation in current DTO); `templateType`: `ChecklistTemplateType` (no field annotation in current DTO); `checklistContractVersion`: `Short` (no field annotation in current DTO); `planNumber`: `Integer` (no field annotation in current DTO); `section`: `String` (no field annotation in current DTO); `scheduleType`: `ChecklistScheduleType` (no field annotation in current DTO); `materializationPolicy`: `ChecklistMaterializationPolicy` (no field annotation in current DTO); `scheduleGroupKey`: `String` (no field annotation in current DTO); `scheduleContextType`: `ChecklistCareContextType` (no field annotation in current DTO); `scheduleEndMode`: `ChecklistScheduleEndMode` (no field annotation in current DTO); `weekBoundaryRule`: `ChecklistWeekBoundaryRule` (no field annotation in current DTO); `eligibilityStartInclusive`: `Integer` (no field annotation in current DTO); `eligibilityEndInclusive`: `Integer` (no field annotation in current DTO); `checklistQuarantineReasonCode`: `String` (no field annotation in current DTO); `approvedAt`: `Instant` (no field annotation in current DTO); `approvedBy`: `UUID` (no field annotation in current DTO); `migrationReviewedAt`: `Instant` (no field annotation in current DTO); `migrationReviewedBy`: `UUID` (no field annotation in current DTO); `provenance`: `ChecklistProvenanceResponse` (no field annotation in current DTO); `items`: `List<ChecklistItemResponse>` (no field annotation in current DTO); `latestReviewFeedback`: `ReviewFeedbackResponse` (no field annotation in current DTO); explicit/documented statuses: `201`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/AdminChecklistTemplateController.java` |
| `API-08` | `GET /api/v1/admin/checklist-templates/{id}/versions` | hasAnyRole('CONTENT_ADMIN','SYSTEM_ADMIN') | Handler `getVersionHistory`; parameters: path `id`: `UUID`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<List<ChecklistTemplateVersionSnapshotResponse>>>`; response payload fields: `versionNo`: `Integer` (no field annotation in current DTO); `name`: `String` (no field annotation in current DTO); `stage`: `String` (no field annotation in current DTO); `status`: `String` (no field annotation in current DTO); `itemCount`: `int` (no field annotation in current DTO); `changedBy`: `UUID` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/AdminChecklistTemplateController.java` |
| `API-09` | `POST /api/v1/admin/checklist-templates/{lineageId}/versions/{versionId}/clone` | hasRole('CONTENT_ADMIN') | Handler `cloneVersionInLineage`; parameters: path `lineageId`: `UUID`; path `versionId`: `UUID`; principal `principal`: `Principal`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<AdminChecklistTemplateDetailResponse>>`; response payload fields: `id`: `UUID` (no field annotation in current DTO); `name`: `String` (no field annotation in current DTO); `stage`: `ContentStage` (no field annotation in current DTO); `displayOrder`: `Integer` (no field annotation in current DTO); `status`: `ChecklistTemplateStatus` (no field annotation in current DTO); `description`: `String` (no field annotation in current DTO); `versionNo`: `Integer` (no field annotation in current DTO); `lineageId`: `UUID` (no field annotation in current DTO); `versionId`: `UUID` (no field annotation in current DTO); `recipientRoles`: `List<ChecklistRecipientRole>` (no field annotation in current DTO); `substage`: `ChecklistSubstageResponse` (no field annotation in current DTO); `migrationReviewRequired`: `Boolean` (no field annotation in current DTO); `distributionEnabled`: `Boolean` (no field annotation in current DTO); `templateType`: `ChecklistTemplateType` (no field annotation in current DTO); `checklistContractVersion`: `Short` (no field annotation in current DTO); `planNumber`: `Integer` (no field annotation in current DTO); `section`: `String` (no field annotation in current DTO); `scheduleType`: `ChecklistScheduleType` (no field annotation in current DTO); `materializationPolicy`: `ChecklistMaterializationPolicy` (no field annotation in current DTO); `scheduleGroupKey`: `String` (no field annotation in current DTO); `scheduleContextType`: `ChecklistCareContextType` (no field annotation in current DTO); `scheduleEndMode`: `ChecklistScheduleEndMode` (no field annotation in current DTO); `weekBoundaryRule`: `ChecklistWeekBoundaryRule` (no field annotation in current DTO); `eligibilityStartInclusive`: `Integer` (no field annotation in current DTO); `eligibilityEndInclusive`: `Integer` (no field annotation in current DTO); `checklistQuarantineReasonCode`: `String` (no field annotation in current DTO); `approvedAt`: `Instant` (no field annotation in current DTO); `approvedBy`: `UUID` (no field annotation in current DTO); `migrationReviewedAt`: `Instant` (no field annotation in current DTO); `migrationReviewedBy`: `UUID` (no field annotation in current DTO); `provenance`: `ChecklistProvenanceResponse` (no field annotation in current DTO); `items`: `List<ChecklistItemResponse>` (no field annotation in current DTO); `latestReviewFeedback`: `ReviewFeedbackResponse` (no field annotation in current DTO); explicit/documented statuses: `201`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/AdminChecklistTemplateController.java` |

Method-level Spring handlers, authorization annotations, request DTO fields/validators, response payload fields, and explicit/documented statuses above were extracted from the cited current source. Service/advice-only application error codes are not claimed where the controller does not declare them.

### 9.1 Handler Contract — `GET /api/v1/admin/checklist-templates`

| Item | Exact current contract |
| --- | --- |
| Handler | `list` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/AdminChecklistTemplateController.java` |
| Authorization annotation / boundary | hasAnyRole('CONTENT_ADMIN','SYSTEM_ADMIN') |
| Parameters | query `status`: `ChecklistTemplateStatus`; query `stage`: `ContentStage`; query `keyword`: `String`; query `page`: `int`; query `size`: `int` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<Page<AdminChecklistTemplateDetailResponse>>>` |
| Response payload fields | `id`: `UUID` (no field annotation in current DTO); `name`: `String` (no field annotation in current DTO); `stage`: `ContentStage` (no field annotation in current DTO); `displayOrder`: `Integer` (no field annotation in current DTO); `status`: `ChecklistTemplateStatus` (no field annotation in current DTO); `description`: `String` (no field annotation in current DTO); `versionNo`: `Integer` (no field annotation in current DTO); `lineageId`: `UUID` (no field annotation in current DTO); `versionId`: `UUID` (no field annotation in current DTO); `recipientRoles`: `List<ChecklistRecipientRole>` (no field annotation in current DTO); `substage`: `ChecklistSubstageResponse` (no field annotation in current DTO); `migrationReviewRequired`: `Boolean` (no field annotation in current DTO); `distributionEnabled`: `Boolean` (no field annotation in current DTO); `templateType`: `ChecklistTemplateType` (no field annotation in current DTO); `checklistContractVersion`: `Short` (no field annotation in current DTO); `planNumber`: `Integer` (no field annotation in current DTO); `section`: `String` (no field annotation in current DTO); `scheduleType`: `ChecklistScheduleType` (no field annotation in current DTO); `materializationPolicy`: `ChecklistMaterializationPolicy` (no field annotation in current DTO); `scheduleGroupKey`: `String` (no field annotation in current DTO); `scheduleContextType`: `ChecklistCareContextType` (no field annotation in current DTO); `scheduleEndMode`: `ChecklistScheduleEndMode` (no field annotation in current DTO); `weekBoundaryRule`: `ChecklistWeekBoundaryRule` (no field annotation in current DTO); `eligibilityStartInclusive`: `Integer` (no field annotation in current DTO); `eligibilityEndInclusive`: `Integer` (no field annotation in current DTO); `checklistQuarantineReasonCode`: `String` (no field annotation in current DTO); `approvedAt`: `Instant` (no field annotation in current DTO); `approvedBy`: `UUID` (no field annotation in current DTO); `migrationReviewedAt`: `Instant` (no field annotation in current DTO); `migrationReviewedBy`: `UUID` (no field annotation in current DTO); `provenance`: `ChecklistProvenanceResponse` (no field annotation in current DTO); `items`: `List<ChecklistItemResponse>` (no field annotation in current DTO); `latestReviewFeedback`: `ReviewFeedbackResponse` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-001` / `UC-AD-10-TC-API-001` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.2 Handler Contract — `POST /api/v1/admin/checklist-templates`

| Item | Exact current contract |
| --- | --- |
| Handler | `create` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/AdminChecklistTemplateController.java` |
| Authorization annotation / boundary | hasRole('CONTENT_ADMIN') |
| Parameters | body `request`: `CreateChecklistTemplateRequest`; principal `principal`: `Principal` |
| Request body type | `CreateChecklistTemplateRequest` |
| Request fields and validators | `name`: `String` (@NotBlank, @Size(max = 200)); `description`: `String` (@Size(max = 2000)); `templateType`: `ChecklistTemplateType` (no field annotation in current DTO); `checklistContractVersion`: `Short` (no field annotation in current DTO); `recipientRoles`: `Set<ChecklistRecipientRole>` (no field annotation in current DTO); `stage`: `ContentStage` (no field annotation in current DTO); `substage`: `ChecklistSubstageRequest` (no field annotation in current DTO); `items`: `List<ChecklistItemRequest>` (no field annotation in current DTO); `displayOrder`: `Integer` (no field annotation in current DTO); `scheduleType`: `ChecklistScheduleType` (no field annotation in current DTO); `materializationPolicy`: `ChecklistMaterializationPolicy` (no field annotation in current DTO); `scheduleGroupKey`: `String` (no field annotation in current DTO); `scheduleContextType`: `ChecklistCareContextType` (no field annotation in current DTO); `scheduleEndMode`: `ChecklistScheduleEndMode` (no field annotation in current DTO); `weekBoundaryRule`: `ChecklistWeekBoundaryRule` (no field annotation in current DTO) |
| Response type | `ResponseEntity<ApiResponse<AdminChecklistTemplateDetailResponse>>` |
| Response payload fields | `id`: `UUID` (no field annotation in current DTO); `name`: `String` (no field annotation in current DTO); `stage`: `ContentStage` (no field annotation in current DTO); `displayOrder`: `Integer` (no field annotation in current DTO); `status`: `ChecklistTemplateStatus` (no field annotation in current DTO); `description`: `String` (no field annotation in current DTO); `versionNo`: `Integer` (no field annotation in current DTO); `lineageId`: `UUID` (no field annotation in current DTO); `versionId`: `UUID` (no field annotation in current DTO); `recipientRoles`: `List<ChecklistRecipientRole>` (no field annotation in current DTO); `substage`: `ChecklistSubstageResponse` (no field annotation in current DTO); `migrationReviewRequired`: `Boolean` (no field annotation in current DTO); `distributionEnabled`: `Boolean` (no field annotation in current DTO); `templateType`: `ChecklistTemplateType` (no field annotation in current DTO); `checklistContractVersion`: `Short` (no field annotation in current DTO); `planNumber`: `Integer` (no field annotation in current DTO); `section`: `String` (no field annotation in current DTO); `scheduleType`: `ChecklistScheduleType` (no field annotation in current DTO); `materializationPolicy`: `ChecklistMaterializationPolicy` (no field annotation in current DTO); `scheduleGroupKey`: `String` (no field annotation in current DTO); `scheduleContextType`: `ChecklistCareContextType` (no field annotation in current DTO); `scheduleEndMode`: `ChecklistScheduleEndMode` (no field annotation in current DTO); `weekBoundaryRule`: `ChecklistWeekBoundaryRule` (no field annotation in current DTO); `eligibilityStartInclusive`: `Integer` (no field annotation in current DTO); `eligibilityEndInclusive`: `Integer` (no field annotation in current DTO); `checklistQuarantineReasonCode`: `String` (no field annotation in current DTO); `approvedAt`: `Instant` (no field annotation in current DTO); `approvedBy`: `UUID` (no field annotation in current DTO); `migrationReviewedAt`: `Instant` (no field annotation in current DTO); `migrationReviewedBy`: `UUID` (no field annotation in current DTO); `provenance`: `ChecklistProvenanceResponse` (no field annotation in current DTO); `items`: `List<ChecklistItemResponse>` (no field annotation in current DTO); `latestReviewFeedback`: `ReviewFeedbackResponse` (no field annotation in current DTO) |
| Explicit/documented statuses | `201` |
| Positive test mapping | `COND-API-002` / `UC-AD-10-TC-API-002` |
| Negative test mapping | `COND-API-002-VAL` / `UC-AD-10-TC-API-002-VAL`; plus `COND-AUTH` for protected access |

### 9.3 Handler Contract — `POST /api/v1/admin/checklist-templates/import-batch`

| Item | Exact current contract |
| --- | --- |
| Handler | `importBatch` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/AdminChecklistTemplateController.java` |
| Authorization annotation / boundary | hasRole('CONTENT_ADMIN') |
| Parameters | body `request`: `ChecklistTemplateBatchImportRequest`; principal `principal`: `Principal` |
| Request body type | `ChecklistTemplateBatchImportRequest` |
| Request fields and validators | `templates`: `List<ChecklistTemplateBatchImportRowRequest>` (@NotEmpty, @Size(max = 100, message = "templates must contain at most 100 entries"), @NotNull) |
| Response type | `ResponseEntity<ApiResponse<ChecklistTemplateBatchImportResponse>>` |
| Response payload fields | `totalRows`: `int` (no field annotation in current DTO); `successCount`: `int` (no field annotation in current DTO); `failedCount`: `int` (no field annotation in current DTO); `errors`: `List<String>` (no field annotation in current DTO); `createdIds`: `List<UUID>` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-003` / `UC-AD-10-TC-API-003` |
| Negative test mapping | `COND-API-003-VAL` / `UC-AD-10-TC-API-003-VAL`; plus `COND-AUTH` for protected access |

### 9.4 Handler Contract — `GET /api/v1/admin/checklist-templates/{id}`

| Item | Exact current contract |
| --- | --- |
| Handler | `getById` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/AdminChecklistTemplateController.java` |
| Authorization annotation / boundary | hasAnyRole('CONTENT_ADMIN','SYSTEM_ADMIN') |
| Parameters | path `id`: `UUID` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<AdminChecklistTemplateDetailResponse>>` |
| Response payload fields | `id`: `UUID` (no field annotation in current DTO); `name`: `String` (no field annotation in current DTO); `stage`: `ContentStage` (no field annotation in current DTO); `displayOrder`: `Integer` (no field annotation in current DTO); `status`: `ChecklistTemplateStatus` (no field annotation in current DTO); `description`: `String` (no field annotation in current DTO); `versionNo`: `Integer` (no field annotation in current DTO); `lineageId`: `UUID` (no field annotation in current DTO); `versionId`: `UUID` (no field annotation in current DTO); `recipientRoles`: `List<ChecklistRecipientRole>` (no field annotation in current DTO); `substage`: `ChecklistSubstageResponse` (no field annotation in current DTO); `migrationReviewRequired`: `Boolean` (no field annotation in current DTO); `distributionEnabled`: `Boolean` (no field annotation in current DTO); `templateType`: `ChecklistTemplateType` (no field annotation in current DTO); `checklistContractVersion`: `Short` (no field annotation in current DTO); `planNumber`: `Integer` (no field annotation in current DTO); `section`: `String` (no field annotation in current DTO); `scheduleType`: `ChecklistScheduleType` (no field annotation in current DTO); `materializationPolicy`: `ChecklistMaterializationPolicy` (no field annotation in current DTO); `scheduleGroupKey`: `String` (no field annotation in current DTO); `scheduleContextType`: `ChecklistCareContextType` (no field annotation in current DTO); `scheduleEndMode`: `ChecklistScheduleEndMode` (no field annotation in current DTO); `weekBoundaryRule`: `ChecklistWeekBoundaryRule` (no field annotation in current DTO); `eligibilityStartInclusive`: `Integer` (no field annotation in current DTO); `eligibilityEndInclusive`: `Integer` (no field annotation in current DTO); `checklistQuarantineReasonCode`: `String` (no field annotation in current DTO); `approvedAt`: `Instant` (no field annotation in current DTO); `approvedBy`: `UUID` (no field annotation in current DTO); `migrationReviewedAt`: `Instant` (no field annotation in current DTO); `migrationReviewedBy`: `UUID` (no field annotation in current DTO); `provenance`: `ChecklistProvenanceResponse` (no field annotation in current DTO); `items`: `List<ChecklistItemResponse>` (no field annotation in current DTO); `latestReviewFeedback`: `ReviewFeedbackResponse` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-004` / `UC-AD-10-TC-API-004` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.5 Handler Contract — `PUT /api/v1/admin/checklist-templates/{id}`

| Item | Exact current contract |
| --- | --- |
| Handler | `update` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/AdminChecklistTemplateController.java` |
| Authorization annotation / boundary | hasRole('CONTENT_ADMIN') |
| Parameters | path `id`: `UUID`; body `request`: `UpdateChecklistTemplateRequest`; principal `principal`: `Principal` |
| Request body type | `UpdateChecklistTemplateRequest` |
| Request fields and validators | `name`: `String` (@NotBlank, @Size(max = 200)); `description`: `String` (@Size(max = 2000)); `templateType`: `ChecklistTemplateType` (no field annotation in current DTO); `checklistContractVersion`: `Short` (no field annotation in current DTO); `recipientRoles`: `Set<ChecklistRecipientRole>` (no field annotation in current DTO); `stage`: `ContentStage` (no field annotation in current DTO); `substage`: `ChecklistSubstageRequest` (no field annotation in current DTO); `status`: `ChecklistTemplateStatus` (@NotNull); `items`: `full replace List<ChecklistItemRequest>` (no field annotation in current DTO); `displayOrder`: `Integer` (no field annotation in current DTO); `scheduleType`: `ChecklistScheduleType` (no field annotation in current DTO); `materializationPolicy`: `ChecklistMaterializationPolicy` (no field annotation in current DTO); `scheduleGroupKey`: `String` (no field annotation in current DTO); `scheduleContextType`: `ChecklistCareContextType` (no field annotation in current DTO); `scheduleEndMode`: `ChecklistScheduleEndMode` (no field annotation in current DTO); `weekBoundaryRule`: `ChecklistWeekBoundaryRule` (no field annotation in current DTO) |
| Response type | `ResponseEntity<ApiResponse<AdminChecklistTemplateDetailResponse>>` |
| Response payload fields | `id`: `UUID` (no field annotation in current DTO); `name`: `String` (no field annotation in current DTO); `stage`: `ContentStage` (no field annotation in current DTO); `displayOrder`: `Integer` (no field annotation in current DTO); `status`: `ChecklistTemplateStatus` (no field annotation in current DTO); `description`: `String` (no field annotation in current DTO); `versionNo`: `Integer` (no field annotation in current DTO); `lineageId`: `UUID` (no field annotation in current DTO); `versionId`: `UUID` (no field annotation in current DTO); `recipientRoles`: `List<ChecklistRecipientRole>` (no field annotation in current DTO); `substage`: `ChecklistSubstageResponse` (no field annotation in current DTO); `migrationReviewRequired`: `Boolean` (no field annotation in current DTO); `distributionEnabled`: `Boolean` (no field annotation in current DTO); `templateType`: `ChecklistTemplateType` (no field annotation in current DTO); `checklistContractVersion`: `Short` (no field annotation in current DTO); `planNumber`: `Integer` (no field annotation in current DTO); `section`: `String` (no field annotation in current DTO); `scheduleType`: `ChecklistScheduleType` (no field annotation in current DTO); `materializationPolicy`: `ChecklistMaterializationPolicy` (no field annotation in current DTO); `scheduleGroupKey`: `String` (no field annotation in current DTO); `scheduleContextType`: `ChecklistCareContextType` (no field annotation in current DTO); `scheduleEndMode`: `ChecklistScheduleEndMode` (no field annotation in current DTO); `weekBoundaryRule`: `ChecklistWeekBoundaryRule` (no field annotation in current DTO); `eligibilityStartInclusive`: `Integer` (no field annotation in current DTO); `eligibilityEndInclusive`: `Integer` (no field annotation in current DTO); `checklistQuarantineReasonCode`: `String` (no field annotation in current DTO); `approvedAt`: `Instant` (no field annotation in current DTO); `approvedBy`: `UUID` (no field annotation in current DTO); `migrationReviewedAt`: `Instant` (no field annotation in current DTO); `migrationReviewedBy`: `UUID` (no field annotation in current DTO); `provenance`: `ChecklistProvenanceResponse` (no field annotation in current DTO); `items`: `List<ChecklistItemResponse>` (no field annotation in current DTO); `latestReviewFeedback`: `ReviewFeedbackResponse` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-005` / `UC-AD-10-TC-API-005` |
| Negative test mapping | `COND-API-005-VAL` / `UC-AD-10-TC-API-005-VAL`; plus `COND-AUTH` for protected access |

### 9.6 Handler Contract — `POST /api/v1/admin/checklist-templates/{id}/archive`

| Item | Exact current contract |
| --- | --- |
| Handler | `archive` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/AdminChecklistTemplateController.java` |
| Authorization annotation / boundary | hasRole('CONTENT_ADMIN') |
| Parameters | path `id`: `UUID`; body `request`: `HideChecklistTemplateRequest`; principal `principal`: `Principal` |
| Request body type | `HideChecklistTemplateRequest` |
| Request fields and validators | `reason`: `String` (@NotBlank, @Size(max = 1000)) |
| Response type | `ResponseEntity<ApiResponse<HideChecklistTemplateResponse>>` |
| Response payload fields | `id`: `UUID` (no field annotation in current DTO); `previousStatus`: `ChecklistTemplateStatus` (no field annotation in current DTO); `newStatus`: `ChecklistTemplateStatus` (no field annotation in current DTO); `reason`: `String` (no field annotation in current DTO); `hiddenByAdminId`: `UUID` (no field annotation in current DTO); `hiddenAt`: `Instant` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-006` / `UC-AD-10-TC-API-006` |
| Negative test mapping | `COND-API-006-VAL` / `UC-AD-10-TC-API-006-VAL`; plus `COND-AUTH` for protected access |

### 9.7 Handler Contract — `POST /api/v1/admin/checklist-templates/{id}/clone`

| Item | Exact current contract |
| --- | --- |
| Handler | `cloneVersion` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/AdminChecklistTemplateController.java` |
| Authorization annotation / boundary | hasRole('CONTENT_ADMIN') |
| Parameters | path `id`: `UUID`; principal `principal`: `Principal` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<AdminChecklistTemplateDetailResponse>>` |
| Response payload fields | `id`: `UUID` (no field annotation in current DTO); `name`: `String` (no field annotation in current DTO); `stage`: `ContentStage` (no field annotation in current DTO); `displayOrder`: `Integer` (no field annotation in current DTO); `status`: `ChecklistTemplateStatus` (no field annotation in current DTO); `description`: `String` (no field annotation in current DTO); `versionNo`: `Integer` (no field annotation in current DTO); `lineageId`: `UUID` (no field annotation in current DTO); `versionId`: `UUID` (no field annotation in current DTO); `recipientRoles`: `List<ChecklistRecipientRole>` (no field annotation in current DTO); `substage`: `ChecklistSubstageResponse` (no field annotation in current DTO); `migrationReviewRequired`: `Boolean` (no field annotation in current DTO); `distributionEnabled`: `Boolean` (no field annotation in current DTO); `templateType`: `ChecklistTemplateType` (no field annotation in current DTO); `checklistContractVersion`: `Short` (no field annotation in current DTO); `planNumber`: `Integer` (no field annotation in current DTO); `section`: `String` (no field annotation in current DTO); `scheduleType`: `ChecklistScheduleType` (no field annotation in current DTO); `materializationPolicy`: `ChecklistMaterializationPolicy` (no field annotation in current DTO); `scheduleGroupKey`: `String` (no field annotation in current DTO); `scheduleContextType`: `ChecklistCareContextType` (no field annotation in current DTO); `scheduleEndMode`: `ChecklistScheduleEndMode` (no field annotation in current DTO); `weekBoundaryRule`: `ChecklistWeekBoundaryRule` (no field annotation in current DTO); `eligibilityStartInclusive`: `Integer` (no field annotation in current DTO); `eligibilityEndInclusive`: `Integer` (no field annotation in current DTO); `checklistQuarantineReasonCode`: `String` (no field annotation in current DTO); `approvedAt`: `Instant` (no field annotation in current DTO); `approvedBy`: `UUID` (no field annotation in current DTO); `migrationReviewedAt`: `Instant` (no field annotation in current DTO); `migrationReviewedBy`: `UUID` (no field annotation in current DTO); `provenance`: `ChecklistProvenanceResponse` (no field annotation in current DTO); `items`: `List<ChecklistItemResponse>` (no field annotation in current DTO); `latestReviewFeedback`: `ReviewFeedbackResponse` (no field annotation in current DTO) |
| Explicit/documented statuses | `201` |
| Positive test mapping | `COND-API-007` / `UC-AD-10-TC-API-007` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.8 Handler Contract — `GET /api/v1/admin/checklist-templates/{id}/versions`

| Item | Exact current contract |
| --- | --- |
| Handler | `getVersionHistory` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/AdminChecklistTemplateController.java` |
| Authorization annotation / boundary | hasAnyRole('CONTENT_ADMIN','SYSTEM_ADMIN') |
| Parameters | path `id`: `UUID` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<List<ChecklistTemplateVersionSnapshotResponse>>>` |
| Response payload fields | `versionNo`: `Integer` (no field annotation in current DTO); `name`: `String` (no field annotation in current DTO); `stage`: `String` (no field annotation in current DTO); `status`: `String` (no field annotation in current DTO); `itemCount`: `int` (no field annotation in current DTO); `changedBy`: `UUID` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-008` / `UC-AD-10-TC-API-008` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.9 Handler Contract — `POST /api/v1/admin/checklist-templates/{lineageId}/versions/{versionId}/clone`

| Item | Exact current contract |
| --- | --- |
| Handler | `cloneVersionInLineage` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/AdminChecklistTemplateController.java` |
| Authorization annotation / boundary | hasRole('CONTENT_ADMIN') |
| Parameters | path `lineageId`: `UUID`; path `versionId`: `UUID`; principal `principal`: `Principal` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<AdminChecklistTemplateDetailResponse>>` |
| Response payload fields | `id`: `UUID` (no field annotation in current DTO); `name`: `String` (no field annotation in current DTO); `stage`: `ContentStage` (no field annotation in current DTO); `displayOrder`: `Integer` (no field annotation in current DTO); `status`: `ChecklistTemplateStatus` (no field annotation in current DTO); `description`: `String` (no field annotation in current DTO); `versionNo`: `Integer` (no field annotation in current DTO); `lineageId`: `UUID` (no field annotation in current DTO); `versionId`: `UUID` (no field annotation in current DTO); `recipientRoles`: `List<ChecklistRecipientRole>` (no field annotation in current DTO); `substage`: `ChecklistSubstageResponse` (no field annotation in current DTO); `migrationReviewRequired`: `Boolean` (no field annotation in current DTO); `distributionEnabled`: `Boolean` (no field annotation in current DTO); `templateType`: `ChecklistTemplateType` (no field annotation in current DTO); `checklistContractVersion`: `Short` (no field annotation in current DTO); `planNumber`: `Integer` (no field annotation in current DTO); `section`: `String` (no field annotation in current DTO); `scheduleType`: `ChecklistScheduleType` (no field annotation in current DTO); `materializationPolicy`: `ChecklistMaterializationPolicy` (no field annotation in current DTO); `scheduleGroupKey`: `String` (no field annotation in current DTO); `scheduleContextType`: `ChecklistCareContextType` (no field annotation in current DTO); `scheduleEndMode`: `ChecklistScheduleEndMode` (no field annotation in current DTO); `weekBoundaryRule`: `ChecklistWeekBoundaryRule` (no field annotation in current DTO); `eligibilityStartInclusive`: `Integer` (no field annotation in current DTO); `eligibilityEndInclusive`: `Integer` (no field annotation in current DTO); `checklistQuarantineReasonCode`: `String` (no field annotation in current DTO); `approvedAt`: `Instant` (no field annotation in current DTO); `approvedBy`: `UUID` (no field annotation in current DTO); `migrationReviewedAt`: `Instant` (no field annotation in current DTO); `migrationReviewedBy`: `UUID` (no field annotation in current DTO); `provenance`: `ChecklistProvenanceResponse` (no field annotation in current DTO); `items`: `List<ChecklistItemResponse>` (no field annotation in current DTO); `latestReviewFeedback`: `ReviewFeedbackResponse` (no field annotation in current DTO) |
| Explicit/documented statuses | `201` |
| Positive test mapping | `COND-API-009` / `UC-AD-10-TC-API-009` |
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
| `VG-01` | Create or load a checklist template/version. | `COND-01` | `UC-AD-10-TC-001` |
| `VG-02` | Edit tasks/rules or clone/import validated content. | `COND-02` | `UC-AD-10-TC-002` |
| `VG-03` | Save/submit/archive using an allowed author lifecycle action. | `COND-03` | `UC-AD-10-TC-003` |
| `VG-AUTH` | Reject wrong authentication/role/ownership/membership/consent scope | `COND-AUTH` | `UC-AD-10-TC-SEC-001` |
| `VG-GAP` | Characterize each known gap without claiming a false completed path | `COND-GAP` | `UC-AD-10-TC-GAP-001` |

## 14. Verification Methods

Existing focused evidence:

- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/content/AdminChecklistTemplateControllerTest.java`
- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/content/AdminChecklistTemplateServiceImplTest.java`
- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/checklist/ChecklistImportControllerTest.java`

Exact supported commands derived from the audited test paths:

- `cd 05_Development/CareBridgeAPI && ./mvnw -Dtest=AdminChecklistTemplateControllerTest test`
- `cd 05_Development/CareBridgeAPI && ./mvnw -Dtest=AdminChecklistTemplateServiceImplTest test`
- `cd 05_Development/CareBridgeAPI && ./mvnw -Dtest=ChecklistImportControllerTest test`

Record pass/fail/skip counts only after executing these commands on the exact revision.

## 15. Verification Samples

| Sample | Value |
| --- | --- |
| Primary contract | Checklist-template CRUD/clone/import/archive endpoints under `/api/v1/admin/checklist-templates/**` |
| Request | `GET /api/v1/admin/checklist-templates` → `list`; `None` with Not applicable — no request body; authorization: hasAnyRole('CONTENT_ADMIN','SYSTEM_ADMIN'). |
| Success response | `ResponseEntity<ApiResponse<Page<AdminChecklistTemplateDetailResponse>>>` with `id`: `UUID` (no field annotation in current DTO); `name`: `String` (no field annotation in current DTO); `stage`: `ContentStage` (no field annotation in current DTO); `displayOrder`: `Integer` (no field annotation in current DTO); `status`: `ChecklistTemplateStatus` (no field annotation in current DTO); `description`: `String` (no field annotation in current DTO); `versionNo`: `Integer` (no field annotation in current DTO); `lineageId`: `UUID` (no field annotation in current DTO); `versionId`: `UUID` (no field annotation in current DTO); `recipientRoles`: `List<ChecklistRecipientRole>` (no field annotation in current DTO); `substage`: `ChecklistSubstageResponse` (no field annotation in current DTO); `migrationReviewRequired`: `Boolean` (no field annotation in current DTO); `distributionEnabled`: `Boolean` (no field annotation in current DTO); `templateType`: `ChecklistTemplateType` (no field annotation in current DTO); `checklistContractVersion`: `Short` (no field annotation in current DTO); `planNumber`: `Integer` (no field annotation in current DTO); `section`: `String` (no field annotation in current DTO); `scheduleType`: `ChecklistScheduleType` (no field annotation in current DTO); `materializationPolicy`: `ChecklistMaterializationPolicy` (no field annotation in current DTO); `scheduleGroupKey`: `String` (no field annotation in current DTO); `scheduleContextType`: `ChecklistCareContextType` (no field annotation in current DTO); `scheduleEndMode`: `ChecklistScheduleEndMode` (no field annotation in current DTO); `weekBoundaryRule`: `ChecklistWeekBoundaryRule` (no field annotation in current DTO); `eligibilityStartInclusive`: `Integer` (no field annotation in current DTO); `eligibilityEndInclusive`: `Integer` (no field annotation in current DTO); `checklistQuarantineReasonCode`: `String` (no field annotation in current DTO); `approvedAt`: `Instant` (no field annotation in current DTO); `approvedBy`: `UUID` (no field annotation in current DTO); `migrationReviewedAt`: `Instant` (no field annotation in current DTO); `migrationReviewedBy`: `UUID` (no field annotation in current DTO); `provenance`: `ChecklistProvenanceResponse` (no field annotation in current DTO); `items`: `List<ChecklistItemResponse>` (no field annotation in current DTO); `latestReviewFeedback`: `ReviewFeedbackResponse` (no field annotation in current DTO); explicit/documented statuses `200`. |
| Negative sample | Use wrong role/owner/state and assert the exact mapped error without protected payload. |

## 16. Authorization Matrix

| Actor / role | Operation | Decision |
| --- | --- | --- |
| Content Admin | `GET /api/v1/admin/checklist-templates` | hasAnyRole('CONTENT_ADMIN','SYSTEM_ADMIN'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/AdminChecklistTemplateController.java` |
| Content Admin | `POST /api/v1/admin/checklist-templates` | hasRole('CONTENT_ADMIN'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/AdminChecklistTemplateController.java` |
| Content Admin | `POST /api/v1/admin/checklist-templates/import-batch` | hasRole('CONTENT_ADMIN'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/AdminChecklistTemplateController.java` |
| Content Admin | `GET /api/v1/admin/checklist-templates/{id}` | hasAnyRole('CONTENT_ADMIN','SYSTEM_ADMIN'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/AdminChecklistTemplateController.java` |
| Content Admin | `PUT /api/v1/admin/checklist-templates/{id}` | hasRole('CONTENT_ADMIN'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/AdminChecklistTemplateController.java` |
| Content Admin | `POST /api/v1/admin/checklist-templates/{id}/archive` | hasRole('CONTENT_ADMIN'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/AdminChecklistTemplateController.java` |
| Content Admin | `POST /api/v1/admin/checklist-templates/{id}/clone` | hasRole('CONTENT_ADMIN'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/AdminChecklistTemplateController.java` |
| Content Admin | `GET /api/v1/admin/checklist-templates/{id}/versions` | hasAnyRole('CONTENT_ADMIN','SYSTEM_ADMIN'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/AdminChecklistTemplateController.java` |
| Content Admin | `POST /api/v1/admin/checklist-templates/{lineageId}/versions/{versionId}/clone` | hasRole('CONTENT_ADMIN'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/AdminChecklistTemplateController.java` |
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
