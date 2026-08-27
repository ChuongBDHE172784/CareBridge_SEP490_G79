# ENGINEERING DOCUMENTATION STANDARD (EDS) v2.0

# TECHNICAL DESIGN SPECIFICATION — Create Expert Profile and Select Expert Type

| Field | Value |
| --- | --- |
| Document ID | `UC-EX-01-TDS` |
| Version | `0.1` |
| Date | `2026-08-23` |
| Status | `Draft` |
| Function ID | `UC-EX-01` |
| Canonical Use Case | `UC-EX-01 — Create Expert Profile and Select Expert Type` |
| Module / Bounded Context | `Expert and Consultation` |
| Primary Actor | `Expert Applicant` |
| Platforms | `Mobile / Web / Backend` |
| Priority | `Medium` |
| Data Classification | `Confidential professional and consultation data; Restricted identity/credential evidence and purpose-bound attachments` |
| Compliance Scope | `PDPA purpose limitation, least privilege, purpose-bound file access, consultation confidentiality, and consent-aware recording` |
| Owner | `CareBridge Team` |
| Reviewer / Approver |  |
| Source Baseline | Current worktree on `2026-08-23`; SRS `UC-EX-01`; exact evidence in Section 1.4 |

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

- **Goal:** Create the expert profile shell, select the supported expert type, load onboarding master data, and persist onboarding progress.
- **Trigger:** The actor enters Mobile `/expert-onboarding`, `/expert-profile-setup`, `/expert/type`.
- **Outcome:** Persist onboarding progress and continue to the next required step.
- **Current state:** `High` confidence from reachable code/test audit; no manifest-level exclusion is recorded.
- **Target state:** Preserve current code-backed behavior and resolve only explicitly evidenced limitations through approved implementation work.

### 1.2 Scope

**In scope**

- Mobile `/expert-onboarding`, `/expert-profile-setup`, `/expert/type`
- Web `/expert/onboarding`

- POST `/api/v1/expert/profiles`
- PATCH `/api/v1/expert/profiles/me/expert-type`
- GET `/api/v1/expert/onboarding`
- GET `/api/v1/master-data/provinces`
- GET `/api/v1/master-data/districts`
- GET `/api/v1/master-data/wards`
- GET `/api/v1/master-data/hospitals`
- GET `/api/v1/master-data/hospitals/search/trackasia`
- GET `/api/v1/master-data/hospitals/{id}`

**Out of scope / limitations**

- Not applicable — no manifest-level exclusion is recorded. Scope is limited to the entry points and exact contracts listed above; unrelated handlers in the same module are excluded.

### 1.3 Preconditions and Postconditions

| Type | Condition |
| --- | --- |
| Precondition | Expert Applicant is authenticated/authorized where the current contract requires it. |
| Precondition | Required ownership, membership, consent, resource state, and device/provider prerequisites pass current policies. |
| Postcondition | Persist onboarding progress and continue to the next required step. |
| Postcondition | No side effect outside the feature-owned persistence/event/provider boundary occurs. |

### 1.4 Evidence Baseline

| Source ID | Type | Exact path | Revision | Authority |
| --- | --- | --- | --- | --- |
| `SRC-SRS-01` | Requirement | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-EX-01 | 2026-08-23 | Draft code-first requirement |
| `SRC-CODE-01` | Current code | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/controller/ExpertProfileController.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-02` | Current code | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertIdentityVerificationController.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-03` | Current code | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/masterdata/controller/MasterDataController.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-04` | Current code | `05_Development/CareBridgeMobileApp/lib/features/expert/screens/expert_profile_setup_screen.dart` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-05` | Current code | `05_Development/CareBridgeWebApp/src/features/expert/pages/ExpertOnboardingPage.tsx` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-01` | Existing test | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/expert/controller/ExpertProfileControllerTest.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-02` | Existing test | `05_Development/CareBridgeWebApp/src/features/expert/pages/ExpertOnboardingPage.test.tsx` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-ERROR-01` | Current exception advice | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/common/exception/GlobalExceptionHandler.java` | Worktree `2026-08-23` | Current-state evidence |

### 1.5 Open Contradictions / Questions

- No material contradiction was found among the exact sources cited in Section 1.4. Behavior not evidenced by those sources is not approved or implied by this Draft.

## 2. Traceability Matrix

