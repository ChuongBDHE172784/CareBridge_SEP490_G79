# ENGINEERING DOCUMENTATION STANDARD (EDS) v2.0

# TECHNICAL DESIGN SPECIFICATION — Manage Posture Analysis Configuration

| Field | Value |
| --- | --- |
| Document ID | `UC-AD-13-TDS` |
| Version | `0.1` |
| Date | `2026-08-23` |
| Status | `Draft` |
| Function ID | `UC-AD-13` |
| Canonical Use Case | `UC-AD-13 — Manage Posture Analysis Configuration` |
| Module / Bounded Context | `Administration and Operations` |
| Primary Actor | `System Admin` |
| Platforms | `Web / Backend` |
| Priority | `Medium` |
| Data Classification | `Confidential administrative configuration/audit/moderation data; Restricted account, identity, credential, and report evidence where applicable` |
| Compliance Scope | `Least-privilege administration, immutable/auditable decisions, reason capture, protected-evidence minimization, and secret redaction` |
| Owner | `CareBridge Team` |
| Reviewer / Approver |  |
| Source Baseline | Current worktree on `2026-08-23`; SRS `UC-AD-13`; exact evidence in Section 1.4 |

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

- **Goal:** Create, version, validate, activate, and retire posture-analysis configuration associated with exercises.
- **Trigger:** The actor enters Web `/admin/posture-configs*`.
- **Outcome:** Activate or retire an eligible version through the guarded lifecycle.
- **Current state:** `High` confidence from reachable code/test audit; no manifest-level exclusion is recorded.
- **Target state:** Preserve current code-backed behavior and resolve only explicitly evidenced limitations through approved implementation work.

### 1.2 Scope

**In scope**

- Web `/admin/posture-configs*`

- Admin posture-config endpoints under `/api/v1/admin/posture-configs/**`

**Out of scope / limitations**

- Not applicable — no manifest-level exclusion is recorded. Scope is limited to the entry points and exact contracts listed above; unrelated handlers in the same module are excluded.

### 1.3 Preconditions and Postconditions

| Type | Condition |
| --- | --- |
| Precondition | System Admin is authenticated/authorized where the current contract requires it. |
| Precondition | Required ownership, membership, consent, resource state, and device/provider prerequisites pass current policies. |
| Postcondition | Activate or retire an eligible version through the guarded lifecycle. |
| Postcondition | No side effect outside the feature-owned persistence/event/provider boundary occurs. |

### 1.4 Evidence Baseline

| Source ID | Type | Exact path | Revision | Authority |
| --- | --- | --- | --- | --- |
| `SRC-SRS-01` | Requirement | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-AD-13 | 2026-08-23 | Draft code-first requirement |
| `SRC-CODE-01` | Current code | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/AdminPostureConfigController.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-02` | Current code | `05_Development/CareBridgeWebApp/src/features/postureConfiguration/pages/PostureConfigListPage.tsx` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-03` | Current code | `05_Development/CareBridgeWebApp/src/features/postureConfiguration/pages/EditPostureConfigPage.tsx` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-01` | Existing test | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/exercise/AdminPostureConfigControllerSecurityTest.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-02` | Existing test | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/exercise/PostureConfigLifecycleIntegrationTest.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-03` | Existing test | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/exercise/PostureConfigServiceTest.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-ERROR-01` | Current exception advice | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/common/exception/GlobalExceptionHandler.java` | Worktree `2026-08-23` | Current-state evidence |

### 1.5 Open Contradictions / Questions

- No material contradiction was found among the exact sources cited in Section 1.4. Behavior not evidenced by those sources is not approved or implied by this Draft.

## 2. Traceability Matrix

| Requirement | Behavior | Exact oracle source | Component | Test condition / case |
| --- | --- | --- | --- | --- |
| `UC-AD-13-FR-01` | Load an exercise's posture configurations. | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-AD-13 Normal Flow 1 | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/AdminPostureConfigController.java` | `COND-01` / `UC-AD-13-TC-001` |
| `UC-AD-13-FR-02` | Create/edit a validated version. | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-AD-13 Normal Flow 2 | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/AdminPostureConfigController.java` | `COND-02` / `UC-AD-13-TC-002` |
| `UC-AD-13-FR-03` | Activate or retire an eligible version through the guarded lifecycle. | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-AD-13 Normal Flow 3 | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/AdminPostureConfigController.java` | `COND-03` / `UC-AD-13-TC-003` |
| `BR-01` | Only System Admin may mutate posture configuration. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/AdminPostureConfigController.java` | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/AdminPostureConfigController.java` | `COND-BR-01` / `UC-AD-13-TC-BR-001` |
| `BR-02` | Version uniqueness, activation exclusivity, compatibility, and lifecycle state are server authoritative. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/AdminPostureConfigController.java` | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/AdminPostureConfigController.java` | `COND-BR-02` / `UC-AD-13-TC-BR-002` |

