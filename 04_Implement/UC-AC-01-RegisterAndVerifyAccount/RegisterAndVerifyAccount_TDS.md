# ENGINEERING DOCUMENTATION STANDARD (EDS) v2.0

# TECHNICAL DESIGN SPECIFICATION — Register and Verify Account

| Field | Value |
| --- | --- |
| Document ID | `UC-AC-01-TDS` |
| Version | `0.1` |
| Date | `2026-08-23` |
| Status | `Draft` |
| Function ID | `UC-AC-01` |
| Canonical Use Case | `UC-AC-01 — Register and Verify Account` |
| Module / Bounded Context | `Access, Identity, and Trust` |
| Primary Actor | `Guest` |
| Platforms | `Mobile / Web Expert Portal / Backend` |
| Priority | `High` |
| Data Classification | `Confidential identity, authentication/session, notification-device, privacy, and consent data; credentials/tokens are secrets` |
| Compliance Scope | `PDPA purpose limitation, authentication secrecy, session/device ownership, consent proof, and audit redaction` |
| Owner | `CareBridge Team` |
| Reviewer / Approver |  |
| Source Baseline | Current worktree on `2026-08-23`; SRS `UC-AC-01`; exact evidence in Section 1.4 |

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

- **Goal:** Create a supported email, phone, or federated account, verify the one-time proof, and select an allowed initial role before protected navigation.
- **Trigger:** The actor enters Mobile `/welcome` and nested registration/OTP screens.
- **Outcome:** Select an allowed initial role and continue to the role-aware entry point.
- **Current state:** `High` confidence from reachable code/test audit; documented limitations remain visible below.
- **Target state:** Preserve current code-backed behavior and resolve only explicitly evidenced limitations through approved implementation work.

### 1.2 Scope

**In scope**

- Mobile `/welcome` and nested registration/OTP screens
- Mobile `/role-selection`
- Web `/expert/register` and `/login/otp`

- POST `/api/v1/auth/register`
- POST `/api/v1/auth/phone/register`
- POST `/api/v1/auth/federated`
- POST `/api/v1/auth/verify-otp`
- POST `/api/v1/auth/resend-otp`
- PUT `/api/v1/auth/role`

**Out of scope / limitations**

- Open / current limitation: Deleted legacy federated-registration pages are not valid Web entry points.

### 1.3 Preconditions and Postconditions

| Type | Condition |
| --- | --- |
| Precondition | Guest is authenticated/authorized where the current contract requires it. |
| Precondition | Required ownership, membership, consent, resource state, and device/provider prerequisites pass current policies. |
| Postcondition | Select an allowed initial role and continue to the role-aware entry point. |
| Postcondition | No side effect outside the feature-owned persistence/event/provider boundary occurs. |

### 1.4 Evidence Baseline

| Source ID | Type | Exact path | Revision | Authority |
| --- | --- | --- | --- | --- |
| `SRC-SRS-01` | Requirement | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-AC-01 | 2026-08-23 | Draft code-first requirement |
| `SRC-CODE-01` | Current code | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-02` | Current code | `05_Development/CareBridgeMobileApp/lib/features/auth/screens/register_screen.dart` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-03` | Current code | `05_Development/CareBridgeMobileApp/lib/features/auth/screens/otp_verification_screen.dart` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-01` | Existing test | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/security/integration/RegistrationIntegrationTest.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-02` | Existing test | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/security/integration/VerifyOtpIntegrationTest.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-03` | Existing test | `05_Development/CareBridgeWebApp/src/features/auth/pages/ExpertRegisterPage.test.tsx` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-ERROR-01` | Current exception advice | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/common/exception/GlobalExceptionHandler.java` | Worktree `2026-08-23` | Current-state evidence |

### 1.5 Open Contradictions / Questions

- Open / current limitation: Deleted legacy federated-registration pages are not valid Web entry points.

## 2. Traceability Matrix

| Requirement | Behavior | Exact oracle source | Component | Test condition / case |
| --- | --- | --- | --- | --- |
| `UC-AC-01-FR-01` | Choose a supported registration method and submit identity data. | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-AC-01 Normal Flow 1 | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java` | `COND-01` / `UC-AC-01-TC-001` |
| `UC-AC-01-FR-02` | Receive and verify the supported OTP or federated proof. | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-AC-01 Normal Flow 2 | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java` | `COND-02` / `UC-AC-01-TC-002` |
| `UC-AC-01-FR-03` | Select an allowed initial role and continue to the role-aware entry point. | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-AC-01 Normal Flow 3 | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java` | `COND-03` / `UC-AC-01-TC-003` |
| `BR-01` | OTP cooldown, uniqueness, proof validity, and the role allow-list are server authoritative. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java` | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java` | `COND-BR-01` / `UC-AC-01-TC-BR-001` |
| `BR-02` | General Web registration redirects to login; only expert registration is reachable on Web. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java` | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java` | `COND-BR-02` / `UC-AC-01-TC-BR-002` |

