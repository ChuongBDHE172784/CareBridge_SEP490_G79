# ENGINEERING DOCUMENTATION STANDARD (EDS) v2.0

# TECHNICAL DESIGN SPECIFICATION — Exchange Direct Messages and Attachments

| Field | Value |
| --- | --- |
| Document ID | `UC-EX-10-TDS` |
| Version | `0.1` |
| Date | `2026-08-23` |
| Status | `Draft` |
| Function ID | `UC-EX-10` |
| Canonical Use Case | `UC-EX-10 — Exchange Direct Messages and Attachments` |
| Module / Bounded Context | `Expert and Consultation` |
| Primary Actor | `Mother / Expert` |
| Platforms | `Mobile / Web / Backend / Firebase / File Storage` |
| Priority | `Medium` |
| Data Classification | `Confidential professional and consultation data; Restricted identity/credential evidence and purpose-bound attachments` |
| Compliance Scope | `PDPA purpose limitation, least privilege, purpose-bound file access, consultation confidentiality, and consent-aware recording` |
| Owner | `CareBridge Team` |
| Reviewer / Approver |  |
| Source Baseline | Current worktree on `2026-08-23`; SRS `UC-EX-10`; exact evidence in Section 1.4 |

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

- **Goal:** Exchange authorized conversation messages and attachments, obtain the scoped Firebase token used by live synchronization, mark reads, and recall a message only when current policy permits.
- **Trigger:** The actor enters Mobile `/direct-chats`, `/direct-chat/:conversationId`.
- **Outcome:** Mark reads or recall an owned eligible message and refresh the timeline.
- **Current state:** `High` confidence from reachable code/test audit; no manifest-level exclusion is recorded.
- **Target state:** Preserve current code-backed behavior and resolve only explicitly evidenced limitations through approved implementation work.

### 1.2 Scope

**In scope**

- Mobile `/direct-chats`, `/direct-chat/:conversationId`
- Web `/direct-chats*`, `/expert/direct-chats*`

- Conversation/timeline/message/attachment/read/recall endpoints under `/api/v1/direct-conversations/**`
- POST `/api/v1/firebase/custom-token`

**Out of scope / limitations**

- Not applicable — no manifest-level exclusion is recorded. Scope is limited to the entry points and exact contracts listed above; unrelated handlers in the same module are excluded.

### 1.3 Preconditions and Postconditions

| Type | Condition |
| --- | --- |
| Precondition | Mother / Expert is authenticated/authorized where the current contract requires it. |
| Precondition | Required ownership, membership, consent, resource state, and device/provider prerequisites pass current policies. |
| Postcondition | Mark reads or recall an owned eligible message and refresh the timeline. |
| Postcondition | No side effect outside the feature-owned persistence/event/provider boundary occurs. |

### 1.4 Evidence Baseline

| Source ID | Type | Exact path | Revision | Authority |
| --- | --- | --- | --- | --- |
| `SRC-SRS-01` | Requirement | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-EX-10 | 2026-08-23 | Draft code-first requirement |
| `SRC-CODE-01` | Current code | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/DirectConversationController.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-02` | Current code | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/DirectMessageController.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-03` | Current code | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/integration/firebase/FirebaseTokenController.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-04` | Current code | `05_Development/CareBridgeMobileApp/lib/features/directChat/screens/direct_chat_screen.dart` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-05` | Current code | `05_Development/CareBridgeWebApp/src/features/directChat/pages/ConversationRoomPage.tsx` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-01` | Existing test | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/directchat/integration/DirectChatIntegrationTest.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-02` | Existing test | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/directchat/service/impl/DirectConversationServiceImplTest.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-03` | Existing test | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/directchat/service/impl/DirectMessageServiceImplTest.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-04` | Existing test | `05_Development/CareBridgeMobileApp/test/features/directChat/direct_chat_screen_test.dart` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-05` | Existing test | `05_Development/CareBridgeWebApp/src/features/directChat/services/directChatApi.test.ts` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-ERROR-01` | Current exception advice | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/common/exception/GlobalExceptionHandler.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-SUPP-01` | Historical architecture/design evidence | `04_Implement/MotherExpertDiscoveryInbox/MotherExpertDiscoveryInbox_Architecture-Evidence.md` | Worktree `2026-08-23` | Historical evidence only; current code and canonical SRS/TDS override conflicts |
| `SRC-SUPP-02` | Historical verification evidence | `04_Implement/MotherExpertDiscoveryInbox/MotherExpertDiscoveryInbox_Verification-Evidence.md` | Worktree `2026-08-23` | Historical evidence only; current code and canonical SRS/TDS override conflicts |

### 1.5 Open Contradictions / Questions

- No material contradiction was found among the exact sources cited in Section 1.4. Behavior not evidenced by those sources is not approved or implied by this Draft.

## 2. Traceability Matrix

| Requirement | Behavior | Exact oracle source | Component | Test condition / case |
| --- | --- | --- | --- | --- |
| `UC-EX-10-FR-01` | Open an authorized direct conversation and load its timeline. | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-EX-10 Normal Flow 1 | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/DirectConversationController.java` | `COND-01` / `UC-EX-10-TC-001` |
| `UC-EX-10-FR-02` | Send a supported message or attachment using the scoped live-sync integration. | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-EX-10 Normal Flow 2 | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/DirectConversationController.java` | `COND-02` / `UC-EX-10-TC-002` |
| `UC-EX-10-FR-03` | Mark reads or recall an owned eligible message and refresh the timeline. | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-EX-10 Normal Flow 3 | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/DirectConversationController.java` | `COND-03` / `UC-EX-10-TC-003` |
| `BR-01` | Conversation membership is server authoritative. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/DirectConversationController.java` | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/DirectConversationController.java` | `COND-BR-01` / `UC-EX-10-TC-BR-001` |
| `BR-02` | Attachment access is purpose-bound; recall follows current ownership/time/state rules. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/DirectMessageController.java` | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/DirectMessageController.java` | `COND-BR-02` / `UC-EX-10-TC-BR-002` |

