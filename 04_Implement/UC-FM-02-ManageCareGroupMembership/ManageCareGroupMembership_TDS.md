# ENGINEERING DOCUMENTATION STANDARD (EDS) v2.0

# TECHNICAL DESIGN SPECIFICATION — Manage Invitations, Join Requests, and Membership

| Field | Value |
| --- | --- |
| Document ID | `UC-FM-02-TDS` |
| Version | `0.1` |
| Date | `2026-08-23` |
| Status | `Draft` |
| Function ID | `UC-FM-02` |
| Canonical Use Case | `UC-FM-02 — Manage Invitations, Join Requests, and Membership` |
| Module / Bounded Context | `Family Cooperative Care` |
| Primary Actor | `Group Owner / Invitee / Applicant` |
| Platforms | `Mobile / Backend` |
| Priority | `Medium` |
| Data Classification | `Restricted care-group relationship, shared-care task, appointment, note, permission, and consent-scoped data` |
| Compliance Scope | `PDPA relationship/consent scoping, least privilege, cross-member isolation, and auditability of permission-changing actions` |
| Owner | `CareBridge Team` |
| Reviewer / Approver |  |
| Source Baseline | Current worktree on `2026-08-23`; SRS `UC-FM-02`; exact evidence in Section 1.4 |

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

- **Goal:** Invite a member, accept/decline/revoke an invitation, request to join, approve/reject a request, or remove an eligible member.
- **Trigger:** The actor enters Mobile care-group invitation/member/join-request screens.
- **Outcome:** Refresh canonical membership and invitation state.
- **Current state:** `High` confidence from reachable code/test audit; no manifest-level exclusion is recorded.
- **Target state:** Preserve current code-backed behavior and resolve only explicitly evidenced limitations through approved implementation work.

### 1.2 Scope

**In scope**

- Mobile care-group invitation/member/join-request screens

- GET `/api/v1/care-groups/invitations/me`
- POST `/api/v1/care-groups/invitations/{token}/accept`
- POST `/api/v1/care-groups/join`
- POST `/api/v1/care-groups/{groupId}/invitations`
- POST `/api/v1/care-groups/{groupId}/invitations/accept`
- POST `/api/v1/care-groups/{groupId}/invitations/decline`
- POST `/api/v1/care-groups/{groupId}/invitations/{targetUserId}/revoke`
- GET `/api/v1/care-groups/{groupId}/join-requests`
- POST `/api/v1/care-groups/{groupId}/join-requests/{memberId}/respond`
- GET `/api/v1/care-groups/{groupId}/members`
- DELETE `/api/v1/care-groups/{groupId}/members/{targetUserId}`

**Out of scope / limitations**

- Not applicable — no manifest-level exclusion is recorded. Scope is limited to the entry points and exact contracts listed above; unrelated handlers in the same module are excluded.

### 1.3 Preconditions and Postconditions

| Type | Condition |
| --- | --- |
| Precondition | Group Owner / Invitee / Applicant is authenticated/authorized where the current contract requires it. |
| Precondition | Required ownership, membership, consent, resource state, and device/provider prerequisites pass current policies. |
| Postcondition | Refresh canonical membership and invitation state. |
| Postcondition | No side effect outside the feature-owned persistence/event/provider boundary occurs. |

### 1.4 Evidence Baseline

