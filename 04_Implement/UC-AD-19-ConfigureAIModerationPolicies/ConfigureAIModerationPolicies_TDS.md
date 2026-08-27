# ENGINEERING DOCUMENTATION STANDARD (EDS) v2.0

# TECHNICAL DESIGN SPECIFICATION — Configure AI Moderation Policies

| Field | Value |
| --- | --- |
| Document ID | `UC-AD-19-TDS` |
| Version | `0.1` |
| Date | `2026-08-23` |
| Status | `Draft` |
| Function ID | `UC-AD-19` |
| Canonical Use Case | `UC-AD-19 — Configure AI Moderation Policies` |
| Module / Bounded Context | `Administration and Operations` |
| Primary Actor | `System Admin` |
| Platforms | `Web / Backend / Gemini` |
| Priority | `Medium` |
| Data Classification | `Confidential administrative configuration/audit/moderation data; Restricted account, identity, credential, and report evidence where applicable` |
| Compliance Scope | `Least-privilege administration, immutable/auditable decisions, reason capture, protected-evidence minimization, and secret redaction` |
| Owner | `CareBridge Team` |
| Reviewer / Approver |  |
| Source Baseline | Current worktree on `2026-08-23`; SRS `UC-AD-19`; exact evidence in Section 1.4 |

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

- **Goal:** Manage AI moderation policy versions/status, test policy behavior, and request supported rescans without allowing the model to enforce penalties directly.
- **Trigger:** The actor enters Web `/admin/safety-rules`.
- **Outcome:** Activate or request a bounded rescan and inspect the result.
- **Current state:** `High` confidence from reachable code/test audit; no manifest-level exclusion is recorded.
- **Target state:** Preserve current code-backed behavior and resolve only explicitly evidenced limitations through approved implementation work.

### 1.2 Scope

**In scope**

- Web `/admin/safety-rules`

- Policy/status/test/rescan endpoints under `/api/v1/admin/ai-moderation/**`

**Out of scope / limitations**

- Not applicable — no manifest-level exclusion is recorded. Scope is limited to the entry points and exact contracts listed above; unrelated handlers in the same module are excluded.

### 1.3 Preconditions and Postconditions

| Type | Condition |
| --- | --- |
| Precondition | System Admin is authenticated/authorized where the current contract requires it. |
| Precondition | Required ownership, membership, consent, resource state, and device/provider prerequisites pass current policies. |
| Postcondition | Activate or request a bounded rescan and inspect the result. |
| Postcondition | No side effect outside the feature-owned persistence/event/provider boundary occurs. |

### 1.4 Evidence Baseline

| Source ID | Type | Exact path | Revision | Authority |
| --- | --- | --- | --- | --- |
| `SRC-SRS-01` | Requirement | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-AD-19 | 2026-08-23 | Draft code-first requirement |
| `SRC-CODE-01` | Current code | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/aimoderation/controller/AiModerationAdminController.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-02` | Current code | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/aimoderation/service/AiPolicyServiceImpl.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-03` | Current code | `05_Development/CareBridgeWebApp/src/features/aiRuleManagement/pages/SafetyRuleManagementPage.tsx` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-01` | Existing test | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/aimoderation/AiModerationAdminControllerSecurityTest.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-02` | Existing test | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/aimoderation/AiPolicyServiceImplTest.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-03` | Existing test | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/aimoderation/AiModerationDecisionPolicyTest.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-04` | Existing test | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/aimoderation/AiContentScanWorkerTest.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-ERROR-01` | Current exception advice | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/common/exception/GlobalExceptionHandler.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-SUPP-01` | Historical architecture/design evidence | `04_Implement/AIContentModeration/AIContentModeration_Architecture-Evidence.md` | Worktree `2026-08-23` | Historical evidence only; current code and canonical SRS/TDS override conflicts |
| `SRC-SUPP-02` | Historical verification evidence | `04_Implement/AIContentModeration/AIContentModeration_Verification-Evidence.md` | Worktree `2026-08-23` | Historical evidence only; current code and canonical SRS/TDS override conflicts |

### 1.5 Open Contradictions / Questions

- No material contradiction was found among the exact sources cited in Section 1.4. Behavior not evidenced by those sources is not approved or implied by this Draft.

## 2. Traceability Matrix