## 3. Architecture Decision Records (ADR)

### ADR-UC-AD-13-01 — Use a distinct actor-goal boundary

| Item | Decision |
| --- | --- |
| Context | The retired 43-UC catalogue grouped multiple triggers, lifecycles, and permission boundaries, making implementation/test traceability generic. |
| Options | Keep the broad catalogue; split by screen; split by actor goal plus lifecycle/security boundary. |
| Decision | Use `UC-AD-13 — Manage Posture Analysis Configuration` as the canonical boundary because its operations share the stated actor outcome and current implementation evidence. |
| Consequences | Related supporting screens/endpoints stay in one TDS; different lifecycle/actor outcomes have separate UCs. |
| Source / Status | SRS Section 3.1 and current code audit / Draft |

### ADR-UC-AD-13-02 — Preserve unknowns as Open

| Item | Decision |
| --- | --- |
| Context | Exact schema fields, SLA values, and some controller error codes are not fully evidenced by this manifest alone. |
| Decision | Do not invent them. Mark them `Open` and require the exact DTO/entity/migration/policy oracle before production-code changes. |
| Consequences | This document accurately characterizes current scope; unresolved design-changing items block implementation approval. |
| Source / Status | `create-specs` evidence discipline / Draft |

## 4. Non-Functional Requirements and SLA

| NFR | Target | Oracle Source | Verification |
| --- | --- | --- | --- |
| Authorization / isolation | All requests follow exact role/ownership/membership/consent policy in current code. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/AdminPostureConfigController.java` | Negative security cases in paired Test-Spec |
| Protected-data handling | No secrets or unnecessary health/location/identity/conversation/file payloads in logs, fixtures, screenshots, or audit detail. | Data classification header plus exact Section 9 request/response field inventories | Log/fixture review plus security tests |
| Availability / latency | Open — no approved feature-specific numeric SLA found. | Evidence needed: approved NFR/SLA source | Measure only; do not assert a fixed threshold |
| Accessibility | Open — confirm project-standard criteria for reachable UI surfaces. | Evidence needed: approved UX/accessibility standard | Applicable Web/Mobile UI checks |
| Retry / idempotency | Apply only semantics explicitly implemented by the owning service. | No explicit lock/version/idempotency marker is evidenced in the cited implementation sources; preserve observed behavior and add characterization before changing concurrency semantics | State/duplicate/concurrency cases where applicable |

## 5. Static Modeling

### 5.1 Component Responsibilities and Change Disposition

| Exact path | Disposition | Responsibility |
| --- | --- | --- |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/AdminPostureConfigController.java` | Reuse | Current implementation evidence for Manage Posture Analysis Configuration; inspect the exact symbol before implementation changes. |
| `05_Development/CareBridgeWebApp/src/features/postureConfiguration/pages/PostureConfigListPage.tsx` | Reuse | Current implementation evidence for Manage Posture Analysis Configuration; inspect the exact symbol before implementation changes. |
| `05_Development/CareBridgeWebApp/src/features/postureConfiguration/pages/EditPostureConfigPage.tsx` | Reuse | Current implementation evidence for Manage Posture Analysis Configuration; inspect the exact symbol before implementation changes. |

### 5.2 Current Component Diagram

```plantuml
@startuml
skinparam classAttributeIconSize 0
class AdminPostureConfigController as "AdminPostureConfigController.java"
class PostureConfigListPage as "PostureConfigListPage.tsx"
AdminPostureConfigController --> PostureConfigListPage
class EditPostureConfigPage as "EditPostureConfigPage.tsx"
PostureConfigListPage --> EditPostureConfigPage
@enduml
```