| Requirement | Behavior | Exact oracle source | Component | Test condition / case |
| --- | --- | --- | --- | --- |
| `UC-EX-01-FR-01` | Create or load the applicant's expert profile. | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-EX-01 Normal Flow 1 | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertIdentityVerificationController.java` | `COND-01` / `UC-EX-01-TC-001` |
| `UC-EX-01-FR-02` | Select an allowed expert type and supported location/facility master data. | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-EX-01 Normal Flow 2 | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/controller/ExpertProfileController.java` | `COND-02` / `UC-EX-01-TC-002` |
| `UC-EX-01-FR-03` | Persist onboarding progress and continue to the next required step. | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-EX-01 Normal Flow 3 | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/controller/ExpertProfileController.java` | `COND-03` / `UC-EX-01-TC-003` |
| `BR-01` | Expert type and onboarding state are server authoritative. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/controller/ExpertProfileController.java` | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/controller/ExpertProfileController.java` | `COND-BR-01` / `UC-EX-01-TC-BR-001` |
| `BR-02` | Protected expert workspaces remain gated until all required verification stages are eligible. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertIdentityVerificationController.java` | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertIdentityVerificationController.java` | `COND-BR-02` / `UC-EX-01-TC-BR-002` |

## 3. Architecture Decision Records (ADR)

### ADR-UC-EX-01-01 — Use a distinct actor-goal boundary

| Item | Decision |
| --- | --- |
| Context | The retired 43-UC catalogue grouped multiple triggers, lifecycles, and permission boundaries, making implementation/test traceability generic. |
| Options | Keep the broad catalogue; split by screen; split by actor goal plus lifecycle/security boundary. |
| Decision | Use `UC-EX-01 — Create Expert Profile and Select Expert Type` as the canonical boundary because its operations share the stated actor outcome and current implementation evidence. |
| Consequences | Related supporting screens/endpoints stay in one TDS; different lifecycle/actor outcomes have separate UCs. |
| Source / Status | SRS Section 3.1 and current code audit / Draft |

### ADR-UC-EX-01-02 — Preserve unknowns as Open

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
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/controller/ExpertProfileController.java` | Reuse | Current implementation evidence for Create Expert Profile and Select Expert Type; inspect the exact symbol before implementation changes. |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertIdentityVerificationController.java` | Reuse | Current implementation evidence for Create Expert Profile and Select Expert Type; inspect the exact symbol before implementation changes. |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/masterdata/controller/MasterDataController.java` | Reuse | Current implementation evidence for Create Expert Profile and Select Expert Type; inspect the exact symbol before implementation changes. |
| `05_Development/CareBridgeMobileApp/lib/features/expert/screens/expert_profile_setup_screen.dart` | Reuse | Current implementation evidence for Create Expert Profile and Select Expert Type; inspect the exact symbol before implementation changes. |
| `05_Development/CareBridgeWebApp/src/features/expert/pages/ExpertOnboardingPage.tsx` | Reuse | Current implementation evidence for Create Expert Profile and Select Expert Type; inspect the exact symbol before implementation changes. |

### 5.2 Current Component Diagram

```plantuml
@startuml
skinparam classAttributeIconSize 0
class ExpertProfileController as "ExpertProfileController.java"
class ExpertIdentityVerificationController as "ExpertIdentityVerificationController.java"
ExpertProfileController --> ExpertIdentityVerificationController
class MasterDataController as "MasterDataController.java"
ExpertIdentityVerificationController --> MasterDataController
class expert_profile_setup_screen as "expert_profile_setup_screen.dart"
MasterDataController --> expert_profile_setup_screen
class ExpertOnboardingPage as "ExpertOnboardingPage.tsx"
expert_profile_setup_screen --> ExpertOnboardingPage
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
Actor -> Client: Enter Create Expert Profile and Select Expert Type
Client -> Domain: Create or load the applicant's expert profile.
Domain --> Client: Result for step 1
Client -> Domain: Select an allowed expert type and supported location/facility master data.
Domain --> Client: Result for step 2
Client -> Domain: Persist onboarding progress and continue to the next required step.
Domain --> Client: Result for step 3
Client --> Actor: Render canonical outcome
@enduml
```

### 6.2 Alternative, Error, Retry, and Concurrency Flows

| Flow | Expected design behavior | Oracle |
| --- | --- | --- |
| Cancel before mutation | No unintended write or provider side effect. | SRS UC-EX-01 Alternative Flow |
| Invalid input/state | Reject with the current contract; keep canonical state unchanged. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/controller/ExpertProfileController.java` |
| Wrong actor/scope | Fail closed without protected resource disclosure. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/controller/ExpertProfileController.java` |
| Dependency failure | Use only the implemented bounded fallback/retry; never report false success. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/adapter/CompreFacePipelineAdapter.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/adapter/FaceVerificationAdapter.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/adapter/FaceVerificationResult.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/file/dto/ViewFileResponse.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/integration/trackasia/TrackAsiaPlaceDto.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/integration/trackasia/TrackAsiaService.java` |
| Duplicate/concurrent mutation | Apply only current lock/version/idempotency semantics. | No explicit lock/version/idempotency marker is evidenced in the cited implementation sources; preserve observed behavior and add characterization before changing concurrency semantics |

