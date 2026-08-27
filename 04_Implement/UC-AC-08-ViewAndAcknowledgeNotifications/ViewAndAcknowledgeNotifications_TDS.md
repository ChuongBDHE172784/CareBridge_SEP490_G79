# ENGINEERING DOCUMENTATION STANDARD (EDS) v2.0

# TECHNICAL DESIGN SPECIFICATION — View and Acknowledge Notifications

| Field | Value |
| --- | --- |
| Document ID | `UC-AC-08-TDS` |
| Version | `0.1` |
| Date | `2026-08-23` |
| Status | `Draft` |
| Function ID | `UC-AC-08` |
| Canonical Use Case | `UC-AC-08 — View and Acknowledge Notifications` |
| Module / Bounded Context | `Access, Identity, and Trust` |
| Primary Actor | `Authenticated User` |
| Platforms | `Mobile / Web role portals / Backend` |
| Priority | `High` |
| Data Classification | `Confidential identity, authentication/session, notification-device, privacy, and consent data; credentials/tokens are secrets` |
| Compliance Scope | `PDPA purpose limitation, authentication secrecy, session/device ownership, consent proof, and audit redaction` |
| Owner | `CareBridge Team` |
| Reviewer / Approver |  |
| Source Baseline | Current worktree on `2026-08-23`; SRS `UC-AC-08`; exact evidence in Section 1.4 |

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

- **Goal:** View account notifications, mark one or all notifications read, and register or deregister the current device token for delivery.
- **Trigger:** The actor enters Mobile `/notifications`.
- **Outcome:** Mark one/all items read and reconcile the current device token lifecycle.
- **Current state:** `High` confidence from reachable code/test audit; no manifest-level exclusion is recorded.
- **Target state:** Preserve current code-backed behavior and resolve only explicitly evidenced limitations through approved implementation work.

### 1.2 Scope

**In scope**

- Mobile `/notifications`
- Web `/admin/notifications`
- Web `/content/notifications`

- GET `/api/v1/notifications/me`
- PUT `/api/v1/notifications/{id}/read`
- PUT `/api/v1/notifications/read-all`
- POST `/api/v1/notifications/device-token`
- DELETE `/api/v1/notifications/device-token`

**Out of scope / limitations**

- Not applicable — no manifest-level exclusion is recorded. Scope is limited to the entry points and exact contracts listed above; unrelated handlers in the same module are excluded.

### 1.3 Preconditions and Postconditions

| Type | Condition |
| --- | --- |
| Precondition | Authenticated User is authenticated/authorized where the current contract requires it. |
| Precondition | Required ownership, membership, consent, resource state, and device/provider prerequisites pass current policies. |
| Postcondition | Mark one/all items read and reconcile the current device token lifecycle. |
| Postcondition | No side effect outside the feature-owned persistence/event/provider boundary occurs. |

### 1.4 Evidence Baseline

| Source ID | Type | Exact path | Revision | Authority |
| --- | --- | --- | --- | --- |
| `SRC-SRS-01` | Requirement | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-AC-08 | 2026-08-23 | Draft code-first requirement |
| `SRC-CODE-01` | Current code | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/controller/NotificationController.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-02` | Current code | `05_Development/CareBridgeMobileApp/lib/features/notification/screens/notification_center_screen.dart` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-03` | Current code | `05_Development/CareBridgeWebApp/src/features/notification/pages/NotificationCenterPage.tsx` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-04` | Current code | `05_Development/CareBridgeMobileApp/lib/core/notifications/fcm_service.dart` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-01` | Existing test | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/notification/NotificationControllerContractTest.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-02` | Existing test | `05_Development/CareBridgeMobileApp/test/features/notification/emergency_notification_routing_test.dart` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-ERROR-01` | Current exception advice | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/common/exception/GlobalExceptionHandler.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-SUPP-01` | Historical architecture/design evidence | `04_Implement/EpdsFamilyNotification/EpdsFamilyNotification_Architecture-Evidence.md` | Worktree `2026-08-23` | Historical evidence only; current code and canonical SRS/TDS override conflicts |
| `SRC-SUPP-02` | Historical verification evidence | `04_Implement/EpdsFamilyNotification/EpdsFamilyNotification_Verification-Evidence.md` | Worktree `2026-08-23` | Historical evidence only; current code and canonical SRS/TDS override conflicts |

### 1.5 Open Contradictions / Questions

- No material contradiction was found among the exact sources cited in Section 1.4. Behavior not evidenced by those sources is not approved or implied by this Draft.

## 2. Traceability Matrix

| Requirement | Behavior | Exact oracle source | Component | Test condition / case |
| --- | --- | --- | --- | --- |
| `UC-AC-08-FR-01` | Load notifications belonging to the authenticated account. | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-AC-08 Normal Flow 1 | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/controller/NotificationController.java` | `COND-01` / `UC-AC-08-TC-001` |
| `UC-AC-08-FR-02` | Open or acknowledge an eligible notification. | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-AC-08 Normal Flow 2 | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/controller/NotificationController.java` | `COND-02` / `UC-AC-08-TC-002` |
| `UC-AC-08-FR-03` | Mark one/all items read and reconcile the current device token lifecycle. | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-AC-08 Normal Flow 3 | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/controller/NotificationController.java` | `COND-03` / `UC-AC-08-TC-003` |
| `BR-01` | Notification ownership and unread counts are server authoritative. | `05_Development/CareBridgeMobileApp/lib/core/notifications/fcm_service.dart` | `05_Development/CareBridgeMobileApp/lib/core/notifications/fcm_service.dart` | `COND-BR-01` / `UC-AC-08-TC-BR-001` |
| `BR-02` | Device tokens belong to the authenticated account and must not expose another user's notifications. | `05_Development/CareBridgeMobileApp/lib/core/notifications/fcm_service.dart` | `05_Development/CareBridgeMobileApp/lib/core/notifications/fcm_service.dart` | `COND-BR-02` / `UC-AC-08-TC-BR-002` |

