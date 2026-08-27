# ENGINEERING DOCUMENTATION STANDARD (EDS) v2.0

# TECHNICAL DESIGN SPECIFICATION — Browse and Search Community Q&A

| Field | Value |
| --- | --- |
| Document ID | `UC-CO-01-TDS` |
| Version | `0.1` |
| Date | `2026-08-23` |
| Status | `Draft` |
| Function ID | `UC-CO-01` |
| Canonical Use Case | `UC-CO-01 — Browse and Search Community Q&A` |
| Module / Bounded Context | `Community and Content Consumption` |
| Primary Actor | `Authenticated User` |
| Platforms | `Mobile / Backend` |
| Priority | `Medium` |
| Data Classification | `Internal community/content engagement data; Confidential reporter identity and moderation-bound submissions` |
| Compliance Scope | `PDPA account minimization, moderation visibility, reporter confidentiality, and attachment access/cleanup controls` |
| Owner | `CareBridge Team` |
| Reviewer / Approver |  |
| Source Baseline | Current worktree on `2026-08-23`; SRS `UC-CO-01`; exact evidence in Section 1.4 |

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

- **Goal:** Browse the moderated community feed, search eligible questions, inspect a question, and browse the topic directory.
- **Trigger:** The actor enters Mobile Community Feed and Question Detail screens.
- **Outcome:** Open a visible question detail and its allowed actions.
- **Current state:** `High` confidence from reachable code/test audit; no manifest-level exclusion is recorded.
- **Target state:** Preserve current code-backed behavior and resolve only explicitly evidenced limitations through approved implementation work.

### 1.2 Scope

**In scope**

- Mobile Community Feed and Question Detail screens

- GET `/api/v1/community/feed`
- GET `/api/v1/community/questions`
- GET `/api/v1/community/questions/{id}`
- GET `/api/v1/community/topics`

**Out of scope / limitations**

- Not applicable — no manifest-level exclusion is recorded. Scope is limited to the entry points and exact contracts listed above; unrelated handlers in the same module are excluded.

### 1.3 Preconditions and Postconditions

| Type | Condition |
| --- | --- |
| Precondition | Authenticated User is authenticated/authorized where the current contract requires it. |
| Precondition | Required ownership, membership, consent, resource state, and device/provider prerequisites pass current policies. |
| Postcondition | Open a visible question detail and its allowed actions. |
| Postcondition | No side effect outside the feature-owned persistence/event/provider boundary occurs. |

### 1.4 Evidence Baseline

| Source ID | Type | Exact path | Revision | Authority |
| --- | --- | --- | --- | --- |
| `SRC-SRS-01` | Requirement | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-CO-01 | 2026-08-23 | Draft code-first requirement |
| `SRC-CODE-01` | Current code | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/controller/CommunityFeedController.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-02` | Current code | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/controller/CommunityQuestionController.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-03` | Current code | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/controller/CommunityTopicController.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-04` | Current code | `05_Development/CareBridgeMobileApp/lib/features/community/screens/community_feed_screen.dart` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-01` | Existing test | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/community/controller/CommunityFeedControllerTest.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-02` | Existing test | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/community/service/CommunityFeedServiceImplTest.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-03` | Existing test | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/community/service/CommunityQuestionSearchServiceImplTest.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-04` | Existing test | `05_Development/CareBridgeMobileApp/test/features/community/community_feed_screen_test.dart` | Worktree `2026-08-23` | Current-state evidence |

### 1.5 Open Contradictions / Questions

- No material contradiction was found among the exact sources cited in Section 1.4. Behavior not evidenced by those sources is not approved or implied by this Draft.

## 2. Traceability Matrix

| Requirement | Behavior | Exact oracle source | Component | Test condition / case |
| --- | --- | --- | --- | --- |
| `UC-CO-01-FR-01` | Load the eligible moderated feed or topic directory. | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-CO-01 Normal Flow 1 | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/controller/CommunityFeedController.java` | `COND-01` / `UC-CO-01-TC-001` |
| `UC-CO-01-FR-02` | Search/filter community questions. | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-CO-01 Normal Flow 2 | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/controller/CommunityQuestionController.java` | `COND-02` / `UC-CO-01-TC-002` |
| `UC-CO-01-FR-03` | Open a visible question detail and its allowed actions. | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-CO-01 Normal Flow 3 | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/controller/CommunityQuestionController.java` | `COND-03` / `UC-CO-01-TC-003` |
| `BR-01` | Publication/moderation state controls visibility. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/controller/CommunityFeedController.java` | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/controller/CommunityFeedController.java` | `COND-BR-01` / `UC-CO-01-TC-BR-001` |
| `BR-02` | Community Q&A is distinct from verified health-content lifecycle. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/controller/CommunityQuestionController.java` | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/controller/CommunityQuestionController.java` | `COND-BR-02` / `UC-CO-01-TC-BR-002` |