### 6.3 State Model and Invariants

```plantuml
@startuml
[*] --> Eligible
Eligible --> InProgress : Create or load the applicant's expert profile.
InProgress --> Outcome : Persist onboarding progress and continue to the next required step.
InProgress --> Rejected : validation / authorization / state failure
Outcome --> [*]
Rejected --> Eligible : actor corrects eligible input
@enduml
```

- Expert type and onboarding state are server authoritative.
- Protected expert workspaces remain gated until all required verification stages are eligible.

## 7. Domain Event Catalog

| Direction | Event | Producer / Consumer | Payload / delivery / idempotency |
| --- | --- | --- | --- |
| Publish / consume | Current event evidence | Not applicable at this cited baseline — no event type, publisher, or listener is evidenced in the listed implementation sources | Preserve only events evidenced by the cited source set; if later call-path inspection finds none, the flow remains synchronous. |

## 8. Interface Specification

### 8.1 User / Operator Interfaces

| # | Entry point | Actor | Contract |
| ---: | --- | --- | --- |
| 1 | Mobile `/expert-onboarding`, `/expert-profile-setup`, `/expert/type` | Expert Applicant | Reachable current entry point |
| 2 | Web `/expert/onboarding` | Expert Applicant | Reachable current entry point |

### 8.2 Service / Repository Interfaces

