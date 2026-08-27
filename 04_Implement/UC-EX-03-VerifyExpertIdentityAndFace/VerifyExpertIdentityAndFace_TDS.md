# ENGINEERING DOCUMENTATION STANDARD (EDS) v2.0

# TECHNICAL DESIGN SPECIFICATION — Verify Expert Identity and Face

| Field | Value |
| --- | --- |
| Document ID | `UC-EX-03-TDS` |
| Version | `0.1` |
| Date | `2026-08-23` |
| Status | `Draft` |
| Function ID | `UC-EX-03` |
| Canonical Use Case | `UC-EX-03 — Verify Expert Identity and Face` |
| Module / Bounded Context | `Expert and Consultation` |
| Primary Actor | `Expert Applicant` |
| Platforms | `Mobile / Web / Backend / File Storage` |
| Priority | `Medium` |
| Data Classification | `Confidential professional and consultation data; Restricted identity/credential evidence and purpose-bound attachments` |
| Compliance Scope | `PDPA purpose limitation, least privilege, purpose-bound file access, consultation confidentiality, and consent-aware recording` |
| Owner | `CareBridge Team` |
| Reviewer / Approver |  |
| Source Baseline | Current worktree on `2026-08-23`; SRS `UC-EX-03`; exact evidence in Section 1.4 |

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

- **Goal:** Capture purpose-bound identity evidence and complete the implemented face/identity verification workflow.
- **Trigger:** The actor enters Mobile `/expert/identity`.
- **Outcome:** Receive and display the server verification state.
- **Current state:** `High` confidence from reachable code/test audit; no manifest-level exclusion is recorded.
- **Target state:** Preserve current code-backed behavior and resolve only explicitly evidenced limitations through approved implementation work.

### 1.2 Scope

**In scope**

- Mobile `/expert/identity`
- Web expert onboarding identity step

- POST `/api/v1/expert/identity`
- POST `/api/v1/expert/verify-face`
- GET `/api/v1/expert/identity/files/**`

**Out of scope / limitations**

- Not applicable — no manifest-level exclusion is recorded. Scope is limited to the entry points and exact contracts listed above; unrelated handlers in the same module are excluded.

### 1.3 Preconditions and Postconditions

| Type | Condition |
| --- | --- |
| Precondition | Expert Applicant is authenticated/authorized where the current contract requires it. |
| Precondition | Required ownership, membership, consent, resource state, and device/provider prerequisites pass current policies. |
| Postcondition | Receive and display the server verification state. |
| Postcondition | No side effect outside the feature-owned persistence/event/provider boundary occurs. |

### 1.4 Evidence Baseline

| Source ID | Type | Exact path | Revision | Authority |
| --- | --- | --- | --- | --- |
| `SRC-SRS-01` | Requirement | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-EX-03 | 2026-08-23 | Draft code-first requirement |
| `SRC-CODE-01` | Current code | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertIdentityVerificationController.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-02` | Current code | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/controller/ExpertProfileController.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-03` | Current code | `05_Development/CareBridgeMobileApp/lib/features/expert/screens/expert_identity_capture_screen.dart` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-01` | Existing test | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/expertverification/ExpertIdentityVerificationServiceTest.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-02` | Existing test | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/expertverification/DuplicateIdentityFaceServiceTest.java` | Worktree `2026-08-23` | Current-state evidence |

### 1.5 Open Contradictions / Questions

- No material contradiction was found among the exact sources cited in Section 1.4. Behavior not evidenced by those sources is not approved or implied by this Draft.

## 2. Traceability Matrix

| Requirement | Behavior | Exact oracle source | Component | Test condition / case |
| --- | --- | --- | --- | --- |
| `UC-EX-03-FR-01` | Capture the supported identity document and face evidence. | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-EX-03 Normal Flow 1 | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertIdentityVerificationController.java` | `COND-01` / `UC-EX-03-TC-001` |
| `UC-EX-03-FR-02` | Submit evidence through purpose-bound file access. | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-EX-03 Normal Flow 2 | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertIdentityVerificationController.java` | `COND-02` / `UC-EX-03-TC-002` |
| `UC-EX-03-FR-03` | Receive and display the server verification state. | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-EX-03 Normal Flow 3 | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/controller/ExpertProfileController.java` | `COND-03` / `UC-EX-03-TC-003` |
| `BR-01` | Identity files use purpose-bound authorization and are not public URLs. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/controller/ExpertProfileController.java` | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/controller/ExpertProfileController.java` | `COND-BR-01` / `UC-EX-03-TC-BR-001` |
| `BR-02` | Duplicate identity/face policy is server authoritative. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertIdentityVerificationController.java` | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertIdentityVerificationController.java` | `COND-BR-02` / `UC-EX-03-TC-BR-002` |