## 3. Architecture Decision Records (ADR)

### ADR-UC-AC-08-01 — Use a distinct actor-goal boundary

| Item | Decision |
| --- | --- |
| Context | The retired 43-UC catalogue grouped multiple triggers, lifecycles, and permission boundaries, making implementation/test traceability generic. |
| Options | Keep the broad catalogue; split by screen; split by actor goal plus lifecycle/security boundary. |
| Decision | Use `UC-AC-08 — View and Acknowledge Notifications` as the canonical boundary because its operations share the stated actor outcome and current implementation evidence. |
| Consequences | Related supporting screens/endpoints stay in one TDS; different lifecycle/actor outcomes have separate UCs. |
| Source / Status | SRS Section 3.1 and current code audit / Draft |

### ADR-UC-AC-08-02 — Preserve unknowns as Open

| Item | Decision |
| --- | --- |
| Context | Exact schema fields, SLA values, and some controller error codes are not fully evidenced by this manifest alone. |
| Decision | Do not invent them. Mark them `Open` and require the exact DTO/entity/migration/policy oracle before production-code changes. |
| Consequences | This document accurately characterizes current scope; unresolved design-changing items block implementation approval. |
| Source / Status | `create-specs` evidence discipline / Draft |

## 4. Non-Functional Requirements and SLA

| NFR | Target | Oracle Source | Verification |
| --- | --- | --- | --- |
| Authorization / isolation | All requests follow exact role/ownership/membership/consent policy in current code. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/controller/NotificationController.java` | Negative security cases in paired Test-Spec |
| Protected-data handling | No secrets or unnecessary health/location/identity/conversation/file payloads in logs, fixtures, screenshots, or audit detail. | Data classification header plus exact Section 9 request/response field inventories | Log/fixture review plus security tests |
| Availability / latency | Open — no approved feature-specific numeric SLA found. | Evidence needed: approved NFR/SLA source | Measure only; do not assert a fixed threshold |
| Accessibility | Open — confirm project-standard criteria for reachable UI surfaces. | Evidence needed: approved UX/accessibility standard | Applicable Web/Mobile UI checks |
| Retry / idempotency | Apply only semantics explicitly implemented by the owning service. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/controller/NotificationController.java` | State/duplicate/concurrency cases where applicable |

## 5. Static Modeling

### 5.1 Component Responsibilities and Change Disposition

