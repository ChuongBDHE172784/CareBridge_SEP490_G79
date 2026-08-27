# ENGINEERING DOCUMENTATION STANDARD (EDS) v2.0

# TECHNICAL DESIGN SPECIFICATION — Like, Bookmark, and Follow Community Content

| Field | Value |
| --- | --- |
| Document ID | `UC-CO-04-TDS` |
| Version | `0.1` |
| Date | `2026-08-23` |
| Status | `Draft` |
| Function ID | `UC-CO-04` |
| Canonical Use Case | `UC-CO-04 — Like, Bookmark, and Follow Community Content` |
| Module / Bounded Context | `Community and Content Consumption` |
| Primary Actor | `Authenticated User` |
| Platforms | `Mobile / Backend` |
| Priority | `Medium` |
| Data Classification | `Internal community/content engagement data; Confidential reporter identity and moderation-bound submissions` |
| Compliance Scope | `PDPA account minimization, moderation visibility, reporter confidentiality, and attachment access/cleanup controls` |
| Owner | `CareBridge Team` |
| Reviewer / Approver |  |
| Source Baseline | Current worktree on `2026-08-23`; SRS `UC-CO-04`; exact evidence in Section 1.4 |

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

- **Goal:** Toggle question/answer likes, bookmark eligible questions, and follow topics for the authenticated user.
- **Trigger:** The actor enters Question/answer detail.
- **Outcome:** Reload aggregate/owned engagement state.
- **Current state:** `High` confidence from reachable code/test audit; no manifest-level exclusion is recorded.
- **Target state:** Preserve current code-backed behavior and resolve only explicitly evidenced limitations through approved implementation work.

### 1.2 Scope

**In scope**

- Question/answer detail
- Mobile Bookmarked Questions screen
- Topic directory

- POST `/api/v1/community/questions/{questionId}/like`
- POST `/api/v1/community/answers/{answerId}/like`
- POST `/api/v1/community/questions/{questionId}/bookmark`
- GET `/api/v1/community/me/bookmarks`
- POST `/api/v1/community/topics/{id}/follow`

**Out of scope / limitations**

- Not applicable — no manifest-level exclusion is recorded. Scope is limited to the entry points and exact contracts listed above; unrelated handlers in the same module are excluded.

### 1.3 Preconditions and Postconditions

| Type | Condition |
| --- | --- |
| Precondition | Authenticated User is authenticated/authorized where the current contract requires it. |
| Precondition | Required ownership, membership, consent, resource state, and device/provider prerequisites pass current policies. |
| Postcondition | Reload aggregate/owned engagement state. |
| Postcondition | No side effect outside the feature-owned persistence/event/provider boundary occurs. |

### 1.4 Evidence Baseline

| Source ID | Type | Exact path | Revision | Authority |
| --- | --- | --- | --- | --- |
| `SRC-SRS-01` | Requirement | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-CO-04 | 2026-08-23 | Draft code-first requirement |
| `SRC-CODE-01` | Current code | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/controller/CommunityQuestionLikeController.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-02` | Current code | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/controller/CommunityAnswerLikeController.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-03` | Current code | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/controller/CommunityBookmarkController.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-04` | Current code | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/controller/CommunityTopicController.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-01` | Existing test | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/community/service/CommunityQuestionLikeServiceImplTest.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-02` | Existing test | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/community/service/CommunityAnswerLikeServiceImplTest.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-03` | Existing test | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/community/service/CommunityBookmarkServiceImplTest.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-04` | Existing test | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/community/service/TopicFollowServiceImplTest.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-SUPP-01` | Historical architecture/design evidence | `04_Implement/CommunityQuestionLike/CommunityQuestionLike_Architecture-Evidence.md` | Worktree `2026-08-23` | Historical evidence only; current code and canonical SRS/TDS override conflicts |
| `SRC-SUPP-02` | Historical verification evidence | `04_Implement/CommunityQuestionLike/CommunityQuestionLike_Verification-Evidence.md` | Worktree `2026-08-23` | Historical evidence only; current code and canonical SRS/TDS override conflicts |

### 1.5 Open Contradictions / Questions

- No material contradiction was found among the exact sources cited in Section 1.4. Behavior not evidenced by those sources is not approved or implied by this Draft.

## 2. Traceability Matrix

