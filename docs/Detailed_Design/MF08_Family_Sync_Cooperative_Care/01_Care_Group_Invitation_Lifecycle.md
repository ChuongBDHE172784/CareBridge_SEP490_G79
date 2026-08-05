# MF-08 / Spec 01 — Care Group & Invitation Lifecycle

| Field | Value |
| --- | --- |
| Feature | MF-08 — Family Sync & Cooperative Care |
| Use Cases Covered | Create/list care groups; invite/revoke; accept/decline; join request; remove/leave |
| Primary Actor(s) | Mother (Owner), Family Member |
| Platform | Mother Mobile App, Family Mobile App, CareBridge API |
| Main Flow Summary | Mother creates a care group and invites a family member. The authenticated recipient accepts or declines; membership changes are persisted and rechecked on every protected family operation. |
| Grounding (source code) | `family/controller/CareGroupController.java`, `family/service/impl/CareGroupServiceImpl.java`, `family/entity/CareGroup.java`, `family/entity/CareGroupMember.java`, Mobile `features/familySync/services/care_group_service.dart` |

## 1. Tổng quan luồng chính (Main Flow Overview)

Mother tạo `CareGroup` gắn với hành trình mẹ hoặc hồ sơ bé và trở thành `OWNER`. Lời mời được lưu trên `CareGroupMember` ở trạng thái `PENDING`; hệ thống hỗ trợ lời mời trực tiếp và token/deep-link. Người nhận phải đăng nhập trước khi chấp nhận. Ngoài lời mời, Family Member có thể gửi yêu cầu tham gia bằng mã và chờ Mother duyệt. Thu hồi lời mời, xóa thành viên và rời nhóm đều cập nhật membership hiện hữu; quyền truy cập không được suy ra từ token cũ.

## 2. Class Diagram

```plantuml
@startuml MF08_01_CareGroup_ClassDiagram
skinparam classAttributeIconSize 0
class CareGroup {
  +id: UUID
  +ownerUserId: UUID
  +groupName: String
  +linkedJourneyId: UUID
  +linkedBabyProfileId: UUID
  +status: CareGroupStatus
}
class CareGroupMember {
  +id: UUID
  +careGroupId: UUID
  +userId: UUID
  +memberRole: GroupMemberRole
  +inviteStatus: InviteStatus
  +inviteToken: String
  +inviteExpiresAt: Instant
  +permissionJson: String
}
enum GroupMemberRole { OWNER; MEMBER; VIEWER }
enum InviteStatus { PENDING; ACCEPTED; REJECTED; REVOKED; EXPIRED }
class CareGroupController
interface ICareGroupService
class CareGroupServiceImpl
interface CareGroupRepository
interface CareGroupMemberRepository
CareGroup "1" *-- "1..*" CareGroupMember
CareGroupMember --> GroupMemberRole
CareGroupMember --> InviteStatus
CareGroupController --> ICareGroupService
CareGroupServiceImpl ..|> ICareGroupService
CareGroupServiceImpl --> CareGroupRepository
CareGroupServiceImpl --> CareGroupMemberRepository
@enduml
```

**Hình 1 — Class Diagram: Care Group và vòng đời membership**

## 3. Sequence Diagram — Main Flow

