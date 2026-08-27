# ENGINEERING DOCUMENTATION STANDARD (EDS) v2.0

# TECHNICAL DESIGN SPECIFICATION — Verify Experts and Credentials

| Field | Value |
| --- | --- |
| Document ID | `UC-AD-06-TDS` |
| Version | `0.1` |
| Date | `2026-08-23` |
| Status | `Draft` |
| Function ID | `UC-AD-06` |
| Canonical Use Case | `UC-AD-06 — Verify Experts and Credentials` |
| Module / Bounded Context | `Administration and Operations` |
| Primary Actor | `System Admin / Authorized Reviewer` |
| Platforms | `Web / Backend / File Storage` |
| Priority | `Medium` |
| Data Classification | `Confidential administrative configuration/audit/moderation data; Restricted account, identity, credential, and report evidence where applicable` |
| Compliance Scope | `Least-privilege administration, immutable/auditable decisions, reason capture, protected-evidence minimization, and secret redaction` |
| Owner | `CareBridge Team` |
| Reviewer / Approver |  |
| Source Baseline | Current worktree on `2026-08-23`; SRS `UC-AD-06`; exact evidence in Section 1.4 |

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

- **Goal:** Review expert profiles, identity and credential evidence, record review decisions, and approve/reject/trust eligible experts.
- **Trigger:** The actor enters Web `/admin/experts*` and `/admin/expert-verification-queue`.
- **Outcome:** Record the eligible decision and resulting profile/trust state.
- **Current state:** `High` confidence from reachable code/test audit; no manifest-level exclusion is recorded.
- **Target state:** Preserve current code-backed behavior and resolve only explicitly evidenced limitations through approved implementation work.

### 1.2 Scope

**In scope**

- Web `/admin/experts*` and `/admin/expert-verification-queue`

- GET `/api/v1/expert/admin/profiles`
- GET `/api/v1/expert/review-cases`
- GET `/api/v1/expert/review-cases/{expertProfileId}`
- GET `/api/v1/expert/identity/pending`
- GET `/api/v1/expert/identity/files/{fileId}/url`
- PUT `/api/v1/expert/identity/{attemptId}/review`
- GET `/api/v1/expert/credentials/pending`
- GET `/api/v1/expert/credentials/{credentialId}`
- GET `/api/v1/expert/credentials/{credentialId}/preview`
- GET `/api/v1/expert/credentials/{credentialId}/file`
- PUT `/api/v1/expert/credentials/{credentialId}/review`
- POST `/api/v1/expert/profiles/{expertProfileId}/approve`
- POST `/api/v1/expert/profiles/{expertProfileId}/reject`
- PATCH `/api/v1/expert/profiles/{expertProfileId}/trust`
- PATCH `/api/v1/expert/profiles/{expertProfileId}/expert-type`

**Out of scope / limitations**

- Not applicable — no manifest-level exclusion is recorded. Scope is limited to the entry points and exact contracts listed above; unrelated handlers in the same module are excluded.

### 1.3 Preconditions and Postconditions

| Type | Condition |
| --- | --- |
| Precondition | System Admin / Authorized Reviewer is authenticated/authorized where the current contract requires it. |
| Precondition | Required ownership, membership, consent, resource state, and device/provider prerequisites pass current policies. |
| Postcondition | Record the eligible decision and resulting profile/trust state. |
| Postcondition | No side effect outside the feature-owned persistence/event/provider boundary occurs. |

### 1.4 Evidence Baseline

| Source ID | Type | Exact path | Revision | Authority |
| --- | --- | --- | --- | --- |
| `SRC-SRS-01` | Requirement | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-AD-06 | 2026-08-23 | Draft code-first requirement |
| `SRC-CODE-01` | Current code | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/controller/ExpertProfileController.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-02` | Current code | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertIdentityVerificationController.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-03` | Current code | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertCredentialController.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-04` | Current code | `05_Development/CareBridgeWebApp/src/features/expert/pages/ExpertVerificationQueuePage.tsx` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-01` | Existing test | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/expertverification/ExpertIdentityVerificationServiceTest.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-02` | Existing test | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/expertverification/ExpertCredentialPreviewServiceTest.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-03` | Existing test | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/expertverification/registry/RegistryMatcherTest.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-04` | Existing test | `05_Development/CareBridgeWebApp/src/features/expert/pages/ExpertVerificationQueuePage.test.tsx` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-ERROR-01` | Current exception advice | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/common/exception/GlobalExceptionHandler.java` | Worktree `2026-08-23` | Current-state evidence |

### 1.5 Open Contradictions / Questions

- No material contradiction was found among the exact sources cited in Section 1.4. Behavior not evidenced by those sources is not approved or implied by this Draft.

## 2. Traceability Matrix

| Requirement | Behavior | Exact oracle source | Component | Test condition / case |
| --- | --- | --- | --- | --- |
| `UC-AD-06-FR-01` | Load the verification queue and open an applicant case. | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-AD-06 Normal Flow 1 | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/controller/ExpertProfileController.java` | `COND-01` / `UC-AD-06-TC-001` |
| `UC-AD-06-FR-02` | Review purpose-authorized identity/credential evidence and registry results. | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-AD-06 Normal Flow 2 | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertCredentialController.java` | `COND-02` / `UC-AD-06-TC-002` |
| `UC-AD-06-FR-03` | Record the eligible decision and resulting profile/trust state. | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-AD-06 Normal Flow 3 | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertCredentialController.java` | `COND-03` / `UC-AD-06-TC-003` |
| `BR-01` | Backend verification state, not UI state, grants expert eligibility. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/controller/ExpertProfileController.java` | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/controller/ExpertProfileController.java` | `COND-BR-01` / `UC-AD-06-TC-BR-001` |
| `BR-02` | Sensitive evidence access is purpose-bound and review decisions are audited. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertCredentialController.java` | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertCredentialController.java` | `COND-BR-02` / `UC-AD-06-TC-BR-002` |

## 3. Architecture Decision Records (ADR)

### ADR-UC-AD-06-01 — Use a distinct actor-goal boundary

| Item | Decision |
| --- | --- |
| Context | The retired 43-UC catalogue grouped multiple triggers, lifecycles, and permission boundaries, making implementation/test traceability generic. |
| Options | Keep the broad catalogue; split by screen; split by actor goal plus lifecycle/security boundary. |
| Decision | Use `UC-AD-06 — Verify Experts and Credentials` as the canonical boundary because its operations share the stated actor outcome and current implementation evidence. |
| Consequences | Related supporting screens/endpoints stay in one TDS; different lifecycle/actor outcomes have separate UCs. |
| Source / Status | SRS Section 3.1 and current code audit / Draft |

