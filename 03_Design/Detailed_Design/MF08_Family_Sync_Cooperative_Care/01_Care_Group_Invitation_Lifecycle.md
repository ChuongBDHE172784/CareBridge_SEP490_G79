# MF-08 / Spec 01 — Care Group, Invitation and Membership Lifecycle

| Field | Value |
| --- | --- |
| Status | Draft |
| Use Cases Covered | UC-54 Manage Care Groups; UC-55 Manage Care Group Invitations; UC-60 Manage Care Group Membership |
| Use Case Group | Mobile App |
| Platform | Mother and Family Mobile; Backend |
| Primary Actors | Mother / Family |
| In Scope | Only the owner manages the group; invited members explicitly accept or reject |
| Explicitly Excluded | Placeholder add-group route as evidence by itself |
| Implementation Trace | UI: CareGroupsScreen, CareGroupInvitationScreen, CareGroupMembersScreen; Controller: CareGroupController; Service: CareGroupServiceImpl; Repository: CareGroupRepository, CareGroupMemberRepository; Entity: CareGroup, CareGroupMember |

## 1. Tổng quan luồng chính (Main Flow Overview)

Only the owner manages the group; invited members explicitly accept or reject. The flow starts only from the reachable UI or system trigger named in the metadata. The Backend rechecks authentication, role, ownership, membership, consent and current state as applicable; client-side visibility alone never grants access. A confirmed mutation is persisted before the UI displays success. External-service failure returns a safe retry state and must not fabricate completion.

## 2. Class Diagram