## 3. Architecture Decision Records (ADR)

### ADR-UC-CO-01-01 — Use a distinct actor-goal boundary

| Item | Decision |
| --- | --- |
| Context | The retired 43-UC catalogue grouped multiple triggers, lifecycles, and permission boundaries, making implementation/test traceability generic. |
| Options | Keep the broad catalogue; split by screen; split by actor goal plus lifecycle/security boundary. |
| Decision | Use `UC-CO-01 — Browse and Search Community Q&A` as the canonical boundary because its operations share the stated actor outcome and current implementation evidence. |
| Consequences | Related supporting screens/endpoints stay in one TDS; different lifecycle/actor outcomes have separate UCs. |
| Source / Status | SRS Section 3.1 and current code audit / Draft |

### ADR-UC-CO-01-02 — Preserve unknowns as Open

| Item | Decision |
| --- | --- |
| Context | Exact schema fields, SLA values, and some controller error codes are not fully evidenced by this manifest alone. |
| Decision | Do not invent them. Mark them `Open` and require the exact DTO/entity/migration/policy oracle before production-code changes. |
| Consequences | This document accurately characterizes current scope; unresolved design-changing items block implementation approval. |
| Source / Status | `create-specs` evidence discipline / Draft |

## 4. Non-Functional Requirements and SLA

| NFR | Target | Oracle Source | Verification |
| --- | --- | --- | --- |
| Authorization / isolation | All requests follow exact role/ownership/membership/consent policy in current code. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/controller/CommunityFeedController.java` | Negative security cases in paired Test-Spec |
| Protected-data handling | No secrets or unnecessary health/location/identity/conversation/file payloads in logs, fixtures, screenshots, or audit detail. | Data classification header plus exact Section 9 request/response field inventories | Log/fixture review plus security tests |
| Availability / latency | Open — no approved feature-specific numeric SLA found. | Evidence needed: approved NFR/SLA source | Measure only; do not assert a fixed threshold |
| Accessibility | Open — confirm project-standard criteria for reachable UI surfaces. | Evidence needed: approved UX/accessibility standard | Applicable Web/Mobile UI checks |
| Retry / idempotency | Apply only semantics explicitly implemented by the owning service. | No explicit lock/version/idempotency marker is evidenced in the cited implementation sources; preserve observed behavior and add characterization before changing concurrency semantics | State/duplicate/concurrency cases where applicable |

## 5. Static Modeling

### 5.1 Component Responsibilities and Change Disposition

| Exact path | Disposition | Responsibility |
| --- | --- | --- |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/controller/CommunityFeedController.java` | Reuse | Current implementation evidence for Browse and Search Community Q&A; inspect the exact symbol before implementation changes. |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/controller/CommunityQuestionController.java` | Reuse | Current implementation evidence for Browse and Search Community Q&A; inspect the exact symbol before implementation changes. |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/controller/CommunityTopicController.java` | Reuse | Current implementation evidence for Browse and Search Community Q&A; inspect the exact symbol before implementation changes. |
| `05_Development/CareBridgeMobileApp/lib/features/community/screens/community_feed_screen.dart` | Reuse | Current implementation evidence for Browse and Search Community Q&A; inspect the exact symbol before implementation changes. |