## 3. Architecture Decision Records (ADR)

### ADR-UC-EX-10-01 — Use a distinct actor-goal boundary

| Item | Decision |
| --- | --- |
| Context | The retired 43-UC catalogue grouped multiple triggers, lifecycles, and permission boundaries, making implementation/test traceability generic. |
| Options | Keep the broad catalogue; split by screen; split by actor goal plus lifecycle/security boundary. |
| Decision | Use `UC-EX-10 — Exchange Direct Messages and Attachments` as the canonical boundary because its operations share the stated actor outcome and current implementation evidence. |
| Consequences | Related supporting screens/endpoints stay in one TDS; different lifecycle/actor outcomes have separate UCs. |
| Source / Status | SRS Section 3.1 and current code audit / Draft |

### ADR-UC-EX-10-02 — Preserve unknowns as Open

| Item | Decision |
| --- | --- |
| Context | Exact schema fields, SLA values, and some controller error codes are not fully evidenced by this manifest alone. |
| Decision | Do not invent them. Mark them `Open` and require the exact DTO/entity/migration/policy oracle before production-code changes. |
| Consequences | This document accurately characterizes current scope; unresolved design-changing items block implementation approval. |
| Source / Status | `create-specs` evidence discipline / Draft |

## 4. Non-Functional Requirements and SLA

| NFR | Target | Oracle Source | Verification |
| --- | --- | --- | --- |
| Authorization / isolation | All requests follow exact role/ownership/membership/consent policy in current code. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/DirectConversationController.java` | Negative security cases in paired Test-Spec |
| Protected-data handling | No secrets or unnecessary health/location/identity/conversation/file payloads in logs, fixtures, screenshots, or audit detail. | Data classification header plus exact Section 9 request/response field inventories | Log/fixture review plus security tests |
| Availability / latency | Open — no approved feature-specific numeric SLA found. | Evidence needed: approved NFR/SLA source | Measure only; do not assert a fixed threshold |
| Accessibility | Open — confirm project-standard criteria for reachable UI surfaces. | Evidence needed: approved UX/accessibility standard | Applicable Web/Mobile UI checks |
| Retry / idempotency | Apply only semantics explicitly implemented by the owning service. | No explicit lock/version/idempotency marker is evidenced in the cited implementation sources; preserve observed behavior and add characterization before changing concurrency semantics | State/duplicate/concurrency cases where applicable |

## 5. Static Modeling

### 5.1 Component Responsibilities and Change Disposition

| Exact path | Disposition | Responsibility |
| --- | --- | --- |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/DirectConversationController.java` | Reuse | Current implementation evidence for Exchange Direct Messages and Attachments; inspect the exact symbol before implementation changes. |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/DirectMessageController.java` | Reuse | Current implementation evidence for Exchange Direct Messages and Attachments; inspect the exact symbol before implementation changes. |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/integration/firebase/FirebaseTokenController.java` | Reuse | Current implementation evidence for Exchange Direct Messages and Attachments; inspect the exact symbol before implementation changes. |
| `05_Development/CareBridgeMobileApp/lib/features/directChat/screens/direct_chat_screen.dart` | Reuse | Current implementation evidence for Exchange Direct Messages and Attachments; inspect the exact symbol before implementation changes. |
| `05_Development/CareBridgeWebApp/src/features/directChat/pages/ConversationRoomPage.tsx` | Reuse | Current implementation evidence for Exchange Direct Messages and Attachments; inspect the exact symbol before implementation changes. |