### ADR-UC-AD-06-02 — Preserve unknowns as Open

| Item | Decision |
| --- | --- |
| Context | Exact schema fields, SLA values, and some controller error codes are not fully evidenced by this manifest alone. |
| Decision | Do not invent them. Mark them `Open` and require the exact DTO/entity/migration/policy oracle before production-code changes. |
| Consequences | This document accurately characterizes current scope; unresolved design-changing items block implementation approval. |
| Source / Status | `create-specs` evidence discipline / Draft |

## 4. Non-Functional Requirements and SLA

| NFR | Target | Oracle Source | Verification |
| --- | --- | --- | --- |
| Authorization / isolation | All requests follow exact role/ownership/membership/consent policy in current code. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/controller/ExpertProfileController.java` | Negative security cases in paired Test-Spec |
| Protected-data handling | No secrets or unnecessary health/location/identity/conversation/file payloads in logs, fixtures, screenshots, or audit detail. | Data classification header plus exact Section 9 request/response field inventories | Log/fixture review plus security tests |
| Availability / latency | Open — no approved feature-specific numeric SLA found. | Evidence needed: approved NFR/SLA source | Measure only; do not assert a fixed threshold |
| Accessibility | Open — confirm project-standard criteria for reachable UI surfaces. | Evidence needed: approved UX/accessibility standard | Applicable Web/Mobile UI checks |
| Retry / idempotency | Apply only semantics explicitly implemented by the owning service. | No explicit lock/version/idempotency marker is evidenced in the cited implementation sources; preserve observed behavior and add characterization before changing concurrency semantics | State/duplicate/concurrency cases where applicable |

## 5. Static Modeling

### 5.1 Component Responsibilities and Change Disposition

| Exact path | Disposition | Responsibility |
| --- | --- | --- |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/controller/ExpertProfileController.java` | Reuse | Current implementation evidence for Verify Experts and Credentials; inspect the exact symbol before implementation changes. |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertIdentityVerificationController.java` | Reuse | Current implementation evidence for Verify Experts and Credentials; inspect the exact symbol before implementation changes. |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertCredentialController.java` | Reuse | Current implementation evidence for Verify Experts and Credentials; inspect the exact symbol before implementation changes. |
| `05_Development/CareBridgeWebApp/src/features/expert/pages/ExpertVerificationQueuePage.tsx` | Reuse | Current implementation evidence for Verify Experts and Credentials; inspect the exact symbol before implementation changes. |

### 5.2 Current Component Diagram

```plantuml
@startuml
skinparam classAttributeIconSize 0
class ExpertProfileController as "ExpertProfileController.java"
class ExpertIdentityVerificationController as "ExpertIdentityVerificationController.java"
ExpertProfileController --> ExpertIdentityVerificationController
class ExpertCredentialController as "ExpertCredentialController.java"
ExpertIdentityVerificationController --> ExpertCredentialController
class ExpertVerificationQueuePage as "ExpertVerificationQueuePage.tsx"
ExpertCredentialController --> ExpertVerificationQueuePage
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
Actor -> Client: Enter Verify Experts and Credentials
Client -> Domain: Load the verification queue and open an applicant case.
Domain --> Client: Result for step 1
Client -> Domain: Review purpose-authorized identity/credential evidence and registry results.
Domain --> Client: Result for step 2
Client -> Domain: Record the eligible decision and resulting profile/trust state.
Domain --> Client: Result for step 3
Client --> Actor: Render canonical outcome
@enduml
```

### 6.2 Alternative, Error, Retry, and Concurrency Flows

| Flow | Expected design behavior | Oracle |
| --- | --- | --- |
| Cancel before mutation | No unintended write or provider side effect. | SRS UC-AD-06 Alternative Flow |
| Invalid input/state | Reject with the current contract; keep canonical state unchanged. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/controller/ExpertProfileController.java` |
| Wrong actor/scope | Fail closed without protected resource disclosure. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/controller/ExpertProfileController.java` |
| Dependency failure | Use only the implemented bounded fallback/retry; never report false success. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/adapter/CompreFacePipelineAdapter.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/adapter/FaceVerificationAdapter.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/adapter/FaceVerificationResult.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/file/dto/ViewFileResponse.java` |
| Duplicate/concurrent mutation | Apply only current lock/version/idempotency semantics. | No explicit lock/version/idempotency marker is evidenced in the cited implementation sources; preserve observed behavior and add characterization before changing concurrency semantics |

### 6.3 State Model and Invariants

```plantuml
@startuml
[*] --> Eligible
Eligible --> InProgress : Load the verification queue and open an applicant case.
InProgress --> Outcome : Record the eligible decision and resulting profile/trust state.
InProgress --> Rejected : validation / authorization / state failure
Outcome --> [*]
Rejected --> Eligible : actor corrects eligible input
@enduml
```

- Backend verification state, not UI state, grants expert eligibility.
- Sensitive evidence access is purpose-bound and review decisions are audited.

## 7. Domain Event Catalog

| Direction | Event | Producer / Consumer | Payload / delivery / idempotency |
| --- | --- | --- | --- |
| Publish / consume | Current event evidence | Not applicable at this cited baseline — no event type, publisher, or listener is evidenced in the listed implementation sources | Preserve only events evidenced by the cited source set; if later call-path inspection finds none, the flow remains synchronous. |

## 8. Interface Specification

### 8.1 User / Operator Interfaces

| # | Entry point | Actor | Contract |
| ---: | --- | --- | --- |
| 1 | Web `/admin/experts*` and `/admin/expert-verification-queue` | System Admin / Authorized Reviewer | Reachable current entry point |

### 8.2 Service / Repository Interfaces

| Interface evidence | Required responsibility |
| --- | --- |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/controller/ExpertProfileController.java` | Support the mapped operations without broadening authorization or lifecycle semantics. |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertIdentityVerificationController.java` | Support the mapped operations without broadening authorization or lifecycle semantics. |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertCredentialController.java` | Support the mapped operations without broadening authorization or lifecycle semantics. |
| `05_Development/CareBridgeWebApp/src/features/expert/pages/ExpertVerificationQueuePage.tsx` | Support the mapped operations without broadening authorization or lifecycle semantics. |

## 9. API Specification