## 3. Architecture Decision Records (ADR)

### ADR-UC-EX-03-01 — Use a distinct actor-goal boundary

| Item | Decision |
| --- | --- |
| Context | The retired 43-UC catalogue grouped multiple triggers, lifecycles, and permission boundaries, making implementation/test traceability generic. |
| Options | Keep the broad catalogue; split by screen; split by actor goal plus lifecycle/security boundary. |
| Decision | Use `UC-EX-03 — Verify Expert Identity and Face` as the canonical boundary because its operations share the stated actor outcome and current implementation evidence. |
| Consequences | Related supporting screens/endpoints stay in one TDS; different lifecycle/actor outcomes have separate UCs. |
| Source / Status | SRS Section 3.1 and current code audit / Draft |

### ADR-UC-EX-03-02 — Preserve unknowns as Open

| Item | Decision |
| --- | --- |
| Context | Exact schema fields, SLA values, and some controller error codes are not fully evidenced by this manifest alone. |
| Decision | Do not invent them. Mark them `Open` and require the exact DTO/entity/migration/policy oracle before production-code changes. |
| Consequences | This document accurately characterizes current scope; unresolved design-changing items block implementation approval. |
| Source / Status | `create-specs` evidence discipline / Draft |

## 4. Non-Functional Requirements and SLA

| NFR | Target | Oracle Source | Verification |
| --- | --- | --- | --- |
| Authorization / isolation | All requests follow exact role/ownership/membership/consent policy in current code. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertIdentityVerificationController.java` | Negative security cases in paired Test-Spec |
| Protected-data handling | No secrets or unnecessary health/location/identity/conversation/file payloads in logs, fixtures, screenshots, or audit detail. | Data classification header plus exact Section 9 request/response field inventories | Log/fixture review plus security tests |
| Availability / latency | Open — no approved feature-specific numeric SLA found. | Evidence needed: approved NFR/SLA source | Measure only; do not assert a fixed threshold |
| Accessibility | Open — confirm project-standard criteria for reachable UI surfaces. | Evidence needed: approved UX/accessibility standard | Applicable Web/Mobile UI checks |
| Retry / idempotency | Apply only semantics explicitly implemented by the owning service. | No explicit lock/version/idempotency marker is evidenced in the cited implementation sources; preserve observed behavior and add characterization before changing concurrency semantics | State/duplicate/concurrency cases where applicable |

## 5. Static Modeling

### 5.1 Component Responsibilities and Change Disposition

| Exact path | Disposition | Responsibility |
| --- | --- | --- |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertIdentityVerificationController.java` | Reuse | Current implementation evidence for Verify Expert Identity and Face; inspect the exact symbol before implementation changes. |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/controller/ExpertProfileController.java` | Reuse | Current implementation evidence for Verify Expert Identity and Face; inspect the exact symbol before implementation changes. |
| `05_Development/CareBridgeMobileApp/lib/features/expert/screens/expert_identity_capture_screen.dart` | Reuse | Current implementation evidence for Verify Expert Identity and Face; inspect the exact symbol before implementation changes. |

### 5.2 Current Component Diagram

```plantuml
@startuml
skinparam classAttributeIconSize 0
class ExpertIdentityVerificationController as "ExpertIdentityVerificationController.java"
class ExpertProfileController as "ExpertProfileController.java"
ExpertIdentityVerificationController --> ExpertProfileController
class expert_identity_capture_screen as "expert_identity_capture_screen.dart"
ExpertProfileController --> expert_identity_capture_screen
@enduml
```

### 5.3 Data / Schema / Migration Assessment

| Item | Assessment |
| --- | --- |
| Current stores/entities | No entity/repository import is present in the cited entry/source set; follow the exact handler-to-service call path before any schema change |
| Sensitive fields | Confidential professional and consultation data; Restricted identity/credential evidence and purpose-bound attachments. Exact transport fields and validators are enumerated per handler in Section 9; entity-only fields require the cited service/entity source before a schema change. |
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
Actor -> Client: Enter Verify Expert Identity and Face
Client -> Domain: Capture the supported identity document and face evidence.
Domain --> Client: Result for step 1
Client -> Domain: Submit evidence through purpose-bound file access.
Domain --> Client: Result for step 2
Client -> Domain: Receive and display the server verification state.
Domain --> Client: Result for step 3
Client --> Actor: Render canonical outcome
@enduml
```