### 5.2 Current Component Diagram

```plantuml
@startuml
skinparam classAttributeIconSize 0
class CommunityFeedController as "CommunityFeedController.java"
class CommunityQuestionController as "CommunityQuestionController.java"
CommunityFeedController --> CommunityQuestionController
class CommunityTopicController as "CommunityTopicController.java"
CommunityQuestionController --> CommunityTopicController
class community_feed_screen as "community_feed_screen.dart"
CommunityTopicController --> community_feed_screen
@enduml
```

### 5.3 Data / Schema / Migration Assessment

| Item | Assessment |
| --- | --- |
| Current stores/entities | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/entity/PregnancyStage.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/entity/TopicType.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/entity/UrgencyLevel.java` |
| Sensitive fields | Internal community/content engagement data; Confidential reporter identity and moderation-bound submissions. Exact transport fields and validators are enumerated per handler in Section 9; entity-only fields require the cited service/entity source before a schema change. |
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
Actor -> Client: Enter Browse and Search Community Q&A
Client -> Domain: Load the eligible moderated feed or topic directory.
Domain --> Client: Result for step 1
Client -> Domain: Search/filter community questions.
Domain --> Client: Result for step 2
Client -> Domain: Open a visible question detail and its allowed actions.
Domain --> Client: Result for step 3
Client --> Actor: Render canonical outcome
@enduml
```

### 6.2 Alternative, Error, Retry, and Concurrency Flows

| Flow | Expected design behavior | Oracle |
| --- | --- | --- |
| Cancel before mutation | No unintended write or provider side effect. | SRS UC-CO-01 Alternative Flow |
| Invalid input/state | Reject with the current contract; keep canonical state unchanged. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/controller/CommunityFeedController.java` |
| Wrong actor/scope | Fail closed without protected resource disclosure. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/controller/CommunityFeedController.java` |
| Dependency failure | Use only the implemented bounded fallback/retry; never report false success. | Not applicable at this cited baseline — no external adapter/provider import is evidenced in the listed implementation sources |
| Duplicate/concurrent mutation | Apply only current lock/version/idempotency semantics. | No explicit lock/version/idempotency marker is evidenced in the cited implementation sources; preserve observed behavior and add characterization before changing concurrency semantics |

### 6.3 State Model and Invariants

```plantuml
@startuml
[*] --> Eligible
Eligible --> InProgress : Load the eligible moderated feed or topic directory.
InProgress --> Outcome : Open a visible question detail and its allowed actions.
InProgress --> Rejected : validation / authorization / state failure
Outcome --> [*]
Rejected --> Eligible : actor corrects eligible input
@enduml
```

- Publication/moderation state controls visibility.
- Community Q&A is distinct from verified health-content lifecycle.

## 7. Domain Event Catalog

| Direction | Event | Producer / Consumer | Payload / delivery / idempotency |
| --- | --- | --- | --- |
| Publish / consume | Current event evidence | Not applicable at this cited baseline — no event type, publisher, or listener is evidenced in the listed implementation sources | Preserve only events evidenced by the cited source set; if later call-path inspection finds none, the flow remains synchronous. |

## 8. Interface Specification

### 8.1 User / Operator Interfaces

| # | Entry point | Actor | Contract |
| ---: | --- | --- | --- |
| 1 | Mobile Community Feed and Question Detail screens | Authenticated User | Reachable current entry point |

### 8.2 Service / Repository Interfaces