| Requirement | Behavior | Exact oracle source | Component | Test condition / case |
| --- | --- | --- | --- | --- |
| `UC-CO-04-FR-01` | Open eligible content or a topic. | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-CO-04 Normal Flow 1 | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/controller/CommunityAnswerLikeController.java` | `COND-01` / `UC-CO-04-TC-001` |
| `UC-CO-04-FR-02` | Toggle the requested engagement for the authenticated actor. | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-CO-04 Normal Flow 2 | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/controller/CommunityBookmarkController.java` | `COND-02` / `UC-CO-04-TC-002` |
| `UC-CO-04-FR-03` | Reload aggregate/owned engagement state. | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-CO-04 Normal Flow 3 | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/controller/CommunityBookmarkController.java` | `COND-03` / `UC-CO-04-TC-003` |
| `BR-01` | Each actor-target toggle is unique/idempotent according to its owning service. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/controller/CommunityAnswerLikeController.java` | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/controller/CommunityAnswerLikeController.java` | `COND-BR-01` / `UC-CO-04-TC-BR-001` |
| `BR-02` | Engagement cannot make hidden/ineligible content visible. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/controller/CommunityBookmarkController.java` | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/controller/CommunityBookmarkController.java` | `COND-BR-02` / `UC-CO-04-TC-BR-002` |

## 3. Architecture Decision Records (ADR)

### ADR-UC-CO-04-01 — Use a distinct actor-goal boundary

| Item | Decision |
| --- | --- |
| Context | The retired 43-UC catalogue grouped multiple triggers, lifecycles, and permission boundaries, making implementation/test traceability generic. |
| Options | Keep the broad catalogue; split by screen; split by actor goal plus lifecycle/security boundary. |
| Decision | Use `UC-CO-04 — Like, Bookmark, and Follow Community Content` as the canonical boundary because its operations share the stated actor outcome and current implementation evidence. |
| Consequences | Related supporting screens/endpoints stay in one TDS; different lifecycle/actor outcomes have separate UCs. |
| Source / Status | SRS Section 3.1 and current code audit / Draft |

### ADR-UC-CO-04-02 — Preserve unknowns as Open

| Item | Decision |
| --- | --- |
| Context | Exact schema fields, SLA values, and some controller error codes are not fully evidenced by this manifest alone. |
| Decision | Do not invent them. Mark them `Open` and require the exact DTO/entity/migration/policy oracle before production-code changes. |
| Consequences | This document accurately characterizes current scope; unresolved design-changing items block implementation approval. |
| Source / Status | `create-specs` evidence discipline / Draft |

## 4. Non-Functional Requirements and SLA

| NFR | Target | Oracle Source | Verification |
| --- | --- | --- | --- |
| Authorization / isolation | All requests follow exact role/ownership/membership/consent policy in current code. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/controller/CommunityQuestionLikeController.java` | Negative security cases in paired Test-Spec |
| Protected-data handling | No secrets or unnecessary health/location/identity/conversation/file payloads in logs, fixtures, screenshots, or audit detail. | Data classification header plus exact Section 9 request/response field inventories | Log/fixture review plus security tests |
| Availability / latency | Open — no approved feature-specific numeric SLA found. | Evidence needed: approved NFR/SLA source | Measure only; do not assert a fixed threshold |
| Accessibility | Open — confirm project-standard criteria for reachable UI surfaces. | Evidence needed: approved UX/accessibility standard | Applicable Web/Mobile UI checks |
| Retry / idempotency | Apply only semantics explicitly implemented by the owning service. | No explicit lock/version/idempotency marker is evidenced in the cited implementation sources; preserve observed behavior and add characterization before changing concurrency semantics | State/duplicate/concurrency cases where applicable |

## 5. Static Modeling

### 5.1 Component Responsibilities and Change Disposition

| Exact path | Disposition | Responsibility |
| --- | --- | --- |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/controller/CommunityQuestionLikeController.java` | Reuse | Current implementation evidence for Like, Bookmark, and Follow Community Content; inspect the exact symbol before implementation changes. |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/controller/CommunityAnswerLikeController.java` | Reuse | Current implementation evidence for Like, Bookmark, and Follow Community Content; inspect the exact symbol before implementation changes. |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/controller/CommunityBookmarkController.java` | Reuse | Current implementation evidence for Like, Bookmark, and Follow Community Content; inspect the exact symbol before implementation changes. |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/controller/CommunityTopicController.java` | Reuse | Current implementation evidence for Like, Bookmark, and Follow Community Content; inspect the exact symbol before implementation changes. |