| Exact path | Disposition | Responsibility |
| --- | --- | --- |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/controller/NotificationController.java` | Reuse | Current implementation evidence for View and Acknowledge Notifications; inspect the exact symbol before implementation changes. |
| `05_Development/CareBridgeMobileApp/lib/features/notification/screens/notification_center_screen.dart` | Reuse | Current implementation evidence for View and Acknowledge Notifications; inspect the exact symbol before implementation changes. |
| `05_Development/CareBridgeWebApp/src/features/notification/pages/NotificationCenterPage.tsx` | Reuse | Current implementation evidence for View and Acknowledge Notifications; inspect the exact symbol before implementation changes. |
| `05_Development/CareBridgeMobileApp/lib/core/notifications/fcm_service.dart` | Reuse | Current implementation evidence for View and Acknowledge Notifications; inspect the exact symbol before implementation changes. |

### 5.2 Current Component Diagram

```plantuml
@startuml
skinparam classAttributeIconSize 0
class NotificationController as "NotificationController.java"
class notification_center_screen as "notification_center_screen.dart"
NotificationController --> notification_center_screen
class NotificationCenterPage as "NotificationCenterPage.tsx"
notification_center_screen --> NotificationCenterPage
class fcm_service as "fcm_service.dart"
NotificationCenterPage --> fcm_service
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
Actor -> Client: Enter View and Acknowledge Notifications
Client -> Domain: Load notifications belonging to the authenticated account.
Domain --> Client: Result for step 1
Client -> Domain: Open or acknowledge an eligible notification.
Domain --> Client: Result for step 2
Client -> Domain: Mark one/all items read and reconcile the current device token lifecycle.
Domain --> Client: Result for step 3
Client --> Actor: Render canonical outcome
@enduml
```

### 6.2 Alternative, Error, Retry, and Concurrency Flows

| Flow | Expected design behavior | Oracle |
| --- | --- | --- |
| Cancel before mutation | No unintended write or provider side effect. | SRS UC-AC-08 Alternative Flow |
| Invalid input/state | Reject with the current contract; keep canonical state unchanged. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/controller/NotificationController.java` |
| Wrong actor/scope | Fail closed without protected resource disclosure. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/controller/NotificationController.java` |
| Dependency failure | Use only the implemented bounded fallback/retry; never report false success. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/dto/MarkAllReadResponse.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/dto/NotificationRecordResponse.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/dto/RegisterDeviceTokenRequest.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/dto/SendNotificationRequest.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/service/NotificationService.java` |
| Duplicate/concurrent mutation | Apply only current lock/version/idempotency semantics. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/controller/NotificationController.java` |

### 6.3 State Model and Invariants

```plantuml
@startuml
[*] --> Eligible
Eligible --> InProgress : Load notifications belonging to the authenticated account.
InProgress --> Outcome : Mark one/all items read and reconcile the current device token lifecycle.
InProgress --> Rejected : validation / authorization / state failure
Outcome --> [*]
Rejected --> Eligible : actor corrects eligible input
@enduml
```

- Notification ownership and unread counts are server authoritative.
- Device tokens belong to the authenticated account and must not expose another user's notifications.

## 7. Domain Event Catalog

| Direction | Event | Producer / Consumer | Payload / delivery / idempotency |
| --- | --- | --- | --- |
| Publish / consume | Current event evidence | Not applicable at this cited baseline — no event type, publisher, or listener is evidenced in the listed implementation sources | Preserve only events evidenced by the cited source set; if later call-path inspection finds none, the flow remains synchronous. |

## 8. Interface Specification

### 8.1 User / Operator Interfaces

| # | Entry point | Actor | Contract |
| ---: | --- | --- | --- |
| 1 | Mobile `/notifications` | Authenticated User | Reachable current entry point |
| 2 | Web `/admin/notifications` | Authenticated User | Reachable current entry point |
| 3 | Web `/content/notifications` | Authenticated User | Reachable current entry point |

### 8.2 Service / Repository Interfaces

| Interface evidence | Required responsibility |
| --- | --- |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/controller/NotificationController.java` | Support the mapped operations without broadening authorization or lifecycle semantics. |
| `05_Development/CareBridgeMobileApp/lib/features/notification/screens/notification_center_screen.dart` | Support the mapped operations without broadening authorization or lifecycle semantics. |
| `05_Development/CareBridgeWebApp/src/features/notification/pages/NotificationCenterPage.tsx` | Support the mapped operations without broadening authorization or lifecycle semantics. |
| `05_Development/CareBridgeMobileApp/lib/core/notifications/fcm_service.dart` | Support the mapped operations without broadening authorization or lifecycle semantics. |

## 9. API Specification