| Interface evidence | Required responsibility |
| --- | --- |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/controller/CommunityFeedController.java` | Support the mapped operations without broadening authorization or lifecycle semantics. |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/controller/CommunityQuestionController.java` | Support the mapped operations without broadening authorization or lifecycle semantics. |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/controller/CommunityTopicController.java` | Support the mapped operations without broadening authorization or lifecycle semantics. |
| `05_Development/CareBridgeMobileApp/lib/features/community/screens/community_feed_screen.dart` | Support the mapped operations without broadening authorization or lifecycle semantics. |

## 9. API Specification

| ID | Method / path or grouped controller surface | Auth / role | Request / response / errors |
| --- | --- | --- | --- |
| `API-01` | `GET /api/v1/community/feed` | No @PreAuthorize on handler/class; effective access comes from the security chain | Handler `getFeed`; parameters: query `topicId`: `UUID`; query `page`: `int`; query `size`: `int`; principal `principal`: `Principal`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<PaginatedResponse<CommunityFeedItemResponse>>`; response payload fields: Not applicable or unresolved from the handler import; explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/controller/CommunityFeedController.java` |
| `API-02` | `GET /api/v1/community/questions` | No @PreAuthorize on handler/class; effective access comes from the security chain | Handler `searchQuestions`; parameters: query `keyword`: `String`; query `topicId`: `UUID`; query `stage`: `PregnancyStage`; query `urgency`: `UrgencyLevel`; query `hasExpertAnswer`: `Boolean`; query `page`: `int`; query `size`: `int`; principal `principal`: `Principal`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<PaginatedResponse<CommunityQuestionSummaryResponse>>`; response payload fields: Not applicable or unresolved from the handler import; explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/controller/CommunityQuestionController.java` |
| `API-03` | `GET /api/v1/community/questions/{id}` | isAuthenticated() | Handler `getQuestionDetail`; parameters: path `id`: `UUID`; principal `principal`: `Principal`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<CommunityQuestionDetailResponse>>`; response payload fields: `id`: `UUID` (no field annotation in current DTO); `topicId`: `UUID` (no field annotation in current DTO); `topicName`: `String` (no field annotation in current DTO); `title`: `String` (no field annotation in current DTO); `body`: `String` (no field annotation in current DTO); `imageUrls`: `List<String>` (no field annotation in current DTO); `stage`: `String` (no field annotation in current DTO); `pregnancyWeek`: `Short` (no field annotation in current DTO); `babyAgeMonths`: `Short` (no field annotation in current DTO); `urgency`: `String` (no field annotation in current DTO); `anonymous`: `boolean` (no field annotation in current DTO); `authorId`: `UUID` (no field annotation in current DTO); `authorDisplay`: `String` (no field annotation in current DTO); `status`: `String` (no field annotation in current DTO); `answerCount`: `int` (no field annotation in current DTO); `likeCount`: `int` (no field annotation in current DTO); `isBookmarked`: `boolean` (no field annotation in current DTO); `isLiked`: `boolean` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO); `updatedAt`: `Instant` (no field annotation in current DTO); `answers`: `List<CommunityAnswerResponse>` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/controller/CommunityQuestionController.java` |
| `API-04` | `GET /api/v1/community/topics` | No @PreAuthorize on handler/class; effective access comes from the security chain | Handler `getTopics`; parameters: query `keyword`: `String`; query `includeHidden`: `boolean`; query `type`: `TopicType`; context `authentication`: `Authentication`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<List<CommunityTopicResponse>>>`; response payload fields: `id`: `UUID` (no field annotation in current DTO); `name`: `String` (no field annotation in current DTO); `description`: `String` (no field annotation in current DTO); `icon`: `String` (no field annotation in current DTO); `type`: `TopicType` (no field annotation in current DTO); `slug`: `String` (no field annotation in current DTO); `parentId`: `UUID` (no field annotation in current DTO); `questionCount`: `long` (no field annotation in current DTO); `isHidden`: `boolean` (no field annotation in current DTO); `isFollowed`: `boolean` (no field annotation in current DTO); `sortOrder`: `int` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO); `updatedAt`: `Instant` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/controller/CommunityTopicController.java` |

Method-level Spring handlers, authorization annotations, request DTO fields/validators, response payload fields, and explicit/documented statuses above were extracted from the cited current source. Service/advice-only application error codes are not claimed where the controller does not declare them.

### 9.1 Handler Contract — `GET /api/v1/community/feed`

| Item | Exact current contract |
| --- | --- |
| Handler | `getFeed` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/controller/CommunityFeedController.java` |
| Authorization annotation / boundary | No @PreAuthorize on handler/class; effective access comes from the security chain |
| Parameters | query `topicId`: `UUID`; query `page`: `int`; query `size`: `int`; principal `principal`: `Principal` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<PaginatedResponse<CommunityFeedItemResponse>>` |
| Response payload fields | Not applicable or unresolved from the handler import |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-001` / `UC-CO-01-TC-API-001` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.2 Handler Contract — `GET /api/v1/community/questions`

