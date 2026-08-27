# ENGINEERING DOCUMENTATION STANDARD (EDS) v2.0

# TECHNICAL DESIGN SPECIFICATION — Make and Receive Voice/Video Calls

| Field | Value |
| --- | --- |
| Document ID | `UC-EX-11-TDS` |
| Version | `0.1` |
| Date | `2026-08-23` |
| Status | `Draft` |
| Function ID | `UC-EX-11` |
| Canonical Use Case | `UC-EX-11 — Make and Receive Voice/Video Calls` |
| Module / Bounded Context | `Expert and Consultation` |
| Primary Actor | `Mother / Expert` |
| Platforms | `Mobile / Web / Backend / Zego` |
| Priority | `Medium` |
| Data Classification | `Confidential professional and consultation data; Restricted identity/credential evidence and purpose-bound attachments` |
| Compliance Scope | `PDPA purpose limitation, least privilege, purpose-bound file access, consultation confidentiality, and consent-aware recording` |
| Owner | `CareBridge Team` |
| Reviewer / Approver |  |
| Source Baseline | Current worktree on `2026-08-23`; SRS `UC-EX-11`; exact evidence in Section 1.4 |

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

- **Goal:** Create, receive, join, end, or decline a direct voice/video call and apply consent-aware recording behavior.
- **Trigger:** The actor enters Call controls inside Mobile/Web direct conversation rooms.
- **Outcome:** End/decline and handle recording only after the implemented consent attestation.
- **Current state:** `High` confidence from reachable code/test audit; no manifest-level exclusion is recorded.
- **Target state:** Preserve current code-backed behavior and resolve only explicitly evidenced limitations through approved implementation work.

### 1.2 Scope

**In scope**

- Call controls inside Mobile/Web direct conversation rooms

- Call lifecycle endpoints under `/api/v1/direct-conversations/{conversationId}/calls/**`
- GET `/api/v1/direct-conversations/calls/active`

**Out of scope / limitations**

- Not applicable — no manifest-level exclusion is recorded. Scope is limited to the entry points and exact contracts listed above; unrelated handlers in the same module are excluded.

### 1.3 Preconditions and Postconditions

| Type | Condition |
| --- | --- |
| Precondition | Mother / Expert is authenticated/authorized where the current contract requires it. |
| Precondition | Required ownership, membership, consent, resource state, and device/provider prerequisites pass current policies. |
| Postcondition | End/decline and handle recording only after the implemented consent attestation. |
| Postcondition | No side effect outside the feature-owned persistence/event/provider boundary occurs. |

### 1.4 Evidence Baseline

| Source ID | Type | Exact path | Revision | Authority |
| --- | --- | --- | --- | --- |
| `SRC-SRS-01` | Requirement | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-EX-11 | 2026-08-23 | Draft code-first requirement |
| `SRC-CODE-01` | Current code | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/ConversationCallController.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-02` | Current code | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/ActiveConversationCallController.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-03` | Current code | `05_Development/CareBridgeMobileApp/lib/features/directChat/calls/direct_call_coordinator.dart` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-04` | Current code | `05_Development/CareBridgeWebApp/src/features/directChat/calls/DirectCallProvider.tsx` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-01` | Existing test | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/directchat/service/impl/ConversationCallServiceImplTest.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-02` | Existing test | `05_Development/CareBridgeMobileApp/test/features/directChat/calls/direct_call_coordinator_test.dart` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-03` | Existing test | `05_Development/CareBridgeMobileApp/test/features/directChat/calls/rtc_permissions_test.dart` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-04` | Existing test | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/integration/zegocloud/ZegoCloudServiceImplTest.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-ERROR-01` | Current exception advice | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/common/exception/GlobalExceptionHandler.java` | Worktree `2026-08-23` | Current-state evidence |

### 1.5 Open Contradictions / Questions

- No material contradiction was found among the exact sources cited in Section 1.4. Behavior not evidenced by those sources is not approved or implied by this Draft.

## 2. Traceability Matrix

| Requirement | Behavior | Exact oracle source | Component | Test condition / case |
| --- | --- | --- | --- | --- |
| `UC-EX-11-FR-01` | Initiate or receive a call inside an authorized conversation. | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-EX-11 Normal Flow 1 | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/ActiveConversationCallController.java` | `COND-01` / `UC-EX-11-TC-001` |
| `UC-EX-11-FR-02` | Obtain provider join credentials and join with granted device permissions. | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-EX-11 Normal Flow 2 | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/ConversationCallController.java` | `COND-02` / `UC-EX-11-TC-002` |
| `UC-EX-11-FR-03` | End/decline and handle recording only after the implemented consent attestation. | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-EX-11 Normal Flow 3 | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/ConversationCallController.java` | `COND-03` / `UC-EX-11-TC-003` |
| `BR-01` | Conversation membership and call state are server authoritative. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/ActiveConversationCallController.java` | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/ActiveConversationCallController.java` | `COND-BR-01` / `UC-EX-11-TC-BR-001` |
| `BR-02` | Provider secrets never belong in UI state; recording requires implemented consent. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/ConversationCallController.java` | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/ConversationCallController.java` | `COND-BR-02` / `UC-EX-11-TC-BR-002` |