```plantuml
@startuml MF08_01_CareGroupInvitationandMembershipLifecycle_ClassDiagram
skinparam classAttributeIconSize 0
hide empty members

class "CareGroupsScreen" as UI1 <<UI>>
class "CareGroupInvitationScreen" as UI2 <<UI>>
class "CareGroupMembersScreen" as UI3 <<UI>>
class "CareGroupController" as Controller1 <<Controller>> {
  - careGroupService: ICareGroupService
  - careTaskService: ICareTaskService
  + createCareGroup(request: CreateCareGroupRequest, principal: Principal): ResponseEntity<ApiResponse<CreateCareGroupResponse>>
  + deleteCareGroup(groupId: UUID, principal: Principal): ResponseEntity<ApiResponse<Void>>
  + leaveCareGroup(groupId: UUID, principal: Principal): ResponseEntity<ApiResponse<LeaveCareGroupResponse>>
  + acceptInvite(groupId: UUID, request: com.carebridge.backend.family.dto.FamilyRelationshipRoleRequest, principal: Principal): ResponseEntity<ApiResponse<CareGroupMemberDto>>
  + getFamilyPermission(groupId: UUID, memberId: UUID, principal: Principal): ResponseEntity<ApiResponse<FamilyPermissionResponse>>
  + inviteFamilyMember(groupId: UUID, request: InviteFamilyMemberRequest, principal: Principal): ResponseEntity<ApiResponse<InviteFamilyMemberResponse>>
  + joinGroupByCode(request: JoinCareGroupRequest, principal: Principal): ResponseEntity<ApiResponse<CareGroupSummaryDto>>
}
class "CareGroupServiceImpl" as Service1 <<Service>> {
  - groupRepository: CareGroupRepository
  - memberRepository: CareGroupMemberRepository
  - userRepository: UserRepository
  - auditService: AuditService
  + acceptInvitationByToken(inviteToken: String, familyRelationshipRole: String, customFamilyRelationshipRole: String, ...): AcceptInvitationByTokenResponse
  + createCareGroup(request: CreateCareGroupRequest, callerId: UUID): CreateCareGroupResponse
  + deleteCareGroup(groupId: UUID, callerId: UUID): void
  + leaveCareGroup(groupId: UUID, callerId: UUID): LeaveCareGroupResponse
  + acceptInvite(groupId: UUID, familyRelationshipRole: String, customFamilyRelationshipRole: String, ...): CareGroupMemberDto
}
interface "ICareGroupService" as Service1Contract <<Service>>
interface "CareGroupRepository" as Repository1 {
  + countByOwnerUserIdAndStatus(ownerUserId: UUID, status: CareGroupStatus): long
  + existsByOwnerUserIdAndGroupNameIgnoreCase(ownerUserId: UUID, groupName: String): boolean
  + findByIdAndStatus(id: UUID, status: CareGroupStatus): Optional<CareGroup>
  + findByOwnerUserIdAndStatus(ownerUserId: UUID, status: CareGroupStatus): List<CareGroup>
  + findByStatus(status: CareGroupStatus): List<CareGroup>
  + findByLinkedBabyProfileId(linkedBabyProfileId: UUID): List<CareGroup>
}
interface "CareGroupMemberRepository" as Repository2 {
  + existsByCareGroupIdAndUserIdAndInviteStatus(careGroupId: UUID, userId: UUID, status: InviteStatus): boolean
  + findByCareGroupIdAndInviteStatusIn(careGroupId: UUID, statuses: List<InviteStatus>): List<CareGroupMember>
  + countByCareGroupId(careGroupId: UUID): long
  + deleteByCareGroupId(careGroupId: UUID): void
  + findAllByCareGroupIdAndUserId(careGroupId: UUID, userId: UUID): List<CareGroupMember>
  + findFirstByCareGroupIdAndUserIdAndInviteStatus(careGroupId: UUID, userId: UUID, inviteStatus: InviteStatus): Optional<CareGroupMember>
}
class "CareGroup" as Entity1 <<Entity>> {
  - id: UUID
  - ownerUserId: UUID
  - groupName: String
  - description: String
  - linkedJourneyId: UUID
  - linkedBabyProfileId: UUID
  - status: CareGroupStatus
}
class "CareGroupMember" as Entity2 <<Entity>> {
  - id: UUID
  - careGroupId: UUID
  - userId: UUID
  - memberRole: GroupMemberRole
  - familyRelationshipRole: String
  - customFamilyRelationshipRole: String
  - inviteStatus: InviteStatus
}
interface "JpaRepository<CareGroup, UUID>" as Repository1Base <<Framework>>
interface "JpaRepository<CareGroupMember, UUID>" as Repository2Base <<Framework>>
class "PostgreSQL" as DB <<Database>>
class "NotificationRecord service" as External <<External Service>>

Service1Contract <|.. Service1 : implements
Repository1Base <|-- Repository1 : extends
Repository2Base <|-- Repository2 : extends
UI1 ..> Controller1 : invokes API
UI2 ..> Controller1 : invokes API
UI3 ..> Controller1 : invokes API
Controller1 --> Service1Contract : delegates
Service1 --> Repository1 : reads / writes
Service1 --> Repository2 : reads / writes
Repository1 ..> Entity1 : maps
Repository1 ..> DB : persists
Repository2 ..> Entity2 : maps
Repository2 ..> DB : persists
Service1 ..> External : invokes when required
Entity1 "1" *-- "0..*" Entity2 : members
@enduml
```

**Figure 1 — Class Diagram: Care Group, Invitation and Membership Lifecycle**

## 3. Sequence Diagram — Main Flow