| Item | Exact current contract |
| --- | --- |
| Handler | `searchQuestions` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/controller/CommunityQuestionController.java` |
| Authorization annotation / boundary | No @PreAuthorize on handler/class; effective access comes from the security chain |
| Parameters | query `keyword`: `String`; query `topicId`: `UUID`; query `stage`: `PregnancyStage`; query `urgency`: `UrgencyLevel`; query `hasExpertAnswer`: `Boolean`; query `page`: `int`; query `size`: `int`; principal `principal`: `Principal` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<PaginatedResponse<CommunityQuestionSummaryResponse>>` |
| Response payload fields | Not applicable or unresolved from the handler import |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-002` / `UC-CO-01-TC-API-002` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.3 Handler Contract — `GET /api/v1/community/questions/{id}`

| Item | Exact current contract |
| --- | --- |
| Handler | `getQuestionDetail` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/controller/CommunityQuestionController.java` |
| Authorization annotation / boundary | isAuthenticated() |
| Parameters | path `id`: `UUID`; principal `principal`: `Principal` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<CommunityQuestionDetailResponse>>` |
| Response payload fields | `id`: `UUID` (no field annotation in current DTO); `topicId`: `UUID` (no field annotation in current DTO); `topicName`: `String` (no field annotation in current DTO); `title`: `String` (no field annotation in current DTO); `body`: `String` (no field annotation in current DTO); `imageUrls`: `List<String>` (no field annotation in current DTO); `stage`: `String` (no field annotation in current DTO); `pregnancyWeek`: `Short` (no field annotation in current DTO); `babyAgeMonths`: `Short` (no field annotation in current DTO); `urgency`: `String` (no field annotation in current DTO); `anonymous`: `boolean` (no field annotation in current DTO); `authorId`: `UUID` (no field annotation in current DTO); `authorDisplay`: `String` (no field annotation in current DTO); `status`: `String` (no field annotation in current DTO); `answerCount`: `int` (no field annotation in current DTO); `likeCount`: `int` (no field annotation in current DTO); `isBookmarked`: `boolean` (no field annotation in current DTO); `isLiked`: `boolean` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO); `updatedAt`: `Instant` (no field annotation in current DTO); `answers`: `List<CommunityAnswerResponse>` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-003` / `UC-CO-01-TC-API-003` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.4 Handler Contract — `GET /api/v1/community/topics`