## 3. Architecture Decision Records (ADR)

### ADR-UC-AC-01-01 — Use a distinct actor-goal boundary

| Item | Decision |
| --- | --- |
| Context | The retired 43-UC catalogue grouped multiple triggers, lifecycles, and permission boundaries, making implementation/test traceability generic. |
| Options | Keep the broad catalogue; split by screen; split by actor goal plus lifecycle/security boundary. |
| Decision | Use `UC-AC-01 — Register and Verify Account` as the canonical boundary because its operations share the stated actor outcome and current implementation evidence. |
| Consequences | Related supporting screens/endpoints stay in one TDS; different lifecycle/actor outcomes have separate UCs. |
| Source / Status | SRS Section 3.1 and current code audit / Draft |

### ADR-UC-AC-01-02 — Preserve unknowns as Open

| Item | Decision |
| --- | --- |
| Context | Exact schema fields, SLA values, and some controller error codes are not fully evidenced by this manifest alone. |
| Decision | Do not invent them. Mark them `Open` and require the exact DTO/entity/migration/policy oracle before production-code changes. |
| Consequences | This document accurately characterizes current scope; unresolved design-changing items block implementation approval. |
| Source / Status | `create-specs` evidence discipline / Draft |

## 4. Non-Functional Requirements and SLA

| NFR | Target | Oracle Source | Verification |
| --- | --- | --- | --- |
| Authorization / isolation | All requests follow exact role/ownership/membership/consent policy in current code. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java` | Negative security cases in paired Test-Spec |
| Protected-data handling | No secrets or unnecessary health/location/identity/conversation/file payloads in logs, fixtures, screenshots, or audit detail. | Data classification header plus exact Section 9 request/response field inventories | Log/fixture review plus security tests |
| Availability / latency | Open — no approved feature-specific numeric SLA found. | Evidence needed: approved NFR/SLA source | Measure only; do not assert a fixed threshold |
| Accessibility | Open — confirm project-standard criteria for reachable UI surfaces. | Evidence needed: approved UX/accessibility standard | Applicable Web/Mobile UI checks |
| Retry / idempotency | Apply only semantics explicitly implemented by the owning service. | No explicit lock/version/idempotency marker is evidenced in the cited implementation sources; preserve observed behavior and add characterization before changing concurrency semantics | State/duplicate/concurrency cases where applicable |

## 5. Static Modeling

### 5.1 Component Responsibilities and Change Disposition

| Exact path | Disposition | Responsibility |
| --- | --- | --- |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java` | Reuse | Current implementation evidence for Register and Verify Account; inspect the exact symbol before implementation changes. |
| `05_Development/CareBridgeMobileApp/lib/features/auth/screens/register_screen.dart` | Reuse | Current implementation evidence for Register and Verify Account; inspect the exact symbol before implementation changes. |
| `05_Development/CareBridgeMobileApp/lib/features/auth/screens/otp_verification_screen.dart` | Reuse | Current implementation evidence for Register and Verify Account; inspect the exact symbol before implementation changes. |

### 5.2 Current Component Diagram

```plantuml
@startuml
skinparam classAttributeIconSize 0
class AuthController as "AuthController.java"
class register_screen as "register_screen.dart"
AuthController --> register_screen
class otp_verification_screen as "otp_verification_screen.dart"
register_screen --> otp_verification_screen
@enduml
```

### 5.3 Data / Schema / Migration Assessment