## 3. Architecture Decision Records (ADR)

### ADR-UC-EX-11-01 — Use a distinct actor-goal boundary

| Item | Decision |
| --- | --- |
| Context | The retired 43-UC catalogue grouped multiple triggers, lifecycles, and permission boundaries, making implementation/test traceability generic. |
| Options | Keep the broad catalogue; split by screen; split by actor goal plus lifecycle/security boundary. |
| Decision | Use `UC-EX-11 — Make and Receive Voice/Video Calls` as the canonical boundary because its operations share the stated actor outcome and current implementation evidence. |
| Consequences | Related supporting screens/endpoints stay in one TDS; different lifecycle/actor outcomes have separate UCs. |
| Source / Status | SRS Section 3.1 and current code audit / Draft |

### ADR-UC-EX-11-02 — Preserve unknowns as Open

| Item | Decision |
| --- | --- |
| Context | Exact schema fields, SLA values, and some controller error codes are not fully evidenced by this manifest alone. |
| Decision | Do not invent them. Mark them `Open` and require the exact DTO/entity/migration/policy oracle before production-code changes. |
| Consequences | This document accurately characterizes current scope; unresolved design-changing items block implementation approval. |
| Source / Status | `create-specs` evidence discipline / Draft |

## 4. Non-Functional Requirements and SLA