| Requirement | Behavior | Exact oracle source | Component | Test condition / case |
| --- | --- | --- | --- | --- |
| `UC-AD-19-FR-01` | Load current AI moderation policy/status. | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-AD-19 Normal Flow 1 | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/aimoderation/controller/AiModerationAdminController.java` | `COND-01` / `UC-AD-19-TC-001` |
| `UC-AD-19-FR-02` | Create/update/test an eligible policy version. | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-AD-19 Normal Flow 2 | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/aimoderation/controller/AiModerationAdminController.java` | `COND-02` / `UC-AD-19-TC-002` |
| `UC-AD-19-FR-03` | Activate or request a bounded rescan and inspect the result. | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-AD-19 Normal Flow 3 | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/aimoderation/controller/AiModerationAdminController.java` | `COND-03` / `UC-AD-19-TC-003` |
| `BR-01` | Only System Admin manages policies. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/aimoderation/service/AiPolicyServiceImpl.java` | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/aimoderation/service/AiPolicyServiceImpl.java` | `COND-BR-01` / `UC-AD-19-TC-BR-001` |
| `BR-02` | Deterministic server policy decides cases; Gemini output is advisory signal and medical symptom text is not automatically a violation. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/aimoderation/service/AiPolicyServiceImpl.java` | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/aimoderation/service/AiPolicyServiceImpl.java` | `COND-BR-02` / `UC-AD-19-TC-BR-002` |

## 3. Architecture Decision Records (ADR)

### ADR-UC-AD-19-01 — Use a distinct actor-goal boundary

| Item | Decision |
| --- | --- |
| Context | The retired 43-UC catalogue grouped multiple triggers, lifecycles, and permission boundaries, making implementation/test traceability generic. |
| Options | Keep the broad catalogue; split by screen; split by actor goal plus lifecycle/security boundary. |
| Decision | Use `UC-AD-19 — Configure AI Moderation Policies` as the canonical boundary because its operations share the stated actor outcome and current implementation evidence. |
| Consequences | Related supporting screens/endpoints stay in one TDS; different lifecycle/actor outcomes have separate UCs. |
| Source / Status | SRS Section 3.1 and current code audit / Draft |

### ADR-UC-AD-19-02 — Preserve unknowns as Open

| Item | Decision |
| --- | --- |
| Context | Exact schema fields, SLA values, and some controller error codes are not fully evidenced by this manifest alone. |
| Decision | Do not invent them. Mark them `Open` and require the exact DTO/entity/migration/policy oracle before production-code changes. |
| Consequences | This document accurately characterizes current scope; unresolved design-changing items block implementation approval. |
| Source / Status | `create-specs` evidence discipline / Draft |

## 4. Non-Functional Requirements and SLA

| NFR | Target | Oracle Source | Verification |
| --- | --- | --- | --- |
| Authorization / isolation | All requests follow exact role/ownership/membership/consent policy in current code. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/aimoderation/controller/AiModerationAdminController.java` | Negative security cases in paired Test-Spec |
| Protected-data handling | No secrets or unnecessary health/location/identity/conversation/file payloads in logs, fixtures, screenshots, or audit detail. | Data classification header plus exact Section 9 request/response field inventories | Log/fixture review plus security tests |
| Availability / latency | Open — no approved feature-specific numeric SLA found. | Evidence needed: approved NFR/SLA source | Measure only; do not assert a fixed threshold |
| Accessibility | Open — confirm project-standard criteria for reachable UI surfaces. | Evidence needed: approved UX/accessibility standard | Applicable Web/Mobile UI checks |
| Retry / idempotency | Apply only semantics explicitly implemented by the owning service. | No explicit lock/version/idempotency marker is evidenced in the cited implementation sources; preserve observed behavior and add characterization before changing concurrency semantics | State/duplicate/concurrency cases where applicable |

## 5. Static Modeling

### 5.1 Component Responsibilities and Change Disposition

| Exact path | Disposition | Responsibility |
| --- | --- | --- |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/aimoderation/controller/AiModerationAdminController.java` | Reuse | Current implementation evidence for Configure AI Moderation Policies; inspect the exact symbol before implementation changes. |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/aimoderation/service/AiPolicyServiceImpl.java` | Reuse | Current implementation evidence for Configure AI Moderation Policies; inspect the exact symbol before implementation changes. |
| `05_Development/CareBridgeWebApp/src/features/aiRuleManagement/pages/SafetyRuleManagementPage.tsx` | Reuse | Current implementation evidence for Configure AI Moderation Policies; inspect the exact symbol before implementation changes. |

### 5.2 Current Component Diagram

```plantuml
@startuml
skinparam classAttributeIconSize 0
class AiModerationAdminController as "AiModerationAdminController.java"
class AiPolicyServiceImpl as "AiPolicyServiceImpl.java"
AiModerationAdminController --> AiPolicyServiceImpl
class SafetyRuleManagementPage as "SafetyRuleManagementPage.tsx"
AiPolicyServiceImpl --> SafetyRuleManagementPage
@enduml
```

### 5.3 Data / Schema / Migration Assessment