| Source ID | Type | Exact path | Revision | Authority |
| --- | --- | --- | --- | --- |
| `SRC-SRS-01` | Requirement | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-FM-02 | 2026-08-23 | Draft code-first requirement |
| `SRC-CODE-01` | Current code | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-02` | Current code | `05_Development/CareBridgeMobileApp/lib/features/familySync/screens/care_group_invitation_screen.dart` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-03` | Current code | `05_Development/CareBridgeMobileApp/lib/features/familySync/screens/care_group_members_screen.dart` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-01` | Existing test | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/CareGroupInviteIntegrationTest.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-02` | Existing test | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/service/CareGroupServiceImplMembershipLifecycleTest.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-03` | Existing test | `05_Development/CareBridgeMobileApp/test/features/familySync/care_group_invitation_screen_test.dart` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-ERROR-01` | Current exception advice | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/common/exception/GlobalExceptionHandler.java` | Worktree `2026-08-23` | Current-state evidence |

### 1.5 Open Contradictions / Questions

- No material contradiction was found among the exact sources cited in Section 1.4. Behavior not evidenced by those sources is not approved or implied by this Draft.

## 2. Traceability Matrix

| Requirement | Behavior | Exact oracle source | Component | Test condition / case |
| --- | --- | --- | --- | --- |
| `UC-FM-02-FR-01` | Create an invitation or join request. | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-FM-02 Normal Flow 1 | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` | `COND-01` / `UC-FM-02-TC-001` |
| `UC-FM-02-FR-02` | The receiving/owning actor accepts, declines, revokes, approves, or rejects it. | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-FM-02 Normal Flow 2 | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` | `COND-02` / `UC-FM-02-TC-002` |
| `UC-FM-02-FR-03` | Refresh canonical membership and invitation state. | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-FM-02 Normal Flow 3 | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` | `COND-03` / `UC-FM-02-TC-003` |
| `BR-01` | Invitation/join-request states, expiry, uniqueness, and actor authority are server authoritative. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` | `COND-BR-01` / `UC-FM-02-TC-BR-001` |
| `BR-02` | A token or UI state cannot bypass membership policy. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` | `COND-BR-02` / `UC-FM-02-TC-BR-002` |

## 3. Architecture Decision Records (ADR)

### ADR-UC-FM-02-01 — Use a distinct actor-goal boundary

| Item | Decision |
| --- | --- |
| Context | The retired 43-UC catalogue grouped multiple triggers, lifecycles, and permission boundaries, making implementation/test traceability generic. |
| Options | Keep the broad catalogue; split by screen; split by actor goal plus lifecycle/security boundary. |
| Decision | Use `UC-FM-02 — Manage Invitations, Join Requests, and Membership` as the canonical boundary because its operations share the stated actor outcome and current implementation evidence. |
| Consequences | Related supporting screens/endpoints stay in one TDS; different lifecycle/actor outcomes have separate UCs. |
| Source / Status | SRS Section 3.1 and current code audit / Draft |

### ADR-UC-FM-02-02 — Preserve unknowns as Open

| Item | Decision |
| --- | --- |
| Context | Exact schema fields, SLA values, and some controller error codes are not fully evidenced by this manifest alone. |
| Decision | Do not invent them. Mark them `Open` and require the exact DTO/entity/migration/policy oracle before production-code changes. |
| Consequences | This document accurately characterizes current scope; unresolved design-changing items block implementation approval. |
| Source / Status | `create-specs` evidence discipline / Draft |

## 4. Non-Functional Requirements and SLA

| NFR | Target | Oracle Source | Verification |
| --- | --- | --- | --- |
| Authorization / isolation | All requests follow exact role/ownership/membership/consent policy in current code. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` | Negative security cases in paired Test-Spec |
| Protected-data handling | No secrets or unnecessary health/location/identity/conversation/file payloads in logs, fixtures, screenshots, or audit detail. | Data classification header plus exact Section 9 request/response field inventories | Log/fixture review plus security tests |
| Availability / latency | Open — no approved feature-specific numeric SLA found. | Evidence needed: approved NFR/SLA source | Measure only; do not assert a fixed threshold |
| Accessibility | Open — confirm project-standard criteria for reachable UI surfaces. | Evidence needed: approved UX/accessibility standard | Applicable Web/Mobile UI checks |
| Retry / idempotency | Apply only semantics explicitly implemented by the owning service. | No explicit lock/version/idempotency marker is evidenced in the cited implementation sources; preserve observed behavior and add characterization before changing concurrency semantics | State/duplicate/concurrency cases where applicable |

## 5. Static Modeling

### 5.1 Component Responsibilities and Change Disposition

| Exact path | Disposition | Responsibility |
| --- | --- | --- |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` | Reuse | Current implementation evidence for Manage Invitations, Join Requests, and Membership; inspect the exact symbol before implementation changes. |
| `05_Development/CareBridgeMobileApp/lib/features/familySync/screens/care_group_invitation_screen.dart` | Reuse | Current implementation evidence for Manage Invitations, Join Requests, and Membership; inspect the exact symbol before implementation changes. |
| `05_Development/CareBridgeMobileApp/lib/features/familySync/screens/care_group_members_screen.dart` | Reuse | Current implementation evidence for Manage Invitations, Join Requests, and Membership; inspect the exact symbol before implementation changes. |