| Item | Assessment |
| --- | --- |
| Current stores/entities | No entity/repository import is present in the cited entry/source set; follow the exact handler-to-service call path before any schema change |
| Sensitive fields | Confidential identity, authentication/session, notification-device, privacy, and consent data; credentials/tokens are secrets. Exact transport fields and validators are enumerated per handler in Section 9; entity-only fields require the cited service/entity source before a schema change. |
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
Actor -> Client: Enter Register and Verify Account
Client -> Domain: Choose a supported registration method and submit identity data.
Domain --> Client: Result for step 1
Client -> Domain: Receive and verify the supported OTP or federated proof.
Domain --> Client: Result for step 2
Client -> Domain: Select an allowed initial role and continue to the role-aware entry point.
Domain --> Client: Result for step 3
Client --> Actor: Render canonical outcome
@enduml
```

### 6.2 Alternative, Error, Retry, and Concurrency Flows

| Flow | Expected design behavior | Oracle |
| --- | --- | --- |
| Cancel before mutation | No unintended write or provider side effect. | SRS UC-AC-01 Alternative Flow |
| Invalid input/state | Reject with the current contract; keep canonical state unchanged. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java` |
| Wrong actor/scope | Fail closed without protected resource disclosure. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java` |
| Dependency failure | Use only the implemented bounded fallback/retry; never report false success. | Not applicable at this cited baseline — no external adapter/provider import is evidenced in the listed implementation sources |
| Duplicate/concurrent mutation | Apply only current lock/version/idempotency semantics. | No explicit lock/version/idempotency marker is evidenced in the cited implementation sources; preserve observed behavior and add characterization before changing concurrency semantics |

### 6.3 State Model and Invariants

```plantuml
@startuml
[*] --> Eligible
Eligible --> InProgress : Choose a supported registration method and submit identity data.
InProgress --> Outcome : Select an allowed initial role and continue to the role-aware entry point.
InProgress --> Rejected : validation / authorization / state failure
Outcome --> [*]
Rejected --> Eligible : actor corrects eligible input
@enduml
```

- OTP cooldown, uniqueness, proof validity, and the role allow-list are server authoritative.
- General Web registration redirects to login; only expert registration is reachable on Web.

## 7. Domain Event Catalog

| Direction | Event | Producer / Consumer | Payload / delivery / idempotency |
| --- | --- | --- | --- |
| Publish / consume | Current event evidence | Not applicable at this cited baseline — no event type, publisher, or listener is evidenced in the listed implementation sources | Preserve only events evidenced by the cited source set; if later call-path inspection finds none, the flow remains synchronous. |

## 8. Interface Specification

### 8.1 User / Operator Interfaces

| # | Entry point | Actor | Contract |
| ---: | --- | --- | --- |
| 1 | Mobile `/welcome` and nested registration/OTP screens | Guest | Reachable current entry point |
| 2 | Mobile `/role-selection` | Guest | Reachable current entry point |
| 3 | Web `/expert/register` and `/login/otp` | Guest | Reachable current entry point |

### 8.2 Service / Repository Interfaces

| Interface evidence | Required responsibility |
| --- | --- |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java` | Support the mapped operations without broadening authorization or lifecycle semantics. |
| `05_Development/CareBridgeMobileApp/lib/features/auth/screens/register_screen.dart` | Support the mapped operations without broadening authorization or lifecycle semantics. |
| `05_Development/CareBridgeMobileApp/lib/features/auth/screens/otp_verification_screen.dart` | Support the mapped operations without broadening authorization or lifecycle semantics. |

## 9. API Specification