| Item | Assessment |
| --- | --- |
| Current stores/entities | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/aimoderation/entity/AiModerationPolicy.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/aimoderation/repository/AiModerationPolicyRepository.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/entity/AuditAction.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/entity/ReportTargetType.java` |
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
Actor -> Client: Enter Configure AI Moderation Policies
Client -> Domain: Load current AI moderation policy/status.
Domain --> Client: Result for step 1
Client -> Domain: Create/update/test an eligible policy version.
Domain --> Client: Result for step 2
Client -> Domain: Activate or request a bounded rescan and inspect the result.
Domain --> Client: Result for step 3
Client --> Actor: Render canonical outcome
@enduml
```

### 6.2 Alternative, Error, Retry, and Concurrency Flows

| Flow | Expected design behavior | Oracle |
| --- | --- | --- |
| Cancel before mutation | No unintended write or provider side effect. | SRS UC-AD-19 Alternative Flow |
| Invalid input/state | Reject with the current contract; keep canonical state unchanged. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/aimoderation/controller/AiModerationAdminController.java` |
| Wrong actor/scope | Fail closed without protected resource disclosure. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/aimoderation/controller/AiModerationAdminController.java` |
| Dependency failure | Use only the implemented bounded fallback/retry; never report false success. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/integration/gemini/client/GeminiModerationClient.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/integration/gemini/exception/GeminiConfigurationException.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/integration/gemini/exception/GeminiUnavailableException.java` |
| Duplicate/concurrent mutation | Apply only current lock/version/idempotency semantics. | No explicit lock/version/idempotency marker is evidenced in the cited implementation sources; preserve observed behavior and add characterization before changing concurrency semantics |

### 6.3 State Model and Invariants

```plantuml
@startuml
[*] --> Eligible
Eligible --> InProgress : Load current AI moderation policy/status.
InProgress --> Outcome : Activate or request a bounded rescan and inspect the result.
InProgress --> Rejected : validation / authorization / state failure
Outcome --> [*]
Rejected --> Eligible : actor corrects eligible input
@enduml
```

- Only System Admin manages policies.
- Deterministic server policy decides cases; Gemini output is advisory signal and medical symptom text is not automatically a violation.

## 7. Domain Event Catalog

| Direction | Event | Producer / Consumer | Payload / delivery / idempotency |
| --- | --- | --- | --- |
| Publish / consume | Current event evidence | Not applicable at this cited baseline — no event type, publisher, or listener is evidenced in the listed implementation sources | Preserve only events evidenced by the cited source set; if later call-path inspection finds none, the flow remains synchronous. |

## 8. Interface Specification

### 8.1 User / Operator Interfaces

| # | Entry point | Actor | Contract |
| ---: | --- | --- | --- |
| 1 | Web `/admin/safety-rules` | System Admin | Reachable current entry point |

### 8.2 Service / Repository Interfaces

| Interface evidence | Required responsibility |
| --- | --- |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/aimoderation/controller/AiModerationAdminController.java` | Support the mapped operations without broadening authorization or lifecycle semantics. |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/aimoderation/service/AiPolicyServiceImpl.java` | Support the mapped operations without broadening authorization or lifecycle semantics. |
| `05_Development/CareBridgeWebApp/src/features/aiRuleManagement/pages/SafetyRuleManagementPage.tsx` | Support the mapped operations without broadening authorization or lifecycle semantics. |

## 9. API Specification

| ID | Method / path or grouped controller surface | Auth / role | Request / response / errors |
| --- | --- | --- | --- |
| `API-01` | `GET /api/v1/admin/ai-moderation/policies` | hasRole('SYSTEM_ADMIN') | Handler `listPolicies`; parameters: query `active`: `Boolean`; query `page`: `int`; query `size`: `int`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<AiPolicyPageResponse>>`; response payload fields: `content`: `List<AiPolicyResponse>` (no field annotation in current DTO); `totalElements`: `long` (no field annotation in current DTO); `page`: `int` (no field annotation in current DTO); `size`: `int` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/aimoderation/controller/AiModerationAdminController.java` |
| `API-02` | `POST /api/v1/admin/ai-moderation/policies` | hasRole('SYSTEM_ADMIN') | Handler `createPolicy`; parameters: body `request`: `CreateAiPolicyRequest`; principal `principal`: `Principal`; request body: `CreateAiPolicyRequest`; request fields/validation: `policyCode`: `String` (@NotBlank, @Pattern(regexp = "[A-Z0-9_]{3,60}")); `name`: `String` (@NotBlank, @Size(max = 150)); `detectionGuidance`: `String` (@NotBlank, @Size(max = 3000)); `violationCategory`: `AiViolationCategory` (@NotNull); `reportCategory`: `ReportCategory` (@NotNull); `severity`: `AiPolicySeverity` (@NotNull); `applicableTargetTypes`: `List<ReportTargetType>` (@NotEmpty); `confidenceThreshold`: `BigDecimal` (@NotNull); `active`: `Boolean` (no field annotation in current DTO); `referenceLinks`: `List<PolicyReferenceLink>` (no field annotation in current DTO); `referenceFiles`: `List<PolicyReferenceFile>` (no field annotation in current DTO); response: `ResponseEntity<ApiResponse<AiPolicyResponse>>`; response payload fields: `id`: `UUID` (no field annotation in current DTO); `policyCode`: `String` (no field annotation in current DTO); `name`: `String` (no field annotation in current DTO); `detectionGuidance`: `String` (no field annotation in current DTO); `violationCategory`: `AiViolationCategory` (no field annotation in current DTO); `reportCategory`: `ReportCategory` (no field annotation in current DTO); `severity`: `AiPolicySeverity` (no field annotation in current DTO); `applicableTargetTypes`: `List<ReportTargetType>` (no field annotation in current DTO); `confidenceThreshold`: `BigDecimal` (no field annotation in current DTO); `active`: `boolean` (no field annotation in current DTO); `systemDefault`: `boolean` (no field annotation in current DTO); `version`: `int` (no field annotation in current DTO); `referenceLinks`: `List<PolicyReferenceLink>` (no field annotation in current DTO); `referenceFiles`: `List<PolicyReferenceFile>` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO); `updatedAt`: `Instant` (no field annotation in current DTO); explicit/documented statuses: `201`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/aimoderation/controller/AiModerationAdminController.java` |
| `API-03` | `PUT /api/v1/admin/ai-moderation/policies/{id}` | hasRole('SYSTEM_ADMIN') | Handler `updatePolicy`; parameters: path `id`: `UUID`; body `request`: `UpdateAiPolicyRequest`; principal `principal`: `Principal`; request body: `UpdateAiPolicyRequest`; request fields/validation: `name`: `String` (@Size(max = 150)); `detectionGuidance`: `String` (@Size(max = 3000)); `violationCategory`: `AiViolationCategory` (no field annotation in current DTO); `reportCategory`: `ReportCategory` (no field annotation in current DTO); `severity`: `AiPolicySeverity` (no field annotation in current DTO); `applicableTargetTypes`: `List<ReportTargetType>` (no field annotation in current DTO); `confidenceThreshold`: `BigDecimal` (no field annotation in current DTO); `active`: `Boolean` (no field annotation in current DTO); `referenceLinks`: `List<PolicyReferenceLink>` (no field annotation in current DTO); `referenceFiles`: `List<PolicyReferenceFile>` (no field annotation in current DTO); response: `ResponseEntity<ApiResponse<AiPolicyResponse>>`; response payload fields: `id`: `UUID` (no field annotation in current DTO); `policyCode`: `String` (no field annotation in current DTO); `name`: `String` (no field annotation in current DTO); `detectionGuidance`: `String` (no field annotation in current DTO); `violationCategory`: `AiViolationCategory` (no field annotation in current DTO); `reportCategory`: `ReportCategory` (no field annotation in current DTO); `severity`: `AiPolicySeverity` (no field annotation in current DTO); `applicableTargetTypes`: `List<ReportTargetType>` (no field annotation in current DTO); `confidenceThreshold`: `BigDecimal` (no field annotation in current DTO); `active`: `boolean` (no field annotation in current DTO); `systemDefault`: `boolean` (no field annotation in current DTO); `version`: `int` (no field annotation in current DTO); `referenceLinks`: `List<PolicyReferenceLink>` (no field annotation in current DTO); `referenceFiles`: `List<PolicyReferenceFile>` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO); `updatedAt`: `Instant` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/aimoderation/controller/AiModerationAdminController.java` |
| `API-04` | `PATCH /api/v1/admin/ai-moderation/policies/{id}/status` | hasRole('SYSTEM_ADMIN') | Handler `updatePolicyStatus`; parameters: path `id`: `UUID`; body `request`: `UpdateAiPolicyStatusRequest`; principal `principal`: `Principal`; request body: `UpdateAiPolicyStatusRequest`; request fields/validation: `active`: `Boolean` (@NotNull); response: `ResponseEntity<ApiResponse<AiPolicyResponse>>`; response payload fields: `id`: `UUID` (no field annotation in current DTO); `policyCode`: `String` (no field annotation in current DTO); `name`: `String` (no field annotation in current DTO); `detectionGuidance`: `String` (no field annotation in current DTO); `violationCategory`: `AiViolationCategory` (no field annotation in current DTO); `reportCategory`: `ReportCategory` (no field annotation in current DTO); `severity`: `AiPolicySeverity` (no field annotation in current DTO); `applicableTargetTypes`: `List<ReportTargetType>` (no field annotation in current DTO); `confidenceThreshold`: `BigDecimal` (no field annotation in current DTO); `active`: `boolean` (no field annotation in current DTO); `systemDefault`: `boolean` (no field annotation in current DTO); `version`: `int` (no field annotation in current DTO); `referenceLinks`: `List<PolicyReferenceLink>` (no field annotation in current DTO); `referenceFiles`: `List<PolicyReferenceFile>` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO); `updatedAt`: `Instant` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/aimoderation/controller/AiModerationAdminController.java` |
| `API-05` | `POST /api/v1/admin/ai-moderation/rescan` | hasRole('SYSTEM_ADMIN') | Handler `rescan`; parameters: body `request`: `AiRescanRequest`; principal `principal`: `Principal`; request body: `AiRescanRequest`; request fields/validation: `targetType`: `ReportTargetType` (@NotNull); `targetId`: `UUID` (@NotNull); response: `ResponseEntity<ApiResponse<AiRescanResponse>>`; response payload fields: `jobId`: `UUID` (no field annotation in current DTO); `queued`: `boolean` (no field annotation in current DTO); explicit/documented statuses: `202`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/aimoderation/controller/AiModerationAdminController.java` |
| `API-06` | `GET /api/v1/admin/ai-moderation/status` | hasAnyRole('SYSTEM_ADMIN', 'MODERATOR') | Handler `status`; parameters: No explicit handler parameter; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<AiModerationStatusResponse>>`; response payload fields: `enabled`: `boolean` (no field annotation in current DTO); `configured`: `boolean` (no field annotation in current DTO); `model`: `String` (no field annotation in current DTO); `resolvedModel`: `String` (no field annotation in current DTO); `state`: `String` (no field annotation in current DTO); `businessToggleEnabled`: `boolean` (no field annotation in current DTO); `queuedJobs`: `long` (no field annotation in current DTO); `processingJobs`: `long` (no field annotation in current DTO); `failedJobs`: `long` (no field annotation in current DTO); `lastCompletedAt`: `Instant` (no field annotation in current DTO); `policySetHash`: `String` (no field annotation in current DTO); `activePolicies`: `long` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/aimoderation/controller/AiModerationAdminController.java` |
| `API-07` | `POST /api/v1/admin/ai-moderation/test` | hasRole('SYSTEM_ADMIN') | Handler `testPolicies`; parameters: body `request`: `AiPolicyTestRequest`; principal `principal`: `Principal`; request body: `AiPolicyTestRequest`; request fields/validation: `targetType`: `ReportTargetType` (@NotNull); `sampleText`: `String` (@NotBlank, @Size(max = 5000)); response: `ResponseEntity<ApiResponse<AiPolicyTestResponse>>`; response payload fields: `classification`: `AiClassification` (no field annotation in current DTO); `overallSeverity`: `AiPolicySeverity` (no field annotation in current DTO); `confidence`: `BigDecimal` (no field annotation in current DTO); `recommendedAction`: `AiRecommendedAction` (no field annotation in current DTO); `explanation`: `String` (no field annotation in current DTO); `matches`: `List<AiAssessmentMatchResponse>` (no field annotation in current DTO); `wouldCreateCase`: `boolean` (no field annotation in current DTO); `wouldCreatePriority`: `CasePriority` (no field annotation in current DTO); `model`: `String` (no field annotation in current DTO); `latencyMs`: `long` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/aimoderation/controller/AiModerationAdminController.java` |