### 5.2 Current Component Diagram

```plantuml
@startuml
skinparam classAttributeIconSize 0
class DirectConversationController as "DirectConversationController.java"
class DirectMessageController as "DirectMessageController.java"
DirectConversationController --> DirectMessageController
class FirebaseTokenController as "FirebaseTokenController.java"
DirectMessageController --> FirebaseTokenController
class direct_chat_screen as "direct_chat_screen.dart"
FirebaseTokenController --> direct_chat_screen
class ConversationRoomPage as "ConversationRoomPage.tsx"
direct_chat_screen --> ConversationRoomPage
@enduml
```

### 5.3 Data / Schema / Migration Assessment

| Item | Assessment |
| --- | --- |
| Current stores/entities | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/entity/AuditAction.java` |
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
Actor -> Client: Enter Exchange Direct Messages and Attachments
Client -> Domain: Open an authorized direct conversation and load its timeline.
Domain --> Client: Result for step 1
Client -> Domain: Send a supported message or attachment using the scoped live-sync integration.
Domain --> Client: Result for step 2
Client -> Domain: Mark reads or recall an owned eligible message and refresh the timeline.
Domain --> Client: Result for step 3
Client --> Actor: Render canonical outcome
@enduml
```

### 6.2 Alternative, Error, Retry, and Concurrency Flows

| Flow | Expected design behavior | Oracle |
| --- | --- | --- |
| Cancel before mutation | No unintended write or provider side effect. | SRS UC-EX-10 Alternative Flow |
| Invalid input/state | Reject with the current contract; keep canonical state unchanged. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/DirectConversationController.java` |
| Wrong actor/scope | Fail closed without protected resource disclosure. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/DirectConversationController.java` |
| Dependency failure | Use only the implemented bounded fallback/retry; never report false success. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/file/dto/UploadFileResponse.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/file/dto/ViewFileResponse.java` |
| Duplicate/concurrent mutation | Apply only current lock/version/idempotency semantics. | No explicit lock/version/idempotency marker is evidenced in the cited implementation sources; preserve observed behavior and add characterization before changing concurrency semantics |

### 6.3 State Model and Invariants

```plantuml
@startuml
[*] --> Eligible
Eligible --> InProgress : Open an authorized direct conversation and load its timeline.
InProgress --> Outcome : Mark reads or recall an owned eligible message and refresh the timeline.
InProgress --> Rejected : validation / authorization / state failure
Outcome --> [*]
Rejected --> Eligible : actor corrects eligible input
@enduml
```

- Conversation membership is server authoritative.
- Attachment access is purpose-bound; recall follows current ownership/time/state rules.

## 7. Domain Event Catalog

| Direction | Event | Producer / Consumer | Payload / delivery / idempotency |
| --- | --- | --- | --- |
| Publish / consume | Current event evidence | Not applicable at this cited baseline — no event type, publisher, or listener is evidenced in the listed implementation sources | Preserve only events evidenced by the cited source set; if later call-path inspection finds none, the flow remains synchronous. |

## 8. Interface Specification

### 8.1 User / Operator Interfaces

| # | Entry point | Actor | Contract |
| ---: | --- | --- | --- |
| 1 | Mobile `/direct-chats`, `/direct-chat/:conversationId` | Mother / Expert | Reachable current entry point |
| 2 | Web `/direct-chats*`, `/expert/direct-chats*` | Mother / Expert | Reachable current entry point |

### 8.2 Service / Repository Interfaces

| Interface evidence | Required responsibility |
| --- | --- |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/DirectConversationController.java` | Support the mapped operations without broadening authorization or lifecycle semantics. |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/DirectMessageController.java` | Support the mapped operations without broadening authorization or lifecycle semantics. |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/integration/firebase/FirebaseTokenController.java` | Support the mapped operations without broadening authorization or lifecycle semantics. |
| `05_Development/CareBridgeMobileApp/lib/features/directChat/screens/direct_chat_screen.dart` | Support the mapped operations without broadening authorization or lifecycle semantics. |
| `05_Development/CareBridgeWebApp/src/features/directChat/pages/ConversationRoomPage.tsx` | Support the mapped operations without broadening authorization or lifecycle semantics. |

## 9. API Specification