| ID | Method / path or grouped controller surface | Auth / role | Request / response / errors |
| --- | --- | --- | --- |
| `API-01` | `POST /api/v1/auth/federated` | No @PreAuthorize on handler/class; effective access comes from the security chain | Handler `federated`; parameters: body `request`: `FederatedAuthRequest`; request body: `FederatedAuthRequest`; request fields/validation: `idToken`: `String` (@NotBlank); `deviceInfo`: `String` (@Size(max = 500)); response: `ResponseEntity<ApiResponse<FederatedAuthResponse>>`; response payload fields: `accessToken`: `String` (no field annotation in current DTO); `refreshToken`: `String` (no field annotation in current DTO); `user`: `UserProfileResponse` (no field annotation in current DTO); `newUser`: `boolean` (no field annotation in current DTO); `profileCompleted`: `boolean` (no field annotation in current DTO); explicit/documented statuses: `200, 201`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java` |
| `API-02` | `POST /api/v1/auth/phone/register` | No @PreAuthorize on handler/class; effective access comes from the security chain | Handler `registerPhone`; parameters: body `request`: `PhoneRegisterRequest`; request body: `PhoneRegisterRequest`; request fields/validation: `idToken`: `String` (@NotBlank, @Size(max = 8192)); `name`: `String` (@NotBlank, @Size(min = 2, max = 120)); `email`: `String` (@NotBlank, @Email, @Size(max = 255)); `phone`: `String` (@NotBlank); `password`: `String` (@NotBlank, @Size(min = 8, max = 100)); `role`: `Role` (no field annotation in current DTO); `deviceInfo`: `String` (@Size(max = 500)); response: `ResponseEntity<ApiResponse<FederatedAuthResponse>>`; response payload fields: `accessToken`: `String` (no field annotation in current DTO); `refreshToken`: `String` (no field annotation in current DTO); `user`: `UserProfileResponse` (no field annotation in current DTO); `newUser`: `boolean` (no field annotation in current DTO); `profileCompleted`: `boolean` (no field annotation in current DTO); explicit/documented statuses: `201`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java` |
| `API-03` | `POST /api/v1/auth/register` | No @PreAuthorize on handler/class; effective access comes from the security chain | Handler `register`; parameters: body `request`: `RegisterRequest`; request body: `RegisterRequest`; request fields/validation: `name`: `String` (@NotBlank, @Size(min = 2, max = 120)); `phone`: `String` (no field annotation in current DTO); `email`: `String` (@Email); `password`: `String` (@NotBlank, @Size(min = 8, max = 100)); `role`: `Role` (no field annotation in current DTO); `verificationMethod`: `VerificationMethod` (@NotNull); response: `ResponseEntity<ApiResponse<OtpSendResponse>>`; response payload fields: `message`: `String` (no field annotation in current DTO); `expiresIn`: `long` (no field annotation in current DTO); `userId`: `UUID` (no field annotation in current DTO); `otpExpiresAt`: `Instant` (no field annotation in current DTO); `auth`: `AuthResponse` (no field annotation in current DTO); explicit/documented statuses: `201, 400, 409`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java` |
| `API-04` | `POST /api/v1/auth/resend-otp` | No @PreAuthorize on handler/class; effective access comes from the security chain | Handler `resendOtp`; parameters: body `request`: `ResendOtpRequest`; request body: `ResendOtpRequest`; request fields/validation: `phone`: `String` (no field annotation in current DTO); `email`: `String` (@Email); response: `ResponseEntity<ApiResponse<OtpResendResponse>>`; response payload fields: `otpExpiresAt`: `Instant` (no field annotation in current DTO); `resendCooldownRemaining`: `long` (no field annotation in current DTO); `message`: `String` (no field annotation in current DTO); explicit/documented statuses: `200, 400, 404, 429`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java` |
| `API-05` | `PUT /api/v1/auth/role` | No @PreAuthorize on handler/class; effective access comes from the security chain | Handler `selectRole`; parameters: principal `principal`: `Principal`; body `request`: `SelectRoleRequest`; request body: `SelectRoleRequest`; request fields/validation: `role`: `Role` (@NotNull); response: `ResponseEntity<ApiResponse<UserProfileResponse>>`; response payload fields: `id`: `java.util.UUID` (no field annotation in current DTO); `name`: `String` (no field annotation in current DTO); `email`: `String` (no field annotation in current DTO); `phone`: `String` (no field annotation in current DTO); `avatarUrl`: `String` (no field annotation in current DTO); `role`: `Role` (no field annotation in current DTO); `accountStatus`: `String` (no field annotation in current DTO); `emailVerified`: `Boolean` (no field annotation in current DTO); `phoneVerified`: `Boolean` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO); explicit/documented statuses: `200, 400, 401`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java` |
| `API-06` | `POST /api/v1/auth/verify-otp` | No @PreAuthorize on handler/class; effective access comes from the security chain | Handler `verifyOtp`; parameters: body `request`: `VerifyOtpRequest`; request body: `VerifyOtpRequest`; request fields/validation: `phone`: `String` (no field annotation in current DTO); `email`: `String` (@Email); `otp`: `String` (@NotBlank, @Pattern(regexp = "^\\d{6}$", message = "OTP must contain 6 digits")); response: `ResponseEntity<ApiResponse<AuthResponse>>`; response payload fields: `accessToken`: `String` (no field annotation in current DTO); `refreshToken`: `String` (no field annotation in current DTO); `user`: `UserProfileResponse` (no field annotation in current DTO); explicit/documented statuses: `200, 400, 404`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java` |