| NFR | Target | Oracle Source | Verification |
| --- | --- | --- | --- |
| Authorization / isolation | All requests follow exact role/ownership/membership/consent policy in current code. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/ConversationCallController.java` | Negative security cases in paired Test-Spec |
| Protected-data handling | No secrets or unnecessary health/location/identity/conversation/file payloads in logs, fixtures, screenshots, or audit detail. | Data classification header plus exact Section 9 request/response field inventories | Log/fixture review plus security tests |
| Availability / latency | Open — no approved feature-specific numeric SLA found. | Evidence needed: approved NFR/SLA source | Measure only; do not assert a fixed threshold |
| Accessibility | Open — confirm project-standard criteria for reachable UI surfaces. | Evidence needed: approved UX/accessibility standard | Applicable Web/Mobile UI checks |
| Retry / idempotency | Apply only semantics explicitly implemented by the owning service. | No explicit lock/version/idempotency marker is evidenced in the cited implementation sources; preserve observed behavior and add characterization before changing concurrency semantics | State/duplicate/concurrency cases where applicable |

## 5. Static Modeling

### 5.1 Component Responsibilities and Change Disposition

| Exact path | Disposition | Responsibility |
| --- | --- | --- |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/ConversationCallController.java` | Reuse | Current implementation evidence for Make and Receive Voice/Video Calls; inspect the exact symbol before implementation changes. |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/ActiveConversationCallController.java` | Reuse | Current implementation evidence for Make and Receive Voice/Video Calls; inspect the exact symbol before implementation changes. |
| `05_Development/CareBridgeMobileApp/lib/features/directChat/calls/direct_call_coordinator.dart` | Reuse | Current implementation evidence for Make and Receive Voice/Video Calls; inspect the exact symbol before implementation changes. |
| `05_Development/CareBridgeWebApp/src/features/directChat/calls/DirectCallProvider.tsx` | Reuse | Current implementation evidence for Make and Receive Voice/Video Calls; inspect the exact symbol before implementation changes. |

### 5.2 Current Component Diagram

```plantuml
@startuml
skinparam classAttributeIconSize 0
class ConversationCallController as "ConversationCallController.java"
class ActiveConversationCallController as "ActiveConversationCallController.java"
ConversationCallController --> ActiveConversationCallController
class direct_call_coordinator as "direct_call_coordinator.dart"
ActiveConversationCallController --> direct_call_coordinator
class DirectCallProvider as "DirectCallProvider.tsx"
direct_call_coordinator --> DirectCallProvider
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
Actor -> Client: Enter Make and Receive Voice/Video Calls
Client -> Domain: Initiate or receive a call inside an authorized conversation.
Domain --> Client: Result for step 1
Client -> Domain: Obtain provider join credentials and join with granted device permissions.
Domain --> Client: Result for step 2
Client -> Domain: End/decline and handle recording only after the implemented consent attestation.
Domain --> Client: Result for step 3
Client --> Actor: Render canonical outcome
@enduml
```

### 6.2 Alternative, Error, Retry, and Concurrency Flows

| Flow | Expected design behavior | Oracle |
| --- | --- | --- |
| Cancel before mutation | No unintended write or provider side effect. | SRS UC-EX-11 Alternative Flow |
| Invalid input/state | Reject with the current contract; keep canonical state unchanged. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/ConversationCallController.java` |
| Wrong actor/scope | Fail closed without protected resource disclosure. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/ConversationCallController.java` |
| Dependency failure | Use only the implemented bounded fallback/retry; never report false success. | Not applicable at this cited baseline — no external adapter/provider import is evidenced in the listed implementation sources |
| Duplicate/concurrent mutation | Apply only current lock/version/idempotency semantics. | No explicit lock/version/idempotency marker is evidenced in the cited implementation sources; preserve observed behavior and add characterization before changing concurrency semantics |

### 6.3 State Model and Invariants

```plantuml
@startuml
[*] --> Eligible
Eligible --> InProgress : Initiate or receive a call inside an authorized conversation.
InProgress --> Outcome : End/decline and handle recording only after the implemented consent attestation.
InProgress --> Rejected : validation / authorization / state failure
Outcome --> [*]
Rejected --> Eligible : actor corrects eligible input
@enduml
```

- Conversation membership and call state are server authoritative.
- Provider secrets never belong in UI state; recording requires implemented consent.

## 7. Domain Event Catalog

| Direction | Event | Producer / Consumer | Payload / delivery / idempotency |
| --- | --- | --- | --- |
| Publish / consume | Current event evidence | Not applicable at this cited baseline — no event type, publisher, or listener is evidenced in the listed implementation sources | Preserve only events evidenced by the cited source set; if later call-path inspection finds none, the flow remains synchronous. |

## 8. Interface Specification

### 8.1 User / Operator Interfaces

| # | Entry point | Actor | Contract |
| ---: | --- | --- | --- |
| 1 | Call controls inside Mobile/Web direct conversation rooms | Mother / Expert | Reachable current entry point |

### 8.2 Service / Repository Interfaces

| Interface evidence | Required responsibility |
| --- | --- |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/ConversationCallController.java` | Support the mapped operations without broadening authorization or lifecycle semantics. |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/ActiveConversationCallController.java` | Support the mapped operations without broadening authorization or lifecycle semantics. |
| `05_Development/CareBridgeMobileApp/lib/features/directChat/calls/direct_call_coordinator.dart` | Support the mapped operations without broadening authorization or lifecycle semantics. |
| `05_Development/CareBridgeWebApp/src/features/directChat/calls/DirectCallProvider.tsx` | Support the mapped operations without broadening authorization or lifecycle semantics. |

## 9. API Specification

| ID | Method / path or grouped controller surface | Auth / role | Request / response / errors |
| --- | --- | --- | --- |
| `API-01` | `GET /api/v1/direct-conversations/calls/active` | hasAnyRole('MOTHER', 'FAMILY', 'EXPERT') | Handler `listActiveCalls`; parameters: principal `principal`: `Principal`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<List<ConversationCallResponse>>>`; response payload fields: `callId`: `UUID` (no field annotation in current DTO); `conversationId`: `UUID` (no field annotation in current DTO); `initiatedByUserId`: `UUID` (no field annotation in current DTO); `callType`: `String` (no field annotation in current DTO); `callStatus`: `String` (no field annotation in current DTO); `initiatedAt`: `Instant` (no field annotation in current DTO); `answeredAt`: `Instant` (no field annotation in current DTO); `endedAt`: `Instant` (no field annotation in current DTO); `durationSeconds`: `Integer` (no field annotation in current DTO); `recordingFileId`: `UUID` (no field annotation in current DTO); `consentAttested`: `Boolean` (no field annotation in current DTO); `recordingStatus`: `String` (no field annotation in current DTO); `recordedDurationSeconds`: `Integer` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/ActiveConversationCallController.java` |
| `API-02` | `POST /api/v1/direct-conversations/{conversationId}/calls` | hasAnyRole('MOTHER', 'FAMILY', 'EXPERT') | Handler `initiateCall`; parameters: path `conversationId`: `UUID`; body `request`: `InitiateCallRequest`; principal `principal`: `Principal`; request body: `InitiateCallRequest`; request fields/validation: `callType`: `CallType` (@NotNull); response: `ResponseEntity<ApiResponse<ConversationCallResponse>>`; response payload fields: `callId`: `UUID` (no field annotation in current DTO); `conversationId`: `UUID` (no field annotation in current DTO); `initiatedByUserId`: `UUID` (no field annotation in current DTO); `callType`: `String` (no field annotation in current DTO); `callStatus`: `String` (no field annotation in current DTO); `initiatedAt`: `Instant` (no field annotation in current DTO); `answeredAt`: `Instant` (no field annotation in current DTO); `endedAt`: `Instant` (no field annotation in current DTO); `durationSeconds`: `Integer` (no field annotation in current DTO); `recordingFileId`: `UUID` (no field annotation in current DTO); `consentAttested`: `Boolean` (no field annotation in current DTO); `recordingStatus`: `String` (no field annotation in current DTO); `recordedDurationSeconds`: `Integer` (no field annotation in current DTO); explicit/documented statuses: `201`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/ConversationCallController.java` |
| `API-03` | `GET /api/v1/direct-conversations/{conversationId}/calls/{callId}` | hasAnyRole('MOTHER', 'FAMILY', 'EXPERT') | Handler `getCall`; parameters: path `conversationId`: `UUID`; path `callId`: `UUID`; principal `principal`: `Principal`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<ConversationCallResponse>>`; response payload fields: `callId`: `UUID` (no field annotation in current DTO); `conversationId`: `UUID` (no field annotation in current DTO); `initiatedByUserId`: `UUID` (no field annotation in current DTO); `callType`: `String` (no field annotation in current DTO); `callStatus`: `String` (no field annotation in current DTO); `initiatedAt`: `Instant` (no field annotation in current DTO); `answeredAt`: `Instant` (no field annotation in current DTO); `endedAt`: `Instant` (no field annotation in current DTO); `durationSeconds`: `Integer` (no field annotation in current DTO); `recordingFileId`: `UUID` (no field annotation in current DTO); `consentAttested`: `Boolean` (no field annotation in current DTO); `recordingStatus`: `String` (no field annotation in current DTO); `recordedDurationSeconds`: `Integer` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/ConversationCallController.java` |
| `API-04` | `PATCH /api/v1/direct-conversations/{conversationId}/calls/{callId}/answer` | hasAnyRole('MOTHER', 'FAMILY', 'EXPERT') | Handler `answer`; parameters: path `conversationId`: `UUID`; path `callId`: `UUID`; principal `principal`: `Principal`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<ConversationCallResponse>>`; response payload fields: `callId`: `UUID` (no field annotation in current DTO); `conversationId`: `UUID` (no field annotation in current DTO); `initiatedByUserId`: `UUID` (no field annotation in current DTO); `callType`: `String` (no field annotation in current DTO); `callStatus`: `String` (no field annotation in current DTO); `initiatedAt`: `Instant` (no field annotation in current DTO); `answeredAt`: `Instant` (no field annotation in current DTO); `endedAt`: `Instant` (no field annotation in current DTO); `durationSeconds`: `Integer` (no field annotation in current DTO); `recordingFileId`: `UUID` (no field annotation in current DTO); `consentAttested`: `Boolean` (no field annotation in current DTO); `recordingStatus`: `String` (no field annotation in current DTO); `recordedDurationSeconds`: `Integer` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/ConversationCallController.java` |
| `API-05` | `PATCH /api/v1/direct-conversations/{conversationId}/calls/{callId}/decline` | hasAnyRole('MOTHER', 'FAMILY', 'EXPERT') | Handler `decline`; parameters: path `conversationId`: `UUID`; path `callId`: `UUID`; principal `principal`: `Principal`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<ConversationCallResponse>>`; response payload fields: `callId`: `UUID` (no field annotation in current DTO); `conversationId`: `UUID` (no field annotation in current DTO); `initiatedByUserId`: `UUID` (no field annotation in current DTO); `callType`: `String` (no field annotation in current DTO); `callStatus`: `String` (no field annotation in current DTO); `initiatedAt`: `Instant` (no field annotation in current DTO); `answeredAt`: `Instant` (no field annotation in current DTO); `endedAt`: `Instant` (no field annotation in current DTO); `durationSeconds`: `Integer` (no field annotation in current DTO); `recordingFileId`: `UUID` (no field annotation in current DTO); `consentAttested`: `Boolean` (no field annotation in current DTO); `recordingStatus`: `String` (no field annotation in current DTO); `recordedDurationSeconds`: `Integer` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/ConversationCallController.java` |
| `API-06` | `PATCH /api/v1/direct-conversations/{conversationId}/calls/{callId}/end` | hasAnyRole('MOTHER', 'FAMILY', 'EXPERT') | Handler `end`; parameters: path `conversationId`: `UUID`; path `callId`: `UUID`; principal `principal`: `Principal`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<ConversationCallResponse>>`; response payload fields: `callId`: `UUID` (no field annotation in current DTO); `conversationId`: `UUID` (no field annotation in current DTO); `initiatedByUserId`: `UUID` (no field annotation in current DTO); `callType`: `String` (no field annotation in current DTO); `callStatus`: `String` (no field annotation in current DTO); `initiatedAt`: `Instant` (no field annotation in current DTO); `answeredAt`: `Instant` (no field annotation in current DTO); `endedAt`: `Instant` (no field annotation in current DTO); `durationSeconds`: `Integer` (no field annotation in current DTO); `recordingFileId`: `UUID` (no field annotation in current DTO); `consentAttested`: `Boolean` (no field annotation in current DTO); `recordingStatus`: `String` (no field annotation in current DTO); `recordedDurationSeconds`: `Integer` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/ConversationCallController.java` |
| `API-07` | `POST /api/v1/direct-conversations/{conversationId}/calls/{callId}/join-credentials` | hasAnyRole('MOTHER', 'FAMILY', 'EXPERT') | Handler `issueJoinCredentials`; parameters: path `conversationId`: `UUID`; path `callId`: `UUID`; principal `principal`: `Principal`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<ZegoJoinCredentialsResponse>>`; response payload fields: `appId`: `long` (no field annotation in current DTO); `roomId`: `String` (no field annotation in current DTO); `userId`: `String` (no field annotation in current DTO); `displayName`: `String` (no field annotation in current DTO); `token`: `String` (no field annotation in current DTO); `expiresAt`: `Instant` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/ConversationCallController.java` |
| `API-08` | `POST /api/v1/direct-conversations/{conversationId}/calls/{callId}/recording` | hasAnyRole('MOTHER', 'FAMILY', 'EXPERT') | Handler `uploadRecording`; parameters: path `conversationId`: `UUID`; path `callId`: `UUID`; context `file`: `org.springframework.web.multipart.MultipartFile`; context `recordedDurationSeconds`: `Integer`; context `consentAttested`: `Boolean`; principal `principal`: `Principal`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<ConversationCallResponse>>`; response payload fields: `callId`: `UUID` (no field annotation in current DTO); `conversationId`: `UUID` (no field annotation in current DTO); `initiatedByUserId`: `UUID` (no field annotation in current DTO); `callType`: `String` (no field annotation in current DTO); `callStatus`: `String` (no field annotation in current DTO); `initiatedAt`: `Instant` (no field annotation in current DTO); `answeredAt`: `Instant` (no field annotation in current DTO); `endedAt`: `Instant` (no field annotation in current DTO); `durationSeconds`: `Integer` (no field annotation in current DTO); `recordingFileId`: `UUID` (no field annotation in current DTO); `consentAttested`: `Boolean` (no field annotation in current DTO); `recordingStatus`: `String` (no field annotation in current DTO); `recordedDurationSeconds`: `Integer` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/ConversationCallController.java` |
| `API-09` | `PATCH /api/v1/direct-conversations/{conversationId}/calls/{callId}/ringing` | hasAnyRole('MOTHER', 'FAMILY', 'EXPERT') | Handler `markRinging`; parameters: path `conversationId`: `UUID`; path `callId`: `UUID`; principal `principal`: `Principal`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<ConversationCallResponse>>`; response payload fields: `callId`: `UUID` (no field annotation in current DTO); `conversationId`: `UUID` (no field annotation in current DTO); `initiatedByUserId`: `UUID` (no field annotation in current DTO); `callType`: `String` (no field annotation in current DTO); `callStatus`: `String` (no field annotation in current DTO); `initiatedAt`: `Instant` (no field annotation in current DTO); `answeredAt`: `Instant` (no field annotation in current DTO); `endedAt`: `Instant` (no field annotation in current DTO); `durationSeconds`: `Integer` (no field annotation in current DTO); `recordingFileId`: `UUID` (no field annotation in current DTO); `consentAttested`: `Boolean` (no field annotation in current DTO); `recordingStatus`: `String` (no field annotation in current DTO); `recordedDurationSeconds`: `Integer` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/ConversationCallController.java` |