| ID | Method / path or grouped controller surface | Auth / role | Request / response / errors |
| --- | --- | --- | --- |
| `API-01` | `GET /api/v1/direct-conversations` | hasAnyRole('MOTHER', 'FAMILY', 'EXPERT') | Handler `listMyConversations`; parameters: principal `principal`: `Principal`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<List<DirectConversationSummaryResponse>>>`; response payload fields: `conversationId`: `UUID` (no field annotation in current DTO); `counterpartUserId`: `UUID` (no field annotation in current DTO); `counterpartRole`: `String` (no field annotation in current DTO); `lastActivityAt`: `Instant` (no field annotation in current DTO); `expertAvailable`: `boolean` (no field annotation in current DTO); `counterpartDisplayName`: `String` (no field annotation in current DTO); `counterpartAvatarUrl`: `String` (no field annotation in current DTO); `counterpartSpecialty`: `String` (no field annotation in current DTO); `lastMessagePreview`: `String` (no field annotation in current DTO); `lastMessageAt`: `Instant` (no field annotation in current DTO); `unreadCount`: `int` (no field annotation in current DTO); `conversationStatus`: `String` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/DirectConversationController.java` |
| `API-02` | `GET /api/v1/direct-conversations/unread-summary` | hasAnyRole('MOTHER', 'FAMILY', 'EXPERT') | Handler `getUnreadSummary`; parameters: principal `principal`: `Principal`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<UnreadSummaryResponse>>`; response payload fields: `unreadConversationCount`: `int` (no field annotation in current DTO); `totalUnreadMessageCount`: `int` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/DirectConversationController.java` |
| `API-03` | `GET /api/v1/direct-conversations/{conversationId}` | hasAnyRole('MOTHER', 'FAMILY', 'EXPERT') | Handler `getConversation`; parameters: path `conversationId`: `UUID`; principal `principal`: `Principal`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<DirectConversationResponse>>`; response payload fields: `conversationId`: `UUID` (no field annotation in current DTO); `motherUserId`: `UUID` (no field annotation in current DTO); `expertUserId`: `UUID` (no field annotation in current DTO); `status`: `String` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO); `lastActivityAt`: `Instant` (no field annotation in current DTO); `expertAvailable`: `boolean` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/DirectConversationController.java` |
| `API-04` | `POST /api/v1/direct-conversations/{conversationId}/attachments` | hasAnyRole('MOTHER', 'FAMILY', 'EXPERT') | Handler `uploadAttachment`; parameters: path `conversationId`: `UUID`; context `file`: `MultipartFile`; principal `principal`: `Principal`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<UploadFileResponse>>`; response payload fields: `fileId`: `UUID` (no field annotation in current DTO); `originalName`: `String` (no field annotation in current DTO); `mimeType`: `String` (no field annotation in current DTO); `fileSizeBytes`: `long` (no field annotation in current DTO); `presignedUrl`: `String` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/DirectMessageController.java` |
| `API-05` | `POST /api/v1/direct-conversations/{conversationId}/messages` | hasAnyRole('MOTHER', 'FAMILY', 'EXPERT') | Handler `sendMessage`; parameters: path `conversationId`: `UUID`; body `request`: `SendDirectMessageRequest`; principal `principal`: `Principal`; request body: `SendDirectMessageRequest`; request fields/validation: `clientMessageId`: `UUID` (@NotNull); `messageBody`: `String` (no field annotation in current DTO); `messageType`: `String` (no field annotation in current DTO); `attachmentId`: `UUID` (no field annotation in current DTO); `locationLatitude`: `Double` (no field annotation in current DTO); `locationLongitude`: `Double` (no field annotation in current DTO); `locationLabel`: `String` (no field annotation in current DTO); response: `ResponseEntity<ApiResponse<TimelineItemResponse>>`; response payload fields: `kind`: `String` (no field annotation in current DTO); `messageId`: `UUID` (no field annotation in current DTO); `clientMessageId`: `UUID` (no field annotation in current DTO); `senderUserId`: `UUID` (no field annotation in current DTO); `messageType`: `String` (no field annotation in current DTO); `messageBody`: `String` (no field annotation in current DTO); `attachmentId`: `UUID` (no field annotation in current DTO); `locationLatitude`: `Double` (no field annotation in current DTO); `locationLongitude`: `Double` (no field annotation in current DTO); `locationLabel`: `String` (no field annotation in current DTO); `recalledAt`: `Instant` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO); `callId`: `UUID` (no field annotation in current DTO); `callType`: `String` (no field annotation in current DTO); `callStatus`: `String` (no field annotation in current DTO); `initiatedByUserId`: `UUID` (no field annotation in current DTO); `durationSeconds`: `Integer` (no field annotation in current DTO); `initiatedAt`: `Instant` (no field annotation in current DTO); `answeredAt`: `Instant` (no field annotation in current DTO); `endedAt`: `Instant` (no field annotation in current DTO); explicit/documented statuses: `200, 201`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/DirectMessageController.java` |
| `API-06` | `GET /api/v1/direct-conversations/{conversationId}/messages/{messageId}/attachment` | hasAnyRole('MOTHER', 'FAMILY', 'EXPERT') | Handler `viewAttachment`; parameters: path `conversationId`: `UUID`; path `messageId`: `UUID`; principal `principal`: `Principal`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<ViewFileResponse>>`; response payload fields: `fileId`: `UUID` (no field annotation in current DTO); `originalName`: `String` (no field annotation in current DTO); `mimeType`: `String` (no field annotation in current DTO); `fileSizeBytes`: `long` (no field annotation in current DTO); `presignedUrl`: `String` (no field annotation in current DTO); `status`: `String` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/DirectMessageController.java` |
| `API-07` | `PATCH /api/v1/direct-conversations/{conversationId}/messages/{messageId}/recall` | hasAnyRole('MOTHER', 'FAMILY', 'EXPERT') | Handler `recallMessage`; parameters: path `conversationId`: `UUID`; path `messageId`: `UUID`; principal `principal`: `Principal`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<Void>>`; response payload fields: Not applicable or unresolved from the handler import; explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/DirectMessageController.java` |
| `API-08` | `PATCH /api/v1/direct-conversations/{conversationId}/read` | hasAnyRole('MOTHER', 'FAMILY', 'EXPERT') | Handler `markRead`; parameters: path `conversationId`: `UUID`; body `request`: `MarkReadRequest`; principal `principal`: `Principal`; request body: `MarkReadRequest`; request fields/validation: `lastSeenMessageId`: `UUID` (@NotNull); response: `ResponseEntity<ApiResponse<MarkReadResponse>>`; response payload fields: `cursorAt`: `Instant` (no field annotation in current DTO); `cursorMessageId`: `UUID` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/DirectConversationController.java` |
| `API-09` | `GET /api/v1/direct-conversations/{conversationId}/timeline` | hasAnyRole('MOTHER', 'FAMILY', 'EXPERT') | Handler `getTimeline`; parameters: path `conversationId`: `UUID`; query `after`: `String`; query `before`: `String`; query `limit`: `int`; principal `principal`: `Principal`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<TimelinePageResponse>>`; response payload fields: `items`: `List<TimelineItemResponse>` (no field annotation in current DTO); `nextCursor`: `String` (no field annotation in current DTO); `hasMoreNewer`: `boolean` (no field annotation in current DTO); `previousCursor`: `String` (no field annotation in current DTO); `hasMoreOlder`: `boolean` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/DirectMessageController.java` |
| `API-10` | `POST /api/v1/firebase/custom-token` | isAuthenticated() | Handler `issueCustomToken`; parameters: principal `principal`: `Principal`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<FirebaseCustomTokenResponse>>`; response payload fields: Not applicable or unresolved from the handler import; explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/integration/firebase/FirebaseTokenController.java` |