Method-level Spring handlers, authorization annotations, request DTO fields/validators, response payload fields, and explicit/documented statuses above were extracted from the cited current source. Service/advice-only application error codes are not claimed where the controller does not declare them.

### 9.1 Handler Contract — `POST /api/v1/auth/federated`

| Item | Exact current contract |
| --- | --- |
| Handler | `federated` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java` |
| Authorization annotation / boundary | No @PreAuthorize on handler/class; effective access comes from the security chain |
| Parameters | body `request`: `FederatedAuthRequest` |
| Request body type | `FederatedAuthRequest` |
| Request fields and validators | `idToken`: `String` (@NotBlank); `deviceInfo`: `String` (@Size(max = 500)) |
| Response type | `ResponseEntity<ApiResponse<FederatedAuthResponse>>` |
| Response payload fields | `accessToken`: `String` (no field annotation in current DTO); `refreshToken`: `String` (no field annotation in current DTO); `user`: `UserProfileResponse` (no field annotation in current DTO); `newUser`: `boolean` (no field annotation in current DTO); `profileCompleted`: `boolean` (no field annotation in current DTO) |
| Explicit/documented statuses | `200, 201` |
| Positive test mapping | `COND-API-001` / `UC-AC-01-TC-API-001` |
| Negative test mapping | `COND-API-001-VAL` / `UC-AC-01-TC-API-001-VAL`; plus `COND-AUTH` for protected access |

### 9.2 Handler Contract — `POST /api/v1/auth/phone/register`

| Item | Exact current contract |
| --- | --- |
| Handler | `registerPhone` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java` |
| Authorization annotation / boundary | No @PreAuthorize on handler/class; effective access comes from the security chain |
| Parameters | body `request`: `PhoneRegisterRequest` |
| Request body type | `PhoneRegisterRequest` |
| Request fields and validators | `idToken`: `String` (@NotBlank, @Size(max = 8192)); `name`: `String` (@NotBlank, @Size(min = 2, max = 120)); `email`: `String` (@NotBlank, @Email, @Size(max = 255)); `phone`: `String` (@NotBlank); `password`: `String` (@NotBlank, @Size(min = 8, max = 100)); `role`: `Role` (no field annotation in current DTO); `deviceInfo`: `String` (@Size(max = 500)) |
| Response type | `ResponseEntity<ApiResponse<FederatedAuthResponse>>` |
| Response payload fields | `accessToken`: `String` (no field annotation in current DTO); `refreshToken`: `String` (no field annotation in current DTO); `user`: `UserProfileResponse` (no field annotation in current DTO); `newUser`: `boolean` (no field annotation in current DTO); `profileCompleted`: `boolean` (no field annotation in current DTO) |
| Explicit/documented statuses | `201` |
| Positive test mapping | `COND-API-002` / `UC-AC-01-TC-API-002` |
| Negative test mapping | `COND-API-002-VAL` / `UC-AC-01-TC-API-002-VAL`; plus `COND-AUTH` for protected access |

### 9.3 Handler Contract — `POST /api/v1/auth/register`