Method-level Spring handlers, authorization annotations, request DTO fields/validators, response payload fields, and explicit/documented statuses above were extracted from the cited current source. Service/advice-only application error codes are not claimed where the controller does not declare them.

### 9.1 Handler Contract — `GET /api/v1/admin/ai-moderation/policies`

| Item | Exact current contract |
| --- | --- |
| Handler | `listPolicies` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/aimoderation/controller/AiModerationAdminController.java` |
| Authorization annotation / boundary | hasRole('SYSTEM_ADMIN') |
| Parameters | query `active`: `Boolean`; query `page`: `int`; query `size`: `int` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<AiPolicyPageResponse>>` |
| Response payload fields | `content`: `List<AiPolicyResponse>` (no field annotation in current DTO); `totalElements`: `long` (no field annotation in current DTO); `page`: `int` (no field annotation in current DTO); `size`: `int` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-001` / `UC-AD-19-TC-API-001` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.2 Handler Contract — `POST /api/v1/admin/ai-moderation/policies`

| Item | Exact current contract |
| --- | --- |
| Handler | `createPolicy` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/aimoderation/controller/AiModerationAdminController.java` |
| Authorization annotation / boundary | hasRole('SYSTEM_ADMIN') |
| Parameters | body `request`: `CreateAiPolicyRequest`; principal `principal`: `Principal` |
| Request body type | `CreateAiPolicyRequest` |
| Request fields and validators | `policyCode`: `String` (@NotBlank, @Pattern(regexp = "[A-Z0-9_]{3,60}")); `name`: `String` (@NotBlank, @Size(max = 150)); `detectionGuidance`: `String` (@NotBlank, @Size(max = 3000)); `violationCategory`: `AiViolationCategory` (@NotNull); `reportCategory`: `ReportCategory` (@NotNull); `severity`: `AiPolicySeverity` (@NotNull); `applicableTargetTypes`: `List<ReportTargetType>` (@NotEmpty); `confidenceThreshold`: `BigDecimal` (@NotNull); `active`: `Boolean` (no field annotation in current DTO); `referenceLinks`: `List<PolicyReferenceLink>` (no field annotation in current DTO); `referenceFiles`: `List<PolicyReferenceFile>` (no field annotation in current DTO) |
| Response type | `ResponseEntity<ApiResponse<AiPolicyResponse>>` |
| Response payload fields | `id`: `UUID` (no field annotation in current DTO); `policyCode`: `String` (no field annotation in current DTO); `name`: `String` (no field annotation in current DTO); `detectionGuidance`: `String` (no field annotation in current DTO); `violationCategory`: `AiViolationCategory` (no field annotation in current DTO); `reportCategory`: `ReportCategory` (no field annotation in current DTO); `severity`: `AiPolicySeverity` (no field annotation in current DTO); `applicableTargetTypes`: `List<ReportTargetType>` (no field annotation in current DTO); `confidenceThreshold`: `BigDecimal` (no field annotation in current DTO); `active`: `boolean` (no field annotation in current DTO); `systemDefault`: `boolean` (no field annotation in current DTO); `version`: `int` (no field annotation in current DTO); `referenceLinks`: `List<PolicyReferenceLink>` (no field annotation in current DTO); `referenceFiles`: `List<PolicyReferenceFile>` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO); `updatedAt`: `Instant` (no field annotation in current DTO) |
| Explicit/documented statuses | `201` |
| Positive test mapping | `COND-API-002` / `UC-AD-19-TC-API-002` |
| Negative test mapping | `COND-API-002-VAL` / `UC-AD-19-TC-API-002-VAL`; plus `COND-AUTH` for protected access |