Method-level Spring handlers, authorization annotations, request DTO fields/validators, response payload fields, and explicit/documented statuses above were extracted from the cited current source. Service/advice-only application error codes are not claimed where the controller does not declare them.

### 9.1 Handler Contract — `GET /api/v1/direct-conversations`

| Item | Exact current contract |
| --- | --- |
| Handler | `listMyConversations` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/DirectConversationController.java` |
| Authorization annotation / boundary | hasAnyRole('MOTHER', 'FAMILY', 'EXPERT') |
| Parameters | principal `principal`: `Principal` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<List<DirectConversationSummaryResponse>>>` |
| Response payload fields | `conversationId`: `UUID` (no field annotation in current DTO); `counterpartUserId`: `UUID` (no field annotation in current DTO); `counterpartRole`: `String` (no field annotation in current DTO); `lastActivityAt`: `Instant` (no field annotation in current DTO); `expertAvailable`: `boolean` (no field annotation in current DTO); `counterpartDisplayName`: `String` (no field annotation in current DTO); `counterpartAvatarUrl`: `String` (no field annotation in current DTO); `counterpartSpecialty`: `String` (no field annotation in current DTO); `lastMessagePreview`: `String` (no field annotation in current DTO); `lastMessageAt`: `Instant` (no field annotation in current DTO); `unreadCount`: `int` (no field annotation in current DTO); `conversationStatus`: `String` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-001` / `UC-EX-10-TC-API-001` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.2 Handler Contract — `GET /api/v1/direct-conversations/unread-summary`

| Item | Exact current contract |
| --- | --- |
| Handler | `getUnreadSummary` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/DirectConversationController.java` |
| Authorization annotation / boundary | hasAnyRole('MOTHER', 'FAMILY', 'EXPERT') |
| Parameters | principal `principal`: `Principal` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<UnreadSummaryResponse>>` |
| Response payload fields | `unreadConversationCount`: `int` (no field annotation in current DTO); `totalUnreadMessageCount`: `int` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-002` / `UC-EX-10-TC-API-002` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.3 Handler Contract — `GET /api/v1/direct-conversations/{conversationId}`

