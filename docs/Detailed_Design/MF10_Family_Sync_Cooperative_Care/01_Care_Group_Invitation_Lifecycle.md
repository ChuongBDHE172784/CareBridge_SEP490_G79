# MF-10 / Spec 01 — Care Group & Invitation Lifecycle

| Field | Value |
| --- | --- |
| Feature | MF-10 — Family Sync & Cooperative Care |
| Use Cases Covered | UC-94 Create Care Group, UC-95 Invite or Revoke Family Member Invitation, UC-96 Accept or Reject Care Group Invitation, UC-97 Manage Care Group Membership |
| Primary Actor(s) | Mother (owner), Family Member (invitee) |
| Platform | Mother Mobile App, Family Mobile App |
| Main Flow Summary | A Mother creates a `CareGroup` (owner by default), invites a family member via link/QR/phone, the recipient accepts or rejects after authenticating, and either the owner removes a member or a member leaves on their own. |
| Grounding (source code) | `family/entity/CareGroup.java`, `CareGroupStatus.java`, `family/entity/CareGroupMember.java`, `GroupMemberRole.java`, `InviteStatus.java`, `InviteChannel.java`, `family/controller/CareGroupController.java` (`/api/v1/care-groups`) |

## 1. Tổng quan luồng chính (Main Flow Overview)

`CareGroup` gắn với một `MotherJourney`/`BabyProfile` cụ thể (`linkedJourneyId`/
`linkedBabyProfileId`), do Mother tạo và tự động là `OWNER` (UC-94). Mời thành viên
(UC-95) tạo một `CareGroupMember` với `inviteStatus=PENDING` và một trong ba kênh
(`LINK`/`QR`/`PHONE`) — link/QR dùng `inviteToken` có hạn (`inviteExpiresAt`), phone dùng
`invitedPhone` trực tiếp. Người được mời xác thực rồi chấp nhận hoặc từ chối (UC-96,
chuyển `inviteStatus` sang `ACCEPTED`/`REJECTED`). Sau khi là thành viên chính thức, owner
có thể gỡ thành viên hoặc chính thành viên tự rời nhóm (UC-97) — cả hai đều là thao tác
trên cùng bản ghi `CareGroupMember`, không tạo entity mới.

## 2. Class Diagram

```plantuml
@startuml MF10_01_CareGroup_ClassDiagram
skinparam classAttributeIconSize 0
skinparam classFontStyle bold
skinparam backgroundColor #FAFAFA
skinparam ArrowColor #555555
skinparam ClassBorderColor #2E75B6
skinparam ClassHeaderBackgroundColor #D5E8F0

class CareGroup {
  + id: UUID
  + ownerUserId: UUID
  + groupName: String
  + description: String
  + linkedJourneyId: UUID
  + linkedBabyProfileId: UUID
  + status: CareGroupStatus
}

enum CareGroupStatus {
  ACTIVE
  ARCHIVED
}

class CareGroupMember {
  + id: UUID
  + careGroupId: UUID
  + userId: UUID
  + memberRole: GroupMemberRole
  + inviteStatus: InviteStatus
  + joinedAt: Instant
  + inviteToken: String
  + inviteChannel: InviteChannel
  + inviteExpiresAt: Instant
  + invitedPhone: String
  + permissionJson: String
}

enum GroupMemberRole {
  OWNER
  MEMBER
  VIEWER
}

enum InviteStatus {
  PENDING
  ACCEPTED
  REJECTED
  REVOKED
  EXPIRED
}

enum InviteChannel {
  LINK
  QR
  PHONE
}

class CareGroupController {
  - careGroupService: ICareGroupService
  + create(CreateCareGroupRequest): ResponseEntity
  + invite(groupId, InviteCareGroupMemberRequest): ResponseEntity
  + revoke(groupId, targetUserId): ResponseEntity
  + acceptByToken(token): ResponseEntity
  + accept(groupId): ResponseEntity
  + decline(groupId): ResponseEntity
  + removeMember(groupId, targetUserId): ResponseEntity
  + leave(groupId): ResponseEntity
  + members(groupId): ResponseEntity
}

interface ICareGroupService <<interface>> {
  + create(ownerId: UUID, request): CareGroup
  + invite(ownerId: UUID, groupId: UUID, request): CareGroupMember
  + respondInvitation(userId: UUID, groupId: UUID, accept: boolean): CareGroupMember
  + removeMember(actorId: UUID, groupId: UUID, targetUserId: UUID): void
}

class CareGroupServiceImpl implements ICareGroupService {
  - careGroupRepository: CareGroupRepository
  - careGroupMemberRepository: CareGroupMemberRepository
  - inviteTokenGenerator: InviteTokenGenerator
  - careGroupAuthorizationPolicy: CareGroupAuthorizationPolicy
  - auditService: AuditService
}

CareGroup --> CareGroupStatus
CareGroup "1" *-- "1..*" CareGroupMember : has (owner + invited)
CareGroupMember --> GroupMemberRole
CareGroupMember --> InviteStatus
CareGroupMember --> InviteChannel
CareGroupController --> ICareGroupService : uses
CareGroupServiceImpl --> CareGroupAuthorizationPolicy : enforces owner-only actions
CareGroupServiceImpl --> AuditService : emits CARE_GROUP_*

@enduml
```