Method-level Spring handlers, authorization annotations, request DTO fields/validators, response payload fields, and explicit/documented statuses above were extracted from the cited current source. Service/advice-only application error codes are not claimed where the controller does not declare them.

### 9.1 Handler Contract — `GET /api/v1/direct-conversations/calls/active`

| Item | Exact current contract |
| --- | --- |
| Handler | `listActiveCalls` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/ActiveConversationCallController.java` |
| Authorization annotation / boundary | hasAnyRole('MOTHER', 'FAMILY', 'EXPERT') |
| Parameters | principal `principal`: `Principal` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<List<ConversationCallResponse>>>` |
| Response payload fields | `callId`: `UUID` (no field annotation in current DTO); `conversationId`: `UUID` (no field annotation in current DTO); `initiatedByUserId`: `UUID` (no field annotation in current DTO); `callType`: `String` (no field annotation in current DTO); `callStatus`: `String` (no field annotation in current DTO); `initiatedAt`: `Instant` (no field annotation in current DTO); `answeredAt`: `Instant` (no field annotation in current DTO); `endedAt`: `Instant` (no field annotation in current DTO); `durationSeconds`: `Integer` (no field annotation in current DTO); `recordingFileId`: `UUID` (no field annotation in current DTO); `consentAttested`: `Boolean` (no field annotation in current DTO); `recordingStatus`: `String` (no field annotation in current DTO); `recordedDurationSeconds`: `Integer` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-001` / `UC-EX-11-TC-API-001` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.2 Handler Contract — `POST /api/v1/direct-conversations/{conversationId}/calls`

| Item | Exact current contract |
| --- | --- |
| Handler | `initiateCall` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/ConversationCallController.java` |
| Authorization annotation / boundary | hasAnyRole('MOTHER', 'FAMILY', 'EXPERT') |
| Parameters | path `conversationId`: `UUID`; body `request`: `InitiateCallRequest`; principal `principal`: `Principal` |
| Request body type | `InitiateCallRequest` |
| Request fields and validators | `callType`: `CallType` (@NotNull) |
| Response type | `ResponseEntity<ApiResponse<ConversationCallResponse>>` |
| Response payload fields | `callId`: `UUID` (no field annotation in current DTO); `conversationId`: `UUID` (no field annotation in current DTO); `initiatedByUserId`: `UUID` (no field annotation in current DTO); `callType`: `String` (no field annotation in current DTO); `callStatus`: `String` (no field annotation in current DTO); `initiatedAt`: `Instant` (no field annotation in current DTO); `answeredAt`: `Instant` (no field annotation in current DTO); `endedAt`: `Instant` (no field annotation in current DTO); `durationSeconds`: `Integer` (no field annotation in current DTO); `recordingFileId`: `UUID` (no field annotation in current DTO); `consentAttested`: `Boolean` (no field annotation in current DTO); `recordingStatus`: `String` (no field annotation in current DTO); `recordedDurationSeconds`: `Integer` (no field annotation in current DTO) |
| Explicit/documented statuses | `201` |
| Positive test mapping | `COND-API-002` / `UC-EX-11-TC-API-002` |
| Negative test mapping | `COND-API-002-VAL` / `UC-EX-11-TC-API-002-VAL`; plus `COND-AUTH` for protected access |