| Item | Exact current contract |
| --- | --- |
| Handler | `getConversation` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/DirectConversationController.java` |
| Authorization annotation / boundary | hasAnyRole('MOTHER', 'FAMILY', 'EXPERT') |
| Parameters | path `conversationId`: `UUID`; principal `principal`: `Principal` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<DirectConversationResponse>>` |
| Response payload fields | `conversationId`: `UUID` (no field annotation in current DTO); `motherUserId`: `UUID` (no field annotation in current DTO); `expertUserId`: `UUID` (no field annotation in current DTO); `status`: `String` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO); `lastActivityAt`: `Instant` (no field annotation in current DTO); `expertAvailable`: `boolean` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-003` / `UC-EX-10-TC-API-003` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.4 Handler Contract — `POST /api/v1/direct-conversations/{conversationId}/attachments`

| Item | Exact current contract |
| --- | --- |
| Handler | `uploadAttachment` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/DirectMessageController.java` |
| Authorization annotation / boundary | hasAnyRole('MOTHER', 'FAMILY', 'EXPERT') |
| Parameters | path `conversationId`: `UUID`; context `file`: `MultipartFile`; principal `principal`: `Principal` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<UploadFileResponse>>` |
| Response payload fields | `fileId`: `UUID` (no field annotation in current DTO); `originalName`: `String` (no field annotation in current DTO); `mimeType`: `String` (no field annotation in current DTO); `fileSizeBytes`: `long` (no field annotation in current DTO); `presignedUrl`: `String` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-004` / `UC-EX-10-TC-API-004` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.5 Handler Contract — `POST /api/v1/direct-conversations/{conversationId}/messages`

| Item | Exact current contract |
| --- | --- |
| Handler | `sendMessage` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/DirectMessageController.java` |
| Authorization annotation / boundary | hasAnyRole('MOTHER', 'FAMILY', 'EXPERT') |
| Parameters | path `conversationId`: `UUID`; body `request`: `SendDirectMessageRequest`; principal `principal`: `Principal` |
| Request body type | `SendDirectMessageRequest` |
| Request fields and validators | `clientMessageId`: `UUID` (@NotNull); `messageBody`: `String` (no field annotation in current DTO); `messageType`: `String` (no field annotation in current DTO); `attachmentId`: `UUID` (no field annotation in current DTO); `locationLatitude`: `Double` (no field annotation in current DTO); `locationLongitude`: `Double` (no field annotation in current DTO); `locationLabel`: `String` (no field annotation in current DTO) |
| Response type | `ResponseEntity<ApiResponse<TimelineItemResponse>>` |
| Response payload fields | `kind`: `String` (no field annotation in current DTO); `messageId`: `UUID` (no field annotation in current DTO); `clientMessageId`: `UUID` (no field annotation in current DTO); `senderUserId`: `UUID` (no field annotation in current DTO); `messageType`: `String` (no field annotation in current DTO); `messageBody`: `String` (no field annotation in current DTO); `attachmentId`: `UUID` (no field annotation in current DTO); `locationLatitude`: `Double` (no field annotation in current DTO); `locationLongitude`: `Double` (no field annotation in current DTO); `locationLabel`: `String` (no field annotation in current DTO); `recalledAt`: `Instant` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO); `callId`: `UUID` (no field annotation in current DTO); `callType`: `String` (no field annotation in current DTO); `callStatus`: `String` (no field annotation in current DTO); `initiatedByUserId`: `UUID` (no field annotation in current DTO); `durationSeconds`: `Integer` (no field annotation in current DTO); `initiatedAt`: `Instant` (no field annotation in current DTO); `answeredAt`: `Instant` (no field annotation in current DTO); `endedAt`: `Instant` (no field annotation in current DTO) |
| Explicit/documented statuses | `200, 201` |
| Positive test mapping | `COND-API-005` / `UC-EX-10-TC-API-005` |
| Negative test mapping | `COND-API-005-VAL` / `UC-EX-10-TC-API-005-VAL`; plus `COND-AUTH` for protected access |

### 9.6 Handler Contract — `GET /api/v1/direct-conversations/{conversationId}/messages/{messageId}/attachment`

| Item | Exact current contract |
| --- | --- |
| Handler | `viewAttachment` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/DirectMessageController.java` |
| Authorization annotation / boundary | hasAnyRole('MOTHER', 'FAMILY', 'EXPERT') |
| Parameters | path `conversationId`: `UUID`; path `messageId`: `UUID`; principal `principal`: `Principal` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<ViewFileResponse>>` |
| Response payload fields | `fileId`: `UUID` (no field annotation in current DTO); `originalName`: `String` (no field annotation in current DTO); `mimeType`: `String` (no field annotation in current DTO); `fileSizeBytes`: `long` (no field annotation in current DTO); `presignedUrl`: `String` (no field annotation in current DTO); `status`: `String` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-006` / `UC-EX-10-TC-API-006` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.7 Handler Contract — `PATCH /api/v1/direct-conversations/{conversationId}/messages/{messageId}/recall`