**Hình 1 — Class Diagram: Care Group & Member Invitation**

## 3. Sequence Diagram — Main Flow

```plantuml
@startuml MF10_01_CareGroup_SequenceDiagram
skinparam sequenceArrowThickness 2
skinparam roundcorner 10
skinparam backgroundColor #FAFAFA

actor "Mother (Owner)" as M
participant "CareGroupController" as Controller
participant "CareGroupServiceImpl" as Service
actor "Family Member" as F
participant "AuditService" as Audit
database "PostgreSQL" as DB

== UC-94 Create Care Group ==
M -> Controller : POST /api/v1/care-groups\n{groupName, linkedJourneyId}
Controller -> Service : create(ownerId, request)
Service -> DB : INSERT INTO care_groups (status=ACTIVE)
Service -> DB : INSERT INTO care_group_members\n(userId=ownerId, memberRole=OWNER, inviteStatus=ACCEPTED)
Service -> Audit : emit(CARE_GROUP_CREATED)
Service --> Controller : CareGroup
Controller --> M : HTTP 201 Created

== UC-95 Invite or Revoke Family Member Invitation ==
M -> Controller : POST /api/v1/care-groups/{groupId}/invitations\n{inviteChannel=LINK}
Controller -> Service : invite(ownerId, groupId, request)
Service -> DB : INSERT INTO care_group_members\n(inviteStatus=PENDING, inviteToken, inviteExpiresAt)
Service -> Audit : emit(CARE_GROUP_MEMBER_INVITED)
Service --> Controller : CareGroupMember{inviteStatus=PENDING}
Controller --> M : HTTP 201 Created {inviteLink}

== UC-96 Accept or Reject Care Group Invitation ==
F -> Controller : POST /api/v1/care-groups/invitations/{token}/accept
Controller -> Service : respondInvitation(userId, token, accept=true)
Service -> Service : check inviteToken hợp lệ & chưa hết hạn
Service -> DB : UPDATE care_group_members\nSET inviteStatus='ACCEPTED', joined_at=now()
Service -> Audit : emit(CARE_GROUP_INVITE_ACCEPTED)
Service --> Controller : CareGroupMember{inviteStatus=ACCEPTED}
Controller --> F : HTTP 200 OK

== UC-97 Manage Care Group Membership ==
M -> Controller : DELETE /api/v1/care-groups/{groupId}/members/{targetUserId}
Controller -> Service : removeMember(ownerId, groupId, targetUserId)
Service -> Service : check actor == OWNER
Service -> DB : DELETE FROM care_group_members WHERE ...
Service -> Audit : emit(CARE_GROUP_MEMBER_REMOVED)
Service --> Controller : void
Controller --> M : HTTP 204 No Content

F -> Controller : POST /api/v1/care-groups/{groupId}/leave
Controller -> Service : removeMember(memberUserId, groupId, memberUserId)
Service -> DB : DELETE FROM care_group_members WHERE user_id=? AND care_group_id=?
Service -> Audit : emit(CARE_GROUP_MEMBER_LEFT)
Service --> F : HTTP 204 No Content

@enduml
```

**Hình 2 — Sequence Diagram: Create Group → Invite → Accept → Remove/Leave (Main Flow)**

## 4. State Machine — `CareGroupMember.inviteStatus`

```plantuml
@startuml MF10_01_InviteStatus_StateMachine
skinparam backgroundColor #FAFAFA
skinparam StateBackgroundColor #D5E8F0
skinparam StateBorderColor #2E75B6

[*] --> ACCEPTED : Owner tạo nhóm → thành viên OWNER tự ACCEPTED (UC-94)
[*] --> PENDING : Owner mời thành viên mới (UC-95)

PENDING --> ACCEPTED : Family Member chấp nhận (UC-96)
PENDING --> REJECTED : Family Member từ chối (UC-96)
PENDING --> REVOKED : Owner thu hồi lời mời trước khi được chấp nhận (UC-95)
PENDING --> EXPIRED : Quá inviteExpiresAt mà chưa phản hồi

ACCEPTED --> [*] : Owner gỡ thành viên hoặc thành viên tự rời (UC-97)\n[bản ghi bị xoá, không chuyển state]

REJECTED --> [*]
REVOKED --> [*]
EXPIRED --> [*]

note right of ACCEPTED
  UC-97 (remove/leave) thực hiện DELETE bản ghi CareGroupMember
  chứ không chuyển sang trạng thái "REMOVED" — đúng theo
  ICareGroupService.removeMember() thật trong code.
end note

@enduml
```

**Hình 3 — State Machine: `CareGroupMember.inviteStatus` Lifecycle**

## 5. Business Rules Applied

- BR-RBAC — chỉ `OWNER` mời/thu hồi lời mời và gỡ thành viên khác; thành viên thường chỉ tự rời được chính mình.
- UC-95 — link/QR dùng `inviteToken` có hạn; kênh phone không cần token nhưng vẫn yêu cầu xác thực khi chấp nhận (UC-96).
- UC-96 — chỉ người dùng đã xác thực mới chấp nhận/từ chối được lời mời của chính họ.
- UC-97 — quyền gỡ thành viên và quyền rời nhóm được tách biệt theo vai trò (`CareGroupAuthorizationPolicy`).
