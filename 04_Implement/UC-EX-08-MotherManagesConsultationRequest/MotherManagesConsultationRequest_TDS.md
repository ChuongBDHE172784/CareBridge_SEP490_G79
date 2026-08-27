# ENGINEERING DOCUMENTATION STANDARD (EDS) v2.0

# TECHNICAL DESIGN SPECIFICATION — Mother Creates and Manages Consultation Request

| Field | Value |
| --- | --- |
| Document ID | `UC-EX-08-TDS` |
| Version | `0.1` |
| Date | `2026-08-23` |
| Status | `Draft` |
| Function ID | `UC-EX-08` |
| Canonical Use Case | `UC-EX-08 — Mother Creates and Manages Consultation Request` |
| Module / Bounded Context | `Expert and Consultation` |
| Primary Actor | `Mother` |
| Platforms | `Mobile / Backend` |
| Priority | `Medium` |
| Data Classification | `Confidential professional and consultation data; Restricted identity/credential evidence and purpose-bound attachments` |
| Compliance Scope | `PDPA purpose limitation, least privilege, purpose-bound file access, consultation confidentiality, and consent-aware recording` |
| Owner | `CareBridge Team` |
| Reviewer / Approver |  |
| Source Baseline | Current worktree on `2026-08-23`; SRS `UC-EX-08`; exact evidence in Section 1.4 |

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

- **Goal:** Create a consultation request for an eligible expert, view its status/detail, and cancel it while the lifecycle permits.
- **Trigger:** The actor enters Mobile consultation form from public expert profile.
- **Outcome:** Cancel an owned request only while cancellation is allowed.
- **Current state:** `High` confidence from reachable code/test audit; no manifest-level exclusion is recorded.
- **Target state:** Preserve current code-backed behavior and resolve only explicitly evidenced limitations through approved implementation work.

### 1.2 Scope

**In scope**

- Mobile consultation form from public expert profile
- Mobile `/consultation-requests`, `/consultation-requests/:requestId`

- POST `/api/v1/consultation-requests`
- GET `/api/v1/consultation-requests/mine`
- GET `/api/v1/consultation-requests/{id}`
- PATCH `/api/v1/consultation-requests/{id}/cancel`

**Out of scope / limitations**

- Not applicable — no manifest-level exclusion is recorded. Scope is limited to the entry points and exact contracts listed above; unrelated handlers in the same module are excluded.

### 1.3 Preconditions and Postconditions

| Type | Condition |
| --- | --- |
| Precondition | Mother is authenticated/authorized where the current contract requires it. |
| Precondition | Required ownership, membership, consent, resource state, and device/provider prerequisites pass current policies. |
| Postcondition | Cancel an owned request only while cancellation is allowed. |
| Postcondition | No side effect outside the feature-owned persistence/event/provider boundary occurs. |

### 1.4 Evidence Baseline

| Source ID | Type | Exact path | Revision | Authority |
| --- | --- | --- | --- | --- |
| `SRC-SRS-01` | Requirement | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-EX-08 | 2026-08-23 | Draft code-first requirement |
| `SRC-CODE-01` | Current code | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consultation/controller/ConsultationRequestController.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-02` | Current code | `05_Development/CareBridgeMobileApp/lib/features/consultation/screens/consultation_request_form_screen.dart` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-03` | Current code | `05_Development/CareBridgeMobileApp/lib/features/consultation/screens/my_consultation_requests_screen.dart` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-01` | Existing test | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/consultation/service/impl/ConsultationRequestServiceImplCreateTest.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-02` | Existing test | `05_Development/CareBridgeMobileApp/test/features/consultation/consultation_request_mobile_test.dart` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-ERROR-01` | Current exception advice | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/common/exception/GlobalExceptionHandler.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-SUPP-01` | Historical architecture/design evidence | `04_Implement/ExpertConsultationRequests/ExpertConsultationRequests_Architecture-Evidence.md` | Worktree `2026-08-23` | Historical evidence only; current code and canonical SRS/TDS override conflicts |
| `SRC-SUPP-02` | Historical verification evidence | `04_Implement/ExpertConsultationRequests/ExpertConsultationRequests_Verification-Evidence.md` | Worktree `2026-08-23` | Historical evidence only; current code and canonical SRS/TDS override conflicts |

### 1.5 Open Contradictions / Questions

- No material contradiction was found among the exact sources cited in Section 1.4. Behavior not evidenced by those sources is not approved or implied by this Draft.