### 5.2 Current Component Diagram

```plantuml
@startuml
skinparam classAttributeIconSize 0
class CommunityQuestionLikeController as "CommunityQuestionLikeController.java"
class CommunityAnswerLikeController as "CommunityAnswerLikeController.java"
CommunityQuestionLikeController --> CommunityAnswerLikeController
class CommunityBookmarkController as "CommunityBookmarkController.java"
CommunityAnswerLikeController --> CommunityBookmarkController
class CommunityTopicController as "CommunityTopicController.java"
CommunityBookmarkController --> CommunityTopicController
@enduml
```

### 5.3 Data / Schema / Migration Assessment

| Item | Assessment |
| --- | --- |
| Current stores/entities | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/entity/TopicType.java` |
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
Actor -> Client: Enter Like, Bookmark, and Follow Community Content
Client -> Domain: Open eligible content or a topic.
Domain --> Client: Result for step 1
Client -> Domain: Toggle the requested engagement for the authenticated actor.
Domain --> Client: Result for step 2
Client -> Domain: Reload aggregate/owned engagement state.
Domain --> Client: Result for step 3
Client --> Actor: Render canonical outcome
@enduml
```

### 6.2 Alternative, Error, Retry, and Concurrency Flows

| Flow | Expected design behavior | Oracle |
| --- | --- | --- |
| Cancel before mutation | No unintended write or provider side effect. | SRS UC-CO-04 Alternative Flow |
| Invalid input/state | Reject with the current contract; keep canonical state unchanged. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/controller/CommunityQuestionLikeController.java` |
| Wrong actor/scope | Fail closed without protected resource disclosure. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/controller/CommunityQuestionLikeController.java` |
| Dependency failure | Use only the implemented bounded fallback/retry; never report false success. | Not applicable at this cited baseline — no external adapter/provider import is evidenced in the listed implementation sources |
| Duplicate/concurrent mutation | Apply only current lock/version/idempotency semantics. | No explicit lock/version/idempotency marker is evidenced in the cited implementation sources; preserve observed behavior and add characterization before changing concurrency semantics |

### 6.3 State Model and Invariants

```plantuml
@startuml
[*] --> Eligible
Eligible --> InProgress : Open eligible content or a topic.
InProgress --> Outcome : Reload aggregate/owned engagement state.
InProgress --> Rejected : validation / authorization / state failure
Outcome --> [*]
Rejected --> Eligible : actor corrects eligible input
@enduml
```

- Each actor-target toggle is unique/idempotent according to its owning service.
- Engagement cannot make hidden/ineligible content visible.

## 7. Domain Event Catalog

| Direction | Event | Producer / Consumer | Payload / delivery / idempotency |
| --- | --- | --- | --- |
| Publish / consume | Current event evidence | Not applicable at this cited baseline — no event type, publisher, or listener is evidenced in the listed implementation sources | Preserve only events evidenced by the cited source set; if later call-path inspection finds none, the flow remains synchronous. |

## 8. Interface Specification

### 8.1 User / Operator Interfaces

| # | Entry point | Actor | Contract |
| ---: | --- | --- | --- |
| 1 | Question/answer detail | Authenticated User | Reachable current entry point |
| 2 | Mobile Bookmarked Questions screen | Authenticated User | Reachable current entry point |
| 3 | Topic directory | Authenticated User | Reachable current entry point |

### 8.2 Service / Repository Interfaces

| Interface evidence | Required responsibility |
| --- | --- |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/controller/CommunityQuestionLikeController.java` | Support the mapped operations without broadening authorization or lifecycle semantics. |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/controller/CommunityAnswerLikeController.java` | Support the mapped operations without broadening authorization or lifecycle semantics. |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/controller/CommunityBookmarkController.java` | Support the mapped operations without broadening authorization or lifecycle semantics. |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/controller/CommunityTopicController.java` | Support the mapped operations without broadening authorization or lifecycle semantics. |

## 9. API Specification