### 9.3 Handler Contract — `PUT /api/v1/admin/ai-moderation/policies/{id}`

| Item | Exact current contract |
| --- | --- |
| Handler | `updatePolicy` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/aimoderation/controller/AiModerationAdminController.java` |
| Authorization annotation / boundary | hasRole('SYSTEM_ADMIN') |
| Parameters | path `id`: `UUID`; body `request`: `UpdateAiPolicyRequest`; principal `principal`: `Principal` |
| Request body type | `UpdateAiPolicyRequest` |
| Request fields and validators | `name`: `String` (@Size(max = 150)); `detectionGuidance`: `String` (@Size(max = 3000)); `violationCategory`: `AiViolationCategory` (no field annotation in current DTO); `reportCategory`: `ReportCategory` (no field annotation in current DTO); `severity`: `AiPolicySeverity` (no field annotation in current DTO); `applicableTargetTypes`: `List<ReportTargetType>` (no field annotation in current DTO); `confidenceThreshold`: `BigDecimal` (no field annotation in current DTO); `active`: `Boolean` (no field annotation in current DTO); `referenceLinks`: `List<PolicyReferenceLink>` (no field annotation in current DTO); `referenceFiles`: `List<PolicyReferenceFile>` (no field annotation in current DTO) |
| Response type | `ResponseEntity<ApiResponse<AiPolicyResponse>>` |
| Response payload fields | `id`: `UUID` (no field annotation in current DTO); `policyCode`: `String` (no field annotation in current DTO); `name`: `String` (no field annotation in current DTO); `detectionGuidance`: `String` (no field annotation in current DTO); `violationCategory`: `AiViolationCategory` (no field annotation in current DTO); `reportCategory`: `ReportCategory` (no field annotation in current DTO); `severity`: `AiPolicySeverity` (no field annotation in current DTO); `applicableTargetTypes`: `List<ReportTargetType>` (no field annotation in current DTO); `confidenceThreshold`: `BigDecimal` (no field annotation in current DTO); `active`: `boolean` (no field annotation in current DTO); `systemDefault`: `boolean` (no field annotation in current DTO); `version`: `int` (no field annotation in current DTO); `referenceLinks`: `List<PolicyReferenceLink>` (no field annotation in current DTO); `referenceFiles`: `List<PolicyReferenceFile>` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO); `updatedAt`: `Instant` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-003` / `UC-AD-19-TC-API-003` |
| Negative test mapping | `COND-API-003-VAL` / `UC-AD-19-TC-API-003-VAL`; plus `COND-AUTH` for protected access |