### 6.2 Alternative, Error, Retry, and Concurrency Flows

| Flow | Expected design behavior | Oracle |
| --- | --- | --- |
| Cancel before mutation | No unintended write or provider side effect. | SRS UC-EX-03 Alternative Flow |
| Invalid input/state | Reject with the current contract; keep canonical state unchanged. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertIdentityVerificationController.java` |
| Wrong actor/scope | Fail closed without protected resource disclosure. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertIdentityVerificationController.java` |
| Dependency failure | Use only the implemented bounded fallback/retry; never report false success. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/adapter/CompreFacePipelineAdapter.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/adapter/FaceVerificationAdapter.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/adapter/FaceVerificationResult.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/file/dto/ViewFileResponse.java` |
| Duplicate/concurrent mutation | Apply only current lock/version/idempotency semantics. | No explicit lock/version/idempotency marker is evidenced in the cited implementation sources; preserve observed behavior and add characterization before changing concurrency semantics |

### 6.3 State Model and Invariants

```plantuml
@startuml
[*] --> Eligible
Eligible --> InProgress : Capture the supported identity document and face evidence.
InProgress --> Outcome : Receive and display the server verification state.
InProgress --> Rejected : validation / authorization / state failure
Outcome --> [*]
Rejected --> Eligible : actor corrects eligible input
@enduml
```

- Identity files use purpose-bound authorization and are not public URLs.
- Duplicate identity/face policy is server authoritative.

## 7. Domain Event Catalog

| Direction | Event | Producer / Consumer | Payload / delivery / idempotency |
| --- | --- | --- | --- |
| Publish / consume | Current event evidence | Not applicable at this cited baseline — no event type, publisher, or listener is evidenced in the listed implementation sources | Preserve only events evidenced by the cited source set; if later call-path inspection finds none, the flow remains synchronous. |

## 8. Interface Specification

### 8.1 User / Operator Interfaces

| # | Entry point | Actor | Contract |
| ---: | --- | --- | --- |
| 1 | Mobile `/expert/identity` | Expert Applicant | Reachable current entry point |
| 2 | Web expert onboarding identity step | Expert Applicant | Reachable current entry point |

### 8.2 Service / Repository Interfaces

| Interface evidence | Required responsibility |
| --- | --- |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertIdentityVerificationController.java` | Support the mapped operations without broadening authorization or lifecycle semantics. |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/controller/ExpertProfileController.java` | Support the mapped operations without broadening authorization or lifecycle semantics. |
| `05_Development/CareBridgeMobileApp/lib/features/expert/screens/expert_identity_capture_screen.dart` | Support the mapped operations without broadening authorization or lifecycle semantics. |

## 9. API Specification

| ID | Method / path or grouped controller surface | Auth / role | Request / response / errors |
| --- | --- | --- | --- |
| `API-01` | `POST /api/v1/expert/identity` | hasRole('EXPERT') | Handler `submitIdentity`; parameters: principal `principal`: `Principal`; context `selfie`: `MultipartFile`; context `identityFront`: `MultipartFile`; context `identityBack`: `MultipartFile`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<IdentityVerificationResponse>>`; response payload fields: `identityVerificationId`: `UUID` (no field annotation in current DTO); `expertProfileId`: `UUID` (no field annotation in current DTO); `expertName`: `String` (no field annotation in current DTO); `expertEmail`: `String` (no field annotation in current DTO); `expertPhone`: `String` (no field annotation in current DTO); `specialty`: `String` (no field annotation in current DTO); `professionalTitle`: `String` (no field annotation in current DTO); `experienceYears`: `Integer` (no field annotation in current DTO); `workplace`: `String` (no field annotation in current DTO); `consultationScope`: `String` (no field annotation in current DTO); `selfieFileId`: `UUID` (no field annotation in current DTO); `identityFrontFileId`: `UUID` (no field annotation in current DTO); `identityBackFileId`: `UUID` (no field annotation in current DTO); `selfieCropFileId`: `UUID` (no field annotation in current DTO); `idCardCropFileId`: `UUID` (no field annotation in current DTO); `faceStatus`: `FaceVerificationStatus` (no field annotation in current DTO); `faceSimilarity`: `BigDecimal` (no field annotation in current DTO); `faceThreshold`: `BigDecimal` (no field annotation in current DTO); `providerErrorCode`: `String` (no field annotation in current DTO); `reviewStatus`: `IdentityReviewStatus` (no field annotation in current DTO); `reviewReason`: `String` (no field annotation in current DTO); `reviewedBy`: `UUID` (no field annotation in current DTO); `reviewedAt`: `Instant` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO); explicit/documented statuses: `201`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertIdentityVerificationController.java` |
| `API-02` | `GET /api/v1/expert/identity/files/{fileId}/url` | hasAnyRole('EXPERT', 'SYSTEM_ADMIN') | Handler `getIdentityFileUrl`; parameters: principal `principal`: `Principal`; path `fileId`: `UUID`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<ViewFileResponse>>`; response payload fields: `fileId`: `UUID` (no field annotation in current DTO); `originalName`: `String` (no field annotation in current DTO); `mimeType`: `String` (no field annotation in current DTO); `fileSizeBytes`: `long` (no field annotation in current DTO); `presignedUrl`: `String` (no field annotation in current DTO); `status`: `String` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertIdentityVerificationController.java` |
| `API-03` | `POST /api/v1/expert/verify-face` | hasRole('EXPERT') | Handler `verifyFace`; parameters: context `selfie`: `MultipartFile`; context `idCard`: `MultipartFile`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<FaceVerificationResult>>`; response payload fields: `status`: `FaceVerificationStatus` (no field annotation in current DTO); `similarity`: `BigDecimal` (no field annotation in current DTO); `threshold`: `BigDecimal` (no field annotation in current DTO); `providerErrorCode`: `String` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/controller/ExpertProfileController.java` |

Method-level Spring handlers, authorization annotations, request DTO fields/validators, response payload fields, and explicit/documented statuses above were extracted from the cited current source. Service/advice-only application error codes are not claimed where the controller does not declare them.

### 9.1 Handler Contract — `POST /api/v1/expert/identity`

| Item | Exact current contract |
| --- | --- |
| Handler | `submitIdentity` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertIdentityVerificationController.java` |
| Authorization annotation / boundary | hasRole('EXPERT') |
| Parameters | principal `principal`: `Principal`; context `selfie`: `MultipartFile`; context `identityFront`: `MultipartFile`; context `identityBack`: `MultipartFile` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<IdentityVerificationResponse>>` |
| Response payload fields | `identityVerificationId`: `UUID` (no field annotation in current DTO); `expertProfileId`: `UUID` (no field annotation in current DTO); `expertName`: `String` (no field annotation in current DTO); `expertEmail`: `String` (no field annotation in current DTO); `expertPhone`: `String` (no field annotation in current DTO); `specialty`: `String` (no field annotation in current DTO); `professionalTitle`: `String` (no field annotation in current DTO); `experienceYears`: `Integer` (no field annotation in current DTO); `workplace`: `String` (no field annotation in current DTO); `consultationScope`: `String` (no field annotation in current DTO); `selfieFileId`: `UUID` (no field annotation in current DTO); `identityFrontFileId`: `UUID` (no field annotation in current DTO); `identityBackFileId`: `UUID` (no field annotation in current DTO); `selfieCropFileId`: `UUID` (no field annotation in current DTO); `idCardCropFileId`: `UUID` (no field annotation in current DTO); `faceStatus`: `FaceVerificationStatus` (no field annotation in current DTO); `faceSimilarity`: `BigDecimal` (no field annotation in current DTO); `faceThreshold`: `BigDecimal` (no field annotation in current DTO); `providerErrorCode`: `String` (no field annotation in current DTO); `reviewStatus`: `IdentityReviewStatus` (no field annotation in current DTO); `reviewReason`: `String` (no field annotation in current DTO); `reviewedBy`: `UUID` (no field annotation in current DTO); `reviewedAt`: `Instant` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO) |
| Explicit/documented statuses | `201` |
| Positive test mapping | `COND-API-001` / `UC-EX-03-TC-API-001` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.2 Handler Contract — `GET /api/v1/expert/identity/files/{fileId}/url`

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
| Positive test mapping | `COND-API-002` / `UC-EX-03-TC-API-002` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.3 Handler Contract — `POST /api/v1/expert/verify-face`

| Item | Exact current contract |
| --- | --- |
| Handler | `verifyFace` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/controller/ExpertProfileController.java` |
| Authorization annotation / boundary | hasRole('EXPERT') |
| Parameters | context `selfie`: `MultipartFile`; context `idCard`: `MultipartFile` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<FaceVerificationResult>>` |
| Response payload fields | `status`: `FaceVerificationStatus` (no field annotation in current DTO); `similarity`: `BigDecimal` (no field annotation in current DTO); `threshold`: `BigDecimal` (no field annotation in current DTO); `providerErrorCode`: `String` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-003` / `UC-EX-03-TC-API-003` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