| ID | Method / path or grouped controller surface | Auth / role | Request / response / errors |
| --- | --- | --- | --- |
| `API-01` | `GET /api/v1/expert/admin/profiles` | hasRole('SYSTEM_ADMIN') | Handler `getAdminProfiles`; parameters: query `status`: `String`; query `keyword`: `String`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<List<ExpertProfileResponse>>>`; response payload fields: `expertProfileId`: `UUID` (no field annotation in current DTO); `userId`: `UUID` (no field annotation in current DTO); `displayName`: `String` (no field annotation in current DTO); `specialty`: `String` (no field annotation in current DTO); `specialtyId`: `String` (no field annotation in current DTO); `specialtyIds`: `List<String>` (no field annotation in current DTO); `professionalTitle`: `String` (no field annotation in current DTO); `experienceYears`: `Integer` (no field annotation in current DTO); `workplace`: `String` (no field annotation in current DTO); `workplaceProvinceId`: `String` (no field annotation in current DTO); `hospitalId`: `String` (no field annotation in current DTO); `consultationScope`: `String` (no field annotation in current DTO); `verificationStatus`: `VerificationStatus` (no field annotation in current DTO); `trustStatus`: `TrustStatus` (no field annotation in current DTO); `expertType`: `ExpertType` (no field annotation in current DTO); `contracted`: `boolean` (no field annotation in current DTO); `isConsultationEligible`: `boolean` (no field annotation in current DTO); `verifiedAt`: `LocalDateTime` (no field annotation in current DTO); `verifiedBy`: `UUID` (no field annotation in current DTO); `ratingAvg`: `BigDecimal` (no field annotation in current DTO); `consultationFeeVnd`: `BigDecimal` (no field annotation in current DTO); `avatarUrl`: `String` (no field annotation in current DTO); `createdAt`: `LocalDateTime` (no field annotation in current DTO); `updatedAt`: `LocalDateTime` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/controller/ExpertProfileController.java` |
| `API-02` | `GET /api/v1/expert/credentials/pending` | hasRole('SYSTEM_ADMIN') | Handler `getPendingReviews`; parameters: principal `principal`: `Principal`; query `credentialType`: `String`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<List<DocumentReviewResponse>>>`; response payload fields: `credentialId`: `UUID` (no field annotation in current DTO); `expertProfileId`: `UUID` (no field annotation in current DTO); `credentialType`: `String` (no field annotation in current DTO); `credentialNumber`: `String` (no field annotation in current DTO); `issuer`: `String` (no field annotation in current DTO); `issuedDate`: `LocalDate` (no field annotation in current DTO); `expiryDate`: `LocalDate` (no field annotation in current DTO); `fileUrl`: `String` (no field annotation in current DTO); `fileId`: `UUID` (no field annotation in current DTO); `fileName`: `String` (no field annotation in current DTO); `mimeType`: `String` (no field annotation in current DTO); `fileSizeBytes`: `Long` (no field annotation in current DTO); `createdAt`: `LocalDateTime` (no field annotation in current DTO); `reviewStatus`: `ReviewStatus` (no field annotation in current DTO); `reviewNote`: `String` (no field annotation in current DTO); `reviewedBy`: `UUID` (no field annotation in current DTO); `reviewedAt`: `LocalDateTime` (no field annotation in current DTO); `expertName`: `String` (no field annotation in current DTO); `specialty`: `String` (no field annotation in current DTO); `professionalTitle`: `String` (no field annotation in current DTO); `experienceYears`: `Integer` (no field annotation in current DTO); `workplace`: `String` (no field annotation in current DTO); `phone`: `String` (no field annotation in current DTO); `email`: `String` (no field annotation in current DTO); `ratingAvg`: `BigDecimal` (no field annotation in current DTO); `avatarUrl`: `String` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertCredentialController.java` |
| `API-03` | `GET /api/v1/expert/credentials/{credentialId}` | hasRole('EXPERT') | Handler `getCredentialDetail`; parameters: principal `principal`: `Principal`; path `credentialId`: `UUID`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<CredentialResponse>>`; response payload fields: `credentialId`: `UUID` (no field annotation in current DTO); `expertProfileId`: `UUID` (no field annotation in current DTO); `credentialType`: `String` (no field annotation in current DTO); `credentialNumber`: `String` (no field annotation in current DTO); `issuer`: `String` (no field annotation in current DTO); `issuedDate`: `LocalDate` (no field annotation in current DTO); `expiryDate`: `LocalDate` (no field annotation in current DTO); `fileUrl`: `String` (no field annotation in current DTO); `fileId`: `UUID` (no field annotation in current DTO); `reviewStatus`: `ReviewStatus` (no field annotation in current DTO); `reviewNote`: `String` (no field annotation in current DTO); `createdAt`: `LocalDateTime` (no field annotation in current DTO); `updatedAt`: `LocalDateTime` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertCredentialController.java` |
| `API-04` | `GET /api/v1/expert/credentials/{credentialId}/file` | hasRole('SYSTEM_ADMIN') | Handler `getCredentialFile`; parameters: principal `principal`: `Principal`; path `credentialId`: `UUID`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<ViewFileResponse>>`; response payload fields: `fileId`: `UUID` (no field annotation in current DTO); `originalName`: `String` (no field annotation in current DTO); `mimeType`: `String` (no field annotation in current DTO); `fileSizeBytes`: `long` (no field annotation in current DTO); `presignedUrl`: `String` (no field annotation in current DTO); `status`: `String` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertCredentialController.java` |
| `API-05` | `GET /api/v1/expert/credentials/{credentialId}/preview` | hasRole('SYSTEM_ADMIN') | Handler `previewCredential`; parameters: principal `principal`: `Principal`; path `credentialId`: `UUID`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<CredentialDocumentPreviewResponse>>`; response payload fields: No serializable fields extracted from `main/java/com/carebridge/backend/expertverification/dto/response/CredentialDocumentPreviewResponse.java`; explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertCredentialController.java` |
| `API-06` | `PUT /api/v1/expert/credentials/{credentialId}/review` | hasRole('SYSTEM_ADMIN') | Handler `reviewCredential`; parameters: principal `principal`: `Principal`; path `credentialId`: `UUID`; body `request`: `ReviewCredentialRequest`; request body: `ReviewCredentialRequest`; request fields/validation: `reviewStatus`: `ReviewStatus` (@NotNull); `reviewNote`: `String` (@Size(max = 2000)); response: `ResponseEntity<ApiResponse<DocumentReviewResponse>>`; response payload fields: `credentialId`: `UUID` (no field annotation in current DTO); `expertProfileId`: `UUID` (no field annotation in current DTO); `credentialType`: `String` (no field annotation in current DTO); `credentialNumber`: `String` (no field annotation in current DTO); `issuer`: `String` (no field annotation in current DTO); `issuedDate`: `LocalDate` (no field annotation in current DTO); `expiryDate`: `LocalDate` (no field annotation in current DTO); `fileUrl`: `String` (no field annotation in current DTO); `fileId`: `UUID` (no field annotation in current DTO); `fileName`: `String` (no field annotation in current DTO); `mimeType`: `String` (no field annotation in current DTO); `fileSizeBytes`: `Long` (no field annotation in current DTO); `createdAt`: `LocalDateTime` (no field annotation in current DTO); `reviewStatus`: `ReviewStatus` (no field annotation in current DTO); `reviewNote`: `String` (no field annotation in current DTO); `reviewedBy`: `UUID` (no field annotation in current DTO); `reviewedAt`: `LocalDateTime` (no field annotation in current DTO); `expertName`: `String` (no field annotation in current DTO); `specialty`: `String` (no field annotation in current DTO); `professionalTitle`: `String` (no field annotation in current DTO); `experienceYears`: `Integer` (no field annotation in current DTO); `workplace`: `String` (no field annotation in current DTO); `phone`: `String` (no field annotation in current DTO); `email`: `String` (no field annotation in current DTO); `ratingAvg`: `BigDecimal` (no field annotation in current DTO); `avatarUrl`: `String` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertCredentialController.java` |
| `API-07` | `GET /api/v1/expert/identity/files/{fileId}/url` | hasAnyRole('EXPERT', 'SYSTEM_ADMIN') | Handler `getIdentityFileUrl`; parameters: principal `principal`: `Principal`; path `fileId`: `UUID`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<ViewFileResponse>>`; response payload fields: `fileId`: `UUID` (no field annotation in current DTO); `originalName`: `String` (no field annotation in current DTO); `mimeType`: `String` (no field annotation in current DTO); `fileSizeBytes`: `long` (no field annotation in current DTO); `presignedUrl`: `String` (no field annotation in current DTO); `status`: `String` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertIdentityVerificationController.java` |
| `API-08` | `GET /api/v1/expert/identity/pending` | hasRole('SYSTEM_ADMIN') | Handler `getPendingReviews`; parameters: No explicit handler parameter; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<List<IdentityVerificationResponse>>>`; response payload fields: `identityVerificationId`: `UUID` (no field annotation in current DTO); `expertProfileId`: `UUID` (no field annotation in current DTO); `expertName`: `String` (no field annotation in current DTO); `expertEmail`: `String` (no field annotation in current DTO); `expertPhone`: `String` (no field annotation in current DTO); `specialty`: `String` (no field annotation in current DTO); `professionalTitle`: `String` (no field annotation in current DTO); `experienceYears`: `Integer` (no field annotation in current DTO); `workplace`: `String` (no field annotation in current DTO); `consultationScope`: `String` (no field annotation in current DTO); `selfieFileId`: `UUID` (no field annotation in current DTO); `identityFrontFileId`: `UUID` (no field annotation in current DTO); `identityBackFileId`: `UUID` (no field annotation in current DTO); `selfieCropFileId`: `UUID` (no field annotation in current DTO); `idCardCropFileId`: `UUID` (no field annotation in current DTO); `faceStatus`: `FaceVerificationStatus` (no field annotation in current DTO); `faceSimilarity`: `BigDecimal` (no field annotation in current DTO); `faceThreshold`: `BigDecimal` (no field annotation in current DTO); `providerErrorCode`: `String` (no field annotation in current DTO); `reviewStatus`: `IdentityReviewStatus` (no field annotation in current DTO); `reviewReason`: `String` (no field annotation in current DTO); `reviewedBy`: `UUID` (no field annotation in current DTO); `reviewedAt`: `Instant` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertIdentityVerificationController.java` |
| `API-09` | `PUT /api/v1/expert/identity/{attemptId}/review` | hasRole('SYSTEM_ADMIN') | Handler `review`; parameters: principal `principal`: `Principal`; path `attemptId`: `UUID`; body `request`: `ReviewIdentityRequest`; request body: `ReviewIdentityRequest`; request fields/validation: `reviewStatus`: `IdentityReviewStatus` (@NotNull); `reason`: `String` (@Size(max = 2000)); response: `ResponseEntity<ApiResponse<IdentityVerificationResponse>>`; response payload fields: `identityVerificationId`: `UUID` (no field annotation in current DTO); `expertProfileId`: `UUID` (no field annotation in current DTO); `expertName`: `String` (no field annotation in current DTO); `expertEmail`: `String` (no field annotation in current DTO); `expertPhone`: `String` (no field annotation in current DTO); `specialty`: `String` (no field annotation in current DTO); `professionalTitle`: `String` (no field annotation in current DTO); `experienceYears`: `Integer` (no field annotation in current DTO); `workplace`: `String` (no field annotation in current DTO); `consultationScope`: `String` (no field annotation in current DTO); `selfieFileId`: `UUID` (no field annotation in current DTO); `identityFrontFileId`: `UUID` (no field annotation in current DTO); `identityBackFileId`: `UUID` (no field annotation in current DTO); `selfieCropFileId`: `UUID` (no field annotation in current DTO); `idCardCropFileId`: `UUID` (no field annotation in current DTO); `faceStatus`: `FaceVerificationStatus` (no field annotation in current DTO); `faceSimilarity`: `BigDecimal` (no field annotation in current DTO); `faceThreshold`: `BigDecimal` (no field annotation in current DTO); `providerErrorCode`: `String` (no field annotation in current DTO); `reviewStatus`: `IdentityReviewStatus` (no field annotation in current DTO); `reviewReason`: `String` (no field annotation in current DTO); `reviewedBy`: `UUID` (no field annotation in current DTO); `reviewedAt`: `Instant` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertIdentityVerificationController.java` |
| `API-10` | `POST /api/v1/expert/profiles/{expertProfileId}/approve` | hasRole('SYSTEM_ADMIN') | Handler `approveExpert`; parameters: path `expertProfileId`: `UUID`; query `expertType`: `ExpertType`; principal `principal`: `Principal`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<Void>>`; response payload fields: Not applicable or unresolved from the handler import; explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/controller/ExpertProfileController.java` |
| `API-11` | `PATCH /api/v1/expert/profiles/{expertProfileId}/expert-type` | hasRole('SYSTEM_ADMIN') | Handler `setExpertType`; parameters: path `expertProfileId`: `UUID`; query `type`: `ExpertType`; principal `principal`: `Principal`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<Void>>`; response payload fields: Not applicable or unresolved from the handler import; explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/controller/ExpertProfileController.java` |
| `API-12` | `POST /api/v1/expert/profiles/{expertProfileId}/reject` | hasRole('SYSTEM_ADMIN') | Handler `rejectExpert`; parameters: path `expertProfileId`: `UUID`; principal `principal`: `Principal`; body `request`: `RejectExpertRequest`; request body: `RejectExpertRequest`; request fields/validation: `reason`: `String` (@NotBlank, @Size(max = 2000)); response: `ResponseEntity<ApiResponse<Void>>`; response payload fields: Not applicable or unresolved from the handler import; explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/controller/ExpertProfileController.java` |
| `API-13` | `PATCH /api/v1/expert/profiles/{expertProfileId}/trust` | hasAnyRole('SYSTEM_ADMIN', 'CONTENT_ADMIN') | Handler `setTrustStatus`; parameters: path `expertProfileId`: `UUID`; principal `principal`: `Principal`; query `status`: `TrustStatus`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<Void>>`; response payload fields: Not applicable or unresolved from the handler import; explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/controller/ExpertProfileController.java` |
| `API-14` | `GET /api/v1/expert/review-cases` | hasRole('SYSTEM_ADMIN') | Handler `getReviewCases`; parameters: principal `principal`: `Principal`; query `search`: `String`; query `status`: `String`; context `pageable`: `org.springframework.data.domain.Pageable`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<org.springframework.data.domain.Page<ExpertReviewCaseResponse>>>`; response payload fields: Not applicable or unresolved from the handler import; explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertIdentityVerificationController.java` |
| `API-15` | `GET /api/v1/expert/review-cases/{expertProfileId}` | hasRole('SYSTEM_ADMIN') | Handler `getReviewCase`; parameters: principal `principal`: `Principal`; path `expertProfileId`: `UUID`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<ExpertReviewCaseResponse>>`; response payload fields: No serializable fields extracted from `main/java/com/carebridge/backend/expertverification/dto/response/ExpertReviewCaseResponse.java`; explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertIdentityVerificationController.java` |

Method-level Spring handlers, authorization annotations, request DTO fields/validators, response payload fields, and explicit/documented statuses above were extracted from the cited current source. Service/advice-only application error codes are not claimed where the controller does not declare them.

### 9.1 Handler Contract — `GET /api/v1/expert/admin/profiles`

| Item | Exact current contract |
| --- | --- |
| Handler | `getAdminProfiles` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/controller/ExpertProfileController.java` |
| Authorization annotation / boundary | hasRole('SYSTEM_ADMIN') |
| Parameters | query `status`: `String`; query `keyword`: `String` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<List<ExpertProfileResponse>>>` |
| Response payload fields | `expertProfileId`: `UUID` (no field annotation in current DTO); `userId`: `UUID` (no field annotation in current DTO); `displayName`: `String` (no field annotation in current DTO); `specialty`: `String` (no field annotation in current DTO); `specialtyId`: `String` (no field annotation in current DTO); `specialtyIds`: `List<String>` (no field annotation in current DTO); `professionalTitle`: `String` (no field annotation in current DTO); `experienceYears`: `Integer` (no field annotation in current DTO); `workplace`: `String` (no field annotation in current DTO); `workplaceProvinceId`: `String` (no field annotation in current DTO); `hospitalId`: `String` (no field annotation in current DTO); `consultationScope`: `String` (no field annotation in current DTO); `verificationStatus`: `VerificationStatus` (no field annotation in current DTO); `trustStatus`: `TrustStatus` (no field annotation in current DTO); `expertType`: `ExpertType` (no field annotation in current DTO); `contracted`: `boolean` (no field annotation in current DTO); `isConsultationEligible`: `boolean` (no field annotation in current DTO); `verifiedAt`: `LocalDateTime` (no field annotation in current DTO); `verifiedBy`: `UUID` (no field annotation in current DTO); `ratingAvg`: `BigDecimal` (no field annotation in current DTO); `consultationFeeVnd`: `BigDecimal` (no field annotation in current DTO); `avatarUrl`: `String` (no field annotation in current DTO); `createdAt`: `LocalDateTime` (no field annotation in current DTO); `updatedAt`: `LocalDateTime` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-001` / `UC-AD-06-TC-API-001` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.2 Handler Contract — `GET /api/v1/expert/credentials/pending`