```plantuml
@startuml MF08_01_CareGroupInvitationandMembershipLifecycle_SequenceDiagram
skinparam shadowing false

actor "Mother / Family" as Actor
boundary ":CareGroupsScreen" as UI1
boundary ":CareGroupInvitationScreen" as UI2
boundary ":CareGroupMembersScreen" as UI3
control ":CareGroupController" as Controller1
participant ":CareGroupServiceImpl" as Service1 <<service>>
participant ":CareGroupRepository" as Repository1 <<repository>>
participant ":CareGroupMemberRepository" as Repository2 <<repository>>
database "PostgreSQL" as DB
participant ":NotificationRecord service" as External1 <<external system>>

group UC-54 Manage Care Groups
  Actor -> UI1 : 1. startManageCareGroups()
  activate UI1
  UI1 -> Controller1 : 2. listMyGroups() / createCareGroup() / deleteCareGroup()
  activate Controller1
  Controller1 -> Service1 : 3. listMyGroups() / createCareGroup() / deleteCareGroup()
  activate Service1
  alt [selected action is view or list]
    Service1 -> Repository1 : 4a. findByOwnerUserIdAndStatus()
    activate Repository1
    Repository1 -> DB : 4a-1. SELECT
    activate DB
    DB --> Repository1 : 4a-2. queryResult
    deactivate DB
    Repository1 --> Service1 : 4a-3. domainRecords
    deactivate Repository1
    Service1 --> Controller1 : 4a-4. resultDTO
    deactivate Service1
    Controller1 --> UI1 : 4a-5. 200 OK
    deactivate Controller1
    UI1 --> Actor : 4a-6. displayCurrentState()
    deactivate UI1
  else [selected action creates, updates, archives or deletes]
    Service1 -> Repository1 : 4b. findByOwnerUserIdAndStatus()
    activate Repository1
    Repository1 -> DB : 4b-1. SELECT
    activate DB
    DB --> Repository1 : 4b-2. currentState
    deactivate DB
    Repository1 --> Service1 : 4b-3. scopedEntity
    deactivate Repository1
    Service1 -> Repository1 : 4b-4. save() / delete()
    activate Repository1
    Repository1 -> DB : 4b-5. INSERT / UPDATE / DELETE
    activate DB
    DB --> Repository1 : 4b-6. persistedState
    deactivate DB
    Repository1 --> Service1 : 4b-7. persistedEntity
    deactivate Repository1
    Service1 --> Controller1 : 4b-8. resultDTO
    deactivate Service1
    Controller1 --> UI1 : 4b-9. 200 OK / 201 Created
    deactivate Controller1
    UI1 --> Actor : 4b-10. displayConfirmedState()
    deactivate UI1
  else [request is invalid, forbidden, not found or conflicting]
    Service1 --> Controller1 : 4c. domainError
    deactivate Service1
    Controller1 --> UI1 : 4c-1. 400 / 401 / 403 / 404 / 409
    deactivate Controller1
    UI1 --> Actor : 4c-2. displayActionableError()
    deactivate UI1
  end
end

group UC-55 Manage Care Group Invitations
  Actor -> UI2 : 5. startManageCareGroupInvitations()
  activate UI2
  UI2 -> Controller1 : 6. inviteFamilyMember() / listMyInvitations() / acceptInvite() / declineInvite()
  activate Controller1
  Controller1 -> Service1 : 7. inviteFamilyMember() / listMyInvitations() / acceptInvite() / declineInvite()
  activate Service1
  alt [selected action is view or list]
    Service1 -> Repository2 : 8a. findByCareGroupIdAndInviteStatusIn()
    activate Repository2
    Repository2 -> DB : 8a-1. SELECT
    activate DB
    DB --> Repository2 : 8a-2. queryResult
    deactivate DB
    Repository2 --> Service1 : 8a-3. domainRecords
    deactivate Repository2
    Service1 --> Controller1 : 8a-4. resultDTO
    deactivate Service1
    Controller1 --> UI2 : 8a-5. 200 OK
    deactivate Controller1
    UI2 --> Actor : 8a-6. displayCurrentState()
    deactivate UI2
  else [selected action creates, updates, archives or deletes]
    Service1 -> Repository2 : 8b. findByCareGroupIdAndInviteStatusIn()
    activate Repository2
    Repository2 -> DB : 8b-1. SELECT
    activate DB
    DB --> Repository2 : 8b-2. currentState
    deactivate DB
    Repository2 --> Service1 : 8b-3. scopedEntity
    deactivate Repository2
    Service1 -> Repository2 : 8b-4. save()
    activate Repository2
    Repository2 -> DB : 8b-5. INSERT / UPDATE
    activate DB
    DB --> Repository2 : 8b-6. persistedState
    deactivate DB
    Repository2 --> Service1 : 8b-7. persistedEntity
    deactivate Repository2
    Service1 ->> External1 : 8b-8. notifyInvitation()
    Service1 --> Controller1 : 8b-9. resultDTO
    deactivate Service1
    Controller1 --> UI2 : 8b-10. 200 OK / 201 Created
    deactivate Controller1
    UI2 --> Actor : 8b-11. displayConfirmedState()
    deactivate UI2
  else [request is invalid, forbidden, not found or conflicting]
    Service1 --> Controller1 : 8c. domainError
    deactivate Service1
    Controller1 --> UI2 : 8c-1. 400 / 401 / 403 / 404 / 409
    deactivate Controller1
    UI2 --> Actor : 8c-2. displayActionableError()
    deactivate UI2
  end
end

group UC-60 Manage Care Group Membership
  Actor -> UI3 : 9. startManageCareGroupMembership()
  activate UI3
  UI3 -> Controller1 : 10. listMembers() / updateFamilyPermission() / removeMember()
  activate Controller1
  Controller1 -> Service1 : 11. listMembers() / updateFamilyPermission() / removeMember()
  activate Service1
  alt [selected action is view or list]
    Service1 -> Repository2 : 12a. findByCareGroupIdAndUserId()
    activate Repository2
    Repository2 -> DB : 12a-1. SELECT
    activate DB
    DB --> Repository2 : 12a-2. queryResult
    deactivate DB
    Repository2 --> Service1 : 12a-3. domainRecords
    deactivate Repository2
    Service1 --> Controller1 : 12a-4. resultDTO
    deactivate Service1
    Controller1 --> UI3 : 12a-5. 200 OK
    deactivate Controller1
    UI3 --> Actor : 12a-6. displayCurrentState()
    deactivate UI3
  else [selected action creates, updates, archives or deletes]
    Service1 -> Repository2 : 12b. findByCareGroupIdAndUserId()
    activate Repository2
    Repository2 -> DB : 12b-1. SELECT
    activate DB
    DB --> Repository2 : 12b-2. currentState
    deactivate DB
    Repository2 --> Service1 : 12b-3. scopedEntity
    deactivate Repository2
    Service1 -> Repository2 : 12b-4. save() / delete()
    activate Repository2
    Repository2 -> DB : 12b-5. INSERT / UPDATE / DELETE
    activate DB
    DB --> Repository2 : 12b-6. persistedState
    deactivate DB
    Repository2 --> Service1 : 12b-7. persistedEntity
    deactivate Repository2
    Service1 ->> External1 : 12b-8. notifyMembershipChange()
    Service1 --> Controller1 : 12b-9. resultDTO
    deactivate Service1
    Controller1 --> UI3 : 12b-10. 200 OK / 201 Created
    deactivate Controller1
    UI3 --> Actor : 12b-11. displayConfirmedState()
    deactivate UI3
  else [request is invalid, forbidden, not found or conflicting]
    Service1 --> Controller1 : 12c. domainError
    deactivate Service1
    Controller1 --> UI3 : 12c-1. 400 / 401 / 403 / 404 / 409
    deactivate Controller1
    UI3 --> Actor : 12c-2. displayActionableError()
    deactivate UI3
  end
end

@enduml
```

**Figure 2 — Sequence Diagram: Care Group, Invitation and Membership Lifecycle Main Flow**

**Brief Explanation:**

1. The diagram separates the implemented goals covered by this specification: UC-54 Manage Care Groups; UC-55 Manage Care Group Invitations; UC-60 Manage Care Group Membership.
2. Each actor action enters through the reachable mobile or web boundary and invokes the exact controller operation exposed by the current Backend.
3. The controller delegates to the mapped service operation; read branches query scoped records, while mutation branches load the current state before persisting the requested transition.
4. Repository calls and PostgreSQL responses are shown explicitly, including call-stack activation and dashed return messages.
5. Invalid, unauthorized, missing or conflicting requests return an actionable error without displaying a false success state.
6. External systems are invoked only for the mapped use cases; notification dispatches are asynchronous, while integrations that return data remain synchronous.

## 4. Business Rules Applied

- Access is enforced server-side using the current actor, role, ownership, membership and consent scope.
- Only the owner manages the group; invited members explicitly accept or reject.
- The following remains outside this contract: Placeholder add-group route as evidence by itself.
- Retries must not duplicate confirmed records, transitions, notifications or external side effects.
- Sensitive health, identity, moderation, location and safety operations retain the minimum required audit evidence.