| Item | Exact current contract |
| --- | --- |
| Handler | `register` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java` |
| Authorization annotation / boundary | No @PreAuthorize on handler/class; effective access comes from the security chain |
| Parameters | body `request`: `RegisterRequest` |
| Request body type | `RegisterRequest` |
| Request fields and validators | `name`: `String` (@NotBlank, @Size(min = 2, max = 120)); `phone`: `String` (no field annotation in current DTO); `email`: `String` (@Email); `password`: `String` (@NotBlank, @Size(min = 8, max = 100)); `role`: `Role` (no field annotation in current DTO); `verificationMethod`: `VerificationMethod` (@NotNull) |
| Response type | `ResponseEntity<ApiResponse<OtpSendResponse>>` |
| Response payload fields | `message`: `String` (no field annotation in current DTO); `expiresIn`: `long` (no field annotation in current DTO); `userId`: `UUID` (no field annotation in current DTO); `otpExpiresAt`: `Instant` (no field annotation in current DTO); `auth`: `AuthResponse` (no field annotation in current DTO) |
| Explicit/documented statuses | `201, 400, 409` |
| Positive test mapping | `COND-API-003` / `UC-AC-01-TC-API-003` |
| Negative test mapping | `COND-API-003-VAL` / `UC-AC-01-TC-API-003-VAL`; plus `COND-AUTH` for protected access |

### 9.4 Handler Contract — `POST /api/v1/auth/resend-otp`

| Item | Exact current contract |
| --- | --- |
| Handler | `resendOtp` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java` |
| Authorization annotation / boundary | No @PreAuthorize on handler/class; effective access comes from the security chain |
| Parameters | body `request`: `ResendOtpRequest` |
| Request body type | `ResendOtpRequest` |
| Request fields and validators | `phone`: `String` (no field annotation in current DTO); `email`: `String` (@Email) |
| Response type | `ResponseEntity<ApiResponse<OtpResendResponse>>` |
| Response payload fields | `otpExpiresAt`: `Instant` (no field annotation in current DTO); `resendCooldownRemaining`: `long` (no field annotation in current DTO); `message`: `String` (no field annotation in current DTO) |
| Explicit/documented statuses | `200, 400, 404, 429` |
| Positive test mapping | `COND-API-004` / `UC-AC-01-TC-API-004` |
| Negative test mapping | `COND-API-004-VAL` / `UC-AC-01-TC-API-004-VAL`; plus `COND-AUTH` for protected access |

### 9.5 Handler Contract — `PUT /api/v1/auth/role`

| Item | Exact current contract |
| --- | --- |
| Handler | `selectRole` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java` |
| Authorization annotation / boundary | No @PreAuthorize on handler/class; effective access comes from the security chain |
| Parameters | principal `principal`: `Principal`; body `request`: `SelectRoleRequest` |
| Request body type | `SelectRoleRequest` |
| Request fields and validators | `role`: `Role` (@NotNull) |
| Response type | `ResponseEntity<ApiResponse<UserProfileResponse>>` |
| Response payload fields | `id`: `java.util.UUID` (no field annotation in current DTO); `name`: `String` (no field annotation in current DTO); `email`: `String` (no field annotation in current DTO); `phone`: `String` (no field annotation in current DTO); `avatarUrl`: `String` (no field annotation in current DTO); `role`: `Role` (no field annotation in current DTO); `accountStatus`: `String` (no field annotation in current DTO); `emailVerified`: `Boolean` (no field annotation in current DTO); `phoneVerified`: `Boolean` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO) |
| Explicit/documented statuses | `200, 400, 401` |
| Positive test mapping | `COND-API-005` / `UC-AC-01-TC-API-005` |
| Negative test mapping | `COND-API-005-VAL` / `UC-AC-01-TC-API-005-VAL`; plus `COND-AUTH` for protected access |

### 9.6 Handler Contract — `POST /api/v1/auth/verify-otp`

| Item | Exact current contract |
| --- | --- |
| Handler | `verifyOtp` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java` |
| Authorization annotation / boundary | No @PreAuthorize on handler/class; effective access comes from the security chain |
| Parameters | body `request`: `VerifyOtpRequest` |
| Request body type | `VerifyOtpRequest` |
| Request fields and validators | `phone`: `String` (no field annotation in current DTO); `email`: `String` (@Email); `otp`: `String` (@NotBlank, @Pattern(regexp = "^\\d{6}$", message = "OTP must contain 6 digits")) |
| Response type | `ResponseEntity<ApiResponse<AuthResponse>>` |
| Response payload fields | `accessToken`: `String` (no field annotation in current DTO); `refreshToken`: `String` (no field annotation in current DTO); `user`: `UserProfileResponse` (no field annotation in current DTO) |
| Explicit/documented statuses | `200, 400, 404` |
| Positive test mapping | `COND-API-006` / `UC-AC-01-TC-API-006` |
| Negative test mapping | `COND-API-006-VAL` / `UC-AC-01-TC-API-006-VAL`; plus `COND-AUTH` for protected access |

## 10. Error Codes