### 9.3 Handler Contract — `GET /api/v1/direct-conversations/{conversationId}/calls/{callId}`

| Item | Exact current contract |
| --- | --- |
| Handler | `getCall` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/ConversationCallController.java` |
| Authorization annotation / boundary | hasAnyRole('MOTHER', 'FAMILY', 'EXPERT') |
| Parameters | path `conversationId`: `UUID`; path `callId`: `UUID`; principal `principal`: `Principal` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<ConversationCallResponse>>` |
| Response payload fields | `callId`: `UUID` (no field annotation in current DTO); `conversationId`: `UUID` (no field annotation in current DTO); `initiatedByUserId`: `UUID` (no field annotation in current DTO); `callType`: `String` (no field annotation in current DTO); `callStatus`: `String` (no field annotation in current DTO); `initiatedAt`: `Instant` (no field annotation in current DTO); `answeredAt`: `Instant` (no field annotation in current DTO); `endedAt`: `Instant` (no field annotation in current DTO); `durationSeconds`: `Integer` (no field annotation in current DTO); `recordingFileId`: `UUID` (no field annotation in current DTO); `consentAttested`: `Boolean` (no field annotation in current DTO); `recordingStatus`: `String` (no field annotation in current DTO); `recordedDurationSeconds`: `Integer` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-003` / `UC-EX-11-TC-API-003` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.4 Handler Contract — `PATCH /api/v1/direct-conversations/{conversationId}/calls/{callId}/answer`

| Item | Exact current contract |
| --- | --- |
| Handler | `answer` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/ConversationCallController.java` |
| Authorization annotation / boundary | hasAnyRole('MOTHER', 'FAMILY', 'EXPERT') |
| Parameters | path `conversationId`: `UUID`; path `callId`: `UUID`; principal `principal`: `Principal` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<ConversationCallResponse>>` |
| Response payload fields | `callId`: `UUID` (no field annotation in current DTO); `conversationId`: `UUID` (no field annotation in current DTO); `initiatedByUserId`: `UUID` (no field annotation in current DTO); `callType`: `String` (no field annotation in current DTO); `callStatus`: `String` (no field annotation in current DTO); `initiatedAt`: `Instant` (no field annotation in current DTO); `answeredAt`: `Instant` (no field annotation in current DTO); `endedAt`: `Instant` (no field annotation in current DTO); `durationSeconds`: `Integer` (no field annotation in current DTO); `recordingFileId`: `UUID` (no field annotation in current DTO); `consentAttested`: `Boolean` (no field annotation in current DTO); `recordingStatus`: `String` (no field annotation in current DTO); `recordedDurationSeconds`: `Integer` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-004` / `UC-EX-11-TC-API-004` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.5 Handler Contract — `PATCH /api/v1/direct-conversations/{conversationId}/calls/{callId}/decline`