| Interface evidence | Required responsibility |
| --- | --- |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/controller/ExpertProfileController.java` | Support the mapped operations without broadening authorization or lifecycle semantics. |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertIdentityVerificationController.java` | Support the mapped operations without broadening authorization or lifecycle semantics. |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/masterdata/controller/MasterDataController.java` | Support the mapped operations without broadening authorization or lifecycle semantics. |
| `05_Development/CareBridgeMobileApp/lib/features/expert/screens/expert_profile_setup_screen.dart` | Support the mapped operations without broadening authorization or lifecycle semantics. |
| `05_Development/CareBridgeWebApp/src/features/expert/pages/ExpertOnboardingPage.tsx` | Support the mapped operations without broadening authorization or lifecycle semantics. |

## 9. API Specification

| ID | Method / path or grouped controller surface | Auth / role | Request / response / errors |
| --- | --- | --- | --- |
| `API-01` | `GET /api/v1/expert/onboarding` | hasRole('EXPERT') | Handler `getOnboarding`; parameters: principal `principal`: `Principal`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<ExpertOnboardingResponse>>`; response payload fields: `profileExists`: `boolean` (no field annotation in current DTO); `identityStatus`: `String` (no field annotation in current DTO); `credentialStatus`: `String` (no field annotation in current DTO); `verificationStatus`: `VerificationStatus` (no field annotation in current DTO); `expertType`: `String` (no field annotation in current DTO); `rejectionReason`: `String` (no field annotation in current DTO); `nextStep`: `String` (no field annotation in current DTO); `latestIdentityAttempt`: `IdentityVerificationResponse` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertIdentityVerificationController.java` |
| `API-02` | `POST /api/v1/expert/profiles` | hasRole('EXPERT') | Handler `createProfile`; parameters: body `request`: `CreateExpertProfileRequest`; principal `principal`: `Principal`; request body: `CreateExpertProfileRequest`; request fields/validation: `specialtyId`: `String` (@NotBlank, @Size(max = 80)); `hospitalId`: `String` (@NotBlank, @Size(max = 150)); `trackAsiaName`: `String` (@Size(max = 255)); `trackAsiaAddress`: `String` (@Size(max = 500)); `trackAsiaLat`: `Double` (no field annotation in current DTO); `trackAsiaLng`: `Double` (no field annotation in current DTO); `specialty`: `String` (@Size(max = 100)); `professionalTitle`: `String` (@Size(max = 150)); `experienceYears`: `Integer` (@Min(0), @Max(80)); `workplace`: `String` (@Size(max = 200)); `workplaceProvinceId`: `String` (@Size(max = 16, message = "Province id must not exceed 16 characters")); `consultationScope`: `String` (@Size(max = 5000)); `ratingAvg`: `BigDecimal` (no field annotation in current DTO); `consultationFeeVnd`: `Long` (@PositiveOrZero); response: `ResponseEntity<ApiResponse<ExpertProfileResponse>>`; response payload fields: `expertProfileId`: `UUID` (no field annotation in current DTO); `userId`: `UUID` (no field annotation in current DTO); `displayName`: `String` (no field annotation in current DTO); `specialty`: `String` (no field annotation in current DTO); `specialtyId`: `String` (no field annotation in current DTO); `specialtyIds`: `List<String>` (no field annotation in current DTO); `professionalTitle`: `String` (no field annotation in current DTO); `experienceYears`: `Integer` (no field annotation in current DTO); `workplace`: `String` (no field annotation in current DTO); `workplaceProvinceId`: `String` (no field annotation in current DTO); `hospitalId`: `String` (no field annotation in current DTO); `consultationScope`: `String` (no field annotation in current DTO); `verificationStatus`: `VerificationStatus` (no field annotation in current DTO); `trustStatus`: `TrustStatus` (no field annotation in current DTO); `expertType`: `ExpertType` (no field annotation in current DTO); `contracted`: `boolean` (no field annotation in current DTO); `isConsultationEligible`: `boolean` (no field annotation in current DTO); `verifiedAt`: `LocalDateTime` (no field annotation in current DTO); `verifiedBy`: `UUID` (no field annotation in current DTO); `ratingAvg`: `BigDecimal` (no field annotation in current DTO); `consultationFeeVnd`: `BigDecimal` (no field annotation in current DTO); `avatarUrl`: `String` (no field annotation in current DTO); `createdAt`: `LocalDateTime` (no field annotation in current DTO); `updatedAt`: `LocalDateTime` (no field annotation in current DTO); explicit/documented statuses: `201`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/controller/ExpertProfileController.java` |
| `API-03` | `PATCH /api/v1/expert/profiles/me/expert-type` | hasRole('EXPERT') | Handler `chooseExpertType`; parameters: query `type`: `ExpertType`; principal `principal`: `Principal`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<ExpertProfileDetailResponse>>`; response payload fields: `expertProfileId`: `UUID` (no field annotation in current DTO); `userId`: `UUID` (no field annotation in current DTO); `displayName`: `String` (no field annotation in current DTO); `specialty`: `String` (no field annotation in current DTO); `specialtyId`: `String` (no field annotation in current DTO); `specialtyIds`: `List<String>` (no field annotation in current DTO); `professionalTitle`: `String` (no field annotation in current DTO); `experienceYears`: `Integer` (no field annotation in current DTO); `workplace`: `String` (no field annotation in current DTO); `workplaceProvinceId`: `String` (no field annotation in current DTO); `hospitalId`: `String` (no field annotation in current DTO); `consultationScope`: `String` (no field annotation in current DTO); `verificationStatus`: `VerificationStatus` (no field annotation in current DTO); `expertType`: `ExpertType` (no field annotation in current DTO); `contracted`: `boolean` (no field annotation in current DTO); `isConsultationEligible`: `boolean` (no field annotation in current DTO); `verifiedAt`: `LocalDateTime` (no field annotation in current DTO); `verifiedBy`: `UUID` (no field annotation in current DTO); `ratingAvg`: `BigDecimal` (no field annotation in current DTO); `consultationFeeVnd`: `BigDecimal` (no field annotation in current DTO); `avatarUrl`: `String` (no field annotation in current DTO); `email`: `String` (no field annotation in current DTO); `phoneNumber`: `String` (no field annotation in current DTO); `phone`: `String` (no field annotation in current DTO); `createdAt`: `LocalDateTime` (no field annotation in current DTO); `updatedAt`: `LocalDateTime` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/controller/ExpertProfileController.java` |
| `API-04` | `GET /api/v1/master-data/districts` | No @PreAuthorize on handler/class; effective access comes from the security chain | Handler `getDistricts`; parameters: query `provinceId`: `String`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<List<DistrictResponse>>>`; response payload fields: `districtId`: `String` (no field annotation in current DTO); `provinceId`: `String` (no field annotation in current DTO); `name`: `String` (no field annotation in current DTO); `nameEn`: `String` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/masterdata/controller/MasterDataController.java` |
| `API-05` | `GET /api/v1/master-data/hospitals` | No @PreAuthorize on handler/class; effective access comes from the security chain | Handler `getHospitals`; parameters: query `provinceId`: `String`; query `districtId`: `String`; query `q`: `String`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<List<HospitalResponse>>>`; response payload fields: `hospitalId`: `String` (no field annotation in current DTO); `name`: `String` (no field annotation in current DTO); `provinceId`: `String` (no field annotation in current DTO); `districtId`: `String` (no field annotation in current DTO); `address`: `String` (no field annotation in current DTO); `level`: `String` (no field annotation in current DTO); `type`: `String` (no field annotation in current DTO); `phone`: `String` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/masterdata/controller/MasterDataController.java` |
| `API-06` | `GET /api/v1/master-data/hospitals/search/trackasia` | No @PreAuthorize on handler/class; effective access comes from the security chain | Handler `searchTrackAsiaHospitals`; parameters: query `q`: `String`; query `provinceId`: `String`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<List<TrackAsiaPlaceDto>>>`; response payload fields: `placeId`: `String` (no field annotation in current DTO); `name`: `String` (no field annotation in current DTO); `label`: `String` (no field annotation in current DTO); `longitude`: `Double` (no field annotation in current DTO); `latitude`: `Double` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/masterdata/controller/MasterDataController.java` |
| `API-07` | `GET /api/v1/master-data/hospitals/{id}` | No @PreAuthorize on handler/class; effective access comes from the security chain | Handler `getHospital`; parameters: path `id`: `String`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<HospitalResponse>>`; response payload fields: `hospitalId`: `String` (no field annotation in current DTO); `name`: `String` (no field annotation in current DTO); `provinceId`: `String` (no field annotation in current DTO); `districtId`: `String` (no field annotation in current DTO); `address`: `String` (no field annotation in current DTO); `level`: `String` (no field annotation in current DTO); `type`: `String` (no field annotation in current DTO); `phone`: `String` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/masterdata/controller/MasterDataController.java` |
| `API-08` | `GET /api/v1/master-data/provinces` | No @PreAuthorize on handler/class; effective access comes from the security chain | Handler `getProvinces`; parameters: No explicit handler parameter; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<List<ProvinceResponse>>>`; response payload fields: `provinceId`: `String` (no field annotation in current DTO); `name`: `String` (no field annotation in current DTO); `nameEn`: `String` (no field annotation in current DTO); `region`: `String` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/masterdata/controller/MasterDataController.java` |
| `API-09` | `GET /api/v1/master-data/wards` | No @PreAuthorize on handler/class; effective access comes from the security chain | Handler `getWards`; parameters: query `districtId`: `String`; query `provinceId`: `String`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<List<WardResponse>>>`; response payload fields: `wardId`: `String` (no field annotation in current DTO); `districtId`: `String` (no field annotation in current DTO); `provinceId`: `String` (no field annotation in current DTO); `name`: `String` (no field annotation in current DTO); `nameEn`: `String` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/masterdata/controller/MasterDataController.java` |