### 5.3 Data / Schema / Migration Assessment

| Item | Assessment |
| --- | --- |
| Current stores/entities | No entity/repository import is present in the cited entry/source set; follow the exact handler-to-service call path before any schema change |
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
Actor -> Client: Enter Manage Posture Analysis Configuration
Client -> Domain: Load an exercise's posture configurations.
Domain --> Client: Result for step 1
Client -> Domain: Create/edit a validated version.
Domain --> Client: Result for step 2
Client -> Domain: Activate or retire an eligible version through the guarded lifecycle.
Domain --> Client: Result for step 3
Client --> Actor: Render canonical outcome
@enduml
```

### 6.2 Alternative, Error, Retry, and Concurrency Flows

| Flow | Expected design behavior | Oracle |
| --- | --- | --- |
| Cancel before mutation | No unintended write or provider side effect. | SRS UC-AD-13 Alternative Flow |
| Invalid input/state | Reject with the current contract; keep canonical state unchanged. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/AdminPostureConfigController.java` |
| Wrong actor/scope | Fail closed without protected resource disclosure. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/AdminPostureConfigController.java` |
| Dependency failure | Use only the implemented bounded fallback/retry; never report false success. | Not applicable at this cited baseline — no external adapter/provider import is evidenced in the listed implementation sources |
| Duplicate/concurrent mutation | Apply only current lock/version/idempotency semantics. | No explicit lock/version/idempotency marker is evidenced in the cited implementation sources; preserve observed behavior and add characterization before changing concurrency semantics |

### 6.3 State Model and Invariants

```plantuml
@startuml
[*] --> Eligible
Eligible --> InProgress : Load an exercise's posture configurations.
InProgress --> Outcome : Activate or retire an eligible version through the guarded lifecycle.
InProgress --> Rejected : validation / authorization / state failure
Outcome --> [*]
Rejected --> Eligible : actor corrects eligible input
@enduml
```

- Only System Admin may mutate posture configuration.
- Version uniqueness, activation exclusivity, compatibility, and lifecycle state are server authoritative.

## 7. Domain Event Catalog

| Direction | Event | Producer / Consumer | Payload / delivery / idempotency |
| --- | --- | --- | --- |
| Publish / consume | Current event evidence | Not applicable at this cited baseline — no event type, publisher, or listener is evidenced in the listed implementation sources | Preserve only events evidenced by the cited source set; if later call-path inspection finds none, the flow remains synchronous. |

## 8. Interface Specification

### 8.1 User / Operator Interfaces

| # | Entry point | Actor | Contract |
| ---: | --- | --- | --- |
| 1 | Web `/admin/posture-configs*` | System Admin | Reachable current entry point |

### 8.2 Service / Repository Interfaces

| Interface evidence | Required responsibility |
| --- | --- |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/AdminPostureConfigController.java` | Support the mapped operations without broadening authorization or lifecycle semantics. |
| `05_Development/CareBridgeWebApp/src/features/postureConfiguration/pages/PostureConfigListPage.tsx` | Support the mapped operations without broadening authorization or lifecycle semantics. |
| `05_Development/CareBridgeWebApp/src/features/postureConfiguration/pages/EditPostureConfigPage.tsx` | Support the mapped operations without broadening authorization or lifecycle semantics. |

## 9. API Specification