| Item | Exact current contract |
| --- | --- |
| Handler | `getPendingReviews` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertCredentialController.java` |
| Authorization annotation / boundary | hasRole('SYSTEM_ADMIN') |
| Parameters | principal `principal`: `Principal`; query `credentialType`: `String` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<List<DocumentReviewResponse>>>` |
| Response payload fields | `credentialId`: `UUID` (no field annotation in current DTO); `expertProfileId`: `UUID` (no field annotation in current DTO); `credentialType`: `String` (no field annotation in current DTO); `credentialNumber`: `String` (no field annotation in current DTO); `issuer`: `String` (no field annotation in current DTO); `issuedDate`: `LocalDate` (no field annotation in current DTO); `expiryDate`: `LocalDate` (no field annotation in current DTO); `fileUrl`: `String` (no field annotation in current DTO); `fileId`: `UUID` (no field annotation in current DTO); `fileName`: `String` (no field annotation in current DTO); `mimeType`: `String` (no field annotation in current DTO); `fileSizeBytes`: `Long` (no field annotation in current DTO); `createdAt`: `LocalDateTime` (no field annotation in current DTO); `reviewStatus`: `ReviewStatus` (no field annotation in current DTO); `reviewNote`: `String` (no field annotation in current DTO); `reviewedBy`: `UUID` (no field annotation in current DTO); `reviewedAt`: `LocalDateTime` (no field annotation in current DTO); `expertName`: `String` (no field annotation in current DTO); `specialty`: `String` (no field annotation in current DTO); `professionalTitle`: `String` (no field annotation in current DTO); `experienceYears`: `Integer` (no field annotation in current DTO); `workplace`: `String` (no field annotation in current DTO); `phone`: `String` (no field annotation in current DTO); `email`: `String` (no field annotation in current DTO); `ratingAvg`: `BigDecimal` (no field annotation in current DTO); `avatarUrl`: `String` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-002` / `UC-AD-06-TC-API-002` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.3 Handler Contract — `GET /api/v1/expert/credentials/{credentialId}`

| Item | Exact current contract |
| --- | --- |
| Handler | `getCredentialDetail` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertCredentialController.java` |
| Authorization annotation / boundary | hasRole('EXPERT') |
| Parameters | principal `principal`: `Principal`; path `credentialId`: `UUID` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<CredentialResponse>>` |
| Response payload fields | `credentialId`: `UUID` (no field annotation in current DTO); `expertProfileId`: `UUID` (no field annotation in current DTO); `credentialType`: `String` (no field annotation in current DTO); `credentialNumber`: `String` (no field annotation in current DTO); `issuer`: `String` (no field annotation in current DTO); `issuedDate`: `LocalDate` (no field annotation in current DTO); `expiryDate`: `LocalDate` (no field annotation in current DTO); `fileUrl`: `String` (no field annotation in current DTO); `fileId`: `UUID` (no field annotation in current DTO); `reviewStatus`: `ReviewStatus` (no field annotation in current DTO); `reviewNote`: `String` (no field annotation in current DTO); `createdAt`: `LocalDateTime` (no field annotation in current DTO); `updatedAt`: `LocalDateTime` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-003` / `UC-AD-06-TC-API-003` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.4 Handler Contract — `GET /api/v1/expert/credentials/{credentialId}/file`