Method-level Spring handlers, authorization annotations, request DTO fields/validators, response payload fields, and explicit/documented statuses above were extracted from the cited current source. Service/advice-only application error codes are not claimed where the controller does not declare them.

### 9.1 Handler Contract — `GET /api/v1/expert/onboarding`

| Item | Exact current contract |
| --- | --- |
| Handler | `getOnboarding` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertIdentityVerificationController.java` |
| Authorization annotation / boundary | hasRole('EXPERT') |
| Parameters | principal `principal`: `Principal` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<ExpertOnboardingResponse>>` |
| Response payload fields | `profileExists`: `boolean` (no field annotation in current DTO); `identityStatus`: `String` (no field annotation in current DTO); `credentialStatus`: `String` (no field annotation in current DTO); `verificationStatus`: `VerificationStatus` (no field annotation in current DTO); `expertType`: `String` (no field annotation in current DTO); `rejectionReason`: `String` (no field annotation in current DTO); `nextStep`: `String` (no field annotation in current DTO); `latestIdentityAttempt`: `IdentityVerificationResponse` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-001` / `UC-EX-01-TC-API-001` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.2 Handler Contract — `POST /api/v1/expert/profiles`

| Item | Exact current contract |
| --- | --- |
| Handler | `createProfile` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/controller/ExpertProfileController.java` |
| Authorization annotation / boundary | hasRole('EXPERT') |
| Parameters | body `request`: `CreateExpertProfileRequest`; principal `principal`: `Principal` |
| Request body type | `CreateExpertProfileRequest` |
| Request fields and validators | `specialtyId`: `String` (@NotBlank, @Size(max = 80)); `hospitalId`: `String` (@NotBlank, @Size(max = 150)); `trackAsiaName`: `String` (@Size(max = 255)); `trackAsiaAddress`: `String` (@Size(max = 500)); `trackAsiaLat`: `Double` (no field annotation in current DTO); `trackAsiaLng`: `Double` (no field annotation in current DTO); `specialty`: `String` (@Size(max = 100)); `professionalTitle`: `String` (@Size(max = 150)); `experienceYears`: `Integer` (@Min(0), @Max(80)); `workplace`: `String` (@Size(max = 200)); `workplaceProvinceId`: `String` (@Size(max = 16, message = "Province id must not exceed 16 characters")); `consultationScope`: `String` (@Size(max = 5000)); `ratingAvg`: `BigDecimal` (no field annotation in current DTO); `consultationFeeVnd`: `Long` (@PositiveOrZero) |
| Response type | `ResponseEntity<ApiResponse<ExpertProfileResponse>>` |
| Response payload fields | `expertProfileId`: `UUID` (no field annotation in current DTO); `userId`: `UUID` (no field annotation in current DTO); `displayName`: `String` (no field annotation in current DTO); `specialty`: `String` (no field annotation in current DTO); `specialtyId`: `String` (no field annotation in current DTO); `specialtyIds`: `List<String>` (no field annotation in current DTO); `professionalTitle`: `String` (no field annotation in current DTO); `experienceYears`: `Integer` (no field annotation in current DTO); `workplace`: `String` (no field annotation in current DTO); `workplaceProvinceId`: `String` (no field annotation in current DTO); `hospitalId`: `String` (no field annotation in current DTO); `consultationScope`: `String` (no field annotation in current DTO); `verificationStatus`: `VerificationStatus` (no field annotation in current DTO); `trustStatus`: `TrustStatus` (no field annotation in current DTO); `expertType`: `ExpertType` (no field annotation in current DTO); `contracted`: `boolean` (no field annotation in current DTO); `isConsultationEligible`: `boolean` (no field annotation in current DTO); `verifiedAt`: `LocalDateTime` (no field annotation in current DTO); `verifiedBy`: `UUID` (no field annotation in current DTO); `ratingAvg`: `BigDecimal` (no field annotation in current DTO); `consultationFeeVnd`: `BigDecimal` (no field annotation in current DTO); `avatarUrl`: `String` (no field annotation in current DTO); `createdAt`: `LocalDateTime` (no field annotation in current DTO); `updatedAt`: `LocalDateTime` (no field annotation in current DTO) |
| Explicit/documented statuses | `201` |
| Positive test mapping | `COND-API-002` / `UC-EX-01-TC-API-002` |
| Negative test mapping | `COND-API-002-VAL` / `UC-EX-01-TC-API-002-VAL`; plus `COND-AUTH` for protected access |