| Item | Exact current contract |
| --- | --- |
| Handler | `decline` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/ConversationCallController.java` |
| Authorization annotation / boundary | hasAnyRole('MOTHER', 'FAMILY', 'EXPERT') |
| Parameters | path `conversationId`: `UUID`; path `callId`: `UUID`; principal `principal`: `Principal` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<ConversationCallResponse>>` |
| Response payload fields | `callId`: `UUID` (no field annotation in current DTO); `conversationId`: `UUID` (no field annotation in current DTO); `initiatedByUserId`: `UUID` (no field annotation in current DTO); `callType`: `String` (no field annotation in current DTO); `callStatus`: `String` (no field annotation in current DTO); `initiatedAt`: `Instant` (no field annotation in current DTO); `answeredAt`: `Instant` (no field annotation in current DTO); `endedAt`: `Instant` (no field annotation in current DTO); `durationSeconds`: `Integer` (no field annotation in current DTO); `recordingFileId`: `UUID` (no field annotation in current DTO); `consentAttested`: `Boolean` (no field annotation in current DTO); `recordingStatus`: `String` (no field annotation in current DTO); `recordedDurationSeconds`: `Integer` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-005` / `UC-EX-11-TC-API-005` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.6 Handler Contract — `PATCH /api/v1/direct-conversations/{conversationId}/calls/{callId}/end`

| Item | Exact current contract |
| --- | --- |
| Handler | `end` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/ConversationCallController.java` |
| Authorization annotation / boundary | hasAnyRole('MOTHER', 'FAMILY', 'EXPERT') |
| Parameters | path `conversationId`: `UUID`; path `callId`: `UUID`; principal `principal`: `Principal` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<ConversationCallResponse>>` |
| Response payload fields | `callId`: `UUID` (no field annotation in current DTO); `conversationId`: `UUID` (no field annotation in current DTO); `initiatedByUserId`: `UUID` (no field annotation in current DTO); `callType`: `String` (no field annotation in current DTO); `callStatus`: `String` (no field annotation in current DTO); `initiatedAt`: `Instant` (no field annotation in current DTO); `answeredAt`: `Instant` (no field annotation in current DTO); `endedAt`: `Instant` (no field annotation in current DTO); `durationSeconds`: `Integer` (no field annotation in current DTO); `recordingFileId`: `UUID` (no field annotation in current DTO); `consentAttested`: `Boolean` (no field annotation in current DTO); `recordingStatus`: `String` (no field annotation in current DTO); `recordedDurationSeconds`: `Integer` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-006` / `UC-EX-11-TC-API-006` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.7 Handler Contract — `POST /api/v1/direct-conversations/{conversationId}/calls/{callId}/join-credentials`