## 10. Error Codes

| Error class | HTTP / code | Trigger | Client behavior | Oracle |
| --- | --- | --- | --- | --- |
| Validation/business rejection | No 4xx declared by selected handler syntax; framework/service/advice mapping applies | Invalid field/range/state/ownership input | No write or false success; show only current mapped error | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertIdentityVerificationController.java` plus exact exception advice/service characterization |
| Authentication/authorization | `401/403` only where the security chain or handler policy maps them | Missing credential or disallowed role/scope | Fail closed with no protected response fields | Security configuration and `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertIdentityVerificationController.java` |
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
| `VG-01` | Capture the supported identity document and face evidence. | `COND-01` | `UC-EX-03-TC-001` |
| `VG-02` | Submit evidence through purpose-bound file access. | `COND-02` | `UC-EX-03-TC-002` |
| `VG-03` | Receive and display the server verification state. | `COND-03` | `UC-EX-03-TC-003` |
| `VG-AUTH` | Reject wrong authentication/role/ownership/membership/consent scope | `COND-AUTH` | `UC-EX-03-TC-SEC-001` |
| `VG-GAP` | Characterize each known gap without claiming a false completed path | `COND-GAP` | `UC-EX-03-TC-GAP-001` |

## 14. Verification Methods

Existing focused evidence:

- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/expertverification/ExpertIdentityVerificationServiceTest.java`
- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/expertverification/DuplicateIdentityFaceServiceTest.java`

Exact supported commands derived from the audited test paths:

- `cd 05_Development/CareBridgeAPI && ./mvnw -Dtest=ExpertIdentityVerificationServiceTest test`
- `cd 05_Development/CareBridgeAPI && ./mvnw -Dtest=DuplicateIdentityFaceServiceTest test`

Record pass/fail/skip counts only after executing these commands on the exact revision.

## 15. Verification Samples

| Sample | Value |
| --- | --- |
| Primary contract | POST `/api/v1/expert/identity` |
| Request | `POST /api/v1/expert/identity` → `submitIdentity`; `None` with Not applicable — no request body; authorization: hasRole('EXPERT'). |
| Success response | `ResponseEntity<ApiResponse<IdentityVerificationResponse>>` with `identityVerificationId`: `UUID` (no field annotation in current DTO); `expertProfileId`: `UUID` (no field annotation in current DTO); `expertName`: `String` (no field annotation in current DTO); `expertEmail`: `String` (no field annotation in current DTO); `expertPhone`: `String` (no field annotation in current DTO); `specialty`: `String` (no field annotation in current DTO); `professionalTitle`: `String` (no field annotation in current DTO); `experienceYears`: `Integer` (no field annotation in current DTO); `workplace`: `String` (no field annotation in current DTO); `consultationScope`: `String` (no field annotation in current DTO); `selfieFileId`: `UUID` (no field annotation in current DTO); `identityFrontFileId`: `UUID` (no field annotation in current DTO); `identityBackFileId`: `UUID` (no field annotation in current DTO); `selfieCropFileId`: `UUID` (no field annotation in current DTO); `idCardCropFileId`: `UUID` (no field annotation in current DTO); `faceStatus`: `FaceVerificationStatus` (no field annotation in current DTO); `faceSimilarity`: `BigDecimal` (no field annotation in current DTO); `faceThreshold`: `BigDecimal` (no field annotation in current DTO); `providerErrorCode`: `String` (no field annotation in current DTO); `reviewStatus`: `IdentityReviewStatus` (no field annotation in current DTO); `reviewReason`: `String` (no field annotation in current DTO); `reviewedBy`: `UUID` (no field annotation in current DTO); `reviewedAt`: `Instant` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO); explicit/documented statuses `201`. |
| Negative sample | Use wrong role/owner/state and assert the exact mapped error without protected payload. |

## 16. Authorization Matrix

| Actor / role | Operation | Decision |
| --- | --- | --- |
| Expert Applicant | `POST /api/v1/expert/identity` | hasRole('EXPERT'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertIdentityVerificationController.java` |
| Expert Applicant | `GET /api/v1/expert/identity/files/{fileId}/url` | hasAnyRole('EXPERT', 'SYSTEM_ADMIN'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertIdentityVerificationController.java` |
| Expert Applicant | `POST /api/v1/expert/verify-face` | hasRole('EXPERT'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/controller/ExpertProfileController.java` |
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