### 9.3 Handler Contract — `PATCH /api/v1/expert/profiles/me/expert-type`

| Item | Exact current contract |
| --- | --- |
| Handler | `chooseExpertType` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/controller/ExpertProfileController.java` |
| Authorization annotation / boundary | hasRole('EXPERT') |
| Parameters | query `type`: `ExpertType`; principal `principal`: `Principal` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<ExpertProfileDetailResponse>>` |
| Response payload fields | `expertProfileId`: `UUID` (no field annotation in current DTO); `userId`: `UUID` (no field annotation in current DTO); `displayName`: `String` (no field annotation in current DTO); `specialty`: `String` (no field annotation in current DTO); `specialtyId`: `String` (no field annotation in current DTO); `specialtyIds`: `List<String>` (no field annotation in current DTO); `professionalTitle`: `String` (no field annotation in current DTO); `experienceYears`: `Integer` (no field annotation in current DTO); `workplace`: `String` (no field annotation in current DTO); `workplaceProvinceId`: `String` (no field annotation in current DTO); `hospitalId`: `String` (no field annotation in current DTO); `consultationScope`: `String` (no field annotation in current DTO); `verificationStatus`: `VerificationStatus` (no field annotation in current DTO); `expertType`: `ExpertType` (no field annotation in current DTO); `contracted`: `boolean` (no field annotation in current DTO); `isConsultationEligible`: `boolean` (no field annotation in current DTO); `verifiedAt`: `LocalDateTime` (no field annotation in current DTO); `verifiedBy`: `UUID` (no field annotation in current DTO); `ratingAvg`: `BigDecimal` (no field annotation in current DTO); `consultationFeeVnd`: `BigDecimal` (no field annotation in current DTO); `avatarUrl`: `String` (no field annotation in current DTO); `email`: `String` (no field annotation in current DTO); `phoneNumber`: `String` (no field annotation in current DTO); `phone`: `String` (no field annotation in current DTO); `createdAt`: `LocalDateTime` (no field annotation in current DTO); `updatedAt`: `LocalDateTime` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-003` / `UC-EX-01-TC-API-003` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.4 Handler Contract — `GET /api/v1/master-data/districts`

| Item | Exact current contract |
| --- | --- |
| Handler | `getDistricts` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/masterdata/controller/MasterDataController.java` |
| Authorization annotation / boundary | No @PreAuthorize on handler/class; effective access comes from the security chain |
| Parameters | query `provinceId`: `String` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<List<DistrictResponse>>>` |
| Response payload fields | `districtId`: `String` (no field annotation in current DTO); `provinceId`: `String` (no field annotation in current DTO); `name`: `String` (no field annotation in current DTO); `nameEn`: `String` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-004` / `UC-EX-01-TC-API-004` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.5 Handler Contract — `GET /api/v1/master-data/hospitals`