| Item | Exact current contract |
| --- | --- |
| Handler | `getCredentialFile` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertCredentialController.java` |
| Authorization annotation / boundary | hasRole('SYSTEM_ADMIN') |
| Parameters | principal `principal`: `Principal`; path `credentialId`: `UUID` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<ViewFileResponse>>` |
| Response payload fields | `fileId`: `UUID` (no field annotation in current DTO); `originalName`: `String` (no field annotation in current DTO); `mimeType`: `String` (no field annotation in current DTO); `fileSizeBytes`: `long` (no field annotation in current DTO); `presignedUrl`: `String` (no field annotation in current DTO); `status`: `String` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-004` / `UC-AD-06-TC-API-004` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.5 Handler Contract — `GET /api/v1/expert/credentials/{credentialId}/preview`

| Item | Exact current contract |
| --- | --- |
| Handler | `previewCredential` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertCredentialController.java` |
| Authorization annotation / boundary | hasRole('SYSTEM_ADMIN') |
| Parameters | principal `principal`: `Principal`; path `credentialId`: `UUID` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<CredentialDocumentPreviewResponse>>` |
| Response payload fields | No serializable fields extracted from `main/java/com/carebridge/backend/expertverification/dto/response/CredentialDocumentPreviewResponse.java` |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-005` / `UC-AD-06-TC-API-005` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.6 Handler Contract — `PUT /api/v1/expert/credentials/{credentialId}/review`