| Error class | HTTP / code | Trigger | Client behavior | Oracle |
| --- | --- | --- | --- | --- |
| Validation/business rule | `400` | Malformed field, range, enum, or rejected transition | Return the current stable error envelope without protected data or unintended mutation | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/common/exception/GlobalExceptionHandler.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java`; application error code is limited to what those sources/advice explicitly declare |
| Authentication | `401` | Missing, expired, revoked, or invalid credential | Return the current stable error envelope without protected data or unintended mutation | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java`; application error code is limited to what those sources/advice explicitly declare |
| Missing/inaccessible resource | `404` | Identifier is absent or deliberately hidden by access policy | Return the current stable error envelope without protected data or unintended mutation | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java`; application error code is limited to what those sources/advice explicitly declare |
| Conflict | `409` | Duplicate, stale, uniqueness, or lifecycle conflict | Return the current stable error envelope without protected data or unintended mutation | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java`; application error code is limited to what those sources/advice explicitly declare |
| Rate limit/cooldown | `429` | Attempt or resend limit/cooldown is exceeded | Return the current stable error envelope without protected data or unintended mutation | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java`; application error code is limited to what those sources/advice explicitly declare |

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
| `VG-01` | Choose a supported registration method and submit identity data. | `COND-01` | `UC-AC-01-TC-001` |
| `VG-02` | Receive and verify the supported OTP or federated proof. | `COND-02` | `UC-AC-01-TC-002` |
| `VG-03` | Select an allowed initial role and continue to the role-aware entry point. | `COND-03` | `UC-AC-01-TC-003` |
| `VG-AUTH` | Reject wrong authentication/role/ownership/membership/consent scope | `COND-AUTH` | `UC-AC-01-TC-SEC-001` |
| `VG-GAP` | Characterize each known gap without claiming a false completed path | `COND-GAP` | `UC-AC-01-TC-GAP-001` |

## 14. Verification Methods

Existing focused evidence:

- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/security/integration/RegistrationIntegrationTest.java`
- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/security/integration/VerifyOtpIntegrationTest.java`
- `05_Development/CareBridgeWebApp/src/features/auth/pages/ExpertRegisterPage.test.tsx`

Exact supported commands derived from the audited test paths:

- `cd 05_Development/CareBridgeAPI && ./mvnw -Dtest=RegistrationIntegrationTest test`
- `cd 05_Development/CareBridgeAPI && ./mvnw -Dtest=VerifyOtpIntegrationTest test`
- `cd 05_Development/CareBridgeWebApp && npm test -- src/features/auth/pages/ExpertRegisterPage.test.tsx`

Record pass/fail/skip counts only after executing these commands on the exact revision.

## 15. Verification Samples

| Sample | Value |
| --- | --- |
| Primary contract | POST `/api/v1/auth/register` |
| Request | `POST /api/v1/auth/federated` → `federated`; `FederatedAuthRequest` with `idToken`: `String` (@NotBlank); `deviceInfo`: `String` (@Size(max = 500)); authorization: No @PreAuthorize on handler/class; effective access comes from the security chain. |
| Success response | `ResponseEntity<ApiResponse<FederatedAuthResponse>>` with `accessToken`: `String` (no field annotation in current DTO); `refreshToken`: `String` (no field annotation in current DTO); `user`: `UserProfileResponse` (no field annotation in current DTO); `newUser`: `boolean` (no field annotation in current DTO); `profileCompleted`: `boolean` (no field annotation in current DTO); explicit/documented statuses `200, 201`. |
| Negative sample | Use wrong role/owner/state and assert the exact mapped error without protected payload. |

## 16. Authorization Matrix

| Actor / role | Operation | Decision |
| --- | --- | --- |
| Guest | `POST /api/v1/auth/federated` | No @PreAuthorize on handler/class; effective access comes from the security chain; handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java` |
| Guest | `POST /api/v1/auth/phone/register` | No @PreAuthorize on handler/class; effective access comes from the security chain; handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java` |
| Guest | `POST /api/v1/auth/register` | No @PreAuthorize on handler/class; effective access comes from the security chain; handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java` |
| Guest | `POST /api/v1/auth/resend-otp` | No @PreAuthorize on handler/class; effective access comes from the security chain; handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java` |
| Guest | `PUT /api/v1/auth/role` | No @PreAuthorize on handler/class; effective access comes from the security chain; handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java` |
| Guest | `POST /api/v1/auth/verify-otp` | No @PreAuthorize on handler/class; effective access comes from the security chain; handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java` |
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