| Item | Exact current contract |
| --- | --- |
| Handler | `getHospitals` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/masterdata/controller/MasterDataController.java` |
| Authorization annotation / boundary | No @PreAuthorize on handler/class; effective access comes from the security chain |
| Parameters | query `provinceId`: `String`; query `districtId`: `String`; query `q`: `String` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<List<HospitalResponse>>>` |
| Response payload fields | `hospitalId`: `String` (no field annotation in current DTO); `name`: `String` (no field annotation in current DTO); `provinceId`: `String` (no field annotation in current DTO); `districtId`: `String` (no field annotation in current DTO); `address`: `String` (no field annotation in current DTO); `level`: `String` (no field annotation in current DTO); `type`: `String` (no field annotation in current DTO); `phone`: `String` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-005` / `UC-EX-01-TC-API-005` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.6 Handler Contract — `GET /api/v1/master-data/hospitals/search/trackasia`

| Item | Exact current contract |
| --- | --- |
| Handler | `searchTrackAsiaHospitals` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/masterdata/controller/MasterDataController.java` |
| Authorization annotation / boundary | No @PreAuthorize on handler/class; effective access comes from the security chain |
| Parameters | query `q`: `String`; query `provinceId`: `String` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<List<TrackAsiaPlaceDto>>>` |
| Response payload fields | `placeId`: `String` (no field annotation in current DTO); `name`: `String` (no field annotation in current DTO); `label`: `String` (no field annotation in current DTO); `longitude`: `Double` (no field annotation in current DTO); `latitude`: `Double` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-006` / `UC-EX-01-TC-API-006` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.7 Handler Contract — `GET /api/v1/master-data/hospitals/{id}`

| Item | Exact current contract |
| --- | --- |
| Handler | `getHospital` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/masterdata/controller/MasterDataController.java` |
| Authorization annotation / boundary | No @PreAuthorize on handler/class; effective access comes from the security chain |
| Parameters | path `id`: `String` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<HospitalResponse>>` |
| Response payload fields | `hospitalId`: `String` (no field annotation in current DTO); `name`: `String` (no field annotation in current DTO); `provinceId`: `String` (no field annotation in current DTO); `districtId`: `String` (no field annotation in current DTO); `address`: `String` (no field annotation in current DTO); `level`: `String` (no field annotation in current DTO); `type`: `String` (no field annotation in current DTO); `phone`: `String` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-007` / `UC-EX-01-TC-API-007` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.8 Handler Contract — `GET /api/v1/master-data/provinces`

| Item | Exact current contract |
| --- | --- |
| Handler | `getProvinces` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/masterdata/controller/MasterDataController.java` |
| Authorization annotation / boundary | No @PreAuthorize on handler/class; effective access comes from the security chain |
| Parameters | No explicit handler parameter |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<List<ProvinceResponse>>>` |
| Response payload fields | `provinceId`: `String` (no field annotation in current DTO); `name`: `String` (no field annotation in current DTO); `nameEn`: `String` (no field annotation in current DTO); `region`: `String` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-008` / `UC-EX-01-TC-API-008` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.9 Handler Contract — `GET /api/v1/master-data/wards`