| ID | Method / path or grouped controller surface | Auth / role | Request / response / errors |
| --- | --- | --- | --- |
| `API-01` | `POST /api/v1/admin/posture-configs` | hasRole('SYSTEM_ADMIN') | Handler `createConfig`; parameters: body `request`: `CreatePostureConfigRequest`; principal `principal`: `Principal`; request body: `CreatePostureConfigRequest`; request fields/validation: `exerciseId`: `UUID` (@NotNull); `analysisMode`: `AnalysisMode` (@NotNull); `ruleOrModelVersion`: `String` (@Size(max = 80)); `confidenceThreshold`: `BigDecimal` (@NotNull, @DecimalMin(value = "0.0"), @DecimalMax(value = "1.0")); `feedbackLevel`: `PostureFeedbackLevel` (no field annotation in current DTO); `configJson`: `String` (no field annotation in current DTO); response: `ResponseEntity<ApiResponse<AdminPostureConfigResponse>>`; response payload fields: `postureConfigId`: `UUID` (no field annotation in current DTO); `exerciseId`: `UUID` (no field annotation in current DTO); `configuredBy`: `UUID` (no field annotation in current DTO); `analysisMode`: `String` (no field annotation in current DTO); `ruleOrModelVersion`: `String` (no field annotation in current DTO); `confidenceThreshold`: `BigDecimal` (no field annotation in current DTO); `feedbackLevel`: `String` (no field annotation in current DTO); `configJson`: `String` (no field annotation in current DTO); `effectiveFrom`: `OffsetDateTime` (no field annotation in current DTO); `effectiveTo`: `OffsetDateTime` (no field annotation in current DTO); `status`: `String` (no field annotation in current DTO); `createdAt`: `OffsetDateTime` (no field annotation in current DTO); `updatedAt`: `OffsetDateTime` (no field annotation in current DTO); explicit/documented statuses: `201`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/AdminPostureConfigController.java` |
| `API-02` | `GET /api/v1/admin/posture-configs/{exerciseId}` | hasRole('SYSTEM_ADMIN') | Handler `listVersions`; parameters: path `exerciseId`: `UUID`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<List<AdminPostureConfigResponse>>>`; response payload fields: `postureConfigId`: `UUID` (no field annotation in current DTO); `exerciseId`: `UUID` (no field annotation in current DTO); `configuredBy`: `UUID` (no field annotation in current DTO); `analysisMode`: `String` (no field annotation in current DTO); `ruleOrModelVersion`: `String` (no field annotation in current DTO); `confidenceThreshold`: `BigDecimal` (no field annotation in current DTO); `feedbackLevel`: `String` (no field annotation in current DTO); `configJson`: `String` (no field annotation in current DTO); `effectiveFrom`: `OffsetDateTime` (no field annotation in current DTO); `effectiveTo`: `OffsetDateTime` (no field annotation in current DTO); `status`: `String` (no field annotation in current DTO); `createdAt`: `OffsetDateTime` (no field annotation in current DTO); `updatedAt`: `OffsetDateTime` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/AdminPostureConfigController.java` |
| `API-03` | `POST /api/v1/admin/posture-configs/{exerciseId}/versions` | hasRole('SYSTEM_ADMIN') | Handler `createNewVersion`; parameters: path `exerciseId`: `UUID`; body `request`: `UpdatePostureConfigRequest`; principal `principal`: `Principal`; request body: `UpdatePostureConfigRequest`; request fields/validation: `analysisMode`: `AnalysisMode` (@NotNull); `ruleOrModelVersion`: `String` (@Size(max = 80)); `confidenceThreshold`: `BigDecimal` (@NotNull, @DecimalMin(value = "0.0"), @DecimalMax(value = "1.0")); `feedbackLevel`: `PostureFeedbackLevel` (no field annotation in current DTO); `configJson`: `String` (no field annotation in current DTO); response: `ResponseEntity<ApiResponse<AdminPostureConfigResponse>>`; response payload fields: `postureConfigId`: `UUID` (no field annotation in current DTO); `exerciseId`: `UUID` (no field annotation in current DTO); `configuredBy`: `UUID` (no field annotation in current DTO); `analysisMode`: `String` (no field annotation in current DTO); `ruleOrModelVersion`: `String` (no field annotation in current DTO); `confidenceThreshold`: `BigDecimal` (no field annotation in current DTO); `feedbackLevel`: `String` (no field annotation in current DTO); `configJson`: `String` (no field annotation in current DTO); `effectiveFrom`: `OffsetDateTime` (no field annotation in current DTO); `effectiveTo`: `OffsetDateTime` (no field annotation in current DTO); `status`: `String` (no field annotation in current DTO); `createdAt`: `OffsetDateTime` (no field annotation in current DTO); `updatedAt`: `OffsetDateTime` (no field annotation in current DTO); explicit/documented statuses: `201`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/AdminPostureConfigController.java` |
| `API-04` | `PATCH /api/v1/admin/posture-configs/{postureConfigId}/activate` | hasRole('SYSTEM_ADMIN') | Handler `activateVersion`; parameters: path `postureConfigId`: `UUID`; principal `principal`: `Principal`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<AdminPostureConfigResponse>>`; response payload fields: `postureConfigId`: `UUID` (no field annotation in current DTO); `exerciseId`: `UUID` (no field annotation in current DTO); `configuredBy`: `UUID` (no field annotation in current DTO); `analysisMode`: `String` (no field annotation in current DTO); `ruleOrModelVersion`: `String` (no field annotation in current DTO); `confidenceThreshold`: `BigDecimal` (no field annotation in current DTO); `feedbackLevel`: `String` (no field annotation in current DTO); `configJson`: `String` (no field annotation in current DTO); `effectiveFrom`: `OffsetDateTime` (no field annotation in current DTO); `effectiveTo`: `OffsetDateTime` (no field annotation in current DTO); `status`: `String` (no field annotation in current DTO); `createdAt`: `OffsetDateTime` (no field annotation in current DTO); `updatedAt`: `OffsetDateTime` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/AdminPostureConfigController.java` |