```plantuml
@startuml MF08_01_CareGroup_SequenceDiagram
actor "Mother" as M
actor "Family Member" as F
participant "Mobile UI" as UI
participant "CareGroupController" as Controller
participant "CareGroupServiceImpl" as Service
participant "CareGroupRepository" as GroupRepo
participant "CareGroupMemberRepository" as MemberRepo
database "PostgreSQL" as DB

M -> UI : 1. Nhập thông tin nhóm chăm sóc
activate UI
UI -> Controller : 2. POST /api/v1/care-groups
activate Controller
Controller -> Service : 3. createCareGroup(request, ownerId)
activate Service
Service -> GroupRepo : 4. save(CareGroup)
activate GroupRepo
GroupRepo -> DB : 5. INSERT care_groups
activate DB
DB --> GroupRepo : 6. group row
deactivate DB
GroupRepo --> Service : 7. CareGroup
deactivate GroupRepo
Service -> MemberRepo : 8. save(OWNER, ACCEPTED)
activate MemberRepo
MemberRepo -> DB : 9. INSERT care_group_members
activate DB
DB --> MemberRepo : 10. owner membership
deactivate DB
MemberRepo --> Service : 11. CareGroupMember
deactivate MemberRepo
Service --> Controller : 12. CreateCareGroupResponse
deactivate Service
Controller --> UI : 13. 201 Created
deactivate Controller
UI --> M : 14. Hiển thị nhóm đã tạo
deactivate UI

M -> UI : 15. Gửi lời mời Family Member
activate UI
UI -> Controller : 16. POST /api/v1/care-groups/{groupId}/invitations
activate Controller
Controller -> Service : 17. inviteFamilyMember(groupId, request, ownerId)
activate Service
Service -> MemberRepo : 18. save(PENDING, token, expiry)
activate MemberRepo
MemberRepo -> DB : 19. INSERT/UPDATE care_group_members
activate DB
DB --> MemberRepo : 20. pending membership
deactivate DB
MemberRepo --> Service : 21. CareGroupMember
deactivate MemberRepo
Service --> Controller : 22. InviteFamilyMemberResponse
deactivate Service
Controller --> UI : 23. 200 OK
deactivate Controller
UI --> M : 24. Hiển thị lời mời đã gửi
deactivate UI

F -> UI : 25. Mở lời mời và chọn chấp nhận
activate UI
UI -> Controller : 26. POST /api/v1/care-groups/invitations/{token}/accept
activate Controller
Controller -> Service : 27. acceptInvitationByToken(token, role, familyId)
activate Service
Service -> MemberRepo : 28. find pending invitation for update
activate MemberRepo
MemberRepo -> DB : 29. SELECT ... FOR UPDATE
activate DB
DB --> MemberRepo : 30. pending membership / empty
deactivate DB
MemberRepo --> Service : 31. Optional<CareGroupMember>
deactivate MemberRepo
alt [token hợp lệ, chưa hết hạn và đúng người nhận]
  Service -> MemberRepo : 32a. save(ACCEPTED, joinedAt)
  activate MemberRepo
  MemberRepo -> DB : 32a-1. UPDATE care_group_members
  activate DB
  DB --> MemberRepo : 32a-2. accepted membership
  deactivate DB
  MemberRepo --> Service : 32a-3. CareGroupMember
  deactivate MemberRepo
  Service --> Controller : 32a-4. AcceptInvitationByTokenResponse
  deactivate Service
  Controller --> UI : 32a-5. 200 OK
  deactivate Controller
else [token thiếu, hết hạn, bị thu hồi hoặc đã xử lý]
  Service --> Controller : 32b. BusinessException
  deactivate Service
  Controller --> UI : 32b-1. 404 Not Found hoặc 409 Conflict
  deactivate Controller
end
UI --> F : 33. Hiển thị kết quả
deactivate UI
@enduml
```

**Hình 2 — Sequence Diagram: Tạo nhóm, mời và chấp nhận lời mời**

## 4. Business Rules Applied

- Chỉ Mother/Owner được tạo nhóm, mời, thu hồi, duyệt yêu cầu tham gia và xóa thành viên.
- Mỗi nhóm có đúng một Owner membership ở trạng thái `ACCEPTED`; Owner không rời nhóm bằng luồng thành viên thường.
- Token phải tồn tại, chưa hết hạn, chưa bị thu hồi và chưa được xử lý; replay không được tạo membership thứ hai.
- Thành viên chỉ có quyền sau khi membership là `ACCEPTED`; thu hồi/xóa phải có hiệu lực ở request kế tiếp.
- `linkedJourneyId` và `linkedBabyProfileId` là phạm vi chăm sóc, không phải quyền truy cập độc lập.