| Item | Exact current contract |
| --- | --- |
| Handler | `getWards` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/masterdata/controller/MasterDataController.java` |
| Authorization annotation / boundary | No @PreAuthorize on handler/class; effective access comes from the security chain |
| Parameters | query `districtId`: `String`; query `provinceId`: `String` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<List<WardResponse>>>` |
| Response payload fields | `wardId`: `String` (no field annotation in current DTO); `districtId`: `String` (no field annotation in current DTO); `provinceId`: `String` (no field annotation in current DTO); `name`: `String` (no field annotation in current DTO); `nameEn`: `String` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-009` / `UC-EX-01-TC-API-009` |
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
| `VG-01` | Create or load the applicant's expert profile. | `COND-01` | `UC-EX-01-TC-001` |
| `VG-02` | Select an allowed expert type and supported location/facility master data. | `COND-02` | `UC-EX-01-TC-002` |
| `VG-03` | Persist onboarding progress and continue to the next required step. | `COND-03` | `UC-EX-01-TC-003` |
| `VG-AUTH` | Reject wrong authentication/role/ownership/membership/consent scope | `COND-AUTH` | `UC-EX-01-TC-SEC-001` |
| `VG-GAP` | Characterize each known gap without claiming a false completed path | `COND-GAP` | `UC-EX-01-TC-GAP-001` |

## 14. Verification Methods

Existing focused evidence:

- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/expert/controller/ExpertProfileControllerTest.java`
- `05_Development/CareBridgeWebApp/src/features/expert/pages/ExpertOnboardingPage.test.tsx`

Exact supported commands derived from the audited test paths:

- `cd 05_Development/CareBridgeAPI && ./mvnw -Dtest=ExpertProfileControllerTest test`
- `cd 05_Development/CareBridgeWebApp && npm test -- src/features/expert/pages/ExpertOnboardingPage.test.tsx`

Record pass/fail/skip counts only after executing these commands on the exact revision.

## 15. Verification Samples

| Sample | Value |
| --- | --- |
| Primary contract | POST `/api/v1/expert/profiles` |
| Request | `GET /api/v1/expert/onboarding` → `getOnboarding`; `None` with Not applicable — no request body; authorization: hasRole('EXPERT'). |
| Success response | `ResponseEntity<ApiResponse<ExpertOnboardingResponse>>` with `profileExists`: `boolean` (no field annotation in current DTO); `identityStatus`: `String` (no field annotation in current DTO); `credentialStatus`: `String` (no field annotation in current DTO); `verificationStatus`: `VerificationStatus` (no field annotation in current DTO); `expertType`: `String` (no field annotation in current DTO); `rejectionReason`: `String` (no field annotation in current DTO); `nextStep`: `String` (no field annotation in current DTO); `latestIdentityAttempt`: `IdentityVerificationResponse` (no field annotation in current DTO); explicit/documented statuses `200`. |
| Negative sample | Use wrong role/owner/state and assert the exact mapped error without protected payload. |

## 16. Authorization Matrix

| Actor / role | Operation | Decision |
| --- | --- | --- |
| Expert Applicant | `GET /api/v1/expert/onboarding` | hasRole('EXPERT'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertIdentityVerificationController.java` |
| Expert Applicant | `POST /api/v1/expert/profiles` | hasRole('EXPERT'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/controller/ExpertProfileController.java` |
| Expert Applicant | `PATCH /api/v1/expert/profiles/me/expert-type` | hasRole('EXPERT'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/controller/ExpertProfileController.java` |
| Expert Applicant | `GET /api/v1/master-data/districts` | No @PreAuthorize on handler/class; effective access comes from the security chain; handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/masterdata/controller/MasterDataController.java` |
| Expert Applicant | `GET /api/v1/master-data/hospitals` | No @PreAuthorize on handler/class; effective access comes from the security chain; handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/masterdata/controller/MasterDataController.java` |
| Expert Applicant | `GET /api/v1/master-data/hospitals/search/trackasia` | No @PreAuthorize on handler/class; effective access comes from the security chain; handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/masterdata/controller/MasterDataController.java` |
| Expert Applicant | `GET /api/v1/master-data/hospitals/{id}` | No @PreAuthorize on handler/class; effective access comes from the security chain; handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/masterdata/controller/MasterDataController.java` |
| Expert Applicant | `GET /api/v1/master-data/provinces` | No @PreAuthorize on handler/class; effective access comes from the security chain; handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/masterdata/controller/MasterDataController.java` |
| Expert Applicant | `GET /api/v1/master-data/wards` | No @PreAuthorize on handler/class; effective access comes from the security chain; handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/masterdata/controller/MasterDataController.java` |
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