### 9.4 Handler Contract — `PATCH /api/v1/admin/ai-moderation/policies/{id}/status`

| Item | Exact current contract |
| --- | --- |
| Handler | `updatePolicyStatus` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/aimoderation/controller/AiModerationAdminController.java` |
| Authorization annotation / boundary | hasRole('SYSTEM_ADMIN') |
| Parameters | path `id`: `UUID`; body `request`: `UpdateAiPolicyStatusRequest`; principal `principal`: `Principal` |
| Request body type | `UpdateAiPolicyStatusRequest` |
| Request fields and validators | `active`: `Boolean` (@NotNull) |
| Response type | `ResponseEntity<ApiResponse<AiPolicyResponse>>` |
| Response payload fields | `id`: `UUID` (no field annotation in current DTO); `policyCode`: `String` (no field annotation in current DTO); `name`: `String` (no field annotation in current DTO); `detectionGuidance`: `String` (no field annotation in current DTO); `violationCategory`: `AiViolationCategory` (no field annotation in current DTO); `reportCategory`: `ReportCategory` (no field annotation in current DTO); `severity`: `AiPolicySeverity` (no field annotation in current DTO); `applicableTargetTypes`: `List<ReportTargetType>` (no field annotation in current DTO); `confidenceThreshold`: `BigDecimal` (no field annotation in current DTO); `active`: `boolean` (no field annotation in current DTO); `systemDefault`: `boolean` (no field annotation in current DTO); `version`: `int` (no field annotation in current DTO); `referenceLinks`: `List<PolicyReferenceLink>` (no field annotation in current DTO); `referenceFiles`: `List<PolicyReferenceFile>` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO); `updatedAt`: `Instant` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-004` / `UC-AD-19-TC-API-004` |
| Negative test mapping | `COND-API-004-VAL` / `UC-AD-19-TC-API-004-VAL`; plus `COND-AUTH` for protected access |