Method-level Spring handlers, authorization annotations, request DTO fields/validators, response payload fields, and explicit/documented statuses above were extracted from the cited current source. Service/advice-only application error codes are not claimed where the controller does not declare them.

### 9.1 Handler Contract — `POST /api/v1/admin/posture-configs`

| Item | Exact current contract |
| --- | --- |
| Handler | `createConfig` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/AdminPostureConfigController.java` |
| Authorization annotation / boundary | hasRole('SYSTEM_ADMIN') |
| Parameters | body `request`: `CreatePostureConfigRequest`; principal `principal`: `Principal` |
| Request body type | `CreatePostureConfigRequest` |
| Request fields and validators | `exerciseId`: `UUID` (@NotNull); `analysisMode`: `AnalysisMode` (@NotNull); `ruleOrModelVersion`: `String` (@Size(max = 80)); `confidenceThreshold`: `BigDecimal` (@NotNull, @DecimalMin(value = "0.0"), @DecimalMax(value = "1.0")); `feedbackLevel`: `PostureFeedbackLevel` (no field annotation in current DTO); `configJson`: `String` (no field annotation in current DTO) |
| Response type | `ResponseEntity<ApiResponse<AdminPostureConfigResponse>>` |
| Response payload fields | `postureConfigId`: `UUID` (no field annotation in current DTO); `exerciseId`: `UUID` (no field annotation in current DTO); `configuredBy`: `UUID` (no field annotation in current DTO); `analysisMode`: `String` (no field annotation in current DTO); `ruleOrModelVersion`: `String` (no field annotation in current DTO); `confidenceThreshold`: `BigDecimal` (no field annotation in current DTO); `feedbackLevel`: `String` (no field annotation in current DTO); `configJson`: `String` (no field annotation in current DTO); `effectiveFrom`: `OffsetDateTime` (no field annotation in current DTO); `effectiveTo`: `OffsetDateTime` (no field annotation in current DTO); `status`: `String` (no field annotation in current DTO); `createdAt`: `OffsetDateTime` (no field annotation in current DTO); `updatedAt`: `OffsetDateTime` (no field annotation in current DTO) |
| Explicit/documented statuses | `201` |
| Positive test mapping | `COND-API-001` / `UC-AD-13-TC-API-001` |
| Negative test mapping | `COND-API-001-VAL` / `UC-AD-13-TC-API-001-VAL`; plus `COND-AUTH` for protected access |

### 9.2 Handler Contract — `GET /api/v1/admin/posture-configs/{exerciseId}`

| Item | Exact current contract |
| --- | --- |
| Handler | `listVersions` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/AdminPostureConfigController.java` |
| Authorization annotation / boundary | hasRole('SYSTEM_ADMIN') |
| Parameters | path `exerciseId`: `UUID` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<List<AdminPostureConfigResponse>>>` |
| Response payload fields | `postureConfigId`: `UUID` (no field annotation in current DTO); `exerciseId`: `UUID` (no field annotation in current DTO); `configuredBy`: `UUID` (no field annotation in current DTO); `analysisMode`: `String` (no field annotation in current DTO); `ruleOrModelVersion`: `String` (no field annotation in current DTO); `confidenceThreshold`: `BigDecimal` (no field annotation in current DTO); `feedbackLevel`: `String` (no field annotation in current DTO); `configJson`: `String` (no field annotation in current DTO); `effectiveFrom`: `OffsetDateTime` (no field annotation in current DTO); `effectiveTo`: `OffsetDateTime` (no field annotation in current DTO); `status`: `String` (no field annotation in current DTO); `createdAt`: `OffsetDateTime` (no field annotation in current DTO); `updatedAt`: `OffsetDateTime` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-002` / `UC-AD-13-TC-API-002` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.3 Handler Contract — `POST /api/v1/admin/posture-configs/{exerciseId}/versions`