### 5.2 Current Component Diagram

```plantuml
@startuml
skinparam classAttributeIconSize 0
class CareGroupController as "CareGroupController.java"
class care_group_invitation_screen as "care_group_invitation_screen.dart"
CareGroupController --> care_group_invitation_screen
class care_group_members_screen as "care_group_members_screen.dart"
care_group_invitation_screen --> care_group_members_screen
@enduml
```

### 5.3 Data / Schema / Migration Assessment

| Item | Assessment |
| --- | --- |
| Current stores/entities | No entity/repository import is present in the cited entry/source set; follow the exact handler-to-service call path before any schema change |
| Sensitive fields | Restricted care-group relationship, shared-care task, appointment, note, permission, and consent-scoped data. Exact transport fields and validators are enumerated per handler in Section 9; entity-only fields require the cited service/entity source before a schema change. |
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
Actor -> Client: Enter Manage Invitations, Join Requests, and Membership
Client -> Domain: Create an invitation or join request.
Domain --> Client: Result for step 1
Client -> Domain: The receiving/owning actor accepts, declines, revokes, approves, or rejects it.
Domain --> Client: Result for step 2
Client -> Domain: Refresh canonical membership and invitation state.
Domain --> Client: Result for step 3
Client --> Actor: Render canonical outcome
@enduml
```

### 6.2 Alternative, Error, Retry, and Concurrency Flows

| Flow | Expected design behavior | Oracle |
| --- | --- | --- |
| Cancel before mutation | No unintended write or provider side effect. | SRS UC-FM-02 Alternative Flow |
| Invalid input/state | Reject with the current contract; keep canonical state unchanged. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` |
| Wrong actor/scope | Fail closed without protected resource disclosure. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` |
| Dependency failure | Use only the implemented bounded fallback/retry; never report false success. | Not applicable at this cited baseline — no external adapter/provider import is evidenced in the listed implementation sources |
| Duplicate/concurrent mutation | Apply only current lock/version/idempotency semantics. | No explicit lock/version/idempotency marker is evidenced in the cited implementation sources; preserve observed behavior and add characterization before changing concurrency semantics |

### 6.3 State Model and Invariants

```plantuml
@startuml
[*] --> Eligible
Eligible --> InProgress : Create an invitation or join request.
InProgress --> Outcome : Refresh canonical membership and invitation state.
InProgress --> Rejected : validation / authorization / state failure
Outcome --> [*]
Rejected --> Eligible : actor corrects eligible input
@enduml
```

- Invitation/join-request states, expiry, uniqueness, and actor authority are server authoritative.
- A token or UI state cannot bypass membership policy.

## 7. Domain Event Catalog

| Direction | Event | Producer / Consumer | Payload / delivery / idempotency |
| --- | --- | --- | --- |
| Publish / consume | Current event evidence | Not applicable at this cited baseline — no event type, publisher, or listener is evidenced in the listed implementation sources | Preserve only events evidenced by the cited source set; if later call-path inspection finds none, the flow remains synchronous. |

## 8. Interface Specification

### 8.1 User / Operator Interfaces

| # | Entry point | Actor | Contract |
| ---: | --- | --- | --- |
| 1 | Mobile care-group invitation/member/join-request screens | Group Owner / Invitee / Applicant | Reachable current entry point |

### 8.2 Service / Repository Interfaces

| Interface evidence | Required responsibility |
| --- | --- |
| `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` | Support the mapped operations without broadening authorization or lifecycle semantics. |
| `05_Development/CareBridgeMobileApp/lib/features/familySync/screens/care_group_invitation_screen.dart` | Support the mapped operations without broadening authorization or lifecycle semantics. |
| `05_Development/CareBridgeMobileApp/lib/features/familySync/screens/care_group_members_screen.dart` | Support the mapped operations without broadening authorization or lifecycle semantics. |

## 9. API Specification

| ID | Method / path or grouped controller surface | Auth / role | Request / response / errors |
| --- | --- | --- | --- |
| `API-01` | `GET /api/v1/care-groups/invitations/me` | isAuthenticated() | Handler `listMyInvitations`; parameters: principal `principal`: `Principal`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<List<PendingInvitationDto>>>`; response payload fields: `groupId`: `UUID` (no field annotation in current DTO); `groupName`: `String` (no field annotation in current DTO); `memberRole`: `String` (no field annotation in current DTO); `invitedAt`: `Instant` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` |
| `API-02` | `POST /api/v1/care-groups/invitations/{token}/accept` | isAuthenticated() | Handler `acceptInvitationByToken`; parameters: path `token`: `String`; body `request`: `com.carebridge.backend.family.dto.FamilyRelationshipRoleRequest`; principal `principal`: `Principal`; request body: `com.carebridge.backend.family.dto.FamilyRelationshipRoleRequest`; request fields/validation: Not applicable or unresolved from the handler import; response: `ResponseEntity<ApiResponse<AcceptInvitationByTokenResponse>>`; response payload fields: `careGroupId`: `UUID` (no field annotation in current DTO); `careGroupMemberId`: `UUID` (no field annotation in current DTO); `inviteStatus`: `String` (no field annotation in current DTO); `joinedAt`: `Instant` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` |
| `API-03` | `POST /api/v1/care-groups/join` | isAuthenticated() | Handler `joinGroupByCode`; parameters: body `request`: `JoinCareGroupRequest`; principal `principal`: `Principal`; request body: `JoinCareGroupRequest`; request fields/validation: `code`: `String` (@NotBlank(message = "Mã mời không được để trống")); `familyRelationshipRole`: `String` (@NotBlank(message = "Vai trò trong gia đình không được để trống")); `customFamilyRelationshipRole`: `String` (no field annotation in current DTO); response: `ResponseEntity<ApiResponse<CareGroupSummaryDto>>`; response payload fields: `groupId`: `UUID` (no field annotation in current DTO); `groupName`: `String` (no field annotation in current DTO); `isActive`: `boolean` (no field annotation in current DTO); `totalMembers`: `int` (no field annotation in current DTO); `myRole`: `String` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` |
| `API-04` | `POST /api/v1/care-groups/{groupId}/invitations` | hasRole('MOTHER') | Handler `inviteFamilyMember`; parameters: path `groupId`: `UUID`; body `request`: `InviteFamilyMemberRequest`; principal `principal`: `Principal`; request body: `InviteFamilyMemberRequest`; request fields/validation: `channel`: `InviteChannel` (@NotNull); `phone`: `String` (no field annotation in current DTO); response: `ResponseEntity<ApiResponse<InviteFamilyMemberResponse>>`; response payload fields: `careGroupMemberId`: `UUID` (no field annotation in current DTO); `channel`: `InviteChannel` (no field annotation in current DTO); `inviteToken`: `String` (no field annotation in current DTO); `inviteExpiresAt`: `Instant` (no field annotation in current DTO); `invitedPhone`: `String` (no field annotation in current DTO); explicit/documented statuses: `201`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` |
| `API-05` | `POST /api/v1/care-groups/{groupId}/invitations/accept` | isAuthenticated() | Handler `acceptInvite`; parameters: path `groupId`: `UUID`; body `request`: `com.carebridge.backend.family.dto.FamilyRelationshipRoleRequest`; principal `principal`: `Principal`; request body: `com.carebridge.backend.family.dto.FamilyRelationshipRoleRequest`; request fields/validation: Not applicable or unresolved from the handler import; response: `ResponseEntity<ApiResponse<CareGroupMemberDto>>`; response payload fields: `memberId`: `UUID` (no field annotation in current DTO); `userId`: `UUID` (no field annotation in current DTO); `displayName`: `String` (no field annotation in current DTO); `memberRole`: `String` (no field annotation in current DTO); `familyRelationshipRole`: `String` (no field annotation in current DTO); `customFamilyRelationshipRole`: `String` (no field annotation in current DTO); `inviteStatus`: `String` (no field annotation in current DTO); `isJoinRequest`: `Boolean` (no field annotation in current DTO); `joinedAt`: `Instant` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` |
| `API-06` | `POST /api/v1/care-groups/{groupId}/invitations/decline` | isAuthenticated() | Handler `declineInvite`; parameters: path `groupId`: `UUID`; principal `principal`: `Principal`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<Void>>`; response payload fields: Not applicable or unresolved from the handler import; explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` |
| `API-07` | `POST /api/v1/care-groups/{groupId}/invitations/{targetUserId}/revoke` | hasRole('MOTHER') | Handler `revokeInvitation`; parameters: path `groupId`: `UUID`; path `targetUserId`: `UUID`; principal `principal`: `Principal`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<RevokeInvitationResponse>>`; response payload fields: `careGroupMemberId`: `UUID` (no field annotation in current DTO); `groupId`: `UUID` (no field annotation in current DTO); `targetUserId`: `UUID` (no field annotation in current DTO); `inviteStatus`: `String` (no field annotation in current DTO); `revokedAt`: `Instant` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` |
| `API-08` | `GET /api/v1/care-groups/{groupId}/join-requests` | hasRole('MOTHER') | Handler `listJoinRequests`; parameters: path `groupId`: `UUID`; principal `principal`: `Principal`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<List<com.carebridge.backend.family.dto.JoinRequestDto>>>`; response payload fields: Not applicable or unresolved from the handler import; explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` |
| `API-09` | `POST /api/v1/care-groups/{groupId}/join-requests/{memberId}/respond` | hasRole('MOTHER') | Handler `respondJoinRequest`; parameters: path `groupId`: `UUID`; path `memberId`: `UUID`; query `approve`: `boolean`; principal `principal`: `Principal`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<CareGroupMemberDto>>`; response payload fields: `memberId`: `UUID` (no field annotation in current DTO); `userId`: `UUID` (no field annotation in current DTO); `displayName`: `String` (no field annotation in current DTO); `memberRole`: `String` (no field annotation in current DTO); `familyRelationshipRole`: `String` (no field annotation in current DTO); `customFamilyRelationshipRole`: `String` (no field annotation in current DTO); `inviteStatus`: `String` (no field annotation in current DTO); `isJoinRequest`: `Boolean` (no field annotation in current DTO); `joinedAt`: `Instant` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` |
| `API-10` | `GET /api/v1/care-groups/{groupId}/members` | isAuthenticated() | Handler `listMembers`; parameters: path `groupId`: `UUID`; principal `principal`: `Principal`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<CareGroupMembersResponse>>`; response payload fields: `groupId`: `UUID` (no field annotation in current DTO); `groupName`: `String` (no field annotation in current DTO); `totalMembers`: `int` (no field annotation in current DTO); `members`: `List<CareGroupMemberDto>` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` |
| `API-11` | `DELETE /api/v1/care-groups/{groupId}/members/{targetUserId}` | hasRole('MOTHER') | Handler `removeMember`; parameters: path `groupId`: `UUID`; path `targetUserId`: `UUID`; principal `principal`: `Principal`; request body: `None`; request fields/validation: Not applicable — no request body; response: `ResponseEntity<ApiResponse<RemoveMemberResponse>>`; response payload fields: `careGroupMemberId`: `UUID` (no field annotation in current DTO); `groupId`: `UUID` (no field annotation in current DTO); `targetUserId`: `UUID` (no field annotation in current DTO); `inviteStatus`: `String` (no field annotation in current DTO); `removedAt`: `Instant` (no field annotation in current DTO); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` |