| Item | Exact current contract |
| --- | --- |
| Handler | `reviewCredential` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertCredentialController.java` |
| Authorization annotation / boundary | hasRole('SYSTEM_ADMIN') |
| Parameters | principal `principal`: `Principal`; path `credentialId`: `UUID`; body `request`: `ReviewCredentialRequest` |
| Request body type | `ReviewCredentialRequest` |
| Request fields and validators | `reviewStatus`: `ReviewStatus` (@NotNull); `reviewNote`: `String` (@Size(max = 2000)) |
| Response type | `ResponseEntity<ApiResponse<DocumentReviewResponse>>` |
| Response payload fields | `credentialId`: `UUID` (no field annotation in current DTO); `expertProfileId`: `UUID` (no field annotation in current DTO); `credentialType`: `String` (no field annotation in current DTO); `credentialNumber`: `String` (no field annotation in current DTO); `issuer`: `String` (no field annotation in current DTO); `issuedDate`: `LocalDate` (no field annotation in current DTO); `expiryDate`: `LocalDate` (no field annotation in current DTO); `fileUrl`: `String` (no field annotation in current DTO); `fileId`: `UUID` (no field annotation in current DTO); `fileName`: `String` (no field annotation in current DTO); `mimeType`: `String` (no field annotation in current DTO); `fileSizeBytes`: `Long` (no field annotation in current DTO); `createdAt`: `LocalDateTime` (no field annotation in current DTO); `reviewStatus`: `ReviewStatus` (no field annotation in current DTO); `reviewNote`: `String` (no field annotation in current DTO); `reviewedBy`: `UUID` (no field annotation in current DTO); `reviewedAt`: `LocalDateTime` (no field annotation in current DTO); `expertName`: `String` (no field annotation in current DTO); `specialty`: `String` (no field annotation in current DTO); `professionalTitle`: `String` (no field annotation in current DTO); `experienceYears`: `Integer` (no field annotation in current DTO); `workplace`: `String` (no field annotation in current DTO); `phone`: `String` (no field annotation in current DTO); `email`: `String` (no field annotation in current DTO); `ratingAvg`: `BigDecimal` (no field annotation in current DTO); `avatarUrl`: `String` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-006` / `UC-AD-06-TC-API-006` |
| Negative test mapping | `COND-API-006-VAL` / `UC-AD-06-TC-API-006-VAL`; plus `COND-AUTH` for protected access |

### 9.7 Handler Contract — `GET /api/v1/expert/identity/files/{fileId}/url`

| Item | Exact current contract |
| --- | --- |
| Handler | `getIdentityFileUrl` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertIdentityVerificationController.java` |
| Authorization annotation / boundary | hasAnyRole('EXPERT', 'SYSTEM_ADMIN') |
| Parameters | principal `principal`: `Principal`; path `fileId`: `UUID` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<ViewFileResponse>>` |
| Response payload fields | `fileId`: `UUID` (no field annotation in current DTO); `originalName`: `String` (no field annotation in current DTO); `mimeType`: `String` (no field annotation in current DTO); `fileSizeBytes`: `long` (no field annotation in current DTO); `presignedUrl`: `String` (no field annotation in current DTO); `status`: `String` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-007` / `UC-AD-06-TC-API-007` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.8 Handler Contract — `GET /api/v1/expert/identity/pending`

| Item | Exact current contract |
| --- | --- |
| Handler | `getPendingReviews` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertIdentityVerificationController.java` |
| Authorization annotation / boundary | hasRole('SYSTEM_ADMIN') |
| Parameters | No explicit handler parameter |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<List<IdentityVerificationResponse>>>` |
| Response payload fields | `identityVerificationId`: `UUID` (no field annotation in current DTO); `expertProfileId`: `UUID` (no field annotation in current DTO); `expertName`: `String` (no field annotation in current DTO); `expertEmail`: `String` (no field annotation in current DTO); `expertPhone`: `String` (no field annotation in current DTO); `specialty`: `String` (no field annotation in current DTO); `professionalTitle`: `String` (no field annotation in current DTO); `experienceYears`: `Integer` (no field annotation in current DTO); `workplace`: `String` (no field annotation in current DTO); `consultationScope`: `String` (no field annotation in current DTO); `selfieFileId`: `UUID` (no field annotation in current DTO); `identityFrontFileId`: `UUID` (no field annotation in current DTO); `identityBackFileId`: `UUID` (no field annotation in current DTO); `selfieCropFileId`: `UUID` (no field annotation in current DTO); `idCardCropFileId`: `UUID` (no field annotation in current DTO); `faceStatus`: `FaceVerificationStatus` (no field annotation in current DTO); `faceSimilarity`: `BigDecimal` (no field annotation in current DTO); `faceThreshold`: `BigDecimal` (no field annotation in current DTO); `providerErrorCode`: `String` (no field annotation in current DTO); `reviewStatus`: `IdentityReviewStatus` (no field annotation in current DTO); `reviewReason`: `String` (no field annotation in current DTO); `reviewedBy`: `UUID` (no field annotation in current DTO); `reviewedAt`: `Instant` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-008` / `UC-AD-06-TC-API-008` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.9 Handler Contract — `PUT /api/v1/expert/identity/{attemptId}/review`