| Item | Exact current contract |
| --- | --- |
| Handler | `createNewVersion` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/AdminPostureConfigController.java` |
| Authorization annotation / boundary | hasRole('SYSTEM_ADMIN') |
| Parameters | path `exerciseId`: `UUID`; body `request`: `UpdatePostureConfigRequest`; principal `principal`: `Principal` |
| Request body type | `UpdatePostureConfigRequest` |
| Request fields and validators | `analysisMode`: `AnalysisMode` (@NotNull); `ruleOrModelVersion`: `String` (@Size(max = 80)); `confidenceThreshold`: `BigDecimal` (@NotNull, @DecimalMin(value = "0.0"), @DecimalMax(value = "1.0")); `feedbackLevel`: `PostureFeedbackLevel` (no field annotation in current DTO); `configJson`: `String` (no field annotation in current DTO) |
| Response type | `ResponseEntity<ApiResponse<AdminPostureConfigResponse>>` |
| Response payload fields | `postureConfigId`: `UUID` (no field annotation in current DTO); `exerciseId`: `UUID` (no field annotation in current DTO); `configuredBy`: `UUID` (no field annotation in current DTO); `analysisMode`: `String` (no field annotation in current DTO); `ruleOrModelVersion`: `String` (no field annotation in current DTO); `confidenceThreshold`: `BigDecimal` (no field annotation in current DTO); `feedbackLevel`: `String` (no field annotation in current DTO); `configJson`: `String` (no field annotation in current DTO); `effectiveFrom`: `OffsetDateTime` (no field annotation in current DTO); `effectiveTo`: `OffsetDateTime` (no field annotation in current DTO); `status`: `String` (no field annotation in current DTO); `createdAt`: `OffsetDateTime` (no field annotation in current DTO); `updatedAt`: `OffsetDateTime` (no field annotation in current DTO) |
| Explicit/documented statuses | `201` |
| Positive test mapping | `COND-API-003` / `UC-AD-13-TC-API-003` |
| Negative test mapping | `COND-API-003-VAL` / `UC-AD-13-TC-API-003-VAL`; plus `COND-AUTH` for protected access |

### 9.4 Handler Contract — `PATCH /api/v1/admin/posture-configs/{postureConfigId}/activate`

| Item | Exact current contract |
| --- | --- |
| Handler | `activateVersion` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/AdminPostureConfigController.java` |
| Authorization annotation / boundary | hasRole('SYSTEM_ADMIN') |
| Parameters | path `postureConfigId`: `UUID`; principal `principal`: `Principal` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<AdminPostureConfigResponse>>` |
| Response payload fields | `postureConfigId`: `UUID` (no field annotation in current DTO); `exerciseId`: `UUID` (no field annotation in current DTO); `configuredBy`: `UUID` (no field annotation in current DTO); `analysisMode`: `String` (no field annotation in current DTO); `ruleOrModelVersion`: `String` (no field annotation in current DTO); `confidenceThreshold`: `BigDecimal` (no field annotation in current DTO); `feedbackLevel`: `String` (no field annotation in current DTO); `configJson`: `String` (no field annotation in current DTO); `effectiveFrom`: `OffsetDateTime` (no field annotation in current DTO); `effectiveTo`: `OffsetDateTime` (no field annotation in current DTO); `status`: `String` (no field annotation in current DTO); `createdAt`: `OffsetDateTime` (no field annotation in current DTO); `updatedAt`: `OffsetDateTime` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-004` / `UC-AD-13-TC-API-004` |
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
| `VG-01` | Load an exercise's posture configurations. | `COND-01` | `UC-AD-13-TC-001` |
| `VG-02` | Create/edit a validated version. | `COND-02` | `UC-AD-13-TC-002` |
| `VG-03` | Activate or retire an eligible version through the guarded lifecycle. | `COND-03` | `UC-AD-13-TC-003` |
| `VG-AUTH` | Reject wrong authentication/role/ownership/membership/consent scope | `COND-AUTH` | `UC-AD-13-TC-SEC-001` |
| `VG-GAP` | Characterize each known gap without claiming a false completed path | `COND-GAP` | `UC-AD-13-TC-GAP-001` |