Method-level Spring handlers, authorization annotations, request DTO fields/validators, response payload fields, and explicit/documented statuses above were extracted from the cited current source. Service/advice-only application error codes are not claimed where the controller does not declare them.

### 9.1 Handler Contract — `GET /api/v1/care-groups/invitations/me`

| Item | Exact current contract |
| --- | --- |
| Handler | `listMyInvitations` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` |
| Authorization annotation / boundary | isAuthenticated() |
| Parameters | principal `principal`: `Principal` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<List<PendingInvitationDto>>>` |
| Response payload fields | `groupId`: `UUID` (no field annotation in current DTO); `groupName`: `String` (no field annotation in current DTO); `memberRole`: `String` (no field annotation in current DTO); `invitedAt`: `Instant` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-001` / `UC-FM-02-TC-API-001` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.2 Handler Contract — `POST /api/v1/care-groups/invitations/{token}/accept`

| Item | Exact current contract |
| --- | --- |
| Handler | `acceptInvitationByToken` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` |
| Authorization annotation / boundary | isAuthenticated() |
| Parameters | path `token`: `String`; body `request`: `com.carebridge.backend.family.dto.FamilyRelationshipRoleRequest`; principal `principal`: `Principal` |
| Request body type | `com.carebridge.backend.family.dto.FamilyRelationshipRoleRequest` |
| Request fields and validators | Not applicable or unresolved from the handler import |
| Response type | `ResponseEntity<ApiResponse<AcceptInvitationByTokenResponse>>` |
| Response payload fields | `careGroupId`: `UUID` (no field annotation in current DTO); `careGroupMemberId`: `UUID` (no field annotation in current DTO); `inviteStatus`: `String` (no field annotation in current DTO); `joinedAt`: `Instant` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-002` / `UC-FM-02-TC-API-002` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.3 Handler Contract — `POST /api/v1/care-groups/join`

| Item | Exact current contract |
| --- | --- |
| Handler | `joinGroupByCode` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` |
| Authorization annotation / boundary | isAuthenticated() |
| Parameters | body `request`: `JoinCareGroupRequest`; principal `principal`: `Principal` |
| Request body type | `JoinCareGroupRequest` |
| Request fields and validators | `code`: `String` (@NotBlank(message = "Mã mời không được để trống")); `familyRelationshipRole`: `String` (@NotBlank(message = "Vai trò trong gia đình không được để trống")); `customFamilyRelationshipRole`: `String` (no field annotation in current DTO) |
| Response type | `ResponseEntity<ApiResponse<CareGroupSummaryDto>>` |
| Response payload fields | `groupId`: `UUID` (no field annotation in current DTO); `groupName`: `String` (no field annotation in current DTO); `isActive`: `boolean` (no field annotation in current DTO); `totalMembers`: `int` (no field annotation in current DTO); `myRole`: `String` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-003` / `UC-FM-02-TC-API-003` |
| Negative test mapping | `COND-API-003-VAL` / `UC-FM-02-TC-API-003-VAL`; plus `COND-AUTH` for protected access |

### 9.4 Handler Contract — `POST /api/v1/care-groups/{groupId}/invitations`

| Item | Exact current contract |
| --- | --- |
| Handler | `inviteFamilyMember` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` |
| Authorization annotation / boundary | hasRole('MOTHER') |
| Parameters | path `groupId`: `UUID`; body `request`: `InviteFamilyMemberRequest`; principal `principal`: `Principal` |
| Request body type | `InviteFamilyMemberRequest` |
| Request fields and validators | `channel`: `InviteChannel` (@NotNull); `phone`: `String` (no field annotation in current DTO) |
| Response type | `ResponseEntity<ApiResponse<InviteFamilyMemberResponse>>` |
| Response payload fields | `careGroupMemberId`: `UUID` (no field annotation in current DTO); `channel`: `InviteChannel` (no field annotation in current DTO); `inviteToken`: `String` (no field annotation in current DTO); `inviteExpiresAt`: `Instant` (no field annotation in current DTO); `invitedPhone`: `String` (no field annotation in current DTO) |
| Explicit/documented statuses | `201` |
| Positive test mapping | `COND-API-004` / `UC-FM-02-TC-API-004` |
| Negative test mapping | `COND-API-004-VAL` / `UC-FM-02-TC-API-004-VAL`; plus `COND-AUTH` for protected access |