| ID | Method / path or grouped controller surface | Auth / role | Request / response / errors |
| --- | --- | --- | --- |
| `API-01` | `POST /api/v1/community/answers/{answerId}/like` | isAuthenticated() | Handler `toggleLike`; parameters: path `answerId`: `UUID`; principal `principal`: `Principal`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<LikeToggleResponse>>`; response payload fields: `liked`: `boolean` (no field annotation in current DTO); `likeCount`: `int` (no field annotation in current DTO); `answerId`: `UUID` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/controller/CommunityAnswerLikeController.java` |
| `API-02` | `GET /api/v1/community/me/bookmarks` | isAuthenticated() | Handler `getBookmarks`; parameters: query `page`: `int`; query `size`: `int`; principal `principal`: `Principal`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<PaginatedResponse<CommunityFeedItemResponse>>`; response payload fields: Not applicable or unresolved from the handler import; explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/controller/CommunityBookmarkController.java` |
| `API-03` | `POST /api/v1/community/questions/{questionId}/bookmark` | isAuthenticated() | Handler `toggleBookmark`; parameters: path `questionId`: `UUID`; principal `principal`: `Principal`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<BookmarkToggleResponse>>`; response payload fields: `bookmarked`: `boolean` (no field annotation in current DTO); `questionId`: `UUID` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/controller/CommunityBookmarkController.java` |
| `API-04` | `POST /api/v1/community/questions/{questionId}/like` | isAuthenticated() | Handler `toggleLike`; parameters: path `questionId`: `UUID`; principal `principal`: `Principal`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<QuestionLikeToggleResponse>>`; response payload fields: `liked`: `boolean` (no field annotation in current DTO); `likeCount`: `int` (no field annotation in current DTO); `questionId`: `UUID` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/controller/CommunityQuestionLikeController.java` |
| `API-05` | `POST /api/v1/community/topics/{id}/follow` | isAuthenticated() | Handler `toggleFollow`; parameters: path `id`: `UUID`; principal `principal`: `Principal`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<TopicFollowResponse>>`; response payload fields: `topicId`: `UUID` (no field annotation in current DTO); `followed`: `boolean` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/controller/CommunityTopicController.java` |

Method-level Spring handlers, authorization annotations, request DTO fields/validators, response payload fields, and explicit/documented statuses above were extracted from the cited current source. Service/advice-only application error codes are not claimed where the controller does not declare them.

### 9.1 Handler Contract — `POST /api/v1/community/answers/{answerId}/like`

| Item | Exact current contract |
| --- | --- |
| Handler | `toggleLike` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/controller/CommunityAnswerLikeController.java` |
| Authorization annotation / boundary | isAuthenticated() |
| Parameters | path `answerId`: `UUID`; principal `principal`: `Principal` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<LikeToggleResponse>>` |
| Response payload fields | `liked`: `boolean` (no field annotation in current DTO); `likeCount`: `int` (no field annotation in current DTO); `answerId`: `UUID` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-001` / `UC-CO-04-TC-API-001` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.2 Handler Contract — `GET /api/v1/community/me/bookmarks`

| Item | Exact current contract |
| --- | --- |
| Handler | `getBookmarks` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/controller/CommunityBookmarkController.java` |
| Authorization annotation / boundary | isAuthenticated() |
| Parameters | query `page`: `int`; query `size`: `int`; principal `principal`: `Principal` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<PaginatedResponse<CommunityFeedItemResponse>>` |
| Response payload fields | Not applicable or unresolved from the handler import |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-002` / `UC-CO-04-TC-API-002` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.3 Handler Contract — `POST /api/v1/community/questions/{questionId}/bookmark`

| Item | Exact current contract |
| --- | --- |
| Handler | `toggleBookmark` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/controller/CommunityBookmarkController.java` |
| Authorization annotation / boundary | isAuthenticated() |
| Parameters | path `questionId`: `UUID`; principal `principal`: `Principal` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<BookmarkToggleResponse>>` |
| Response payload fields | `bookmarked`: `boolean` (no field annotation in current DTO); `questionId`: `UUID` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-003` / `UC-CO-04-TC-API-003` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.4 Handler Contract — `POST /api/v1/community/questions/{questionId}/like`

| Item | Exact current contract |
| --- | --- |
| Handler | `toggleLike` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/controller/CommunityQuestionLikeController.java` |
| Authorization annotation / boundary | isAuthenticated() |
| Parameters | path `questionId`: `UUID`; principal `principal`: `Principal` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<QuestionLikeToggleResponse>>` |
| Response payload fields | `liked`: `boolean` (no field annotation in current DTO); `likeCount`: `int` (no field annotation in current DTO); `questionId`: `UUID` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-004` / `UC-CO-04-TC-API-004` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.5 Handler Contract — `POST /api/v1/community/topics/{id}/follow`