## 14. Verification Methods

Existing focused evidence:

- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/exercise/AdminPostureConfigControllerSecurityTest.java`
- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/exercise/PostureConfigLifecycleIntegrationTest.java`
- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/exercise/PostureConfigServiceTest.java`

Exact supported commands derived from the audited test paths:

- `cd 05_Development/CareBridgeAPI && ./mvnw -Dtest=AdminPostureConfigControllerSecurityTest test`
- `cd 05_Development/CareBridgeAPI && ./mvnw -Dtest=PostureConfigLifecycleIntegrationTest test`
- `cd 05_Development/CareBridgeAPI && ./mvnw -Dtest=PostureConfigServiceTest test`

Record pass/fail/skip counts only after executing these commands on the exact revision.

## 15. Verification Samples

| Sample | Value |
| --- | --- |
| Primary contract | Admin posture-config endpoints under `/api/v1/admin/posture-configs/**` |
| Request | `POST /api/v1/admin/posture-configs` → `createConfig`; `CreatePostureConfigRequest` with `exerciseId`: `UUID` (@NotNull); `analysisMode`: `AnalysisMode` (@NotNull); `ruleOrModelVersion`: `String` (@Size(max = 80)); `confidenceThreshold`: `BigDecimal` (@NotNull, @DecimalMin(value = "0.0"), @DecimalMax(value = "1.0")); `feedbackLevel`: `PostureFeedbackLevel` (no field annotation in current DTO); `configJson`: `String` (no field annotation in current DTO); authorization: hasRole('SYSTEM_ADMIN'). |
| Success response | `ResponseEntity<ApiResponse<AdminPostureConfigResponse>>` with `postureConfigId`: `UUID` (no field annotation in current DTO); `exerciseId`: `UUID` (no field annotation in current DTO); `configuredBy`: `UUID` (no field annotation in current DTO); `analysisMode`: `String` (no field annotation in current DTO); `ruleOrModelVersion`: `String` (no field annotation in current DTO); `confidenceThreshold`: `BigDecimal` (no field annotation in current DTO); `feedbackLevel`: `String` (no field annotation in current DTO); `configJson`: `String` (no field annotation in current DTO); `effectiveFrom`: `OffsetDateTime` (no field annotation in current DTO); `effectiveTo`: `OffsetDateTime` (no field annotation in current DTO); `status`: `String` (no field annotation in current DTO); `createdAt`: `OffsetDateTime` (no field annotation in current DTO); `updatedAt`: `OffsetDateTime` (no field annotation in current DTO); explicit/documented statuses `201`. |
| Negative sample | Use wrong role/owner/state and assert the exact mapped error without protected payload. |

## 16. Authorization Matrix

| Actor / role | Operation | Decision |
| --- | --- | --- |
| System Admin | `POST /api/v1/admin/posture-configs` | hasRole('SYSTEM_ADMIN'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/AdminPostureConfigController.java` |
| System Admin | `GET /api/v1/admin/posture-configs/{exerciseId}` | hasRole('SYSTEM_ADMIN'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/AdminPostureConfigController.java` |
| System Admin | `POST /api/v1/admin/posture-configs/{exerciseId}/versions` | hasRole('SYSTEM_ADMIN'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/AdminPostureConfigController.java` |
| System Admin | `PATCH /api/v1/admin/posture-configs/{postureConfigId}/activate` | hasRole('SYSTEM_ADMIN'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/AdminPostureConfigController.java` |
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