### 9.5 Handler Contract — `POST /api/v1/care-groups/{groupId}/invitations/accept`

| Item | Exact current contract |
| --- | --- |
| Handler | `acceptInvite` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` |
| Authorization annotation / boundary | isAuthenticated() |
| Parameters | path `groupId`: `UUID`; body `request`: `com.carebridge.backend.family.dto.FamilyRelationshipRoleRequest`; principal `principal`: `Principal` |
| Request body type | `com.carebridge.backend.family.dto.FamilyRelationshipRoleRequest` |
| Request fields and validators | Not applicable or unresolved from the handler import |
| Response type | `ResponseEntity<ApiResponse<CareGroupMemberDto>>` |
| Response payload fields | `memberId`: `UUID` (no field annotation in current DTO); `userId`: `UUID` (no field annotation in current DTO); `displayName`: `String` (no field annotation in current DTO); `memberRole`: `String` (no field annotation in current DTO); `familyRelationshipRole`: `String` (no field annotation in current DTO); `customFamilyRelationshipRole`: `String` (no field annotation in current DTO); `inviteStatus`: `String` (no field annotation in current DTO); `isJoinRequest`: `Boolean` (no field annotation in current DTO); `joinedAt`: `Instant` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-005` / `UC-FM-02-TC-API-005` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.6 Handler Contract — `POST /api/v1/care-groups/{groupId}/invitations/decline`

