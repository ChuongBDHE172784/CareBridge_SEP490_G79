# ENGINEERING DOCUMENTATION STANDARD (EDS) v2.0

# TECHNICAL DESIGN SPECIFICATION — View and Edit Account Profile

| Field | Value |
| --- | --- |
| Document ID | `UC-AC-06-TDS` |
| Version | `0.1` |
| Date | `2026-08-23` |
| Status | `Draft` |
| Function ID | `UC-AC-06` |
| Canonical Use Case | `UC-AC-06 — View and Edit Account Profile` |
| Module / Bounded Context | `Access, Identity, and Trust` |
| Primary Actor | `Authenticated User` |
| Platforms | `Mobile / Web / Backend` |
| Priority | `High` |
| Data Classification | `Confidential identity, authentication/session, notification-device, privacy, and consent data; credentials/tokens are secrets` |
| Compliance Scope | `PDPA purpose limitation, authentication secrecy, session/device ownership, consent proof, and audit redaction` |
| Owner | `CareBridge Team` |
| Reviewer / Approver |  |
| Source Baseline | Current worktree on `2026-08-23`; SRS `UC-AC-06`; exact evidence in Section 1.4 |

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

- **Goal:** View and update the supported profile fields of the authenticated account.
- **Trigger:** The actor enters Mobile `/profile` and `/profile/edit`.
- **Outcome:** Save and reload the server-authoritative profile.
- **Current state:** `High` confidence from reachable code/test audit; no manifest-level exclusion is recorded.
- **Target state:** Preserve current code-backed behavior and resolve only explicitly evidenced limitations through approved implementation work.

### 1.2 Scope

**In scope**

- Mobile `/profile` and `/profile/edit`
- Web `/account/profile`

- GET `/api/v1/auth/profile`
- PUT `/api/v1/auth/profile`
- GET `/api/v1/profile`
- PATCH `/api/v1/profile`

**Out of scope / limitations**

- Not applicable — no manifest-level exclusion is recorded. Scope is limited to the entry points and exact contracts listed above; unrelated handlers in the same module are excluded.

### 1.3 Preconditions and Postconditions

| Type | Condition |
| --- | --- |
| Precondition | Authenticated User is authenticated/authorized where the current contract requires it. |
| Precondition | Required ownership, membership, consent, resource state, and device/provider prerequisites pass current policies. |
| Postcondition | Save and reload the server-authoritative profile. |
| Postcondition | No side effect outside the feature-owned persistence/event/provider boundary occurs. |

### 1.4 Evidence Baseline

| Source ID | Type | Exact path | Revision | Authority |
| --- | --- | --- | --- | --- |
| `SRC-SRS-01` | Requirement | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-AC-06 | 2026-08-23 | Draft code-first requirement |
| `SRC-CODE-01` | Current code | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-02` | Current code | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/profile/controller/ProfileController.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-03` | Current code | `05_Development/CareBridgeMobileApp/lib/features/auth/screens/account_profile_screen.dart` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-04` | Current code | `05_Development/CareBridgeMobileApp/lib/features/auth/screens/edit_profile_screen.dart` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-05` | Current code | `05_Development/CareBridgeWebApp/src/features/auth/pages/AccountProfilePage.tsx` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-01` | Existing test | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/profile/ProfileIntegrationTest.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-02` | Existing test | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/security/integration/AuthProfileIntegrationTest.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-ERROR-01` | Current exception advice | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/common/exception/GlobalExceptionHandler.java` | Worktree `2026-08-23` | Current-state evidence |

### 1.5 Open Contradictions / Questions

- No material contradiction was found among the exact sources cited in Section 1.4. Behavior not evidenced by those sources is not approved or implied by this Draft.

## 2. Traceability Matrix