| ID | Method / path or grouped controller surface | Auth / role | Request / response / errors |
| --- | --- | --- | --- |
| `API-01` | `DELETE /api/v1/notifications/device-token` | isAuthenticated() | Handler `deregisterDeviceToken`; parameters: query `token`: `String`; principal `principal`: `Principal`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<Void>>`; response payload fields: Not applicable or unresolved from the handler import; explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/controller/NotificationController.java` |
| `API-02` | `POST /api/v1/notifications/device-token` | isAuthenticated() | Handler `registerDeviceToken`; parameters: body `request`: `RegisterDeviceTokenRequest`; principal `principal`: `Principal`; request body: `RegisterDeviceTokenRequest`; request fields/validation: `token`: `String` (@NotBlank, @Size(max = 512)); `platform`: `DevicePlatform` (@NotNull); response: `ResponseEntity<ApiResponse<Void>>`; response payload fields: Not applicable or unresolved from the handler import; explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/controller/NotificationController.java` |
| `API-03` | `GET /api/v1/notifications/me` | isAuthenticated() | Handler `getMyNotifications`; parameters: query `type`: `String`; query `page`: `int`; query `size`: `int`; principal `principal`: `Principal`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<Page<NotificationRecordResponse>>>`; response payload fields: `id`: `UUID` (no field annotation in current DTO); `userId`: `UUID` (no field annotation in current DTO); `type`: `String` (no field annotation in current DTO); `title`: `String` (no field annotation in current DTO); `body`: `String` (no field annotation in current DTO); `referenceId`: `UUID` (no field annotation in current DTO); `referenceType`: `String` (no field annotation in current DTO); `status`: `String` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO); `sentAt`: `Instant` (no field annotation in current DTO); `isRead`: `boolean` (no field annotation in current DTO); `readAt`: `Instant` (no field annotation in current DTO); `channel`: `String` (no field annotation in current DTO); `fcmMessageId`: `String` (no field annotation in current DTO); `attemptCount`: `int` (no field annotation in current DTO); `failedAt`: `Instant` (no field annotation in current DTO); `updatedAt`: `Instant` (no field annotation in current DTO); `metadata`: `Map<String, String>` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/controller/NotificationController.java` |
| `API-04` | `PUT /api/v1/notifications/read-all` | isAuthenticated() | Handler `markAllAsRead`; parameters: principal `principal`: `Principal`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<MarkAllReadResponse>>`; response payload fields: `markedCount`: `int` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/controller/NotificationController.java` |
| `API-05` | `PUT /api/v1/notifications/{notificationId}/read` | isAuthenticated() | Handler `markAsRead`; parameters: path `notificationId`: `UUID`; principal `principal`: `Principal`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<Void>>`; response payload fields: Not applicable or unresolved from the handler import; explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/controller/NotificationController.java` |

Method-level Spring handlers, authorization annotations, request DTO fields/validators, response payload fields, and explicit/documented statuses above were extracted from the cited current source. Service/advice-only application error codes are not claimed where the controller does not declare them.

### 9.1 Handler Contract — `DELETE /api/v1/notifications/device-token`

| Item | Exact current contract |
| --- | --- |
| Handler | `deregisterDeviceToken` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/controller/NotificationController.java` |
| Authorization annotation / boundary | isAuthenticated() |
| Parameters | query `token`: `String`; principal `principal`: `Principal` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<Void>>` |
| Response payload fields | Not applicable or unresolved from the handler import |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-001` / `UC-AC-08-TC-API-001` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.2 Handler Contract — `POST /api/v1/notifications/device-token`

| Item | Exact current contract |
| --- | --- |
| Handler | `registerDeviceToken` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/controller/NotificationController.java` |
| Authorization annotation / boundary | isAuthenticated() |
| Parameters | body `request`: `RegisterDeviceTokenRequest`; principal `principal`: `Principal` |
| Request body type | `RegisterDeviceTokenRequest` |
| Request fields and validators | `token`: `String` (@NotBlank, @Size(max = 512)); `platform`: `DevicePlatform` (@NotNull) |
| Response type | `ResponseEntity<ApiResponse<Void>>` |
| Response payload fields | Not applicable or unresolved from the handler import |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-002` / `UC-AC-08-TC-API-002` |
| Negative test mapping | `COND-API-002-VAL` / `UC-AC-08-TC-API-002-VAL`; plus `COND-AUTH` for protected access |

### 9.3 Handler Contract — `GET /api/v1/notifications/me`