| Item | Exact current contract |
| --- | --- |
| Handler | `recallMessage` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/DirectMessageController.java` |
| Authorization annotation / boundary | hasAnyRole('MOTHER', 'FAMILY', 'EXPERT') |
| Parameters | path `conversationId`: `UUID`; path `messageId`: `UUID`; principal `principal`: `Principal` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<Void>>` |
| Response payload fields | Not applicable or unresolved from the handler import |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-007` / `UC-EX-10-TC-API-007` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.8 Handler Contract — `PATCH /api/v1/direct-conversations/{conversationId}/read`

| Item | Exact current contract |
| --- | --- |
| Handler | `markRead` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/DirectConversationController.java` |
| Authorization annotation / boundary | hasAnyRole('MOTHER', 'FAMILY', 'EXPERT') |
| Parameters | path `conversationId`: `UUID`; body `request`: `MarkReadRequest`; principal `principal`: `Principal` |
| Request body type | `MarkReadRequest` |
| Request fields and validators | `lastSeenMessageId`: `UUID` (@NotNull) |
| Response type | `ResponseEntity<ApiResponse<MarkReadResponse>>` |
| Response payload fields | `cursorAt`: `Instant` (no field annotation in current DTO); `cursorMessageId`: `UUID` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-008` / `UC-EX-10-TC-API-008` |
| Negative test mapping | `COND-API-008-VAL` / `UC-EX-10-TC-API-008-VAL`; plus `COND-AUTH` for protected access |

### 9.9 Handler Contract — `GET /api/v1/direct-conversations/{conversationId}/timeline`

| Item | Exact current contract |
| --- | --- |
| Handler | `getTimeline` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/DirectMessageController.java` |
| Authorization annotation / boundary | hasAnyRole('MOTHER', 'FAMILY', 'EXPERT') |
| Parameters | path `conversationId`: `UUID`; query `after`: `String`; query `before`: `String`; query `limit`: `int`; principal `principal`: `Principal` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<TimelinePageResponse>>` |
| Response payload fields | `items`: `List<TimelineItemResponse>` (no field annotation in current DTO); `nextCursor`: `String` (no field annotation in current DTO); `hasMoreNewer`: `boolean` (no field annotation in current DTO); `previousCursor`: `String` (no field annotation in current DTO); `hasMoreOlder`: `boolean` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-009` / `UC-EX-10-TC-API-009` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.10 Handler Contract — `POST /api/v1/firebase/custom-token`

| Item | Exact current contract |
| --- | --- |
| Handler | `issueCustomToken` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/integration/firebase/FirebaseTokenController.java` |
| Authorization annotation / boundary | isAuthenticated() |
| Parameters | principal `principal`: `Principal` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<FirebaseCustomTokenResponse>>` |
| Response payload fields | Not applicable or unresolved from the handler import |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-010` / `UC-EX-10-TC-API-010` |
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
| `VG-01` | Open an authorized direct conversation and load its timeline. | `COND-01` | `UC-EX-10-TC-001` |
| `VG-02` | Send a supported message or attachment using the scoped live-sync integration. | `COND-02` | `UC-EX-10-TC-002` |
| `VG-03` | Mark reads or recall an owned eligible message and refresh the timeline. | `COND-03` | `UC-EX-10-TC-003` |
| `VG-AUTH` | Reject wrong authentication/role/ownership/membership/consent scope | `COND-AUTH` | `UC-EX-10-TC-SEC-001` |
| `VG-GAP` | Characterize each known gap without claiming a false completed path | `COND-GAP` | `UC-EX-10-TC-GAP-001` |

## 14. Verification Methods

Existing focused evidence:

- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/directchat/integration/DirectChatIntegrationTest.java`
- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/directchat/service/impl/DirectConversationServiceImplTest.java`
- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/directchat/service/impl/DirectMessageServiceImplTest.java`
- `05_Development/CareBridgeMobileApp/test/features/directChat/direct_chat_screen_test.dart`
- `05_Development/CareBridgeWebApp/src/features/directChat/services/directChatApi.test.ts`