## 2. Traceability Matrix

| Requirement | Behavior | Exact oracle source | Component | Test condition / case |
| --- | --- | --- | --- | --- |
| `UC-EX-08-FR-01` | Choose an eligible expert and enter request details. | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-EX-08 Normal Flow 1 | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consultation/controller/ConsultationRequestController.java` | `COND-01` / `UC-EX-08-TC-001` |
| `UC-EX-08-FR-02` | Submit and track the server lifecycle state. | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-EX-08 Normal Flow 2 | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consultation/controller/ConsultationRequestController.java` | `COND-02` / `UC-EX-08-TC-002` |
| `UC-EX-08-FR-03` | Cancel an owned request only while cancellation is allowed. | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-EX-08 Normal Flow 3 | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consultation/controller/ConsultationRequestController.java` | `COND-03` / `UC-EX-08-TC-003` |
| `BR-01` | Expert eligibility and request ownership are rechecked on mutation. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consultation/controller/ConsultationRequestController.java` | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consultation/controller/ConsultationRequestController.java` | `COND-BR-01` / `UC-EX-08-TC-BR-001` |
| `BR-02` | Notifications are side effects, not the source of request state. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consultation/controller/ConsultationRequestController.java` | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consultation/controller/ConsultationRequestController.java` | `COND-BR-02` / `UC-EX-08-TC-BR-002` |

## 3. Architecture Decision Records (ADR)

### ADR-UC-EX-08-01 — Use a distinct actor-goal boundary

| Item | Decision |
| --- | --- |
| Context | The retired 43-UC catalogue grouped multiple triggers, lifecycles, and permission boundaries, making implementation/test traceability generic. |
| Options | Keep the broad catalogue; split by screen; split by actor goal plus lifecycle/security boundary. |
| Decision | Use `UC-EX-08 — Mother Creates and Manages Consultation Request` as the canonical boundary because its operations share the stated actor outcome and current implementation evidence. |
| Consequences | Related supporting screens/endpoints stay in one TDS; different lifecycle/actor outcomes have separate UCs. |
| Source / Status | SRS Section 3.1 and current code audit / Draft |

### ADR-UC-EX-08-02 — Preserve unknowns as Open

| Item | Decision |
| --- | --- |
| Context | Exact schema fields, SLA values, and some controller error codes are not fully evidenced by this manifest alone. |
| Decision | Do not invent them. Mark them `Open` and require the exact DTO/entity/migration/policy oracle before production-code changes. |
| Consequences | This document accurately characterizes current scope; unresolved design-changing items block implementation approval. |
| Source / Status | `create-specs` evidence discipline / Draft |

## 4. Non-Functional Requirements and SLA

| NFR | Target | Oracle Source | Verification |
| --- | --- | --- | --- |
| Authorization / isolation | All requests follow exact role/ownership/membership/consent policy in current code. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consultation/controller/ConsultationRequestController.java` | Negative security cases in paired Test-Spec |
| Protected-data handling | No secrets or unnecessary health/location/identity/conversation/file payloads in logs, fixtures, screenshots, or audit detail. | Data classification header plus exact Section 9 request/response field inventories | Log/fixture review plus security tests |
| Availability / latency | Open — no approved feature-specific numeric SLA found. | Evidence needed: approved NFR/SLA source | Measure only; do not assert a fixed threshold |
| Accessibility | Open — confirm project-standard criteria for reachable UI surfaces. | Evidence needed: approved UX/accessibility standard | Applicable Web/Mobile UI checks |
| Retry / idempotency | Apply only semantics explicitly implemented by the owning service. | No explicit lock/version/idempotency marker is evidenced in the cited implementation sources; preserve observed behavior and add characterization before changing concurrency semantics | State/duplicate/concurrency cases where applicable |

## 5. Static Modeling

### 5.1 Component Responsibilities and Change Disposition