| Item | Exact current contract |
| --- | --- |
| Handler | `toggleFollow` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/controller/CommunityTopicController.java` |
| Authorization annotation / boundary | isAuthenticated() |
| Parameters | path `id`: `UUID`; principal `principal`: `Principal` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<TopicFollowResponse>>` |
| Response payload fields | `topicId`: `UUID` (no field annotation in current DTO); `followed`: `boolean` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-005` / `UC-CO-04-TC-API-005` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

## 10. Error Codes

| Error class | HTTP / code | Trigger | Client behavior | Oracle |
| --- | --- | --- | --- | --- |
| Validation/business rejection | No 4xx declared by selected handler syntax; framework/service/advice mapping applies | Invalid field/range/state/ownership input | No write or false success; show only current mapped error | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/controller/CommunityQuestionLikeController.java` plus exact exception advice/service characterization |
| Authentication/authorization | `401/403` only where the security chain or handler policy maps them | Missing credential or disallowed role/scope | Fail closed with no protected response fields | Security configuration and `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/controller/CommunityQuestionLikeController.java` |
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
| `VG-01` | Open eligible content or a topic. | `COND-01` | `UC-CO-04-TC-001` |
| `VG-02` | Toggle the requested engagement for the authenticated actor. | `COND-02` | `UC-CO-04-TC-002` |
| `VG-03` | Reload aggregate/owned engagement state. | `COND-03` | `UC-CO-04-TC-003` |
| `VG-AUTH` | Reject wrong authentication/role/ownership/membership/consent scope | `COND-AUTH` | `UC-CO-04-TC-SEC-001` |
| `VG-GAP` | Characterize each known gap without claiming a false completed path | `COND-GAP` | `UC-CO-04-TC-GAP-001` |

## 14. Verification Methods

Existing focused evidence:

- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/community/service/CommunityQuestionLikeServiceImplTest.java`
- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/community/service/CommunityAnswerLikeServiceImplTest.java`
- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/community/service/CommunityBookmarkServiceImplTest.java`
- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/community/service/TopicFollowServiceImplTest.java`

Exact supported commands derived from the audited test paths:

- `cd 05_Development/CareBridgeAPI && ./mvnw -Dtest=CommunityQuestionLikeServiceImplTest test`
- `cd 05_Development/CareBridgeAPI && ./mvnw -Dtest=CommunityAnswerLikeServiceImplTest test`
- `cd 05_Development/CareBridgeAPI && ./mvnw -Dtest=CommunityBookmarkServiceImplTest test`
- `cd 05_Development/CareBridgeAPI && ./mvnw -Dtest=TopicFollowServiceImplTest test`

Record pass/fail/skip counts only after executing these commands on the exact revision.

## 15. Verification Samples

| Sample | Value |
| --- | --- |
| Primary contract | POST `/api/v1/community/questions/{questionId}/like` |
| Request | `POST /api/v1/community/answers/{answerId}/like` → `toggleLike`; `None` with Not applicable — no request body; authorization: isAuthenticated(). |
| Success response | `ResponseEntity<ApiResponse<LikeToggleResponse>>` with `liked`: `boolean` (no field annotation in current DTO); `likeCount`: `int` (no field annotation in current DTO); `answerId`: `UUID` (no field annotation in current DTO); explicit/documented statuses `200`. |
| Negative sample | Use wrong role/owner/state and assert the exact mapped error without protected payload. |

## 16. Authorization Matrix

| Actor / role | Operation | Decision |
| --- | --- | --- |
| Authenticated User | `POST /api/v1/community/answers/{answerId}/like` | isAuthenticated(); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/controller/CommunityAnswerLikeController.java` |
| Authenticated User | `GET /api/v1/community/me/bookmarks` | isAuthenticated(); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/controller/CommunityBookmarkController.java` |
| Authenticated User | `POST /api/v1/community/questions/{questionId}/bookmark` | isAuthenticated(); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/controller/CommunityBookmarkController.java` |
| Authenticated User | `POST /api/v1/community/questions/{questionId}/like` | isAuthenticated(); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/controller/CommunityQuestionLikeController.java` |
| Authenticated User | `POST /api/v1/community/topics/{id}/follow` | isAuthenticated(); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/controller/CommunityTopicController.java` |
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