| Item | Exact current contract |
| --- | --- |
| Handler | `issueJoinCredentials` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/ConversationCallController.java` |
| Authorization annotation / boundary | hasAnyRole('MOTHER', 'FAMILY', 'EXPERT') |
| Parameters | path `conversationId`: `UUID`; path `callId`: `UUID`; principal `principal`: `Principal` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<ZegoJoinCredentialsResponse>>` |
| Response payload fields | `appId`: `long` (no field annotation in current DTO); `roomId`: `String` (no field annotation in current DTO); `userId`: `String` (no field annotation in current DTO); `displayName`: `String` (no field annotation in current DTO); `token`: `String` (no field annotation in current DTO); `expiresAt`: `Instant` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-007` / `UC-EX-11-TC-API-007` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.8 Handler Contract — `POST /api/v1/direct-conversations/{conversationId}/calls/{callId}/recording`

| Item | Exact current contract |
| --- | --- |
| Handler | `uploadRecording` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/ConversationCallController.java` |
| Authorization annotation / boundary | hasAnyRole('MOTHER', 'FAMILY', 'EXPERT') |
| Parameters | path `conversationId`: `UUID`; path `callId`: `UUID`; context `file`: `org.springframework.web.multipart.MultipartFile`; context `recordedDurationSeconds`: `Integer`; context `consentAttested`: `Boolean`; principal `principal`: `Principal` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<ConversationCallResponse>>` |
| Response payload fields | `callId`: `UUID` (no field annotation in current DTO); `conversationId`: `UUID` (no field annotation in current DTO); `initiatedByUserId`: `UUID` (no field annotation in current DTO); `callType`: `String` (no field annotation in current DTO); `callStatus`: `String` (no field annotation in current DTO); `initiatedAt`: `Instant` (no field annotation in current DTO); `answeredAt`: `Instant` (no field annotation in current DTO); `endedAt`: `Instant` (no field annotation in current DTO); `durationSeconds`: `Integer` (no field annotation in current DTO); `recordingFileId`: `UUID` (no field annotation in current DTO); `consentAttested`: `Boolean` (no field annotation in current DTO); `recordingStatus`: `String` (no field annotation in current DTO); `recordedDurationSeconds`: `Integer` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-008` / `UC-EX-11-TC-API-008` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.9 Handler Contract — `PATCH /api/v1/direct-conversations/{conversationId}/calls/{callId}/ringing`