| Item | Exact current contract |
| --- | --- |
| Handler | `getMyNotifications` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/controller/NotificationController.java` |
| Authorization annotation / boundary | isAuthenticated() |
| Parameters | query `type`: `String`; query `page`: `int`; query `size`: `int`; principal `principal`: `Principal` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<Page<NotificationRecordResponse>>>` |
| Response payload fields | `id`: `UUID` (no field annotation in current DTO); `userId`: `UUID` (no field annotation in current DTO); `type`: `String` (no field annotation in current DTO); `title`: `String` (no field annotation in current DTO); `body`: `String` (no field annotation in current DTO); `referenceId`: `UUID` (no field annotation in current DTO); `referenceType`: `String` (no field annotation in current DTO); `status`: `String` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO); `sentAt`: `Instant` (no field annotation in current DTO); `isRead`: `boolean` (no field annotation in current DTO); `readAt`: `Instant` (no field annotation in current DTO); `channel`: `String` (no field annotation in current DTO); `fcmMessageId`: `String` (no field annotation in current DTO); `attemptCount`: `int` (no field annotation in current DTO); `failedAt`: `Instant` (no field annotation in current DTO); `updatedAt`: `Instant` (no field annotation in current DTO); `metadata`: `Map<String, String>` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-003` / `UC-AC-08-TC-API-003` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.4 Handler Contract — `PUT /api/v1/notifications/read-all`

| Item | Exact current contract |
| --- | --- |
| Handler | `markAllAsRead` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/controller/NotificationController.java` |
| Authorization annotation / boundary | isAuthenticated() |
| Parameters | principal `principal`: `Principal` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<MarkAllReadResponse>>` |
| Response payload fields | `markedCount`: `int` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-004` / `UC-AC-08-TC-API-004` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.5 Handler Contract — `PUT /api/v1/notifications/{notificationId}/read`

| Item | Exact current contract |
| --- | --- |
| Handler | `markAsRead` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/controller/NotificationController.java` |
| Authorization annotation / boundary | isAuthenticated() |
| Parameters | path `notificationId`: `UUID`; principal `principal`: `Principal` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<Void>>` |
| Response payload fields | Not applicable or unresolved from the handler import |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-005` / `UC-AC-08-TC-API-005` |
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
| `VG-01` | Load notifications belonging to the authenticated account. | `COND-01` | `UC-AC-08-TC-001` |
| `VG-02` | Open or acknowledge an eligible notification. | `COND-02` | `UC-AC-08-TC-002` |
| `VG-03` | Mark one/all items read and reconcile the current device token lifecycle. | `COND-03` | `UC-AC-08-TC-003` |
| `VG-AUTH` | Reject wrong authentication/role/ownership/membership/consent scope | `COND-AUTH` | `UC-AC-08-TC-SEC-001` |
| `VG-GAP` | Characterize each known gap without claiming a false completed path | `COND-GAP` | `UC-AC-08-TC-GAP-001` |

## 14. Verification Methods

Existing focused evidence:

- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/notification/NotificationControllerContractTest.java`
- `05_Development/CareBridgeMobileApp/test/features/notification/emergency_notification_routing_test.dart`

Exact supported commands derived from the audited test paths:

- `cd 05_Development/CareBridgeAPI && ./mvnw -Dtest=NotificationControllerContractTest test`
- `cd 05_Development/CareBridgeMobileApp && flutter test test/features/notification/emergency_notification_routing_test.dart`

Record pass/fail/skip counts only after executing these commands on the exact revision.

## 15. Verification Samples

| Sample | Value |
| --- | --- |
| Primary contract | GET `/api/v1/notifications/me` |
| Request | `DELETE /api/v1/notifications/device-token` → `deregisterDeviceToken`; `None` with Not applicable — no request body; authorization: isAuthenticated(). |
| Success response | `ResponseEntity<ApiResponse<Void>>` with Not applicable or unresolved from the handler import; explicit/documented statuses `200`. |
| Negative sample | Use wrong role/owner/state and assert the exact mapped error without protected payload. |

## 16. Authorization Matrix

| Actor / role | Operation | Decision |
| --- | --- | --- |
| Authenticated User | `DELETE /api/v1/notifications/device-token` | isAuthenticated(); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/controller/NotificationController.java` |
| Authenticated User | `POST /api/v1/notifications/device-token` | isAuthenticated(); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/controller/NotificationController.java` |
| Authenticated User | `GET /api/v1/notifications/me` | isAuthenticated(); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/controller/NotificationController.java` |
| Authenticated User | `PUT /api/v1/notifications/read-all` | isAuthenticated(); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/controller/NotificationController.java` |
| Authenticated User | `PUT /api/v1/notifications/{notificationId}/read` | isAuthenticated(); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/controller/NotificationController.java` |
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