### 9.5 Handler Contract — `POST /api/v1/admin/ai-moderation/rescan`

| Item | Exact current contract |
| --- | --- |
| Handler | `rescan` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/aimoderation/controller/AiModerationAdminController.java` |
| Authorization annotation / boundary | hasRole('SYSTEM_ADMIN') |
| Parameters | body `request`: `AiRescanRequest`; principal `principal`: `Principal` |
| Request body type | `AiRescanRequest` |
| Request fields and validators | `targetType`: `ReportTargetType` (@NotNull); `targetId`: `UUID` (@NotNull) |
| Response type | `ResponseEntity<ApiResponse<AiRescanResponse>>` |
| Response payload fields | `jobId`: `UUID` (no field annotation in current DTO); `queued`: `boolean` (no field annotation in current DTO) |
| Explicit/documented statuses | `202` |
| Positive test mapping | `COND-API-005` / `UC-AD-19-TC-API-005` |
| Negative test mapping | `COND-API-005-VAL` / `UC-AD-19-TC-API-005-VAL`; plus `COND-AUTH` for protected access |

### 9.6 Handler Contract — `GET /api/v1/admin/ai-moderation/status`

| Item | Exact current contract |
| --- | --- |
| Handler | `status` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/aimoderation/controller/AiModerationAdminController.java` |
| Authorization annotation / boundary | hasAnyRole('SYSTEM_ADMIN', 'MODERATOR') |
| Parameters | No explicit handler parameter |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<AiModerationStatusResponse>>` |
| Response payload fields | `enabled`: `boolean` (no field annotation in current DTO); `configured`: `boolean` (no field annotation in current DTO); `model`: `String` (no field annotation in current DTO); `resolvedModel`: `String` (no field annotation in current DTO); `state`: `String` (no field annotation in current DTO); `businessToggleEnabled`: `boolean` (no field annotation in current DTO); `queuedJobs`: `long` (no field annotation in current DTO); `processingJobs`: `long` (no field annotation in current DTO); `failedJobs`: `long` (no field annotation in current DTO); `lastCompletedAt`: `Instant` (no field annotation in current DTO); `policySetHash`: `String` (no field annotation in current DTO); `activePolicies`: `long` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-006` / `UC-AD-19-TC-API-006` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.7 Handler Contract — `POST /api/v1/admin/ai-moderation/test`