| Item | Exact current contract |
| --- | --- |
| Handler | `declineInvite` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` |
| Authorization annotation / boundary | isAuthenticated() |
| Parameters | path `groupId`: `UUID`; principal `principal`: `Principal` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<Void>>` |
| Response payload fields | Not applicable or unresolved from the handler import |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-006` / `UC-FM-02-TC-API-006` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.7 Handler Contract — `POST /api/v1/care-groups/{groupId}/invitations/{targetUserId}/revoke`

| Item | Exact current contract |
| --- | --- |
| Handler | `revokeInvitation` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` |
| Authorization annotation / boundary | hasRole('MOTHER') |
| Parameters | path `groupId`: `UUID`; path `targetUserId`: `UUID`; principal `principal`: `Principal` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<RevokeInvitationResponse>>` |
| Response payload fields | `careGroupMemberId`: `UUID` (no field annotation in current DTO); `groupId`: `UUID` (no field annotation in current DTO); `targetUserId`: `UUID` (no field annotation in current DTO); `inviteStatus`: `String` (no field annotation in current DTO); `revokedAt`: `Instant` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-007` / `UC-FM-02-TC-API-007` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.8 Handler Contract — `GET /api/v1/care-groups/{groupId}/join-requests`

| Item | Exact current contract |
| --- | --- |
| Handler | `listJoinRequests` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` |
| Authorization annotation / boundary | hasRole('MOTHER') |
| Parameters | path `groupId`: `UUID`; principal `principal`: `Principal` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<List<com.carebridge.backend.family.dto.JoinRequestDto>>>` |
| Response payload fields | Not applicable or unresolved from the handler import |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-008` / `UC-FM-02-TC-API-008` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.9 Handler Contract — `POST /api/v1/care-groups/{groupId}/join-requests/{memberId}/respond`

| Item | Exact current contract |
| --- | --- |
| Handler | `respondJoinRequest` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` |
| Authorization annotation / boundary | hasRole('MOTHER') |
| Parameters | path `groupId`: `UUID`; path `memberId`: `UUID`; query `approve`: `boolean`; principal `principal`: `Principal` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<CareGroupMemberDto>>` |
| Response payload fields | `memberId`: `UUID` (no field annotation in current DTO); `userId`: `UUID` (no field annotation in current DTO); `displayName`: `String` (no field annotation in current DTO); `memberRole`: `String` (no field annotation in current DTO); `familyRelationshipRole`: `String` (no field annotation in current DTO); `customFamilyRelationshipRole`: `String` (no field annotation in current DTO); `inviteStatus`: `String` (no field annotation in current DTO); `isJoinRequest`: `Boolean` (no field annotation in current DTO); `joinedAt`: `Instant` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-009` / `UC-FM-02-TC-API-009` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.10 Handler Contract — `GET /api/v1/care-groups/{groupId}/members`