Exact supported commands derived from the audited test paths:

- `cd 05_Development/CareBridgeAPI && ./mvnw -Dtest=DirectChatIntegrationTest test`
- `cd 05_Development/CareBridgeAPI && ./mvnw -Dtest=DirectConversationServiceImplTest test`
- `cd 05_Development/CareBridgeAPI && ./mvnw -Dtest=DirectMessageServiceImplTest test`
- `cd 05_Development/CareBridgeMobileApp && flutter test test/features/directChat/direct_chat_screen_test.dart`
- `cd 05_Development/CareBridgeWebApp && npm test -- src/features/directChat/services/directChatApi.test.ts`

Record pass/fail/skip counts only after executing these commands on the exact revision.

## 15. Verification Samples

| Sample | Value |
| --- | --- |
| Primary contract | Conversation/timeline/message/attachment/read/recall endpoints under `/api/v1/direct-conversations/**` |
| Request | `GET /api/v1/direct-conversations` → `listMyConversations`; `None` with Not applicable — no request body; authorization: hasAnyRole('MOTHER', 'FAMILY', 'EXPERT'). |
| Success response | `ResponseEntity<ApiResponse<List<DirectConversationSummaryResponse>>>` with `conversationId`: `UUID` (no field annotation in current DTO); `counterpartUserId`: `UUID` (no field annotation in current DTO); `counterpartRole`: `String` (no field annotation in current DTO); `lastActivityAt`: `Instant` (no field annotation in current DTO); `expertAvailable`: `boolean` (no field annotation in current DTO); `counterpartDisplayName`: `String` (no field annotation in current DTO); `counterpartAvatarUrl`: `String` (no field annotation in current DTO); `counterpartSpecialty`: `String` (no field annotation in current DTO); `lastMessagePreview`: `String` (no field annotation in current DTO); `lastMessageAt`: `Instant` (no field annotation in current DTO); `unreadCount`: `int` (no field annotation in current DTO); `conversationStatus`: `String` (no field annotation in current DTO); explicit/documented statuses `200`. |
| Negative sample | Use wrong role/owner/state and assert the exact mapped error without protected payload. |

## 16. Authorization Matrix

| Actor / role | Operation | Decision |
| --- | --- | --- |
| Mother / Expert | `GET /api/v1/direct-conversations` | hasAnyRole('MOTHER', 'FAMILY', 'EXPERT'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/DirectConversationController.java` |
| Mother / Expert | `GET /api/v1/direct-conversations/unread-summary` | hasAnyRole('MOTHER', 'FAMILY', 'EXPERT'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/DirectConversationController.java` |
| Mother / Expert | `GET /api/v1/direct-conversations/{conversationId}` | hasAnyRole('MOTHER', 'FAMILY', 'EXPERT'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/DirectConversationController.java` |
| Mother / Expert | `POST /api/v1/direct-conversations/{conversationId}/attachments` | hasAnyRole('MOTHER', 'FAMILY', 'EXPERT'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/DirectMessageController.java` |
| Mother / Expert | `POST /api/v1/direct-conversations/{conversationId}/messages` | hasAnyRole('MOTHER', 'FAMILY', 'EXPERT'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/DirectMessageController.java` |
| Mother / Expert | `GET /api/v1/direct-conversations/{conversationId}/messages/{messageId}/attachment` | hasAnyRole('MOTHER', 'FAMILY', 'EXPERT'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/DirectMessageController.java` |
| Mother / Expert | `PATCH /api/v1/direct-conversations/{conversationId}/messages/{messageId}/recall` | hasAnyRole('MOTHER', 'FAMILY', 'EXPERT'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/DirectMessageController.java` |
| Mother / Expert | `PATCH /api/v1/direct-conversations/{conversationId}/read` | hasAnyRole('MOTHER', 'FAMILY', 'EXPERT'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/DirectConversationController.java` |
| Mother / Expert | `GET /api/v1/direct-conversations/{conversationId}/timeline` | hasAnyRole('MOTHER', 'FAMILY', 'EXPERT'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/DirectMessageController.java` |
| Mother / Expert | `POST /api/v1/firebase/custom-token` | isAuthenticated(); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/integration/firebase/FirebaseTokenController.java` |
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