| Exact path | Disposition | Responsibility |
| --- | --- | --- |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consultation/controller/ConsultationRequestController.java` | Reuse | Current implementation evidence for Mother Creates and Manages Consultation Request; inspect the exact symbol before implementation changes. |
| `05_Development/CareBridgeMobileApp/lib/features/consultation/screens/consultation_request_form_screen.dart` | Reuse | Current implementation evidence for Mother Creates and Manages Consultation Request; inspect the exact symbol before implementation changes. |
| `05_Development/CareBridgeMobileApp/lib/features/consultation/screens/my_consultation_requests_screen.dart` | Reuse | Current implementation evidence for Mother Creates and Manages Consultation Request; inspect the exact symbol before implementation changes. |

### 5.2 Current Component Diagram

```plantuml
@startuml
skinparam classAttributeIconSize 0
class ConsultationRequestController as "ConsultationRequestController.java"
class consultation_request_form_screen as "consultation_request_form_screen.dart"
ConsultationRequestController --> consultation_request_form_screen
class my_consultation_requests_screen as "my_consultation_requests_screen.dart"
consultation_request_form_screen --> my_consultation_requests_screen
@enduml
```

### 5.3 Data / Schema / Migration Assessment

| Item | Assessment |
| --- | --- |
| Current stores/entities | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consultation/entity/ConsultationRequestStatus.java` |
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
Actor -> Client: Enter Mother Creates and Manages Consultation Request
Client -> Domain: Choose an eligible expert and enter request details.
Domain --> Client: Result for step 1
Client -> Domain: Submit and track the server lifecycle state.
Domain --> Client: Result for step 2
Client -> Domain: Cancel an owned request only while cancellation is allowed.
Domain --> Client: Result for step 3
Client --> Actor: Render canonical outcome
@enduml
```

### 6.2 Alternative, Error, Retry, and Concurrency Flows

| Flow | Expected design behavior | Oracle |
| --- | --- | --- |
| Cancel before mutation | No unintended write or provider side effect. | SRS UC-EX-08 Alternative Flow |
| Invalid input/state | Reject with the current contract; keep canonical state unchanged. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consultation/controller/ConsultationRequestController.java` |
| Wrong actor/scope | Fail closed without protected resource disclosure. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consultation/controller/ConsultationRequestController.java` |
| Dependency failure | Use only the implemented bounded fallback/retry; never report false success. | Not applicable at this cited baseline — no external adapter/provider import is evidenced in the listed implementation sources |
| Duplicate/concurrent mutation | Apply only current lock/version/idempotency semantics. | No explicit lock/version/idempotency marker is evidenced in the cited implementation sources; preserve observed behavior and add characterization before changing concurrency semantics |

### 6.3 State Model and Invariants

```plantuml
@startuml
[*] --> Eligible
Eligible --> InProgress : Choose an eligible expert and enter request details.
InProgress --> Outcome : Cancel an owned request only while cancellation is allowed.
InProgress --> Rejected : validation / authorization / state failure
Outcome --> [*]
Rejected --> Eligible : actor corrects eligible input
@enduml
```

- Expert eligibility and request ownership are rechecked on mutation.
- Notifications are side effects, not the source of request state.

## 7. Domain Event Catalog

| Direction | Event | Producer / Consumer | Payload / delivery / idempotency |
| --- | --- | --- | --- |
| Publish / consume | Current event evidence | Not applicable at this cited baseline — no event type, publisher, or listener is evidenced in the listed implementation sources | Preserve only events evidenced by the cited source set; if later call-path inspection finds none, the flow remains synchronous. |

## 8. Interface Specification

### 8.1 User / Operator Interfaces

| # | Entry point | Actor | Contract |
| ---: | --- | --- | --- |
| 1 | Mobile consultation form from public expert profile | Mother | Reachable current entry point |
| 2 | Mobile `/consultation-requests`, `/consultation-requests/:requestId` | Mother | Reachable current entry point |

### 8.2 Service / Repository Interfaces

| Interface evidence | Required responsibility |
| --- | --- |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consultation/controller/ConsultationRequestController.java` | Support the mapped operations without broadening authorization or lifecycle semantics. |
| `05_Development/CareBridgeMobileApp/lib/features/consultation/screens/consultation_request_form_screen.dart` | Support the mapped operations without broadening authorization or lifecycle semantics. |
| `05_Development/CareBridgeMobileApp/lib/features/consultation/screens/my_consultation_requests_screen.dart` | Support the mapped operations without broadening authorization or lifecycle semantics. |

## 9. API Specification