| Item | Exact current contract |
| --- | --- |
| Handler | `markRinging` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/ConversationCallController.java` |
| Authorization annotation / boundary | hasAnyRole('MOTHER', 'FAMILY', 'EXPERT') |
| Parameters | path `conversationId`: `UUID`; path `callId`: `UUID`; principal `principal`: `Principal` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<ConversationCallResponse>>` |
| Response payload fields | `callId`: `UUID` (no field annotation in current DTO); `conversationId`: `UUID` (no field annotation in current DTO); `initiatedByUserId`: `UUID` (no field annotation in current DTO); `callType`: `String` (no field annotation in current DTO); `callStatus`: `String` (no field annotation in current DTO); `initiatedAt`: `Instant` (no field annotation in current DTO); `answeredAt`: `Instant` (no field annotation in current DTO); `endedAt`: `Instant` (no field annotation in current DTO); `durationSeconds`: `Integer` (no field annotation in current DTO); `recordingFileId`: `UUID` (no field annotation in current DTO); `consentAttested`: `Boolean` (no field annotation in current DTO); `recordingStatus`: `String` (no field annotation in current DTO); `recordedDurationSeconds`: `Integer` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-009` / `UC-EX-11-TC-API-009` |
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
| `VG-01` | Initiate or receive a call inside an authorized conversation. | `COND-01` | `UC-EX-11-TC-001` |
| `VG-02` | Obtain provider join credentials and join with granted device permissions. | `COND-02` | `UC-EX-11-TC-002` |
| `VG-03` | End/decline and handle recording only after the implemented consent attestation. | `COND-03` | `UC-EX-11-TC-003` |
| `VG-AUTH` | Reject wrong authentication/role/ownership/membership/consent scope | `COND-AUTH` | `UC-EX-11-TC-SEC-001` |
| `VG-GAP` | Characterize each known gap without claiming a false completed path | `COND-GAP` | `UC-EX-11-TC-GAP-001` |

## 14. Verification Methods

Existing focused evidence:

- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/directchat/service/impl/ConversationCallServiceImplTest.java`
- `05_Development/CareBridgeMobileApp/test/features/directChat/calls/direct_call_coordinator_test.dart`
- `05_Development/CareBridgeMobileApp/test/features/directChat/calls/rtc_permissions_test.dart`
- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/integration/zegocloud/ZegoCloudServiceImplTest.java`

Exact supported commands derived from the audited test paths:

- `cd 05_Development/CareBridgeAPI && ./mvnw -Dtest=ConversationCallServiceImplTest test`
- `cd 05_Development/CareBridgeMobileApp && flutter test test/features/directChat/calls/direct_call_coordinator_test.dart`
- `cd 05_Development/CareBridgeMobileApp && flutter test test/features/directChat/calls/rtc_permissions_test.dart`
- `cd 05_Development/CareBridgeAPI && ./mvnw -Dtest=ZegoCloudServiceImplTest test`

Record pass/fail/skip counts only after executing these commands on the exact revision.

## 15. Verification Samples

| Sample | Value |
| --- | --- |
| Primary contract | Call lifecycle endpoints under `/api/v1/direct-conversations/{conversationId}/calls/**` |
| Request | `GET /api/v1/direct-conversations/calls/active` → `listActiveCalls`; `None` with Not applicable — no request body; authorization: hasAnyRole('MOTHER', 'FAMILY', 'EXPERT'). |
| Success response | `ResponseEntity<ApiResponse<List<ConversationCallResponse>>>` with `callId`: `UUID` (no field annotation in current DTO); `conversationId`: `UUID` (no field annotation in current DTO); `initiatedByUserId`: `UUID` (no field annotation in current DTO); `callType`: `String` (no field annotation in current DTO); `callStatus`: `String` (no field annotation in current DTO); `initiatedAt`: `Instant` (no field annotation in current DTO); `answeredAt`: `Instant` (no field annotation in current DTO); `endedAt`: `Instant` (no field annotation in current DTO); `durationSeconds`: `Integer` (no field annotation in current DTO); `recordingFileId`: `UUID` (no field annotation in current DTO); `consentAttested`: `Boolean` (no field annotation in current DTO); `recordingStatus`: `String` (no field annotation in current DTO); `recordedDurationSeconds`: `Integer` (no field annotation in current DTO); explicit/documented statuses `200`. |
| Negative sample | Use wrong role/owner/state and assert the exact mapped error without protected payload. |

## 16. Authorization Matrix

| Actor / role | Operation | Decision |
| --- | --- | --- |
| Mother / Expert | `GET /api/v1/direct-conversations/calls/active` | hasAnyRole('MOTHER', 'FAMILY', 'EXPERT'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/ActiveConversationCallController.java` |
| Mother / Expert | `POST /api/v1/direct-conversations/{conversationId}/calls` | hasAnyRole('MOTHER', 'FAMILY', 'EXPERT'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/ConversationCallController.java` |
| Mother / Expert | `GET /api/v1/direct-conversations/{conversationId}/calls/{callId}` | hasAnyRole('MOTHER', 'FAMILY', 'EXPERT'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/ConversationCallController.java` |
| Mother / Expert | `PATCH /api/v1/direct-conversations/{conversationId}/calls/{callId}/answer` | hasAnyRole('MOTHER', 'FAMILY', 'EXPERT'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/ConversationCallController.java` |
| Mother / Expert | `PATCH /api/v1/direct-conversations/{conversationId}/calls/{callId}/decline` | hasAnyRole('MOTHER', 'FAMILY', 'EXPERT'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/ConversationCallController.java` |
| Mother / Expert | `PATCH /api/v1/direct-conversations/{conversationId}/calls/{callId}/end` | hasAnyRole('MOTHER', 'FAMILY', 'EXPERT'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/ConversationCallController.java` |
| Mother / Expert | `POST /api/v1/direct-conversations/{conversationId}/calls/{callId}/join-credentials` | hasAnyRole('MOTHER', 'FAMILY', 'EXPERT'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/ConversationCallController.java` |
| Mother / Expert | `POST /api/v1/direct-conversations/{conversationId}/calls/{callId}/recording` | hasAnyRole('MOTHER', 'FAMILY', 'EXPERT'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/ConversationCallController.java` |
| Mother / Expert | `PATCH /api/v1/direct-conversations/{conversationId}/calls/{callId}/ringing` | hasAnyRole('MOTHER', 'FAMILY', 'EXPERT'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/ConversationCallController.java` |
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