| Requirement | Behavior | Exact oracle source | Component | Test condition / case |
| --- | --- | --- | --- | --- |
| `UC-AC-06-FR-01` | Load the authenticated profile projection. | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-AC-06 Normal Flow 1 | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java` | `COND-01` / `UC-AC-06-TC-001` |
| `UC-AC-06-FR-02` | Edit only supported profile fields. | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-AC-06 Normal Flow 2 | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java` | `COND-02` / `UC-AC-06-TC-002` |
| `UC-AC-06-FR-03` | Save and reload the server-authoritative profile. | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-AC-06 Normal Flow 3 | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/profile/controller/ProfileController.java` | `COND-03` / `UC-AC-06-TC-003` |
| `BR-01` | Profile ownership comes from the authenticated principal. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/profile/controller/ProfileController.java` | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/profile/controller/ProfileController.java` | `COND-BR-01` / `UC-AC-06-TC-BR-001` |
| `BR-02` | Role, verification, and protected identity fields cannot be self-escalated through profile edits. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java` | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java` | `COND-BR-02` / `UC-AC-06-TC-BR-002` |

## 3. Architecture Decision Records (ADR)

### ADR-UC-AC-06-01 — Use a distinct actor-goal boundary

| Item | Decision |
| --- | --- |
| Context | The retired 43-UC catalogue grouped multiple triggers, lifecycles, and permission boundaries, making implementation/test traceability generic. |
| Options | Keep the broad catalogue; split by screen; split by actor goal plus lifecycle/security boundary. |
| Decision | Use `UC-AC-06 — View and Edit Account Profile` as the canonical boundary because its operations share the stated actor outcome and current implementation evidence. |
| Consequences | Related supporting screens/endpoints stay in one TDS; different lifecycle/actor outcomes have separate UCs. |
| Source / Status | SRS Section 3.1 and current code audit / Draft |

### ADR-UC-AC-06-02 — Preserve unknowns as Open

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
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java` | Reuse | Current implementation evidence for View and Edit Account Profile; inspect the exact symbol before implementation changes. |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/profile/controller/ProfileController.java` | Reuse | Current implementation evidence for View and Edit Account Profile; inspect the exact symbol before implementation changes. |
| `05_Development/CareBridgeMobileApp/lib/features/auth/screens/account_profile_screen.dart` | Reuse | Current implementation evidence for View and Edit Account Profile; inspect the exact symbol before implementation changes. |
| `05_Development/CareBridgeMobileApp/lib/features/auth/screens/edit_profile_screen.dart` | Reuse | Current implementation evidence for View and Edit Account Profile; inspect the exact symbol before implementation changes. |
| `05_Development/CareBridgeWebApp/src/features/auth/pages/AccountProfilePage.tsx` | Reuse | Current implementation evidence for View and Edit Account Profile; inspect the exact symbol before implementation changes. |

### 5.2 Current Component Diagram

```plantuml
@startuml
skinparam classAttributeIconSize 0
class AuthController as "AuthController.java"
class ProfileController as "ProfileController.java"
AuthController --> ProfileController
class account_profile_screen as "account_profile_screen.dart"
ProfileController --> account_profile_screen
class edit_profile_screen as "edit_profile_screen.dart"
account_profile_screen --> edit_profile_screen
class AccountProfilePage as "AccountProfilePage.tsx"
edit_profile_screen --> AccountProfilePage
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
Actor -> Client: Enter View and Edit Account Profile
Client -> Domain: Load the authenticated profile projection.
Domain --> Client: Result for step 1
Client -> Domain: Edit only supported profile fields.
Domain --> Client: Result for step 2
Client -> Domain: Save and reload the server-authoritative profile.
Domain --> Client: Result for step 3
Client --> Actor: Render canonical outcome
@enduml
```

### 6.2 Alternative, Error, Retry, and Concurrency Flows

| Flow | Expected design behavior | Oracle |
| --- | --- | --- |
| Cancel before mutation | No unintended write or provider side effect. | SRS UC-AC-06 Alternative Flow |
| Invalid input/state | Reject with the current contract; keep canonical state unchanged. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java` |
| Wrong actor/scope | Fail closed without protected resource disclosure. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java` |
| Dependency failure | Use only the implemented bounded fallback/retry; never report false success. | Not applicable at this cited baseline — no external adapter/provider import is evidenced in the listed implementation sources |
| Duplicate/concurrent mutation | Apply only current lock/version/idempotency semantics. | No explicit lock/version/idempotency marker is evidenced in the cited implementation sources; preserve observed behavior and add characterization before changing concurrency semantics |

### 6.3 State Model and Invariants

```plantuml
@startuml
[*] --> Eligible
Eligible --> InProgress : Load the authenticated profile projection.
InProgress --> Outcome : Save and reload the server-authoritative profile.
InProgress --> Rejected : validation / authorization / state failure
Outcome --> [*]
Rejected --> Eligible : actor corrects eligible input
@enduml
```

- Profile ownership comes from the authenticated principal.
- Role, verification, and protected identity fields cannot be self-escalated through profile edits.

## 7. Domain Event Catalog

| Direction | Event | Producer / Consumer | Payload / delivery / idempotency |
| --- | --- | --- | --- |
| Publish / consume | Current event evidence | Not applicable at this cited baseline — no event type, publisher, or listener is evidenced in the listed implementation sources | Preserve only events evidenced by the cited source set; if later call-path inspection finds none, the flow remains synchronous. |

## 8. Interface Specification

### 8.1 User / Operator Interfaces

| # | Entry point | Actor | Contract |
| ---: | --- | --- | --- |
| 1 | Mobile `/profile` and `/profile/edit` | Authenticated User | Reachable current entry point |
| 2 | Web `/account/profile` | Authenticated User | Reachable current entry point |

### 8.2 Service / Repository Interfaces

| Interface evidence | Required responsibility |
| --- | --- |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java` | Support the mapped operations without broadening authorization or lifecycle semantics. |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/profile/controller/ProfileController.java` | Support the mapped operations without broadening authorization or lifecycle semantics. |
| `05_Development/CareBridgeMobileApp/lib/features/auth/screens/account_profile_screen.dart` | Support the mapped operations without broadening authorization or lifecycle semantics. |
| `05_Development/CareBridgeMobileApp/lib/features/auth/screens/edit_profile_screen.dart` | Support the mapped operations without broadening authorization or lifecycle semantics. |
| `05_Development/CareBridgeWebApp/src/features/auth/pages/AccountProfilePage.tsx` | Support the mapped operations without broadening authorization or lifecycle semantics. |

## 9. API Specification

| ID | Method / path or grouped controller surface | Auth / role | Request / response / errors |
| --- | --- | --- | --- |
| `API-01` | `GET /api/v1/auth/profile` | No @PreAuthorize on handler/class; effective access comes from the security chain | Handler `profile`; parameters: principal `principal`: `Principal`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<UserProfileResponse>>`; response payload fields: `id`: `java.util.UUID` (no field annotation in current DTO); `name`: `String` (no field annotation in current DTO); `email`: `String` (no field annotation in current DTO); `phone`: `String` (no field annotation in current DTO); `avatarUrl`: `String` (no field annotation in current DTO); `role`: `Role` (no field annotation in current DTO); `accountStatus`: `String` (no field annotation in current DTO); `emailVerified`: `Boolean` (no field annotation in current DTO); `phoneVerified`: `Boolean` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO); explicit/documented statuses: `200, 401`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java` |
| `API-02` | `PUT /api/v1/auth/profile` | No @PreAuthorize on handler/class; effective access comes from the security chain | Handler `updateProfile`; parameters: principal `principal`: `Principal`; body `request`: `UpdateProfileRequest`; request body: `UpdateProfileRequest`; request fields/validation: `name`: `String` (@Size(max = 120)); `avatarUrl`: `String` (@Size(max = 500)); response: `ResponseEntity<ApiResponse<UserProfileResponse>>`; response payload fields: `id`: `java.util.UUID` (no field annotation in current DTO); `name`: `String` (no field annotation in current DTO); `email`: `String` (no field annotation in current DTO); `phone`: `String` (no field annotation in current DTO); `avatarUrl`: `String` (no field annotation in current DTO); `role`: `Role` (no field annotation in current DTO); `accountStatus`: `String` (no field annotation in current DTO); `emailVerified`: `Boolean` (no field annotation in current DTO); `phoneVerified`: `Boolean` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO); explicit/documented statuses: `200, 400, 401`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java` |
| `API-03` | `GET /api/v1/profile` | isAuthenticated() | Handler `getProfile`; parameters: principal `principal`: `Principal`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ApiResponse<ProfileResponse>`; response payload fields: `userId`: `UUID` (no field annotation in current DTO); `displayName`: `String` (no field annotation in current DTO); `avatarUrl`: `String` (no field annotation in current DTO); `phoneNumber`: `String` (no field annotation in current DTO); `dateOfBirth`: `LocalDate` (no field annotation in current DTO); `area`: `String` (no field annotation in current DTO); `updatedAt`: `Instant` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/profile/controller/ProfileController.java` |
| `API-04` | `PATCH /api/v1/profile` | isAuthenticated() | Handler `updateProfile`; parameters: body `request`: `UpdateProfileRequest`; principal `principal`: `Principal`; request body: `UpdateProfileRequest`; request fields/validation: `displayName`: `String` (@Size(min = 2, max = 100, message = "Display name must be between 2 and 100 characters"), @Pattern(regexp = "^[^<>&\"']*$", message = "Display name contains illegal characters")); `avatarUrl`: `String` (@Size(max = 500, message = "Avatar URL must not exceed 500 characters"), @Pattern(regexp = "^(https?://.*)); `phoneNumber`: `String` (@Pattern(regexp = "^(0[3-9][0-9]{8}\|\\+84[3-9][0-9]{8})); `dateOfBirth`: `LocalDate` (@Past(message = "Date of birth must be in the past")); `area`: `String` (@Size(max = 100, message = "Area must not exceed 100 characters")); response: `ApiResponse<ProfileResponse>`; response payload fields: `userId`: `UUID` (no field annotation in current DTO); `displayName`: `String` (no field annotation in current DTO); `avatarUrl`: `String` (no field annotation in current DTO); `phoneNumber`: `String` (no field annotation in current DTO); `dateOfBirth`: `LocalDate` (no field annotation in current DTO); `area`: `String` (no field annotation in current DTO); `updatedAt`: `Instant` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/profile/controller/ProfileController.java` |

Method-level Spring handlers, authorization annotations, request DTO fields/validators, response payload fields, and explicit/documented statuses above were extracted from the cited current source. Service/advice-only application error codes are not claimed where the controller does not declare them.

### 9.1 Handler Contract — `GET /api/v1/auth/profile`

| Item | Exact current contract |
| --- | --- |
| Handler | `profile` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java` |
| Authorization annotation / boundary | No @PreAuthorize on handler/class; effective access comes from the security chain |
| Parameters | principal `principal`: `Principal` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<UserProfileResponse>>` |
| Response payload fields | `id`: `java.util.UUID` (no field annotation in current DTO); `name`: `String` (no field annotation in current DTO); `email`: `String` (no field annotation in current DTO); `phone`: `String` (no field annotation in current DTO); `avatarUrl`: `String` (no field annotation in current DTO); `role`: `Role` (no field annotation in current DTO); `accountStatus`: `String` (no field annotation in current DTO); `emailVerified`: `Boolean` (no field annotation in current DTO); `phoneVerified`: `Boolean` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO) |
| Explicit/documented statuses | `200, 401` |
| Positive test mapping | `COND-API-001` / `UC-AC-06-TC-API-001` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.2 Handler Contract — `PUT /api/v1/auth/profile`

| Item | Exact current contract |
| --- | --- |
| Handler | `updateProfile` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java` |
| Authorization annotation / boundary | No @PreAuthorize on handler/class; effective access comes from the security chain |
| Parameters | principal `principal`: `Principal`; body `request`: `UpdateProfileRequest` |
| Request body type | `UpdateProfileRequest` |
| Request fields and validators | `name`: `String` (@Size(max = 120)); `avatarUrl`: `String` (@Size(max = 500)) |
| Response type | `ResponseEntity<ApiResponse<UserProfileResponse>>` |
| Response payload fields | `id`: `java.util.UUID` (no field annotation in current DTO); `name`: `String` (no field annotation in current DTO); `email`: `String` (no field annotation in current DTO); `phone`: `String` (no field annotation in current DTO); `avatarUrl`: `String` (no field annotation in current DTO); `role`: `Role` (no field annotation in current DTO); `accountStatus`: `String` (no field annotation in current DTO); `emailVerified`: `Boolean` (no field annotation in current DTO); `phoneVerified`: `Boolean` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO) |
| Explicit/documented statuses | `200, 400, 401` |
| Positive test mapping | `COND-API-002` / `UC-AC-06-TC-API-002` |
| Negative test mapping | `COND-API-002-VAL` / `UC-AC-06-TC-API-002-VAL`; plus `COND-AUTH` for protected access |