| ID | Method / path or grouped controller surface | Auth / role | Request / response / errors |
| --- | --- | --- | --- |
| `API-01` | `POST /api/v1/consultation-requests` | hasAnyRole('MOTHER', 'FAMILY') | Handler `create`; parameters: body `request`: `CreateConsultationRequestRequest`; principal `principal`: `Principal`; request body: `CreateConsultationRequestRequest`; request fields/validation: `clientRequestId`: `UUID` (@NotNull); `expertProfileId`: `UUID` (@NotNull); `topic`: `String` (@NotBlank, @Size(max = 200)); `description`: `String` (@NotBlank, @Size(max = 2000)); `preferredWindowStart`: `Instant` (no field annotation in current DTO); `preferredWindowEnd`: `Instant` (no field annotation in current DTO); response: `ResponseEntity<ApiResponse<ConsultationRequestResponse>>`; response payload fields: `id`: `UUID` (no field annotation in current DTO); `expertProfileId`: `UUID` (no field annotation in current DTO); `counterpartDisplayName`: `String` (no field annotation in current DTO); `counterpartAvatarUrl`: `String` (no field annotation in current DTO); `topic`: `String` (no field annotation in current DTO); `description`: `String` (no field annotation in current DTO); `preferredWindowStart`: `Instant` (no field annotation in current DTO); `preferredWindowEnd`: `Instant` (no field annotation in current DTO); `status`: `String` (no field annotation in current DTO); `rejectReason`: `String` (no field annotation in current DTO); `directConversationId`: `UUID` (no field annotation in current DTO); `respondedAt`: `Instant` (no field annotation in current DTO); `expiresAt`: `Instant` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO); explicit/documented statuses: `200, 201`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consultation/controller/ConsultationRequestController.java` |
| `API-02` | `GET /api/v1/consultation-requests/mine` | hasAnyRole('MOTHER', 'FAMILY') | Handler `listMine`; parameters: query `status`: `ConsultationRequestStatus`; query `page`: `int`; query `size`: `int`; principal `principal`: `Principal`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<PaginatedResponse<ConsultationRequestSummaryResponse>>`; response payload fields: Not applicable or unresolved from the handler import; explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consultation/controller/ConsultationRequestController.java` |
| `API-03` | `GET /api/v1/consultation-requests/{id}` | hasAnyRole('MOTHER', 'FAMILY', 'EXPERT') | Handler `getById`; parameters: path `id`: `UUID`; principal `principal`: `Principal`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<ConsultationRequestResponse>>`; response payload fields: `id`: `UUID` (no field annotation in current DTO); `expertProfileId`: `UUID` (no field annotation in current DTO); `counterpartDisplayName`: `String` (no field annotation in current DTO); `counterpartAvatarUrl`: `String` (no field annotation in current DTO); `topic`: `String` (no field annotation in current DTO); `description`: `String` (no field annotation in current DTO); `preferredWindowStart`: `Instant` (no field annotation in current DTO); `preferredWindowEnd`: `Instant` (no field annotation in current DTO); `status`: `String` (no field annotation in current DTO); `rejectReason`: `String` (no field annotation in current DTO); `directConversationId`: `UUID` (no field annotation in current DTO); `respondedAt`: `Instant` (no field annotation in current DTO); `expiresAt`: `Instant` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consultation/controller/ConsultationRequestController.java` |
| `API-04` | `PATCH /api/v1/consultation-requests/{id}/cancel` | hasAnyRole('MOTHER', 'FAMILY') | Handler `cancel`; parameters: path `id`: `UUID`; principal `principal`: `Principal`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<ConsultationRequestResponse>>`; response payload fields: `id`: `UUID` (no field annotation in current DTO); `expertProfileId`: `UUID` (no field annotation in current DTO); `counterpartDisplayName`: `String` (no field annotation in current DTO); `counterpartAvatarUrl`: `String` (no field annotation in current DTO); `topic`: `String` (no field annotation in current DTO); `description`: `String` (no field annotation in current DTO); `preferredWindowStart`: `Instant` (no field annotation in current DTO); `preferredWindowEnd`: `Instant` (no field annotation in current DTO); `status`: `String` (no field annotation in current DTO); `rejectReason`: `String` (no field annotation in current DTO); `directConversationId`: `UUID` (no field annotation in current DTO); `respondedAt`: `Instant` (no field annotation in current DTO); `expiresAt`: `Instant` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consultation/controller/ConsultationRequestController.java` |

Method-level Spring handlers, authorization annotations, request DTO fields/validators, response payload fields, and explicit/documented statuses above were extracted from the cited current source. Service/advice-only application error codes are not claimed where the controller does not declare them.

### 9.1 Handler Contract — `POST /api/v1/consultation-requests`

| Item | Exact current contract |
| --- | --- |
| Handler | `create` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consultation/controller/ConsultationRequestController.java` |
| Authorization annotation / boundary | hasAnyRole('MOTHER', 'FAMILY') |
| Parameters | body `request`: `CreateConsultationRequestRequest`; principal `principal`: `Principal` |
| Request body type | `CreateConsultationRequestRequest` |
| Request fields and validators | `clientRequestId`: `UUID` (@NotNull); `expertProfileId`: `UUID` (@NotNull); `topic`: `String` (@NotBlank, @Size(max = 200)); `description`: `String` (@NotBlank, @Size(max = 2000)); `preferredWindowStart`: `Instant` (no field annotation in current DTO); `preferredWindowEnd`: `Instant` (no field annotation in current DTO) |
| Response type | `ResponseEntity<ApiResponse<ConsultationRequestResponse>>` |
| Response payload fields | `id`: `UUID` (no field annotation in current DTO); `expertProfileId`: `UUID` (no field annotation in current DTO); `counterpartDisplayName`: `String` (no field annotation in current DTO); `counterpartAvatarUrl`: `String` (no field annotation in current DTO); `topic`: `String` (no field annotation in current DTO); `description`: `String` (no field annotation in current DTO); `preferredWindowStart`: `Instant` (no field annotation in current DTO); `preferredWindowEnd`: `Instant` (no field annotation in current DTO); `status`: `String` (no field annotation in current DTO); `rejectReason`: `String` (no field annotation in current DTO); `directConversationId`: `UUID` (no field annotation in current DTO); `respondedAt`: `Instant` (no field annotation in current DTO); `expiresAt`: `Instant` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO) |
| Explicit/documented statuses | `200, 201` |
| Positive test mapping | `COND-API-001` / `UC-EX-08-TC-API-001` |
| Negative test mapping | `COND-API-001-VAL` / `UC-EX-08-TC-API-001-VAL`; plus `COND-AUTH` for protected access |