| Item | Exact current contract |
| --- | --- |
| Handler | `review` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertIdentityVerificationController.java` |
| Authorization annotation / boundary | hasRole('SYSTEM_ADMIN') |
| Parameters | principal `principal`: `Principal`; path `attemptId`: `UUID`; body `request`: `ReviewIdentityRequest` |
| Request body type | `ReviewIdentityRequest` |
| Request fields and validators | `reviewStatus`: `IdentityReviewStatus` (@NotNull); `reason`: `String` (@Size(max = 2000)) |
| Response type | `ResponseEntity<ApiResponse<IdentityVerificationResponse>>` |
| Response payload fields | `identityVerificationId`: `UUID` (no field annotation in current DTO); `expertProfileId`: `UUID` (no field annotation in current DTO); `expertName`: `String` (no field annotation in current DTO); `expertEmail`: `String` (no field annotation in current DTO); `expertPhone`: `String` (no field annotation in current DTO); `specialty`: `String` (no field annotation in current DTO); `professionalTitle`: `String` (no field annotation in current DTO); `experienceYears`: `Integer` (no field annotation in current DTO); `workplace`: `String` (no field annotation in current DTO); `consultationScope`: `String` (no field annotation in current DTO); `selfieFileId`: `UUID` (no field annotation in current DTO); `identityFrontFileId`: `UUID` (no field annotation in current DTO); `identityBackFileId`: `UUID` (no field annotation in current DTO); `selfieCropFileId`: `UUID` (no field annotation in current DTO); `idCardCropFileId`: `UUID` (no field annotation in current DTO); `faceStatus`: `FaceVerificationStatus` (no field annotation in current DTO); `faceSimilarity`: `BigDecimal` (no field annotation in current DTO); `faceThreshold`: `BigDecimal` (no field annotation in current DTO); `providerErrorCode`: `String` (no field annotation in current DTO); `reviewStatus`: `IdentityReviewStatus` (no field annotation in current DTO); `reviewReason`: `String` (no field annotation in current DTO); `reviewedBy`: `UUID` (no field annotation in current DTO); `reviewedAt`: `Instant` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-009` / `UC-AD-06-TC-API-009` |
| Negative test mapping | `COND-API-009-VAL` / `UC-AD-06-TC-API-009-VAL`; plus `COND-AUTH` for protected access |

### 9.10 Handler Contract — `POST /api/v1/expert/profiles/{expertProfileId}/approve`

| Item | Exact current contract |
| --- | --- |
| Handler | `approveExpert` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/controller/ExpertProfileController.java` |
| Authorization annotation / boundary | hasRole('SYSTEM_ADMIN') |
| Parameters | path `expertProfileId`: `UUID`; query `expertType`: `ExpertType`; principal `principal`: `Principal` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<Void>>` |
| Response payload fields | Not applicable or unresolved from the handler import |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-010` / `UC-AD-06-TC-API-010` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.11 Handler Contract — `PATCH /api/v1/expert/profiles/{expertProfileId}/expert-type`

| Item | Exact current contract |
| --- | --- |
| Handler | `setExpertType` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/controller/ExpertProfileController.java` |
| Authorization annotation / boundary | hasRole('SYSTEM_ADMIN') |
| Parameters | path `expertProfileId`: `UUID`; query `type`: `ExpertType`; principal `principal`: `Principal` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<Void>>` |
| Response payload fields | Not applicable or unresolved from the handler import |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-011` / `UC-AD-06-TC-API-011` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.12 Handler Contract — `POST /api/v1/expert/profiles/{expertProfileId}/reject`

| Item | Exact current contract |
| --- | --- |
| Handler | `rejectExpert` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/controller/ExpertProfileController.java` |
| Authorization annotation / boundary | hasRole('SYSTEM_ADMIN') |
| Parameters | path `expertProfileId`: `UUID`; principal `principal`: `Principal`; body `request`: `RejectExpertRequest` |
| Request body type | `RejectExpertRequest` |
| Request fields and validators | `reason`: `String` (@NotBlank, @Size(max = 2000)) |
| Response type | `ResponseEntity<ApiResponse<Void>>` |
| Response payload fields | Not applicable or unresolved from the handler import |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-012` / `UC-AD-06-TC-API-012` |
| Negative test mapping | `COND-API-012-VAL` / `UC-AD-06-TC-API-012-VAL`; plus `COND-AUTH` for protected access |

### 9.13 Handler Contract — `PATCH /api/v1/expert/profiles/{expertProfileId}/trust`

| Item | Exact current contract |
| --- | --- |
| Handler | `setTrustStatus` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/controller/ExpertProfileController.java` |
| Authorization annotation / boundary | hasAnyRole('SYSTEM_ADMIN', 'CONTENT_ADMIN') |
| Parameters | path `expertProfileId`: `UUID`; principal `principal`: `Principal`; query `status`: `TrustStatus` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<Void>>` |
| Response payload fields | Not applicable or unresolved from the handler import |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-013` / `UC-AD-06-TC-API-013` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.14 Handler Contract — `GET /api/v1/expert/review-cases`

| Item | Exact current contract |
| --- | --- |
| Handler | `getReviewCases` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertIdentityVerificationController.java` |
| Authorization annotation / boundary | hasRole('SYSTEM_ADMIN') |
| Parameters | principal `principal`: `Principal`; query `search`: `String`; query `status`: `String`; context `pageable`: `org.springframework.data.domain.Pageable` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<org.springframework.data.domain.Page<ExpertReviewCaseResponse>>>` |
| Response payload fields | Not applicable or unresolved from the handler import |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-014` / `UC-AD-06-TC-API-014` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.15 Handler Contract — `GET /api/v1/expert/review-cases/{expertProfileId}`