| Item | Exact current contract |
| --- | --- |
| Handler | `listMembers` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` |
| Authorization annotation / boundary | isAuthenticated() |
| Parameters | path `groupId`: `UUID`; principal `principal`: `Principal` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<CareGroupMembersResponse>>` |
| Response payload fields | `groupId`: `UUID` (no field annotation in current DTO); `groupName`: `String` (no field annotation in current DTO); `totalMembers`: `int` (no field annotation in current DTO); `members`: `List<CareGroupMemberDto>` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-010` / `UC-FM-02-TC-API-010` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.11 Handler Contract — `DELETE /api/v1/care-groups/{groupId}/members/{targetUserId}`

| Item | Exact current contract |
| --- | --- |
| Handler | `removeMember` |
| Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` |
| Authorization annotation / boundary | hasRole('MOTHER') |
| Parameters | path `groupId`: `UUID`; path `targetUserId`: `UUID`; principal `principal`: `Principal` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `ResponseEntity<ApiResponse<RemoveMemberResponse>>` |
| Response payload fields | `careGroupMemberId`: `UUID` (no field annotation in current DTO); `groupId`: `UUID` (no field annotation in current DTO); `targetUserId`: `UUID` (no field annotation in current DTO); `inviteStatus`: `String` (no field annotation in current DTO); `removedAt`: `Instant` (no field annotation in current DTO) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-011` / `UC-FM-02-TC-API-011` |
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
| `VG-01` | Create an invitation or join request. | `COND-01` | `UC-FM-02-TC-001` |
| `VG-02` | The receiving/owning actor accepts, declines, revokes, approves, or rejects it. | `COND-02` | `UC-FM-02-TC-002` |
| `VG-03` | Refresh canonical membership and invitation state. | `COND-03` | `UC-FM-02-TC-003` |
| `VG-AUTH` | Reject wrong authentication/role/ownership/membership/consent scope | `COND-AUTH` | `UC-FM-02-TC-SEC-001` |
| `VG-GAP` | Characterize each known gap without claiming a false completed path | `COND-GAP` | `UC-FM-02-TC-GAP-001` |

## 14. Verification Methods

Existing focused evidence:

- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/CareGroupInviteIntegrationTest.java`
- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/service/CareGroupServiceImplMembershipLifecycleTest.java`
- `05_Development/CareBridgeMobileApp/test/features/familySync/care_group_invitation_screen_test.dart`

Exact supported commands derived from the audited test paths:

- `cd 05_Development/CareBridgeAPI && ./mvnw -Dtest=CareGroupInviteIntegrationTest test`
- `cd 05_Development/CareBridgeAPI && ./mvnw -Dtest=CareGroupServiceImplMembershipLifecycleTest test`
- `cd 05_Development/CareBridgeMobileApp && flutter test test/features/familySync/care_group_invitation_screen_test.dart`

Record pass/fail/skip counts only after executing these commands on the exact revision.

## 15. Verification Samples

| Sample | Value |
| --- | --- |
| Primary contract | GET `/api/v1/care-groups/invitations/me` |
| Request | `GET /api/v1/care-groups/invitations/me` → `listMyInvitations`; `None` with Not applicable — no request body; authorization: isAuthenticated(). |
| Success response | `ResponseEntity<ApiResponse<List<PendingInvitationDto>>>` with `groupId`: `UUID` (no field annotation in current DTO); `groupName`: `String` (no field annotation in current DTO); `memberRole`: `String` (no field annotation in current DTO); `invitedAt`: `Instant` (no field annotation in current DTO); explicit/documented statuses `200`. |
| Negative sample | Use wrong role/owner/state and assert the exact mapped error without protected payload. |

## 16. Authorization Matrix

| Actor / role | Operation | Decision |
| --- | --- | --- |
| Group Owner / Invitee / Applicant | `GET /api/v1/care-groups/invitations/me` | isAuthenticated(); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` |
| Group Owner / Invitee / Applicant | `POST /api/v1/care-groups/invitations/{token}/accept` | isAuthenticated(); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` |
| Group Owner / Invitee / Applicant | `POST /api/v1/care-groups/join` | isAuthenticated(); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` |
| Group Owner / Invitee / Applicant | `POST /api/v1/care-groups/{groupId}/invitations` | hasRole('MOTHER'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` |
| Group Owner / Invitee / Applicant | `POST /api/v1/care-groups/{groupId}/invitations/accept` | isAuthenticated(); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` |
| Group Owner / Invitee / Applicant | `POST /api/v1/care-groups/{groupId}/invitations/decline` | isAuthenticated(); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` |
| Group Owner / Invitee / Applicant | `POST /api/v1/care-groups/{groupId}/invitations/{targetUserId}/revoke` | hasRole('MOTHER'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` |
| Group Owner / Invitee / Applicant | `GET /api/v1/care-groups/{groupId}/join-requests` | hasRole('MOTHER'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` |
| Group Owner / Invitee / Applicant | `POST /api/v1/care-groups/{groupId}/join-requests/{memberId}/respond` | hasRole('MOTHER'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` |
| Group Owner / Invitee / Applicant | `GET /api/v1/care-groups/{groupId}/members` | isAuthenticated(); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` |
| Group Owner / Invitee / Applicant | `DELETE /api/v1/care-groups/{groupId}/members/{targetUserId}` | hasRole('MOTHER'); handler oracle `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java` |
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