### 9.2 Handler Contract — `GET /api/v1/consultation-requests/mine`

| Item | Exact current contract |
| --- | --- |
| Handler | `listMine` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consultation/controller/ConsultationRequestController.java` |
| Authorization annotation / boundary | hasAnyRole('MOTHER', 'FAMILY') |
| Parameters | query `status`: `ConsultationRequestStatus`; query `page`: `int`; query `size`: `int`; principal `principal`: `Principal` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<PaginatedResponse<ConsultationRequestSummaryResponse>>` |
| Response payload fields | Not applicable or unresolved from the handler import |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-002` / `UC-EX-08-TC-API-002` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.3 Handler Contract — `GET /api/v1/consultation-requests/{id}`

| Item | Exact current contract |
| --- | --- |
| Handler | `getById` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consultation/controller/ConsultationRequestController.java` |
| Authorization annotation / boundary | hasAnyRole('MOTHER', 'FAMILY', 'EXPERT') |
| Parameters | path `id`: `UUID`; principal `principal`: `Principal` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<ConsultationRequestResponse>>` |
| Response payload fields | `id`: `UUID` (no field annotation in current DTO); `expertProfileId`: `UUID` (no field annotation in current DTO); `counterpartDisplayName`: `String` (no field annotation in current DTO); `counterpartAvatarUrl`: `String` (no field annotation in current DTO); `topic`: `String` (no field annotation in current DTO); `description`: `String` (no field annotation in current DTO); `preferredWindowStart`: `Instant` (no field annotation in current DTO); `preferredWindowEnd`: `Instant` (no field annotation in current DTO); `status`: `String` (no field annotation in current DTO); `rejectReason`: `String` (no field annotation in current DTO); `directConversationId`: `UUID` (no field annotation in current DTO); `respondedAt`: `Instant` (no field annotation in current DTO); `expiresAt`: `Instant` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-003` / `UC-EX-08-TC-API-003` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.4 Handler Contract — `PATCH /api/v1/consultation-requests/{id}/cancel`