| Item | Exact current contract |
| --- | --- |
| Handler | `getReviewCase` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertIdentityVerificationController.java` |
| Authorization annotation / boundary | hasRole('SYSTEM_ADMIN') |
| Parameters | principal `principal`: `Principal`; path `expertProfileId`: `UUID` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<ExpertReviewCaseResponse>>` |
| Response payload fields | No serializable fields extracted from `main/java/com/carebridge/backend/expertverification/dto/response/ExpertReviewCaseResponse.java` |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-015` / `UC-AD-06-TC-API-015` |
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
| `VG-01` | Load the verification queue and open an applicant case. | `COND-01` | `UC-AD-06-TC-001` |
| `VG-02` | Review purpose-authorized identity/credential evidence and registry results. | `COND-02` | `UC-AD-06-TC-002` |
| `VG-03` | Record the eligible decision and resulting profile/trust state. | `COND-03` | `UC-AD-06-TC-003` |
| `VG-AUTH` | Reject wrong authentication/role/ownership/membership/consent scope | `COND-AUTH` | `UC-AD-06-TC-SEC-001` |
| `VG-GAP` | Characterize each known gap without claiming a false completed path | `COND-GAP` | `UC-AD-06-TC-GAP-001` |

## 14. Verification Methods

Existing focused evidence:

- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/expertverification/ExpertIdentityVerificationServiceTest.java`
- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/expertverification/ExpertCredentialPreviewServiceTest.java`
- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/expertverification/registry/RegistryMatcherTest.java`
- `05_Development/CareBridgeWebApp/src/features/expert/pages/ExpertVerificationQueuePage.test.tsx`

Exact supported commands derived from the audited test paths:

- `cd 05_Development/CareBridgeAPI && ./mvnw -Dtest=ExpertIdentityVerificationServiceTest test`
- `cd 05_Development/CareBridgeAPI && ./mvnw -Dtest=ExpertCredentialPreviewServiceTest test`
- `cd 05_Development/CareBridgeAPI && ./mvnw -Dtest=RegistryMatcherTest test`
- `cd 05_Development/CareBridgeWebApp && npm test -- src/features/expert/pages/ExpertVerificationQueuePage.test.tsx`

Record pass/fail/skip counts only after executing these commands on the exact revision.

## 15. Verification Samples

| Sample | Value |
| --- | --- |
| Primary contract | GET `/api/v1/expert/admin/profiles` |
| Request | `GET /api/v1/expert/admin/profiles` → `getAdminProfiles`; `None` with Not applicable — no request body; authorization: hasRole('SYSTEM_ADMIN'). |
| Success response | `ResponseEntity<ApiResponse<List<ExpertProfileResponse>>>` with `expertProfileId`: `UUID` (no field annotation in current DTO); `userId`: `UUID` (no field annotation in current DTO); `displayName`: `String` (no field annotation in current DTO); `specialty`: `String` (no field annotation in current DTO); `specialtyId`: `String` (no field annotation in current DTO); `specialtyIds`: `List<String>` (no field annotation in current DTO); `professionalTitle`: `String` (no field annotation in current DTO); `experienceYears`: `Integer` (no field annotation in current DTO); `workplace`: `String` (no field annotation in current DTO); `workplaceProvinceId`: `String` (no field annotation in current DTO); `hospitalId`: `String` (no field annotation in current DTO); `consultationScope`: `String` (no field annotation in current DTO); `verificationStatus`: `VerificationStatus` (no field annotation in current DTO); `trustStatus`: `TrustStatus` (no field annotation in current DTO); `expertType`: `ExpertType` (no field annotation in current DTO); `contracted`: `boolean` (no field annotation in current DTO); `isConsultationEligible`: `boolean` (no field annotation in current DTO); `verifiedAt`: `LocalDateTime` (no field annotation in current DTO); `verifiedBy`: `UUID` (no field annotation in current DTO); `ratingAvg`: `BigDecimal` (no field annotation in current DTO); `consultationFeeVnd`: `BigDecimal` (no field annotation in current DTO); `avatarUrl`: `String` (no field annotation in current DTO); `createdAt`: `LocalDateTime` (no field annotation in current DTO); `updatedAt`: `LocalDateTime` (no field annotation in current DTO); explicit/documented statuses `200`. |
| Negative sample | Use wrong role/owner/state and assert the exact mapped error without protected payload. |

## 16. Authorization Matrix

| Actor / role | Operation | Decision |
| --- | --- | --- |
| System Admin / Authorized Reviewer | `GET /api/v1/expert/admin/profiles` | hasRole('SYSTEM_ADMIN'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/controller/ExpertProfileController.java` |
| System Admin / Authorized Reviewer | `GET /api/v1/expert/credentials/pending` | hasRole('SYSTEM_ADMIN'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertCredentialController.java` |
| System Admin / Authorized Reviewer | `GET /api/v1/expert/credentials/{credentialId}` | hasRole('EXPERT'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertCredentialController.java` |
| System Admin / Authorized Reviewer | `GET /api/v1/expert/credentials/{credentialId}/file` | hasRole('SYSTEM_ADMIN'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertCredentialController.java` |
| System Admin / Authorized Reviewer | `GET /api/v1/expert/credentials/{credentialId}/preview` | hasRole('SYSTEM_ADMIN'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertCredentialController.java` |
| System Admin / Authorized Reviewer | `PUT /api/v1/expert/credentials/{credentialId}/review` | hasRole('SYSTEM_ADMIN'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertCredentialController.java` |
| System Admin / Authorized Reviewer | `GET /api/v1/expert/identity/files/{fileId}/url` | hasAnyRole('EXPERT', 'SYSTEM_ADMIN'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertIdentityVerificationController.java` |
| System Admin / Authorized Reviewer | `GET /api/v1/expert/identity/pending` | hasRole('SYSTEM_ADMIN'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertIdentityVerificationController.java` |
| System Admin / Authorized Reviewer | `PUT /api/v1/expert/identity/{attemptId}/review` | hasRole('SYSTEM_ADMIN'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertIdentityVerificationController.java` |
| System Admin / Authorized Reviewer | `POST /api/v1/expert/profiles/{expertProfileId}/approve` | hasRole('SYSTEM_ADMIN'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/controller/ExpertProfileController.java` |
| System Admin / Authorized Reviewer | `PATCH /api/v1/expert/profiles/{expertProfileId}/expert-type` | hasRole('SYSTEM_ADMIN'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/controller/ExpertProfileController.java` |
| System Admin / Authorized Reviewer | `POST /api/v1/expert/profiles/{expertProfileId}/reject` | hasRole('SYSTEM_ADMIN'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/controller/ExpertProfileController.java` |
| System Admin / Authorized Reviewer | `PATCH /api/v1/expert/profiles/{expertProfileId}/trust` | hasAnyRole('SYSTEM_ADMIN', 'CONTENT_ADMIN'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/controller/ExpertProfileController.java` |
| System Admin / Authorized Reviewer | `GET /api/v1/expert/review-cases` | hasRole('SYSTEM_ADMIN'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertIdentityVerificationController.java` |
| System Admin / Authorized Reviewer | `GET /api/v1/expert/review-cases/{expertProfileId}` | hasRole('SYSTEM_ADMIN'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertIdentityVerificationController.java` |
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