| Item | Exact current contract |
| --- | --- |
| Handler | `testPolicies` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/aimoderation/controller/AiModerationAdminController.java` |
| Authorization annotation / boundary | hasRole('SYSTEM_ADMIN') |
| Parameters | body `request`: `AiPolicyTestRequest`; principal `principal`: `Principal` |
| Request body type | `AiPolicyTestRequest` |
| Request fields and validators | `targetType`: `ReportTargetType` (@NotNull); `sampleText`: `String` (@NotBlank, @Size(max = 5000)) |
| Response type | `ResponseEntity<ApiResponse<AiPolicyTestResponse>>` |
| Response payload fields | `classification`: `AiClassification` (no field annotation in current DTO); `overallSeverity`: `AiPolicySeverity` (no field annotation in current DTO); `confidence`: `BigDecimal` (no field annotation in current DTO); `recommendedAction`: `AiRecommendedAction` (no field annotation in current DTO); `explanation`: `String` (no field annotation in current DTO); `matches`: `List<AiAssessmentMatchResponse>` (no field annotation in current DTO); `wouldCreateCase`: `boolean` (no field annotation in current DTO); `wouldCreatePriority`: `CasePriority` (no field annotation in current DTO); `model`: `String` (no field annotation in current DTO); `latencyMs`: `long` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-007` / `UC-AD-19-TC-API-007` |
| Negative test mapping | `COND-API-007-VAL` / `UC-AD-19-TC-API-007-VAL`; plus `COND-AUTH` for protected access |

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
| `VG-01` | Load current AI moderation policy/status. | `COND-01` | `UC-AD-19-TC-001` |
| `VG-02` | Create/update/test an eligible policy version. | `COND-02` | `UC-AD-19-TC-002` |
| `VG-03` | Activate or request a bounded rescan and inspect the result. | `COND-03` | `UC-AD-19-TC-003` |
| `VG-AUTH` | Reject wrong authentication/role/ownership/membership/consent scope | `COND-AUTH` | `UC-AD-19-TC-SEC-001` |
| `VG-GAP` | Characterize each known gap without claiming a false completed path | `COND-GAP` | `UC-AD-19-TC-GAP-001` |

## 14. Verification Methods

Existing focused evidence:

- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/aimoderation/AiModerationAdminControllerSecurityTest.java`
- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/aimoderation/AiPolicyServiceImplTest.java`
- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/aimoderation/AiModerationDecisionPolicyTest.java`
- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/aimoderation/AiContentScanWorkerTest.java`

Exact supported commands derived from the audited test paths:

- `cd 05_Development/CareBridgeAPI && ./mvnw -Dtest=AiModerationAdminControllerSecurityTest test`
- `cd 05_Development/CareBridgeAPI && ./mvnw -Dtest=AiPolicyServiceImplTest test`
- `cd 05_Development/CareBridgeAPI && ./mvnw -Dtest=AiModerationDecisionPolicyTest test`
- `cd 05_Development/CareBridgeAPI && ./mvnw -Dtest=AiContentScanWorkerTest test`

Record pass/fail/skip counts only after executing these commands on the exact revision.

## 15. Verification Samples

| Sample | Value |
| --- | --- |
| Primary contract | Policy/status/test/rescan endpoints under `/api/v1/admin/ai-moderation/**` |
| Request | `GET /api/v1/admin/ai-moderation/policies` → `listPolicies`; `None` with Not applicable — no request body; authorization: hasRole('SYSTEM_ADMIN'). |
| Success response | `ResponseEntity<ApiResponse<AiPolicyPageResponse>>` with `content`: `List<AiPolicyResponse>` (no field annotation in current DTO); `totalElements`: `long` (no field annotation in current DTO); `page`: `int` (no field annotation in current DTO); `size`: `int` (no field annotation in current DTO); explicit/documented statuses `200`. |
| Negative sample | Use wrong role/owner/state and assert the exact mapped error without protected payload. |

## 16. Authorization Matrix

| Actor / role | Operation | Decision |
| --- | --- | --- |
| System Admin | `GET /api/v1/admin/ai-moderation/policies` | hasRole('SYSTEM_ADMIN'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/aimoderation/controller/AiModerationAdminController.java` |
| System Admin | `POST /api/v1/admin/ai-moderation/policies` | hasRole('SYSTEM_ADMIN'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/aimoderation/controller/AiModerationAdminController.java` |
| System Admin | `PUT /api/v1/admin/ai-moderation/policies/{id}` | hasRole('SYSTEM_ADMIN'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/aimoderation/controller/AiModerationAdminController.java` |
| System Admin | `PATCH /api/v1/admin/ai-moderation/policies/{id}/status` | hasRole('SYSTEM_ADMIN'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/aimoderation/controller/AiModerationAdminController.java` |
| System Admin | `POST /api/v1/admin/ai-moderation/rescan` | hasRole('SYSTEM_ADMIN'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/aimoderation/controller/AiModerationAdminController.java` |
| System Admin | `GET /api/v1/admin/ai-moderation/status` | hasAnyRole('SYSTEM_ADMIN', 'MODERATOR'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/aimoderation/controller/AiModerationAdminController.java` |
| System Admin | `POST /api/v1/admin/ai-moderation/test` | hasRole('SYSTEM_ADMIN'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/aimoderation/controller/AiModerationAdminController.java` |
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