| Item | Exact current contract |
| --- | --- |
| Handler | `cancel` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consultation/controller/ConsultationRequestController.java` |
| Authorization annotation / boundary | hasAnyRole('MOTHER', 'FAMILY') |
| Parameters | path `id`: `UUID`; principal `principal`: `Principal` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<ConsultationRequestResponse>>` |
| Response payload fields | `id`: `UUID` (no field annotation in current DTO); `expertProfileId`: `UUID` (no field annotation in current DTO); `counterpartDisplayName`: `String` (no field annotation in current DTO); `counterpartAvatarUrl`: `String` (no field annotation in current DTO); `topic`: `String` (no field annotation in current DTO); `description`: `String` (no field annotation in current DTO); `preferredWindowStart`: `Instant` (no field annotation in current DTO); `preferredWindowEnd`: `Instant` (no field annotation in current DTO); `status`: `String` (no field annotation in current DTO); `rejectReason`: `String` (no field annotation in current DTO); `directConversationId`: `UUID` (no field annotation in current DTO); `respondedAt`: `Instant` (no field annotation in current DTO); `expiresAt`: `Instant` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-004` / `UC-EX-08-TC-API-004` |
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
| `VG-01` | Choose an eligible expert and enter request details. | `COND-01` | `UC-EX-08-TC-001` |
| `VG-02` | Submit and track the server lifecycle state. | `COND-02` | `UC-EX-08-TC-002` |
| `VG-03` | Cancel an owned request only while cancellation is allowed. | `COND-03` | `UC-EX-08-TC-003` |
| `VG-AUTH` | Reject wrong authentication/role/ownership/membership/consent scope | `COND-AUTH` | `UC-EX-08-TC-SEC-001` |
| `VG-GAP` | Characterize each known gap without claiming a false completed path | `COND-GAP` | `UC-EX-08-TC-GAP-001` |

## 14. Verification Methods

Existing focused evidence:

- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/consultation/service/impl/ConsultationRequestServiceImplCreateTest.java`
- `05_Development/CareBridgeMobileApp/test/features/consultation/consultation_request_mobile_test.dart`

Exact supported commands derived from the audited test paths:

- `cd 05_Development/CareBridgeAPI && ./mvnw -Dtest=ConsultationRequestServiceImplCreateTest test`
- `cd 05_Development/CareBridgeMobileApp && flutter test test/features/consultation/consultation_request_mobile_test.dart`

Record pass/fail/skip counts only after executing these commands on the exact revision.

## 15. Verification Samples

| Sample | Value |
| --- | --- |
| Primary contract | POST `/api/v1/consultation-requests` |
| Request | `POST /api/v1/consultation-requests` → `create`; `CreateConsultationRequestRequest` with `clientRequestId`: `UUID` (@NotNull); `expertProfileId`: `UUID` (@NotNull); `topic`: `String` (@NotBlank, @Size(max = 200)); `description`: `String` (@NotBlank, @Size(max = 2000)); `preferredWindowStart`: `Instant` (no field annotation in current DTO); `preferredWindowEnd`: `Instant` (no field annotation in current DTO); authorization: hasAnyRole('MOTHER', 'FAMILY'). |
| Success response | `ResponseEntity<ApiResponse<ConsultationRequestResponse>>` with `id`: `UUID` (no field annotation in current DTO); `expertProfileId`: `UUID` (no field annotation in current DTO); `counterpartDisplayName`: `String` (no field annotation in current DTO); `counterpartAvatarUrl`: `String` (no field annotation in current DTO); `topic`: `String` (no field annotation in current DTO); `description`: `String` (no field annotation in current DTO); `preferredWindowStart`: `Instant` (no field annotation in current DTO); `preferredWindowEnd`: `Instant` (no field annotation in current DTO); `status`: `String` (no field annotation in current DTO); `rejectReason`: `String` (no field annotation in current DTO); `directConversationId`: `UUID` (no field annotation in current DTO); `respondedAt`: `Instant` (no field annotation in current DTO); `expiresAt`: `Instant` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO); explicit/documented statuses `200, 201`. |
| Negative sample | Use wrong role/owner/state and assert the exact mapped error without protected payload. |

## 16. Authorization Matrix

| Actor / role | Operation | Decision |
| --- | --- | --- |
| Mother | `POST /api/v1/consultation-requests` | hasAnyRole('MOTHER', 'FAMILY'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consultation/controller/ConsultationRequestController.java` |
| Mother | `GET /api/v1/consultation-requests/mine` | hasAnyRole('MOTHER', 'FAMILY'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consultation/controller/ConsultationRequestController.java` |
| Mother | `GET /api/v1/consultation-requests/{id}` | hasAnyRole('MOTHER', 'FAMILY', 'EXPERT'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consultation/controller/ConsultationRequestController.java` |
| Mother | `PATCH /api/v1/consultation-requests/{id}/cancel` | hasAnyRole('MOTHER', 'FAMILY'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consultation/controller/ConsultationRequestController.java` |
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