### 9.3 Handler Contract — `GET /api/v1/profile`

| Item | Exact current contract |
| --- | --- |
| Handler | `getProfile` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/profile/controller/ProfileController.java` |
| Authorization annotation / boundary | isAuthenticated() |
| Parameters | principal `principal`: `Principal` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ApiResponse<ProfileResponse>` |
| Response payload fields | `userId`: `UUID` (no field annotation in current DTO); `displayName`: `String` (no field annotation in current DTO); `avatarUrl`: `String` (no field annotation in current DTO); `phoneNumber`: `String` (no field annotation in current DTO); `dateOfBirth`: `LocalDate` (no field annotation in current DTO); `area`: `String` (no field annotation in current DTO); `updatedAt`: `Instant` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-003` / `UC-AC-06-TC-API-003` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.4 Handler Contract — `PATCH /api/v1/profile`

| Item | Exact current contract |
| --- | --- |
| Handler | `updateProfile` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/profile/controller/ProfileController.java` |
| Authorization annotation / boundary | isAuthenticated() |
| Parameters | body `request`: `UpdateProfileRequest`; principal `principal`: `Principal` |
| Request body type | `UpdateProfileRequest` |
| Request fields and validators | `displayName`: `String` (@Size(min = 2, max = 100, message = "Display name must be between 2 and 100 characters"), @Pattern(regexp = "^[^<>&\"']*$", message = "Display name contains illegal characters")); `avatarUrl`: `String` (@Size(max = 500, message = "Avatar URL must not exceed 500 characters"), @Pattern(regexp = "^(https?://.*)); `phoneNumber`: `String` (@Pattern(regexp = "^(0[3-9][0-9]{8}\|\\+84[3-9][0-9]{8})); `dateOfBirth`: `LocalDate` (@Past(message = "Date of birth must be in the past")); `area`: `String` (@Size(max = 100, message = "Area must not exceed 100 characters")) |
| Response type | `ApiResponse<ProfileResponse>` |
| Response payload fields | `userId`: `UUID` (no field annotation in current DTO); `displayName`: `String` (no field annotation in current DTO); `avatarUrl`: `String` (no field annotation in current DTO); `phoneNumber`: `String` (no field annotation in current DTO); `dateOfBirth`: `LocalDate` (no field annotation in current DTO); `area`: `String` (no field annotation in current DTO); `updatedAt`: `Instant` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-004` / `UC-AC-06-TC-API-004` |
| Negative test mapping | `COND-API-004-VAL` / `UC-AC-06-TC-API-004-VAL`; plus `COND-AUTH` for protected access |

## 10. Error Codes

| Error class | HTTP / code | Trigger | Client behavior | Oracle |
| --- | --- | --- | --- | --- |
| Validation/business rule | `400` | Malformed field, range, enum, or rejected transition | Return the current stable error envelope without protected data or unintended mutation | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/common/exception/GlobalExceptionHandler.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java`; application error code is limited to what those sources/advice explicitly declare |
| Authentication | `401` | Missing, expired, revoked, or invalid credential | Return the current stable error envelope without protected data or unintended mutation | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java`; application error code is limited to what those sources/advice explicitly declare |

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
| `VG-01` | Load the authenticated profile projection. | `COND-01` | `UC-AC-06-TC-001` |
| `VG-02` | Edit only supported profile fields. | `COND-02` | `UC-AC-06-TC-002` |
| `VG-03` | Save and reload the server-authoritative profile. | `COND-03` | `UC-AC-06-TC-003` |
| `VG-AUTH` | Reject wrong authentication/role/ownership/membership/consent scope | `COND-AUTH` | `UC-AC-06-TC-SEC-001` |
| `VG-GAP` | Characterize each known gap without claiming a false completed path | `COND-GAP` | `UC-AC-06-TC-GAP-001` |

## 14. Verification Methods

Existing focused evidence:

- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/profile/ProfileIntegrationTest.java`
- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/security/integration/AuthProfileIntegrationTest.java`

Exact supported commands derived from the audited test paths:

- `cd 05_Development/CareBridgeAPI && ./mvnw -Dtest=ProfileIntegrationTest test`
- `cd 05_Development/CareBridgeAPI && ./mvnw -Dtest=AuthProfileIntegrationTest test`

Record pass/fail/skip counts only after executing these commands on the exact revision.

## 15. Verification Samples

| Sample | Value |
| --- | --- |
| Primary contract | GET `/api/v1/auth/profile` |
| Request | `GET /api/v1/auth/profile` → `profile`; `None` with Not applicable — no request body; authorization: No @PreAuthorize on handler/class; effective access comes from the security chain. |
| Success response | `ResponseEntity<ApiResponse<UserProfileResponse>>` with `id`: `java.util.UUID` (no field annotation in current DTO); `name`: `String` (no field annotation in current DTO); `email`: `String` (no field annotation in current DTO); `phone`: `String` (no field annotation in current DTO); `avatarUrl`: `String` (no field annotation in current DTO); `role`: `Role` (no field annotation in current DTO); `accountStatus`: `String` (no field annotation in current DTO); `emailVerified`: `Boolean` (no field annotation in current DTO); `phoneVerified`: `Boolean` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO); explicit/documented statuses `200, 401`. |
| Negative sample | Use wrong role/owner/state and assert the exact mapped error without protected payload. |

## 16. Authorization Matrix

| Actor / role | Operation | Decision |
| --- | --- | --- |
| Authenticated User | `GET /api/v1/auth/profile` | No @PreAuthorize on handler/class; effective access comes from the security chain; handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java` |
| Authenticated User | `PUT /api/v1/auth/profile` | No @PreAuthorize on handler/class; effective access comes from the security chain; handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java` |
| Authenticated User | `GET /api/v1/profile` | isAuthenticated(); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/profile/controller/ProfileController.java` |
| Authenticated User | `PATCH /api/v1/profile` | isAuthenticated(); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/profile/controller/ProfileController.java` |
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