| Item | Exact current contract |
| --- | --- |
| Handler | `getTopics` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/controller/CommunityTopicController.java` |
| Authorization annotation / boundary | No @PreAuthorize on handler/class; effective access comes from the security chain |
| Parameters | query `keyword`: `String`; query `includeHidden`: `boolean`; query `type`: `TopicType`; context `authentication`: `Authentication` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<List<CommunityTopicResponse>>>` |
| Response payload fields | `id`: `UUID` (no field annotation in current DTO); `name`: `String` (no field annotation in current DTO); `description`: `String` (no field annotation in current DTO); `icon`: `String` (no field annotation in current DTO); `type`: `TopicType` (no field annotation in current DTO); `slug`: `String` (no field annotation in current DTO); `parentId`: `UUID` (no field annotation in current DTO); `questionCount`: `long` (no field annotation in current DTO); `isHidden`: `boolean` (no field annotation in current DTO); `isFollowed`: `boolean` (no field annotation in current DTO); `sortOrder`: `int` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO); `updatedAt`: `Instant` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-004` / `UC-CO-01-TC-API-004` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

## 10. Error Codes

| Error class | HTTP / code | Trigger | Client behavior | Oracle |
| --- | --- | --- | --- | --- |
| Validation/business rejection | No 4xx declared by selected handler syntax; framework/service/advice mapping applies | Invalid field/range/state/ownership input | No write or false success; show only current mapped error | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/controller/CommunityFeedController.java` plus exact exception advice/service characterization |
| Authentication/authorization | `401/403` only where the security chain or handler policy maps them | Missing credential or disallowed role/scope | Fail closed with no protected response fields | Security configuration and `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/controller/CommunityFeedController.java` |
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
| `VG-01` | Load the eligible moderated feed or topic directory. | `COND-01` | `UC-CO-01-TC-001` |
| `VG-02` | Search/filter community questions. | `COND-02` | `UC-CO-01-TC-002` |
| `VG-03` | Open a visible question detail and its allowed actions. | `COND-03` | `UC-CO-01-TC-003` |
| `VG-AUTH` | Reject wrong authentication/role/ownership/membership/consent scope | `COND-AUTH` | `UC-CO-01-TC-SEC-001` |
| `VG-GAP` | Characterize each known gap without claiming a false completed path | `COND-GAP` | `UC-CO-01-TC-GAP-001` |

## 14. Verification Methods

Existing focused evidence:

- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/community/controller/CommunityFeedControllerTest.java`
- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/community/service/CommunityFeedServiceImplTest.java`
- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/community/service/CommunityQuestionSearchServiceImplTest.java`
- `05_Development/CareBridgeMobileApp/test/features/community/community_feed_screen_test.dart`

Exact supported commands derived from the audited test paths:

- `cd 05_Development/CareBridgeAPI && ./mvnw -Dtest=CommunityFeedControllerTest test`
- `cd 05_Development/CareBridgeAPI && ./mvnw -Dtest=CommunityFeedServiceImplTest test`
- `cd 05_Development/CareBridgeAPI && ./mvnw -Dtest=CommunityQuestionSearchServiceImplTest test`
- `cd 05_Development/CareBridgeMobileApp && flutter test test/features/community/community_feed_screen_test.dart`

Record pass/fail/skip counts only after executing these commands on the exact revision.

## 15. Verification Samples

| Sample | Value |
| --- | --- |
| Primary contract | GET `/api/v1/community/feed` |
| Request | `GET /api/v1/community/feed` → `getFeed`; `None` with Not applicable — no request body; authorization: No @PreAuthorize on handler/class; effective access comes from the security chain. |
| Success response | `ResponseEntity<PaginatedResponse<CommunityFeedItemResponse>>` with Not applicable or unresolved from the handler import; explicit/documented statuses `200`. |
| Negative sample | Use wrong role/owner/state and assert the exact mapped error without protected payload. |

## 16. Authorization Matrix

| Actor / role | Operation | Decision |
| --- | --- | --- |
| Authenticated User | `GET /api/v1/community/feed` | No @PreAuthorize on handler/class; effective access comes from the security chain; handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/controller/CommunityFeedController.java` |
| Authenticated User | `GET /api/v1/community/questions` | No @PreAuthorize on handler/class; effective access comes from the security chain; handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/controller/CommunityQuestionController.java` |
| Authenticated User | `GET /api/v1/community/questions/{id}` | isAuthenticated(); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/controller/CommunityQuestionController.java` |
| Authenticated User | `GET /api/v1/community/topics` | No @PreAuthorize on handler/class; effective access comes from the security chain; handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/controller/CommunityTopicController.java` |
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
